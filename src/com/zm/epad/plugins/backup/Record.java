package com.zm.epad.plugins.backup;

abstract class Record {
    protected final RecordSet mRecordSet;
    String mDisplayName;
    long mSize;
    int mOrder = 1000;

    Record(RecordSet recordSet) {
        mRecordSet = recordSet;
    }

    abstract void backup();

    abstract void restore();

    abstract String path();

    static final int TYPE_SYSTEM = 1;
    static final int TYPE_INSTALLED = 2;
    static final int TYPE_FILE = 3;
    abstract int type();
}
