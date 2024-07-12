package com.example.yourable;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Locale;

public class ConversationPage extends AppCompatActivity {

    private boolean isListening = false;
    private boolean permissionToRecordAccepted = false;
    private static final int REQUEST_RECORD_PERMISSION = 100;

    private RelativeLayout relativeLayout;
    private int lastTextViewId = 0;
    private int clicked = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.conversation_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_PERMISSION);
        } else {
            permissionToRecordAccepted = true;
        }

        Button triggerBtn = findViewById(R.id.trigger_btn);
        Button backBtn = findViewById(R.id.back_btn);
        relativeLayout = findViewById(R.id.scrollChat);

        triggerBtn.setOnClickListener(v -> {
            if (clicked == 0){
                clicked = 1;
                triggerBtn.setBackgroundResource(R.drawable.button_bg);
                triggerBtn.setText("Stop");
                startListening();
            }else{
                clicked = 0;
                triggerBtn.setBackgroundResource(R.drawable.start_btn);
                triggerBtn.setText("Start");
                stopListening();
            }
        });

        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainPageDeaf.class);
            startActivity(intent);
        });
    }

    private void addTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(15);
        textView.setBackgroundResource(R.drawable.cloud_word);
        textView.setTextColor(getResources().getColor(R.color.black));
        textView.setPadding(50, 30, 40, 30);
        textView.setId(View.generateViewId());
        textView.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        ));

        textView.setSingleLine(false);
        textView.setEllipsize(null);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        if (lastTextViewId != 0) {
            params.addRule(RelativeLayout.BELOW, lastTextViewId);
        }

        params.setMargins(0, 0, 0, 20); // Setting bottom margin

        textView.setLayoutParams(params);
        relativeLayout.addView(textView);
        lastTextViewId = textView.getId();
    }

    private void startListening() {
        if (permissionToRecordAccepted) {
            isListening = true;

            Intent intentAudio = new Intent("com.example.yourable.RECORDING_STATUS");
            intentAudio.putExtra("recordingName", true);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intentAudio);

            SpeechRecognizer speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Toast.makeText(ConversationPage.this, "Listening...", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    if (isListening) {
                        startListening();
                    }
                }

                @Override
                public void onError(int error) {
                    Toast.makeText(ConversationPage.this, "Sorry, didn't hear", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        addTextView(matches.get(0));

                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
            speechRecognizer.startListening(speechRecognizerIntent);
        } else {
            Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopListening() {
        isListening = false;
    }

}