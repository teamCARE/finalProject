package com.example.android.speechapplication;

import android.speech.RecognitionListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.maxwell.speechrecognition.OnSpeechRecognition;
import com.maxwell.speechrecognition.OnSpeechRecognitionListener;
import com.maxwell.speechrecognition.OnSpeechRecognitionPermissionListener;
import com.maxwell.speechrecognition.SpeechRecognition;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements OnSpeechRecognitionListener, OnSpeechRecognitionPermissionListener {

    private TextView results;
    private Button speakButton; private TextView volumeText;

    private static final String TAG = "MainActivity";
    private String history;
    private  SpeechRecognition speechRecognition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set up layout
        results = (TextView) findViewById(R.id.resultsText);
        volumeText = (TextView) findViewById(R.id.volume);

        //set up recognition
        speechRecognition = new SpeechRecognition(this);
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
    public void OnSpeechRecognitionFinalResult(String s, float[] confidence) {
        //triggered when SpeechRecognition is done listening.
        //it returns the translated text from the voice input
        //history = history + s;     //for later
        //results.setText(history);  //for later
        if (confidence != null){
            if (confidence.length > 0){
                Log.i(TAG + " confidence", String.valueOf(confidence[0]));
            } else {
                Log.i(TAG + " confidence score not available", "unknown confidence");
            }
        } else {
            Log.i(TAG, "confidence not found");
        }

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
    public void OnSpeechRecognitionError(int i, String s) {
        speechRecognition.stopSpeechRecognition();
        speechRecognition.startSpeechRecognition();
    }


    @Override
    public void OnSpeechRecognitionRmsChanged(float rmsChangedValue) {
        //Log.i(TAG, "Rms change value = " + rmsChangedValue);
        volumeText.setText("rmsdB: " + rmsChangedValue);
    }

}