package com.example.android.bluetoothrx_keenasr;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.view.View.OnClickListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.lang.Runnable;
import java.lang.Thread;

public class MainActivity extends AppCompatActivity {

    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final String NAME = "device_name";
    private static final String TAG = "MY_APP_DEBUG_TAG";

    private BluetoothAdapter mBluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;
    private AcceptThread AcceptThreadObj;
    private OutputStream outputStream;
    private InputStream inStream;;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bluetoothsetup();


        final Button BTstartButton = (Button)findViewById(R.id.BTstart);
        final TextView result = (TextView)findViewById(R.id.result);

        //replace with raptor tap command //note button won't disable on android until it BTconnects, make new thread if want separate from UI
        ((Button) findViewById(R.id.BTstart)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                BTstartButton.setEnabled(false);
                //Toast.makeText(MainActivity.this, "starting BT server socket", Toast.LENGTH_SHORT).show();
                AcceptThreadObj = new AcceptThread();
                AcceptThreadObj.run();
                //Toast.makeText(MainActivity.this, "BT connection succefull", Toast.LENGTH_SHORT).show();
            }
        });

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
                            Toast.makeText(MainActivity.this, "Connection Accpted", Toast.LENGTH_SHORT).show();
                            try {
                                inStream = socket.getInputStream();
                                outputStream = socket.getOutputStream();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            MssgRun();
                           /* try {
                                mmServerSocket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close the connect socket", e);
                            }*/

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

    private String readMessage;

    public void MssgRun() {
        final TextView result = (TextView) findViewById(R.id.result);
        //Thread readThread = new Thread(new Runnable() {
            final int BUFFER_SIZE = 1024;
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytes = 0;
            int b = BUFFER_SIZE;


            while(true){

                try {
                    bytes = inStream.read(buffer);
                    readMessage = new String(buffer, 0, bytes);
                    result.append(readMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                    //Toast.makeText(MainActivity.this, "test2" + readMessage, Toast.LENGTH_SHORT).show();
                    break;
                }
            }

       // });
    }
       /* synchronized (readThread) {
            readThread.start();
            try {
                readThread.wait(2000);

                if (readThread.isAlive()) {
                    // probably really not good practice!
                    inStream.close();
                    System.out.println("Timeout exceeded!");
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        */




   /* public void MssgRun() {
        final TextView result = (TextView) findViewById(R.id.result);
        int avilableBytes=0;
        final int BUFFER_SIZE = 1024;
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytes;
        int b = BUFFER_SIZE;

        try {

            avilableBytes=inStream.available();

            if (avilableBytes>0) {
                //  bytes = inStream.read(buffer, bytes, BUFFER_SIZE - bytes);
                Toast.makeText(MainActivity.this, "availbytes:" + avilableBytes, Toast.LENGTH_SHORT).show();
                bytes = inStream.read(buffer);
                String readMessage = new String(buffer, 0, bytes);
                result.setText(readMessage);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
       /*
        try{
            while (true) {
                try {
                    avilableBytes=inStream.available();
                    if (avilableBytes>0) {
                        //  bytes = inStream.read(buffer, bytes, BUFFER_SIZE - bytes);
                        bytes = inStream.read(buffer);
                        readMessage = new String(buffer, 0, bytes);
                    }
                } catch (IOException e) {
                   e.printStackTrace();
                }
        }
        } catch (Exception e){
                e.printStackTrace();
        }
        */


}


