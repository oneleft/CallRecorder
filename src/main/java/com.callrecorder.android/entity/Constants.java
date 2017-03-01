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
package com.callrecorder.android.entity;

public class Constants {
	public static final String TAG = "CallRecorder";
	public static final String DefaultDir = "callrecords";

	public static final String FILE_NAME_PATTERN = "^[\\d]{14}_[_\\d]*\\..+$";

	public static final int STATE_INCOMING_NUMBER = 1;
	public static final int STATE_CALL_START = 2;
	public static final int STATE_CALL_END = 3;
	public static final int RECORDING_ENABLED = 4;
	public static final int RECORDING_DISABLED = 5;

	public static final String DefaultNumber = "1111"; // must be a number

	/* Update */
	public static final String UPDATE_CONFIG_URL = "https://raw.githubusercontent.com/jokinkuang/CallRecorder/master/update.json";
	public static final String UPDATE_CHECK_SUCCESS = "update_check_success";
	public static final String UPDATE_CHECK_FAILED = "update_check_failed";
	public static final String JSON_SYNTAX_ERROR = "json_syntax_exception";


}
