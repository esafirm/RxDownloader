package com.incendiary.rxdownloader;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.util.LongSparseArray;

import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * Created by esa on 10/11/15, with awesomeness
 */
public class RxDownloader {

	private static RxDownloader mRxDownloader;

	private Context mContext;
	private DownloadManager mDownloadManager;

	private LongSparseArray<PublishSubject<String>> mSubjectMap = new LongSparseArray<>();

	public static RxDownloader getInstance(Context context) {
		if (mRxDownloader == null)
			mRxDownloader = new RxDownloader(context.getApplicationContext());
		return mRxDownloader;
	}

	private RxDownloader(Context context) {
		mContext = context;
		DownloadStatusReceiver downloadStatusReceiver = new DownloadStatusReceiver();
		IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		context.registerReceiver(downloadStatusReceiver, intentFilter);
	}

	public Observable<String> download(String link, String filename) {
		return download(link, filename, "*/*");
	}

	public Observable<String> download(String link, String filename, String mimeType) {
		return download(getDefaultRequest(link, filename, mimeType));
	}

	public Observable<String> download(DownloadManager.Request request) {
		if (mDownloadManager == null)
			mDownloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);

		long downloadId = mDownloadManager.enqueue(request);

		PublishSubject<String> publishSubject = PublishSubject.create();
		mSubjectMap.put(downloadId, publishSubject);

		return publishSubject;
	}

	private DownloadManager.Request getDefaultRequest(String link, String filename, String mimeType) {
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(link));
		request.setDescription("Downloading file...");
		request.setMimeType(mimeType);
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
		return request;
	}

	private class DownloadStatusReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
			PublishSubject<String> publishSubject = mSubjectMap.get(id);

			if (publishSubject == null)
				return;

			DownloadManager.Query query = new DownloadManager.Query();
			query.setFilterById(id);
			Cursor cursor = mDownloadManager.query(query);

			if (!cursor.moveToFirst()) {
				mDownloadManager.remove(id);
				publishSubject.onError(new IllegalStateException("Cursor empty, this shouldn't happened"));
				mSubjectMap.remove(id);
				return;
			}

			int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
			if (DownloadManager.STATUS_SUCCESSFUL != cursor.getInt(statusIndex)) {
				mDownloadManager.remove(id);
				publishSubject.onError(new IllegalStateException("Download Failed"));
				mSubjectMap.remove(id);
				return;
			}

			int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
			String downloadedPackageUriString = cursor.getString(uriIndex);

			publishSubject.onNext(downloadedPackageUriString);
			publishSubject.onCompleted();
			mSubjectMap.remove(id);
		}
	}
}