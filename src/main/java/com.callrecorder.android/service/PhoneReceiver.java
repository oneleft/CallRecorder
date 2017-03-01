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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.callrecorder.android.entity.Constants;
import com.callrecorder.android.util.FileHelper;
import com.callrecorder.android.util.UserPreferences;

public class PhoneReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (!action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED) &&
				!action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
			Log.e(Constants.TAG, "PhoneReceiver: Received unexpected intent: " + action);
			return;
		}

		String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
		String extraState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
		Log.d(Constants.TAG, "PhoneReceiver phone number: " + phoneNumber + ", extraState: " + extraState);

//		电话状态对应：
//		RINGING ----对应来电时响起铃声
//		OFFHOOK ----如果是来电，则是来电接通到挂断之前，如果是拨出，则是点击拨打电话那一刻(还未接通)，直到挂断
//		IDLE    ----对应挂断电话，即没有任何的电话事件了
//
//		电话号码：
//		仅且一次，Intent.EXTRA_PHONE_NUMBER的非null值。
//
//		特别说明：如果你在打电话过程中(状态为 OFFHOOK)，此时如果有人再给你打电话，就会想起铃声，这时候的状态会变化为 RINGING，如果你挂断该来电，继续之前的电话，状态又变为 OFFHOOK

		UserPreferences.init(context);
		if (!FileHelper.isStorageWritable(context)) {
			Log.e(Constants.TAG, "Storage is not writable !");
			return;
		}

		if (extraState != null) {
			Log.d(Constants.TAG, "DispatchState: " + extraState);
			dispatchExtra(context, intent, phoneNumber, extraState);
		} else if (phoneNumber != null) {
			context.startService(new Intent(context, RecordService.class)
				.putExtra("commandType", Constants.STATE_INCOMING_NUMBER)
				.putExtra("phoneNumber", phoneNumber));
		}
	}

	private void dispatchExtra(Context context, Intent intent,
			String phoneNumber, String extraState) {
		if (extraState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
			context.startService(new Intent(context, RecordService.class)
				.putExtra("commandType", Constants.STATE_CALL_START));
		} else if (extraState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
			context.startService(new Intent(context, RecordService.class)
				.putExtra("commandType", Constants.STATE_CALL_END));
		} else if (extraState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
			if (phoneNumber == null)
				phoneNumber = intent.getStringExtra(
					TelephonyManager.EXTRA_INCOMING_NUMBER);

			context.startService(new Intent(context, RecordService.class)
				.putExtra("commandType", Constants.STATE_INCOMING_NUMBER)
				.putExtra("phoneNumber", phoneNumber));
		}
	}
}
