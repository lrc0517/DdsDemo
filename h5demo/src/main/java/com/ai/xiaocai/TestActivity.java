package com.ai.xiaocai;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class TestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
    }

    public void startServer(View view) {
        startService(new Intent(this, XiaocaiService.class));
    }

    public void stopServer(View view) {
        stopService(new Intent(this, XiaocaiService.class));
    }

    public void startTranslate(View view) {
        sendBroadcast(new Intent("action_speech_translate_start"));
    }

    public void stopTranslate(View view) {
        sendBroadcast(new Intent("action_speech_translate_end"));
    }


    public void pause(View view) {
        sendBroadcast(new Intent("action_speech_play_pause"));
    }

    public void sleep(View view) {
        sendBroadcast(new Intent("action_speech_sleep"));
    }

    public void fmMusic(View view) {
        sendBroadcast(new Intent("action_speech_chat_recieve"));
    }

    public void sendTXT(View view) {
        sendBroadcast(new Intent("action_speech_send_txt"));
    }
}
