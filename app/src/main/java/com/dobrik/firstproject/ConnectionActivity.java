package com.dobrik.firstproject;


import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class ConnectionActivity extends AppCompatActivity {

    public static BluetoothConnection BTConnection;
    private TextView response_text;

    final String TAG = "ConnectionActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
         response_text = findViewById(R.id.response_text);


        BTConnection.AddOnDataReceiveListener(new BluetoothConnection.onDataReceiveListener(){
            @Override
            public void onDataReceive(String data) {
                response_text.append(data + "\n");
            }
        });
        sendDataOnClickListener();
    }


    public void send0(View v) {
        try {
            BTConnection.sendBytes(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send100(View v){
        try {
            BTConnection.sendBytes(100);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send255(View v){
        try {
            BTConnection.sendBytes(255);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendDataOnClickListener() {
        Button send_btn = (Button) findViewById(R.id.button_send);
        send_btn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EditText textField = (EditText) findViewById(R.id.data_send_input);
                        try {
                            BTConnection.sendString(textField.getText().toString());
                            textField.setText(null);
                            Toast.makeText(ConnectionActivity.this, "Data sent", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    @Override
    public void onPause() {
        super.onPause();
        //BTConnection.closeStream();
    }
}
