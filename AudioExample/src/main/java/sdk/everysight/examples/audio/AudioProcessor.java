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

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;


public class AudioProcessor extends Thread
{
    private boolean stopped = false;
    private boolean playback = false;
    private boolean end = false;
    private boolean killed = false;
    private AudioRecord recorder = null;
    private AudioTrack track = null;
    private short[] buffer;
    private int N;

    /**
     * Give the thread high priority so that it's not canceled unexpectedly, and start it
     */
    public AudioProcessor()
    {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
    }

    @Override
    public void run()
    {
        Log.i("Audio", "Running Audio Thread");

        try
        {
            int sampleRate = 16000;
            int selectedMic = MediaRecorder.AudioSource.CAMCORDER; // front microphone
            //MediaRecorder.AudioSource.DEFAULT // inner microphone

            N = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

            recorder = new AudioRecord(selectedMic, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, N * 10);
            track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, N * 10, AudioTrack.MODE_STREAM);

            while(!killed) {}
        }
        catch (Throwable x)
        {
            Log.w("Audio", "Error running audio thread", x);
        }
    }

    public void startRec() {
        Log.i("audio","recording");
        try{
            buffer = new short[10000*N];
            recorder.startRecording();

            int res;
            int offset = 0;
            while (!stopped && offset<buffer.length)
            {
                res = recorder.read(buffer, offset, N);
                if (res < 0)
                {
                    break;
                }
                offset += res;
            }
        }
        catch (Throwable x)
        {
            Log.w("Audio", "Error recording voice audio", x);
        }
    }

    public void stopRec() {
        Log.i("Audio", "Stopping recording");
        try {
            stopped = true;
            recorder.stop();
            recorder.release();
            Log.i("audio", "done recording");
        }
        catch (Throwable x)
        {
            Log.w("Audio", "Error stopping recording", x);
        }
    }
    
    public void play() {
        Log.i("Audio", "Beginning playback");
        try {
            playback = true;
            int offset = 0;
            int s;
            track.play();
            while (!end) {
                if (offset < buffer.length) {
                    s = track.write(buffer, offset, buffer.length);
                    if (s <= 0) {
                        break;
                    }
                    offset += s;
                }
            }
            track.stop();
            track.release();
        }
        catch (Throwable x)
        {
            Log.w("Audio", "Error in playback", x);
        }
    }
    
    public void close() {
        end = true;
    }

    public void kill() {
        killed = true;
    }
}