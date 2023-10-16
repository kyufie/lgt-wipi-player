package android.lgt.wipi;
/* compiled from: JavaThreadMaker.java */
/* loaded from: classes.dex */
class JavaThread implements Runnable {
    int _args;
    int _entryPoint;

    static native void runN(int i, int i2);

    public JavaThread(int entryPoint, int args) {
        this._entryPoint = entryPoint;
        this._args = args;
    }

    @Override // java.lang.Runnable
    public void run() {
        try {
            runN(this._entryPoint, this._args);
        } catch (Exception e) {
        }
    }
}
