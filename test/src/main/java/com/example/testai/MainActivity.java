package com.example.testai;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LXQ/Mason";


    private MusicManager mMusicManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

      /*  new Thread(new Runnable() {
            @Override
            public void run() {
                readMusic(MainActivity.this);
            }
        }).start();

        mMusicManager = new MusicManager(this, new MusicManager.MusicControlListener() {
            @Override
            public void onPlayError() {

            }

            @Override
            public void onPlayComplete(boolean isPlayVoice) {

            }
        });*/
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mMusicManager!=null)mMusicManager.stop();
    }

    public void stop(View view) {
        if (mMusicManager!=null)mMusicManager.stopPlay();
    }

    public void playMusic(View view) {
        new Thread(mLocalMusic).start();
    }

    private String mUrl;
    private Runnable mLocalMusic = new Runnable() {
        @Override
        public void run() {
            Cursor cursor = MainActivity.this.getContentResolver()
                    .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                            MediaStore.Audio.Media.DISPLAY_NAME + "=" + "'小羊之歌.mp3'", null,
                            MediaStore.Audio.Media._ID);
            String musicPath = null;
            while (cursor.moveToNext()) {
                byte[] data = cursor.getBlob(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                musicPath = new String(data, 0, data.length - 1);
                LogUtils.e("musicPath = " + musicPath);
            }
            cursor.close();
            mUrl = musicPath;
            if (mMusicManager!=null)mMusicManager.setUrlAndPlay(mUrl);
        }
    };

    private void readMusic(Context context) {
        Cursor cursor = context.getContentResolver()
                .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        while (cursor.moveToNext()) {
            //if (videoData.size() >20)break;
            String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            //String track = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TRACK));
            String dispalyName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
            // byte[] data = cursor.getBlob(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            // String musicPath = new String(data, 0, data.length - 1);
            // int fileColum = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            //int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

            LogUtils.d(TAG, "dispalyName = " + dispalyName);
            LogUtils.i( "title = " + title);
            // name/path = "videoStr"/ |
        }
        cursor.close();
        
    }
}
