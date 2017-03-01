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

import android.support.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Recording implements Comparable<Recording> {
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
	private Date date = new Date();
	private final String fileName;
	private String userName = "";
	private final String phoneNumber;

	public Recording(String fileName) {
		this.fileName = fileName;

		String dateStr = fileName.substring(0, 14);
		try {
			this.date = formatter.parse(dateStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		phoneNumber = fileName.substring(15, fileName.indexOf('.'));
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getFileName() {
		return fileName;
	}
	public String getPhoneNumber() { return phoneNumber; }
	public Date getDate() { return date; }

	public int compareTo(@NonNull Recording other) {
		return date.compareTo(other.date);
	}

	@Override
	public String toString() {
		return "Recording{" +
				", userName='" + userName + '\'' +
				'}';
	}
}
