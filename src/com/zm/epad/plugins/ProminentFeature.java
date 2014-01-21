package com.zm.epad.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import com.zm.epad.core.LogManager;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Camera;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class ProminentFeature {

    public static final String TAG = "ProminentFeature";
    private Context mContext;
    private Camera mCamera;
    private SurfaceView mSurfaceView;

    public ProminentFeature(Context context) {
        mContext = context;
    }

    public void changeWallPager() {
        LogManager.local(TAG, "changeWallPager");
        WallpaperManager wm = WallpaperManager.getInstance(mContext);
        try {
            int id = mContext.getResources().getIdentifier("wall", "drawable",
                    "com.zm.epad");
            wm.setResource(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void takeScreenshot() {
        LogManager.local(TAG, "takeScreenshot");
        ComponentName cn = new ComponentName("com.android.systemui",
                "com.android.systemui.screenshot.TakeScreenshotService");
        Intent intent = new Intent();
        intent.setComponent(cn);
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LogManager.local(TAG, "onServiceConnected");
                Messenger messenger = new Messenger(service);
                Message msg = Message.obtain(null, 1);
                msg.replyTo = new Messenger(new Handler());

                try {
                    messenger.send(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                LogManager.local(TAG, "onServiceDisconnected");
            }
        };
        mContext.bindService(intent, conn, Context.BIND_AUTO_CREATE);

    }

    public void startCamera() {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        try {
            mContext.startActivity(intent);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void sendNotify(String title, String text) {
        NotificationManager NotifyManager = 
                (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE); 
        Notification n = new Notification.Builder(mContext)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(mContext.getResources().getIdentifier("icon", "drawable",
                        "com.zm.epad"))
                .build(); 
        
        NotifyManager.notify(0, n);
    }

    // can't start camera in system server
    private void takePhotoNG() {
        LogManager.local(TAG, "camera open");
        mCamera = Camera.open();

        LogManager.local(TAG, "camera open done");
        mSurfaceView = new SurfaceView(mContext);

        try {
            LogManager.local(TAG, "startPreview");
            mCamera.setPreviewDisplay(mSurfaceView.getHolder());
            mCamera.startPreview();

            LogManager.local(TAG, "takePicture");
            mCamera.takePicture(null, null, new Camera.PictureCallback() {

                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    // TODO Auto-generated method stub
                    File file_name = null;
                    LogManager.local(TAG, "onPictureTaken");

                    try {
                        Time time = new Time();
                        time.setToNow();

                        file_name = new File("/sdcard/DCIM/Camera/" + "ZM_"
                                + time.toString());
                        if (!file_name.exists()) {
                            file_name.createNewFile();
                        }
                    } catch (final Exception e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }

                    try {
                        final OutputStream os = new FileOutputStream(file_name);
                        os.write(data);
                        os.close();
                    } catch (final Exception e) {
                        e.printStackTrace();
                    } finally {
                        mCamera.release();
                    }
                }
            });
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
