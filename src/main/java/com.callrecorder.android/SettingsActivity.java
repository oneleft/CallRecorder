/* Copyright (c) 2016 ShadowNinja
 *
 * This file is part of Call recorder For Android.
 *
 * Call recorder For Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Call recorder For Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Call recorder For Android.  If not, see <http://www.gnu.org/licenses/>
 */
package com.callrecorder.android;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.Html;
import android.util.Log;

import com.callrecorder.android.entity.Constants;
import com.callrecorder.android.util.FileHelper;
import com.callrecorder.android.util.UserPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class SettingsActivity extends PreferenceActivity {
	private static final int REQUEST_STORAGE_LOCATION_DCA = 0;
	private static final int REQUEST_STORAGE_LOCATION_SAF = 1;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		Preference sl = findPreference("storage_location");
		sl.setOnPreferenceClickListener(preference -> {
			showChooseDataFolderDialog();
			return true;
		});
		updateStorageLocationText();
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK)
			return;
		if (requestCode == REQUEST_STORAGE_LOCATION_DCA) {
			UserPreferences.setStorageUri(Uri.fromFile(new File(
				data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR))));
			updateStorageLocationText();
		} else if (requestCode == REQUEST_STORAGE_LOCATION_SAF) {
			if (data == null) return;
			Uri uri = data.getData();
			Log.i(Constants.TAG, "Uri: " + uri.toString());
			getContentResolver().takePersistableUriPermission(uri,
				Intent.FLAG_GRANT_READ_URI_PERMISSION |
					Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			UserPreferences.setStorageUri(uri);
			updateStorageLocationText();
		}
	}

	private void showChooseDataFolderDialog() {
		Uri currentStorage = UserPreferences.getStorageUri();

		List<File> checkPaths = new ArrayList<>();

		// Add the default path
		checkPaths.add( UserPreferences.getDefaultStoragePath() );

//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//			Collections.addAll(checkPaths, getExternalFilesDirs(null));
//		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
//			File extDir = getExternalFilesDir(null);
//			if (extDir != null)
//				checkPaths.add(extDir);
//		}

		final List<File> folders = new ArrayList<>(checkPaths.size());

		List<CharSequence> choices = new ArrayList<>(checkPaths.size());
		choices.add(getString(R.string.location_select_manual));
		int selectedIndex = 0;

		for (int i = 0; i < checkPaths.size(); i++) {
			File dir = checkPaths.get(i);
			if (dir == null || !dir.exists() || !dir.canRead() || !dir.canWrite()) {
				continue;
			}
			folders.add(dir);
			if (currentStorage.equals(Uri.fromFile(dir))) {
				selectedIndex = i + 1;
			}

			String path = dir.getAbsolutePath();
			long bytes = FileHelper.getFreeSpaceAvailable(path);
			String freeSpace = FileHelper.addUnits(bytes);
			if (!path.endsWith("/")) {
				path += "/";
			}
			String appFilesPostfix = BuildConfig.APPLICATION_ID + "/files/";
			if (path.endsWith("Android/data/" + appFilesPostfix)) {
				path = path.substring(0, path.length() - appFilesPostfix.length() - 14);
			} else if (path.endsWith(appFilesPostfix)) {
				path = path.substring(0, path.length() - appFilesPostfix.length() - 1);
			}
			//noinspection deprecation
			choices.add(Html.fromHtml("<small>" + path
				+ " [" + freeSpace + "]</small>"));
		}
		AlertDialog dialog = new AlertDialog.Builder(this)
			.setTitle(R.string.choose_storage_location)
			.setSingleChoiceItems(choices.toArray(new CharSequence[choices.size()]),
				selectedIndex,
				(dialog1, which) -> {
					dialog1.dismiss();
					if (which == 0) {
						openStorageLocationDirectoryChooser();
						return;
					}
					File folder = folders.get(which - 1);
					Log.d(Constants.TAG, "Data folder: " + folder.getAbsolutePath());
					UserPreferences.setStorageUri(Uri.fromFile(folder));
					updateStorageLocationText();
				})
			.setNegativeButton(R.string.close, (dialog1, i) -> {
				dialog1.dismiss();
			})
			.create();
		dialog.show();
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void openStorageLocationDirectoryChooser() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),
				REQUEST_STORAGE_LOCATION_SAF);
		} else {
			startActivityForResult(new Intent(this, DirectoryChooserActivity.class),
				REQUEST_STORAGE_LOCATION_DCA);
		}
	}

	@SuppressWarnings("deprecation")
	private void updateStorageLocationText() {
		findPreference("storage_location")
			.setSummary(UserPreferences.getStorageUri().toString());
	}
}
