package com.example.android.screenreaderbt;

import android.accessibilityservice.AccessibilityButtonController;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;




public class MyService extends AccessibilityService {

    private  int mDebugDepth;
    private String TAG = "Aserv";


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
      //  log+="("+mNodeInfo.getText() +" <-- " + mNodeInfo.getViewIdResourceName()+")";
        log += mNodeInfo.getText();

        //BT Send
        try {
            MssgWrite(log);
        } catch (IOException connectException) {
            connectException.printStackTrace();
        }

        Log.d(TAG, log);
        if (mNodeInfo.getChildCount() < 1) return;
        mDebugDepth++;

        for (int i = 0; i < mNodeInfo.getChildCount(); i++) {
            printAllViews(mNodeInfo.getChild(i));
        }
        mDebugDepth--;
    }

    /*@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_my_service);
    }*/


    @Override
    public void onInterrupt() {
    }

    private AccessibilityButtonController accessibilityButtonController;
    private AccessibilityButtonController
            .AccessibilityButtonCallback accessibilityButtonCallback;
    private boolean mIsAccessibilityButtonAvailable;

    FrameLayout mLayout;

    @Override
    protected void onServiceConnected() {
        /*AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED |
                AccessibilityEvent.TYPE_VIEW_FOCUSED;
        info.flags = AccessibilityServiceInfo.DEFAULT;
        info.notificationTimeout = 100;
        this.setServiceInfo(info);*/


     /*   WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        mLayout = new FrameLayout(this);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        lp.format = PixelFormat.TRANSLUCENT;
        lp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.TOP;
        LayoutInflater inflater = LayoutInflater.from(this);
        inflater.inflate(R.layout.custom_dialog, mLayout);
        wm.addView(mLayout, lp); */

        Bluetoothsetup();
        ShowPairedBT();


    }

    private BluetoothAdapter mBluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private ListView lv;
    private ArrayAdapter aAdapter;
    private OutputStream outputStream;
    Thread workerThread;
    volatile boolean stopWorker;


    public void Bluetoothsetup(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_SHORT).show();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth!", Toast.LENGTH_SHORT).show();
            //Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }


    public void ShowPairedBT() {
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
        Toast.makeText(MyService.this, "connected", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "CONNECTED");
    }

    public void MssgWrite(String s) throws IOException {
        String sendstr = s + "*"; //* is the delim char
        byte[] mBytes = sendstr.getBytes();
        outputStream.write(mBytes);
    }



   /* public void ShowPairedBT() {

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.

            //for (BluetoothDevice device : pairedDevices) {
            //String deviceName = device.getName();
            //String deviceHardwareAddress = device.getAddress(); // MAC address
            ArrayList list = new ArrayList();
            for (BluetoothDevice device : pairedDevices) {
                String devicename = device.getName();
                String macAddress = device.getAddress();
                list.add("Name: " + devicename + "\n" + "MAC Address: " + macAddress);
                ;
            }

            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.custom_dialog);
            //dialog.setTitle("Select a Paired Device to connect");

            lv = (ListView) dialog.findViewById(R.id.listViewBT);
            aAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list);
            lv.setAdapter(aAdapter);

            TextView titleText = new TextView(this);
            titleText.setText("Select a Paired Device to connect to:");
            lv.addHeaderView(titleText);

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener () {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

                    String item = aAdapter.getItem(position-1).toString();
                   // String MAC_address = "2C:8A:72:F2:83:CB";
                    String MAC_address = item.substring(item.indexOf("\n")+1);
                    MAC_address = MAC_address.substring(MAC_address.indexOf(":")+2);
                    MAC_address.trim();

                    //Toast.makeText(MainActivity.this, "test-" + MAC_address + "-endtest", Toast.LENGTH_SHORT).show();

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
                    Toast.makeText(MyService.this, "connected", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "CONNECTEDtowenfen10");
                }
            });

            Log.d(TAG, "test1");

            /*final Handler handler = new Handler();
            workerThread = new Thread(new Runnable() {
                public void run() {
                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                        handler.post(new Runnable() {
                            public void run() {
                                dialog.show();
                            }
                        });
                    }
                }
            });
            workerThread.start();

            //dialog.show();
            //Log.d(TAG, "test2");


        }
    } */






}




        /*accessibilityButtonController = getAccessibilityButtonController();
        mIsAccessibilityButtonAvailable =
                accessibilityButtonController.isAccessibilityButtonAvailable();

        if (mIsAccessibilityButtonAvailable) {
            Log.d(TAG, "buttonavail");
        }


        if (!mIsAccessibilityButtonAvailable) {
            Log.d(TAG, "buttonNOTavail");
            return;
        }

        AccessibilityServiceInfo serviceInfo = getServiceInfo();
        serviceInfo.flags
                |= AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;
        setServiceInfo(serviceInfo);

        accessibilityButtonCallback =
                new AccessibilityButtonController.AccessibilityButtonCallback() {
                    @Override
                    public void onClicked(AccessibilityButtonController controller) {
                        Log.d("MY_APP_TAG", "Accessibility button pressed!");

                        // Add custom logic for a service to react to the
                        // accessibility button being pressed.
                    }

                    @Override
                    public void onAvailabilityChanged(
                            AccessibilityButtonController controller, boolean available) {
                        if (controller.equals(accessibilityButtonController)) {
                            mIsAccessibilityButtonAvailable = available;
                        }
                    }
                };

        if (accessibilityButtonCallback != null) {
            accessibilityButtonController.registerAccessibilityButtonCallback(
                    accessibilityButtonCallback, null);
        }*/



      /*  //do stuff
        // Set the type of events that this service wants to listen to.  Others
        // won't be passed to this service.
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED |
                AccessibilityEvent.TYPE_VIEW_FOCUSED;
        // Set the type of feedback your service will provide.
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        // Default services are invoked only if no package-specific ones are present
        // for the type of AccessibilityEvent generated.  This service *is*
        // application-specific, so the flag isn't necessary.  If this was a
        // general-purpose service, it would be worth considering setting the
        // DEFAULT flag.
        info.flags = AccessibilityServiceInfo.DEFAULT;

        info.notificationTimeout = 100;

        this.setServiceInfo(info);  */