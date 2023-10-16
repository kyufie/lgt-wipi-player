package android.lgt.wipi;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Message;
import android.util.Log;
import android.view.View;
import java.nio.ShortBuffer;
/* loaded from: classes.dex */
public class FrameSurfaceView extends View {
    public static final int WIPI_PLATFORM_ANN_HEIGHT = 24;
    public static int WIPI_PLATFORM_LCD_HEIGHT;
    public static int WIPI_PLATFORM_LCD_WIDTH;
    public Bitmap _bitmap;
    private int canvas_height;
    private int canvas_width;
    private int dst_height;
    private int dst_width;
    private int dst_x;
    private int dst_y;
    private Activity mActivity;
    private boolean mFirstFlush;
    private boolean mInitDrawArea;
    private boolean mIsAppInited;
    private boolean mIsPortrait;
    private boolean mUseAnn;
    private int src_height;
    private int src_width;
    private int src_x;
    private int src_y;

    private native boolean isUseAnn();

    public native boolean isPortrait();

    public FrameSurfaceView(Context context) {
        super(context);
        this.mInitDrawArea = false;
        this.mIsAppInited = false;
        this.mUseAnn = false;
        this.mFirstFlush = true;
        this.mIsPortrait = true;
        this.mActivity = (Activity) context;
    }

    public boolean didFirstFlush() {
        return this.mFirstFlush;
    }

    public void initBitmap() {
        if (this._bitmap == null) {
            Log.v(WipiPlayer.TAG, "initBitmap() isPortrait : " + isPortrait());
            if (isPortrait()) {
                this.mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                WIPI_PLATFORM_LCD_WIDTH = 240;
                WIPI_PLATFORM_LCD_HEIGHT = 400;
            } else {
                this.mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                WIPI_PLATFORM_LCD_WIDTH = 400;
                WIPI_PLATFORM_LCD_HEIGHT = 240;
            }
            if (isPortrait() != this.mIsPortrait) {
                this.canvas_height = 0;
                this.canvas_width = 0;
                this.mIsPortrait = isPortrait();
            }
            WipiEventManager.setLcdSize(this.mActivity.getWindowManager().getDefaultDisplay().getWidth(), this.mActivity.getWindowManager().getDefaultDisplay().getHeight());
            this._bitmap = Bitmap.createBitmap(WIPI_PLATFORM_LCD_WIDTH, WIPI_PLATFORM_LCD_HEIGHT, Bitmap.Config.RGB_565);
        }
    }

    public void flushFrame(ShortBuffer buf, int x1, int y1, int x2, int y2) {
        int dy1;
        int dy2;
        initBitmap();
        if (!this.mInitDrawArea) {
            setDrawArea();
            this.mInitDrawArea = true;
        }
        synchronized (this._bitmap) {
            this._bitmap.copyPixelsFromBuffer(buf);
        }
        this.mIsAppInited = true;
        if (this.mInitDrawArea) {
            if (this.mUseAnn) {
                y1 -= 24;
                if (y1 <= 0) {
                    y1 = 0;
                }
                y2 -= 24;
                if (y2 <= 0) {
                    y2 = 0;
                }
            }
            int dx1 = (this.canvas_width * x1) / WIPI_PLATFORM_LCD_WIDTH;
            int dx2 = (this.canvas_width * x2) / WIPI_PLATFORM_LCD_WIDTH;
            if (this.mUseAnn) {
                dy1 = (this.canvas_height * y1) / (WIPI_PLATFORM_LCD_HEIGHT - 24);
                dy2 = (this.canvas_height * y2) / (WIPI_PLATFORM_LCD_HEIGHT - 24);
            } else {
                dy1 = (this.canvas_height * y1) / WIPI_PLATFORM_LCD_HEIGHT;
                dy2 = (this.canvas_height * y2) / WIPI_PLATFORM_LCD_HEIGHT;
            }
            if (this.mFirstFlush) {
                Log.v(WipiPlayer.TAG, "firstFlush!!!");
                this.mFirstFlush = false;
                Message msg = new Message();
                msg.arg1 = 7;
                ((WipiPlayer) this.mActivity).getHandler().sendMessage(msg);
                postInvalidate();
                return;
            }
            postInvalidate(dx1, dy1, dx2, dy2);
            return;
        }
        postInvalidate();
    }

    private void setDrawArea() {
        this.src_x = 0;
        if (!((WipiPlayer) this.mActivity).mBexited && isUseAnn()) {
            Message msg = new Message();
            msg.arg1 = 8;
            ((WipiPlayer) this.mActivity).getHandler().sendMessage(msg);
            this.src_y = 24;
            this.mUseAnn = true;
        } else {
            Message msg2 = new Message();
            msg2.arg1 = 1;
            ((WipiPlayer) this.mActivity).getHandler().sendMessage(msg2);
            this.src_y = 0;
            this.mUseAnn = false;
        }
        this.src_width = WIPI_PLATFORM_LCD_WIDTH;
        this.src_height = WIPI_PLATFORM_LCD_HEIGHT;
        this.dst_width = this.canvas_width;
        this.dst_height = this.canvas_height;
        this.dst_x = 0;
        this.dst_y = 0;
        Log.v(WipiPlayer.TAG, "setDrawArea(" + this.dst_width + ", " + this.dst_height + ") useAnn : " + isUseAnn());
        WipiEventManager.setAppSize(this.dst_width, this.dst_height);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        if (this.mIsAppInited && canvas != null) {
            synchronized (this._bitmap) {
                canvas.drawBitmap(this._bitmap, new Rect(this.src_x, this.src_y, this.src_width, this.src_height), new Rect(this.dst_x, this.dst_y, this.dst_width, this.dst_height), (Paint) null);
            }
        }
    }

    @Override // android.view.View
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (!isPortrait() || w <= h) {
            if (isPortrait() || w >= h) {
                super.onSizeChanged(w, h, oldw, oldh);
                this.mInitDrawArea = false;
                this.mFirstFlush = true;
                this.canvas_width = w;
                this.canvas_height = h;
                Log.v(WipiPlayer.TAG, "onSizeChanged " + w + ", " + h + "," + oldw + ", " + oldh);
                WipiEventManager.setLcdSize(this.mActivity.getWindowManager().getDefaultDisplay().getWidth(), this.mActivity.getWindowManager().getDefaultDisplay().getHeight());
            }
        }
    }

    public void setFirstFlush() {
        Log.v(WipiPlayer.TAG, "setFirstFlush ");
        this.mFirstFlush = true;
        this.mInitDrawArea = false;
        this.mIsAppInited = false;
        this._bitmap = null;
    }
}
