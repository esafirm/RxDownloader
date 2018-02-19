package com.esafirm.rxdownloader;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;

import com.esafirm.rxdownloader.utils.LongSparseArray;

import org.reactivestreams.Publisher;

import java.io.File;
import java.util.concurrent.Callable;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.Function;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by esa on 10/11/15, with awesomeness
 */
public class RxDownloader {

    private static final String DEFAULT_MIME_TYPE = "*/*";
    private static final String TAG = RxDownloader.class.getSimpleName();

    private Context context;
    //    private LongSparseArray<PublishSubject<String>> subjectMap = new LongSparseArray<>();
    private LongSparseArray<PublishProcessor<Pair<Integer, String>>> progressSubjectMap = new LongSparseArray<>();
    private DownloadManager downloadManager;

    public RxDownloader(@NonNull Context context) {
        this.context = context.getApplicationContext();
//        DownloadStatusReceiver downloadStatusReceiver = new DownloadStatusReceiver();
//        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
//        context.registerReceiver(downloadStatusReceiver, intentFilter);
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

    public Flowable<String> download(@NonNull String url,
                                     @NonNull String filename,
                                     boolean showCompletedNotification) {
        return download(url, filename, DEFAULT_MIME_TYPE, showCompletedNotification);
    }

    public Flowable<String> download(@NonNull String url,
                                     @NonNull String filename,
                                     @NonNull String mimeType,
                                     boolean showCompletedNotification) {
        return download(createRequest(url, filename, null,
                mimeType, true, showCompletedNotification));
    }

    public Flowable<String> download(@NonNull String url,
                                     @NonNull String filename,
                                     @NonNull String destinationPath,
                                     @NonNull String mimeType,
                                     boolean showCompletedNotification) {
        return download(createRequest(url, filename, destinationPath,
                mimeType, true, showCompletedNotification));
    }

    public Flowable<String> downloadInFilesDir(@NonNull String url,
                                               @NonNull String filename,
                                               @NonNull String destinationPath,
                                               @NonNull String mimeType,
                                               boolean showCompletedNotification) {
        return download(createRequest(url, filename, destinationPath,
                mimeType, false, showCompletedNotification));
    }

    public Flowable<String> download(DownloadManager.Request request) {
        long downloadId = getDownloadManager().enqueue(request);

        PublishProcessor<Pair<Integer, String>> publishProcessor = PublishProcessor.create();
        progressSubjectMap.put(downloadId, publishProcessor);

        return publishProcessor.flatMap(new Function<Pair<Integer, String>, Publisher<String>>() {
            @Override
            public Publisher<String> apply(Pair<Integer, String> pair) {
                return Flowable.just(pair.second);
            }
        });
    }

    public Flowable<Pair<Integer, String>> downloadWithProgress(DownloadManager.Request request) {
        final long downloadId = getDownloadManager().enqueue(request);

        final PublishProcessor<Pair<Integer, String>> publishProcessor = PublishProcessor.create();
        progressSubjectMap.put(downloadId, publishProcessor);

//                        ;
        Flowable<Pair<Integer, String>> flowable =
                Flowable.defer(new Callable<Publisher<Pair<Integer, String>>>() {
                    @Override
                    public Publisher<Pair<Integer, String>> call() {
                        return
                                Flowable.create(new FlowableOnSubscribe<Pair<Integer, String>>() {
                                    @Override
                                    public void subscribe(FlowableEmitter<Pair<Integer, String>> e) throws Exception {
                                        Log.d(TAG, "subscribe: listening for progress");
                                        DownloadManager.Query query = new DownloadManager.Query();
                                        query.setFilterById(downloadId);
                                        while (true) {
                                            Cursor cursor = getDownloadManager().query(query);
                                            cursor.moveToFirst();
                                            int bytesDownloaded = cursor.getInt(cursor
                                                    .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                                            int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                                            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                                            int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                                            String downloadedPackageUriString = cursor.getString(uriIndex);
                                            cursor.close();
                                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                                e.onNext(Pair.create(100, downloadedPackageUriString));
                                                e.onComplete();
                                                break;
                                            } else if (status == DownloadManager.STATUS_FAILED) {
                                                e.onError(new RuntimeException("Download not complete"));
                                                break;
                                            }
//                                    cursor.close();
                                            final int progress = (int) ((bytesDownloaded * 1f) / bytesTotal * 100);
                                            Log.d(TAG, "progress: " + progress + ", status: " + status);
                                            Log.d(TAG, "downloaded: " + bytesDownloaded + "B, total: " + bytesTotal + "B status: " + status);
                                            e.onNext(Pair.<Integer, String>create(progress, null));
                                            Thread.sleep(500);
                                        }
                                    }
                                }, BackpressureStrategy.LATEST)
                                        /*.publish()
                                        .autoConnect()*/;
                    }
                });
//        flowable.subscribe();

//                .subscribeOn(Schedulers.io())
//                .publish()
//                .autoConnect();

        flowable/*.publish()
                .autoConnect()*/
                .subscribe(publishProcessor);
        return publishProcessor;
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

    public Flowable<Pair<Integer, String>> downloadWithProgress(@NonNull String url,
                                                                @NonNull String filename,
                                                                @NonNull String mimeType,
                                                                boolean showCompletedNotification) {
        return downloadWithProgress(createRequest(url, filename, null,
                mimeType, true, showCompletedNotification));
    }

//    private class DownloadStatusReceiver extends BroadcastReceiver {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
//            PublishProcessor<Pair<Integer, String>> publishSubject = progressSubjectMap.get(id);
//
//            if (publishSubject == null)
//                return;
//
//            DownloadManager.Query query = new DownloadManager.Query();
//            query.setFilterById(id);
//            DownloadManager downloadManager = getDownloadManager();
//            Cursor cursor = downloadManager.query(query);
//
//            if (!cursor.moveToFirst()) {
//                cursor.close();
//                downloadManager.remove(id);
//                publishSubject.onError(new IllegalStateException("Cursor empty, this shouldn't happened"));
//                progressSubjectMap.remove(id);
//                return;
//            }
//
//            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
//            if (DownloadManager.STATUS_SUCCESSFUL != cursor.getInt(statusIndex)) {
//                cursor.close();
//                downloadManager.remove(id);
//                publishSubject.onError(new IllegalStateException("Download Failed"));
//                progressSubjectMap.remove(id);
//                return;
//            }
//
//            int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
//            String downloadedPackageUriString = cursor.getString(uriIndex);
//            cursor.close();
//
//            publishSubject.onNext(downloadedPackageUriString);
//            publishSubject.onComplete();
//            subjectMap.remove(id);
//        }
//    }
}
