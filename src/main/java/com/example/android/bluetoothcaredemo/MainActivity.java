package com.example.android.bluetoothcaredemo;

import android.os.Bundle;

import com.everysight.base.EvsBaseActivity;
import com.everysight.notifications.EvsToast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.text.Html;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.lang.Runnable;
import java.lang.Thread;

import android.os.Handler;

public class MainActivity extends EvsBaseActivity {

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
    volatile boolean stopWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Bluetoothsetup();
        CONNECTED = false;

        final Button BTstartButton = (Button)findViewById(R.id.BTstart);

        //link with raptor tap command //note button show signs of trigger until it BTconnects, make new thread if want separate from UI
        ((Button) findViewById(R.id.BTstart)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
               // BTstartButton.setEnabled(false);
                //Toast.makeText(MainActivity.this, "starting BT server socket", Toast.LENGTH_SHORT).show();
                AcceptThreadObj = new AcceptThread();
                AcceptThreadObj.run();
                //Toast.makeText(MainActivity.this, "BT connection succefull", Toast.LENGTH_SHORT).show();
                //result.setText("Begin listing for incoming data...");
            }
        });
    }

    @Override
    public void onTap()
    {
        super.onTap();

        final Button BTstartButton = (Button)findViewById(R.id.BTstart);
        BTstartButton.performClick();
    }
   /* @Override
    public void onDown()
    {
        //the default behaviour of down is to close the activity
        super.onDown();
        android.os.Process.killProcess(android.os.Process.myPid()); //kills activity completely, so every time app is opened it re-initializes
    }*/


    public void Bluetoothsetup() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            EvsToast.show(this, "Bluetooth is not available!");
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
                        EvsToast.show(MainActivity.this, "CONNNECTED=true");
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
    int countert ;
    int i;

    void beginListenForData(BluetoothSocket Socket)
    {
        //set up input and output stream
        try {
            inStream = Socket.getInputStream();
            outputStream = Socket.getOutputStream();
            EvsToast.show(MainActivity.this, "beginlisteningfordata instream sucess");
        } catch (IOException e) {
            e.printStackTrace();
        }

        final Handler handler = new Handler();
        //  final byte delimiter = 10; //This is the ASCII code for a newline character
        final byte delimiter = 45; //This is the ASCII code for a dash
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
                            //EvsToast.show(MainActivity.this, "bytesavail: " + bytesAvailable);

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

                                    handler.post(new Runnable() {
                                        public void run() {
                                            temp = data.replace("span style=\"color:", "font color='").replace(";\"", "'").replace("</span>", "</font>");
                                            // result.setText(data);
                                            result.setText(Html.fromHtml(temp));
                                            // ssbuilderRX = new SpannableString(Html.fromHtml(data));
                                            //ssbuilderRX = new SpannableString(Html.fromHtml(data, Html.FROM_HTML_MODE_COMPACT));
                                            // result.setText(ssbuilderRX, TextView.BufferType.SPANNABLE);
                                            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            //       result.setText(Html.fromHtml(temp, Html.FROM_HTML_MODE_COMPACT));
                                            //  } else {
                                            //   result.setText(Html.fromHtml(temp));
                                            // }

                                        }
                                    });
                                    break;
                                } else {
                                    if (readBufferPosition>3000) {
                                        byte[] temp = new byte[50000];
                                        System.arraycopy(readBuffer, 500, temp, 0, readBuffer.length - 500);
                                        readBuffer = temp;
                                        if (readBufferPosition != 0) {
                                            readBufferPosition = readBufferPosition - 500;
                                        }

                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                Toast.makeText(MainActivity.this, "cut: " + "readbuffil: " + countert , Toast.LENGTH_SHORT).show();
                                            }
                                        });
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


} //end of MainActivity

