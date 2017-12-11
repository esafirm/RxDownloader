## RxDownloader

An Rx wrapper for Download Manager in Android

![](https://raw.githubusercontent.com/esafirm/RxDownloader/master/art/sample.gif)

## Usage

To download a file into your download directory (`Environment.DOWNLOADS`) use `download()`

```java
String url = "https://upload.wikimedia.org/wikipedia/en/e/ed/Nyan_cat_250px_frame.PNG";
RxDownloader.getInstance(appContext)
            .download(url, "nyancat photo", "image/jpg") // url, filename, and mimeType
            .subscribe(path ->{
                // Do what you want with downloaded path
             }, throwable -> {
                // Handle download faile here
             });
```

To download file into the directory on the filesystem (`Context.getFilesDir()`) use `downloadInFilesDir()`

```java
String url = "https://upload.wikimedia.org/wikipedia/en/e/ed/Nyan_cat_250px_frame.PNG";
RxDownloader.getInstance(appContext)
            .downloadInFilesDir(url, "nyancat photo", "image/jpg") // url, filename, and mimeType
            .subscribe(path ->{ /* file path */  }, throwable -> { /* handle error */ });
```

You can always use `download(DowloadManager.Request request)` for more customization and configuration. 


You can also look at the `sample` for complete usage

## Gradle

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
    compile 'com.github.esafirm:rxdownloader:2.0.0'
}
```

## Thanks

- [Vladislav Nikolaev](https://github.com/VladislavNikolaev) who migrate this library to RxJava2 
- Everyone who has contributed code and reported issues!

## License

MIT @ Esa Firman


