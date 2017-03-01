package com.callrecorder.android.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.callrecorder.android.R;
import com.callrecorder.android.entity.Constants;
import com.callrecorder.android.toast.ToastCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

/**
 * Created by jokinkuang on 2017/3/1.
 */
public class URLHelper {
    private static final long AUTO_CHECK_INTERVAL = 12 * 3600 * 1000L;
    private static final int CONNECT_TIMEOUT = 20000;
    private static final int READ_TIMEOUT = 15000;

    public static final String HTTP_TIMEOUT = "Timeout";
    public static final String HTTP_IO_EXCEPTION = "IO_Exception";
    public static final String HTTP_EXCEPTION = "Exception";
    public static final String HTTP_SECURITY_EXCEPTION = "Security_Exception";

    public static String lastErr;

    public static String getUpdateData(String[] list) {
        String data = null;
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        lastErr = "";
        try {
            for (String uri : list) {
                URL url = new URL(uri);
                try {
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(CONNECT_TIMEOUT);
                    connection.setReadTimeout(READ_TIMEOUT);
                    connection.setUseCaches(false); // no cache !
                    connection.connect();
                    if (connection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                        InputStream inputStream = connection.getInputStream();
                        reader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuffer buffer = new StringBuffer();
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            buffer.append(line);
                        }
                        data = buffer.toString();
                        if (!TextUtils.isEmpty(data)) {
                            break;
                        }
                    }
                } catch (SocketTimeoutException e) {
                    handleErr(Constants.UPDATE_CHECK_FAILED, HTTP_TIMEOUT);
                    e.printStackTrace();
                } catch (IOException e) {
                    handleErr(Constants.UPDATE_CHECK_FAILED, HTTP_IO_EXCEPTION);
                    e.printStackTrace();
                } catch (SecurityException e) {
                    handleErr(Constants.UPDATE_CHECK_FAILED, HTTP_SECURITY_EXCEPTION);
                    e.printStackTrace();
                } catch (Exception e) {
                    handleErr(Constants.UPDATE_CHECK_FAILED, HTTP_EXCEPTION);
                    e.printStackTrace();
                } finally {
                    if (null != connection) {
                        connection.disconnect();
                        if (null != reader) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return data;
    }

    private static Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            // This is where you do your work in the UI thread.
            // Your worker tells you in the message what to do.
            ToastCompat.makeText(APPHelper.gMainContext, R.string.security_exception).show();
        }
    };

    public static void handleErr(String err, String msg) {
        lastErr = msg;

        Log.e(Constants.TAG, "Update error: " + err + ", message: " + msg);
        if (msg.equalsIgnoreCase(HTTP_SECURITY_EXCEPTION)) {
            Message message = mHandler.obtainMessage();
            message.sendToTarget();
            // Toast Must be in the MainThread, use Handler to do that.
        }
    }
}
