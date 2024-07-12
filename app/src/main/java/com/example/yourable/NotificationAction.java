package com.example.yourable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class NotificationAction extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("STOP_RECORDING".equals(intent.getAction())) {
            // Tangani penghentian perekaman
            Intent stopServiceIntent = new Intent(context, AudioRecordingService.class);
            context.stopService(stopServiceIntent);
            Toast.makeText(context, "Recording stopped", Toast.LENGTH_SHORT).show();
        }
    }
}
