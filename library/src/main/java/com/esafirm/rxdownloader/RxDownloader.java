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

import java.io.File;

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
    private DownloadManager downloadManager;

    public RxDownloader(@NonNull Context context) {
        this.context = context.getApplicationContext();
        DownloadStatusReceiver downloadStatusReceiver = new DownloadStatusReceiver();
        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        context.registerReceiver(downloadStatusReceiver, intentFilter);
    }

    @NonNull
    private DownloadManager getDownloadManager() {
        if (downloadManager == null) {
            downloadManager = (DownloadManager) context.getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
        }
        if (downloadManager == null) {
            throw new RuntimeException("Can't get DownloadManager from system service");
        }
        return downloadManager;
    }

    public Observable<String> download(@NonNull String url,
                                       @NonNull String filename,
                                       boolean showCompletedNotification) {
        return download(url, filename, DEFAULT_MIME_TYPE, showCompletedNotification);
    }

    public Observable<String> download(@NonNull String url,
                                       @NonNull String filename,
                                       @NonNull String mimeType,
                                       boolean showCompletedNotification) {
        return download(createRequest(url, filename, null,
                mimeType, true, showCompletedNotification));
    }

    public Observable<String> download(@NonNull String url,
                                       @NonNull String filename,
                                       @NonNull String destinationPath,
                                       @NonNull String mimeType,
                                       boolean showCompletedNotification) {
        return download(createRequest(url, filename, destinationPath,
                mimeType, true, showCompletedNotification));
    }

    public Observable<String> downloadInFilesDir(@NonNull String url,
                                                 @NonNull String filename,
                                                 @NonNull String destinationPath,
                                                 @NonNull String mimeType,
                                                 boolean showCompletedNotification) {
        return download(createRequest(url, filename, destinationPath,
                mimeType, false, showCompletedNotification));
    }

    public Observable<String> download(DownloadManager.Request request) {
        long downloadId = getDownloadManager().enqueue(request);

        PublishSubject<String> publishSubject = PublishSubject.create();
        subjectMap.put(downloadId, publishSubject);

        return publishSubject;
    }

    private DownloadManager.Request createRequest(@NonNull String url,
                                                  @NonNull String filename,
                                                  @Nullable String destinationPath,
                                                  @NonNull String mimeType,
                                                  boolean inPublicDir,
                                                  boolean showCompletedNotification) {

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(filename);
        request.setMimeType(mimeType);

        if (destinationPath == null) {
            destinationPath = Environment.DIRECTORY_DOWNLOADS;
        }

        File destinationFolder = inPublicDir
                ? Environment.getExternalStoragePublicDirectory(destinationPath)
                : new File(context.getFilesDir(), destinationPath);

        createFolderIfNeeded(destinationFolder);
        removeDuplicateFileIfExist(destinationFolder, filename);

        if (inPublicDir) {
            request.setDestinationInExternalPublicDir(destinationPath, filename);
        } else {
            request.setDestinationInExternalFilesDir(context, destinationPath, filename);
        }

        request.setNotificationVisibility(showCompletedNotification
                ? DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                : DownloadManager.Request.VISIBILITY_VISIBLE);

        return request;
    }

    private void createFolderIfNeeded(@NonNull File folder) {
        if (!folder.exists() && !folder.mkdirs()) {
            throw new RuntimeException("Can't create directory");
        }
    }

    private void removeDuplicateFileIfExist(@NonNull File folder, @NonNull String fileName) {
        File file = new File(folder, fileName);
        if (file.exists() && !file.delete()) {
            throw new RuntimeException("Can't delete file");
        }
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
                publishSubject.onError(new IllegalStateException("Cursor empty, this shouldn't happened"));
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
