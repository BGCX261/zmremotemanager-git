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
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.zm.epad.IRemoteManager.Stub;

public class RemoteManager {

    public RemoteManager() {
        mService = IRemoteManager.Stub.asInterface(ServiceManager
                .getService(SERVICE));
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public boolean login(String userName, String password) {
        try {
            return mService.login(userName, password);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    private final String SERVICE = "com.zm.epad.IRemoteManager";
    private IRemoteManager mService;
    private Handler mHandler;
}
