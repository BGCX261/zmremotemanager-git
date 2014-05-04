/**
 * Copyright (c) 2014, The ZM-Tech Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zm.epad;

import android.content.ComponentName;
import android.app.Activity;
import android.app.PendingIntent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.zm.epad.IRemoteManager.Stub;

public class RemoteManager {
    public static final int RESULT_OK = Activity.RESULT_OK;
    public static final int RESULT_BASE = Activity.RESULT_FIRST_USER;
    public static final int RESULT_FAILED = RESULT_BASE;
    public static final int RESULT_NETWORK_ERROR = RESULT_BASE + 1;
    public static final int RESULT_LOGIN_INFO_ERROR = RESULT_BASE + 2;
    public static final int RESULT_USER = RESULT_BASE + 8000;

    public RemoteManager() {
        mService = IRemoteManager.Stub.asInterface(ServiceManager
                .getService(SERVICE));
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public boolean isLogined() {
        try {
            return mService.isLogined();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }

    public boolean login(String userName, String password, PendingIntent intent) {
        try {
            return mService.login(userName, password, intent);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    private final String SERVICE = "com.zm.epad.IRemoteManager";
    private IRemoteManager mService;
    private Handler mHandler;
}
