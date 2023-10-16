package android.lgt.wipi;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.util.Log;
import java.lang.reflect.Method;
/* loaded from: classes.dex */
public class HandsetManager {
    private static Method mGetSysemProperty = null;
    private static HandsetManager mInstance;
    boolean bLGUPAPI;
    public String mBaseId;
    public String mBaseLat;
    public String mBaseLong;
    private Activity mContext;
    public String mDeviceGrouop;
    public String mEsn;
    private Handler mMsgHandler;
    public String mNid;
    public String mPhoneNumber;
    public String mProcessorInfo;
    public String mSid;
    private Vibrator mVb;

    private HandsetManager(Object context) {
        this.mContext = null;
        this.mVb = null;
        this.mMsgHandler = null;
        this.mPhoneNumber = null;
        this.mEsn = null;
        this.mNid = null;
        this.mSid = null;
        this.mBaseId = null;
        this.mBaseLat = null;
        this.mBaseLong = null;
        this.mDeviceGrouop = null;
        this.mProcessorInfo = null;
        this.bLGUPAPI = false;
        this.mContext = (Activity) context;
        this.mVb = (Vibrator) this.mContext.getSystemService(Context.VIBRATOR_SERVICE);
        this.mMsgHandler = ((WipiPlayer) this.mContext).getHandler();
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            this.mNid = String.valueOf(((CdmaCellLocation) tm.getCellLocation()).getNetworkId());
            this.mSid = String.valueOf(((CdmaCellLocation) tm.getCellLocation()).getSystemId());
            this.mBaseId = String.valueOf(((CdmaCellLocation) tm.getCellLocation()).getBaseStationId());
            this.mBaseLat = String.valueOf(((CdmaCellLocation) tm.getCellLocation()).getBaseStationLatitude());
            this.mBaseLong = String.valueOf(((CdmaCellLocation) tm.getCellLocation()).getBaseStationLongitude());
        } catch (Exception e) {
            Log.w(WipiPlayer.TAG, e.getMessage() == null ? "NullPointerException" : e.getMessage());
            this.mNid = "";
            this.mSid = "";
            this.mBaseId = "";
            this.mBaseLat = "";
            this.mBaseLong = "";
        }
        this.mPhoneNumber = tm.getLine1Number();
        if (mGetSysemProperty == null) {
            try {
                mGetSysemProperty = Class.forName("android.lgt.handset.HandsetProperty").getMethod("LGUP_getSystemProperty", Class.forName("java.lang.String"));
                this.bLGUPAPI = true;
            } catch (ClassNotFoundException e2) {
                Log.i(WipiPlayer.TAG, "ClassNotFoundException : " + e2.getMessage());
                try {
                    mGetSysemProperty = Class.forName("android.os.SystemProperties").getMethod("get", Class.forName("java.lang.String"));
                } catch (ClassNotFoundException e3) {
                    Log.w(WipiPlayer.TAG, "ClassNotFoundException : " + e2.getMessage());
                    return;
                } catch (NoSuchMethodException e4) {
                    Log.w(WipiPlayer.TAG, "NoSuchMethodException : " + e2.getMessage());
                    return;
                } catch (SecurityException e5) {
                    Log.w(WipiPlayer.TAG, "SecurityException : " + e2.getMessage());
                    return;
                }
            } catch (NoSuchMethodException e6) {
                Log.i(WipiPlayer.TAG, "NoSuchMethodException : " + e6.getMessage());
                try {
                    mGetSysemProperty = Class.forName("android.os.SystemProperties").getMethod("get", Class.forName("java.lang.String"));
                } catch (ClassNotFoundException e7) {
                    Log.w(WipiPlayer.TAG, "ClassNotFoundException : " + e6.getMessage());
                    return;
                } catch (NoSuchMethodException e8) {
                    Log.w(WipiPlayer.TAG, "NoSuchMethodException : " + e6.getMessage());
                    return;
                } catch (SecurityException e9) {
                    Log.w(WipiPlayer.TAG, "SecurityException : " + e6.getMessage());
                    return;
                }
            } catch (SecurityException e10) {
                Log.w(WipiPlayer.TAG, "SecurityException : " + e10.getMessage());
                return;
            }
        }
        if (!this.bLGUPAPI) {
            try {
                this.mEsn = (String) mGetSysemProperty.invoke(null, "ril.cdma.phone.id");
                this.mDeviceGrouop = Settings.Secure.getString(this.mContext.getContentResolver(), "lgtwipi_device_group");
                this.mProcessorInfo = Settings.Secure.getString(this.mContext.getContentResolver(), "lgtwipi_processorinfo");
                return;
            } catch (Exception e11) {
                return;
            }
        }
        try {
            this.mEsn = (String) mGetSysemProperty.invoke(null, "ESN");
            this.mDeviceGrouop = (String) mGetSysemProperty.invoke(null, "DEVICE_GROUP");
            this.mProcessorInfo = (String) mGetSysemProperty.invoke(null, "PROCESSOR_INFO");
        } catch (Exception e12) {
        }
    }

    public static HandsetManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new HandsetManager(context);
        }
        return mInstance;
    }

    public String getDns1() {
        if (this.bLGUPAPI) {
            try {
                String mDns1 = (String) mGetSysemProperty.invoke(null, "DNS1");
                return mDns1;
            } catch (Exception e) {
                return null;
            }
        }
        try {
            String mDns12 = (String) mGetSysemProperty.invoke(null, "net.rmnet0.dns1");
            return mDns12;
        } catch (Exception e2) {
            return null;
        }
    }

    public String getDns2() {
        if (this.bLGUPAPI) {
            try {
                String mDns2 = (String) mGetSysemProperty.invoke(null, "DNS2");
                return mDns2;
            } catch (Exception e) {
                return null;
            }
        }
        try {
            String mDns22 = (String) mGetSysemProperty.invoke(null, "net.rmnet0.dns2");
            return mDns22;
        } catch (Exception e2) {
            return null;
        }
    }

    public String getCurBatteryLevel() {
        return ((WipiPlayer) this.mContext).getBatteryLevel();
    }

    public String getTimeZone() {
        return ((WipiPlayer) this.mContext).getTimeZone();
    }

    public String getCurrentCh() {
        try {
            return this.bLGUPAPI ? (String) mGetSysemProperty.invoke(null, "CURRENTCH") : (String) mGetSysemProperty.invoke(null, "ril.cdma.currentch");
        } catch (Exception e) {
            return null;
        }
    }

    public String getBestPn() {
        try {
            return this.bLGUPAPI ? (String) mGetSysemProperty.invoke(null, "BESTPN") : (String) mGetSysemProperty.invoke(null, "ril.cdma.bestpn");
        } catch (Exception e) {
            return null;
        }
    }

    public String getRoamingArea() {
        try {
            if (this.bLGUPAPI) {
                return (String) mGetSysemProperty.invoke(null, "ROAMING_AREA");
            }
        } catch (Exception e) {
        }
        return null;
    }

    public String getBillGateway() {
        String gw = null;
        if (this.bLGUPAPI) {
            try {
                gw = (String) mGetSysemProperty.invoke(null, "BILL_GW_IP");
            } catch (Exception e) {
            }
        } else {
            gw = Settings.Secure.getString(this.mContext.getContentResolver(), "lgtwipi_bill_gw_ip");
        }
        if (gw == null) {
            return null;
        }
        return gw;
    }

    public String isAirplaneMode() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0 ? "1" : "0";
    }

    public String isDataNetworkLocked() {
        String mobileState;
        String mobileState2 = null;
        if (this.bLGUPAPI) {
            try {
                mobileState2 = (String) mGetSysemProperty.invoke(null, "PREFERRED_DATA_NETWORK_MODE");
            } catch (Exception e) {
            }
        } else {
            mobileState2 = Settings.Secure.getString(this.mContext.getContentResolver(), "lgt_data_network_3g_state");
        }
        if (mobileState2 == null) {
            return null;
        }
        if (mobileState2.equals("0")) {
            mobileState = "1";
        } else {
            mobileState = "0";
        }
        return mobileState;
    }

    public boolean isSubscribed() {
        String reg = null;
        String auth = null;
        if (this.bLGUPAPI) {
            try {
                reg = (String) mGetSysemProperty.invoke(null, "REG");
                auth = (String) mGetSysemProperty.invoke(null, "AUTH");
            } catch (Exception e) {
            }
        }
        return reg != null && auth != null && reg.equals("1") && auth.equals("1");
    }

    public void setVibrator(long milliseconds) {
        this.mVb.vibrate(milliseconds);
    }

    public void setBackLight(float value) {
        Message msg = new Message();
        msg.arg1 = 0;
        msg.obj = new Float(value);
        this.mMsgHandler.sendMessage(msg);
    }
}
