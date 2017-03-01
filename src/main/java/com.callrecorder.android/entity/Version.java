package com.callrecorder.android.entity;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import java.util.List;
import java.util.Locale;

/**
 * Created by jokinkuang on 2017/3/1.
 */
public class Version {
    public static final String DOT = ".";

    public int mMajor = 0;
    public int mMinor = 0;
    public int mBuild = 0;

    @NonNull
    public static Version parseFromString(String version) {
        Version ver = new Version();

        if (version.matches("\\d{1,}.\\d{1,}.\\d{1,}")) {
            int dotPos = version.indexOf(DOT);
            int prevPos = 0;
            ver.mMajor = Integer.valueOf(version.substring(prevPos, dotPos));
            prevPos = dotPos + 1;
            dotPos = version.indexOf(DOT, prevPos);
            ver.mMinor = Integer.valueOf(version.substring(prevPos, dotPos));
            prevPos = dotPos + 1;
            ver.mBuild = Integer.valueOf(version.substring(prevPos));
            return ver;
        }

        return ver;
    }

    @NonNull
    public static Version parseFromList(List<Integer> list) {
        Version ver = new Version();

        if (list.size() >= 3) {
            ver.mMajor = list.get(0);
            ver.mMinor = list.get(1);
            ver.mBuild = list.get(2);
            return ver;
        }

        return ver;
    }

    public boolean bigThan(Version v) {
        return (mMajor > v.mMajor) || ( (mMajor == v.mMajor) && (mMinor > v.mMinor) )
                || ( (mMajor == v.mMajor) && (mMinor == v.mMinor) && (mBuild > v.mBuild) );
    }

    public boolean smallThan(Version v) {
        return (mMajor < v.mMajor) || ( (mMajor == v.mMajor) && (mMinor < v.mMinor) )
                || ( (mMajor == v.mMajor) && (mMinor == v.mMinor) && (mBuild < v.mBuild) );
    }


    public boolean equals(Object o) {
        Version v = (Version) o;
        return (mMajor == v.mMajor) && (mMinor == v.mMinor)
                && (mBuild == v.mBuild);
    }


    @SuppressLint("DefaultLocale")
    public String toString() {
        return String.format(Locale.getDefault(), "%d.%d.%d", mMajor, mMinor, mBuild);
    }
}
