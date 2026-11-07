package com.c5x.trafficdisplay;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.view.WindowManager;
import android.view.Gravity;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.RectF;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.UUID;

public class BluetoothHudService extends Service {
    // Must match UUID in phone-sender.
    public static final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private volatile boolean running = true;
    private BluetoothServerSocket server;
    private WindowManager wm;
    private HudView hud;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        main.post(new Runnable(){ public void run(){ showHud(); }});
        new Thread(new Runnable(){ public void run(){ listenLoop(); }},"C5X-SPP-Server").start();
    }
    private void showHud() {
        if (hud != null) return;
        hud = new HudView(this);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            330, 150, WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.RIGHT; p.x=18; p.y=70;
        try { wm.addView(hud,p); } catch(Exception e) { hud=null; }
    }
    private void listenLoop() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return;
        while (running) {
            BluetoothSocket socket = null;
            try {
                server = adapter.listenUsingRfcommWithServiceRecord("C5X Traffic HUD", APP_UUID);
                socket = server.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                String line;
                while (running && (line=in.readLine())!=null) {
                    final String data=line;
                    main.post(new Runnable(){ public void run(){ if(hud!=null) hud.setPacket(data); }});
                }
            } catch(Exception e) {
                try { Thread.sleep(1200); } catch(InterruptedException ignored) {}
            } finally {
                try { if(socket!=null) socket.close(); } catch(Exception ignored) {}
                try { if(server!=null) server.close(); } catch(Exception ignored) {}
            }
        }
    }
    @Override public int onStartCommand(Intent i,int f,int id){ return START_STICKY; }
    @Override public void onDestroy() {
        running=false;
        try { if(server!=null) server.close(); } catch(Exception ignored) {}
        if(hud!=null) try { wm.removeView(hud); } catch(Exception ignored) {}
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i){ return null; }

    static class HudView extends View {
        Paint p=new Paint(3); String light="WAIT", turn="等待手机连接"; int seconds=0; long received=0;
        HudView(android.content.Context c){ super(c); setLayerType(View.LAYER_TYPE_SOFTWARE,null); }
        void setPacket(String s) {
            String[] a=s.split("\\|");
            if(a.length<3) return;
            if(!("RED".equals(a[0])||"GREEN".equals(a[0])||"YELLOW".equals(a[0]))) return;
            try { int n=Integer.parseInt(a[1]); if(n<0||n>180)return; seconds=n; } catch(Exception e){return;}
            light=a[0]; turn=a[2]; received=System.currentTimeMillis(); invalidate();
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); float w=getWidth(), h=getHeight();
            p.setColor(0xe6102d3d); p.setStyle(Paint.Style.FILL);
            c.drawRoundRect(new RectF(0,0,w,h),18,18,p);
            p.setColor(0xff60d9f5); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
            c.drawRoundRect(new RectF(1,1,w-1,h-1),18,18,p); p.setStyle(Paint.Style.FILL);
            p.setTextSize(13); p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            c.drawText("C5X TRAFFIC HUD",14,22,p);
            int active="RED".equals(light)?0:("YELLOW".equals(light)?1:2);
            int[] colors={0xfff04444,0xffe9b949,0xff20b66b};
            for(int i=0;i<3;i++){ p.setColor(i==active?colors[i]:0xff46545c); c.drawCircle(25,48+i*27,9,p); }
            p.setColor(0xfff3fbff); p.setTextSize(38); c.drawText(received==0?"--":String.valueOf(seconds),52,91,p);
            p.setTextSize(13); p.setColor(0xff60d9f5); c.drawText("SEC",112,90,p);
            p.setTextSize(16); p.setColor(0xffd8f4ff);
            String t="LEFT".equals(turn)?"↰":("RIGHT".equals(turn)?"↱":("STRAIGHT".equals(turn)?"↑":"—"));
            c.drawText(t, w-48,76,p);
            p.setTextSize(10); p.setColor(0xffa8c5d2);
            c.drawText(received==0?"WAITING FOR DATA":(System.currentTimeMillis()-received>3000?"DATA STALE":"LIVE LINK"),14,h-10,p);
            if(received>0 && System.currentTimeMillis()-received>3000) {
                p.setColor(0xff8b969d); p.setTextSize(11); c.drawText("数据超时",w-65,h-10,p);
            }
        }
    }
}
