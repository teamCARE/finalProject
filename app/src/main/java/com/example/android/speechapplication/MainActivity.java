package com.example.android.speechapplication;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.maxwell.speechrecognition.OnSpeechRecognitionListener;
import com.maxwell.speechrecognition.OnSpeechRecognitionPermissionListener;
import com.maxwell.speechrecognition.SpeechRecognition;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements OnSpeechRecognitionListener, OnSpeechRecognitionPermissionListener {

    private TextView resultsText;
    private Button speakButton;
    private TextView volumeText;
    private SpannableStringBuilder ssbuilder = new SpannableStringBuilder();
    private int len_final = 0;
    private boolean AR_GLASS_MODE = true;
    private BluetoothAR bluetoothAR = new BluetoothAR();

    private static final String TAG = "MainActivity";
    private  SpeechRecognition speechRecognition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //if ALREADY connected bluetooth mic already, use that as audio input source
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE); if (audioManager.isBluetoothScoAvailableOffCall()) {
            audioManager.setMode(AudioManager.MODE_IN_CALL); audioManager.startBluetoothSco(); audioManager.setBluetoothScoOn(true); try {
                Thread.sleep(3000); }catch (InterruptedException e) {
                Log.w(TAG, "Exception" + e); }
        }else {
            Log.w(TAG, "WARNING: BluetoothSco is not available"); }
        if (audioManager.isBluetoothScoOn()) {
            Log.i(TAG, "Bluetooth SCO is ON"); }else {
            Log.w(TAG, "Bluetooth SCO is OFF");
        }

        setContentView(R.layout.activity_main);

        //set up layout
        resultsText = (TextView) findViewById(R.id.resultsText);
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

        //below functionality is for if want to send result text up to an AR headset
        Switch onOffSwitch = (Switch)  findViewById(R.id.on_off_switch);
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //Log.i(TAG , "switchstate" + isChecked );
                if (isChecked)
                    AR_GLASS_MODE = true;
                else
                    AR_GLASS_MODE = false;
            }
        });

        ((Button) findViewById(R.id.BTsend)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Log.i(TAG, "click");
                short test = bluetoothAR.Bluetoothsetup(MainActivity.this.getApplicationContext());
                if (test == 1) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); //ask to turn on bluetooth if it's off
                    startActivityForResult(enableIntent, bluetoothAR.REQUEST_ENABLE_BT);      //done in MainActivity instead of BT class to avoid memory leaks
                    Toast.makeText(MainActivity.this,"Please allow BT, then try BTconnect button agian" , Toast.LENGTH_LONG).show();
                    resultsText.setText("Please allow BT, then try BTconnect button agian");
                }

                if (test == 2) {  //when user hits BTconnect button agian, will be enabled
                    bluetoothAR.ConnectBTDevice(TAG, MainActivity.this.getApplicationContext()); //connect to AR headset
                }
            }
        });
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
    public void OnSpeechRecognitionFinalResult(String result, float confidence) {
        //triggered when SpeechRecognition is done listening.
        //it returns the translated text from the voice input
        //results.setText(result);  //old, plain

        Log.i(TAG + " confidence final", String.valueOf(confidence));

        ssbuilder.delete(len_final, ssbuilder.length()); //erases any partial result
        if (result.length()!=0){
            result = "\n" + result;
        }
        len_final = result.length() + len_final;
        SpannableString resFinSpanable= new SpannableString(result);
        if (confidence > 0.7) {
            resFinSpanable.setSpan(new ForegroundColorSpan(Color.GREEN), 0, resFinSpanable.length(), 0);
            ssbuilder.append(resFinSpanable);
        } else {
            resFinSpanable.setSpan(new ForegroundColorSpan(Color.RED), 0, resFinSpanable.length(), 0);
            ssbuilder.append(resFinSpanable);
        }

        //keep ssbuilder length from growing indefinitely. Waits for a few lines (100 chars) to cut to avoid cutting every new result
        if (ssbuilder.length()> 1000){
            ssbuilder.delete(0, 100);
            len_final =  len_final-100;
            //Toast.makeText(MainActivity.this,"cut text" , Toast.LENGTH_SHORT).show();
        }


        resultsText.setText(ssbuilder, TextView.BufferType.SPANNABLE); //BufferType SPANNABLE automatically has scrolling movement for a textview

        //send the results to display on AR glasses
        if (AR_GLASS_MODE) {
            try {
                Thread.sleep(400);   //needed to fix hanging error on receiving end
                bluetoothAR.MssgWrite(ssbuilder);
                Log.i(TAG + "FINAL sent to glasses sucess: " , ssbuilder.toString());
            } catch(InterruptedException | IOException ex) {
                ex.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

        speakButton.performClick(); //makes it auto restart
    }

    @Override
    public void OnSpeechRecognitionCurrentResult(String resultCurrent) {
        //this is called multiple times when SpeechRecognition is
        //still listening. It returns each recognized word when the user is still speaking
        //results.setText(resultCurrent); //old, plain
        
        Log.i(TAG, "CurrentResult = " + resultCurrent);
        
        if (resultCurrent.length()!=0){
            resultCurrent = "\n" + resultCurrent;
        }
        SpannableString resParSpanable= new SpannableString(resultCurrent);
        resParSpanable.setSpan(new ForegroundColorSpan(Color.YELLOW), 0, resParSpanable.length(), 0);
        ssbuilder.delete(len_final, ssbuilder.length());
        ssbuilder.append(resParSpanable);

        //send the results to display on AR glasses
        if (AR_GLASS_MODE) {
            try {
                bluetoothAR.MssgWrite(ssbuilder);
                Log.i(TAG + "PARTIAL sent to glasses sucess: " , ssbuilder.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        resultsText.setText(ssbuilder, TextView.BufferType.SPANNABLE);
    }

    @Override
    public void OnSpeechRecognitionError(int i, String s) {
        speechRecognition.stopSpeechRecognition();
        speechRecognition.startSpeechRecognition();  //makes it auto restart
    }

    @Override
    public void OnSpeechRecognitionRmsChanged(float rmsChangedValue) {
        //Log.i(TAG, "Rms change value = " + rmsChangedValue);
        volumeText.setText("Volume(rmsdB): " + rmsChangedValue);
    }

}