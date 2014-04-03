LOCAL_PATH := $(call my-dir)

##################################################
# Static library
##################################################
include $(CLEAR_VARS)

LOCAL_MODULE := IRemoteManager
LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := 9

LOCAL_JAVA_LIBRARIES := framework

LOCAL_SRC_FILES := $(call all-java-files-under,src)
LOCAL_SRC_FILES += src/com/zm/epad/IRemoteManager.aidl

include $(BUILD_STATIC_JAVA_LIBRARY)
#include $(BUILD_JAVA_LIBRARY)

include $(BUILD_DROIDDOC)
