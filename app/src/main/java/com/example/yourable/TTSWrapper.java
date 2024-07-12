package com.example.yourable;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

public class TTSWrapper implements TextToSpeech.OnInitListener {
    private TextToSpeech textToSpeech;
    private boolean isTTSInitialized = false;
    private Context context;

    public TTSWrapper(Context context) {
        this.context = context;
        textToSpeech = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(context, "Language not supported", Toast.LENGTH_SHORT).show();
            } else {
                isTTSInitialized = true;
            }
        } else {
            Toast.makeText(context, "Initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    public void speak(String text) {
        if (isTTSInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            Log.e("TTS", "TextToSpeech not initialized");
            Toast.makeText(context, "TextToSpeech not initialized", Toast.LENGTH_SHORT).show();
        }
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
