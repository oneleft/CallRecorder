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
package com.callrecorder.android.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.widget.Toast;

import com.callrecorder.android.R;
import com.callrecorder.android.toast.ToastCompat;
import com.callrecorder.android.entity.Constants;
import com.callrecorder.android.util.FileHelper;
import com.callrecorder.android.util.UserPreferences;

public class RecordService extends Service {
	private MediaRecorder recorder;
	private String phoneNumber;

	private DocumentFile file;
	private ParcelFileDescriptor fd;

	private static class RecordConfig {
		public int audioSource = MediaRecorder.AudioSource.DEFAULT;
		public int outputFormat = MediaRecorder.OutputFormat.DEFAULT;
		public int audioEncoder = MediaRecorder.AudioEncoder.DEFAULT;
		public RecordConfig(){}
		public RecordConfig(int audioSource, int outputFormat, int audioEncoder) {
			this.audioSource = audioSource;
			this.outputFormat = outputFormat;
			this.audioEncoder = audioEncoder;
		}
		@Override
		public String toString() {
			return "RecordConfig{" +
					"audioSource=" + audioSource +
					", outputFormat=" + outputFormat +
					", audioEncoder=" + audioEncoder +
					'}';
		}
	}
	private RecordConfig[] configs = {
			new RecordConfig(MediaRecorder.AudioSource.VOICE_CALL, MediaRecorder.OutputFormat.DEFAULT, MediaRecorder.AudioEncoder.DEFAULT),
			new RecordConfig(MediaRecorder.AudioSource.MIC, MediaRecorder.OutputFormat.DEFAULT, MediaRecorder.AudioEncoder.DEFAULT),
			new RecordConfig(MediaRecorder.AudioSource.DEFAULT, MediaRecorder.OutputFormat.DEFAULT, MediaRecorder.AudioEncoder.DEFAULT),
			new RecordConfig(MediaRecorder.AudioSource.VOICE_UPLINK, MediaRecorder.OutputFormat.DEFAULT, MediaRecorder.AudioEncoder.DEFAULT), // 只录对方
			// 为什么把DEFAULT放在下面，因为测试时，锤子手机使用VOICE_CALL正常，使用DEFAULT只能录自己；而使用小米MAX时，MIC和DEFAULT正常，而VOICE_CALL会异常。
			// 上面的流程正好可以兼容。
			// VOICE_UPLINK没测试。

			new RecordConfig(MediaRecorder.AudioSource.VOICE_CALL, MediaRecorder.OutputFormat.THREE_GPP, MediaRecorder.AudioEncoder.DEFAULT),
			new RecordConfig(MediaRecorder.AudioSource.VOICE_CALL, MediaRecorder.OutputFormat.DEFAULT, MediaRecorder.AudioEncoder.AMR_NB),
			new RecordConfig(MediaRecorder.AudioSource.VOICE_CALL, MediaRecorder.OutputFormat.THREE_GPP, MediaRecorder.AudioEncoder.AMR_NB),

			new RecordConfig(MediaRecorder.AudioSource.MIC, MediaRecorder.OutputFormat.THREE_GPP, MediaRecorder.AudioEncoder.DEFAULT),
			new RecordConfig(MediaRecorder.AudioSource.MIC, MediaRecorder.OutputFormat.DEFAULT, MediaRecorder.AudioEncoder.AMR_NB),
			new RecordConfig(MediaRecorder.AudioSource.MIC, MediaRecorder.OutputFormat.THREE_GPP, MediaRecorder.AudioEncoder.AMR_NB)
	};
	private boolean onCall = false;
	private boolean recording = false;
	private boolean onForeground = false;

	@Override
	public IBinder onBind(Intent intent) { return null; }

	@Override
	public void onCreate() {
		UserPreferences.init(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(Constants.TAG, "RecordService onStartCommand");
		if (intent == null)
			return START_NOT_STICKY;

		int commandType = intent.getIntExtra("commandType", 0);
		if (commandType == 0)
			return START_NOT_STICKY;

		boolean enabled = UserPreferences.getEnabled();

		switch (commandType) {
		case Constants.RECORDING_ENABLED:
			Log.d(Constants.TAG, "RecordService RECORDING_ENABLED");
			if (enabled && onCall && !recording) {
				Log.d(Constants.TAG, "RecordService STATE_START_RECORDING");
				showNotification();
				startRecording();
			} else {
				Log.d(Constants.TAG, "Not Recording Enabled");
			}
			break;
		case Constants.RECORDING_DISABLED:
			Log.d(Constants.TAG, "RecordService RECORDING_DISABLED");
			if (onCall && phoneNumber != null && recording) {
				Log.d(Constants.TAG, "RecordService STATE_STOP_RECORDING");
				stopAndReleaseRecorder();
				recording = false;
			}
			break;
		case Constants.STATE_INCOMING_NUMBER:
			Log.d(Constants.TAG, "RecordService STATE_INCOMING_NUMBER");
			//showNotification();
			if (phoneNumber == null)
				phoneNumber = intent.getStringExtra("phoneNumber");
			break;
		case Constants.STATE_CALL_START:
			Log.d(Constants.TAG, "RecordService STATE_CALL_START");
			onCall = true;

			Log.d(Constants.TAG, "enabled: " + enabled + ", phoneNumber: " +phoneNumber+", recordingObject: "+recorder);
			if (enabled && !recording) {
				showNotification();
				startRecording();
			} else {
				Log.d(Constants.TAG, "Not Recording, Maybe is not enabled, Maybe is recording, Mabye phoneNumber is null");
			}
			break;
		case Constants.STATE_CALL_END:
			Log.d(Constants.TAG, "RecordService STATE_CALL_END");
			if (file != null && recording) {
				if (phoneNumber != null) {
					file.renameTo(file.getName().replace(Constants.DefaultNumber, phoneNumber));
					Log.d(Constants.TAG, "Rename the recording file to:"+file.getName());
				} else {
					ToastCompat.makeText(this, this.getString(R.string.can_not_get_phone_number), Toast.LENGTH_LONG).show();
				}
			}
			onCall = false;
			phoneNumber = null;
			stopAndReleaseRecorder();
			recording = false;
			stopService();
			break;
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		Log.d(Constants.TAG, "RecordService onDestroy");
		stopAndReleaseRecorder();
		stopService();
		super.onDestroy();
	}

	/// In case it is impossible to record
	private void terminateAndEraseFile() {
		Log.d(Constants.TAG, "RecordService terminateAndEraseFile");
		stopAndReleaseRecorder();
		recording = false;
		if (file != null)
			deleteFile();
	}

	private void stopService() {
		Log.d(Constants.TAG, "RecordService stopService");
		stopForeground(true);
		onForeground = false;
		this.stopSelf();
	}

	private void deleteFile() {
		Log.d(Constants.TAG, "RecordService deleteFile");
		file.delete();
		file = null;
	}

	private void stopAndReleaseRecorder() {
		if (recorder == null)
			return;

		Log.d(Constants.TAG, "RecordService stopAndReleaseRecorder");
		boolean recorderStopped = false;
		boolean exception = false;

		try {
			recorder.stop();
			recorderStopped = true;
		} catch (IllegalStateException e) {
			Log.e(Constants.TAG, "Failed to stop recorder.  Perhaps it wasn't started?", e);
			exception = true;
		}
		recorder.reset();
		recorder.release();
		recorder = null;
		if (exception) {
			deleteFile();
		}
		if (recorderStopped) {
			ToastCompat.makeText(this, this.getString(R.string.receiver_end_call), Toast.LENGTH_SHORT).show();
		}
	}

	private void startRecording() {
		Log.d(Constants.TAG, "RecordService startRecording");

		try {
			// Cause phone number maybe comes after recording, so rename file after recording.
			if (phoneNumber == null) {
				file = FileHelper.getFile(this, Constants.DefaultNumber);
			} else {
				file = FileHelper.getFile(this, phoneNumber);
			}
			Log.d(Constants.TAG, "Recording file:" + file.getName());

			fd = getContentResolver()
					.openFileDescriptor(file.getUri(), "w");
			if (fd == null)
				throw new Exception("Failed open recording file.");
		} catch (Exception e) {
			Log.e(Constants.TAG, "Failed to set up recorder.", e);
		}

		// loop to get the correct config
		boolean bl = false;
		for (RecordConfig config : configs) {
			Log.d(Constants.TAG, "try config: "+config.toString());
			if (bl = startRecorder(config)) {
				break;
			}
		}

		if (bl == false) {
			Log.e(Constants.TAG, "Failed to set up recorder.");
			ToastCompat.makeText(this, this.getString(R.string.record_impossible), Toast.LENGTH_LONG).show();
			terminateAndEraseFile();
			return;
		}

		recorder.setOnErrorListener((mr, what, extra) -> {
			Log.e(Constants.TAG, "OnErrorListener " + what + "," + extra);
			terminateAndEraseFile();
		});

		recorder.setOnInfoListener((mr, what, extra) -> {
			Log.e(Constants.TAG, "OnInfoListener " + what + "," + extra);
			terminateAndEraseFile();
		});

		recording = true;

		Log.d(Constants.TAG, "RecordService: Recorder started! recording...");

		// 小米手机Toast
		ToastCompat.makeText(this, this.getString(R.string.receiver_start_call), Toast.LENGTH_SHORT).show();
	}

	private boolean startRecorder(RecordConfig config) {
		try {
			recorder = new MediaRecorder();

			recorder.setAudioSource(config.audioSource);
			recorder.setOutputFormat(config.outputFormat);
			recorder.setAudioEncoder(config.audioEncoder);

			recorder.setOutputFile(fd.getFileDescriptor());

			// You have to setup output file before prepare!
			recorder.prepare();

			// Sometimes the recorder takes a while to start up
			Thread.sleep(500);
			recorder.start();

			Log.d(Constants.TAG, "CurrentConfig: audioSource="+config.audioSource+", outputFormat="+config.outputFormat+", audioEncoder="+config.audioEncoder);
			return true;
		} catch (Exception e) {
			Log.e(Constants.TAG, "Exception", e);
			return false;
		}
	}

	private void showNotification() {
		if (onForeground)
			return;

		/*
		Log.d(Constants.TAG, "RecordService showNotification");
		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		PendingIntent pendingIntent = PendingIntent.getActivity(
			getBaseContext(), 0, intent, 0);

		Notification notification = new NotificationCompat.Builder(
				getBaseContext())
			.setContentTitle(this.getString(R.string.notification_title))
			.setTicker(this.getString(R.string.notification_ticker))
			.setContentText(this.getString(R.string.notification_text))
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentIntent(pendingIntent)
			.setOngoing(true)
			.build();

		notification.flags = Notification.FLAG_NO_CLEAR;	// notification you can not clear

		// Always to be 1 if set to 0 it would no notification.
		startForeground(1, notification);

		*/

		onForeground = true;
	}
}
