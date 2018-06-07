package com.esafirm.rxdownloader;

/**
 * Represents download state
 */
public final class DownloadState {
    public final int progress;
    public final String path;

    /**
     * @param progress progress as a percentage (0-100)
     * @param path path of the downloaded file. May be null if not completed.
     */
    /*package*/ DownloadState(int progress, String path) {
        this.progress = progress;
        this.path = path;
    }

}
