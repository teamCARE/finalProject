/*
 * This work contains files distributed in Android, such files Copyright (C) 2016 The Android Open Source Project
 *
 * and are Licensed under the Apache License, Version 2.0 (the "License"); you may not use these files except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */


package sdk.everysight.examples.audio;

import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.everysight.base.EvsBaseActivity;


/**
 * Audio Activity for demonstrating basic audio record and play capabilities
 * - swipe forward and say a word, it will repeat it via the speaker
 */
public class AudioActivity extends EvsBaseActivity
{
    private static final String TAG = "AudioExample";
    private TextView centerLabel;
    private boolean recording = false;
    private boolean playback = false;
    final AudioProcessor audio = new AudioProcessor();

    /******************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout);

        centerLabel = (TextView) findViewById(R.id.centerLable);
    }

    /******************************************************************/
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    /******************************************************************/
    @Override
    public void onResume()
    {
        super.onResume();
    }

    /******************************************************************/
    @Override
    public void onPause()
    {
        super.onPause();
    }

    /******************************************************************/
    @Override
    public void onTap()
    {
        super.onTap();
        if (playback) {
            return;
        }
        if (recording) {
            audio.stopRec();
            centerLabel.setText("Done recording, swipe forward to playback");
        }
        else {
            audio.start();
            centerLabel.setText("Recording, tap to stop");
        }
        recording = !recording;
    }

    /******************************************************************/
    @Override
    public void onUp()
    {
        super.onUp();
    }

    /******************************************************************/
    @Override
    public void onDown()
    {
        finish();
        super.onDown();
    }

    /******************************************************************/
    @Override
    public void onForward()
    {
        super.onForward();
        if (!recording) {
            centerLabel.setText("Playback");
            playback = true;
            int delay = audio.play();
            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    AudioActivity.this.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            audio.close();
                            centerLabel.setText(R.string.welcome);
                            playback = false;
                        }
                    });
                }
            }, delay); //change delay to length of audio recording
        }

    }

    /******************************************************************/
    @Override
    public void onBackward()
    {
        super.onBackward();
    }
}
