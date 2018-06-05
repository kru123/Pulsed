package com.example.user.pulsed;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import  com.example.user.pulsed.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Created by user on 5/12/2018.
 */

public  class Bluetooth extends AppCompatActivity implements AdapterView.OnItemClickListener {

    public static  void  disconnect(){
        if(connectedThread!=null){
            connectedThread.cancel();
            connectedThread=null;

        }
    }
    public static void gethandler (Handler handler) {
        mHandler = handler;

    }

    static Handler mHandler;
    protected static final int SUCCESS_CONNECT = 0;
    protected static final int MESSAGE_READ = 1;
    static ConnectedThread connectedThread;
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    ListView listView;
    ArrayAdapter<String> listAdapter;
    static BluetoothAdapter btAdapter;
    Set<BluetoothDevice> devicesArray;
    ArrayList<String> pairedDevices;
    ArrayList<BluetoothDevice> devices;
    IntentFilter filter;
    BroadcastReceiver receiver;


    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        init();
        if (btAdapter == null) {
            Toast.makeText(getApplicationContext(), "No Bluetooth detected", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (!btAdapter.isEnabled()) {
                turnOnBT();
            }
            getPairedDevice();
            startDiscovery();
        }

    }

    private void getPairedDevice() {
        btAdapter.cancelDiscovery();
        btAdapter.startDiscovery();
    }

    private void turnOnBT() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent,1);
    }

    private void startDiscovery() {
        devicesArray = btAdapter.getBondedDevices();
        if (devicesArray.size() > 0) {
            for (BluetoothDevice device : devicesArray) {
                pairedDevices.add(device.getName());
            }
        }
    }


    public void init() {
        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(this);
        listAdapter = new ArrayAdapter <String>(this, android.R.layout.simple_list_item_1, 0);
        listView.setAdapter(listAdapter);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = new ArrayList <String>();
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        devices = new ArrayList <BluetoothDevice>();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    devices.add(device);

                    listAdapter.add(device.getName() + " " + " " + "\n" + device.getAddress());

                } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {

                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

                } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    if (btAdapter.getState() == btAdapter.STATE_OFF) {
                        turnOnBT();
                    }
                }

            }
        };
        gethandler(mHandler);
        registerReceiver(receiver,filter);
        filter=new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver,filter);
        filter=new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver,filter);
        filter=new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
    }
    @Override

    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode==RESULT_CANCELED){
            Toast.makeText(getApplicationContext(), "Bluetooth must be enabled to continue", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1,int arg2,long arg3){
        if(btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        }
        if(listAdapter.getItem(arg2).contains("Paired")){
            BluetoothDevice selectedDevice=devices.get(arg2);
            ConnectThread connect =new ConnectThread((selectedDevice));
            connect.start();
        }else{
            Toast.makeText(getApplicationContext(), "Device is not paired", Toast.LENGTH_SHORT).show();
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(Bluetooth.MY_UUID);
            } catch (IOException e) {

            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            btAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {

                }
                return;
            }

            mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }

    static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {

            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] Buffer;
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    try {
                        sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Buffer = new byte[1024];
                    // Read from the InputStream.
                    numBytes = mmInStream.read(Buffer);
                    // Send the obtained bytes to the UI activity.
                    mHandler.obtainMessage(MESSAGE_READ, numBytes, -1, Buffer).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(String income) {
            try {
                mmOutStream.write(income.getBytes());
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {

            }

        }
        // Call this method from the main activity to shut down the connection.

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }

}

