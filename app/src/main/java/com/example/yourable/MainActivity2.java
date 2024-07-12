package com.example.yourable;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity2 extends ComponentActivity {

    private EditText editText;
    private TTSWrapper ttsWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String filePath = getIntent().getStringExtra("detectedBitmapPath");
        Bitmap detectedBitmap = readBitmapFromFile(filePath);
        ttsWrapper = new TTSWrapper(this);

        if (detectedBitmap != null) {
            ImageView imageView = findViewById(R.id.imageView2);
            imageView.setImageBitmap(detectedBitmap);
            detect(detectedBitmap);
        } else {
            Toast.makeText(this, "Failed to read bitmap.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public void detect(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(getApplicationContext(), "Bitmap is Null", Toast.LENGTH_LONG).show();
            return;
        }
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        process_text(visionText);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Failed to detect text", Toast.LENGTH_LONG).show();
                        Log.e("TextRecognition", "Error: ", e);
                    }
                });
    }

    private void process_text(Text visionText) {
        List<Text.TextBlock> blocks = visionText.getTextBlocks();
        if (blocks.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No Text Detected", Toast.LENGTH_LONG).show();
        } else {
            StringBuilder resultTextBuilder = new StringBuilder();
            for (Text.TextBlock block : blocks) {
                resultTextBuilder.append(block.getText()).append("\n");
            }

            String text = resultTextBuilder.toString();
            if (ttsWrapper != null) {
                ttsWrapper.speak(text);
            }
            Button btn = findViewById(R.id.btnbtn);
            btn.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainPageBlind.class);
                startActivity(intent);
            });
        }
    }

    private Bitmap readBitmapFromFile(String filePath) {
        try {
            File file = new File(filePath);
            FileInputStream stream = new FileInputStream(file);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            stream.close();
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
