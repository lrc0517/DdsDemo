package com.ai.xiaocai.h5Manager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.webkit.MimeTypeMap;

import com.ai.xiaocai.R;
import com.ai.xiaocai.number.manager.RecordManager;
import com.ai.xiaocai.number.manager.WeChatDataManager;
import com.ai.xiaocai.number.utils.SmartUtil;
import com.ai.xiaocai.number.utils.VoiceResultBean;
import com.ai.xiaocai.utils.LogUtils;

import java.io.File;
import java.util.List;

/**
 * Created by Lucien on 2018/5/10.
 */

public class UISpeechControl {
    public static final String TAG = "Mason/UISpeechControl";
    public static final int MSG_HANDLER_AUTH_FAILED = 0;
    public static final int MSG_HANDLER_UPDATA_OUT_STR = 1;
    public static final int MSG_HANDLER_UPDATA_TIPS = 2;
    public static final int MSG_HANDLER_TIPS_NEED_SHOW = 3;
    public static final int MSG_HANDLER_WAKE_UP_READY = 4;
    public static final int MSG_HANDLER_TTS_COMPLETE = 5;
    public static final int MSG_HANDLER_COMMAND_MESSAGE_VIDEO = 6;
    public static final int MSG_HANDLER_WAKE_UP_SUCCESS = 7;
    public static final int MSG_HANDLER_COMMAND_MESSAGE_VIDEO_STOP = 8;
    public static final int MSG_HANDLER_NET_DATA_DEVICE_GET_FAILED = 10;
    public static final int MSG_HANDLER_NET_DATA_AUTH_FAILED = 11;
    public static final int MSG_HANDLER_NET_DATA_ONLINE_FAILED = 12;
    public static final int MSG_HANDLER_NET_DATA_DEVICE_ONLINE = 13;
    public static final int MSG_HANDLER_NET_DATA_DO_PERIOD = 14;
    public static final int MSG_HANDLER_NET_EVENT_POWEROFF = 15;

    public static final int MSG_HANDLER_NET_DISABLED = 20;
    public static final int MSG_HANDLER_DDS_INIT_COMPLETE = 100;
    public static final int MSG_HANDLER_WLAN_CONFIRM_PASSWORD_FAILED = 400;



    private final Handler mHandler;
    private final String[] mTipArray;


    private Context mContext;
    private DDSH5Manager mSpeechEngineManager;
    private DDSResultManager mAIResultManager;
    private ReminderManager mReminderManager;
    private WeChatDataManager mWeChatDataManager;
    private RecordManager mRecordManager;


    private UISpeechControlListener mUISpeechControlListener;

    public void onDestory() {
        if (mSpeechEngineManager != null) mSpeechEngineManager.onDestory();
        if (mWeChatDataManager != null) mWeChatDataManager.stop();
        if (mAIResultManager != null) mAIResultManager.stop();
        if (mReminderManager != null) mReminderManager.stop();
        if (mRecordManager != null) mRecordManager.stop();

    }

    public void stopAllPlayer() {
        if (mAIResultManager != null) mAIResultManager.pauseMusic();
        if (mSpeechEngineManager != null)
            mSpeechEngineManager.cancel();
    }

    public void doAuth() {
        if (mSpeechEngineManager != null) mSpeechEngineManager.doAuth();
    }


    public void doNetDeviceOnline() {
        if (mWeChatDataManager != null) mWeChatDataManager.doNotifyOnline();
    }

    public void doNetDeviceID() {
        if (mWeChatDataManager != null) mWeChatDataManager.doGetDeviceID();
    }

    public void doNetPeriodWork() {
        if (mWeChatDataManager != null) mWeChatDataManager.doPeriodWork();
    }

    public void batteryPowerConnected() {
        if (mSpeechEngineManager != null) mSpeechEngineManager.doLocalTTS("正在充电", false);
    }

    public void batteryLow() {
        if (mSpeechEngineManager != null) mSpeechEngineManager.doLocalTTS("电池电量低", false);
    }

    public void batteryOkay() {
        if (mSpeechEngineManager != null) mSpeechEngineManager.doLocalTTS("电量已恢复", false);
    }

    public void batteryPowerFull() {
        if (mSpeechEngineManager != null) mSpeechEngineManager.doLocalTTS("电池已充满", false);
    }

    public void batteryPowerLowLevel(int present) {
        if (mSpeechEngineManager != null)
            mSpeechEngineManager.doLocalTTS("剩余电量，百分之" + present, false);
    }

    public void netWorkConnected() {
        if (mSpeechEngineManager != null) mSpeechEngineManager.doLocalTTS("联网已成功，快来和我聊天吧。", false);
    }

    public void netWorkDisConnected() {
        if (mSpeechEngineManager != null) mSpeechEngineManager.doLocalTTS("网络已断开", false);
    }


    public void doNetWorkDisabled() {
        if (mSpeechEngineManager != null)
            mSpeechEngineManager.doLocalTTS("没有找到熟悉的网络，请帮我连上网络吧", false);
    }

    public void wifiapOpend() {
        if (mSpeechEngineManager != null) mSpeechEngineManager.
                doLocalTTS("热点已开启，请用手机帮我配网吧。", false);
    }

    public void wifiapOpening() {
        if (mSpeechEngineManager != null) mSpeechEngineManager.
                doLocalTTS("正在开启配网功能，请稍后。。。", false);
    }

    public void changeWakeState() {
        if (mSpeechEngineManager != null) mSpeechEngineManager.changeWakeState();
    }

    public void initWebUI(View mView) {
        if (mSpeechEngineManager != null) mSpeechEngineManager.initWebUI(mView);
    }

    public void doConfirmPasswordFailed() {
        if (mSpeechEngineManager != null) mSpeechEngineManager.
                doLocalTTS("对不起，密码错误，请重新配网。。。", false);
    }

    public void playFMMusic() {
        //打开音乐电台
        if (mSpeechEngineManager != null) mSpeechEngineManager.doSdsTXT("播报新闻");

    }

    public void sendTXT(String txt) {
        if (mSpeechEngineManager != null) mSpeechEngineManager.doSdsTXT(txt);
    }

    public void setVolumedown() {
        if (mAIResultManager != null) mAIResultManager.setVolumeMinDown();
    }

    public void setVolumeUp() {
        if (mAIResultManager != null) mAIResultManager.setVolumeMinUp();
    }

    public void noPrayRespurce() {
        if (mSpeechEngineManager != null) mSpeechEngineManager.doLocalTTS("没有找到灵修资源。", false);
    }

    public void needUpdate() {
        if (mSpeechEngineManager != null)
            mSpeechEngineManager.doLocalTTS("更新已可以用，请重启设备更新，更新时电量需大于30%。", false);
    }

    private boolean mDataState = true;

    public void setNetPlayState(boolean b) {
        // if (mWeChatDataManager!=null)mWeChatDataManager.setDataState(b);
        mDataState = b;
    }


    public interface UISpeechControlListener {
        void onReady();

        void onWakeUp();

    }

    public void setUISpeechControlListener(UISpeechControlListener l) {
        this.mUISpeechControlListener = l;
    }

    public UISpeechControl(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        if (mSpeechEngineManager == null)
            mSpeechEngineManager = new DDSH5Manager(context, handler, mSpeechEngineListener);
        mSpeechEngineManager.checkDDSReady();

        mAIResultManager = new DDSResultManager(context, mResultManagerListener);
        mReminderManager = new ReminderManager(context);

        mRecordManager = new RecordManager(context);

        mWeChatDataManager = new WeChatDataManager(context, handler, mWeChatDateListener);

        mTipArray = context.getResources().getStringArray(R.array.chat_tips);

    }

    private DDSH5Manager.DDSListener mSpeechEngineListener = new DDSH5Manager.DDSListener() {

        @Override
        public void onInitError() {
            if (mSpeechEngineManager != null)
                mSpeechEngineManager.init();
        }

        @Override
        public void onSpeechAuth(boolean isAuth) {
            if (!isAuth) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mSpeechEngineManager != null) mSpeechEngineManager.doAuth();
                    }
                }, 1000);
            }
        }

        @Override
        public void onInitComplete() {
            mHandler.sendEmptyMessage(MSG_HANDLER_DDS_INIT_COMPLETE);
        }

        @Override
        public void onInitializing() {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mSpeechEngineManager != null) mSpeechEngineManager.checkDDSReady();
                }
            }, 3000);

        }

        @Override
        public void onShowTips(String tips) {
            Message msg = Message.obtain();
            msg.what = MSG_HANDLER_TIPS_NEED_SHOW;
            msg.obj = tips;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onTTSComplete(boolean isMediaNameTTS) {
            mHandler.sendEmptyMessage(MSG_HANDLER_TTS_COMPLETE);
            if (isMediaNameTTS) {
                playMediaData();
            }
        }

        @Override
        public void onNativeResult(String nativeApi, String data) {
            try {
                if (mAIResultManager != null) mAIResultManager.setNativeResult(nativeApi, data);
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.w(TAG, "onNativeResult", e);
            }
        }

        @Override
        public void onCommandResult(String command, String data) {
            try {
                if (mAIResultManager != null) mAIResultManager.setCommandResult(command, data);
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.w(TAG, "onCommandResult", e);
            }
        }

        @Override
        public void onMessageResult(String message, String data) {
            try {
                if (mAIResultManager != null) mAIResultManager.setMessageResult(message, data);
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.w(TAG, "onMessageResult", e);
            }
        }

        @Override
        public void onWakeUp() {
            mHandler.sendEmptyMessage(MSG_HANDLER_WAKE_UP_SUCCESS);
            if (mAIResultManager != null) mAIResultManager.pauseMusic();
        }
    };

    private List<VoiceResultBean.DataBean> mVoices;
    private DDSResultManager.ResultCallBack mResultManagerListener = new DDSResultManager.ResultCallBack() {
        @Override
        public void onCommandCallBack(String command, String strBack) {
            if (mSpeechEngineManager != null) mSpeechEngineManager.doLocalTTS(strBack, false);
        }

        @Override
        public void onCommandPray(boolean pray) {
            if (pray) {
                mContext.sendBroadcast(new Intent("action_speech_pray_start"));
            } else {
                mContext.sendBroadcast(new Intent("action_speech_pray_end"));
            }
        }

        @Override
        public void onCommandMusic(int i) {
            if (i == 3) {
                mContext.sendBroadcast(new Intent("action_speech_music_pre"));
            } else if (i == 4) {
                mContext.sendBroadcast(new Intent("action_speech_music_next"));
            }
        }

        @Override
        public void onCommandVideo(String linkUrl) {
            Message obtain = Message.obtain();
            obtain.what = MSG_HANDLER_COMMAND_MESSAGE_VIDEO;
            obtain.obj = linkUrl;
            mHandler.sendMessage(obtain);
        }

        @Override
        public void onCommandRest() {
            if (mSpeechEngineManager != null) mSpeechEngineManager.changeToRestState();
        }

        @Override
        public void onCommandPraySearch(String title) {
            Intent intent = new Intent("action_speech_music_search");
            intent.putExtra("title", title);
            mContext.sendBroadcast(intent);
        }


        @Override
        public void onPlayError() {

        }

        @Override
        public void onPlayComplete() {

        }

        @Override
        public void onVoicesPlayComplete() {
            if (mVoices != null) mVoices.clear();
            Intent intent = new Intent("action_broad_cast_wecath_noti");
            intent.putExtra("wecath_list_size", 0);
            mContext.sendBroadcast(intent);
        }

        @Override
        public void onLocalMusicPlay(final String str, String url) {
            LogUtils.e("url = " + url);
            LogUtils.e("str = " + str);
            if (url == null) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mSpeechEngineManager != null)
                            mSpeechEngineManager.doLocalTTS("对不起，没有找到" + str, false);
                    }
                }, 1000);
            }
        }

        @Override
        public void onMessageCallback(final String messageBackStr) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mSpeechEngineManager != null)
                        mSpeechEngineManager.doLocalTTS(messageBackStr, false);
                }
            }, 2000);
        }

    };

    private WeChatDataManager.WeChatDateListener mWeChatDateListener = new WeChatDataManager.WeChatDateListener() {

      /*  @Override
        public void onVoiceRecieve(List<VoiceResultBean.DataBean> voices) {
            if (mVoices != null) {
                if (voices != null && voices.size() >= 0)
                    mVoices.addAll(voices);
                if (mSpeechEngineManager != null)
                    mSpeechEngineManager.doLocalTTS("您有新的消息！",false);
            } else {
                if (voices != null && voices.size() >= 0)
                    mVoices = voices;
            }


            Intent intent = new Intent("action_broad_cast_wecath_noti");
            intent.putExtra("wecath_list_size", mVoices == null ? 0 : mVoices.size());
            mContext.sendBroadcast(intent);

            //if (mAIResultManager != null) mAIResultManager.playMediaData(voices);
        }*/

        @Override
        public void onEvent(final String eventStr, final String url, final String singer, final String title) {

            if (mDataState && "play".equals(eventStr)) {
                if (mSpeechEngineManager != null)
                    mSpeechEngineManager.cancel();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (title != null && !TextUtils.isEmpty(title)) {
                            if (singer != null && !TextUtils.isEmpty(singer)) {
                                if (mSpeechEngineManager != null)
                                    mSpeechEngineManager.doLocalTTS("马上为您播放" + singer + "的" + title, true);
                            } else {
                                if (mSpeechEngineManager != null)
                                    mSpeechEngineManager.doLocalTTS("马上为您播放" + title, true);
                            }
                            mEvent = eventStr;
                            mUrl = url;
                        } else {
                            if (mAIResultManager != null)
                                mAIResultManager.playMediaData(eventStr, url);
                        }
                    }
                }, 1000);
            } else if ("pause".equals(eventStr)) {
                if (mAIResultManager != null) mAIResultManager.pauseMusic();
            }
        }

        @Override
        public void onPoweroffEvent() {
            if (mSpeechEngineManager != null)
                mSpeechEngineManager.doLocalTTS("正在关闭小采...", false);
            mHandler.sendEmptyMessageDelayed(MSG_HANDLER_NET_EVENT_POWEROFF, 2000);
        }

        @Override
        public void onVideoEvent(final String eventStr, final String url, final String singer, final String title) {
            if (mDataState && "play".equals(eventStr)) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (title != null && !TextUtils.isEmpty(title)) {
                            if (singer != null && !TextUtils.isEmpty(singer)) {
                                if (mSpeechEngineManager != null)
                                    mSpeechEngineManager.doLocalTTS("马上为您播放" + singer + "的" + title, true);
                            } else {
                                if (mSpeechEngineManager != null)
                                    mSpeechEngineManager.doLocalTTS("马上为您播放" + title, true);
                            }
                        }
                        Message obtain = Message.obtain();
                        obtain.what = MSG_HANDLER_COMMAND_MESSAGE_VIDEO;
                        obtain.obj = url;
                        mHandler.sendMessage(obtain);
                    }
                });
            } else if ("pause".equals(eventStr)) {
                mHandler.sendEmptyMessage(MSG_HANDLER_COMMAND_MESSAGE_VIDEO_STOP);
            }
        }
    };

    private String mEvent;
    private String mUrl;

    public void playMediaData() {
        if (mAIResultManager != null)
            mAIResultManager.playMediaData(mEvent, mUrl);
    }


    public void getWeatherInfo() {
        if (mAIResultManager != null) mAIResultManager.pauseMusic();
        if (mSpeechEngineManager != null) mSpeechEngineManager.doSdsTXT("今天天气");
    }

    public void translateStart() {
        try {
            if (mSpeechEngineManager != null) mSpeechEngineManager.onStop();
            if (mAIResultManager != null) mAIResultManager.pauseMusic();
            if (mSpeechEngineManager != null) mSpeechEngineManager.doTranslateStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void translateEnd() {
        try {
            if (mSpeechEngineManager != null) mSpeechEngineManager.doTranslateEnd();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void playVoiceMessage() {
        if (mAIResultManager != null) mAIResultManager.pauseMusic();
        if (mSpeechEngineManager != null) mSpeechEngineManager.onStop();
        if (mAIResultManager != null) mAIResultManager.playMediaData(mVoices);
    }

    public void voiceSpeakStart() {
        if (mAIResultManager != null) mAIResultManager.pauseMusic();
        if (mSpeechEngineManager != null) mSpeechEngineManager.onStop();
        if (mRecordManager != null) mRecordManager.start();
    }

    public void voiceSpeakEnd() {
        new Runnable() {
            @Override
            public void run() {
                File file = mRecordManager.stopRecord();
                if (mSpeechEngineManager != null)
                    mSpeechEngineManager.postFile(file, SmartUtil.getMac());
            }
        }.run();
    }
}
