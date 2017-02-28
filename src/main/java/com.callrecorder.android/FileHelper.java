/* Copyright (c) 2012 Kobi Krasnoff
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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.StatFs;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v4.provider.DocumentFile;
import android.telephony.PhoneNumberUtils;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class FileHelper {
	private static final boolean DEBUG = false;
	public static void logD(String tag, String msg) {
		if (DEBUG) {
			Log.d(tag, msg);
		}
	}

	/** Returns a file descriptor for a new recording file in write mode.
	 *
	 * @throws Exception
	 */
	public static DocumentFile getFile(Context context, @NonNull String phoneNumber) throws Exception {
		String date = (String) DateFormat.format("yyyyMMddHHmmss", new Date());
		String filename = date + "_" + cleanNumber(phoneNumber);

		return getStorageFile(context).createFile("audio/3gpp", filename);
	}

	public static void deleteAllRecords(Context context) {
		DocumentFile[] dirList = getStorageFile(context).listFiles();
		if (dirList == null)
			return;
		for (DocumentFile file : dirList) {
			file.delete();
		}
	}

	/// Obtains a contact name coresponding to a phone number.
	public static String getContactName(String phoneNum, Context context) {
		@SuppressWarnings("deprecation")
		String res = PhoneNumberUtils.formatNumber(phoneNum);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
				context.checkSelfPermission(Manifest.permission.READ_CONTACTS) !=
					PackageManager.PERMISSION_GRANTED) {
			return res;
		}
		Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
		String[] projection = new String[] {
				ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
				ContactsContract.CommonDataKinds.Phone.NUMBER };
		Cursor names = context.getContentResolver().query(uri, projection,
				null, null, null);
		if (names == null)
			return res;

		int indexName = names.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
		int indexNumber = names.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

		if (names.getCount() > 0) {
			names.moveToFirst();
			do {
				String name = names.getString(indexName);
				String number = cleanNumber(names.getString(indexNumber));

				if (PhoneNumberUtils.compare(number, phoneNum)) {
					res = name;
					break;
				}
			} while (names.moveToNext());
		}
		names.close();

		return res;
	}

	private static String cleanNumber(String phoneNumber) {
		return phoneNumber.replaceAll("[^0-9]", "");
	}

	/// Fetches list of previous recordings
	public static List<Recording> listRecordings(Context context) {
		final DocumentFile directory = getStorageFile(context);

		logD(Constants.TAG, "listing files");

		File dir = new File(SAFHelper.getPath(context, directory.getUri()));
		if (dir == null || dir.isDirectory() == false) {
			return new ArrayList<>();
		}

		File[] files = dir.listFiles();
		List<Recording> fileList = new ArrayList<>();

		logD(Constants.TAG, "loop files for size:" + files.length);
		for (File file : files) {
			if (file.isDirectory()) {
				continue;
			}
			if (! getFileExt(file.getName()).equalsIgnoreCase(".3gpp")) {
				Log.d(Constants.TAG, String.format(
					"'%s' didn't match the file name pattern",
					file.getName()));
				continue;
			}

			Recording recording = new Recording(file.getName());
			String phoneNum = recording.getPhoneNumber();
			//recording.setUserName(getContactName(phoneNum, context));
			fileList.add(recording);
		}
		logD(Constants.TAG, "listing files end");


		return fileList;
	}

	public static String getFileExt(String fileName) {
		final int pos = fileName.lastIndexOf(".");
		return pos == -1 ? "" : fileName.toLowerCase().substring(pos);
	}

	/// Get the number of free bytes that are available on the external storage.
	@SuppressWarnings("deprecation")
	public static long getFreeSpaceAvailable(String path) {
		StatFs stat = new StatFs(path);
		long availableBlocks;
		long blockSize;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			availableBlocks = stat.getAvailableBlocksLong();
			blockSize = stat.getBlockSizeLong();
		} else {
			availableBlocks = stat.getAvailableBlocks();
			blockSize = stat.getBlockSize();
		}
		return availableBlocks * blockSize;
	}

	/** Takes a size in bytes and converts it into a human-readable
	 * String with units.
	 */
	public static String addUnits(final long input) {
		int i = 0;
		long result = input;
		while (i <= 3 && result >= 1024)
			result = input / (long) Math.pow(1024, ++i);

		switch (i) {
		default: return result + " B";
		case 1: return result + " KiB";
		case 2: return result + " MiB";
		case 3: return result + " GiB";
		}
	}

	public static DocumentFile getStorageFile(Context context) {
		Uri uri = UserPreferences.getStorageUri();
		Log.d("file", uri.getPath()+"||"+uri.getScheme()+"||"+uri);
		String scheme = uri.getScheme();
		if (scheme == null || scheme.equals("file")) {
			return DocumentFile.fromFile(new File(uri.getPath()));
		} else {
			return DocumentFile.fromTreeUri(context, uri);
		}
	}

	public static Uri getContentUri(Context context, Uri uri) {
		if (uri.getScheme() == "content")
			return uri;
		// use FileProvider to access the external file!
		return FileProvider.getUriForFile(context,
				"com.callrecorder.android.fileprovider",
				new File(uri.getPath()));
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean isStorageWritable(Context context) {
		return getStorageFile(context).canWrite();
	}

	public static boolean isStorageReadable(Context context) {
		return getStorageFile(context).canRead();
	}
}
