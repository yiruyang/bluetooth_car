package com.example.bluetoothcar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static android.R.layout.simple_spinner_dropdown_item;


public class MainActivity extends Activity {

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice bluetoothDevice;
    BluetoothSocket bluetoothSocket;
    OutputStream outputStream = null;
    private int REQUEST_ENABLE_BT = 2;

    private Switch bluetooth_on_off;
    private Button deviceSearch;
    private Spinner bluetooth_spinner;
    private Button bluetooth_connect;
    private List<String> deviceList = new ArrayList<>();
    private ArrayAdapter<String> arrayAdapter ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //取消标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //取消状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        bluetooth_on_off = (Switch) findViewById(R.id.bluetooth_open_off);
        deviceSearch = (Button) findViewById(R.id.bluetooth_search);
        bluetooth_spinner = (Spinner) findViewById(R.id.bluetooth_spinner);
        bluetooth_connect = (Button) findViewById(R.id.bluetooth_connect);
        bluetooth_on_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetooth_on_off.isChecked()){
                    if(bluetoothAdapter==null){
                        Toast.makeText(MainActivity.this,"不支持蓝牙",Toast.LENGTH_LONG).show();
                        finish();
                    }else if(!bluetoothAdapter.isEnabled()){
                        Log.d("true","开始连接");
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent,REQUEST_ENABLE_BT);
                    }
                }else{
                        bluetoothAdapter.disable();
                }
            }
        });
        deviceSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BroadcastReceiver mFoundReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        //找到设备
                        if (BluetoothDevice.ACTION_FOUND.equals(action)){
                            bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            if (bluetoothDevice.getBondState() != bluetoothDevice.BOND_BONDED){
                                deviceList.add(bluetoothDevice.getName()+bluetoothDevice.getAddress());
                                arrayAdapter.setDropDownViewResource(simple_spinner_dropdown_item);
                                bluetooth_spinner.setAdapter(arrayAdapter);

                            }
                        }
                    }
                };
            }
        });
    }

}
