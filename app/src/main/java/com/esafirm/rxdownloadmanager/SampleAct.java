package com.esafirm.rxdownloadmanager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.esafirm.rxdownloader.RxDownloader;

import java.util.Locale;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by esa on 11/11/15, with awesomeness
 */
public class SampleAct extends FragmentActivity {

    private TextView tv_progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        checkPermission();
    }

    private void startSample() {
        tv_progress = findViewById(R.id.progress);
        findViewById(R.id.btn_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RxDownloader rxDownloader = new RxDownloader(SampleAct.this);
                rxDownloader.downloadWithProgress(
                        "http://ipv4.download.thinkbroadband.com/20MB.zip",
                        "20MB.zip",
                        "application/zip",
                        true)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<Pair<Integer,String>>() {

                            @Override
                            public void onSubscribe(Disposable d) {
                                // do nothing
                            }

                            @Override
                            public void onNext(Pair<Integer, String> pair) {
                                tv_progress.setText(String.format(Locale.US, "%d%%", pair.first));
                                if (pair.second != null) {
                                    Toast.makeText(getApplication(), "Downloaded to " + pair.second, Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                Toast.makeText(getApplication(), "Error!", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onComplete() {
                                // do nothing
                            }
                        });
            }
        });
    }

    private void checkPermission() {
        final String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int permissionCheck = ContextCompat.checkSelfPermission(this, permission);

        if (permissionCheck != PermissionChecker.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
            return;
        }
        startSample();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != 0) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startSample();
        }
    }
}
