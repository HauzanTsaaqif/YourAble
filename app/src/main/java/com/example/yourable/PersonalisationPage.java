package com.example.yourable;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PersonalisationPage extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private int pressed = 0;
    private Animation slideLeft, slideRight;
    private ConstraintLayout consName, consName2, consCondition, screenTrigger;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.personalisation_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView mainLogo = findViewById(R.id.main_logo);
        ConstraintLayout mainCons = findViewById(R.id.main_cons);
        Animation popIn = AnimationUtils.loadAnimation(this, R.anim.pop_in);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);

        mainLogo.setVisibility(View.INVISIBLE);
        mainCons.setVisibility(View.INVISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mainLogo.setVisibility(View.VISIBLE);
                mainLogo.startAnimation(popIn);
                }
        }, 1000);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mainCons.setVisibility(View.VISIBLE);
                mainCons.setAnimation(slideIn);

                float currentLogoY = mainLogo.getY();
                float targetLogoY = currentLogoY - 600;
                float currentContY = mainCons.getY();
                float targetContY = currentContY - 600;

                ObjectAnimator animLogo = ObjectAnimator.ofFloat(mainLogo, "y", currentLogoY, targetLogoY);
                animLogo.setDuration(2000);
                ObjectAnimator animCont = ObjectAnimator.ofFloat(mainCons, "y", currentContY, targetContY);
                animCont.setDuration(2000);
                animLogo.start();
                animCont.start();
            }
        }, 3500);

        Button deafButton = findViewById(R.id.deaf_button);
        Button blindButton = findViewById(R.id.blind_button);
        Button nextButton = findViewById(R.id.next_button);
        consName = findViewById(R.id.container_name);
        consName2 = findViewById(R.id.container_name2);
        consCondition = findViewById(R.id.container_condition);
        screenTrigger = findViewById(R.id.screen_trigger);

        consName.setVisibility(View.INVISIBLE);
        consName2.setVisibility(View.INVISIBLE);
        screenTrigger.setVisibility(View.INVISIBLE);

        textToSpeech = new TextToSpeech(this, this);

        deafButton.setOnClickListener(v -> {
            slideLeft = AnimationUtils.loadAnimation(this, R.anim.slide_left);
            slideRight = AnimationUtils.loadAnimation(this, R.anim.slide_right);

            pressed = 1;
            consName.setVisibility(View.VISIBLE);
            consName.setAnimation(slideLeft);
            consCondition.setAnimation(slideRight);
            consCondition.setVisibility(View.INVISIBLE);

            nextButton.setOnClickListener(v1 -> {
                EditText inputName = findViewById(R.id.input_name);
                String nickName = inputName.getText().toString();

                SharedPreferences sharedPreferences = getSharedPreferences("data", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();

                editor.putString("nickname", nickName);
                editor.apply();
                Intent intent = new Intent(PersonalisationPage.this, MainPageDeaf.class);
                intent.putExtra("nickname", nickName);
                startActivity(intent);
            });
        });

        blindButton.setOnClickListener(v -> {
            resultText = findViewById(R.id.detected_text);
            slideLeft = AnimationUtils.loadAnimation(this, R.anim.slide_left);
            slideRight = AnimationUtils.loadAnimation(this, R.anim.slide_right);

            pressed = 2;
            consName2.setVisibility(View.VISIBLE);
            consName2.setAnimation(slideLeft);
            consCondition.setAnimation(slideRight);
            consCondition.setVisibility(View.INVISIBLE);
            screenTrigger.setVisibility(View.VISIBLE);

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_PERMISSION);

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            if (isTTSInitialized) {
                speak("Hello, I'm Abled, how may I call you?");
            } else {
                Toast.makeText(this, "Text-to-Speech not initialized", Toast.LENGTH_SHORT).show();
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startListening(resultText, 0);
                }
            }, 1000);

        });

        nextButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainPageDeaf.class);
            startActivity(intent);
        });

        screenTrigger.setOnClickListener(v -> {
            startListening(resultText, 0);
        });
    }

    @Override
    public void onBackPressed() {
        slideLeft = AnimationUtils.loadAnimation(this, R.anim.slide_left);
        slideRight = AnimationUtils.loadAnimation(this, R.anim.slide_right);

        if (pressed == 1){
            consName.setVisibility(View.INVISIBLE);
            consName.setAnimation(slideLeft);
            consCondition.setAnimation(slideRight);
            consCondition.setVisibility(View.VISIBLE);
            pressed = 0;
        } else if (pressed == 2) {
            consName2.setVisibility(View.INVISIBLE);
            consName2.setAnimation(slideLeft);
            consCondition.setAnimation(slideRight);
            consCondition.setVisibility(View.VISIBLE);
            screenTrigger.setVisibility(View.INVISIBLE);
            pressed = 0;
        }else {
            super.onBackPressed();
        }
    }

    private void startListening(TextView panel, int type) {
        if (permissionToRecordAccepted) {
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Toast.makeText(PersonalisationPage.this, "Listening...", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(PersonalisationPage.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    if (isTTSInitialized) {
                        speak("Sorry, can you repeat what you said?");
                    }
                    startListening(panel, 0);
                    if (type == 1){
                        if (isTTSInitialized) {
                            speak("ya, or no?");
                        }
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        if (type == 0){
                            userName = matches.get(0);
                            panel.setText(userName);

                            if (isTTSInitialized) {
                                speak("I can call you "+ userName +", is that right?");
                            }

                            startListening(panel, 1);
                        }else if (type == 1){
                            panel.setText(matches.get(0));
                            if(matches.get(0).equals("ya")){
                                if (isTTSInitialized) {
                                    speak("nice to meet you "+ userName +", I hope you have a wonderful day");
                                }
                                Intent intent = new Intent(PersonalisationPage.this, MainPageBlind.class);
                                startActivity(intent);
                            } else if (matches.get(0).equals("no")) {
                                if (isTTSInitialized) {
                                    speak("Sorry, can you repeat what you said?");
                                }
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

    private void speak(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
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
                // Text-to-Speech is now ready to be used.
            }
        } else {
            Toast.makeText(this, "Initialization failed", Toast.LENGTH_SHORT).show();
        }
    }
}