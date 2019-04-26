package com.example.android.speechapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothAR{

    private BluetoothAdapter mBluetoothAdapter;
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    //private static final String MAC_address = "2C:8A:72:F2:83:CB"; //HTC_One_M8
    private static final String MAC_address = "44:FD:A3:0F:05:1A";   //Raptor
    private OutputStream outputStream;


    public void Bluetoothsetup(Context mcontext){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(mcontext, "Bluetooth is not available!", Toast.LENGTH_SHORT).show();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(mcontext, "Please enable Bluetooth!", Toast.LENGTH_SHORT).show();
        }
    }


    public void ConnectBTDevice(String TAG, Context mcontext) {
        //send out request as a client
        BluetoothDevice mDevice = mBluetoothAdapter.getRemoteDevice(MAC_address);
        BluetoothSocket mSocket;
        //connect thread
        BluetoothSocket tmp = null;
        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mSocket = tmp;
        //run
        mBluetoothAdapter.cancelDiscovery();
        try {
            mSocket.connect();
            outputStream = mSocket.getOutputStream();
            //inStream = mSocket.getInputStream();
            //mSocket.close();
            Toast.makeText(mcontext, "sent request succcess: " + MAC_address, Toast.LENGTH_SHORT).show();

        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }
        //the connection was succesful;
        Toast.makeText(mcontext, "Connected", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "CONNECTED");
    }


    public void MssgWrite(SpannableStringBuilder s) throws IOException {
        String htmlString = Html.toHtml(s);
        htmlString = htmlString + "*";  //a asterisk is the delimeter on rx end
        byte[] mBytes = htmlString.getBytes(("UTF-8"));
        outputStream.write(mBytes);
    }

}
