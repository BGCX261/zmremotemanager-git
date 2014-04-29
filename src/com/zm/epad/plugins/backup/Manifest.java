package com.zm.epad.plugins.backup;

import android.os.Build;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SyncFailedException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

class Manifest {
    public final static String TAG = BackupManager.TAG;

    private final String mManifestPath;
    private long mDateInMillis;
    private String mSystemVersion;

    private Document mDoc;
    private Element mRoot;

    final static String BACKUP_MANIFEST = ".manifest";

    private final static String ELMNT_RECORDSET = "recordset";
    private final static String ATTR_RECORDSET_VERSION = "version";
    private final static String ATTR_SYSTEM_VERSION = "systemversion";
    private final static String ATTR_RECORDSET_TIMESTAMP = "timestamp";

    private final static String ELMNT_RECORD = "record";
    private final static String ATTR_RECORD_NAME = "name";
    private final static String ATTR_RECORD_TYPE = "type";
    private final static String ATTR_RECORD_SIZE = "size";
    private final static String ATTR_RECORD_ORDER = "order";
    private final static String ATTR_RECORD_ID = "id";

    private final static String ELMNT_RECORD_FILELIST = "filelist";
    private final static String ATTR_FILE_DEVICE_TYPE = "devicetype";
    private final static String ATTR_FILE_RELATIVE_PATH = "relativepath";

    private final static String ELMNT_RECORD_PACKAGELIST = "packagelist";
    private final static String ELMNT_PACKAGE = "package";

    private final static String ELMNT_FILE = "file";

    private final static int RECORD_MANIFEST_VERSION_1 = 1;

    Manifest(String basePath) {
        mManifestPath = basePath + File.separatorChar + Manifest.BACKUP_MANIFEST;
    }

    void prepareBackup() {
        Calendar calendar = Calendar.getInstance();
        //SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US);
        //String currentDate = simpleDateFormat.format(calendar.getTime());
        mDateInMillis = calendar.getTimeInMillis();
        mSystemVersion = Build.VERSION.RELEASE;
        mRoot = createElementRecordSet();
        if (mRoot == null) {
            Log.e(TAG, "mRoot = null");
        }
    }

    private Element createElementRecordSet() {
        DocumentBuilderFactory docBuilderFactory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
            mDoc = docBuilder.newDocument();
            Element recordSetElmnt = mDoc.createElement(ELMNT_RECORDSET);
            recordSetElmnt.setAttribute(ATTR_RECORDSET_VERSION,
                    String.valueOf(RECORD_MANIFEST_VERSION_1));
            recordSetElmnt.setAttribute(ATTR_RECORDSET_TIMESTAMP,
                    String.valueOf(mDateInMillis));
            recordSetElmnt.setAttribute(ATTR_SYSTEM_VERSION, mSystemVersion);
            return recordSetElmnt;
        } catch (ParserConfigurationException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            docBuilder = null;
            docBuilderFactory = null;
        }
        return null;
    }

    void addElementRecord(Record r) {
        if (mRoot == null) return;
        Element recordElmnt = mDoc.createElement(ELMNT_RECORD);
        recordElmnt.setAttribute(ATTR_RECORD_NAME, r.mDisplayName);
        recordElmnt.setAttribute(ATTR_RECORD_TYPE, String.valueOf(r.type()));
        recordElmnt.setAttribute(ATTR_RECORD_SIZE, String.valueOf(r.mSize));
        recordElmnt.setAttribute(ATTR_RECORD_ORDER, String.valueOf(r.mOrder));

        if (r instanceof AppRecord) {
            AppRecord ar = (AppRecord) r;
            recordElmnt.setAttribute(ATTR_RECORD_ID, ar.mPackageName);
            Element pkgLstElmnt = createBackupPackageListElement(mDoc, ar.mAllPackages);
            recordElmnt.appendChild(pkgLstElmnt);
        } else if (r instanceof FileRecord) {
            FileRecord fr = (FileRecord) r;
            Element fileListElmnt = createBackupFileListElement(mDoc, fr.mFileList);
            recordElmnt.appendChild(fileListElmnt);
        }
        mRoot.appendChild(recordElmnt);
    }

    private Element createBackupPackageListElement(Document doc, String[] pkgList) {
        Element pkgLstElmnt = doc.createElement(ELMNT_RECORD_PACKAGELIST);
        for (String pkg : pkgList) {
            Element pkgElmnt = doc.createElement(ELMNT_PACKAGE);
            pkgElmnt.setTextContent(pkg);
            pkgLstElmnt.appendChild(pkgElmnt);
        }
        return pkgLstElmnt;
    }

    private Element createBackupFileListElement(Document doc, ArrayList<OneFile> fileList) {
        Element fileLstElmnt = doc.createElement(ELMNT_RECORD_FILELIST);

        for (OneFile fp : fileList) {
            Element fileElmnt = doc.createElement(ELMNT_FILE);
            fileElmnt.setAttribute(ATTR_FILE_DEVICE_TYPE, fp.mDeviceType);
            fileElmnt.setAttribute(ATTR_FILE_RELATIVE_PATH, fp.mRelativePath);
            fileLstElmnt.appendChild(fileElmnt);
        }
        return fileLstElmnt;
    }


    void writeToFile() {
        if (mRoot == null) return;
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        FileOutputStream fileOutStream = null;
        File manifest = new File(mManifestPath);
        manifest.getParentFile().mkdirs();
        try {
            transformer = transformerFactory.newTransformer();
            fileOutStream = new FileOutputStream(manifest);
            Result result = new StreamResult(fileOutStream);
            Source source = new DOMSource(mRoot);
            transformer.transform(source, result);
            fileOutStream.getFD().sync();
        } catch (TransformerConfigurationException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (TransformerException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (SyncFailedException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            transformer = null;
            transformerFactory = null;
            try {
                if (fileOutStream != null)
                    fileOutStream.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    void close() {
        if (mRoot != null) mRoot = null;
        if (mDoc != null) mDoc = null;
    }

    String path() {
        return mManifestPath;
    }

    String timeString() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mDateInMillis);
        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US);
        return simpleDateFormat.format(calendar.getTime());
    }

    void readFromFile() {
        if (mRoot != null) return;
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        FileInputStream fileInStream = null;
        File manifest = new File(mManifestPath);
        try {
            transformer = transformerFactory.newTransformer();
            fileInStream = new FileInputStream(manifest);
            Source source = new StreamSource(fileInStream);
            // result
            DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
            dFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
            mDoc = dBuilder.newDocument();
            Result result = new DOMResult(mDoc);
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (TransformerException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            transformer = null;
            transformerFactory = null;
            try {
                if (fileInStream != null)
            fileInStream.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    void readRoot() {
        Element e = mDoc.getDocumentElement();
        if (e.getNodeName().equals(ELMNT_RECORDSET)) {
            String val;
            val = e.getAttribute(ATTR_RECORDSET_VERSION);
            if (val == null || Integer.parseInt(val) != RECORD_MANIFEST_VERSION_1) {
                Log.e(TAG, "parse manifest error: version=" + val);
            }
            val = e.getAttribute(ATTR_RECORDSET_TIMESTAMP);
            mDateInMillis = val == null ? 0 : Long.parseLong(val);
            mSystemVersion = e.getAttribute(ATTR_SYSTEM_VERSION);
        }
    }

    void readRecords(RecordSet recordSet) {
        NodeList nl = mDoc.getElementsByTagName(ELMNT_RECORD);
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element)nl.item(i);
            String val;
            val = e.getAttribute(ATTR_RECORD_TYPE);
            if (val == null) continue;
            int type = Integer.parseInt(val);
            Record r;
            if (type == Record.TYPE_SYSTEM || type == Record.TYPE_INSTALLED) {
                r = new AppRecord(recordSet, type == Record.TYPE_INSTALLED);
            } else if (type == Record.TYPE_FILE) {
                r = new FileRecord(recordSet);
            } else {
                continue;
            }
            r.mDisplayName = e.getAttribute(ATTR_RECORD_NAME);
            val = e.getAttribute(ATTR_RECORD_SIZE);
            r.mSize = (val == null) ? 0 : Long.parseLong(val);
            val = e.getAttribute(ATTR_RECORD_ORDER);
            r.mOrder = (val == null) ? 0 : Integer.parseInt(val);
            if (type == Record.TYPE_SYSTEM || type == Record.TYPE_INSTALLED) {
                AppRecord ar = (AppRecord) r;
                ar.mPackageName = e.getAttribute(ATTR_RECORD_ID);
                NodeList pl = e.getElementsByTagName(ELMNT_PACKAGE);
                ar.mAllPackages = new String[pl.getLength()];
                for (int j = 0; j < pl.getLength(); j++) {
                    Element p = (Element)pl.item(j);
                    ar.mAllPackages[j] = p.getNodeValue();
                }
            } else {
                //FileRecord fr = (FileRecord) r;
            }
            recordSet.addRecord(r);
        }
    }
}
