package com.ai.xiaocai.h5Manager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;


import com.ai.xiaocai.R;
import com.ai.xiaocai.utils.LogUtils;
import com.hanks.htextview.typer.TyperTextView;

import static android.content.Context.AUDIO_SERVICE;

/**
 * Created by Lucien on 2018/5/9.
 */

public class MusicManager {
    private static final String TAG = "Mason/MusicManager";
    private final Context mContext;
    private final AudioManager mAm;

    private MediaPlayer myMediaPlayer;
    private int mCurrentPosition = -1;
    private String mUrl;



    public MusicManager(Context context, MusicControlListener musicControlListener) {
        this.mContext = context;
        this.mMusicControlListener = musicControlListener;
        mAm = (AudioManager) context.getSystemService(AUDIO_SERVICE);
    }

    public boolean getPlayingState() {
        return myMediaPlayer != null && myMediaPlayer.isPlaying();
    }

    public void stopPlay() {
        LogUtils.e("stopPlay  ");
        try {
            myMediaPlayer.stop();
            myMediaPlayer.release();
            myMediaPlayer = null;
            mCurrentPosition = -1;
        } catch (Exception e) {
            mCurrentPosition = -1;
            LogUtils.w(TAG, "stopPlay ", e);
        }
    }

    public void pausePlay() {
        LogUtils.e("pausePlay ");
        try {
            if (myMediaPlayer == null)
                return;
            myMediaPlayer.pause();
            mCurrentPosition = myMediaPlayer.getCurrentPosition();
        } catch (Exception e) {
            mCurrentPosition = -1;
            LogUtils.w(TAG, "pausePlay ", e);
            e.printStackTrace();
        }
    }

    public void continuePlay() {
        LogUtils.e("continuePlay  ");
        try {
            if (requestFocus()) {
                if (myMediaPlayer == null)
                    return;
                myMediaPlayer.start();
                if (mCurrentPosition != -1) myMediaPlayer.seekTo(mCurrentPosition);
            }
        } catch (Exception e) {
            mCurrentPosition = -1;
            LogUtils.w(TAG, "continuePlay ", e);
            e.printStackTrace();
        }
    }

    private boolean mIsPlayVoice;

    public void setUrlAndPlay(String url, boolean isPlayVoice) {
        setUrlAndPlay(url);
        mIsPlayVoice = isPlayVoice;
    }


    public void setUrlAndPlay(String playUrl) {
        mIsPlayVoice = false;
        if (requestFocus()) {
            try {
                if (myMediaPlayer != null) {
                    myMediaPlayer.stop();
                    myMediaPlayer.release();
                }

                myMediaPlayer = new MediaPlayer();
                myMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                myMediaPlayer.setOnPreparedListener(mOnPreparedListener);
                myMediaPlayer.setOnCompletionListener(mOnCompletionListener);
                myMediaPlayer.setOnErrorListener(mOnErrorListener);
                myMediaPlayer.setDataSource(playUrl);
                myMediaPlayer.prepare();
                mUrl = playUrl;
            } catch (Exception e) {
                LogUtils.e(TAG, "setUrlAndPlay Error", e);
                //  doLocalTTS();
                e.printStackTrace();
                if (mMusicControlListener != null) mMusicControlListener.onPlayError();
                mCurrentPosition = -1;
            }
        }
    }

    private boolean requestFocus() {
        // Request audio focus for playback
        int result = mAm.requestAudioFocus(afChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }


    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                //Pause playback
                pausePlay();
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                //Resume playback
                continuePlay();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                // mAm.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
                mAm.abandonAudioFocus(afChangeListener);
                //Stop playback
                //stopPlay();
                pausePlay();
            }
        }
    };


    private MediaPlayer.OnPreparedListener mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            LogUtils.d(TAG, "MediaPlayer onPrepared");
            myMediaPlayer.start();
        }
    };


    private MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            if (!mp.isLooping()) {
                mAm.abandonAudioFocus(afChangeListener);
            }
            mUrl = null;
            mCurrentPosition = -1;
            mMusicControlListener.onPlayComplete(mIsPlayVoice);
        }
    };

    private MediaPlayer.OnErrorListener mOnErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            mCurrentPosition = -1;
            try {
                myMediaPlayer.release();
                //  myMediaPlayer = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            mUrl = null;
            if (mMusicControlListener != null) mMusicControlListener.onPlayError();
            return false;
        }
    };


    public String getUrlPlaying() {
        return mUrl;
    }

    //+++++++++++++++++++++++++++++++++++++++++++++//

    private MusicControlListener mMusicControlListener;

    public void stop() {
        if(mAm!=null)mAm.abandonAudioFocus(afChangeListener);
    }

    /**
     *
     * @param i 0 stop 1 pause  2 continue
     */
    public void doMusicCommand(int i) {
        switch (i){
            case 0:
                stopPlay();
                break;
            case 1:
                pausePlay();
                break;
            case 2:
                continuePlay();
                break;
            case 3: // pre
                //mContext.sendBroadcast(new Intent("action_speech_music_pre"));
                break;
            case 4: //next
                //mContext.sendBroadcast(new Intent("action_speech_music_next"));
                break;
        }

    }





    public interface MusicControlListener {
        void onPlayError();

        void onPlayComplete(boolean isPlayVoice);
    }
}
