package com.example.android.speechapplication;

import android.speech.RecognitionListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.maxwell.speechrecognition.OnSpeechRecognitionListener;
import com.maxwell.speechrecognition.OnSpeechRecognitionPermissionListener;
import com.maxwell.speechrecognition.SpeechRecognition;

public class MainActivity extends AppCompatActivity implements OnSpeechRecognitionListener, OnSpeechRecognitionPermissionListener {

    private TextView results;
    private Button speakButton;
    private static final String TAG = "MainActivity";
    private String history;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        results = (TextView) findViewById(R.id.resultsText);

        final SpeechRecognition speechRecognition = new SpeechRecognition(this);
        speechRecognition.setSpeechRecognitionPermissionListener(this);
        speechRecognition.setSpeechRecognitionListener(this);

        speakButton = findViewById(R.id.Start);
        speakButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                speechRecognition.startSpeechRecognition();
            }
        });

        history = "";

    }


    @Override
    public void onPermissionGranted() {
        //RECORD_AUDIO permission was granted
    }

    @Override
    public void onPermissionDenied() {
        //RECORD_AUDIO permission was denied
    }

    @Override
    public void OnSpeechRecognitionStarted() {}

    @Override
    public void OnSpeechRecognitionStopped() {}

    @Override
    public void OnSpeechRecognitionFinalResult(String s) {
        //triggered when SpeechRecognition is done listening.
        //it returns the translated text from the voice input
        //history = history + s;     //for later
        //results.setText(history);  //for later
        results.setText(s);  //for later
        speakButton.performClick();
    }
    @Override
    public void OnSpeechRecognitionCurrentResult(String s) {
        //this is called multiple times when SpeechRecognition is
        //still listening. It returns each recognized word when the user is still speaking
        Log.i(TAG, "CurrentResult = " + s);
        results.setText(s);
    }

    @Override
    public void OnSpeechRecognitionError(int i, String s) {}


    @Override
    public void onSpeechRecognitionRmsChanged(float rmsChangedValue) {
        Log.i(TAG, "Rms change value = " + rmsChangedValue);
    }


}