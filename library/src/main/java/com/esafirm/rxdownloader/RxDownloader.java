package com.esafirm.rxdownloader;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import com.esafirm.rxdownloader.utils.LongSparseArray;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by esa on 10/11/15, with awesomeness
 */
public class RxDownloader {

    private static final String DEFAULT_MIME_TYPE = "*/*";

    private Context context;
    private LongSparseArray<PublishSubject<String>> subjectMap = new LongSparseArray<>();

    public RxDownloader(@NonNull Context context) {
        this.context = context.getApplicationContext();
        DownloadStatusReceiver downloadStatusReceiver = new DownloadStatusReceiver();
        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        context.registerReceiver(downloadStatusReceiver, intentFilter);
    }

    public Observable<String> downloadExternalPublicDir(@NonNull String url, @NonNull String filename) {
        return downloadExternalPublicDir(url, filename, DEFAULT_MIME_TYPE);
    }

    public Observable<String> downloadExternalPublicDir(@NonNull String url,
                                                        @NonNull String filename,
                                                        @NonNull String mimeType) {
        return download(getDefaultRequest(url, filename, null, mimeType, true));
    }

    public Observable<String> downloadExternalPublicDir(@NonNull String url,
                                                        @NonNull String filename,
                                                        @NonNull String destinationPath,
                                                        @NonNull String mimeType) {
        return download(getDefaultRequest(url, filename, destinationPath, mimeType, true));
    }

    public Observable<String> downloadExternalFilesDir(@NonNull String url,
                                       @NonNull String filename,
                                       @NonNull String destinationPath,
                                       @NonNull String mimeType) {
        return download(getDefaultRequest(url, filename, destinationPath, mimeType, false));
    }

    @Nullable
    private DownloadManager getDownloadManager() {
        return (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public Observable<String> download(DownloadManager.Request request) {
        long downloadId = getDownloadManager().enqueue(request);

        PublishSubject<String> publishSubject = PublishSubject.create();
        subjectMap.put(downloadId, publishSubject);

        return publishSubject;
    }

    private DownloadManager.Request getDefaultRequest(@NonNull String url,
                                                      @NonNull String filename,
                                                      @Nullable String destinationPath,
                                                      @NonNull String mimeType,
                                                      boolean inPublicDir) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(filename);
        request.setMimeType(mimeType);
        destinationPath = destinationPath == null ?
                Environment.DIRECTORY_DOWNLOADS : destinationPath;
        if (inPublicDir) {
            request.setDestinationInExternalPublicDir(destinationPath, filename);
        } else {
            request.setDestinationInExternalFilesDir(context, destinationPath, filename);
        }
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        return request;
    }

    private class DownloadStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
            PublishSubject<String> publishSubject = subjectMap.get(id);

            if (publishSubject == null)
                return;

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(id);
            DownloadManager downloadManager = getDownloadManager();
            Cursor cursor = downloadManager.query(query);

            if (!cursor.moveToFirst()) {
                cursor.close();
                downloadManager.remove(id);
                publishSubject.onError(
                        new IllegalStateException("Cursor empty, this shouldn't happened"));
                subjectMap.remove(id);
                return;
            }

            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            if (DownloadManager.STATUS_SUCCESSFUL != cursor.getInt(statusIndex)) {
                cursor.close();
                downloadManager.remove(id);
                publishSubject.onError(new IllegalStateException("Download Failed"));
                subjectMap.remove(id);
                return;
            }

            int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
            String downloadedPackageUriString = cursor.getString(uriIndex);
            cursor.close();

            publishSubject.onNext(downloadedPackageUriString);
            publishSubject.onComplete();
            subjectMap.remove(id);
        }
    }
}
