LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += src/com/zm/epad/plugins/backup/IZmObserver.aidl
LOCAL_SRC_FILES += src/com/zm/epad/plugins/backup/IZmBackupManager.aidl

#LOCAL_JAVA_LIBRARIES := services telephony-common

LOCAL_STATIC_JAVA_LIBRARIES := smackxmpp zmcommon IRemoteManager

LOCAL_PACKAGE_NAME := RemoteManager
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

#LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
