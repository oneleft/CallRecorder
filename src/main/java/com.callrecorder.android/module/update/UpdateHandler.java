package com.callrecorder.android.module.update;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import com.callrecorder.android.R;
import com.callrecorder.android.entity.Constants;
import com.callrecorder.android.entity.UpdateConfig;
import com.callrecorder.android.entity.Version;
import com.callrecorder.android.toast.ToastCompat;
import com.callrecorder.android.util.APPHelper;
import com.callrecorder.android.util.FileHelper;
import com.callrecorder.android.util.URLHelper;
import com.callrecorder.android.util.UserPreferences;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.lang.ref.WeakReference;

/**
 * Created by jokinkuang on 2017/3/1.
 */
public class UpdateHandler {
    private WeakReference<Context> mContext;
    private boolean isChecking = false;
    private boolean isAutoCheck = false;    // AutoCheck: if it's the latest do not show any message.

    public UpdateHandler(Context context) {
        mContext = new WeakReference<Context>(context);
    }

    public boolean Assert() {
        return mContext.get() == null;
    }

    public void checkUpdateLater() {
        isAutoCheck = true;
        // 5 seconds later
        new CountDownTimer(5000, 5000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }
            @Override
            public void onFinish() {
                checkUpdate();
            }
        }.start();
    }

    public void checkUpdate() {
        if (isChecking) return;

        new AsyncTask<Void, String, UpdateConfig>() {
            @Override
            protected UpdateConfig doInBackground(Void... params) {
                isChecking = true;
                return checkUpdateConfig();
            }
            @Override
            protected void onPostExecute(UpdateConfig updateData) {
                onGetUpdateData(updateData);
                isChecking = false;
                isAutoCheck = false;    // reset
            }
        }.execute();
    }

    private UpdateConfig checkUpdateConfig() {
        String str = URLHelper.getUpdateData(new String[] { Constants.UPDATE_CONFIG_URL });
        UpdateConfig data = null;
        if (!TextUtils.isEmpty(str)) {
            try {
                data = new Gson().fromJson(str, UpdateConfig.class);
            } catch (JsonSyntaxException e) {
                URLHelper.handleErr(Constants.UPDATE_CHECK_FAILED, Constants.JSON_SYNTAX_ERROR);
                e.printStackTrace();
            }
        }
        return data;
    }

    private void onGetUpdateData(UpdateConfig config) {
        FileHelper.logD(Constants.TAG, config.toString());

        if (null == config || config.url == "") {
            if (isAutoCheck) {
                return;
            }

            ToastCompat.makeText(mContext.get(), R.string.update_error).show();
            return;
        }

        if (versionVerify(config)) {    // have new version
            showUpdateDialog(config);
        } else {                        // no new version
            if (isAutoCheck) {
                return;
            }

            Version localVer = Version.parseFromList( APPHelper.getLocalVersion() );
            String showContent = String.format("%s(%s)", mContext.get().getString(R.string.update_latest), localVer.toString());
            ToastCompat.makeText(mContext.get(), showContent).show();
        }
    }

    private boolean versionVerify(UpdateConfig config) {
        if (TextUtils.isEmpty(config.version)) {
            return false;
        }

        Version newVer = Version.parseFromString(config.version);
        Version localVer = Version.parseFromList(APPHelper.getLocalVersion());
        if ( (0 == config.ignore) && newVer.bigThan(localVer) ) {
            return true;
        }
        return false;
    }

    private void showUpdateDialog(UpdateConfig config) {
        if (Assert()) return;
        if (config.url == "") return;

        new AlertDialog.Builder(mContext.get())
                .setTitle(R.string.update_title)
                .setMessage(Html.fromHtml(String.format(mContext.get().getString(R.string.update_message), config.version)) + "\n" + config.description)
                .setNegativeButton(R.string.close, (dialog, i) -> {
                    dialog.dismiss();
                })
                .setPositiveButton(R.string.confirm, (dialog, i) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse( config.url ));
                        mContext.get().startActivity(intent);
                    } catch (Exception e) {
                        Log.e(Constants.TAG, "startActivity Exception:", e);
                        e.printStackTrace();
                    }
                })
                .setNeutralButton(R.string.nomore, (dialog, i) -> {
                    UserPreferences.setNoAutoCheckUpdate();
                })
                .create()
                .show();
    }

}
