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

class Constants {
	public static final String TAG = "CallRecorder";
	public static final String DefaultDir = "callrecords";

	public static final String FILE_NAME_PATTERN = "^[\\d]{14}_[_\\d]*\\..+$";

	public static final int STATE_INCOMING_NUMBER = 1;
	public static final int STATE_CALL_START = 2;
	public static final int STATE_CALL_END = 3;
	public static final int RECORDING_ENABLED = 4;
	public static final int RECORDING_DISABLED = 5;
}
