package com.example.PrincessMemoryGame;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        findViewById(R.id.start_game).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        Intent entryIntent = new Intent(view.getContext(), GameActivity.class);
                        startActivity(entryIntent);
                    }
                });

    }
}