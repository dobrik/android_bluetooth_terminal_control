package com.dobrik.firstproject;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Dobrik on 08.01.2018.
 */


public class BluetoothConnection {

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private List<onDataReceiveListener> listeners = new ArrayList<onDataReceiveListener>();

    private Thread workerThread;
    private byte[] readBuffer;
    private int readBufferPosition;
    private volatile boolean stopWorker;

    final String TAG = "BluetoothConnection";

    private BluetoothDevice device;
    private BluetoothSocket mSocket = null;

    interface onDataReceiveListener {
        void onDataReceive(String data);
    }

    BluetoothConnection(BluetoothDevice device) throws IOException {
        BluetoothSocket sockFallback = null;

        Log.wtf(TAG, "Starting Bluetooth connection..");
        try {
            Log.wtf(TAG, "createRfcommSocketToServiceRecord begin");
            this.device = device;
            mSocket = device.createRfcommSocketToServiceRecord(MY_UUID);

            Log.wtf(TAG, "createRfcommSocketToServiceRecord end");
            Log.wtf(TAG, "connect begin");
            mSocket.connect();
            Log.wtf(TAG, "connect end");
        } catch (Exception e1) {
            Log.wtf(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1);
            Class<?> clazz = mSocket.getRemoteDevice().getClass();
            Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
            try {
                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                Object[] params = new Object[]{Integer.valueOf(1)};
                sockFallback = (BluetoothSocket) m.invoke(mSocket.getRemoteDevice(), params);
                sockFallback.connect();
                mSocket = sockFallback;
            } catch (Exception e2) {
                Log.wtf(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2);
                throw new IOException(e2.getMessage());
            }
        }

        try {
            Log.wtf(TAG, "createOutputStream");
            createOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Log.wtf(TAG, "createInputStream");
            createInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            beginListerReceiveData();
        }catch (Exception e){
            throw new IOException(e.getMessage());

        }
    }

    public boolean getConnectionState() {
        if (mSocket != null && mSocket.isConnected()) {
            return true;
        }
        return false;
    }

    private void createInputStream() throws IOException {
        try {
            mInputStream = mSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createOutputStream() throws IOException {
        try {
            mOutputStream = mSocket.getOutputStream();
            Log.wtf(TAG, "Output Stream Created");
        } catch (IOException e) {
            Log.wtf(TAG, "Output Stream Creating error");
            e.printStackTrace();
        }
    }

    public void sendString(String msg) throws IOException {
        sendBytes(msg.getBytes());
    }

    public void sendBytes(byte[] b) throws IOException {
        mOutputStream.write(b);
    }

    public void sendBytes(int i) throws IOException {
        mOutputStream.write(i);
    }

    public void closeStream() {
        if (mOutputStream != null) {
            try {
                mOutputStream.flush();
            } catch (IOException e) {
                Log.wtf("Fatal Error", "Failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try {
            mSocket.close();
        } catch (IOException e2) {
            Log.wtf("Fatal Error", "Failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void beginListerReceiveData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[10240];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            for (onDataReceiveListener listener : listeners){
                                                listener.onDataReceive(data);
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void AddOnDataReceiveListener(onDataReceiveListener listener) {
        listeners.add(listener);
    }
}
