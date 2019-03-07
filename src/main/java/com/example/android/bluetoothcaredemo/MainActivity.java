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
import java.net.Socket;
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
    //private OutputStream outputStream;
    private InputStream inStream;
    private boolean CONNECTED;

    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    private String temp;
    private int countert;
    private int i;
    private boolean PAUSED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Bluetoothsetup();
        CONNECTED = false;
        PAUSED = false;

        final Button BTstartButton = (Button)findViewById(R.id.BTstart);

        //link with raptor tap command
        ((Button) findViewById(R.id.BTstart)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AcceptThreadObj = new AcceptThread();
                AcceptThreadObj.run();
            }
        });
    }

    @Override
    public void onForward()
    {
        super.onForward();
        EvsToast.show(this,"Opening Server Socket");
        final Button BTstartButton = (Button)findViewById(R.id.BTstart);
        BTstartButton.performClick();
    }
    @Override
    public void onDown()
    {
        //the default behaviour of down is to close the activity
        super.onDown();
        //android.os.Process.killProcess(android.os.Process.myPid());  //kills activity completely, so every time app is opened it re-initializes
        AcceptThreadObj.cancel();
    }
    @Override
    public void onTap()
    {
        super.onTap();

       //pause/play capability from headset
        /* if (!CONNECTED) //disable tap until BT connection successful
            return;

        if (PAUSED) {
            try {
                CommandWrite("Pause");
            } catch (IOException connectException) {
                connectException.printStackTrace();
            }
        }
        else {
            try {
                CommandWrite("Play");
            } catch (IOException connectException) {
                connectException.printStackTrace();
            }
        } */

    }


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
                    if (CONNECTED){
                        EvsToast.show(MainActivity.this, "Connected to Device");
                        beginListenForData(socket);
                    }
                    return;
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

    void beginListenForData(BluetoothSocket Socket)
    {
        //set up input and output stream
        try {
            inStream = Socket.getInputStream();
            //outputStream = Socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final Handler handler = new Handler();
        final byte delimiter = 42; //This is the ASCII code for a asterisk
        final TextView result = (TextView) findViewById(R.id.result);
        result.setMovementMethod(new ScrollingMovementMethod());

        stopWorker = false;
        readBuffer = new byte[10000];
        readBufferPosition = 0;
        //TODO: test this impementation heavily for errors
        //TODO: clean up code, delete random tests and random test variables
        //TODO: on server side change to html completely (i.e. no spannablestringbuilder), would be faster or no?
        //TODO: stop sending full history everytme, have two textboxes, one for history, one for new result

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
                                                result.setText(Html.fromHtml(temp));
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

 /*   public void CommandWrite(String s) throws IOException {
        outputStream.write(s.getBytes());
    }*/


} //end of MainActivity

