package com.example.yourable;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
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

    }
}