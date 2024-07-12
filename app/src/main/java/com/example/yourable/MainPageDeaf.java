package com.example.yourable;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainPageDeaf extends AppCompatActivity {

    private static final int REQUEST_RECORD_PERMISSION = 100;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 200;
    private boolean permissionToRecordAccepted = false;
    private TextView quotesText;
    private BroadcastReceiver recordingStatusReceiver;
    private Switch dangerSwitch, nameSwitch;
    private Intent serviceIntent;
    private boolean isListening = false;
    private boolean recordingName;
    private String nickname;
    private DatabaseReference databaseReference;
    private CardView callingBtn, dangerBtn, conversationBtn;

    private ConstraintLayout bgAdd;

    private List<String> quotesList = new ArrayList<>();
    private Random random = new Random();

    private BroadcastReceiver recordAudioPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.yourable.REQUEST_RECORD_AUDIO_PERMISSION".equals(intent.getAction())) {
                ActivityCompat.requestPermissions(MainPageDeaf.this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_REQUEST_RECORD_AUDIO);

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.main_page_deaf);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        callingBtn = findViewById(R.id.card_calling);
        dangerBtn = findViewById(R.id.card_danger);
        conversationBtn = findViewById(R.id.card_conversation);
        ImageButton toConversation = findViewById(R.id.btn_conversation);
        quotesText = findViewById(R.id.quotes_text);
        TextView titleUser = findViewById(R.id.username);

        dangerSwitch = findViewById(R.id.idktr_danger);
        nameSwitch = findViewById(R.id.idktr_name);

        serviceIntent = new Intent(this, AudioRecordingService.class);

        SharedPreferences sharedPreferences = getSharedPreferences("data", MODE_PRIVATE);
        nickname = sharedPreferences.getString("nickname", null);

        titleUser.setText("Hallo, " + nickname);

        recordingStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean isRecording = intent.getBooleanExtra("isRecording", false);
                recordingName = intent.getBooleanExtra("recordingName", false);
                dangerSwitch.setChecked(isRecording);
                nameSwitch.setChecked(recordingName);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(recordingStatusReceiver,
                new IntentFilter("com.example.yourable.RECORDING_STATUS"));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_PERMISSION);
        } else {
            permissionToRecordAccepted = true;
        }

        callingBtn.setOnClickListener(v -> {
            if (permissionToRecordAccepted) {
                serviceIntent.putExtra("key_service", "name");
                Log.d("before", String.valueOf(isListening));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                if (isListening == false){
                    startListening(quotesText, 0);}
                else {
                    stopListening();
                }
                Log.d("ssssssssssssss", String.valueOf(isListening));
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        });

        dangerBtn.setOnClickListener(v -> {
            if (permissionToRecordAccepted) {
                serviceIntent.putExtra("key_service", "danger");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        });

        toConversation.setOnClickListener(v -> {
            Intent intent = new Intent(MainPageDeaf.this, ConversationPage.class);
            startActivity(intent);
        });

        databaseReference = FirebaseDatabase.getInstance().getReference("Quotes");

        fetchQuotes();

        bgAdd = findViewById(R.id.bg_add);
        Button btnAdd = findViewById(R.id.btn_sign);

        btnAdd.setOnClickListener(v -> {
            bgAdd.setVisibility(View.VISIBLE);
            conversationBtn.setVisibility(View.INVISIBLE);
            callingBtn.setVisibility(View.INVISIBLE);
            dangerBtn.setVisibility(View.INVISIBLE);

            Button btnSend = findViewById(R.id.btn_add);
            Button btnBackAdd = findViewById(R.id.btn_back);

            btnBackAdd.setOnClickListener(v1 -> {
                bgAdd.setVisibility(View.INVISIBLE);
                conversationBtn.setVisibility(View.VISIBLE);
                callingBtn.setVisibility(View.VISIBLE);
                dangerBtn.setVisibility(View.VISIBLE);
            });
            btnSend.setOnClickListener(v1 -> {
                sendQuote();
            });

        });

        LocalBroadcastManager.getInstance(this).registerReceiver(recordingStatusReceiver, new IntentFilter("com.example.yourable.RECORDING_STATUS"));
    }

    private void sendQuote() {
        EditText editQuotes = findViewById(R.id.editQuote);
        String newQuote = editQuotes.getText().toString().trim();
        if (TextUtils.isEmpty(newQuote)) {
            Toast.makeText(MainPageDeaf.this, "Please enter a field", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.push().setValue(newQuote).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(MainPageDeaf.this, "Your message has been conveyed, thank you for your spirit.", Toast.LENGTH_SHORT).show();
                bgAdd.setVisibility(View.INVISIBLE);
                conversationBtn.setVisibility(View.VISIBLE);
                callingBtn.setVisibility(View.VISIBLE);
                dangerBtn.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(MainPageDeaf.this, "Your message failed to send, sorry", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startListening(TextView panel, int type) {
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
                    Toast.makeText(MainPageDeaf.this, "Listening...", Toast.LENGTH_SHORT).show();
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
                        startListening(panel, type);
                    }
                }

                @Override
                public void onError(int error) {
                    Toast.makeText(MainPageDeaf.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String userName = matches.get(0).toLowerCase();
                        String compareName = nickname.toLowerCase();
                        if (compareName.equals(userName)) {
                            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            if (vibrator != null && vibrator.hasVibrator()) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 100, 500}, -1));
                                } else {
                                    vibrator.vibrate(new long[]{0, 500, 100, 500}, -1);
                                }
                            }
                        }
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
        nameSwitch.setChecked(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_PERMISSION) {
            permissionToRecordAccepted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!permissionToRecordAccepted) {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Izin diberikan, panggil layanan untuk memulai mendengarkan
                startListeningService();
            } else {
                // Izin ditolak, beri tahu pengguna atau ambil tindakan alternatif
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startListeningService() {
        Intent serviceIntent = new Intent(this, AudioRecordingService.class);
        serviceIntent.putExtra("key_service", "listening");
        startService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(MainPageDeaf.this, AudioRecordingService.class));
    }

    private void fetchQuotes() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                quotesList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String quote = snapshot.getValue(String.class);
                    if (quote != null && !quote.isEmpty()) {
                        quotesList.add(quote);
                    }
                }
                displayRandomQuote();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainPageDeaf.this, "Failed to load quotes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayRandomQuote() {
        if (quotesList.isEmpty()) {
            quotesText.setText("No quotes available");
            return;
        }

        int index = random.nextInt(quotesList.size());
        String randomQuote = quotesList.get(index);

        int maxLength = 100;
        List<String> filteredQuotes = new ArrayList<>();
        for (String quote : quotesList) {
            if (quote.length() <= maxLength) {
                filteredQuotes.add(quote);
            }
        }

        if (!filteredQuotes.isEmpty()) {
            index = random.nextInt(filteredQuotes.size());
            randomQuote = filteredQuotes.get(index);
        }

        quotesText.setText("\"" + randomQuote + "\"");
    }

}
