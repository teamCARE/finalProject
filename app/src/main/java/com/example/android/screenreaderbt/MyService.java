package com.example.android.screenreaderbt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;


public class MyService extends AccessibilityService {

    //TODO: set it only to work with live transcribe? WOuld avoid app stopping working if get a notifcation or text message in th emiddle (?). Do we want it to work with other apps?

    private  int mDebugDepth;
    private String TAG = "Aserv";
    private BluetoothAdapter mBluetoothAdapter;
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private OutputStream outputStream;
    private int startPos = 1000;
    private static int startPosStep = 1000;
    private static int maxSendSize = 1500;

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        Log.d(TAG, "entering onServiceConnected");
        Bluetoothsetup();
        ConnectBTDevice();
        //prevLen = 0;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        mDebugDepth = 0;
        AccessibilityNodeInfo mNodeInfo = event.getSource();
        printAllViews(mNodeInfo);
    }


    private void printAllViews(AccessibilityNodeInfo mNodeInfo) {
        if (mNodeInfo == null) return;
        String log ="";
        for (int i = 0; i < mDebugDepth; i++) {
            log += ".";
        }

        log += mNodeInfo.getText();
        //Log.d(TAG, "curlen" + log.length());

        if (log.length() - startPos > maxSendSize) {
            startPos += startPosStep;
            //Log.d(TAG, "startPos" + startPos);
        }

        //cut text from becoming infinitely long
        //inital string can be huge, but max string is 2147483647 chars long so ok
        if (log.length()>1500){
                log = log.substring(startPos, log.length()); //sets the maximum size to 400
                //Toast.makeText(MyService.this, "cut text to length" + log.length(), Toast.LENGTH_SHORT).show(); /see when its cutting
                //Log.d(TAG, "cut text to length" + log.length());
        }

        //BT Send
        try {
            MssgWrite(log);
        } catch (IOException connectException) {
            connectException.printStackTrace();
        }

        //Log.d(TAG, log); //uncomment if want to see (what's being sent) in the debug log
        if (mNodeInfo.getChildCount() < 1) return;
        mDebugDepth++;

        for (int i = 0; i < mNodeInfo.getChildCount(); i++) {
            printAllViews(mNodeInfo.getChild(i));
        }
        mDebugDepth--;
    }

    public void Bluetoothsetup(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_SHORT).show();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth!", Toast.LENGTH_SHORT).show();
        }
    }

    public void ConnectBTDevice() {
        // String MAC_address = "2C:8A:72:F2:83:CB"; //HTC_One_M8
        String MAC_address = "44:FD:A3:0F:05:1A"; //Raptor

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
            Toast.makeText(MyService.this, "sent request succcess: " + MAC_address, Toast.LENGTH_SHORT).show();

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
        Toast.makeText(MyService.this, "Connected", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "CONNECTED");
    }

    public void MssgWrite(String s) throws IOException {
        String sendstr = s + "*"; //* is the delim char
        byte[] mBytes = sendstr.getBytes();
        outputStream.write(mBytes);
    }


} //MyService end
