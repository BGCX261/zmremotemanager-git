LOCAL_PATH := $(call my-dir)

##################################################
# Static library
##################################################
include $(CLEAR_VARS)

LOCAL_MODULE := zmcommon
LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := 9

LOCAL_SRC_FILES := $(call all-java-files-under,src)

include $(BUILD_STATIC_JAVA_LIBRARY)
