package com.c5x.trafficsender;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final UUID APP_UUID=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter adapter;
    private BluetoothSocket socket;
    private OutputStream out;
    private Spinner devices;
    private TextView status;
    private ArrayList<BluetoothDevice> paired=new ArrayList<BluetoothDevice>();

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        adapter=BluetoothAdapter.getDefaultAdapter();
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(24,24,24,24);
        root.setBackgroundColor(0xff071827);
        TextView title=new TextView(this); title.setText("C5X TRAFFIC SENDER"); title.setTextColor(0xff60d9f5); title.setTextSize(23); root.addView(title);
        status=new TextView(this); status.setText("选择已配对的 C5X 车机"); status.setTextColor(0xffd7eaf2); status.setPadding(0,20,0,14); root.addView(status);
        devices=new Spinner(this); root.addView(devices);
        Button refresh=new Button(this); refresh.setText("刷新已配对设备"); root.addView(refresh);
        refresh.setOnClickListener(new View.OnClickListener(){public void onClick(View v){loadDevices();}});
        Button connect=new Button(this); connect.setText("连接车机"); root.addView(connect);
        connect.setOnClickListener(new View.OnClickListener(){public void onClick(View v){connectDevice();}});
        addSendButton(root,"发送红灯 20 秒","RED|20|STRAIGHT");
        addSendButton(root,"发送黄灯 3 秒","YELLOW|3|STRAIGHT");
        addSendButton(root,"发送绿灯 28 秒","GREEN|28|RIGHT");
        addSendButton(root,"发送绿灯左转","GREEN|18|LEFT");
        setContentView(root);
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN},77);
        else loadDevices();
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g); if(r==77)loadDevices();}
    private void loadDevices(){
        if(adapter==null){status.setText("设备没有蓝牙适配器");return;}
        try{
            paired.clear(); ArrayList<String> names=new ArrayList<String>();
            Set<BluetoothDevice> set=adapter.getBondedDevices();
            for(BluetoothDevice d:set){paired.add(d); names.add(d.getName()+"  ("+d.getAddress()+")");}
            if(names.size()==0) names.add("没有已配对设备，请先在系统设置配对");
            devices.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));
            status.setText("找到已配对设备："+paired.size());
        }catch(SecurityException e){status.setText("请授予附近设备/蓝牙权限");}
    }
    private void connectDevice(){
        if(paired.size()==0){status.setText("没有已配对设备");return;}
        final BluetoothDevice d=paired.get(devices.getSelectedItemPosition());
        status.setText("正在连接 "+d.getName()+" …");
        new Thread(new Runnable(){public void run(){
            try{
                if(adapter.isDiscovering()) adapter.cancelDiscovery();
                BluetoothSocket s=d.createRfcommSocketToServiceRecord(APP_UUID);
                s.connect(); socket=s; out=s.getOutputStream();
                runOnUiThread(new Runnable(){public void run(){status.setText("SPP 已连接，可以发送测试数据");}});
            }catch(final Exception e){runOnUiThread(new Runnable(){public void run(){status.setText("连接失败："+e.getMessage()+"。请确认车机端已启动接收服务。");}});}
        }}).start();
    }
    private void addSendButton(LinearLayout root,String label,final String packet){
        Button b=new Button(this); b.setText(label); root.addView(b);
        b.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            new Thread(new Runnable(){public void run(){
                try{ if(out==null)throw new Exception("尚未连接"); out.write((packet+"\n").getBytes("UTF-8")); out.flush();
                    runOnUiThread(new Runnable(){public void run(){status.setText("已发送："+packet);}});
                }catch(final Exception e){runOnUiThread(new Runnable(){public void run(){status.setText("发送失败："+e.getMessage());}});}
            }}).start();
        }});
    }
    @Override protected void onDestroy(){super.onDestroy();try{if(socket!=null)socket.close();}catch(Exception ignored){}}
}
