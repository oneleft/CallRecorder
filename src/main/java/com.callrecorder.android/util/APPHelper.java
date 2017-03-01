package com.callrecorder.android.util;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jokinkuang on 2017/3/1.
 */
public class APPHelper {
    public static Application gMainContext = null;

    static List<Integer> sLocalVer = null;
    static String sLocalName = null;
    static String sLocalNameOrigin = null;

    /** 不在这里转换，以免依赖太多 */
    public static List<Integer> getLocalVersion(){
        if( sLocalVer != null ){
            return sLocalVer;
        }

        loadLoaclVer(gMainContext);

        return sLocalVer;
    }

    static void loadLoaclVer(Context c){
        try {
            sLocalName = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
            sLocalNameOrigin = sLocalName;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Local Ver Package Error");
        }

        if( sLocalName == null ){
            throw new RuntimeException("Local Ver VersionName Not Exist");
        }

        // handle maven standard version like this "1.0.0-SNAPSHOT";
        int pos = sLocalName.indexOf('-');
        if (pos != -1) {
            sLocalName = sLocalName.substring(0, pos);
        }
        String verStr[] = sLocalName.split("\\.");

        if( verStr.length != 3 ){
            throw new RuntimeException("Local Ver VersionName Error");
        }

        sLocalVer = new ArrayList<>(3);

        try{
            for( int i = 0; i < 3; i++ ){
                sLocalVer.add( Integer.parseInt(verStr[i]) );
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Local Ver VersionName Error");
        }
    }
}
