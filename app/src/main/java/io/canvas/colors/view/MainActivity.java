package io.canvas.colors.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import io.canvas.colors.R;
import io.canvas.colors.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        //binding.onlineCircle.setVisibility(View.GONE);
        binding.changeDevice.setOnClickListener(view-> Toast.makeText(this, "Clicked!", Toast.LENGTH_SHORT).show());
    }
}