package android.lgt.wipi;

import android.view.MotionEvent;
/* loaded from: classes.dex */
public class WipiEventManager {
    public static final int LGTH_POINTER_DOWN = 0;
    public static final int LGTH_POINTER_EVENT = 52;
    public static final int LGTH_POINTER_MOVE = 2;
    public static final int LGTH_POINTER_REPEAT = 3;
    public static final int LGTH_POINTER_UP = 1;
    public static final int MH_BROWSER_EVENT = 34;
    public static final int MH_EXIT_EVENT = 1;
    private static final int MH_KEY_0 = 48;
    private static final int MH_KEY_1 = 49;
    private static final int MH_KEY_2 = 50;
    private static final int MH_KEY_3 = 51;
    private static final int MH_KEY_4 = 52;
    private static final int MH_KEY_5 = 53;
    private static final int MH_KEY_6 = 54;
    private static final int MH_KEY_7 = 55;
    private static final int MH_KEY_8 = 56;
    private static final int MH_KEY_9 = 57;
    private static final int MH_KEY_CLEAR = -16;
    private static final int MH_KEY_DOWN = -2;
    private static final int MH_KEY_LEFT = -3;
    public static final int MH_KEY_PRESSEVENT = 2;
    public static final int MH_KEY_RELEASEEVENT = 3;
    public static final int MH_KEY_REPEATEVENT = 4;
    private static final int MH_KEY_RIGHT = -4;
    private static final int MH_KEY_SELECT = -5;
    private static final int MH_KEY_SOFT1 = -6;
    private static final int MH_KEY_UP = -1;
    private static float mAppHeight;
    private static float mAppWidth;
    private static float mHeight;
    private static float mWidth;

    public static void setLcdSize(int w, int h) {
        mWidth = w;
        mHeight = h;
    }

    public static void setAppSize(int w, int h) {
        mAppWidth = w;
        mAppHeight = h;
    }

    public static boolean onKeyEvent(int state, int keyCode) {
        int[] key = new int[1];
        switch (keyCode) {
            case WipiPlayer.MSG_FIRST_FLUSH /* 7 */:
                key[0] = MH_KEY_0;
                break;
            case 8:
                key[0] = MH_KEY_1;
                break;
            case 9:
                key[0] = MH_KEY_2;
                break;
            case 10:
                key[0] = MH_KEY_3;
                break;
            case 11:
                key[0] = 52;
                break;
            case 12:
                key[0] = MH_KEY_5;
                break;
            case 13:
                key[0] = MH_KEY_6;
                break;
            case 14:
                key[0] = MH_KEY_7;
                break;
            case 15:
                key[0] = MH_KEY_8;
                break;
            case 16:
                key[0] = MH_KEY_9;
                break;
            case 19:
                key[0] = MH_KEY_UP;
                break;
            case 20:
                key[0] = MH_KEY_DOWN;
                break;
            case 21:
                key[0] = MH_KEY_LEFT;
                break;
            case 22:
                key[0] = MH_KEY_RIGHT;
                break;
            case 23:
                key[0] = MH_KEY_SELECT;
                break;
            case 28:
            case 67:
                key[0] = MH_KEY_CLEAR;
                break;
            case 82:
                key[0] = MH_KEY_SOFT1;
                break;
            default:
                return false;
        }
        int ret = WipiPlayer.pltEventN(state, key);
        return ret == 1;
    }

    public static boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int[] pos = new int[3];
        int posX = (int) event.getX();
        int posY = (int) event.getY();
        pos[1] = (int) ((posX / mWidth) * FrameSurfaceView.WIPI_PLATFORM_LCD_WIDTH);
        if (mAppHeight < mHeight) {
            int gab = (int) (mHeight - mAppHeight);
            int posY2 = posY - gab;
            if (posY2 < 0) {
                posY2 = 0;
            }
            pos[2] = (int) ((posY2 / (mHeight - gab)) * (FrameSurfaceView.WIPI_PLATFORM_LCD_HEIGHT - 24));
        } else {
            pos[2] = (int) ((posY / mHeight) * FrameSurfaceView.WIPI_PLATFORM_LCD_HEIGHT);
        }
        switch (action) {
            case 0:
                pos[0] = 0;
                WipiPlayer.pltEventN(52, pos);
                break;
            case 1:
                pos[0] = 1;
                WipiPlayer.pltEventN(52, pos);
                break;
            case 2:
                pos[0] = 2;
                WipiPlayer.pltEventN(52, pos);
                break;
        }
        return false;
    }
}
