/**
 * @file DataVisualisationPresenter.java
 * @author  clecoued <clement.lecouedic@aura.healthcare>
 * @version 1.0
 *
 *
 * @section LICENSE
 *
 * Aura Mobile Application
 * Copyright (C) 2017 Aura Healthcare
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 *
 * @section DESCRIPTION
 *
 *
 */

package com.wearablesensor.aura.data_visualisation;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.google.common.util.concurrent.AtomicDouble;
import com.wearablesensor.aura.R;
import com.wearablesensor.aura.data_repository.models.PhysioSignalModel;
import com.wearablesensor.aura.data_repository.models.RRIntervalModel;
import com.wearablesensor.aura.device_pairing.notifications.DevicePairingNotification;
import com.wearablesensor.aura.device_pairing.notifications.DevicePairingReceivedDataNotification;
import com.wearablesensor.aura.device_pairing.notifications.DevicePairingStatus;
import com.wearablesensor.aura.real_time_data_processor.TimeSerieEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataVisualisationPresenter implements DataVisualisationContract.Presenter {

    private final String TAG = this.getClass().getSimpleName();

    private final DataVisualisationContract.View mView;

    private HashMap<String, PhysioSignalModel> mCurrentPhysioSignals;

    private Context mContext;

    private HeartBeatPlayer mHeartBeatPlayer;

    private AtomicDouble mSpeedAmplify;
    private AtomicDouble mSpeed;
    private AtomicBoolean mForceStopHeartBeatPlayer;

    public DataVisualisationPresenter(DataVisualisationContract.View iView, Context iContext){
        mView = iView;
        mView.setPresenter(this);

        mContext = iContext;

        mCurrentPhysioSignals = new HashMap<>();
        mSpeed = new AtomicDouble(1.0);
        mSpeedAmplify = new AtomicDouble(1);
        mForceStopHeartBeatPlayer = new AtomicBoolean(false);

        EventBus.getDefault().register(this);
    }

    @Override
    public void start() {

    }

    /**
     * @brief handle receiving a new data sample
     *
     * @param iPhysioSignal physiological data sample
     */
    @Override
    public void receiveNewPhysioSample(PhysioSignalModel iPhysioSignal) {
        // filter invalid heart beat
        if( ((RRIntervalModel) iPhysioSignal).getRrInterval() > 300 && ((RRIntervalModel) iPhysioSignal).getRrInterval() < 2000){
            mCurrentPhysioSignals.put(iPhysioSignal.getDeviceAdress() + "-" + iPhysioSignal.getType(), iPhysioSignal);
        }

        int lTotalRR = 0;
        for(PhysioSignalModel lPhysio: mCurrentPhysioSignals.values()){
            RRIntervalModel lRR = (RRIntervalModel) lPhysio;
            lTotalRR += lRR.getRrInterval();
        }

        double lAverageRR = lTotalRR * 1.0 / mCurrentPhysioSignals.size();
        double lSpeed = 1000 / lAverageRR;
        if(lSpeed > 1){
            lSpeed = lSpeed * mSpeedAmplify.get();
        }
        else{
            lSpeed = lSpeed * 1 / mSpeedAmplify.get();
        }


        mSpeed.set(lSpeed);

        mView.refreshPhysioSignalVisualisation(iPhysioSignal);
    }

    public void startHeartBeat(){
        if(mHeartBeatPlayer == null) {
            mForceStopHeartBeatPlayer.set(false);
            mHeartBeatPlayer = new HeartBeatPlayer();
            mHeartBeatPlayer.execute();
        }
    }

    public void stopHeartBeat(){
        mForceStopHeartBeatPlayer.set(true);
        mHeartBeatPlayer = null;
    }

    public void setSpeedAmplify(double iAmplify){
        mSpeedAmplify.set(iAmplify);
    }

    /**
     * @brief method executed by observer class when receiving a device pairing notification event
     *
     * @param iDevicePairingNotification notification to be processed by observer class
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDevicePairingEvent(DevicePairingNotification iDevicePairingNotification){
        DevicePairingStatus lStatus = iDevicePairingNotification.getStatus();

        if(lStatus == DevicePairingStatus.DEVICE_CONNECTED || lStatus == DevicePairingStatus.DEVICE_DISCONNECTED){
            mView.disablePhysioSignalVisualisation();
        }
        else if(lStatus == DevicePairingStatus.RECEIVED_DATA){
            DevicePairingReceivedDataNotification lDevicePairingNotification = (DevicePairingReceivedDataNotification) iDevicePairingNotification;
            Log.d(TAG, "ReceivedData" + lDevicePairingNotification.getPhysioSignal().toString());
            receiveNewPhysioSample(lDevicePairingNotification.getPhysioSignal());
        }
    }

        @Subscribe(threadMode = ThreadMode.MAIN)
        public void onSignalStatusChange(TimeSerieEvent event){
            /*if(event.getType() == MetricType.HEART_BEAT && event.getState() == TimeSerieState.ANOMALY){
                mView.enterHeartBeatAnomalyMode();
            } else if(event.getType() == MetricType.HEART_BEAT) {
                mView.leavHeartBeatAnomalyMode();
            }*/
        }

    @Override
    public void finalize(){
        EventBus.getDefault().unregister(this);
    }


    class HeartBeatPlayer extends AsyncTask<Void, Void, Void> {
        private MediaPlayer mMediaPlayer;

        @Override
        protected void onPreExecute() {

        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        protected Void doInBackground(Void... voids) {
            playSample();

            return null;
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        private void playSample(){
            if(mMediaPlayer != null) {
                mMediaPlayer.reset();
            }
            mMediaPlayer = MediaPlayer.create(mContext, R.raw.heartbeat);
            mMediaPlayer.setPlaybackParams(new PlaybackParams().setSpeed((float)mSpeed.get()));
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    if(!mForceStopHeartBeatPlayer.get()) {
                        playSample();
                    }
                }
            });
            mMediaPlayer.start();
        }

    }

}
