package com.coolweather.yk.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by YK on 2017/7/5.
 */

public class Basic {
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }
}
