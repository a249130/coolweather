package com.coolweather.yk.gson;

/**
 * Created by YK on 2017/7/5.
 */

public class AQI {
    public AQICity city;

    public class AQICity{
        public String aqi;
        public String pm25;
    }
}
