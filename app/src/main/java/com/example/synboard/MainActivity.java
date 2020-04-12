package com.example.synboard;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;

public class MainActivity extends AppCompatActivity {
    Button enter, reset;
    TextView red, score, usersAnswer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enter = findViewById(R.id.enter);
        reset = findViewById(R.id.reset);
        red = findViewById(R.id.fox);
        score = findViewById(R.id.missed);
        usersAnswer = findViewById(R.id.usersAnswer);

        final String theFox[] = ("The quick red fox jumped over the lazy brown dog.").split("");

        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user[] = (usersAnswer.getText().toString()).split("");

                int misses = 0;

                System.out.println(theFox + ": " +  theFox.length);
                System.out.println(user + " : " + user.length);
                for(int i = 0; i < theFox.length && i < user.length; i++) {
                    if(!theFox[i].toLowerCase().equals(user[i].toLowerCase())) {
                        System.out.println(theFox[i].toLowerCase() + " : " + user[i].toLowerCase());
                        misses++;
                    }
                }

                score.setText(Integer.toString(misses) + " characters missed \n" +
                        "The quick red fox jumped over the lazy brown dog.\n" + usersAnswer.getText().toString());
                usersAnswer.setText("");

            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                usersAnswer.setText("");
            }
        });
    }
}
