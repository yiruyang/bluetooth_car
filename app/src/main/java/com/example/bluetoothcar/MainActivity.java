package com.example.bluetoothcar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static android.R.layout.simple_spinner_dropdown_item;


public class MainActivity extends Activity{

    private final String TAG = "MainActivity";

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice mmDevice;
    BluetoothSocket mmSocket;
    public byte[] message = new byte[1];
    OutputStream outputStream = null;
    private int REQUEST_ENABLE_BT = 2;

    private Switch bluetooth_on_off;
    private Button deviceSearch;
    private Spinner bluetooth_spinner;
    private Button bluetooth_connect;
    private Button ultrasonic_distance;
    private Button voltage;
    private List<String> deviceList = new ArrayList<>();
    private ArrayAdapter<String> mArrayAdapter ;
    private MyBroadcastReceiver mFoundReceiver;
    private TextView textView_bluetooth;
    private ImageView left_control;
    private ImageView up_control;
    private ImageView right_control;
    private ImageView down_control;

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
        left_control = (ImageView) findViewById(R.id.left_icon);
        right_control = (ImageView) findViewById(R.id.right_icon);
        up_control = (ImageView) findViewById(R.id.up_icon);
        down_control = (ImageView) findViewById(R.id.down_icon);
        ultrasonic_distance = (Button) findViewById(R.id.ultrasonic_distance);
        voltage = (Button) findViewById(R.id.voltage);

        mArrayAdapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_item,deviceList);
        mArrayAdapter.setDropDownViewResource(simple_spinner_dropdown_item);
        bluetooth_spinner.setAdapter(mArrayAdapter);

        ImageListener imageListener = new ImageListener();
        left_control.setOnTouchListener(imageListener);
        right_control.setOnTouchListener(imageListener);
        up_control.setOnTouchListener(imageListener);
        down_control.setOnTouchListener(imageListener);

        bluetooth_on_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetooth_on_off.isChecked()){
                    if(bluetoothAdapter==null){
                        Toast.makeText(MainActivity.this,"不支持蓝牙",Toast.LENGTH_LONG).show();
                        finish();
                    }else if(!bluetoothAdapter.isEnabled()){
                        Log.d("true","开启蓝牙！");
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent,REQUEST_ENABLE_BT);
                    } else{
                        Toast.makeText(MainActivity.this,"已经打开！！！",Toast.LENGTH_SHORT).show();
                    }
                }else if (!bluetooth_on_off.isChecked()){
                    bluetoothAdapter.disable();
                }
            }
        });

        deviceSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter.isDiscovering()){
                    bluetoothAdapter.cancelDiscovery();
                }
                mArrayAdapter.clear();
                bluetoothAdapter.startDiscovery();
            }
        });

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


        //绑定连接蓝牙
        bluetooth_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, (String) textView_bluetooth.getText());
                if (mmDevice.getName().equalsIgnoreCase((String) textView_bluetooth.getText())) {
                    // 获取蓝牙设备的连接状态
                    int connectState = mmDevice.getBondState();
                    switch (connectState) {
                        // 未配对
                        case BluetoothDevice.BOND_NONE:
                            try {
                                //绑定
                                Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                                Log.e("TAG","开始配对！！");
                                createBondMethod.invoke(mmDevice);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            new ConnectThread(mmDevice).start();
                            break;
                        // 已配对
                        case BluetoothDevice.BOND_BONDED:
                            new ConnectThread(mmDevice).start();
                            Log.d("BlueToothTestActivity", "连接成功......");
                            break;
                        case BluetoothDevice.BOND_BONDING://正在配对
                            Log.d("BlueToothTestActivity", "正在配对......");
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
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);// 注册广播接收器，接收并处理搜索结果
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//状态改变
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);//行动扫描模式改变了
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//动作状态发生了变化
        this.registerReceiver(mFoundReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(mFoundReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothAdapter.disable();
    }

    /**
     *  广播接收器
     */
    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //找到设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                mmDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(MainActivity.this, "发现蓝牙设备！", Toast.LENGTH_SHORT).show();
                Log.d("tag", "onReceive: " + mmDevice.getName());
                deviceList.add(mmDevice.getName());
                //刷新
                mArrayAdapter.notifyDataSetChanged();
            }else{
                context.unregisterReceiver(this);
            }
        }
    }

    //服务器端
    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                final String SPP_UUID = "4C5AFAF7-C886-1A67-0F36-69665C5AA70B";
                UUID uuid = UUID.fromString(SPP_UUID);
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("bluetooth", uuid);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }
    }

    //连接为客户端
    private class ConnectThread extends Thread {
        private  BluetoothSocket mSocket;
        private  BluetoothDevice mDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            Log.d(TAG,"连接线程开启！！");
            BluetoothSocket tmp = null;
            mDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                final String SPP_UUID = "4C5AFAF7-C886-1A67-0F36-69665C5AA70B";
                UUID uuid = UUID.fromString(SPP_UUID);
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) { }
            mSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                Log.d(TAG, "错误");
                try {
                    mSocket.close();
                } catch (IOException closeException) { }
                return;
            }
            // Do work to manage the connection (in a separate thread)
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) { }
        }
    }

    class ImageListener implements View.OnTouchListener{

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (v.getId()){
                case R.id.left_icon:
                    if (event.getAction() == MotionEvent.ACTION_UP){//放开事件
                        message[0] = (byte)0x40;//设置要发送的值
                        blueSendData(message);//发送数值
                        Log.d(TAG,""+message[0]);
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN){//按下事件
                        message[0] = (byte)0x41;
                        Log.d(TAG,""+message[0]);
                        blueSendData(message);//发送数值
                    }
                    break;
                case R.id.up_icon:
                    if (event.getAction() == MotionEvent.ACTION_UP){
                        message[0] = (byte)0x40;//设置要发送的数值
                        blueSendData(message);
                        Log.d(TAG,""+message[0]);
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN){
                        message[0] = (byte)0x42;
                        blueSendData(message);
                        Log.d(TAG,""+message[0]);
                    }
                    break;
                case R.id.right_icon:
                    if (event.getAction() == MotionEvent.ACTION_UP){
                        message[0] = (byte)0x40;//设置要发送的数值
                        blueSendData(message);
                        Log.d(TAG,""+message[0]);
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN){
                        message[0] = (byte)0x43;//设置要发送的数值
                        blueSendData(message);
                        Log.d(TAG,""+message[0]);
                    }
                    break;
                case R.id.down_icon:
                    if (event.getAction() == MotionEvent.ACTION_UP){
                        message[0] = (byte)0x40;
                        blueSendData(message);
                        Log.d(TAG,""+message[0]);
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN){
                        message[0] = (byte) 0x44;
                        blueSendData(message);
                        Log.d(TAG,""+message[0]);
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    //蓝牙发送数据
    public void blueSendData(byte[] message){

        if (mmSocket != null){
            try{
                outputStream= mmSocket.getOutputStream();
                Log.d("send", Arrays.toString(message));
                outputStream.write(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
