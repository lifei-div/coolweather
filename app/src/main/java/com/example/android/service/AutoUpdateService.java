package com.example.android.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;

import com.bumptech.glide.Glide;
import com.example.android.WeatherActivity;
import com.example.android.gson.Weather;
import com.example.android.util.HttpUtil;
import com.example.android.util.Utility;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.Future;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updateBingPic();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 8 * 60 * 60 * 1000; //这是8小时的毫秒数
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this,AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getActivity(this,0,i,0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 后台更新图片
     */
    private void updateBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequset(requestBingPic, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String bingPic = response.body().string();
                SharedPreferences.Editor editor = getSharedPreferences
                        ("weather_date",MODE_PRIVATE).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();

            }
        });






    }

    /**
     *   后台更新天气
     */
    private void updateWeather() {
        SharedPreferences preferences = getSharedPreferences("weather_date",MODE_PRIVATE);
        String weatherString = preferences.getString("weather",null);
        if (weatherString != null){
            //有缓存直接解析天气
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;
            String weatherUrl =" http://guolin.tech/api/weather?cityid=" +
                    weatherId + "&key=40e19172765f45da9107757783d8c1ff";
            HttpUtil.sendOkHttpRequset(weatherId, new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                     String responseText = response.body().string();
                     Weather weather = Utility.handleWeatherResponse(responseText);
                            if (weather != null &&"ok".equals(weather.status)){
                                SharedPreferences.Editor editor = getSharedPreferences
                                        ("weather_date",MODE_PRIVATE).
                                        edit();
                                editor.putString("weather",responseText);
                                editor.apply();
                            }

                }
            });

        }
    }
}
