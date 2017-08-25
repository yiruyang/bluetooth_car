package com.example.bluetoothcar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.R.layout.simple_spinner_dropdown_item;


public class MainActivity extends Activity {

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice bluetoothDevice;
    BluetoothSocket bluetoothSocket;
    OutputStream outputStream = null;
    private int REQUEST_ENABLE_BT = 2;
    String name;

    private Switch bluetooth_on_off;
    private Button deviceSearch;
    private Spinner bluetooth_spinner;
    private Button bluetooth_connect;
    private List<String> deviceList = new ArrayList<>();
    private ArrayAdapter<String> mArrayAdapter ;
    private MyBroadcastReceiver mFoundReceiver;
    private TextView textView_bluetooth;

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
        textView_bluetooth = (TextView) findViewById(R.id.textView_bluetooth);
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
                mArrayAdapter.clear();
                // 寻找蓝牙设备，android会将查找到的设备以广播形式发出去
                bluetoothAdapter.startDiscovery();
            }
        });
        mArrayAdapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_item,deviceList);
        mArrayAdapter.setDropDownViewResource(simple_spinner_dropdown_item);
        bluetooth_spinner.setAdapter(mArrayAdapter);

        bluetooth_spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {


            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                textView_bluetooth.setText(deviceList.get(position));
                //设置显示当前选择的项
                parent.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                arg0.setVisibility(View.VISIBLE);

            }
        });

        bluetooth_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothDevice.getName().equalsIgnoreCase(name)) {
                    // 搜索蓝牙设备的过程占用资源比较多，一旦找到需要连接的设备后需要及时关闭搜索
                    bluetoothAdapter.cancelDiscovery();
                    // 获取蓝牙设备的连接状态
                    int connectState = bluetoothDevice.getBondState();
                    switch (connectState) {
                        // 未配对
                        case BluetoothDevice.BOND_NONE:
                            // 配对
                            try {
                                Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                                createBondMethod.invoke(bluetoothDevice);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        // 已配对
                        case BluetoothDevice.BOND_BONDED:
                            try {
                                // 连接
                                connect(bluetoothDevice);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFoundReceiver = new MyBroadcastReceiver();
        //广播接收器
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        // 注册广播接收器，接收并处理搜索结果
        this.registerReceiver(mFoundReceiver, intentFilter);
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //找到设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(MainActivity.this, "发现蓝牙设备！", Toast.LENGTH_SHORT).show();
                if (bluetoothDevice.getBondState() != bluetoothDevice.BOND_BONDED){
                    deviceList.add(bluetoothDevice.getName());
                    mArrayAdapter.notifyDataSetChanged();
                }

            }
        }
    }

    private void connect(BluetoothDevice device) throws IOException {
        // 固定的UUID
        final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
        UUID uuid = UUID.fromString(SPP_UUID);
        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuid);
        socket.connect();
    }

}
