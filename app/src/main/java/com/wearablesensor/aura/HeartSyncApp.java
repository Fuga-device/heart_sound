/*
Aura Mobile Application
Copyright (C) 2017 Aura Healthcare

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/
*/

package com.wearablesensor.aura;


import android.content.Context;

import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.facebook.soloader.SoLoader;


public class HeartSyncApp extends MultiDexApplication {

    @Override
    public void onCreate() {
        Log.d("HeartSyncApp", "Init");
        super.onCreate();

        SoLoader.init(this, false);

        Context lApplicationContext = getApplicationContext();
        }

    @Override
    public void onLowMemory(){
        super.onLowMemory();
        Log.d("AURA APP", "LOW MEMORY ");
    }

    @Override
    public void onTrimMemory(int level){
        super.onTrimMemory(level);
        Log.d("AURA APP", "TRIM MEMORY " + level);
    }

}
