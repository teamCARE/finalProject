package com.keenresearch.keenasr_android_poc;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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


import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements KASRRecognizerListener {
    protected static final String TAG =MainActivity.class.getSimpleName();
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;
    private TimerTask levelUpdateTask;
    private Timer levelUpdateTimer;

    private ASyncASRInitializerTask asyncASRInitializerTask;
    public static MainActivity instance;
    private Boolean micPermissionGranted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

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
                TextView resultText = (TextView)findViewById(R.id.resultText);
                //commented out below so can see history
                //resultText.setText("");
                recognizer.startListening();
            }
        });


    }

    public void onPartialResult(KASRRecognizer recognizer, final KASRResult result) {
        Log.i(TAG, "   Partial result: " + result.getCleanText());

        final TextView resultText = (TextView)findViewById(R.id.resultText);
        //resultText.setText(text);
        resultText.post(new Runnable() {
            @Override
            public void run() {
                resultText.setTextColor(Color.LTGRAY);
                //FIX AND UNCOMMENT THIS OUT LATER
                resultText.setText(result.getCleanText());
                //resultText.append(result.getCleanText());
            }
        });
    }

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
                if (result.getConfidence() > 0.8)
                    resultText.setTextColor(Color.GRAY);
                else
                    resultText.setTextColor(Color.argb(90, 200, 0, 0));

                resultText.setText(result.getCleanText());
                //resultText.append("  ");
                //resultText.append(result.getCleanText());
                startButton.setEnabled(true);
                //MAKES IT CONTINUOUS
                startButton.performClick();
            }
        });
        if (!status) {
            Log.w(TAG, "Unable to post runnable to the UI queue");
        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

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
    }

    //Handling callback
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
            String[] phrases = MainActivity.getPhrases();

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
            } else {
                Log.e(TAG, "Recognizer wasn't initialized properly");
            }
        }
    }

    private static String[] getPhrases() {
        /*String[] sentences = {
                "I don't know",
                "yes",
                "no",
                "I love you",
                "I hate you",
                "how are you",
                "I am good",
                "I'm good",
                "I feel good",
                "I don't feel good",
                "I'm sick",
                "I am sick",
                "What's up",
                "How are things",
                "How is life",
                "How's life",
                "Let's go",
                "Let's dance",
                "zero",
                "one",
                "two",
                "three",
                "four",
                "five",
                "six",
                "seven",
                "eight",
                "nine",
                "ten"
        };*/
        String[] sentences = {
                "hello",
                "world",
                "goodbye",
                "bye",
                "quick",
                "brown",
                "fox",
                "jump",
                "jumped",
                "over",
                "lazy",
                "how are you",
                "I'm doing well",
                "I'm sick",
                "where are you going",
                "going",
                "joining",
                "yesterday",
                "movie",
                "movies",
                "did",
                "go",
                "is",
                "was",

                "a",
                "ability",
                "able",
                "about",
                "above",
                "accept",
                "according",
                "account",
                "across",
                "act",
                "action",
                "activity",
                "actually",
                "add",
                "address",
                "administration",
                "admit",
                "adult",
                "affect",
                "after",
                "again",
                "against",
                "age",
                "agency",
                "agent",
                "ago",
                "agree",
                "agreement",
                "ahead",
                "air",
                "all",
                "allow",
                "almost",
                "alone",
                "along",
                "already",
                "also",
                "although",
                "always",
                "American",
                "among",
                "amount",
                "analysis",
                "and",
                "animal",
                "another",
                "answer",
                "any",
                "anyone",
                "anything",
                "appear",
                "apply",
                "approach",
                "area",
                "argue",
                "arm",
                "around",
                "arrive",
                "art",
                "article",
                "artist",
                "as",
                "ask",
                "assume",
                "at",
                "attack",
                "attention",
                "attorney",
                "audience",
                "author",
                "authority",
                "available",
                "avoid",
                "away",
                "baby",
                "back",
                "bad",
                "bag",
                "ball",
                "bank",
                "bar",
                "base",
                "be",
                "beat",
                "beautiful",
                "because",
                "become",
                "bed",
                "before",
                "begin",
                "behavior",
                "behind",
                "believe",
                "benefit",
                "best",
                "better",
                "between",
                "beyond",
                "big",
                "bill",
                "billion",
                "bit",
                "black",
                "blood",
                "blue",
                "board",
                "body",
                "book",
                "born",
                "both",
                "box",
                "boy",
                "break",
                "bring",
                "brother",
                "budget",
                "build",
                "building",
                "business",
                "but",
                "buy",
                "by",
                "call",
                "camera",
                "campaign",
                "can",
                "cancer",
                "candidate",
                "capital",
                "car",
                "card",
                "care",
                "career",
                "carry",
                "case",
                "catch",
                "cause",
                "cell",
                "center",
                "central",
                "century",
                "certain",
                "certainly",
                "chair",
                "challenge",
                "chance",
                "change",
                "character",
                "charge",
                "check",
                "child",
                "choice",
                "choose",
                "church",
                "citizen",
                "city",
                "civil",
                "claim",
                "class",
                "clear",
                "clearly",
                "close",
                "coach",
                "cold",
                "collection",
                "college",
                "color",
                "come",
                "commercial",
                "common",
                "community",
                "company",
                "compare",
                "computer",
                "concern",
                "condition",
                "conference",
                "Congress",
                "consider",
                "consumer",
                "contain",
                "continue",
                "control",
                "cost",
                "could",
                "country",
                "couple",
                "course",
                "court",
                "cover",
                "create",
                "crime",
                "cultural",
                "culture",
                "cup",
                "current",
                "customer",
                "cut",
                "dark",
                "data",
                "daughter",
                "day",
                "dead",
                "deal",
                "death",
                "debate",
                "decade",
                "decide",
                "decision",
                "deep",
                "defense",
                "degree",
                "Democrat",
                "democratic",
                "describe",
                "design",
                "despite",
                "detail",
                "determine",
                "develop",
                "development",
                "die",
                "difference",
                "different",
                "difficult",
                "dinner",
                "direction",
                "director",
                "discover",
                "discuss",
                "discussion",
                "disease",
                "do",
                "doctor",
                "dog",
                "door",
                "down",
                "draw",
                "dream",
                "drive",
                "drop",
                "drug",
                "during",
                "each",
                "early",
                "east",
                "easy",
                "eat",
                "economic",
                "economy",
                "edge",
                "education",
                "effect",
                "effort",
                "eight",
                "either",
                "election",
                "else",
                "employee",
                "end",
                "energy",
                "enjoy",
                "enough",
                "enter",
                "entire",
                "environment",
                "environmental",
                "especially",
                "establish",
                "even",
                "evening",
                "event",
                "ever",
                "every",
                "everybody",
                "everyone",
                "everything",
                "evidence",
                "exactly",
                "example",
                "executive",
                "exist",
                "expect",
                "experience",
                "expert",
                "explain",
                "eye",
                "face",
                "fact",
                "factor",
                "fail",
                "fall",
                "family",
                "far",
                "fast",
                "father",
                "fear",
                "federal",
                "feel",
                "feeling",
                "few",
                "field",
                "fight",
                "figure",
                "fill",
                "film",
                "final",
                "finally",
                "financial",
                "find",
                "fine",
                "finger",
                "finish",
                "fire",
                "firm",
                "first",
                "fish",
                "five",
                "floor",
                "fly",
                "focus",
                "follow",
                "food",
                "foot",
                "for",
                "force",
                "foreign",
                "forget",
                "form",
                "former",
                "forward",
                "four",
                "free",
                "friend",
                "from",
                "front",
                "full",
                "fund",
                "future",
                "game",
                "garden",
                "gas",
                "general",
                "generation",
                "get",
                "girl",
                "give",
                "glass",
                "go",
                "goal",
                "good",
                "government",
                "great",
                "green",
                "ground",
                "group",
                "grow",
                "growth",
                "guess",
                "gun",
                "guy",
                "hair",
                "half",
                "hand",
                "hang",
                "happen",
                "happy",
                "hard",
                "have",
                "he",
                "head",
                "health",
                "hear",
                "heart",
                "heat",
                "heavy",
                "help",
                "her",
                "here",
                "herself",
                "high",
                "him",
                "himself",
                "his",
                "history",
                "hit",
                "hold",
                "home",
                "hope",
                "hospital",
                "hot",
                "hotel",
                "hour",
                "house",
                "how",
                "however",
                "huge",
                "human",
                "hundred",
                "husband",
                "I",
                "idea",
                "identify",
                "if",
                "image",
                "imagine",
                "impact",
                "important",
                "improve",
                "in",
                "include",
                "including",
                "increase",
                "indeed",
                "indicate",
                "individual",
                "industry",
                "information",
                "inside",
                "instead",
                "institution",
                "interest",
                "interesting",
                "international",
                "interview",
                "into",
                "investment",
                "involve",
                "issue",
                "it",
                "item",
                "its",
                "itself",
                "job",
                "join",
                "just",
                "keep",
                "key",
                "kid",
                "kill",
                "kind",
                "kitchen",
                "know",
                "knowledge",
                "land",
                "language",
                "large",
                "last",
                "late",
                "later",
                "laugh",
                "law",
                "lawyer",
                "lay",
                "lead",
                "leader",
                "learn",
                "least",
                "leave",
                "left",
                "leg",
                "legal",
                "less",
                "let",
                "letter",
                "level",
                "lie",
                "life",
                "light",
                "like",
                "likely",
                "line",
                "list",
                "listen",
                "little",
                "live",
                "local",
                "long",
                "look",
                "lose",
                "loss",
                "lot",
                "love",
                "low",
                "machine",
                "magazine",
                "main",
                "maintain",
                "major",
                "majority",
                "make",
                "man",
                "manage",
                "management",
                "manager",
                "many",
                "market",
                "marriage",
                "material",
                "matter",
                "may",
                "maybe",
                "me",
                "mean",
                "measure",
                "media",
                "medical",
                "meet",
                "meeting",
                "member",
                "memory",
                "mention",
                "message",
                "method",
                "middle",
                "might",
                "military",
                "million",
                "mind",
                "minute",
                "miss",
                "mission",
                "model",
                "modern",
                "moment",
                "money",
                "month",
                "more",
                "morning",
                "most",
                "mother",
                "mouth",
                "move",
                "movement",
                "movie",
                "Mr",
                "Mrs",
                "much",
                "music",
                "must",
                "my",
                "myself",
                "name",
                "nation",
                "national",
                "natural",
                "nature",
                "near",
                "nearly",
                "necessary",
                "need",
                "network",
                "never",
                "new",
                "news",
                "newspaper",
                "next",
                "nice",
                "night",
                "no",
                "none",
                "nor",
                "north",
                "not",
                "note",
                "nothing",
                "notice",
                "now",
                "n't",
                "number",
                "occur",
                "of",
                "off",
                "offer",
                "office",
                "officer",
                "official",
                "often",
                "oh",
                "oil",
                "ok",
                "old",
                "on",
                "once",
                "one",
                "only",
                "onto",
                "open",
                "operation",
                "opportunity",
                "option",
                "or",
                "order",
                "organization",
                "other",
                "others",
                "our",
                "out",
                "outside",
                "over",
                "own",
                "owner",
                "page",
                "pain",
                "painting",
                "paper",
                "parent",
                "part",
                "participant",
                "particular",
                "particularly",
                "partner",
                "party",
                "pass",
                "past",
                "patient",
                "pattern",
                "pay",
                "peace",
                "people",
                "per",
                "perform",
                "performance",
                "perhaps",
                "period",
                "person",
                "personal",
                "phone",
                "physical",
                "pick",
                "picture",
                "piece",
                "place",
                "plan",
                "plant",
                "play",
                "player",
                "PM",
                "point",
                "police",
                "policy",
                "political",
                "politics",
                "poor",
                "popular",
                "population",
                "position",
                "positive",
                "possible",
                "power",
                "practice",
                "prepare",
                "present",
                "president",
                "pressure",
                "pretty",
                "prevent",
                "price",
                "private",
                "probably",
                "problem",
                "process",
                "produce",
                "product",
                "production",
                "professional",
                "professor",
                "program",
                "project",
                "property",
                "protect",
                "prove",
                "provide",
                "public",
                "pull",
                "purpose",
                "push",
                "put",
                "quality",
                "question",
                "quickly",
                "quite",
                "race",
                "radio",
                "raise",
                "range",
                "rate",
                "rather",
                "reach",
                "read",
                "ready",
                "real",
                "reality",
                "realize",
                "really",
                "reason",
                "receive",
                "recent",
                "recently",
                "recognize",
                "record",
                "red",
                "reduce",
                "reflect",
                "region",
                "relate",
                "relationship",
                "religious",
                "remain",
                "remember",
                "remove",
                "report",
                "represent",
                "Republican",
                "require",
                "research",
                "resource",
                "respond",
                "response",
                "responsibility",
                "rest",
                "result",
                "return",
                "reveal",
                "rich",
                "right",
                "rise",
                "risk",
                "road",
                "rock",
                "role",
                "room",
                "rule",
                "run",
                "safe",
                "same",
                "save",
                "say",
                "scene",
                "school",
                "science",
                "scientist",
                "score",
                "sea",
                "season",
                "seat",
                "second",
                "section",
                "security",
                "see",
                "seek",
                "seem",
                "sell",
                "send",
                "senior",
                "sense",
                "series",
                "serious",
                "serve",
                "service",
                "set",
                "seven",
                "several",
                "sex",
                "sexual",
                "shake",
                "share",
                "she",
                "shoot",
                "short",
                "shot",
                "should",
                "shoulder",
                "show",
                "side",
                "sign",
                "significant",
                "similar",
                "simple",
                "simply",
                "since",
                "sing",
                "single",
                "sister",
                "sit",
                "site",
                "situation",
                "six",
                "size",
                "skill",
                "skin",
                "small",
                "smile",
                "so",
                "social",
                "society",
                "soldier",
                "some",
                "somebody",
                "someone",
                "something",
                "sometimes",
                "son",
                "song",
                "soon",
                "sort",
                "sound",
                "source",
                "south",
                "southern",
                "space",
                "speak",
                "special",
                "specific",
                "speech",
                "spend",
                "sport",
                "spring",
                "staff",
                "stage",
                "stand",
                "standard",
                "star",
                "start",
                "state",
                "statement",
                "station",
                "stay",
                "step",
                "still",
                "stock",
                "stop",
                "store",
                "story",
                "strategy",
                "street",
                "strong",
                "structure",
                "student",
                "study",
                "stuff",
                "style",
                "subject",
                "success",
                "successful",
                "such",
                "suddenly",
                "suffer",
                "suggest",
                "summer",
                "support",
                "sure",
                "surface",
                "system",
                "table",
                "take",
                "talk",
                "task",
                "tax",
                "teach",
                "teacher",
                "team",
                "technology",
                "television",
                "tell",
                "ten",
                "tend",
                "term",
                "test",
                "than",
                "thank",
                "that",
                "the",
                "their",
                "them",
                "themselves",
                "then",
                "theory",
                "there",
                "these",
                "they",
                "thing",
                "think",
                "third",
                "this",
                "those",
                "though",
                "thought",
                "thousand",
                "threat",
                "three",
                "through",
                "throughout",
                "throw",
                "thus",
                "time",
                "to",
                "today",
                "together",
                "tonight",
                "too",
                "top",
                "total",
                "tough",
                "toward",
                "town",
                "trade",
                "traditional",
                "training",
                "travel",
                "treat",
                "treatment",
                "tree",
                "trial",
                "trip",
                "trouble",
                "true",
                "truth",
                "try",
                "turn",
                "TV",
                "two",
                "type",
                "under",
                "understand",
                "unit",
                "until",
                "up",
                "upon",
                "us",
                "use",
                "usually",
                "value",
                "various",
                "very",
                "victim",
                "view",
                "violence",
                "visit",
                "voice",
                "vote",
                "wait",
                "walk",
                "wall",
                "want",
                "war",
                "watch",
                "water",
                "way",
                "we",
                "weapon",
                "wear",
                "week",
                "weight",
                "well",
                "west",
                "western",
                "what",
                "whatever",
                "when",
                "where",
                "whether",
                "which",
                "while",
                "white",
                "who",
                "whole",
                "whom",
                "whose",
                "why",
                "wide",
                "wife",
                "will",
                "win",
                "wind",
                "window",
                "wish",
                "with",
                "within",
                "without",
                "woman",
                "wonder",
                "word",
                "work",
                "worker",
                "world",
                "worry",
                "would",
                "write",
                "writer",
                "wrong",
                "yard",
                "yeah",
                "year",
                "yes",
                "yet",
                "you",
                "young",
                "your",
                "yourself"
        };
        return sentences;
    }
}


