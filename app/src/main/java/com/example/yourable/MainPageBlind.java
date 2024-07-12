package com.example.yourable;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainPageBlind extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private CardView readBtn;
    private TextView quotesText;
    private String nickname;
    private ConstraintLayout bgAdd;
    private DatabaseReference databaseReference;
    private List<String> quotesList = new ArrayList<>();
    private Random random = new Random();
    private TTSWrapper ttsWrapper;

    private static final int REQUEST_RECORD_PERMISSION = 100;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private boolean permissionToRecordAccepted = false;
    private TextView resultText;
    private boolean isListening = false;
    private int detectedName = 0;
    private String userName;

    private TextToSpeech textToSpeech;
    private boolean isTTSInitialized = false;
    private SharedPreferences.Editor editor;
    private ConstraintLayout triggerScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.main_page_blind);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        ttsWrapper = new TTSWrapper(this);
        readBtn = findViewById(R.id.read_book);
        quotesText = findViewById(R.id.quotes_text);
        TextView titleUser = findViewById(R.id.username);
        triggerScreen = findViewById(R.id.screen_trigger);

        SharedPreferences sharedPreferences = getSharedPreferences("data", MODE_PRIVATE);
        nickname = sharedPreferences.getString("nickname", null);

        titleUser.setText("Hallo, " + nickname);
        textToSpeech = new TextToSpeech(this, this);

        triggerScreen.setOnClickListener(v -> {
            String quotes = quotesText.getText().toString().trim();
            if (ttsWrapper != null) {
                ttsWrapper.speak("Hallo " + nickname + ", Nice to meet you, "+ quotes+". Please, if you want to open read book feature, tap once on the screen, point the camera at the book, tap again to read. After that, if you want to exit, tap on the screen again.");
            }
            triggerScreen.setOnClickListener(v2 -> {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            });
        });

        databaseReference = FirebaseDatabase.getInstance().getReference("Quotes");

        fetchQuotes();

        bgAdd = findViewById(R.id.bg_add);
        Button btnAdd = findViewById(R.id.btn_sign);

        btnAdd.setOnClickListener(v -> {
            bgAdd.setVisibility(View.VISIBLE);
            readBtn.setVisibility(View.INVISIBLE);

            Button btnSend = findViewById(R.id.btn_add);
            Button btnBackAdd = findViewById(R.id.btn_back);

            btnBackAdd.setOnClickListener(v1 -> {
                bgAdd.setVisibility(View.INVISIBLE);
                readBtn.setVisibility(View.VISIBLE);
            });
            btnSend.setOnClickListener(v1 -> {
                sendQuote();
            });

        });

        readBtn.setOnClickListener(v -> {

        });
    }

    private void sendQuote() {
        EditText editQuotes = findViewById(R.id.editQuote);
        String newQuote = editQuotes.getText().toString().trim();
        if (TextUtils.isEmpty(newQuote)) {
            Toast.makeText(MainPageBlind.this, "Please enter a field", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.push().setValue(newQuote).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(MainPageBlind.this, "Your message has been conveyed, thank you for your spirit.", Toast.LENGTH_SHORT).show();
                bgAdd.setVisibility(View.INVISIBLE);
                readBtn.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(MainPageBlind.this, "Your message failed to send, sorry", Toast.LENGTH_SHORT).show();
            }
        });
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
                Toast.makeText(MainPageBlind.this, "Failed to load quotes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void speak(String text) {
        if (ttsWrapper != null) {
            ttsWrapper.speak(text);
        }
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

    private void startListening(TextView panel, int type) {
        if (permissionToRecordAccepted) {
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Toast.makeText(MainPageBlind.this, "Listening...", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onBeginningOfSpeech() {
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                }

                @Override
                public void onError(int error) {
                    isListening = true;
                    Toast.makeText(MainPageBlind.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    if (isTTSInitialized) {
                        speak("Sorry, can you repeat what you said?");
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        if (type == 0){
                            userName = matches.get(0);
                            panel.setText(userName);
                            editor.putString("nickname", userName);
                            editor.apply();

                            if (isTTSInitialized) {
                                speak("I can call you "+ userName +", is that right?");
                            }

                            startListening(panel, 1);
                        }else if (type == 1){
                            panel.setText(matches.get(0));
                            String compare = matches.get(0).toString().toLowerCase().trim();
                            if(compare.equals("yes")){
                                if (isTTSInitialized) {
                                    speak("nice to meet you "+ userName +", I hope you have a wonderful day");
                                }
                                Intent intent = new Intent(MainPageBlind.this, MainPageBlind.class);
                                startActivity(intent);
                            } else if (matches.get(0).equals("no")) {
                                speak("sorry, so what can I call you");
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        startListening(resultText, 0);
                                    }
                                }, 1000);
                            }
                        }
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });
            speechRecognizer.startListening(speechRecognizerIntent);
            isListening = true;
        } else {
            Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_PERMISSION) {
            permissionToRecordAccepted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!permissionToRecordAccepted) {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }



    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show();
            } else {
                isTTSInitialized = true;
            }
        } else {
            Toast.makeText(this, "Initialization failed", Toast.LENGTH_SHORT).show();
        }
    }
}
