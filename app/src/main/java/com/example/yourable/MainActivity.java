package com.example.yourable;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private ExecutorService cameraExecutor;
    private PreviewView previewView;

    private long lastDetectionTime = 0;
    private static final long DETECTION_INTERVAL_MS = 2000; // 2 detik
    private String filePath;
    private Bitmap detectedBitmap;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Unable to load OpenCV");
        } else {
            Log.d("OpenCV", "OpenCV loaded");
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastDetectionTime >= DETECTION_INTERVAL_MS) {
                            lastDetectionTime = currentTime;
                            detectWhitePaper(image);
                        }
                        image.close();
                    }
                });

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void detectWhitePaper(ImageProxy image) {
        Bitmap bitmap = YuvToRgbConverter.yuvToRgb(image);

        if (bitmap != null) {
            Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
            org.opencv.android.Utils.bitmapToMat(bitmap, mat);

            Mat grayMat = new Mat();
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY);

            Mat blurredMat = new Mat();
            Imgproc.GaussianBlur(grayMat, blurredMat, new Size(5, 5), 0);

            Mat edgedMat = new Mat();
            Imgproc.Canny(blurredMat, edgedMat, 75, 200);

            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(edgedMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

            Bitmap detectedBitmap = null;

            for (MatOfPoint contour : contours) {
                MatOfPoint2f approxCurve = new MatOfPoint2f();
                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                double contourLength = Imgproc.arcLength(contour2f, true);
                Imgproc.approxPolyDP(contour2f, approxCurve, 0.02 * contourLength, true);

                if (approxCurve.total() == 4) {
                    Rect rect = Imgproc.boundingRect(new MatOfPoint(approxCurve.toArray()));

                    // Adjust aspect ratio condition to capture the entire paper
                    float aspectRatio = (float) rect.width / rect.height;
                    if (aspectRatio > 0.8 && aspectRatio < 1.2) {
                        Mat roi = mat.submat(rect);
                        Scalar meanColor = Core.mean(roi);

                        if (meanColor.val[0] > 100 && meanColor.val[1] > 100 && meanColor.val[2] > 100) {
                            detectedBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                            org.opencv.android.Utils.matToBitmap(mat, detectedBitmap);

                            filePath = saveBitmapToFile(detectedBitmap);
                            break;
                        }
                    }
                }
            }

            if (detectedBitmap != null && filePath != null) {
                Log.d("ColorDetection", "Kertas putih terdeteksi!");

                Bitmap finalDetectedBitmap = detectedBitmap;
                runOnUiThread(() -> {
                    Button btn = findViewById(R.id.btnbtn);
                    btn.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                        intent.putExtra("detectedBitmapPath", filePath);
                        startActivity(intent);
                    });
                });

            } else {
                Log.d("ColorDetection", "Tidak ada kertas putih yang terdeteksi.");
            }
        } else {
            Log.e("ColorDetection", "Bitmap is null");
        }
    }

    private String saveBitmapToFile(Bitmap bitmap) {
        String filename = "detected_paper.jpg";
        File outputDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File outputFile = new File(outputDir, filename);
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
            return outputFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }






    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
