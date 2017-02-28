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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.provider.DocumentFile;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.List;

class RecordingsAdapter extends ArrayAdapter<Recording> {

	private final Context context;
	private final List<Recording> list;

	public RecordingsAdapter(Context context, List<Recording> list) {
		super(context, R.layout.record_row, list);
		this.list = list;
		this.context = context;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.record_row, parent, false);
		}
		final TextView titleView = (TextView) rowView.findViewById(
			R.id.recording_title);
		final TextView dateView = (TextView) rowView.findViewById(
			R.id.recording_date);
		final TextView numberView = (TextView) rowView.findViewById(
			R.id.recording_number);
		final Recording entry = list.get(position);
		FileHelper.logD(Constants.TAG, "pos:"+position+"titleView:"+titleView.hashCode()+",text:"+titleView.getText()+"|entry:"+entry);

		String phoneNumber = entry.getPhoneNumber();
		if (phoneNumber.matches("^[\\d]+$")) {
			//noinspection deprecation
			phoneNumber = PhoneNumberUtils.formatNumber(phoneNumber);
		} else {
			phoneNumber = context.getString(R.string.withheld_number);
		}

		dateView.setText(DateFormat.getDateTimeInstance().format(entry.getDate()));
		titleView.setText("");	// @NOTE titleView was reused by the scrollview when scroll to next page !!
		numberView.setText(phoneNumber);

		// query contact
		if (entry.getUserName() != null && entry.getUserName() != "") {
			titleView.setText(entry.getUserName());
		} else if (entry.getUserName() != null && entry.getUserName() == "") {
			new AsyncTask<Void, Void, String>() {
				@Override
				protected String doInBackground(Void... params) {
					return FileHelper.getContactName(entry.getPhoneNumber(), context);
				}

				@Override
				protected void onPostExecute(String str) {
					FileHelper.logD(Constants.TAG, "got user name:"+str+"|titleview:"+titleView.hashCode()+"|entry:"+entry);
					entry.setUserName(str);
					titleView.setText(str);
				}
			}.execute();
		}

		return rowView;
	}

	/// Shows a contextual dialog with actions for the selected recording
	public void showContextMenu(final String fileName, final int position) {
		final CharSequence[] items = {
			context.getString(R.string.confirm_play),
			context.getString(R.string.confirm_send),
			context.getString(R.string.options_delete)
		};

		new AlertDialog.Builder(context)
			.setTitle(R.string.options_title)
			.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					if (item == 0) {
						startPlayExternal(fileName);
					} else if (item == 1) {
						sendMail(fileName);
					} else if (item == 2) {
						deleteRecord(fileName, position);
					}
				}
			})
			.show();
	}

	private void deleteRecord(final String fileName, final int position) {
		new AlertDialog.Builder(context)
			.setTitle(R.string.confirm_delete_title)
			.setMessage(R.string.confirm_delete_text)
			.setPositiveButton(R.string.yes,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						DocumentFile file = FileHelper.getStorageFile(context)
							.findFile(fileName);

						if (file.exists() && file.delete()) {
							list.remove(position);
							notifyDataSetChanged();
						} else {
							Toast toast = Toast.makeText(getContext(),
								getContext().getString(R.string.delete_failed),
								Toast.LENGTH_SHORT);
							toast.show();
						}
					}
				})
			.setNegativeButton(R.string.no,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				})
			.show();
	}

	private void sendMail(String fileName) {
		DocumentFile file = FileHelper.getStorageFile(context)
			.findFile(fileName);
		Uri uri = FileHelper.getContentUri(context, file.getUri());

		// get the real path or the 3rd app would not find the file
		uri = Uri.parse("file://"+ SAFHelper.getPath(context, file.getUri()));

		Intent sendIntent = new Intent(Intent.ACTION_SEND)
			.putExtra(Intent.EXTRA_SUBJECT,
				context.getString(R.string.mail_subject))
			.putExtra(Intent.EXTRA_TEXT,
				context.getString(R.string.mail_body))
			.putExtra(Intent.EXTRA_STREAM, uri)
			.setData(uri)
			.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			.setType("audio/3gpp");

		context.startActivity(Intent.createChooser(sendIntent,
				context.getString(R.string.send_mail)));
	}

	private void startPlayExternal(String fileName) {
		Log.d("file", fileName);
		DocumentFile file = FileHelper.getStorageFile(context)
			.findFile(fileName);
		Log.d("file", file.getName()+"||"+file.getUri()+"||"+file.getParentFile().getName()+"||"+file.exists());
		//Uri uri = FileHelper.getContentUri(context, file.getUri());

		// get the real path or the 3rd app would not find the file
		Uri uri = Uri.parse("file://"+ SAFHelper.getPath(context, file.getUri()));

		context.startActivity(new Intent()
			.setAction(Intent.ACTION_VIEW)
			.setDataAndType(uri, "audio/3gpp")
			.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			);
	}
}
