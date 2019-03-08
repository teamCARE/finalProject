package com.example.android.bluetoothrx_keenasr;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import android.view.View.OnClickListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.lang.Runnable;
import java.lang.Thread;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final String NAME = "device_name";
    private static final String TAG = "MY_APP_DEBUG_TAG";

    private BluetoothAdapter mBluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;
    private AcceptThread AcceptThreadObj;
    private OutputStream outputStream;
    private InputStream inStream;
    private boolean CONNECTED;

    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bluetoothsetup();
        CONNECTED = false;

        final Button BTstartButton = (Button)findViewById(R.id.BTstart);
        final Button BTlistenButton = (Button)findViewById(R.id.listen);
      //  final TextView result = (TextView)findViewById(R.id.result);
        final ScrollView scroller=(ScrollView )findViewById(R.id.scrollView1);


        //replace with raptor tap command //note button won't disable on android until it BTconnects, make new thread if want separate from UI
        ((Button) findViewById(R.id.BTstart)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                BTstartButton.setEnabled(false);
                //Toast.makeText(MainActivity.this, "starting BT server socket", Toast.LENGTH_SHORT).show();
                AcceptThreadObj = new AcceptThread();
                AcceptThreadObj.run();
                //Toast.makeText(MainActivity.this, "BT connection succefull", Toast.LENGTH_SHORT).show();
                //result.setText("Begin listing for incoming data...");
            }
        });

        //ignore for now
      /*  ((Button) findViewById(R.id.listen)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                beginListenForData();
            }
        });*/


    }


    public void Bluetoothsetup() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_SHORT).show();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    private class AcceptThread extends Thread {
            private final BluetoothServerSocket mmServerSocket;
            public AcceptThread() {
                BluetoothServerSocket tmp = null;
                try {
                    // MY_UUID is the app's UUID string, also used by the client code.
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
                } catch (IOException e) {
                    Log.e(TAG, "Socket's listen() method failed", e);
                }
                mmServerSocket = tmp;
            }

                public void run() {
                    BluetoothSocket socket = null;
                    // Keep listening until exception occurs or a socket is returned.
                    while (true) {
                        try {
                            socket = mmServerSocket.accept();
                        } catch (IOException e) {
                            Log.e(TAG, "Socket's accept() method failed", e);
                            break;
                        }

                        if (socket != null) {
                            // A connection was accepted. Perform work associated with
                            // the connection in a separate thread.
                            CONNECTED = true;
                            //Toast.makeText(MainActivity.this, "Connection Accpted", Toast.LENGTH_SHORT).show();

                            if (CONNECTED){
                                Toast.makeText(MainActivity.this, "CONNNECTED=true", Toast.LENGTH_SHORT).show();
                                beginListenForData(socket);
                            }

                           /* try {
                                mmServerSocket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close the connect socket", e);
                            }*/

                           return;
                           //socket.close();
                           //retutn;
                        }
                    }
                }

            public void cancel() {
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Could not close the connect socket", e);
                }
            }
        }


    String temp;
    Spanned temp2;
    CharSequence temp3;
    int countert ;
    int i;

    void beginListenForData(BluetoothSocket Socket)
    {
        //set up input and output stream
        try {
            inStream = Socket.getInputStream();
            outputStream = Socket.getOutputStream();
            Toast.makeText(MainActivity.this, "beginlisteningfordata instream sucess", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final Handler handler = new Handler();
      //  final byte delimiter = 10; //This is the ASCII code for a newline character
        final byte delimiter = 42; //This is the ASCII code for a asterisk
        final TextView result = (TextView) findViewById(R.id.result);
        result.setMovementMethod(new ScrollingMovementMethod());

        stopWorker = false;
        readBuffer = new byte[50000]; //TODO: really only needs to be like 6000 because filled never gets past ~3000 before cutting
        readBufferPosition = 0;
        //TODO: test this impementation heavily for erros
        //TODO: clean up code, delete random tests and random test variables
        //TODO: change delim char to an * or  another less common char
        //TODO: on server side change to html completely (i.e. no spannablestringbuilder), would be faster or no?

        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try {
                        final int bytesAvailable = inStream.available();
                        if (bytesAvailable > 0) {
                            ///shift
                            countert = 0;
                            for (int j = 0; j < readBuffer.length; j++) {
                                if (readBuffer[j] != (byte) 0)
                                    countert++;
                            }
                           /* runOnUiThread(new Runnable() {
                                public void run() {
                                    //Toast.makeText(MainActivity.this, "cosistent", Toast.LENGTH_SHORT).show();
                                    // Toast.makeText(MainActivity.this, "bytesavil: " + bytesAvailable + "\nreachedat: " + i, Toast.LENGTH_SHORT).show();
                                    Toast.makeText(MainActivity.this, "readbuffil: " + countert+ "\nreadbufpo: " + readBufferPosition + "\nbytesavil: " + bytesAvailable , Toast.LENGTH_SHORT).show();
                                }
                            });*/
                            if (countert>3000){
                                byte[] temp = new byte[50000];
                                System.arraycopy(readBuffer, 900, temp, 0, readBuffer.length-900);
                                readBuffer = temp;
                                if (readBufferPosition!=0) {
                                    readBufferPosition = readBufferPosition - 900;
                                }
                            }


                            final byte[] packetBytes = new byte[bytesAvailable];
                            inStream.read(packetBytes);
                            for (i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];

                                if (b == delimiter) {
                                    final byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "UTF-8");

                                    readBufferPosition = 0;
                                    if (data.contains("~")){
                                        handler.post(new Runnable() {
                                            public void run() {
                                                result.setText("");
                                            }
                                        });
                                    }
                                    else {
                                        handler.post(new Runnable() {
                                            public void run() {
                                                temp = data.replace("span style=\"color:", "font color='").replace(";\"", "'").replace("</span>", "</font>");
                                                temp2 = Html.fromHtml(temp); //fromHtml adds two trailing newlines
                                                temp3 = trimTrailingWhitespace(temp2);//remove the said trailing newlines
                                                result.setText(temp3);
                                            }
                                        });
                                    }
                                    break;
                                } else {
                                    if (readBufferPosition>3000) {
                                        byte[] temp = new byte[50000];
                                        System.arraycopy(readBuffer, 500, temp, 0, readBuffer.length - 500);
                                        readBuffer = temp;
                                        if (readBufferPosition != 0) {
                                            readBufferPosition = readBufferPosition - 500;
                                        }

                                        /*runOnUiThread(new Runnable() {
                                            public void run() {
                                                Toast.makeText(MainActivity.this, "cut: " + "readbuffil: " + countert , Toast.LENGTH_SHORT).show();
                                            }
                                        });*/
                                    }

                                    readBufferPosition++;
                                    readBuffer[readBufferPosition] = b;
                                }

                            }

                        }
                    }

                    catch (IOException ex)
                    {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this, "connectionbroke ", Toast.LENGTH_SHORT).show();
                            }
                        });

                        stopWorker = true;
                        //break;
                    }
                }
            }
        });

        workerThread.start();
    }

    public static CharSequence trimTrailingWhitespace(CharSequence source) {
        if(source == null)
            return "";
        int i = source.length();
        // loop back to the first non-whitespace character
        while(--i >= 0 && Character.isWhitespace(source.charAt(i))) {
        }
        return source.subSequence(0, i+1);
    }



}


