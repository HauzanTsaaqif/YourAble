package com.example.yourable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class NotificationAction extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("STOP_RECORDING_DANGER".equals(intent.getAction())) {
            Intent stopServiceIntent = new Intent(context, AudioRecordingService.class);
            context.stopService(stopServiceIntent);
            Toast.makeText(context, "Feature stopped", Toast.LENGTH_SHORT).show();
            Intent intentAudio = new Intent("com.example.yourable.RECORDING_STATUS");
            intentAudio.putExtra("isRecording", false);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intentAudio);
        }
        if ("STOP_RECORDING_NAME".equals(intent.getAction())) {
            Intent stopServiceIntent = new Intent(context, AudioRecordingService.class);
            context.stopService(stopServiceIntent);
            Toast.makeText(context, "Feature stopped", Toast.LENGTH_SHORT).show();
            Intent intentAudio = new Intent("com.example.yourable.RECORDING_STATUS");
            intentAudio.putExtra("recordingName", false);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intentAudio);
        }
    }
}
