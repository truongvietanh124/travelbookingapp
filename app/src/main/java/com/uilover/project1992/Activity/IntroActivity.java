package com.uilover.project1992.Activity;

import android.content.Intent;
import android.os.Bundle;

import com.uilover.project1992.databinding.ActivityIntroBinding;

public class IntroActivity extends BaseActivity {
private ActivityIntroBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.startBtn.setOnClickListener(v -> startActivity(new Intent(IntroActivity.this, MainActivity.class)));

    }
}