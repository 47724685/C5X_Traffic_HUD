package com.c5x.trafficdisplay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(28, 24, 28, 24);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(0xff071827);
        TextView title = new TextView(this);
        title.setText("C5X TRAFFIC HUD  |  Android 4.4+");
        title.setTextColor(0xff66d9f5); title.setTextSize(22);
        box.addView(title);
        TextView info = new TextView(this);
        info.setText("先启动接收服务，再从手机端连接。\n本版本用于验证蓝牙 SPP 与悬浮窗。");
        info.setTextColor(0xffd7eaf2); info.setTextSize(16);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-2, -2); ip.topMargin=20;
        box.addView(info, ip);
        Button start = new Button(this); start.setText("启动接收与悬浮窗");
        start.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            startService(new Intent(MainActivity.this, BluetoothHudService.class));
            info.setText("接收服务已启动。等待手机通过蓝牙 SPP 连接…");
        }});
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2); bp.topMargin=24;
        box.addView(start, bp);
        setContentView(box);
    }
}
