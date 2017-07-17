package com.coolweather.yk;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.bumptech.glide.Glide;
import com.coolweather.yk.db.County;
import com.coolweather.yk.gson.Forecast;
import com.coolweather.yk.gson.Weather;
import com.coolweather.yk.service.AutoUpdateService;
import com.coolweather.yk.util.HttpUtil;
import com.coolweather.yk.util.Utility;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    public DrawerLayout drawerLayout;

    private Button navButton;

    public SwipeRefreshLayout swipeRefresh;
    private String mWeatherId;

    private ScrollView weatherLayout;

    private TextView titleCity;

    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    private ImageView bingPicImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        //初始化各控件
        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swip_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_button);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            //无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d("mWeatherId",mWeatherId);
                requestWeather(mWeatherId);
            }
        });
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        titleCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("test","1111111111111111111");
                Intent intent = new Intent("com.baidu.action.RECOGNIZE_SPEECH");
                Log.d("test","2222222222222222222");
                intent.putExtra("grammar", "asset:///baidu_speech_grammar.bsg"); // 设置离线的授权文件(离线模块需要授权), 该语法可以用自定义语义工具生成, 链接http://yuyin.baidu.com/asr#m5
                Log.d("test","3333333333333333333");
                //intent.putExtra("slot-data", your slots); // 设置grammar中需要覆盖的词条,如联系人名
                startActivityForResult(intent, 1);
                Log.d("test","44444444444444444444");

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("test","aaaaaaaaaaaaaaaaaaa");

        if (resultCode == RESULT_OK) {
            Log.d("test","bbbbbbbbbbbbbbbbbb");
            Bundle results = data.getExtras();
            Log.d("test","ccccccccccccccccccccc");
            ArrayList<String> results_recognition = results.getStringArrayList("results_recognition");
            Log.d("test","dddddddddddddddddddddd");

            final String str = results_recognition.get(0);
            str.replace("[","");
            str.replace("]","");

            final Cursor cursor = DataSupport.findBySQL("select * from county where countyName = '"+str+"'");
            if (cursor.moveToFirst()){
//                Log.d("test","cursor："+cursor.getString(0)+"   "+cursor.getString(1)+"  "+cursor.getString(2)+"  "+cursor.getString(3));
                String weatherUrl = "http://guolin.tech/api/weather?cityid=" + cursor.getString(2) + "&key=52f860176ebb4ff0bf866ea089419e85";
                HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                                swipeRefresh.setRefreshing(false);
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String responseText = response.body().string();
                        final Weather weather = Utility.handleWeatherResponse(responseText);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (weather != null && weather.status.equals("ok")) {
                                    Toast.makeText(WeatherActivity.this,"当前查询的是："+str,Toast.LENGTH_SHORT).show();
                                    mWeatherId = cursor.getString(2);
                                    Log.d("test", "cityName:"+weather.basic.cityName+"-"+responseText);
                                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                                    editor.putString("weather", responseText);
                                    editor.apply();
                                    showWeatherInfo(weather);
                                } else {
                                    Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                                }
                                swipeRefresh.setRefreshing(false);
                            }
                        });
                    }
                });
                loadBingPic();
            }else{
                Toast.makeText(WeatherActivity.this,"不能查询城市："+str,Toast.LENGTH_SHORT).show();
            }
//            txtLog.append("识别结果(数组形式): " + results_recognition + "\n");
        }
    }

//    private EventManager mWpEventManager;
//
//        @Override
//    protected void onResume() {
//        super.onResume();
//
//        // 唤醒功能打开步骤
//        // 1) 创建唤醒事件管理器
//        mWpEventManager = EventManagerFactory.create(WeatherActivity.this, "wp");
//
//        // 2) 注册唤醒事件监听器
//        mWpEventManager.registerListener(new EventListener() {
//            @Override
//            public void onEvent(String name, String params, byte[] data, int offset, int length) {
//                Log.d("WeatherActivity", String.format("event: name=%s, params=%s", name, params));
//                try {
//                    JSONObject json = new JSONObject(params);
//                    if ("wp.data".equals(name)) { // 每次唤醒成功, 将会回调name=wp.data的时间, 被激活的唤醒词在params的word字段
//                        String word = json.getString("word");
//                        Toast.makeText(WeatherActivity.this,"唤醒成功, 唤醒词: "+word,Toast.LENGTH_SHORT).show();
////                        txtLog.append("唤醒成功, 唤醒词: " + word + "\r\n");
//                    } else if ("wp.exit".equals(name)) {
//                        Toast.makeText(WeatherActivity.this,"唤醒已经停止: "+params,Toast.LENGTH_SHORT).show();
////                        txtLog.append("唤醒已经停止: " + params + "\r\n");
//                    }
//                } catch (JSONException e) {
//                    throw new AndroidRuntimeException(e);
//                }
//            }
//        });
//
//        // 3) 通知唤醒管理器, 启动唤醒功能
//        HashMap params = new HashMap();
//        params.put("kws-file", "assets:///WakeUp.bin"); // 设置唤醒资源, 唤醒资源请到 http://yuyin.baidu.com/wake#m4 来评估和导出
//        mWpEventManager.send("wp.start", new JSONObject(params).toString(), null, 0, 0);
//
////        txtLog.setText(DESC_TEXT);
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        // 停止唤醒监听
//        mWpEventManager.send("wp.stop", null, null, 0, 0);
//    }

    /**
     * 根据天气id请求城市天气信息
     */
    public void requestWeather(final String weatherId) {
//        String weatherUrl = "https://free-api.heweather.com/v5/weather?city="+weatherId+"&key=52f860176ebb4ff0bf866ea089419e85";
        Log.d("test", "weatherId:" + weatherId);
        mWeatherId = weatherId;
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=52f860176ebb4ff0bf866ea089419e85";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && weather.status.equals("ok")) {
                            Log.d("test", "cityName:"+weather.basic.cityName+"-"+responseText);
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPic();
    }

    /**
     * 处理并展示Weather实体类中的数据
     */
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        try {
            String comfort = "舒适度：" + weather.suggestion.comfort.info;
            String carWash = "洗车指数：" + weather.suggestion.carWash.info;
            String sport = "运动指数：" + weather.suggestion.sport.info;
            comfortText.setText(comfort);
            carWashText.setText(carWash);
            weatherLayout.setVisibility(View.VISIBLE);
            sportText.setText(sport);
        }catch (Exception e){

        }
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }
    /**
     * 加载必应每日一图
     */
    private void loadBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }
}
