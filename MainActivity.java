package ru.promichail.bluetoothapp;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.attribute.AttributeView;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements
        CompoundButton.OnCheckedChangeListener,
        AdapterView.OnItemClickListener,
View.OnClickListener {

    private static final int REQUEST_CODE_LOC = 1;
    private static final int REQ_ENABLE_BT = 10;
    private static final int BT_BOUNED = 21;
    private static final int BT_SEARCH = 22;
    private static final double SENS = 0.35;
    private static final int V = 5;
    private static double Xcalib=1.58;
    private static double Ycalib=1.585;
    private static double Zcalib=1.57;
    private static boolean OneMore=false;
    private static double shakeOld=0;
 private FrameLayout frameMessage;
 private LinearLayout frameControl;

 private RelativeLayout frameLedControls;
 private Button btnDisconnect;
 private TextView messageOnline;
 private TextView messageOnline2;
 private ImageView truck;
    private ImageView boom1;

 private Switch switchEnableBt;
 private Button btnEnableSearch;
 private ProgressBar pbProgress;
 private ListView lisstBtDevices;

 private BluetoothAdapter bluetoothAdapter;
 private BtListAdapter listAdapter;
 private ArrayList<BluetoothDevice> bluetoothDevices;

 private ConnectThread connectThread;
 private ConnectedThread connectedThread;

private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        frameMessage = findViewById(R.id.frame_message);
        frameControl = findViewById(R.id.frame_countrol);

        switchEnableBt  =   findViewById(R.id.switch_enable_bt);
        btnEnableSearch =   findViewById(R.id.btn_enable_search);
        pbProgress      =   findViewById(R.id.pb_progress);
        lisstBtDevices  =   findViewById(R.id.lv_bt_device);
        truck = findViewById(R.id.truck);
        boom1 = findViewById(R.id.boom1);

        frameLedControls = findViewById(R.id.frameLedControls);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        messageOnline = findViewById(R.id.message_online);
        messageOnline2 = findViewById(R.id.message_online2);

        switchEnableBt.setOnCheckedChangeListener(this);
        btnEnableSearch.setOnClickListener(this);
        lisstBtDevices.setOnItemClickListener(this);
        btnDisconnect.setOnClickListener(this);

        bluetoothDevices=new ArrayList<>();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(getString(R.string.connecting));
        progressDialog.setMessage(getString(R.string.please_wait));

        IntentFilter filter = new IntentFilter();
        filter.addAction(bluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(bluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver,filter);

        bluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        if(bluetoothAdapter.isEnabled()){
            showFrameControls();
            switchEnableBt.setChecked(true);
            setListAdapter(BT_BOUNED);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        if (connectThread != null){
            connectThread.cancel();
        }
        if (connectedThread != null){
            connectedThread.cancel();
        }
    }

    @Override
    public void onClick(View v) {
        if(v.equals(btnEnableSearch)){
            enableSearch();
        }else if (v.equals(btnDisconnect)){
            if (connectThread != null){
                connectThread.cancel();
            }
            if (connectedThread != null){
                connectedThread.cancel();
            }
            showFrameControls();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(parent.equals(lisstBtDevices)){
            BluetoothDevice device = bluetoothDevices.get(position);
            if(device != null){
                connectThread = new ConnectThread(device);
                connectThread.start();
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(buttonView.equals(switchEnableBt)) {
            enableBt(isChecked);
            if (!isChecked) {
                showFrameMessage();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ENABLE_BT) {
            if (resultCode == RESULT_OK && bluetoothAdapter.isEnabled()) {
                showFrameControls();
                setListAdapter(BT_BOUNED);
            } else if (resultCode == RESULT_CANCELED) {
                enableBt(true);
            }
        }
    }

    private void  showFrameMessage(){
        frameMessage.setVisibility(View.VISIBLE);
        frameLedControls.setVisibility(View.GONE);
        switchEnableBt.setVisibility(View.VISIBLE);
        frameControl.setVisibility(View.GONE);
    }
    private void  showFrameControls(){
        frameMessage.setVisibility(View.GONE);
        frameLedControls.setVisibility(View.GONE);
        switchEnableBt.setVisibility(View.VISIBLE);
        frameControl.setVisibility(View.VISIBLE);
    }
    private void  showFrameLedControls(){
        frameLedControls.setVisibility(View.VISIBLE);
        switchEnableBt.setVisibility(View.GONE);
        frameMessage.setVisibility(View.GONE);
        frameControl.setVisibility(View.GONE);
    }
    private void enableBt(boolean flag){
        if(flag)
        {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,REQ_ENABLE_BT);

        } else{
            bluetoothAdapter.disable();
        }
    }
    private  void setListAdapter(int type){
        bluetoothDevices.clear();
        int iconType = R.drawable.ic_bluetooth_bounded_device;
        switch (type){
            case BT_BOUNED:
                bluetoothDevices = getBluetoothDevices();
                iconType = R.drawable.ic_bluetooth_bounded_device;
                break;
            case BT_SEARCH:
                iconType = R.drawable.ic_bluetooth_search_device;
                break;
        }
        listAdapter=new BtListAdapter(this,bluetoothDevices,iconType);
        lisstBtDevices.setAdapter(listAdapter);
    }
    private  ArrayList<BluetoothDevice> getBluetoothDevices(){
        Set<BluetoothDevice> deviceSet = bluetoothAdapter.getBondedDevices();
        ArrayList<BluetoothDevice> tmpArrayList = new ArrayList<>();
        if(deviceSet.size()>0){
            for(BluetoothDevice device: deviceSet){
                tmpArrayList.add(device);
            }
        }
        return tmpArrayList;
    }
    private void enableSearch(){
        if (bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }else{
            bluetoothAdapter.startDiscovery();
        }
    }
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            switch (action){
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    btnEnableSearch.setText(R.string.stop);
                    pbProgress.setVisibility(View.VISIBLE);
                    setListAdapter(BT_SEARCH);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    btnEnableSearch.setText(R.string.start_search);
                    pbProgress.setVisibility(View.GONE);
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device!=null){
                        bluetoothDevices.add(device);
                        listAdapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
    };
    /**
     * Запрос на разрешение данных о местоположении (для Marshmallow 6.0)
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_LOC:

                if (grantResults.length > 0) {
                    for (int gr : grantResults) {
                        // Check if request is granted or not
                        if (gr != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                    }
                    //TODO - Add your code here to start Discovery
                }
                break;
            default:
                return;
        }
    }
    private class ConnectThread extends Thread {
        private BluetoothSocket bluetoothSocket = null;
        private boolean success = false;
        public ConnectThread(BluetoothDevice device) {
            try {
                Method method = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                bluetoothSocket = (BluetoothSocket) method.invoke(device, 1);

                progressDialog.show();
            }catch(Exception e){
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            try {
                bluetoothSocket.connect();
                success = true;

                progressDialog.dismiss();
            } catch (IOException e){
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this,"Не могу соединиться!", Toast.LENGTH_SHORT).show();
                    }
                });
                cancel();
            }
            if(success){
               connectedThread = new ConnectedThread(bluetoothSocket);
               connectedThread.start();
               runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       showFrameLedControls();
                   }
               });
            }
        }
        public boolean isConnect(){                   //activ!!!
            return bluetoothSocket.isConnected();
        }
        public void cancel(){
            try{
                bluetoothSocket.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
    private class ConnectedThread extends Thread{
        private InputStream inputStream;
        private boolean isConnected = false;
        public ConnectedThread(BluetoothSocket bluetoothSocket){
            InputStream inputStream = null;

            try{
                inputStream = bluetoothSocket.getInputStream();
            }catch (IOException e){
                e.printStackTrace();
            }
            this.inputStream = inputStream;
            isConnected = true;
        }

        @Override
        public void run() {
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            final StringBuffer buffer = new StringBuffer();
            final String[] str = {new String()};
            while (isConnected){
                try{
                    int bytes=bis.read();
                    buffer.append((char) bytes);
                    int eof = buffer.indexOf("\r\n");
                    if(eof>0){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                str[0] =buffer.toString();
                                String [] text = str[0].split("[s-]+");
                                if(text.length>=3) {
                                    try {
                                        int outX = Integer.parseInt(text[1]);
                                        int outY = Integer.parseInt(text[2]);
                                        int outZ = Integer.parseInt(text[3]);
                                        if (OneMore == false) {
                                            Xcalib = outX * V / 1024.0;
                                            Ycalib = outY * V / 1024.0;
                                            OneMore = true;
                                        }
                                        double Gx = (outX * V / 1024.0 - Xcalib) / SENS;
                                        double Gy = (outY * V / 1024.0 - Ycalib) / SENS;
                                        double Gz = (outZ * V / 1024.0 - Zcalib) / SENS;
                                        double shake =Math.sqrt(Math.pow(Gx,2)+Math.pow(Gy,2)+Math.pow(Gz,2));
                                        double angle_R = Math.atan(Math.sqrt(Math.pow(Gy,2)+Math.pow(Gz,2))/Gy)*180/Math.PI+90;
                                        if (angle_R>=90){
                                            angle_R=angle_R-180;
                                        }
                                        truck.setRotation((float) -angle_R);

                                        if (Math.abs(angle_R) <=30) {
                                            messageOnline.setText("Угол крена автомобиля: " + (new DecimalFormat("##").format(angle_R))+" градусов");
                                            messageOnline.setTextColor(getResources().getColor(R.color.color_normal));
                                            boom1.setVisibility(View.GONE);
                                        }else {
                                            messageOnline.setText("Угол крена автомобиля критический!");
                                            messageOnline.setTextColor(getResources().getColor(R.color.color_danger));
                                            boom1.setVisibility(View.VISIBLE);
                                        }

                                        if(shake >=1.1){
                                            messageOnline2.setText("Снизте уровень тряски!!! Опасно для хрупких грузов!");
                                            messageOnline2.setTextColor(getResources().getColor(R.color.color_danger));
                                            shakeOld=shake;
                                        }else if ((shakeOld - shake)>=0.4){
                                            messageOnline2.setText("Уровень вибраций и тряски благоприятен для хрупких грузов! ");
                                            messageOnline2.setTextColor(getResources().getColor(R.color.color_normal1));
                                        }
                                        buffer.delete(0, buffer.length());
                                    } catch (NumberFormatException e) {

                                    }
                                }
                            }
                        });
                    }
                }catch (IOException e){
                   // e.printStackTrace();
                }
            }
            try{
                bis.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        public  void  cancel(){
            try{
                isConnected = false;
                inputStream.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
