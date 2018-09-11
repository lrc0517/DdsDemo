package com.ai.xiaocai.h5Manager;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

import com.ai.xiaocai.bean.BeanCook;
import com.ai.xiaocai.number.utils.SmartUtil;
import com.ai.xiaocai.number.utils.VoiceResultBean;
import com.ai.xiaocai.utils.LocalMusicUtil;
import com.ai.xiaocai.utils.LogUtils;
import com.aispeech.dui.dds.DDS;
import com.aispeech.dui.dds.exceptions.DDSNotInitCompleteException;
import com.aispeech.dui.dsk.duiwidget.TextWidget;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Lucien on 2018/6/30.
 */

public class DDSResultManager {
    private static final String TAG = "Mason/result";
    private final ReminderManager mReminderManager;
    private final MusicManager mMusicControl;


    public interface ResultCallBack {


        void onPlayError();

        void onPlayComplete();

        void onVoicesPlayComplete();

        void onLocalMusicPlay(String name, String url);

        void onMessageCallback(String messageBackStr);

        void onCommandCallBack(String command, String strBack);

        void onCommandPray(boolean pray);

        void onCommandMusic(int i);

        void onCommandVideo(String linkUrl);

        void onCommandRest();

        void onCommandPraySearch(String title);
    }

    private final Context mContext;
    private AudioManager mAudioManager;
    private int mMaxVolume;

    private ResultCallBack mResultCallBack;

    public DDSResultManager(Context context, ResultCallBack l) {
        this.mResultCallBack = l;
        this.mContext = context;

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.e(TAG,"mMaxVolume = "+mMaxVolume);
        mReminderManager = new ReminderManager(context);
        mMusicControl = new MusicManager(context, mMusicControlListener);
    }


    public void stop() {
        if (mMusicControl != null) mMusicControl.stop();
    }

    public void pauseMusic() {
        if (mMusicControl != null) mMusicControl.pausePlay();
    }


    public void setMessageResult(String message, String data) throws Exception {
        LogUtils.e("message = " + message + ",data = " + data);
        if (message.equals("context.widget.content")) {
            JSONObject jsonData = new JSONObject(data);
            String intentName = jsonData.optString("intentName");
            if ("快递查询".equals(intentName)) {
                //2018-06-29 13:54:47：快件已被 已签收 签收<br>2018-06-29 10:58:20：[深圳市]广东深圳公司宝安区西乡铁仔山分部派件员 邓荣武 18018713482正在为您派件<br>
                //2018-07-20 07:29:47：[泉州市]已离开泉州处理中心，发往中国邮政集团公司南昌邮区中心局邮件处理中心<br>2018-07-19 14:19:00：泉州市分公司电子商务仓储配送揽投部已收件（揽投员姓名：孙伟达,联系电话:）<br>"
                String subTitle = jsonData.optString("subTitle");
                String substring = null;
                if (subTitle!=null){
                    if (subTitle.contains("<")){
                        int i = subTitle.indexOf("<");
                        substring = subTitle.substring(0, i);
                    }else {
                        substring = subTitle;
                    }
                }else {
                    substring = "";
                }
                if (mResultCallBack != null) mResultCallBack.onMessageCallback(substring);
            }
        } else if (message.equals("context.widget.web")) {

        } else if (message.equals("context.widget.custom")) {
            JSONObject jsonData = new JSONObject(data);
            String skillId = jsonData.optString("skillId");
            if ("2018032300000019".equals(skillId)) { //AITEK菜谱
                // String extra = jsonData.optString("extra");
                // LogUtils.saveLongString(TAG,data);

                Gson gson = new Gson();
                BeanCook cookBean = gson.fromJson(data, new TypeToken<BeanCook>() {
                }.getType());
                List<BeanCook.ExtraBean.ListBean> list = cookBean.getExtra().getList();
                int num = (int) (Math.random() * list.size());
                BeanCook.ExtraBean.ListBean listBean = list.get(num);
                List<BeanCook.ExtraBean.ListBean.ProcessBean> process = listBean.getProcess();
                StringBuffer stringBuffer = new StringBuffer("步骤：");
                for (BeanCook.ExtraBean.ListBean.ProcessBean bean : process) {
                    String pcontent = bean.getPcontent();
                    if (pcontent != null) {
                        pcontent.trim();
                        stringBuffer.append(pcontent);
                        stringBuffer.append(".");
                    }


                }
                if (mResultCallBack != null)
                    mResultCallBack.onMessageCallback(new String(stringBuffer));
            }
        } else if (message.equals("context.widget.list")) {

        } else if (message.equals("context.widget.media")) {

        }
    }


    public void setNativeResult(String nativeApi, String data) throws Exception {
        LogUtils.i("nativeApi = " + nativeApi + ",date = " + data);
        if (nativeApi.equals("query_battery")) {

            JSONObject jsonData = new JSONObject(data);
            String intentName = jsonData.optString("intentName");
            // 执行查询电量操作
            Intent batteryInfoIntent = mContext.getApplicationContext()
                    .registerReceiver(null,
                            new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            int level = batteryInfoIntent.getIntExtra("level", 0);//电量（0-100）

            String battery = "电量剩余,百分之" + level;
            DDS.getInstance().getAgent().feedbackNativeApiResult(nativeApi, new TextWidget().setText(battery));

        } else if (nativeApi.contains("settings")) {
            //DO settings
            doNativeSettings(nativeApi, data);
        } else if (nativeApi.contains("alarm")) {
            //DO alarm
            doNativeAlarm(nativeApi, data);
        }
    }

    private void doNativeAlarm(String nativeApi, String data) throws Exception {

        JSONObject dataJson = new JSONObject(data);
        String intent = dataJson.getString("intent");
        String backStr = "正在为您" + intent;
        String backTime = null;
        if ("alarm_localhook_local_alarm_add_single".equals(nativeApi)) {
            String date = dataJson.getString("date");
            String time = backTime = dataJson.getString("time");
            if (mReminderManager != null) mReminderManager.setSingleAlarm(date, time);
        } else if ("alarm_localhook_local_add_time".equals(nativeApi)) {
            String addTime = backTime = dataJson.getString("addTime");
            if (mReminderManager != null) mReminderManager.setInterval(addTime);
        } else if ("alarm_localhook_local_alarm_add_repeat".equals(nativeApi)) {
            String timeOfDay = backTime = dataJson.getString("timeOfDay");
            String dayOfWeek = dataJson.getString("dayOfWeek");
            if (mReminderManager != null) mReminderManager.setRepeatAlarm(dayOfWeek, timeOfDay);
        } else if ("alarm_localhook_local_alarm_add_timer".equals(nativeApi)) {
            String timerNum = backTime = dataJson.getString("timerNum");
            if (mReminderManager != null) mReminderManager.setInterval(timerNum);
        } else if ("alarm_localhook_local_alarm_add_timer_by_action".equals(nativeApi)) {

        } else if ("alarm.set".equals(nativeApi)) {
            try {
                String relativeTime = backTime = dataJson.getString("relativeTime");
                if (mReminderManager != null) mReminderManager.setInterval(relativeTime);
            } catch (Exception e) {
                LogUtils.w(TAG, "alarm.set Error ", e);
                try {
                    String date = dataJson.getString("date");
                    String time = backTime = dataJson.getString("backTime");
                    if (mReminderManager != null) mReminderManager.setSingleAlarm(date, time);
                } catch (Exception ex) {
                    String date = dataJson.getString("date");
                    String time = backTime = dataJson.getString("time");
                    if (mReminderManager != null) mReminderManager.setSingleAlarm(date, time);
                }

            }
        } else if ("alarm.list".equals(nativeApi)) {
            backStr = "我还没有学会呢。";
        } else if ("alarm.delete".equals(nativeApi)) {
            backStr = "我还没有学会呢。";
        }


        DDS.getInstance().getAgent().
                feedbackNativeApiResult(nativeApi,
                        new TextWidget()
                                .addExtra("time", backTime)
                                .addExtra("mode", "")
                                .setText(backStr));
    }

    private void doNativeSettings(String nativeApi, String data) {

        if ("settings.volume.inc".equals(nativeApi)) {
            int c = setVolumeUp();

            DDS.getInstance().getAgent().
                    feedbackNativeApiResult(nativeApi,
                            new TextWidget().setText("音量已调到" + c));
        } else if ("settings.volume.dec".equals(nativeApi)) {
            int c = setVolumeDown();
            DDS.getInstance().getAgent().
                    feedbackNativeApiResult(nativeApi,
                            new TextWidget().setText("音量已调到" + c));
        } else if ("settings.volume.set".equals(nativeApi)) {
            LogUtils.e("nativeApi = " + nativeApi + ",data = " + data);
            try {
                int c = 0;
                JSONObject dataJson = new JSONObject(data);
                String intent = dataJson.getString("intent");
                if ("设置音量".equals(intent)) { //volume
                    String volume = dataJson.getString("volume");
                    int i = Integer.parseInt(volume.trim());
                    if (i < 0 || i > 100) {
                        DDS.getInstance().getAgent().
                                feedbackNativeApiResult(nativeApi,
                                        new TextWidget().setText("亲，不要为难本精灵了。"));
                        try {
                            DDS.getInstance().getAgent().getTTSEngine().speak("亲，不要为难本精灵了。", 0);
                            JSONObject output = new JSONObject();
                            output.put("text", "亲，不要为难本精灵了。");
                            DDS.getInstance().getAgent().getBusClient().publish("context.output.text", output.toString());

                        } catch (DDSNotInitCompleteException e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                    c = setSoundsProgress(i);
                } else if ("音量调到最大".equals(intent)) {
                    c = setSoundsProgress(100);
                } else if ("音量调到最小".equals(intent)) {
                    c = setSoundsProgress(10);
                }
                DDS.getInstance().getAgent().
                        feedbackNativeApiResult(nativeApi,
                                new TextWidget().setText("音量已调到" + c));
            } catch (JSONException e) {
                e.printStackTrace();

            }
        } else if ("settings.mutemode.open".equals(nativeApi)) {
            int c = setSoundsProgress(10);
            DDS.getInstance().getAgent().
                    feedbackNativeApiResult(nativeApi,
                            new TextWidget().setText("音量已调到" + c));
        }
    }

    //+=================================================+//
    public void setCommandResult(String command, String data) throws Exception {
        LogUtils.e("command = " + command + ",data = " + data);
        JSONObject dataJson = new JSONObject(data);
        String intent = dataJson.getString("intentName");
        if (command.equals("device_light")) {

            if (intent.equals("控制")) {
                String action = dataJson.getString("action");
                if (action.equals("on")) {
                    int preValue = Settings.System.getInt(mContext.getContentResolver(), "light_effect", 0);
                    if (preValue == 0) preValue = 1;
                    Settings.System.putInt(mContext.getContentResolver(), "light_effect", preValue);
                } else if (action.equals("off")) {
                    Settings.System.putInt(mContext.getContentResolver(), "light_effect", 0);
                }
            } else if (intent.equals("切换")) {
                int preValue = Settings.System.getInt(mContext.getContentResolver(), "light_effect", 0);
                if (preValue == 1) {
                    Settings.System.putInt(mContext.getContentResolver(), "light_effect", 2);
                } else if (preValue == 2) {
                    Settings.System.putInt(mContext.getContentResolver(), "light_effect", 3);
                } else if (preValue == 3) {
                    Settings.System.putInt(mContext.getContentResolver(), "light_effect", 1);
                }
            } else if (intent.equals("选择")) {
                String list = dataJson.getString("list");
                Settings.System.putInt(mContext.getContentResolver(), "light_effect", Integer.parseInt(list));
            }
            mContext.sendBroadcast(new Intent("action_broad_cast_led"));
        } else if (command.equals("device_shadow")) {
            String action = dataJson.getString("action");
            Intent intent1 =  new Intent("action_broad_cast_shadow");
            if (action.equals("on")) {
                intent1.putExtra("shadow_toggle",true);
            } else if (action.equals("off")) {
                intent1.putExtra("shadow_toggle",false);
            }
            mContext.sendBroadcast(intent1);
        } else if (command.equals("device_battery")) {
            Intent batteryInfoIntent = mContext
                    .registerReceiver(null,
                            new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = batteryInfoIntent.getIntExtra("level", 0);//电量（0-100）
            LogUtils.i("level = " + level);

            if (mResultCallBack != null)
                mResultCallBack.onCommandCallBack(command, "当前电量为" + level);
        } else if (command.equals("device_rest")) {
           if (mResultCallBack!=null)mResultCallBack.onCommandRest();
        } else if ("local_music_play".equals(command)) {
            String displayName = dataJson.getString("display_name");
            //FIXME play local music
            mMusicDisplayName = displayName;
            new Thread(mLocalMusic).start();
        }  else if ("local_music_pray_search".equals(command)) {
            String title = dataJson.getString("title");
            if (mResultCallBack != null)
                mResultCallBack.onCommandPraySearch(title);
        } else if ("local_music_control".equals(command)) {
            String state = dataJson.getString("state");
            int i = Integer.parseInt(state);
            if (i == 3 ||i == 4){
                if (mResultCallBack!=null)mResultCallBack.onCommandMusic(i);
            }else {
                if (mMusicControl != null) mMusicControl.doMusicCommand(i);
            }
        } else if ("local_music_pray".equals(command)) {
            //TODO 祷告
            String pray = dataJson.getString("pray");
            if ("start".equals(pray)) {
                if (mResultCallBack != null)
                    mResultCallBack.onCommandPray(true);
            } else if ("end".equals(pray)) {
                if (mResultCallBack != null)
                    mResultCallBack.onCommandPray(false);
            }
        }else if ("custom_media_video".equals(command)) {
            //TODO 外部服务器视频
            String linkUrl = dataJson.getString("linkUrl");
            //if (mMusicControl!=null)mMusicControl.setUrlAndPlayVideo(linkUrl);
            if (mResultCallBack!=null)mResultCallBack.onCommandVideo(linkUrl);
        } else if ("device_help".equals(command)) {
            //String help = dataJson.getString("help");
            SmartUtil.postHelpMessage(SmartUtil.getMac());
        }
    }

    //+=================================================+//
    public int setVolumeUp() {
        int curPro = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        curPro += 3;
        if (curPro >= mMaxVolume) {
            curPro = mMaxVolume;
        } else if (curPro <= 0) {
            curPro = 0;
        }
        int c = setSoundsProgress(curPro * 100 / mMaxVolume);
        return c;
    }

    public int setVolumeMinUp() {
        int curPro = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        Log.e(TAG,"curPro = "+curPro);
        Log.e(TAG,"mMaxVolume = "+mMaxVolume);
        curPro += 2;
        if (curPro >= mMaxVolume) {
            curPro = mMaxVolume;
        } else if (curPro <= 1) {
            curPro = 1;
        }
        Log.e(TAG,"curPro = "+curPro);
        int c = setSoundsMinProgress(curPro * 100 / mMaxVolume);
        return c;
    }

    public int setVolumeDown() {
        int curPro = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        curPro -= 3;
        if (curPro >= mMaxVolume) {
            curPro = mMaxVolume;
        } else if (curPro <= 0) {
            curPro = 0;
        }
        Log.e(TAG,"curPro = "+curPro);
        int c = setSoundsProgress(curPro * 100 / mMaxVolume);
        return c;
    }

    public int setVolumeMinDown() {
        int curPro = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        curPro -= 2;
        if (curPro >= mMaxVolume) {
            curPro = mMaxVolume;
        } else if (curPro <= 0) {
            curPro = 0;
        }
        Log.e(TAG,"curPro = "+curPro);
        int c = setSoundsMinProgress(curPro * 100 / mMaxVolume);
        return c;
    }

    private int setSoundsProgress(int currentPro) {
        LogUtils.e( "currentPro = " + currentPro);
        int v = currentPro * mMaxVolume / 100;
        LogUtils.e( "v = " + v);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0);
        try {
            DDS.getInstance().getAgent().getTTSEngine().speak("音量已调到" + currentPro, 0);
            JSONObject output = new JSONObject();
            output.put("text", "音量已调到" + currentPro);
            DDS.getInstance().getAgent().getBusClient().publish("context.output.text", output.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return currentPro;
    }

    private int setSoundsMinProgress(int currentPro) {
        LogUtils.e( "currentPro = " + currentPro);
        int v = currentPro * mMaxVolume / 100;
        LogUtils.e( "v = " + v);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0);
        /*try {
            DDS.getInstance().getAgent().getTTSEngine().speak("音量已调到" + currentPro, 0);
            JSONObject output = new JSONObject();
            output.put("text", "音量已调到" + currentPro);
            DDS.getInstance().getAgent().getBusClient().publish("context.output.text", output.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        return currentPro;
    }

    private String mMusicDisplayName;
    private Runnable mLocalMusic = new Runnable() {
        @Override
        public void run() {
            if (mMusicDisplayName != null) {
                // mMusicDisplayName = deciphering(mMusicDisplayName);
                String musicPath = null;//searchSystemDB(mContext, mMusicDisplayName);
                try {
                    //LocalMusicUtil.openDatabase(mContext);
                    musicPath = LocalMusicUtil.getPath(mContext,mMusicDisplayName);
                } catch (Exception e) {
                    e.printStackTrace();
                    LogUtils.e("Search Local DB Error ",e);
                }
                LogUtils.i("mMusicDisplayName = "+mMusicDisplayName);
                LogUtils.i("musicPath = "+musicPath);
                if (mResultCallBack != null)
                    mResultCallBack.onLocalMusicPlay(mMusicDisplayName, musicPath);
                if (mMusicControl != null && musicPath!=null) mMusicControl.setUrlAndPlay(musicPath);
            }
        }
    };

    private String searchSystemDB(Context context,String name){
        Cursor cursor = context.getContentResolver()
                .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                        MediaStore.Audio.Media.DISPLAY_NAME + "=" + "'" + name + "'", null,
                        MediaStore.Audio.Media._ID);
        String musicPath = null;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                byte[] data = cursor.getBlob(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                musicPath = new String(data, 0, data.length - 1);
                LogUtils.e("musicPath = " + musicPath);
            }
            cursor.close();
        }
        return musicPath;
    }



    private String deciphering(String mMusicDisPlayName) {
        String name = mMusicDisPlayName + ".mp3";
        return name;
    }

    //+=================================================+//
    public void playMediaData(String action, String url) {
        if (mMusicControl != null) {
            if ("pause".equals(action)) {
                String urlPlaying = mMusicControl.getUrlPlaying();
                if (urlPlaying != null && urlPlaying.equals(url))
                    mMusicControl.pausePlay();
            } else if ("play".equals(action)) {
                mMusicControl.setUrlAndPlay(url);
            }
        }
    }


    private int voiceIndex = 0;
    private List<VoiceResultBean.DataBean> mVoices;

    public void playMediaData(List<VoiceResultBean.DataBean> voices) {
        if (voices == null || voices.isEmpty()) return;
        mVoices = voices;
        add2PlayList();
    }

    private void add2PlayList() {
        if (mMusicControl != null) {
            try {
                LogUtils.e("add2PlayList voiceIndex = " + voiceIndex);
                VoiceResultBean.DataBean bean = mVoices.get(voiceIndex);
                LogUtils.e("add2PlayList bean = " + bean);
                String url = mVoices.get(voiceIndex).getUrl();
                LogUtils.e("add2PlayList url = " + url);
                mMusicControl.setUrlAndPlay(url, true);
            } catch (Exception e) {
                LogUtils.i("No more voices");
                voiceIndex = 0;
                if (mVoices != null) {
                    mVoices.clear();
                    mVoices = null;
                }
                if (mResultCallBack != null) mResultCallBack.onVoicesPlayComplete();
            }

        }
    }

    private MusicManager.MusicControlListener mMusicControlListener = new MusicManager.MusicControlListener() {
        @Override
        public void onPlayError() {
            if (mResultCallBack != null) mResultCallBack.onPlayError();
        }

        @Override
        public void onPlayComplete(boolean isPlayVoice) {
            if (isPlayVoice) {
                voiceIndex += 1;
                add2PlayList();
            } else {
                if (mResultCallBack != null) mResultCallBack.onPlayComplete();
            }
        }
    };

    public void palyControl() throws Exception {
        DDS.getInstance().getAgent()
                .triggerIntent("Toy播放控制", "通用控制", "下一个",
                        new JSONObject()
                                .put("语义槽名称一", "语义槽取值一").
                                put("语义槽名称二", "语义槽取值二").toString());
    }


}
