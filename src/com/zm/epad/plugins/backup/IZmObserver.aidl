package com.zm.epad.plugins.backup;

oneway interface IZmObserver {
    void onStart(String path);
    void onRecordStart(String name);
    void onRecordProgress(String name);
    void onRecordEnd(String name);
    void onRecordTimeout(String name);
    void onEnd(String path, int system, int installed, int file);
}
