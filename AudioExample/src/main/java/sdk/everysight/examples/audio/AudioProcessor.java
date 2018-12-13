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
    private boolean started = false;
    private boolean playback = false;
    private boolean end = false;
    private boolean killed = false;
    private int shorts = 18285; //number of shorts per second of audio
    private int seconds = 4;

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
        int res, offset;
        int sampleRate = 16000;
        int selectedMic = MediaRecorder.AudioSource.CAMCORDER; // front microphone
        //MediaRecorder.AudioSource.DEFAULT // inner microphone
        int N = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(selectedMic, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, N * 10);
        track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, N * 10, AudioTrack.MODE_STREAM);


        try
        {
            while(!killed) {
                started = false;
                stopped =false;
                playback = false;
                end = false;
                buffer = new short[seconds*shorts];

                while (!started) {
                }
                recorder.startRecording();
                Log.i("audio", "recording");

                offset = 0;
                while (!stopped && offset < buffer.length) {
                    res = recorder.read(buffer, offset, N);
                    if (res < 0) {
                        break;
                    }
                    offset += res;
                }
                Log.i("audio", "done recording");
                recorder.stop();
                while (!playback) {
                }
                track.play();
                offset = 0;
                int s;
                while (!end) {
                    if (offset < buffer.length) {
                        s = track.write(buffer, offset, buffer.length);
                        if (s <= 0) {
                            Log.i("audio", "break");
                            break;
                        }
                        offset += s;
                    }
                }
                track.stop();
            }
        }
        catch (Throwable x)
        {
            Log.w("Audio", "Error reading voice audio", x);
        }
        finally
        {
            recorder.release();
            track.release();
        }
    }

    /**
     * Called from outside of the thread in order to progress run function
     */
    public void startRec() {
        started = true;
    }

    public void stopRec() {
        stopped = true;
    }

    public int play() {
        playback = true;
        return 1000*seconds;
    }

    public void close() {
        end = true;
    }

    public void kill() {
        killed = true;
    }
}