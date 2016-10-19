package com.esafirm.rxdownloadmanager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.esafirm.rxdownloader.RxDownloader;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by esa on 11/11/15, with awesomeness
 */
public class SampleAct extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        checkPermission();
    }

    private void startSample() {
        findViewById(R.id.btn_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplication(), "Look at the notification!", Toast.LENGTH_SHORT).show();

                RxDownloader.getInstance(SampleAct.this)
                        .download("https://upload.wikimedia.org/wikipedia/en/e/ed/Nyan_cat_250px_frame.PNG",
                                "nyancat photo", "image/jpg")
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<String>() {
                            @Override
                            public void onCompleted() {
                                Log.d("Sample", "Is in main thread? " + (Looper.getMainLooper() == Looper.myLooper()));
                            }

                            @Override
                            public void onError(Throwable e) {
                                Toast.makeText(getApplication(), "Error!", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onNext(String s) {
                                Toast.makeText(getApplication(), "Downloaded to " + s, Toast.LENGTH_SHORT).show();

                                ImageView imageView = new ImageView(SampleAct.this);
                                imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT));
                                ((ViewGroup) findViewById(R.id.container)).addView(imageView);

                                Glide.with(SampleAct.this)
                                        .load(s)
                                        .into(imageView);
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
