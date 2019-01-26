package com.example.android.caredemo;

import android.app.Activity;
import android.os.Bundle;

import com.everysight.base.EvsBaseActivity;
import com.everysight.notifications.EvsToast;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.Color;
import android.widget.Toast;

import com.keenresearch.keenasr.KASRDecodingGraph;
import com.keenresearch.keenasr.KASRRecognizer;
import com.keenresearch.keenasr.KASRResult;
import com.keenresearch.keenasr.KASRRecognizerListener;
import com.keenresearch.keenasr.KASRBundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends EvsBaseActivity implements KASRRecognizerListener{
    protected static final String TAG =MainActivity.class.getSimpleName();
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;
    private TimerTask levelUpdateTask;
    private Timer levelUpdateTimer;

    private ASyncASRInitializerTask asyncASRInitializerTask;
    public static MainActivity instance;
    private Boolean micPermissionGranted = false;
    private int len_final = 0;
    private SpannableStringBuilder ssbuilder = new SpannableStringBuilder();
    private boolean IS_RECORDING = false;
    private boolean ready = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // disable start button until initialization is completed
        final Button startButton = (Button)findViewById(R.id.startListening);
        startButton.setEnabled(false);

        // we need to make sure audio permission is granted before initializing KeenASR SDK
        requestAudioPermissions();

        if (KASRRecognizer.sharedInstance() == null) {
            Log.i(TAG, "Initializing KeenASR recognizer");
            KASRRecognizer.setLogLevel(KASRRecognizer.KASRRecognizerLogLevel.KASRRecognizerLogLevelDebug);
            Context context = this.getApplication().getApplicationContext();
            asyncASRInitializerTask = new ASyncASRInitializerTask(context);
            asyncASRInitializerTask.execute();
        } else {
            startButton.setEnabled(true);
            //MAKES IT CONTINUOUS
            startButton.performClick();
            IS_RECORDING = true;

            final TextView resultText = (TextView)findViewById(R.id.resultText);
            resultText.setTextColor(Color.GREEN);
            resultText.setText("Ready to Start!");

        }

        MainActivity.instance = this;

        ((Button) findViewById(R.id.startListening)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                Log.i(TAG, "Starting to listen...");
                final KASRRecognizer recognizer = KASRRecognizer.sharedInstance();

                levelUpdateTimer = new Timer();
                levelUpdateTask = new TimerTask() {
                    public void run() {
//                        Log.i(TAG, "     " + recognizer.getInputLevel());
                    }
                };
                levelUpdateTimer.schedule(levelUpdateTask, 0, 80); // ~12 updates/sec

                view.setEnabled(false);
                //TextView resultText = (TextView)findViewById(R.id.resultText);
                //commented out below so can see history
                //resultText.setText("");
                recognizer.startListening();
            }
        });

        //replace this play/pause button with swipe command when integrating with Raptor headset
        ((Button) findViewById(R.id.pauseRecognition)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                Log.i(TAG, "Pause listen...");
                if (IS_RECORDING == true) {
                    final KASRRecognizer recognizer = KASRRecognizer.sharedInstance();
                    recognizer.stopListening();
                    IS_RECORDING = false;
                    //indicate to user
                    final TextView resultText = (TextView)findViewById(R.id.resultText);
                    resultText.setTextColor(Color.RED);
                    resultText.setText("Recognition Paused");
                }
                else{
                    startButton.setEnabled(true);
                    startButton.performClick();
                    IS_RECORDING = true;
                    //indicate to user
                    final TextView resultText = (TextView)findViewById(R.id.resultText);
                    resultText.setTextColor(Color.GREEN);
                    resultText.setText("Recognition Play");
                    ready = true;
                }
            }
        });

    } //end of OnCreate

    //Everysight stuff
    /******************************************************************/
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume : We are running again!");
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }


    @Override
    public void onTap() {
        super.onTap();
        if (!ready) {return;}
        final Button pauseButton = (Button)findViewById(R.id.pauseRecognition);
        pauseButton.performClick();

    }
    /******************************************************************/

    public void onPartialResult(KASRRecognizer recognizer, final KASRResult result) {
        Log.i(TAG, "   Partial result: " + result.getCleanText());

        final TextView resultText = (TextView)findViewById(R.id.resultText);
        //resultText.setText(text); //commented out in original Keen source
        resultText.post(new Runnable() {
            @Override
            public void run() {
                //method 1 (original)
                // resultText.setTextColor(Color.LTGRAY);
                // resultText.setText(result.getCleanText());

                //method 2
                String resPar = result.getCleanText();
                if (resPar.length()!=0){
                    resPar = "\n" + resPar;
                }
                SpannableString resParSpanable= new SpannableString(resPar);
                resParSpanable.setSpan(new ForegroundColorSpan(Color.LTGRAY), 0, resParSpanable.length(), 0);

                ssbuilder.delete(len_final, ssbuilder.length());

                ssbuilder.append(resParSpanable);

                resultText.setText(ssbuilder, TextView.BufferType.SPANNABLE); //BufferType SPANNABLE automatically has scrolling movement for a textview
            }
        });
    } //end of onPartialResult

    public void onFinalResult(KASRRecognizer recognizer, final KASRResult result) {
        Log.i(TAG, "Final result: " + result);
        Log.i(TAG, "Final result JSON: " + result.toJSON());

        final TextView resultText = (TextView)findViewById(R.id.resultText);
        final Button startButton = (Button)findViewById(R.id.startListening);
        Log.i(TAG, "resultText: " + resultText);
        if (levelUpdateTimer!=null)
            levelUpdateTimer.cancel();

        Log.i(TAG, "audioFile is in " + recognizer.getLastRecordingFilename());

        boolean status = resultText.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Updating UI after receiving final result");

                ssbuilder.delete(len_final, ssbuilder.length()); //erases any partial result
                String resFin = result.getCleanText();
                if (resFin.length()!=0){
                    resFin = "\n" + resFin;
                }
                len_final = resFin.length() + len_final;
                SpannableString resFinSpanable= new SpannableString(resFin);

                if (result.getConfidence() > 0.8) {
                    //resultText.setTextColor(Color.GRAY);
                    resFinSpanable.setSpan(new ForegroundColorSpan(Color.BLACK), 0, resFinSpanable.length(), 0);
                    ssbuilder.append(resFinSpanable);
                }
                else {
                    //resultText.setTextColor(Color.argb(90, 200, 0, 0));
                    resFinSpanable.setSpan(new ForegroundColorSpan(Color.argb(90, 200, 0, 0)), 0, resFinSpanable.length(), 0);
                    ssbuilder.append(resFinSpanable);
                }

                //resultText.setText(result.getCleanText());
                resultText.setText(ssbuilder, TextView.BufferType.SPANNABLE);

                //keep ssbuilder length from growing indefinitely
                //waits for a few lines (100 chars) to cut to avoid cutting every new result
                if (ssbuilder.length()>500){
                    ssbuilder.delete(0,100);
                    len_final =  len_final-100;
                    //resultText.append("cutText"); //for debug purposes
                }

                startButton.setEnabled(true);
                //MAKES IT CONTINUOUS
                startButton.performClick();
            }
        });
        if (!status) {
            Log.w(TAG, "Unable to post runnable to the UI queue");
        }

    } //end of onFinalResult


    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Log.i(TAG, "Requesting mic permission from the users");
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();
                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
                Log.i(TAG, "Requesting mic permission from the users");
            }
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Microphone permission has already been granted");
            micPermissionGranted = true;
        }
    } //end of requestAudioPermissions

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    micPermissionGranted = true;
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permissions Denied to record audio. You will have to allow microphone access from the Settings->App->KeenASR->Permissions'", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private class ASyncASRInitializerTask extends AsyncTask<String, Integer, Long> {
        private Context context;

        public ASyncASRInitializerTask(Context context) {
            this.context = context;
        }

        protected Long doInBackground(String... params) {
            Log.i(TAG, "Installing ASR Bundle");
            KASRBundle asrBundle = new KASRBundle(this.context);
            ArrayList<String> assets = new ArrayList<String>();

            assets.add("keenB2mQT-nnet3chain-en-us/decode.conf");
            assets.add("keenB2mQT-nnet3chain-en-us/final.dubm");
            assets.add("keenB2mQT-nnet3chain-en-us/final.ie");
            assets.add("keenB2mQT-nnet3chain-en-us/final.mat");
            assets.add("keenB2mQT-nnet3chain-en-us/final.mdl");
            assets.add("keenB2mQT-nnet3chain-en-us/global_cmvn.stats");
            assets.add("keenB2mQT-nnet3chain-en-us/ivector_extractor.conf");
            assets.add("keenB2mQT-nnet3chain-en-us/mfcc.conf");
            assets.add("keenB2mQT-nnet3chain-en-us/online_cmvn.conf");
            assets.add("keenB2mQT-nnet3chain-en-us/splice.conf");
            assets.add("keenB2mQT-nnet3chain-en-us/splice_opts");
            assets.add("keenB2mQT-nnet3chain-en-us/wordBoundaries.int");
            assets.add("keenB2mQT-nnet3chain-en-us/words.txt");
            assets.add("keenB2mQT-nnet3chain-en-us/lang/lexicon.txt");
            assets.add("keenB2mQT-nnet3chain-en-us/lang/phones.txt");
            assets.add("keenB2mQT-nnet3chain-en-us/lang/tree");


            String asrBundleRootPath = getApplicationInfo().dataDir;
            String asrBundlePath = new String(asrBundleRootPath + "/keenB2mQT-nnet3chain-en-us");

            try {
                asrBundle.installASRBundle(assets, asrBundleRootPath);
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when installing ASR bundle" + e);
                return 0l;
            }
            Log.i(TAG, "Waiting for microphone permission to be granted");
            while (!micPermissionGranted) {
                ;
                // TODO should handle the situation where user denied to grant access
                // so we can return without initailizing the SD
            }
            Log.i(TAG, "Microphone permission is granted");
            Log.i(TAG, "Initializing with bundle at path: " + asrBundlePath);
            KASRRecognizer.initWithASRBundleAtPath(asrBundlePath, getApplicationContext());
            String[] phrases = MainActivity.getPhrases(context);

            KASRRecognizer recognizer = KASRRecognizer.sharedInstance();
            if (recognizer != null) {
                String dgName = "words";
                // we don't have to recreate the decoding graph every time, but during the development
                // this could be a problem if the list of sentences/phrases is changed (decoding graph
                // would not be re-created), so we opt to create it every time
//                if (KASRDecodingGraph.decodingGraphWithNameExists(dgName, recognizer)) {
//                    Log.i(TAG, "Decoding graph " + dgName + " alread exists. IT WON'T BE RECREATED");
//                    Log.i(TAG, "Created on " + KASRDecodingGraph.getDecodingGraphCreationDate(dgName, recognizer));
//                } else {
//                    KASRDecodingGraph.createDecodingGraphFromSentences(phrases, recognizer, dgName); //
//                }
                KASRDecodingGraph.createDecodingGraphFromSentences(phrases, recognizer, dgName); // TODO check return code

                recognizer.prepareForListeningWithCustomDecodingGraphWithName(dgName);

            } else {
                Log.e(TAG, "Unable to retrieve recognizer");
            }
            return 1l;
        }

        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        protected void onPostExecute(Long result) {
            super.onPostExecute(result);
            Log.i(TAG, "Initialized KeenASR in the background");
            KASRRecognizer recognizer = KASRRecognizer.sharedInstance();
            if (recognizer!=null) {
                Log.i(TAG, "Adding listener");
                recognizer.addListener(MainActivity.instance);
                recognizer.setVADParameter(KASRRecognizer.KASRVadParameter.KASRVadTimeoutEndSilenceForGoodMatch, 1.0f);
                recognizer.setVADParameter(KASRRecognizer.KASRVadParameter.KASRVadTimeoutEndSilenceForAnyMatch, 1.0f);
                recognizer.setVADParameter(KASRRecognizer.KASRVadParameter.KASRVadTimeoutMaxDuration, 100.0f);
                recognizer.setVADParameter(KASRRecognizer.KASRVadParameter.KASRVadTimeoutForNoSpeech, 5.0f);

                //recognizer.setCreateAudioRecordings(true);

                final Button startButton = (Button) findViewById(R.id.startListening);
                startButton.setEnabled(true);
                //MAKES IT CONTINUOUS
                startButton.performClick();
                IS_RECORDING = true;

                final TextView resultText = (TextView)findViewById(R.id.resultText);
                resultText.setTextColor(Color.GREEN);
                resultText.setText("Ready to Start!");
                ready = true;
            } else {
                Log.e(TAG, "Recognizer wasn't initialized properly");
            }
        }

    } //end of ASyncASRInitializerTask

    private static String[] getPhrases(Context context) {

        List<String> data = new ArrayList<String>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("engwords.txt")));
            String s;
            while ((s = reader.readLine()) != null) {
                data.add(s);
                System.out.println(s);
            }
            reader.close();
        }
        catch (IOException e) {
            System.out.println("Couldn't Read File Correctly");
        }

        String[] sentences = data.toArray(new String[]{});   //converts to a String[]
        return sentences;
    } //end of getPhrases


} //end of MainActivity
