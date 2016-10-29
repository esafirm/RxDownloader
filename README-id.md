## RxDownloadManager
Sebuah Pembungkus Rx untuk Download Manager di Android

![](https://raw.githubusercontent.com/esafirm/RxDownloader/master/art/sample.gif)

### Penggunaan
```java
String url = "https://upload.wikimedia.org/wikipedia/en/e/ed/Nyan_cat_250px_frame.PNG";
RxDownloader.getInstance(SampleAct.this)
            .download(url, "nyancat photo", "image/jpg") // url, filename, and mimeType
            .subscribe(path ->{
                // Do what you want with downloaded path
             }, throwable -> {
                // Handle download faile here
             });
```

Anda juga dapat melihat `sample` untuk penggunaan lengkap

### Gradle

```groovy
allprojects {
		repositories {
			...
			maven { url "https://jitpack.io" }
		}
}
```

```groovy
dependencies {
	  compile 'com.github.esafirm:RxDownloader:1.0.1'
}
```
