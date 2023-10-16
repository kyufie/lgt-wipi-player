package android.lgt.wipi;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
/* loaded from: classes.dex */
public class WipiMediaManager {
    private static WipiMediaManager mInstance;
    private String privatePath;
    private SoundPool soundPool;
    private float volume = 1.0f;
    private int streamID = 0;
    private AudioManager audioMgr = (AudioManager) WipiPlayer.getContext().getSystemService("audio");
    private int maxVolume = this.audioMgr.getStreamMaxVolume(3);
    private Hashtable<String, Integer> mediaTable = new Hashtable<>();
    private MediaPlayer mediaPlayer = new MediaPlayer();

    WipiMediaManager() {
    }

    public void finalize() {
        this.mediaPlayer.reset();
        this.mediaTable.clear();
    }

    public static WipiMediaManager getInstance() {
        if (mInstance == null) {
            mInstance = new WipiMediaManager();
        }
        return mInstance;
    }

    public void init(String aid) {
        new SoundLoader(aid).start();
    }

    public int playSound(String filename, boolean repeat) {
        if (filename.indexOf(".wav") != -1) {
            filename = String.valueOf(filename.substring(0, filename.indexOf(".wav"))) + ".ogg";
        }
        synchronized (this) {
            setVolume((int) (this.volume * 100.0f));
            if (repeat) {
                this.mediaPlayer.reset();
                try {
                    try {
                        FileInputStream fin = new FileInputStream(String.valueOf(this.privatePath) + filename);
                        this.mediaPlayer.setDataSource(fin.getFD());
                        this.mediaPlayer.prepare();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e2) {
                    e2.printStackTrace();
                } catch (IllegalArgumentException e3) {
                    e3.printStackTrace();
                }
                this.mediaPlayer.setLooping(true);
                Log.v(WipiPlayer.TAG, "[media start] " + filename);
                this.mediaPlayer.start();
                return 0;
            }
            Integer sid = this.mediaTable.get(String.valueOf(this.privatePath) + filename);
            if (sid == null) {
                return -1;
            }
            int soundID = sid.intValue();
            Log.v(WipiPlayer.TAG, "[media play] " + filename);
            this.streamID = this.soundPool.play(soundID, this.volume, this.volume, 0, 0, 1.0f);
            return this.streamID == 0 ? -1 : 0;
        }
    }

    public int playSound(boolean repeat) {
        synchronized (this) {
            setVolume((int) (this.volume * 100.0f));
            this.mediaPlayer.setLooping(repeat);
            this.mediaPlayer.start();
        }
        return 0;
    }

    public int setSoundBuffer(byte[] buf) {
        int i;
        synchronized (this) {
            try {
                try {
                    setVolume((int) (this.volume * 100.0f));
                    this.mediaPlayer.reset();
                    try {
                        try {
                            try {
                                try {
                                    try {
                                        Class.forName("android.media.MediaPlayer").getMethod("setDataSource", byte[].class).invoke(this.mediaPlayer, buf);
                                        this.mediaPlayer.prepare();
                                    } catch (IOException e) {
                                        Log.e(WipiPlayer.TAG, "IOException : " + e.getMessage());
                                        return -1;
                                    }
                                } catch (SecurityException e2) {
                                    Log.e(WipiPlayer.TAG, "SecurityException : " + e2.getMessage());
                                    i = -1;
                                }
                            } catch (InvocationTargetException e3) {
                                Log.e(WipiPlayer.TAG, "InvocationTargetException : " + e3.getMessage());
                                i = -1;
                            }
                        } catch (NoSuchMethodException e4) {
                            Log.e(WipiPlayer.TAG, "NoSuchMethodException : " + e4.getMessage());
                            i = -1;
                        }
                    } catch (ClassNotFoundException e5) {
                        Log.e(WipiPlayer.TAG, "ClassNotFoundException : " + e5.getMessage());
                        i = -1;
                    } catch (IllegalAccessException e6) {
                        Log.e(WipiPlayer.TAG, "IllegalAccessException : " + e6.getMessage());
                        i = -1;
                    }
                } catch (IllegalArgumentException e7) {
                    Log.e(WipiPlayer.TAG, "IllegalArgumentException : " + e7.getMessage());
                    return -1;
                }
            } catch (IllegalStateException e8) {
                Log.e(WipiPlayer.TAG, "IllegalStateException : " + e8.getMessage());
                return -1;
            }
        }
        i = 0;
        return i;
    }

    public void pauseSound() {
        synchronized (this) {
            try {
                this.soundPool.pause(this.streamID);
                if (this.mediaPlayer.isPlaying()) {
                    this.mediaPlayer.pause();
                }
            } catch (NullPointerException e) {
            }
        }
    }

    public void resumeSound() {
        synchronized (this) {
            try {
                this.soundPool.resume(this.streamID);
                if (!this.mediaPlayer.isPlaying()) {
                    this.mediaPlayer.start();
                }
            } catch (NullPointerException e) {
            }
        }
    }

    public void stopSound() {
        synchronized (this) {
            try {
                this.soundPool.stop(this.streamID);
                if (this.mediaPlayer.isPlaying()) {
                    this.mediaPlayer.stop();
                }
            } catch (NullPointerException e) {
            }
        }
    }

    public void setVolume(int value) {
        synchronized (this) {
            this.volume = value / 100.0f;
            this.soundPool.setVolume(this.streamID, this.volume, this.volume);
            this.mediaPlayer.setVolume(this.volume, this.volume);
        }
    }

    public int getVolume() {
        this.volume = this.audioMgr.getStreamVolume(3) / this.maxVolume;
        return (int) (this.volume * 100.0f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public ArrayList<String> listupFiles(String jarfilename, String destPath, FileFilter filter) {
        ArrayList<String> mediaList = new ArrayList<>();
        try {
            JarFile jarfile = new JarFile(jarfilename);
            Enumeration<JarEntry> entries = jarfile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String filename = entry.getName();
                if (!filename.endsWith("/") && filter.accept(new File(filename))) {
                    File file = new File(String.valueOf(destPath) + filename);
                    mediaList.add(file.getAbsolutePath());
                    if (!file.exists()) {
                        file.mkdirs();
                        file.delete();
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
            jarfile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mediaList;
    }

    /* loaded from: classes.dex */
    class SimpleFileFilter implements FileFilter {
        String description;
        String[] extensions;

        public SimpleFileFilter(WipiMediaManager wipiMediaManager, String ext) {
            this(new String[]{ext}, null);
        }

        public SimpleFileFilter(String[] exts, String descr) {
            this.extensions = new String[exts.length];
            for (int i = exts.length - 1; i >= 0; i--) {
                this.extensions[i] = exts[i].toLowerCase();
            }
            this.description = descr == null ? String.valueOf(exts[0]) + " files" : descr;
        }

        @Override // java.io.FileFilter
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return false;
            }
            String name = f.getName().toLowerCase();
            for (int i = this.extensions.length - 1; i >= 0; i--) {
                if (name.endsWith(this.extensions[i])) {
                    return true;
                }
            }
            return false;
        }

        public String getDescription() {
            return this.description;
        }
    }

    /* loaded from: classes.dex */
    class SoundLoader extends Thread {
        ArrayList<String> mediaList;

        public SoundLoader(String aid) {
            WipiMediaManager.this.privatePath = "/data/data/android.lgt.wipi.App" + aid + "/files/";
            SimpleFileFilter filter = new SimpleFileFilter(new String[]{"ogg"}, "Audio File");
            this.mediaList = WipiMediaManager.this.listupFiles(String.valueOf(WipiMediaManager.this.privatePath) + aid + ".jar", WipiMediaManager.this.privatePath, filter);
            WipiMediaManager.this.soundPool = new SoundPool(1, 3, 0);
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            super.run();
            Iterator<String> it = this.mediaList.iterator();
            while (it.hasNext()) {
                String filename = it.next();
                File file = new File(filename);
                Log.v(WipiPlayer.TAG, "[media load start] " + filename + " size:" + file.length());
                int sid = WipiMediaManager.this.soundPool.load(filename, 1);
                Log.v(WipiPlayer.TAG, "[media load end] " + filename);
                WipiMediaManager.this.mediaTable.put(filename, new Integer(sid));
            }
        }
    }
}
