package com.zm.epad.plugins.backup;

oneway interface IZmObserver {
    void onStart(String path);
    void onRecordStart(String name);
    void onRecordProgress(String name, int index);
    void onRecordEnd(String name);
    void onRecordTimeout(String name);
    void onEnd(String path, in String[] key, in int[] stats, boolean special);
}
