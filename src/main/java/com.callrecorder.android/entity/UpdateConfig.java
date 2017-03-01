package com.callrecorder.android.entity;

/**
 * Created by jokinkuang on 2017/3/1.
 */

/*
{
    "version": "1.0.0",
    "url": "http://callrecorder-1.0.0.apk",
    "description": "callrecorder1.0.0\n版本更新：\n1、送礼数量可自定义\n2、添加连送按钮，送礼后5秒内点击可连送,
    "size": 1000,
    "force": 0,
    "ignore": 0,
    "ignore_auto_check":0
}
*/
public class UpdateConfig {
    public String version = "";
    public int version_code;
    public String url = "";
    public long size;              // kb
    public String md5 = "";
    public String description = "";
    public int force;
    public int ignore;
    public int ignore_auto_check;

    @Override
    public String toString() {
        return "UpdateConfig{" +
                "version='" + version + '\'' +
                ", version_code=" + version_code +
                ", url='" + url + '\'' +
                ", size=" + size +
                ", md5='" + md5 + '\'' +
                ", description='" + description + '\'' +
                ", force=" + force +
                ", ignore=" + ignore +
                ", ignore_auto_check=" + ignore_auto_check +
                '}';
    }
}
