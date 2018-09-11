package com.ai.xiaocai;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.MediaController;
import android.widget.VideoView;

import com.ai.xiaocai.h5Manager.UISpeechControl;
import com.ai.xiaocai.utils.LogUtils;
import com.ai.xiaocai.utils.NetworkUtil;
import com.hanks.htextview.typer.TyperTextView;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class XiaocaiService extends Service {

    private static final String TAG = "Mason/Xiaocai";

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {

                case UISpeechControl.MSG_HANDLER_AUTH_FAILED:
                    if (mUISpeechControl != null) mUISpeechControl.doAuth();
                    break;
                case UISpeechControl.MSG_HANDLER_UPDATA_OUT_STR:
                    break;
                case UISpeechControl.MSG_HANDLER_UPDATA_TIPS:
                    break;
                case UISpeechControl.MSG_HANDLER_TIPS_NEED_SHOW:
                    String str = (String) msg.obj;
                    if (isShowTips && str != null && mTtvContent != null) {
                        if (str.length() > 40)
                            str = str.substring(0, 40) + "...";
                        mTtvContent.animateText(str);
                        mAlertDialog.show();
                        mHandler.sendEmptyMessageDelayed(UISpeechControl.MSG_HANDLER_TTS_COMPLETE,10000);
                    }
                    break;
                case UISpeechControl.MSG_HANDLER_WAKE_UP_READY:
                    break;
                case UISpeechControl.MSG_HANDLER_WAKE_UP_SUCCESS:
                   /* if (mAlertVideoDialog != null) mAlertVideoDialog.dismiss();
                    if (mVideoView != null) mVideoView.stopPlayback();*/

                    break;
                case UISpeechControl.MSG_HANDLER_TTS_COMPLETE:
                    mHandler.removeMessages(UISpeechControl.MSG_HANDLER_TTS_COMPLETE);
                    mAlertDialog.dismiss();
                    break;
                case UISpeechControl.MSG_HANDLER_NET_DATA_DEVICE_GET_FAILED:
                    if (mUISpeechControl != null) mUISpeechControl.doNetDeviceID();
                    break;
                case UISpeechControl.MSG_HANDLER_NET_DATA_ONLINE_FAILED:
                    if (mUISpeechControl != null) mUISpeechControl.doNetDeviceOnline();
                    break;
                case UISpeechControl.MSG_HANDLER_NET_DATA_DEVICE_ONLINE:
                    if (mUISpeechControl != null) mUISpeechControl.doNetPeriodWork();
                    break;
                case UISpeechControl.MSG_HANDLER_NET_DATA_DO_PERIOD:
                    if (mUISpeechControl != null) mUISpeechControl.doNetPeriodWork();
                    break;
                case UISpeechControl.MSG_HANDLER_NET_DISABLED:
                    if (mUISpeechControl != null && !NetworkUtil.isWifiConnected(getApplicationContext()))
                        mUISpeechControl.doNetWorkDisabled();
                    break;
                case UISpeechControl.MSG_HANDLER_DDS_INIT_COMPLETE:
                    if (mUISpeechControl != null) mUISpeechControl.initWebUI(mView);
                    break;
                case UISpeechControl.MSG_HANDLER_NET_EVENT_POWEROFF:
                    LogUtils.e("MSG_HANDLER_NET_EVENT_POWEROFF");
                    XiaocaiService.this.sendBroadcast(new Intent("app_shut_down"));
                    break;

                case UISpeechControl.MSG_HANDLER_WLAN_CONFIRM_PASSWORD_FAILED:
                    LogUtils.e("MSG_HANDLER_WLAN_CONFIRM_PASSWORD");
                    if (mUISpeechControl != null && !NetworkUtil.isWifiConnected(getApplicationContext()))
                        mUISpeechControl.doConfirmPasswordFailed();
                    break;

                //LXQ 2018.7.27
                case UISpeechControl.MSG_HANDLER_COMMAND_MESSAGE_VIDEO:
                    // initVideoUI();
                    Intent in =  new Intent("action_speech_play_new_video");
                    in.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                    sendBroadcast(in);

                    final String videoUrl = (String) msg.obj;
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                          //   playCommandVideo(videoUrl);
                            String extension = MimeTypeMap.getFileExtensionFromUrl(videoUrl);
                            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                            Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
                            mediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mediaIntent.setDataAndType(Uri.parse(videoUrl), mimeType);
                            startActivity(mediaIntent);
                        }
                    }, 1500);
                    break;
                case UISpeechControl.MSG_HANDLER_COMMAND_MESSAGE_VIDEO_STOP:
                    Intent inte =  new Intent("action_speech_play_new_video");
                    inte.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                    sendBroadcast(inte);
                    break;
            }
        }
    };


    private AlertDialog mAlertDialog;
    private TyperTextView mTtvContent;

    private View mView;
    private UISpeechControl mUISpeechControl;


    public XiaocaiService() {
        LogUtils.v(TAG, "XiaocaiService");
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        AlertDialog.Builder mbuilder = new AlertDialog.Builder(getApplicationContext(), R.style.AppTheme_Dailog);
        mView = View
                .inflate(this, R.layout.custom_dialog, null);
        mTtvContent = (TyperTextView) mView.findViewById(R.id.ttv_dialog_content);


        mbuilder.setView(mView);
        mAlertDialog = mbuilder.create();
        mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        mUISpeechControl = new UISpeechControl(getApplicationContext(), mHandler);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
        intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction("speech_action_weather_request");
        intentFilter.addAction("action_speech_translate_start");
        intentFilter.addAction("action_speech_translate_end");
        intentFilter.addAction("action_update_speech_tone");
        intentFilter.addAction("action_update_speech_wakeup_words");
        intentFilter.addAction("action_speech_chat_recieve");
        intentFilter.addAction("action_speech_chat_speak_start");
        intentFilter.addAction("action_speech_chat_speak_end");
        intentFilter.addAction("action_speech_luancher_music");
        intentFilter.addAction("action_speech_play_pause");
        intentFilter.addAction("action_speech_daogao_on");
        intentFilter.addAction("action_speech_daogao_off");
        intentFilter.addAction("alarm_killed");
        intentFilter.addAction("action_speech_sleep");
        intentFilter.addAction("wifi_ap_opend");
        intentFilter.addAction("action_speech_connect_wifi");
        intentFilter.addAction("wlan_confirm_password");
        intentFilter.addAction("action_speech_send_txt");
        intentFilter.addAction("action_speech_vulome_action_down");
        intentFilter.addAction("action_speech_vulome_action_up");
        intentFilter.addAction("action_speech_pray_empty");
        intentFilter.addAction("com.fota.info.update");
        registerReceiver(mBroadcastReceiver, intentFilter);

        mHandler.sendEmptyMessageDelayed(UISpeechControl.MSG_HANDLER_NET_DISABLED, 30000);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.v(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        mUISpeechControl.onDestory();
        super.onDestroy();

    }

    private boolean isShowTips = true;

    private void setTipsShowState(boolean show) {
        if (!show) mAlertDialog.dismiss();
        isShowTips = show;
    }


    private AlertDialog mAlertVideoDialog;
    private View mVideoRootView;
    private VideoView mVideoView;
    private AlertDialog.Builder mVideobuilder;
    private int mVideoPosition = -1;

    private void initVideoUI() {
        if (mVideobuilder == null || mVideoRootView == null || mVideoView == null) {
            mVideobuilder = new AlertDialog.Builder(getApplicationContext(), R.style.AppTheme_Dailog);
            mVideoRootView = View
                    .inflate(this, R.layout.custom_video_dialog, null);
            mVideoView = (VideoView) mVideoRootView.findViewById(R.id.vv_dialog_content);
            mVideoView.canPause();
            mVideobuilder.setView(mVideoRootView);
            mAlertVideoDialog = mVideobuilder.create();
            mAlertVideoDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
    }

    private void playCommandVideo(String videoUrl) {
        if (mVideoView != null && mAlertVideoDialog != null) {
            mAlertVideoDialog.show();
            Uri uri = Uri.parse(videoUrl);
            // mVideoView.setVisibility(View.VISIBLE);
            mVideoView.setMediaController(new MediaController(this));
            mVideoView.setVideoURI(uri);
            //videoView.start();
            //videoView.requestFocus();

            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    mp.setLooping(false);
                    LogUtils.e("topVideoView.setOnPreparedListener -mp.start()");
                }
            });
            mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mVideoPosition = -1;
                    mAlertVideoDialog.dismiss();
                }
            });
            mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    return false;
                }
            });
        }
    }


    //+======================================================+//
    private boolean mIsConnected = false;
    private int mStatus = 1;
    private int mLevel = 0;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtils.e("ACTION = " + intent.getAction());

            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_NEW_STATE, 0);
                LogUtils.v(TAG, "wifiState = " + wifiState);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_DISABLING: //0
                        break;
                    case WifiManager.WIFI_STATE_DISABLED://1
                        break;

                    case WifiManager.WIFI_STATE_ENABLING://2
                        break;
                    case WifiManager.WIFI_STATE_ENABLED://3
                        break;
                    case WifiManager.WIFI_STATE_UNKNOWN://4
                        break;
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (parcelableExtra != null) {
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    NetworkInfo.State state = networkInfo.getState();
                    boolean isConnected = state == NetworkInfo.State.CONNECTED;
                    LogUtils.i("isConnected:" + isConnected);
                    if (isConnected && !mIsConnected) {
                        if (mUISpeechControl != null) mUISpeechControl.netWorkConnected();
                    } else if (!isConnected && mIsConnected) {
                        if (mUISpeechControl != null) mUISpeechControl.netWorkDisConnected();
                    }
                    mIsConnected = isConnected;
                }
            } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(intent.getAction())) {

                int linkWifiResult = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                Bundle extras = intent.getExtras();
                SupplicantState wifiState = (SupplicantState) extras.get(WifiManager.EXTRA_NEW_STATE);
                //SupplicantState wifiState = ;
                //intent.getExtra(WifiManager.EXTRA_NEW_STATE, -1);
                LogUtils.i("linkWifiResult = " + linkWifiResult + ",wifiState = " + wifiState);
                if (linkWifiResult == WifiManager.ERROR_AUTHENTICATING) {

                }

            } else if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {

                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                boolean present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false);
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
                LogUtils.v(TAG, "level = " + level + ",present = " + present + ",status = " + status);
                if (status == BatteryManager.BATTERY_STATUS_FULL) {
                    /*if (mStatus != status) {
                        if (mUISpeechControl != null) mUISpeechControl.batteryPowerFull();
                    }*/
                } else if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    if (level == 100 && mLevel != level) {
                        if (mUISpeechControl != null) mUISpeechControl.batteryPowerFull();
                    }
                } else {
                    if (level == 20 && mLevel != level) {
                        if (mUISpeechControl != null) mUISpeechControl.batteryPowerLowLevel(20);
                    } else if (level == 10 && mLevel != level) {
                        if (mUISpeechControl != null) mUISpeechControl.batteryPowerLowLevel(10);
                    } else if (level == 5 && mLevel != level) {
                        if (mUISpeechControl != null) mUISpeechControl.batteryPowerLowLevel(5);
                    } else {
                    }
                }
                mStatus = status;
                mLevel = level;

            } else if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
                if (mUISpeechControl != null) mUISpeechControl.batteryPowerConnected();
            } else if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) {
                if (mUISpeechControl != null) mUISpeechControl.batteryLow();
            } else if (Intent.ACTION_BATTERY_OKAY.equals(intent.getAction())) {
                if (mUISpeechControl != null) mUISpeechControl.batteryOkay();
            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {

            } else if ("speech_action_weather_request".equals(intent.getAction())) {
                if (mUISpeechControl != null) mUISpeechControl.getWeatherInfo();
            } else if ("action_speech_translate_start".equals(intent.getAction())) {
                //remove 2018.7.14 if (mUISpeechControl != null) mUISpeechControl.translateStart();
            } else if ("action_speech_translate_end".equals(intent.getAction())) {
                //remove 2018.7.14 //if (mUISpeechControl != null) mUISpeechControl.translateEnd();
            } else if ("action_update_speech_tone".equals(intent.getAction())) {
                int speechTone = Settings.System.getInt(getContentResolver(), "speech_tone", 0);
                LogUtils.v(TAG, "speechTone = " + speechTone);
                //FIXME modify speaker
            } else if ("action_speech_chat_recieve".equals(intent.getAction())) {
                //if (mUISpeechControl != null) mUISpeechControl.playVoiceMessage();
                if (mUISpeechControl != null) mUISpeechControl.playFMMusic();
            } else if ("action_speech_chat_speak_start".equals(intent.getAction())) {
                //if (mUISpeechControl != null) mUISpeechControl.voiceSpeakStart();
            } else if ("action_speech_chat_speak_end".equals(intent.getAction())) {
                //if (mUISpeechControl != null)mUISpeechControl.voiceSpeakEnd();
            } else if ("action_speech_play_pause".equals(intent.getAction())) {
                if (mUISpeechControl != null)
                    mUISpeechControl.stopAllPlayer();
            } else if ("action_speech_sleep".equals(intent.getAction())) {
                if (mUISpeechControl != null)
                    mUISpeechControl.changeWakeState();
            } else if ("wifi_ap_opend".equals(intent.getAction())) {
                if (mUISpeechControl != null) mUISpeechControl.wifiapOpend();
            } else if ("action_speech_connect_wifi".equals(intent.getAction())) {
                if (mUISpeechControl != null) mUISpeechControl.wifiapOpening();
            } else if ("wlan_confirm_password".equals(intent.getAction())) {
                mHandler.sendEmptyMessageDelayed(UISpeechControl.MSG_HANDLER_WLAN_CONFIRM_PASSWORD_FAILED, 10000);
            } else if ("action_speech_send_txt".equals(intent.getAction())) {
                if (mUISpeechControl != null) mUISpeechControl.sendTXT("播放视频约");
            } else if ("action_speech_vulome_action_down".equals(intent.getAction())) {
                if (mUISpeechControl != null) mUISpeechControl.setVolumedown();
            } else if ("action_speech_vulome_action_up".equals(intent.getAction())) {
                if (mUISpeechControl != null) mUISpeechControl.setVolumeUp();
            } else if ("action_speech_pray_empty".equals(intent.getAction())) {
                if (mUISpeechControl != null) mUISpeechControl.noPrayRespurce();
            } else if ("com.fota.info.update".equals(intent.getAction())) {
                if (mUISpeechControl != null) mUISpeechControl.needUpdate();
            } else if ("action_speech_daogao_on".equals(intent.getAction())) {
                setTipsShowState(false);
                if (mUISpeechControl != null) mUISpeechControl.setNetPlayState(false);
            } else if ("action_speech_daogao_off".equals(intent.getAction())) {
                setTipsShowState(true);
                if (mUISpeechControl != null) mUISpeechControl.setNetPlayState(true);
            }
            //action_speech_play_pauseing
        }
    };

    //+==================================================+//

}
