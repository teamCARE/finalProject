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
        AudioRecord recorder = null;
        AudioTrack track = null;
        short[] buffer;

        /*
         * Initialize buffer to hold continuously recorded audio data, start recording, and start
         * playback.
         */
        try
        {
            int sampleRate = 16000;
            int selectedMic = MediaRecorder.AudioSource.CAMCORDER; // front microphone
            //MediaRecorder.AudioSource.DEFAULT // inner microphone

            int N = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            buffer = new short[N];
            recorder = new AudioRecord(selectedMic, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, N * 10);
            track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, N * 10, AudioTrack.MODE_STREAM);
            recorder.startRecording();
//            track.play();
            /*
             * Loops until something outside of this thread stops it.
             * Reads the data from the recorder and writes it to the audio track for playback.
             */
            int res = 0;
            while (!stopped)
            {
                res = recorder.read(buffer, 0, buffer.length);
                if (res < 0)
                {
                    break;
                }
            }
            Log.i("Buffer", "Contents of buffer: %d", buffer);
            recorder.stop();
            recorder.release();
            while (!playback) {}
            track.play();
            while (!end) {
                if (res > 0) {
                    track.write(buffer, 0, res);
                }
            }
            track.stop();
            track.release();
        }
        catch (Throwable x)
        {
            Log.w("Audio", "Error reading voice audio", x);
        }
//        finally
//        {
//            recorder.stop();
//            recorder.release();
//            track.stop();
//            track.release();
//        }
    }

    /**
     * Called from outside of the thread in order to stop the recording/playback loop
     */
    public void stopRec() {
        stopped = true;
    }
    
    public int play() {
        playback = true;
        return 4000; //somehow make this audio length
    }
    
    public void close() {
        end = true;
    }
}