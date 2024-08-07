package org.linccy.graffiti_sample;

import android.os.Bundle;

import com.tencent.mmkv.MMKV;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private GraffitiFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MMKV.initialize(getApplicationContext());

        setContentView(R.layout.activity_main);
        fragment = new GraffitiFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.content, fragment).commit();
    }
}
