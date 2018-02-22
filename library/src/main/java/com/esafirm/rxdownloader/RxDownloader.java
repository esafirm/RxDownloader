package com.esafirm.rxdownloader;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import com.esafirm.rxdownloader.utils.LongSparseArray;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by esa on 10/11/15, with awesomeness
 */
public class RxDownloader {

    private static final String DEFAULT_MIME_TYPE = "*/*";
    private static final int PROGRESS_INTERVAL_MILLIS = 500;

    private Context context;
    private LongSparseArray<PublishSubject<DownloadState>> progressSubjectMap = new LongSparseArray<>();
    private DownloadManager downloadManager;

    public RxDownloader(@NonNull Context context) {
        this.context = context.getApplicationContext();
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

    public Single<String> download(@NonNull String url,
                                             @NonNull String filename,
                                             boolean showCompletedNotification) {
        return download(url, filename, DEFAULT_MIME_TYPE, showCompletedNotification);
    }

    public Single<String> download(@NonNull String url,
                                             @NonNull String filename,
                                             @NonNull String mimeType,
                                             boolean showCompletedNotification) {
        return download(createRequest(url, filename, null,
                mimeType, true, showCompletedNotification));
    }

    public Single<String> download(@NonNull String url,
                                             @NonNull String filename,
                                             @NonNull String destinationPath,
                                             @NonNull String mimeType,
                                             boolean showCompletedNotification) {
        return download(createRequest(url, filename, destinationPath,
                mimeType, true, showCompletedNotification));
    }

    public Single<String> downloadInFilesDir(@NonNull String url,
                                                       @NonNull String filename,
                                                       @NonNull String destinationPath,
                                                       @NonNull String mimeType,
                                                       boolean showCompletedNotification) {
        return download(createRequest(url, filename, destinationPath,
                mimeType, false, showCompletedNotification));
    }

    public Single<String> download(DownloadManager.Request request) {
        long downloadId = getDownloadManager().enqueue(request);

        PublishSubject<DownloadState> publishSubject = PublishSubject.create();
        progressSubjectMap.put(downloadId, publishSubject);

        return publishSubject
                .filter(new Predicate<DownloadState>() {
                    @Override
                    public boolean test(DownloadState s) {
                        return s.path != null;
                    }
                })
                .map(new Function<DownloadState, String>() {
                    @Override
                    public String apply(DownloadState progressState) {
                        return progressState.path;
                    }
                })
                .firstOrError();
    }

    public Observable<DownloadState> downloadWithProgress(DownloadManager.Request request) {
        final long downloadId = getDownloadManager().enqueue(request);

        final PublishSubject<DownloadState> publishSubject = PublishSubject.create();
        progressSubjectMap.put(downloadId, publishSubject);

        Observable<DownloadState> observable =
                Observable.create(new ObservableOnSubscribe<DownloadState>() {
                    @Override
                    public void subscribe(ObservableEmitter<DownloadState> e) throws Exception {
                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(downloadId);
                        while (true) {
                            Cursor cursor = getDownloadManager().query(query);
                            if (!cursor.moveToFirst()) {
                                cursor.close();
                                downloadManager.remove(downloadId);
                                publishSubject.onError(new IllegalStateException("Cursor empty, this shouldn't happen"));
                                progressSubjectMap.remove(downloadId);
                                return;
                            }
                            int bytesDownloaded = cursor.getInt(cursor
                                    .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                            int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                            String downloadedPackageUriString = cursor.getString(uriIndex);
                            cursor.close();
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                e.onNext(DownloadState.create(100, downloadedPackageUriString));
                                e.onComplete();
                                progressSubjectMap.remove(downloadId);
                                break;
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                e.onError(new RuntimeException("Download not complete"));
                                progressSubjectMap.remove(downloadId);
                                break;
                            }
                            final int progress = (int) ((bytesDownloaded * 1f) / bytesTotal * 100);
                            e.onNext(DownloadState.create(progress, null));
                            Thread.sleep(PROGRESS_INTERVAL_MILLIS);
                        }
                    }
                });

        observable.subscribeOn(Schedulers.newThread())
                .subscribe(publishSubject);
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

    public Observable<DownloadState> downloadWithProgress(@NonNull String url,
                                                          @NonNull String filename,
                                                          @NonNull String mimeType,
                                                          boolean showCompletedNotification) {
        return downloadWithProgress(createRequest(url, filename, null,
                mimeType, true, showCompletedNotification));
    }

}
