package com.esafirm.rxdownloadmanager;

import android.app.Activity;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.esafirm.rxdownloader.RxDownloader;

import rx.Subscriber;

/**
 * Created by esa on 11/11/15, with awesomeness
 */
public class SampleAct extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Button button = new Button(this);
        button.setLayoutParams(new FrameLayout.LayoutParams(200, 200));
        button.setText("Download");
        setContentView(button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplication(), "Look at the notification!", Toast.LENGTH_SHORT).show();

                RxDownloader.getInstance(SampleAct.this)
                        .download("https://upload.wikimedia.org/wikipedia/en/e/ed/Nyan_cat_250px_frame.PNG",
                                "nyancat photo", "image/jpg")
                        .subscribe(new Subscriber<String>() {
                            @Override
                            public void onCompleted() {
                                Log.d("Sample", "Is in main thread? " + (Looper.getMainLooper() == Looper.myLooper()));
                            }

                            @Override
                            public void onError(Throwable e) {
                            }

                            @Override
                            public void onNext(String s) {
                                Toast.makeText(getApplication(), "Downloaded to " + s, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }
}
