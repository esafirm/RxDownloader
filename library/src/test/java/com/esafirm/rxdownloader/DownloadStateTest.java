package com.esafirm.rxdownloader;

import org.junit.Test;

import static org.junit.Assert.*;

public class DownloadStateTest {
    @Test
    public void noPathGiven_downloadIncomplete() {
        DownloadState state = new DownloadState(0, null);
        assertFalse(state.isComplete());
    }

    @Test
    public void emptyPathGiven_downloadIncomplete() {
        DownloadState state = new DownloadState(0, "");
        assertFalse(state.isComplete());
    }

    @Test
    public void withPath_downloadIncomplete() {
        DownloadState state = new DownloadState(0, "/media/sdcard/example.jpg");
        assertTrue(state.isComplete());
    }
}