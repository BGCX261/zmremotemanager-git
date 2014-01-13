LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)


#LOCAL_JAVA_LIBRARIES := services telephony-common

LOCAL_STATIC_JAVA_LIBRARIES := smackxmpp

LOCAL_PACKAGE_NAME := RemoteManager
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

#LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
