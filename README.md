## RxDownloadManager
An Rx wrapper for Download Manager in Android

### Usage
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

You can also look at the `sample` for complete usage

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
	  compile 'com.github.esafirm:RxDownloader:1.0.0'
}
```


