package com.example.yourable;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class AudioRecordingService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AudioRecordingServiceChannel";
    private static final int SAMPLE_RATE = 44100;
    private AudioRecord audioRecord;
    private boolean isRecording = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.main_logo);
        Intent stopIntent = new Intent(this, NotificationAction.class);
        stopIntent.setAction("STOP_RECORDING");
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Danger Detection")
                .setContentText("Start in the background, ready to help you :D")
                .setSmallIcon(R.drawable.main_logo)
                .setLargeIcon(largeIcon)
                .addAction(R.drawable.button_bg, "Stop Recording", stopPendingIntent)  // Menambahkan tombol
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();


        startForeground(NOTIFICATION_ID, notification);

        startRecording();

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Audio Recording Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void startRecording() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.startRecording();
            isRecording = true;

            new Thread(() -> {
                short[] buffer = new short[bufferSize];
                while (isRecording) {
                    int readSize = audioRecord.read(buffer, 0, bufferSize);
                    double rms = calculateRMS(buffer, readSize);
                    double decibel = 20 * Math.log10(rms);
                    Log.d("AudioRecordingService", "Decibel: " + decibel);
                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (decibel > 90) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 100, 500}, -1));
                        } else {
                            vibrator.vibrate(new long[]{0, 500, 100, 500}, -1);
                        }
                    }
                }
            }).start();
        }
    }

    private double calculateRMS(short[] buffer, int readSize) {
        long sum = 0;
        for (int i = 0; i < readSize; i++) {
            sum += buffer[i] * buffer[i];
        }
        return Math.sqrt(sum / (double) readSize);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioRecord != null) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
        }
    }
}
