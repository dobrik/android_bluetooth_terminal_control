package com.dobrik.firstproject;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.bluetooth.BluetoothDevice;
import android.widget.ListView;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    final String TAG = "MainActivity";

    private Button search_button, stop_button;
    private FloatingActionButton bluetooth_connected_fab;
    private ArrayAdapter<String> adapter;

    /**
     * BT variables
     **/

    final HashMap<String, BluetoothDevice> device_list = new HashMap<>();
    private final static int REQUEST_ENABLE_BT = 1;

    /**
     * BT variables
     **/

    // Get the default adapter
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private Set<BluetoothDevice> pairedDevices;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(MainActivity.this, "Founded:" + device.getName(), Toast.LENGTH_SHORT).show();
                if (!device_list.containsKey(device.getAddress())) {
                    device_list.put(device.getAddress(), device);
                    adapter.add(device.getName() + "\n(" + device.getAddress() + ")");
                    adapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1);
        bluetooth_connected_fab = findViewById(R.id.bluetooth_connected_fab);

        final ListView listview = (ListView) findViewById(R.id.device_list);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                String device_name = (String) parent.getItemAtPosition(position);
                String mac_address = device_name.substring(device_name.length() - 18, device_name.length() - 1);
                BluetoothDevice device = device_list.get(mac_address);
                pairedDevices = mBluetoothAdapter.getBondedDevices();
                mBluetoothAdapter.cancelDiscovery();

                //make bound with selected device
                if (!pairedDevices.contains(device)) {
                    Log.wtf(TAG, "createBond");
                    device.createBond();
                } else {
                    mBluetoothAdapter.cancelDiscovery();
                    try {
                        Log.wtf(TAG, "Create BluetoothConnection begin");
                        BluetoothConnection connection = new BluetoothConnection(device);
                        Log.wtf(TAG, "Create BluetoothConnection end");
                        if (connection.getConnectionState()) {
                            ConnectionActivity.BTConnection = connection;

                            Toast.makeText(MainActivity.this, "Connected to :" + device.getName(), Toast.LENGTH_SHORT).show();
                            final Intent myIntent = new Intent(MainActivity.this, ConnectionActivity.class);
                            bluetooth_connected_fab.show();
                            bluetooth_connected_fab.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    startActivity(myIntent);
                                }
                            });
                            startActivity(myIntent);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                /*view.animate().setDuration(1000).alpha(0).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        paired_list.remove(item);
                        adapter.notifyDataSetChanged();
                        view.setAlpha(1);
                    }
                });*/
            }

        });

        searchDeviceListener();
    }

    /**
     * Button search devices click event listener
     */
    public void searchDeviceListener() {
        search_button = (Button) findViewById(R.id.search_devices_button);
        search_button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Register for broadcasts when a device is discovered.
                        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                        registerReceiver(mReceiver, filter);
                        device_list.clear();
                        adapter.clear();

                        if (!mBluetoothAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        } else {
                            mBluetoothAdapter.startDiscovery();
                            Log.wtf("DEVICE SEARCH", "START");
                            Toast.makeText(MainActivity.this, "Start searching", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    public void colorPick(View v)
    {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }

}
