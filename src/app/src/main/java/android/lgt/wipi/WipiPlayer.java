package android.lgt.wipi;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ShortBuffer;
import java.util.Enumeration;
import java.util.TimeZone;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
/* loaded from: classes.dex */
public class WipiPlayer extends Activity {
    public static final int MSG_CONTROL_3G_BLOCKED = 2;
    public static final int MSG_CONTROL_AIRPLANE_MODE = 6;
    public static final int MSG_CONTROL_BACKLIGHT = 0;
    public static final int MSG_CONTROL_HIDE_ANN_AREA = 1;
    public static final int MSG_CONTROL_NOT_SUBSCRIBED = 5;
    public static final int MSG_CONTROL_OUT_OF_SERVICE = 4;
    public static final int MSG_CONTROL_ROAMING_AREA = 3;
    public static final int MSG_CONTROL_SHOW_ANN_AREA = 8;
    public static final int MSG_ERROR_NO_SPACE = 8;
    public static final int MSG_FIRST_FLUSH = 7;
    public static final String TAG = "WIPIPlayer";
    private static final int WIPI_PLATFORM_STATE_BACKGROUND = 0;
    private static final int WIPI_PLATFORM_STATE_FOREGROUND = 1;
    private static ShortBuffer mBuf;
    static FrameSurfaceView mFrame;
    private static AlertDialog mPopup;
    private String mAid;
    private String mCurBatteryLevel;
    private Handler mMsghandler;
    private String mTimeZone;
    private String mWipiParam;
    private WipiMediaManager mediaMgr;
    private ProgressBar progressBar;
    private static boolean mIsBatteryPopupShown = false;
    private static boolean mIsFirstCheck = true;
    private static boolean mBpaused = false;
    private IntentFilter mIntentfilter = null;
    private BroadcastReceiver mReceiver = null;
    public boolean mBexited = false;

    public static native int pltEventN(int i, Object obj);

    public native int pltChangeStateN(int i);

    public native int startWipiN(String str, String[] strArr);

    static {
        Log.i(TAG, "=========== Starting WipiPlayer ===========");
        File lib = new File("/data/data/android.lgt.wipi/lib/liblgt_system.so");
        if (lib.exists()) {
            Log.i(TAG, "try to load /data/data/android.lgt.wipi/lib/liblgt_system.so");
            System.load("/data/data/android.lgt.wipi/lib/liblgt_system.so");
            return;
        }
        Log.i(TAG, "/data/data/android.lgt.wipi/lib/liblgt_system.so not exist");
        Log.i(TAG, "try to load /system/lib/liblgt_system.so");
        System.load("/system/lib/liblgt_system.so");
    }

    @Override // android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        init();
        String action = getIntent().getAction();
        if (action.equalsIgnoreCase("android.intent.action.LAUNCH_WIPI") || action.equalsIgnoreCase("android.intent.action.MAIN")) {
            WipiThread wipiThread = new WipiThread();
            wipiThread.start();
        } else if (action.equalsIgnoreCase("android.intent.action.UPDATE_APP")) {
            String packageName = getIntent().getStringExtra("PACKAGE_NAME");
            if (packageName.startsWith("android.lgt.wipi.App")) {
                String aid = packageName.substring("android.lgt.wipi.App".length());
                File fd = new File("/data/data/" + packageName + "/files", "binary.mod");
                fd.delete();
                File fd2 = new File("/data/data/" + packageName + "/files", String.valueOf(aid) + ".jar");
                fd2.delete();
            }
            finish();
        }
    }

    @Override // android.app.Activity
    protected void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();
    }

    @Override // android.app.Activity
    protected void onRestart() {
        Log.d(TAG, "onRestart()");
        super.onRestart();
    }

    @Override // android.app.Activity
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        if ((mPopup == null || !mPopup.isShowing()) && mBpaused && !this.mBexited) {
            pltChangeStateN(1);
            mBpaused = false;
        }
        if (!mFrame.didFirstFlush()) {
            this.progressBar.setVisibility(8);
            setContentView(mFrame);
        }
        if (this.mediaMgr != null) {
            this.mediaMgr.resumeSound();
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
        }
        setContentView(R.layout.main);
        this.progressBar = (ProgressBar) findViewById(16908301);
        this.progressBar.setVisibility(0);
        if ((mPopup == null || !mPopup.isShowing()) && !mBpaused && !this.mBexited) {
            pltChangeStateN(0);
            mBpaused = true;
        }
        if (this.mediaMgr != null) {
            this.mediaMgr.pauseSound();
        }
        ActivityManager am = (ActivityManager) getSystemService("activity");
        am.killBackgroundProcesses("com.lgt.arm");
    }

    @Override // android.app.Activity
    protected void onStop() {
        Log.d(TAG, "onStop()");
        super.onStop();
        if (this.mediaMgr != null) {
            this.mediaMgr.stopSound();
        }
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        unregisterReceiver(this.mReceiver);
        ActivityManager am = (ActivityManager) getSystemService("activity");
        am.restartPackage(getPackageName());
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        Log.v(TAG, "OnNewIntent()");
        String intentAid = intent.getStringExtra("AID_PATH");
        String intentAid2 = intentAid.substring(0, intentAid.indexOf(".jar"));
        if (!intentAid2.equals(this.mAid)) {
            String action = intent.getAction();
            if (action.equalsIgnoreCase("android.intent.action.LAUNCH_WIPI") || action.equalsIgnoreCase("android.intent.action.MAIN")) {
                this.mAid = intentAid2;
                this.mWipiParam = "/android/" + this.mAid + ".jar:binary.mod";
                pltEventN(34, this.mWipiParam);
            }
            mFrame.setFirstFlush();
            if (this.mediaMgr != null) {
                this.mediaMgr.finalize();
                this.mediaMgr.init(this.mAid);
            }
            super.onNewIntent(intent);
        }
    }

    @Override // android.app.Activity, android.view.KeyEvent.Callback
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown()");
        AudioManager audio = (AudioManager) getSystemService("audio");
        switch (keyCode) {
            case 4:
                showAlertDialog("종료", "애플리케이션을 종료하시겠습니까?", "예", "아니요", null, new DialogInterface.OnClickListener() { // from class: android.lgt.wipi.WipiPlayer.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int arg1) {
                        WipiPlayer.this.exitPlatform(true);
                    }
                }, new DialogInterface.OnClickListener() { // from class: android.lgt.wipi.WipiPlayer.2
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int arg1) {
                        if (WipiPlayer.mBpaused && !WipiPlayer.this.mBexited) {
                            WipiPlayer.this.pltChangeStateN(1);
                            WipiPlayer.mBpaused = false;
                        }
                    }
                }, null);
                break;
            case FrameSurfaceView.WIPI_PLATFORM_ANN_HEIGHT /* 24 */:
                audio.adjustStreamVolume(3, 1, 1);
                return true;
            case 25:
                audio.adjustStreamVolume(3, -1, 1);
                return true;
            default:
                WipiEventManager.onKeyEvent(2, keyCode);
                break;
        }
        return false;
    }

    @Override // android.app.Activity, android.view.KeyEvent.Callback
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyUp()");
        WipiEventManager.onKeyEvent(3, keyCode);
        return false;
    }

    @Override // android.app.Activity
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mBexited) {
            return false;
        }
        return WipiEventManager.onTouchEvent(event);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            WipiEventManager.setLcdSize(getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight());
        }
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void init() {
        requestWindowFeature(1);
        mFrame = new FrameSurfaceView(this);
        setContentView(R.layout.main);
        this.progressBar = (ProgressBar) findViewById(16908301);
        this.progressBar.setVisibility(0);
        mBuf = ShortBuffer.allocate(96000);
        this.mAid = getIntent().getStringExtra("AID_PATH");
        this.mAid = this.mAid.substring(0, this.mAid.indexOf(".jar"));
        this.mWipiParam = "/android/" + this.mAid + ".jar:binary.mod";
        FileInputStream dummyFs = null;
        try {
            FileInputStream dummyFs2 = openFileInput("dummy");
            if (dummyFs2 != null) {
                try {
                    dummyFs2.close();
                } catch (IOException e) {
                }
            }
        } catch (FileNotFoundException e2) {
            if (0 != 0) {
                try {
                    dummyFs.close();
                } catch (IOException e3) {
                }
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    dummyFs.close();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
        this.mCurBatteryLevel = "0";
        this.mTimeZone = TimeZone.getDefault().getDisplayName();
        this.mIntentfilter = new IntentFilter();
        this.mIntentfilter.addAction("android.intent.action.BATTERY_CHANGED");
        this.mIntentfilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        this.mReceiver = new BroadcastReceiver() { // from class: android.lgt.wipi.WipiPlayer.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                    if (!WipiPlayer.mIsBatteryPopupShown) {
                        int level = intent.getIntExtra("level", 0);
                        int scale = intent.getIntExtra("scale", 100);
                        int plugged = intent.getIntExtra("plugged", 0);
                        WipiPlayer.this.mCurBatteryLevel = String.valueOf((level * 100) / scale);
                        if (Integer.parseInt(WipiPlayer.this.mCurBatteryLevel) < 10 && plugged == 0) {
                            if (WipiPlayer.mIsFirstCheck) {
                                WipiPlayer.this.showAlertDialog("배터리 부족", "휴대폰 전원이 부족하여 애플리케이션을 실행할 수 없습니다", "확인", null, null, new DialogInterface.OnClickListener() { // from class: android.lgt.wipi.WipiPlayer.3.1
                                    @Override // android.content.DialogInterface.OnClickListener
                                    public void onClick(DialogInterface dialog, int arg1) {
                                        WipiPlayer.this.exitPlatform(true);
                                    }
                                }, null, null);
                            } else {
                                WipiPlayer.this.showAlertDialog("배터리 부족", "휴대폰 전원이 부족하여 애플리케이션을 종료합니다", "확인", null, null, new DialogInterface.OnClickListener() { // from class: android.lgt.wipi.WipiPlayer.3.2
                                    @Override // android.content.DialogInterface.OnClickListener
                                    public void onClick(DialogInterface dialog, int arg1) {
                                        WipiPlayer.this.exitPlatform(true);
                                    }
                                }, null, null);
                            }
                            WipiPlayer.mIsBatteryPopupShown = true;
                        }
                    }
                    WipiPlayer.mIsFirstCheck = false;
                } else if (action.equals("android.intent.action.TIMEZONE_CHANGED")) {
                    String tz = intent.getStringExtra("time-zone");
                    WipiPlayer.this.mTimeZone = TimeZone.getTimeZone(tz).getDisplayName();
                }
            }
        };
        registerReceiver(this.mReceiver, this.mIntentfilter);
        this.mMsghandler = new Handler() { // from class: android.lgt.wipi.WipiPlayer.4
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg != null) {
                    if (msg.arg1 == 0) {
                        WindowManager.LayoutParams lp = WipiPlayer.this.getWindow().getAttributes();
                        float value = ((Float) msg.obj).floatValue();
                        lp.screenBrightness = value == -99.0f ? -1.0f : value;
                        WipiPlayer.this.getWindow().setAttributes(lp);
                        if (value == -99.0f) {
                            WipiPlayer.this.getWindow().addFlags(128);
                        } else {
                            WipiPlayer.this.getWindow().clearFlags(128);
                        }
                    } else if (msg.arg1 == 1) {
                        WipiPlayer.this.getWindow().clearFlags(2048);
                        WipiPlayer.this.getWindow().setFlags(1024, 1024);
                    } else if (msg.arg1 == 8) {
                        WipiPlayer.this.getWindow().clearFlags(1024);
                        WipiPlayer.this.getWindow().setFlags(2048, 2048);
                    } else if (msg.arg1 == 2) {
                        WipiPlayer.this.showAlertDialog("3G 접속 차단", "3G 데이터 접속이 차단되었습니다. '설정>무선제어>3G 데이터 접속 설정'에서 설정 변경 후 사용할 수 있습니다.", "확인", "취소", null, new DialogInterface.OnClickListener() { // from class: android.lgt.wipi.WipiPlayer.4.1
                            @Override // android.content.DialogInterface.OnClickListener
                            public void onClick(DialogInterface dialog, int arg1) {
                                Intent intent = new Intent();
                                intent.setClassName("com.android.settings", "com.android.settings.WirelessSettings");
                                intent.setAction("android.settings.WIRELESS_SETTINGS.NETWORK_MODE");
                                WipiPlayer.this.startActivity(intent);
                            }
                        }, new DialogInterface.OnClickListener() { // from class: android.lgt.wipi.WipiPlayer.4.2
                            @Override // android.content.DialogInterface.OnClickListener
                            public void onClick(DialogInterface dialog, int arg1) {
                                if (WipiPlayer.mBpaused && !WipiPlayer.this.mBexited) {
                                    WipiPlayer.this.pltChangeStateN(1);
                                    WipiPlayer.mBpaused = false;
                                }
                            }
                        }, null);
                    } else if (msg.arg1 == 3) {
                        Toast.makeText(WipiPlayer.this, "해외로밍지역 서비스불가", 1).show();
                    } else if (msg.arg1 == 4) {
                        Toast.makeText(WipiPlayer.this, "연결된 네트워크 없음", 1).show();
                    } else if (msg.arg1 == 5) {
                        Toast.makeText(WipiPlayer.this, "미가입 단말 서비스불가", 1).show();
                    } else if (msg.arg1 == 6) {
                        Toast.makeText(WipiPlayer.this, "비행기 모드 서비스불가", 1).show();
                    } else if (msg.arg1 == 7) {
                        WipiPlayer.this.progressBar.setVisibility(8);
                        WipiPlayer.this.setContentView(WipiPlayer.mFrame);
                    } else if (msg.arg1 == 8) {
                        WipiPlayer.this.showAlertDialog("저장공간 부족", "휴대전화에서 저장 공간을 늘린 후에 다시 시도해주세요.", "확인", null, null, new DialogInterface.OnClickListener() { // from class: android.lgt.wipi.WipiPlayer.4.3
                            @Override // android.content.DialogInterface.OnClickListener
                            public void onClick(DialogInterface dialog, int arg1) {
                                WipiPlayer.this.finish();
                            }
                        }, null, null);
                    }
                }
            }
        };
    }

    public void showAlertDialog(String title, String msg, String positiveButton, String negativeButton, String neutralButton, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener, DialogInterface.OnClickListener neutralListener) {
        if (mPopup == null || !mPopup.isShowing()) {
            if (!mBpaused && !this.mBexited) {
                pltChangeStateN(0);
                mBpaused = true;
            }
            AlertDialog.Builder ad = new AlertDialog.Builder(this);
            ad.setTitle(title);
            ad.setMessage(msg);
            if (positiveButton != null && positiveListener != null) {
                ad.setPositiveButton(positiveButton, positiveListener);
            }
            if (negativeButton != null && negativeListener != null) {
                ad.setNegativeButton(negativeButton, negativeListener);
            }
            if (neutralButton != null && neutralListener != null) {
                ad.setNeutralButton(neutralButton, neutralListener);
            }
            ad.setCancelable(false);
            mPopup = ad.show();
        }
    }

    /* JADX WARN: Incorrect condition in loop: B:7:0x000f */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void exitPlatform(boolean r6) {
        /*
            r5 = this;
            r4 = 1
            r1 = 0
            boolean r2 = r5.mBexited
            if (r2 != 0) goto L11
            r5.mBexited = r4
            if (r6 == 0) goto L11
        La:
            r2 = 0
            int r1 = pltEventN(r4, r2)
            if (r1 != r4) goto L12
        L11:
            return
        L12:
            r2 = 100
            java.lang.Thread.sleep(r2)     // Catch: java.lang.InterruptedException -> L18
            goto La
        L18:
            r0 = move-exception
            r0.printStackTrace()
            goto La
        */
        throw new UnsupportedOperationException("Method not decompiled: android.lgt.wipi.WipiPlayer.exitPlatform(boolean):void");
    }

    public void finishWipiPlayer() {
        finish();
    }

    public static void flushBitmap(short[] framebuffer, int left, int top, int right, int bottom) {
        if (!mBpaused) {
            mBuf.put(framebuffer);
            mBuf.rewind();
            mFrame.flushFrame(mBuf, left, top, right, bottom);
        }
    }

    public void startWAPBrowser(String url) {
        Log.d(TAG, "startWAPBrowser()");
        try {
            Class<?> clsAppIntent = Class.forName("android.lgt.intents.AppIntent");
            String strAction = (String) clsAppIntent.getField("ACTION_OPEN_WAPURL").get(null);
            Intent intent = new Intent(strAction);
            intent.setData(Uri.parse("wapurl://" + url));
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "ClassNotFoundException : " + e.getMessage());
        } catch (IllegalAccessException e2) {
            Log.e(TAG, "IllegalAccessException : " + e2.getMessage());
        } catch (IllegalArgumentException e3) {
            Log.e(TAG, "IllegalArgumentException : " + e3.getMessage());
        } catch (NoSuchFieldException e4) {
            Log.e(TAG, "NoSuchFieldException : " + e4.getMessage());
        } catch (SecurityException e5) {
            Log.e(TAG, "SecurityException : " + e5.getMessage());
        } catch (Exception e6) {
            Log.e(TAG, "Exception : " + e6.getMessage());
        }
    }

    public void callPlace(String phonenum) {
        startActivity(new Intent("android.intent.action.CALL", Uri.parse("tel:" + phonenum)));
    }

    public static Context getContext() {
        return mFrame.getContext();
    }

    public Handler getHandler() {
        return this.mMsghandler;
    }

    public String getBatteryLevel() {
        return this.mCurBatteryLevel;
    }

    public String getTimeZone() {
        return this.mTimeZone;
    }

    /* loaded from: classes.dex */
    class WipiThread extends Thread {
        WipiThread() {
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            try {
                Log.d(WipiPlayer.TAG, "AID : " + WipiPlayer.this.mAid);
                WipiPlayer.this.mediaMgr = WipiMediaManager.getInstance();
                WipiPlayer.this.mediaMgr.init(WipiPlayer.this.mAid);
                WipiPlayer.this.startWipiN(WipiPlayer.this.mWipiParam, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean inflateJar(String jarfilename, String destPath, FileFilter filter) {
        try {
            JarFile jarfile = new JarFile(jarfilename);
            Enumeration<JarEntry> entries = jarfile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String filename = entry.getName();
                if (filter == null || filter.accept(new File(filename))) {
                    File file = new File(String.valueOf(destPath) + filename);
                    if (!file.exists()) {
                        if (entry.isDirectory()) {
                            file.mkdirs();
                        } else {
                            file.getParentFile().mkdirs();
                            file.createNewFile();
                            FileOutputStream fos = new FileOutputStream(file);
                            InputStream is = jarfile.getInputStream(entry);
                            long size = entry.getSize();
                            int totalReadBytes = 0;
                            byte[] data = new byte[(int) (1 + size)];
                            while (true) {
                                int readBytes = is.read(data, totalReadBytes, data.length - totalReadBytes);
                                if (readBytes == -1) {
                                    break;
                                }
                                fos.write(data, totalReadBytes, readBytes);
                                totalReadBytes += readBytes;
                            }
                            fos.close();
                            is.close();
                        }
                    }
                }
            }
            jarfile.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
