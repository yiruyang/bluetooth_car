package com.example.bluetoothcar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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

        ImageListener imageListener = new ImageListener();
        left_control.setOnTouchListener(imageListener);
        right_control.setOnTouchListener(imageListener);
        up_control.setOnTouchListener(imageListener);
        down_control.setOnTouchListener(imageListener);

        mArrayAdapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_item,deviceList);
        mArrayAdapter.setDropDownViewResource(simple_spinner_dropdown_item);
        bluetooth_spinner.setAdapter(mArrayAdapter);

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
//                mArrayAdapter.clear();
                // 寻找蓝牙设备，android会将查找到的设备以广播形式发出去
                Log.d("tag", "onClick: deviceSearch" );
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


        //连接蓝牙
        bluetooth_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mmDevice.getName().equalsIgnoreCase((String) textView_bluetooth.getText())) {
                    // 搜索蓝牙设备的过程占用资源比较多，一旦找到需要连接的设备后需要及时关闭搜索
                    bluetoothAdapter.cancelDiscovery();
                    // 获取蓝牙设备的连接状态
                    int connectState = mmDevice.getBondState();
                    switch (connectState) {
                        // 未配对
                        case BluetoothDevice.BOND_NONE:
                            // 配对
                            try {
                                Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                                Log.e("TAG","开始配对！！");
                                createBondMethod.invoke(mmDevice);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            bluetoothAdapter.cancelDiscovery();
                            try {
                                connect();
                            } catch (IOException e) {
                                try {
                                    mmSocket.close();
                                    mmSocket = null;
                                } catch (IOException e1) {
                                    Log.e("Tag","连接失败！无法连接！");
                                }
                            }
                            break;
                        // 已配对
                        case BluetoothDevice.BOND_BONDED:
                            Toast.makeText(MainActivity.this, "已配对！！", Toast.LENGTH_SHORT).show();
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
                // 已经匹配
//                if (bluetoothDevice.getBondState() != bluetoothDevice.BOND_BONDED){
                    //
                    Log.d("tag", "onReceive: " + mmDevice.getName());
                    deviceList.add(mmDevice.getName());
                    mArrayAdapter.notifyDataSetChanged();
//                }
            }else{
                context.unregisterReceiver(this);
            }
        }
    }

//    private class ConnectThread extends Thread {
//        private final BluetoothSocket mSocket;
//        private final BluetoothDevice mDevice;
//
//        public ConnectThread(BluetoothDevice device) {
//            // Use a temporary object that is later assigned to mmSocket,
//            // because mmSocket is final
//            BluetoothSocket tmp = null;
//            mDevice = device;
//
//            // Get a BluetoothSocket to connect with the given BluetoothDevice
//            try {
//                final String SPP_UUID = "4C5AFAF7-C886-1A67-0F36-69665C5AA70B";
//                UUID uuid = UUID.fromString(SPP_UUID);
//                // MY_UUID is the app's UUID string, also used by the server code
//                tmp = device.createRfcommSocketToServiceRecord(uuid);
//            } catch (IOException e) { }
//            mSocket = tmp;
//        }
//
//        public void run() {
//            // Cancel discovery because it will slow down the connection
//            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
//
//            try {
//                // Connect the device through the socket. This will block
//                // until it succeeds or throws an exception
//                mSocket.connect();
//            } catch (IOException connectException) {
//                // Unable to connect; close the socket and get out
//                try {
//                    mSocket.close();
//                } catch (IOException closeException) { }
//                return;
//            }
//            // Do work to manage the connection (in a separate thread)
//        }
//
//        /** Will cancel an in-progress connection, and close the socket */
//        public void cancel() {
//            try {
//                mSocket.close();
//            } catch (IOException e) { }
//        }
//    }


    private void connect()  throws IOException {
        // 固定的UUID
        final String SPP_UUID = "4C5AFAF7-C886-1A67-0F36-69665C5AA70B";
        UUID uuid = UUID.fromString(SPP_UUID);
        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mmSocket.connect();
        } catch (IOException e) {
            try {
                mmSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        Log.d("tag:", "connect: " + mmSocket);
    }

    class ImageListener implements View.OnTouchListener{

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (v.getId()){
                case R.id.left_icon:
                    if (event.getAction() == MotionEvent.ACTION_UP){//放开事件
                        message[0] = (byte)0x40;//设置要发送的值
                        blueSendData(message);//发送数值
                        Log.d("MainActivity",""+message[0]);
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN){//按下事件
                        message[0] = (byte)0x41;
                        Log.d("MainActivity",""+message[0]);
                        blueSendData(message);//发送数值
                    }
                    break;
                case R.id.up_icon:
                    if (event.getAction() == MotionEvent.ACTION_UP){
                        message[0] = (byte)0x40;//设置要发送的数值
                        blueSendData(message);
                        Log.d("MainActivity",""+message[0]);
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN){
                        message[0] = (byte)0x42;
                        blueSendData(message);
                        Log.d("MainActivity",""+message[0]);
                    }
                    break;
                case R.id.right_icon:
                    if (event.getAction() == MotionEvent.ACTION_UP){
                        message[0] = (byte)0x40;//设置要发送的数值
                        blueSendData(message);
                        Log.d("MainActivity",""+message[0]);
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN){
                        message[0] = (byte)0x43;//设置要发送的数值
                        blueSendData(message);
                        Log.d("MainActivity",""+message[0]);
                    }
                    break;
                case R.id.down_icon:
                    if (event.getAction() == MotionEvent.ACTION_UP){
                        message[0] = (byte)0x40;
                        blueSendData(message);
                        Log.d("MainActivity",""+message[0]);
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN){
                        message[0] = (byte) 0x44;
                        blueSendData(message);
                        Log.d("MainActivity",""+message[0]);
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

        if ( null == mmSocket){
            return;
        }

        try{
            outputStream= mmSocket.getOutputStream();
            Log.d("send", Arrays.toString(message));
            outputStream.write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
