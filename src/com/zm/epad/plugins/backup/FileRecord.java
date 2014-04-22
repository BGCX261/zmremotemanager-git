package com.zm.epad.plugins.backup;

import java.util.ArrayList;

class FileRecord extends Record {
    ArrayList<OneFile> mFileList;

    FileRecord(RecordSet recordSet) {
        super(recordSet);
    }

    @Override
    void backup() {
    }

    @Override
    String path() {
        return null;
    }

    @Override
    void restore() {
    }

    @Override
    int type() {
        return TYPE_FILE;
    }

    int count() {
	return 0;
    }
}
