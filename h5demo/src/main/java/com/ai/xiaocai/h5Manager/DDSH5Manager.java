package com.ai.xiaocai.h5Manager;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import com.ai.xiaocai.R;
import com.ai.xiaocai.number.manager.RecordManager;
import com.ai.xiaocai.number.utils.ReSmartContants;
import com.ai.xiaocai.number.utils.SmartUtil;
import com.ai.xiaocai.utils.LogUtils;
import com.aispeech.ddsdemo.webview.HybridWebViewClient;
import com.aispeech.ddsdemo.widget.InputField;
import com.aispeech.dui.dds.DDS;
import com.aispeech.dui.dds.DDSAuthListener;
import com.aispeech.dui.dds.DDSConfig;
import com.aispeech.dui.dds.DDSInitListener;
import com.aispeech.dui.dds.agent.ASREngine;
import com.aispeech.dui.dds.agent.MessageObserver;
import com.aispeech.dui.dds.agent.TTSEngine;
import com.aispeech.dui.dds.auth.AuthType;
import com.aispeech.dui.dds.exceptions.DDSNotInitCompleteException;
import com.aispeech.dui.dsk.duiwidget.CommandObserver;
import com.aispeech.dui.dsk.duiwidget.NativeApiObserver;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Lucien on 2018/6/29.
 */

public class DDSH5Manager {

    private static final String TAG = "Mason/DDSH5";
    private static OkHttpClient okHttpClient;

    private final RecordManager mRecordManager;
    private final DDSListener mDDSListener;
    private final AudioManager mAm;
    private final Handler mHandler;
    private WebView webview;
    private InputField inputField;
    private RelativeLayout webContainer;
    private Context mContext;
    private MediaPlayer mediaPlayer;


    public interface DDSListener {
        void onInitError();

        void onSpeechAuth(boolean isAuth);

        void onInitComplete();

        void onInitializing();

        void onShowTips(String tips);

        void onTTSComplete(boolean mIsMediaNameTTS);

        void onNativeResult(String nativeApi, String data);

        void onCommandResult(String command, String data);

        void onMessageResult(String message, String data);

        void onWakeUp();

    }

    public DDSH5Manager(Context context, Handler handler, DDSListener l) {
        this.mContext = context;
        this.mHandler = handler;
        this.mDDSListener = l;
        mRecordManager = new RecordManager(mContext);
        mAm = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        init();
        initTipsAudio();
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
                //mediaPlayer.pause();
                try {
                    DDS.getInstance().getAgent().getTTSEngine().shutup("100");
                } catch (DDSNotInitCompleteException e) {
                    e.printStackTrace();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                //Resume playback
                // mediaPlayer.resume ();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                // mAm.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
                mAm.abandonAudioFocus(afChangeListener);
                try {
                    DDS.getInstance().getAgent().getTTSEngine().shutup("100");
                } catch (DDSNotInitCompleteException e) {
                    e.printStackTrace();
                }
                //Stop playback
                // mediaPlayer.stop();

            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {

            }
        }
    };


    private void initTipsAudio() {
        mediaPlayer = new MediaPlayer();
        try {
            AssetManager assetManager = mContext.getAssets();
            AssetFileDescriptor fileDescriptor = assetManager.openFd("notice.wav");
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setVolume(0.3f, 0.3f);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(),
                    fileDescriptor.getStartOffset(),
                    fileDescriptor.getLength());
            //mediaPlayer.setDataSource("/sdcard/notice.wav");
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    LogUtils.i("Audio Complete");
                    try {
                        translate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            mediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private boolean isAuthed = false;

    public void doAuth() {
        try {
            DDS.getInstance().doAuth();
            mDDSListener.onInitializing();
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    public void checkDDSReady() {
        if (DDS.getInstance().getInitStatus() != DDS.INIT_COMPLETE_NONE) {
            try {
                if (DDS.getInstance().isAuthSuccess()) {
                    Log.w(TAG, " auth success");
                    mDDSListener.onSpeechAuth(true);
                    isAuthed = true;
                } else {
                    mDDSListener.onSpeechAuth(false);
                    Log.w(TAG, " auth failed");
                }
            } catch (DDSNotInitCompleteException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, " waiting  init complete finish...");
            mDDSListener.onInitializing();
        }
    }

    private boolean mLoadedTotally = false;

    public void initWebUI(View view) {
        inputField = (InputField) view.findViewById(R.id.input_field);
        inputField.setListener(mInputFieldCallback);

        webContainer = (RelativeLayout) view.findViewById(R.id.main_web_container);
        setWebView();
        webContainer.addView(webview, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams
                .MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
    }

    private void setWebView() {
        webview = new WebView(mContext);
        webview.setWebViewClient(new HybridWebViewClient(mContext));
        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                Log.d(TAG, "view " + view + " progress " + newProgress + " mLoadedTotally " + mLoadedTotally);
                if (newProgress == 100 && !mLoadedTotally) {
                    mLoadedTotally = true;
                    //sendHiMessage();
                    //TODO
                }
            }
        });
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setBackgroundColor(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            webview.getSettings().setMediaPlaybackRequiresUserGesture(false);
        }
        try {
            loadUI(DDS.getInstance().getAgent().getValidH5Path());
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    void loadUI(String h5UiPath) {
        Log.d(TAG, "loadUI " + h5UiPath);
        String url = h5UiPath;
        mLoadedTotally = false;

        webview.loadUrl(url);
    }

    private InputField.Listener mInputFieldCallback = new InputField.Listener() {
        @Override
        public boolean onMicClicked() {
            try {
                DDS.getInstance().getAgent().avatarClick();
            } catch (DDSNotInitCompleteException e) {
                e.printStackTrace();
            }
            return true;
        }
    };

    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++//
    public void init() {
        Log.i(TAG, "init");
        DDS.getInstance().setDebugMode(2);//在调试时可以打开sdk调试日志，在发布版本时，请关闭
        DDS.getInstance().init(mContext, createConfig(), new DDSInitListener() {
            @Override
            public void onInitComplete(boolean isFull) {
                Log.i(TAG, "onInitComplete isFull = " + isFull + ",isAuthed = " + isAuthed);
                if (isFull) {
                    try {
                        DDS.getInstance().getAgent().getTTSEngine().setSpeaker("qianranc");
                        doLocalTTS("我已经开机了", false);
                        DDS.getInstance().getAgent().getWakeupEngine().enableWakeup();
                        DDS.getInstance().getAgent().getTTSEngine().setListener(mTtsListener);
                       /* DDS.getInstance().getAgent().getWakeupEngine().updateCommandWakeupWord(
                                new String[]{"打开灯效", "关闭灯效","打开投影","关闭投影",
                                        "我要灵修", "我要祷告","开始灵修","开始祷告",
                                        "结束灵修", "结束祷告","灵修结束","祷告结束"
                                },//1.actions 命令唤醒词指令, 为string数组, 不为null
                                new String[]{"打开灯效","关闭灯效","打开投影","关闭投影",
                                        "我要灵修", "我要祷告","开始灵修","开始祷告",
                                        "结束灵修", "结束祷告","灵修结束","祷告结束"
                                },//2.words 命令唤醒词, 为string数组, 不为null
                                new String[]{"da kai deng xiao",
                                        "guan bi deng xiao",
                                        "da kai tou ying",
                                        "guan bi tou ying",
                                        "wo yao ling xiu",
                                        "wo yao dao gao",
                                        "kai shi ling xiu",
                                        "kai shi dao gao",
                                        "jie shu ling xiu",
                                        "jie shu dao gao",
                                        "ling xiu jie shu",
                                        "dao gao jie shu"
                                },//3.pinyins 命令唤醒词的拼音, 形如：ni hao xiao chi, 为string数组, 不为null
                                new String[]{"0.25","0.25","0.25","0.25","0.25","0.25","0.25","0.25","0.25","0.25","0.25","0.25"},//4.thresholds 命令唤醒词的阈值, 形如：0.120(取值范围：0-1)。为string数组, 不为null
                                new String[][]{{"好的"},{"好的"},{"好的"},{"好的"},
                                        {""},{""},{""},{""},
                                        {""},{""},{""},{""}
                                }//5.greetings 命令唤醒词的欢迎语, 为string二维数组, 不为null, 每维对应一个唤醒词的欢迎语
                        );*/

                      /*  DDS.getInstance().getAgent().getWakeupEngine().updateShortcutWakeupWord(
                                new String[]{"再见","拜拜","休息吧"},//1.words 打断唤醒词, 为string数组, 不为null
                                new String[]{"zai jian","bai bai","xiu xi ba"},//2.pinyins 打断唤醒词的拼音, 形如：ni hao xiao chi, 为string数组, 不为null
                                new String[]{"0.08","0.08","0.08"}//3.thresholds 打断唤醒词的阈值, 形如：0.120(取值范围：0-1) 为string数组, 不为null
                        );*/

                        isSleep = false;
                        mDDSListener.onInitComplete();
                    } catch (DDSNotInitCompleteException e) {
                        Log.w(TAG, "onInitComplete Error", e);
                    }
                } else {
                    DDS.getInstance().release();
                    init();
                }
            }

            @Override
            public void onError(int what, final String msg) {
                Log.e(TAG, "Init onError: " + what + ", error: " + msg);
                mDDSListener.onInitError();
            }

        }, new DDSAuthListener() {
            @Override
            public void onAuthSuccess() {
                Log.i(TAG, "onAuthSuccess");
                // mHandler.sendEmptyMessage(MSG_HANDLER_DDS_AUTH_SUCCESS);
            }

            @Override
            public void onAuthFailed(final String errId, final String error) {
                Log.e(TAG, "onAuthFailed: " + errId + ", error:" + error);
                // mHandler.sendEmptyMessageDelayed(MSG_HANDLER_DDS_AUTH_FAILED, 1000);
            }
        });


        DDS.getInstance().getAgent().subscribe(new String[]{
                "sys.dialog.start",
                "sys.dialog.end",
                "sys.wakeup.result",
                "context.input.text",
                "context.output.text",
                "context.widget.media",
                "context.widget.custom",
                "context.widget.web",
                "context.widget.content",
                "context.widget.list"
        }, messageObserver);


        DDS.getInstance().getAgent().subscribe(new String[]{
                "open_window",
                "device_help",
                "device_light",
                "device_shadow",
                "device_battery",
                "device_rest",
                "local_music_play",
                "local_music_pray",
                "local_music_pray_search",
                "local_music_control",
                "custom_media_video",
                "alarm_localhook_local_alarm_delete",
                "alarm.query.action",
                "Timer"
        }, commandObserver);


        DDS.getInstance().getAgent().subscribe(new String[]{
                "query_battery",
                "alarm.set",
                "alarm.list",
                "alarm.delete",
                /*闹钟*/
                "alarm_localhook_local_alarm_add_single",
                "alarm_localhook_local_add_time",
                "alarm_localhook_local_alarm_add_repeat",

                "alarm_localhook_local_alarm_add_timer",
                "alarm_localhook_local_alarm_add_timer_by_action",
                "settings.volume.inc",
                "settings.volume.dec",
                "settings.volume.set",
                "settings.mutemode.open",
                "settings.wifi.open",
                "settings.wifi.close"
        }, nativeApiObserver);

    }

    private DDSConfig createConfig() {

        DDSConfig config = new DDSConfig();
        //278573265
        config.addConfig(DDSConfig.K_PRODUCT_ID, "278573265");  // TODO 填写自己的产品ID
        config.addConfig(DDSConfig.K_USER_ID, "2874863675@qq.com");  // TODO 填写真是的用户I
        config.addConfig(DDSConfig.K_AUTH_TYPE, AuthType.PROFILE);
        config.addConfig(DDSConfig.K_ALIAS_KEY, "xiaocai");   // TODO 填写产品的发布分支
        //bf031387a13bbf031387a13b5b3b27c1
        config.addConfig(DDSConfig.K_API_KEY, "bf031387a13bbf031387a13b5b3b27c1");  // TODO 填写API KEY
        config.addConfig(DDSConfig.K_DUICORE_ZIP, "duicore.zip");
        config.addConfig(DDSConfig.K_CUSTOM_ZIP, "product.zip");
        config.addConfig(DDSConfig.K_USE_UPDATE_NOTIFICATION, "false");//系统更新通知
        config.addConfig(DDSConfig.K_USE_UPDATE_DUICORE, "false");//热更新
        config.addConfig(DDSConfig.K_MIC_TYPE, "5");
        config.addConfig(DDSConfig.K_AEC_MODE, "external");
        // config.addConfig(DDSConfig.K_WAKEUP_DEBUG, "true");
        // config.addConfig(DDSConfig.K_VAD_DEBUG, "true");
        // config.addConfig(DDSConfig.K_ASR_DEBUG, "true");
        // config.addConfig(DDSConfig.K_TTS_DEBUG, "true");
        //TTS
        config.addConfig(DDSConfig.K_STREAM_TYPE, AudioManager.STREAM_MUSIC);//
        //ASR
        config.addConfig(DDSConfig.K_ASR_ENABLE_PUNCTUATION, "true");//识别标点
        config.addConfig(DDSConfig.K_ASR_ENABLE_TONE, "true");//识别声调
        config.addConfig("CLOSE_TIPS", "true");//关闭提示音

        Log.i(TAG, "config->" + config.toString());
        return config;
    }

    private boolean mIsWakesound = false;

    private boolean mIsMediaNameTTS = false;

    public void doLocalTTS(String txt, boolean isMediaName) {
        mIsMediaNameTTS = isMediaName;
        if (txt == null) return;
        try {
            DDS.getInstance().getAgent().getTTSEngine().shutup("100");
            //DDS.getInstance().getAgent().stopDialog();
            // DDS.getInstance().getAgent().getTTSEngine().speak(txt, 1);
            DDS.getInstance().getAgent().getTTSEngine().speak(txt, 1, "100", AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            if (!mIsWakesound) {
                postChatInfo(false, getDeviceId(), txt);
            }
            if (mDDSListener != null) mDDSListener.onShowTips(txt);
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    private String mDeviceID;

    public String getDeviceId() {
        if (mDeviceID == null)
            mDeviceID = SmartUtil.getMac();
        return mDeviceID;
    }


    public void onDestory() {
        if (mRecordManager != null) mRecordManager.stop();
        if (webContainer != null) webContainer.removeAllViews();
        if (webview != null) webview.removeAllViews();
        if (webview != null) webview.destroy();
        if (inputField != null) inputField.destroy();
        DDS.getInstance().getAgent().unSubscribe(commandObserver);
        DDS.getInstance().getAgent().unSubscribe(nativeApiObserver);
        DDS.getInstance().getAgent().unSubscribe(messageObserver);
        DDS.getInstance().release();
        System.exit(0);
    }

    public void onStop() {
        try {
            DDS.getInstance().getAgent().avatarClick();
            DDS.getInstance().getAgent().getASREngine().cancel();
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }
    }

    private NativeApiObserver nativeApiObserver = new NativeApiObserver() {
        @Override
        public void onQuery(final String nativeApi, final String data) {
            mDDSListener.onNativeResult(nativeApi, data);
        }
    };

    private CommandObserver commandObserver = new CommandObserver() {
        @Override
        public void onCall(final String command, final String data) {
            mDDSListener.onCommandResult(command, data);
        }
    };

    private MessageObserver messageObserver = new MessageObserver() {
        @Override
        public void onMessage(String message, String data) {
            LogUtils.e("message = " + message + ",data = " + data);
            if ("context.input.text".equals(message)) {
                try {
                    if (data.contains("text")) {
                        JSONObject jsonObject = new JSONObject(data);
                        String text = jsonObject.getString("text");
                        postChatInfo(true, getDeviceId(), text);
                        String type = jsonObject.getString("type");
                        if ("command".equals(type)) {
                            doSdsTXT(text);
                        }
                    }
                    //FIXME post to net server
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if ("context.output.text".equals(message)) {
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    String text = jsonObject.getString("text");
                    postChatInfo(false, getDeviceId(), text);
                    //FIXME post to net server
                    if (text != null && !TextUtils.isEmpty(text)) {
                        mDDSListener.onShowTips(text);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if ("sys.wakeup.result".equals(message)) {
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    String greeting = jsonObject.getString("greeting");
                    if (requestFocus()) {
                        // mIsWakesound = true;
                        // doLocalTTS(greeting);
                        DDS.getInstance().getAgent().avatarClick(greeting);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.w(TAG, "Wake error", e);
                }
            } else if ("sys.dialog.start".equals(message)) {
                // mContext.sendBroadcast(new Intent("action_speech_wake_up"));
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    String reason = jsonObject.getString("reason");
                    if ("wakeup.major".equals(reason)) {
                        requestFocus();
                        if (mDDSListener != null) mDDSListener.onWakeUp();

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if ("sys.dialog.end".equals(message)) {
                //{"skillId":"2018070300000051","reason":"normal"} 灵修
                //{"errMsg":"error retry max","reason":"error","skillId":"2017120200000013","errId":71309}
                //TODO continue play
                //if (mDDSListener!=null)mDDSListener.onWakeUp();
            } else {
                mDDSListener.onMessageResult(message, data);
            }
        }
    };

    private TTSEngine.Callback mTtsListener = new TTSEngine.Callback() {
        @Override
        public void beginning(String s) {

        }

        @Override
        public void received(byte[] bytes) {

        }

        @Override
        public void end(String s, int i) {
            Log.e(TAG, "s = " + s + ",i = " + i);
            if (mIsWakesound) {
                //TODO start SDS
                try {
                    DDS.getInstance().getAgent().startDialog();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mIsWakesound = false;
            } else {
                mDDSListener.onTTSComplete(mIsMediaNameTTS);
            }
            mIsMediaNameTTS = false;
            isChanging = false;
        }

        @Override
        public void error(String s) {
            mIsMediaNameTTS = false;
            isChanging = false;
        }
    };


    public void doSdsTXT(String s) {
        try {
            DDS.getInstance().getAgent().sendText(s);
        } catch (DDSNotInitCompleteException e) {
            e.printStackTrace();
        }

    }

    private String mStrAsr;

    public void doTranslateStart() {
        if (mediaPlayer != null) mediaPlayer.start();
    }

    private void translate() throws Exception {
        DDS.getInstance().getAgent().getASREngine().startListening(new ASREngine.Callback() {
            @Override
            public void beginningOfSpeech() {

            }

            @Override
            public void endOfSpeech() {

            }

            @Override
            public void bufferReceived(byte[] bytes) {

            }

            @Override
            public void partialResults(String s) {
                Log.e(TAG, "partialResults = " + s);
            }

            @Override
            public void finalResults(String s) {
                Log.e(TAG, "finalResults = " + s);

                try {
                    JSONObject obj = new JSONObject(s);
                    mStrAsr = obj.getString("text").trim();
                    String deviceId = getDeviceId();
                    if (deviceId != null && !TextUtils.isEmpty(deviceId)) {
                        doTxtTranslateToWeChat(mStrAsr, deviceId);
                    } else {
                        doSdsTXT("翻译" + mStrAsr);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void error(String s) {

            }

            @Override
            public void rmsChanged(float v) {

            }
        });

    }

    public void doTranslateEnd() throws Exception {
        DDS.getInstance().getAgent().getASREngine().stopListening();
    }


    private boolean isSleep = false;
    private boolean isChanging = false;

    public void changeWakeState() {
        if (isChanging) {
            return;
        }
        isChanging = true;
        try {
            isSleep = !isSleep;
            final String[] sleepTip = mContext.getResources().getStringArray(isSleep ? R.array.sleep_tips : R.array.wakeup_tips);
            final int num = (int) (Math.random() * sleepTip.length);
            LogUtils.e("num = " + num);
            LogUtils.e("sleepTip[num] = " + sleepTip[num]);
            DDS.getInstance().getAgent().avatarClick(" ");
            DDS.getInstance().getAgent().stopDialog();
            final String tts = sleepTip[num];
            if (isSleep) {
                DDS.getInstance().getAgent().getWakeupEngine().disableWakeup();
            } else {
                DDS.getInstance().getAgent().getWakeupEngine().enableWakeup();
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    doLocalTTS(tts, false);
                }
            }, 1000);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, "changeWakeState Error-->", e);
            isChanging = false;
        }

      /*  if (requestFocus()) {
            isSleep = !isSleep;
            String[] sleepTip = mContext.getResources().getStringArray(isSleep ? R.array.sleep_tips : R.array.wakeup_tips);
            int num = (int) (Math.random() * sleepTip.length);
            LogUtils.e("num = " + num);
            LogUtils.e("sleepTip[num] = " + sleepTip[num]);
            try {
                DDS.getInstance().getAgent().stopDialog();
                if (isSleep) {
                    DDS.getInstance().getAgent().getWakeupEngine().disableWakeup();
                } else {
                    DDS.getInstance().getAgent().getWakeupEngine().enableWakeup();
                }
                doLocalTTS(sleepTip[num],false);
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.e(TAG,"changeWakeState Error-->",e);
            }
        }*/
        //isChanging = false;
    }

    public void changeToRestState() {
        try {
            isSleep = true;
            DDS.getInstance().getAgent().getWakeupEngine().disableWakeup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void cancel() {
        isChanging = false;
        Log.e(TAG, "cancel");
        try {
            requestFocus();
            DDS.getInstance().getAgent().getTTSEngine().shutup("100");
            DDS.getInstance().getAgent().stopDialog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void doTxtTranslateToWeChat(String inputStr, String deviceid) {
        String from = "?from=" + (SmartUtil.isContainsChinese(inputStr) ? "zh" : "en");
        //String content = "&content=" + (SmartUtil.isContainsChinese(inputStr) ? SmartUtil.string2Unicode(inputStr) : inputStr);
        String content = "&content=" + inputStr;
        String url = ReSmartContants.URL + ReSmartContants.URL_TRANSLATE + from + "&equip=" + deviceid + content;
        getDataAsync(url);
    }

    private void getDataAsync(final String url) {

        LogUtils.d(TAG, "translate url = " + url);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogUtils.w(TAG, "getDataAsync onFailure Error ", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String strReponse = response.body().string();
                    LogUtils.d(TAG, "strReponse ==" + strReponse);
                    try {
                        JSONObject reponJson = new JSONObject(strReponse);
                        String reponCode = reponJson.getString("code");
                        reponCode = reponCode.trim();
                        if ("0".equals(reponCode)) {
                            //
                            String transResult = reponJson.getString("data");
                            if (transResult == null || TextUtils.isEmpty(transResult) || transResult.equals("[]"))
                                return;
                            doLocalTTS(transResult, false);
                        } else {
                            LogUtils.d(TAG, "getDataAsync failed ");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        LogUtils.w(TAG, "getDataAsync onResponse Error ", e);
                    }
                }
            }
        });
    }

    public void postChatInfo(boolean isInput, String deviceID, String content) {

        LogUtils.d(TAG, "isInput " + isInput);
        LogUtils.d(TAG, "deviceID " + deviceID);
        LogUtils.d(TAG, "content " + content);

        if (deviceID == null || content == null || TextUtils.isEmpty(content)) {
            return;
        }

        if (okHttpClient == null) okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        //MediaType  设置Content-Type 标头中包含的媒体类型值
        FormBody formBody = new FormBody.Builder()
                .add("equip", deviceID)
                .add("content", content)
                .add("status", isInput ? "3" : "2")
                .build();

        Request request = new Request.Builder()
                .url(ReSmartContants.URL + ReSmartContants.URL_CHAT)//请求的url
                .post(formBody)
                .build();

        Call call = okHttpClient.newCall(request);
        call.enqueue(mOkHttpCallBacl);
    }

    public void postFile(final File file, String deviceID) {
        LogUtils.d(TAG, "post file");
        try {
            if (file.exists() && deviceID != null) {
                String audioBase64 = SmartUtil.getFileByteString(file);

                if (okHttpClient == null) okHttpClient = new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build();
                //MediaType  设置Content-Type 标头中包含的媒体类型值
                FormBody formBody = new FormBody.Builder()
                        .add("equip", deviceID)
                        .add("media", audioBase64)
                        .build();

                Request request = new Request.Builder()
                        .url(ReSmartContants.URL + ReSmartContants.URL_POST_VOICE)//请求的url
                        .post(formBody)
                        .build();

                Call call = okHttpClient.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        LogUtils.w(TAG, "onFailure ", e);
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        LogUtils.v(TAG, "postFile response = " + response.body().string());
                        file.delete();
                    }
                });
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "get Base64 audio String Error ", e);
            e.printStackTrace();
        }
    }

    private static Callback mOkHttpCallBacl = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            LogUtils.w("post Chat onFailure " + e);
            e.printStackTrace();
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            LogUtils.v("post Chat response = " + response.body().string());
        }
    };

}
