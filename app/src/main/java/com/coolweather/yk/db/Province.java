package com.coolweather.yk.db;

import org.litepal.crud.DataSupport;

/**
 * Created by YK on 2017/7/4.
 * 省份
 */

public class Province extends DataSupport {

    private int id; //主键
    private String provinceName; //省份名称
    private int provinceCode; //省份代号

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public int getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(int provinceCode) {
        this.provinceCode = provinceCode;
    }


}
