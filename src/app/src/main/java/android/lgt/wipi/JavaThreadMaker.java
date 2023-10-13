package android.lgt.wipi;
/* loaded from: classes.dex */
public class JavaThreadMaker {
    Thread _thread;

    public JavaThreadMaker(int entryPoint, int args) {
        Runnable runnable = new JavaThread(entryPoint, args);
        this._thread = new Thread(runnable);
    }

    public void startThread() {
        this._thread.start();
    }
}
