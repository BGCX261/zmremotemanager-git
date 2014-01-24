package com.zm.epad.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


import android.net.Uri;

import com.zm.epad.core.LogManager;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.text.format.Time;
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

    public void takeScreenshot(final Handler handler) {
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
                msg.replyTo = new Messenger(handler);

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
    
    public File getLatestScreenshot() {

        File ret = null;
        try {
            String[] proj = { MediaStore.Images.Media.TITLE,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.SIZE, MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media._ID };

            ContentResolver resolver = mContext.getContentResolver();
            Cursor cursor = MediaStore.Images.Media.query(resolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj);

            String title = null;
            long time = Long.MIN_VALUE;
            int size = 0;
            String data = null;
            int id = 0;

            cursor.moveToFirst();
            do {
                if (time < cursor.getLong(cursor
                        .getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED))) {
                    time = cursor
                            .getLong(cursor
                                    .getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED));
                    title = cursor.getString(cursor
                            .getColumnIndex(MediaStore.Images.Media.TITLE));
                    size = cursor.getInt(cursor
                            .getColumnIndex(MediaStore.Images.Media.SIZE));
                    data = cursor.getString(cursor
                            .getColumnIndex(MediaStore.Images.Media.DATA));
                    id = cursor.getInt(cursor
                            .getColumnIndex(MediaStore.Images.Media._ID));
                }
            } while (cursor.moveToNext());

            LogManager.local(TAG, "media data:" + data);
            LogManager.local(TAG, "media id:" + id);

            Uri fileUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    .buildUpon().appendPath(String.valueOf(id)).build();
            LogManager.local(TAG, "data Uri:" + fileUri.toString());
            InputStream in = resolver.openInputStream(fileUri);

            ret = new File(mContext.getFilesDir().getAbsolutePath()
                    + "/temp.png");
            ret.createNewFile();
            FileOutputStream out = new FileOutputStream(ret);
            byte[] buffer = new byte[size];
            in.read(buffer);
            out.write(buffer);
            out.flush();
            out.close();
            in.close();

            LogManager.local(TAG, String.valueOf(ret.canRead()));
            LogManager.local(TAG, String.valueOf(ret.canWrite()));
            LogManager.local(TAG, String.valueOf(ret.canExecute()));
            LogManager.local(TAG, String.valueOf(ret.exists()));
            LogManager.local(TAG, ret.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        LogManager.local(TAG, ret == null ? "null"
                : (ret.toString() + "|" + ret.length()));
        return ret;
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
    
    public void saveFileAsImage(File file){
        
        ContentValues values = new ContentValues();
        ContentResolver resolver = mContext.getContentResolver();
        
        try{
            long time = System.currentTimeMillis();
            String fileName = String.valueOf(time)+"_"+file.getName();
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "Recieved");
            String filePath = new File(dir, fileName).getAbsolutePath();
            LogManager.local(TAG, "file path: "+filePath);
            
            dir.mkdir();
            
            values.put(MediaStore.Images.ImageColumns.DATA, filePath);
            values.put(MediaStore.Images.ImageColumns.TITLE, fileName);
            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, time);
            values.put(MediaStore.Images.ImageColumns.DATE_ADDED, time/1000);
            values.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, time/1000);
            values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpg");
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            LogManager.local(TAG, "file Uri: "+uri.toString());
            
            FileInputStream in = new FileInputStream(file);
            OutputStream out = resolver.openOutputStream(uri);
            byte[] buffer = new byte[(int)file.length()];
            in.read(buffer);
            out.write(buffer);
            out.flush();
            out.close();
            in.close();
            
            values.clear();
            long length = new File(filePath).length();
            values.put(MediaStore.Images.ImageColumns.SIZE, length);
            resolver.update(uri, values, null, null);
            
            LogManager.local(TAG, "file length: "+String.valueOf(length));
        }catch(Exception e){
            e.printStackTrace();
        }

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
