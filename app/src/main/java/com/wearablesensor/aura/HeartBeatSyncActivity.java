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

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.wearablesensor.aura.data_visualisation.DataVisualisationPresenter;
import com.wearablesensor.aura.data_visualisation.PhysioSignalVisualisationFragment;
import com.wearablesensor.aura.device_pairing_details.DevicePairingDetailsFragment;
import com.wearablesensor.aura.navigation.NavigationConstants;
import com.wearablesensor.aura.navigation.NavigationNotification;
import com.wearablesensor.aura.navigation.NavigationWithIndexNotification;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import butterknife.ButterKnife;


public class HeartBeatSyncActivity extends AppCompatActivity implements DevicePairingDetailsFragment.OnFragmentInteractionListener, PhysioSignalVisualisationFragment.OnFragmentInteractionListener{

    private final static String TAG = HeartBeatSyncActivity.class.getSimpleName();

    private ActionBarDrawerToggle mDrawerToggle;
    //@BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    //@BindView(R.id.nav_view) NavigationView mNavigationView;
    /*@BindView(R.id.drawer_menu_button) ImageButton mDrawerImageButton;
    @OnClick(R.id.drawer_menu_button)
    public void openDrawerMenu(){
        mDrawerLayout.openDrawer(Gravity.LEFT);
    }*/

    //private DevicePairingDetailsPresenter mDevicePairingDetailsPresenter;
    //private DevicePairingDetailsFragment mDevicePairingFragment;

    private DataVisualisationPresenter mDataVisualisationPresenter;
    private PhysioSignalVisualisationFragment mPhysioSignalVisualisationFragment;

    private ArrayList<Fragment> mAdditionalInformationFragments;
    private static final int REQUEST_ENABLE_BT = 1;

    private DataCollectorService mDataCollectorService;
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            mDataCollectorService = ((DataCollectorService.LocalBinder)service).getService();

            //mDevicePairingDetailsPresenter.setDevicePairingService(mDataCollectorService.getDevicePairingService());
           // mDevicePairingDetailsPresenter.start();
        }

        public void onServiceDisconnected(ComponentName className) {
            mDataCollectorService = null;
        }
    };
    private Boolean mIsDataCollectorBound = false;

    void doBindService() {

        bindService(new Intent(getApplicationContext(), DataCollectorService.class),
                mConnection,
                Context.BIND_AUTO_CREATE);
        mIsDataCollectorBound = true;
    }

    void doUnbindService() {
        if (mIsDataCollectorBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsDataCollectorBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_heart_beat_sync);

        //mDevicePairingFragment = new DevicePairingDetailsFragment();
        //mDevicePairingDetailsPresenter = new DevicePairingDetailsPresenter(( (mDataCollectorService != null) ? mDataCollectorService.getDevicePairingService():null), mDevicePairingFragment);
        mPhysioSignalVisualisationFragment = new PhysioSignalVisualisationFragment();
        mDataVisualisationPresenter = new DataVisualisationPresenter(mPhysioSignalVisualisationFragment, this);

        displayFragments();

        EventBus.getDefault().register(this);
        ButterKnife.bind(this);

        //mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, 0, 0) {

        /** Called when a drawer has settled in a completely closed state. */
         /*   public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
           }*/

            /** Called when a drawer has settled in a completely open state. */

     /*       public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mNavigationView.setCheckedItem(R.id.nav_ContinuousMonitoring);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.nav_ContinuousMonitoring:
                        break;
                    case R.id.nav_DisconnectDevices:
                        disconnectDevices();
                        break;
                    case R.id.nav_LeaveApp:
                        quitApplication();
                        break;
                }
                return true;
            }

        });*/
        //wait the fragment to be fully displayed before starting automatic pairing
        startDataCollector();
    }

    private void disconnectDevices() {
        if(mDataCollectorService == null
        || mDataCollectorService.getDevicePairingService() == null){
            return;
        }

        if(mDataCollectorService.getDevicePairingService().disconnectDevices()) {
            Toast.makeText(getBaseContext(), R.string.devices_disconnected_info, Toast.LENGTH_LONG).show();
        }
    }

    private void quitApplication() {
        stopDataCollector();

        finish();
    }

    private void stopDataCollector() {
        doUnbindService();

        Intent stopIntent = new Intent(HeartBeatSyncActivity.this, DataCollectorService.class);
        stopIntent.setAction(DataCollectorServiceConstants.ACTION.STOPFOREGROUND_ACTION);
        stopService(stopIntent);
    }

    private void startDataCollector(){
        // no running Aura Data Collector service
        if(!isMyServiceRunning(DataCollectorService.class)){
            Intent startIntent = new Intent(HeartBeatSyncActivity.this, DataCollectorService.class);
            startIntent.putExtra("UserUUID", "00000-000000-0000000");
            startIntent.setAction(DataCollectorServiceConstants.ACTION.STARTFOREGROUND_ACTION);
            startService(startIntent);

            doBindService();
        }
        // running Aura Data Collector service but not binded to Activity
        else if(isMyServiceRunning(DataCollectorService.class) && mDataCollectorService == null){
            doBindService();
        }
    }

    @Override
    public void onStart(){
        super.onStart();

    }

    @Override
    public void onStop(){
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        Fragment lSeizureReportFragment = null;
        Fragment lSeizureMonitoringFragment = getSupportFragmentManager().findFragmentByTag(DevicePairingDetailsFragment.class.getSimpleName());
        if(lSeizureReportFragment != null && lSeizureReportFragment.isVisible()){
            goToSeizureMonitoring();
        }
        else if(lSeizureMonitoringFragment != null && lSeizureMonitoringFragment.isVisible()){
            moveTaskToBack(true);
        }
        else {
            super.onBackPressed();
        }
    }



    private boolean isMyServiceRunning(Class<?> iServiceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (iServiceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void displayFragments(){
        FragmentTransaction lTransaction = getSupportFragmentManager().beginTransaction();

        lTransaction.add(R.id.content_frame, mPhysioSignalVisualisationFragment, PhysioSignalVisualisationFragment.class.getSimpleName() );

        // Commit the transaction
        lTransaction.commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
       // mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
       // mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
       /* if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }*/
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDevicePairingAttempt() {

    }

    @Override
    public void onHRVRealTimeDisplayFragmentInteraction(Uri uri) {

    }

    @Override
    public void onDestroy(){
        doUnbindService();
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    //TODO: replace by an independent Navigation component
    /**
     * @brief method to handle navigation between additional question fragments
     * @param iQuestionIndex
     */
    public void goToAdditionnalQuestions(int iQuestionIndex){

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNavigationEvent(NavigationNotification iNavigationEvent){

        switch (iNavigationEvent.getNavigationFlag()){
            case NavigationConstants.NAVIGATION_SEIZURE_MONITORING:
                goToSeizureMonitoring();
                break;
            case NavigationConstants.NAVIGATION_SEIZURE_REPORTING:
                goToSeizureReporting();
                break;
            case NavigationConstants.NAVIGATION_SEIZURE_NEXT_QUESTION:
                NavigationWithIndexNotification iNavigationEventWithIndex = (NavigationWithIndexNotification) iNavigationEvent;
                goToAdditionnalQuestions(iNavigationEventWithIndex.getIndex());
                break;
            case NavigationConstants.NAVIGATION_DEVICE_SCANNING:
                goToDeviceScanning();
                break;
            default:
        }
    }

    private void goToDeviceScanning() {
        Intent intent = new Intent(this, DeviceScanActivity.class);
        startActivity(intent);

        this.finish();
    }

    @SuppressLint("RestrictedApi")
    private void goToSeizureMonitoring() {
        android.support.v4.app.FragmentManager lFragmentManager = getSupportFragmentManager();
        FragmentTransaction lTransaction = lFragmentManager.beginTransaction();

        for(Fragment lFragment: lFragmentManager.getFragments()){
            if (lFragment != null) {
                lTransaction.remove(lFragment);
            }
        }

        lTransaction.add(R.id.content_frame, lFragmentManager.findFragmentByTag(PhysioSignalVisualisationFragment.class.getSimpleName()));
        lTransaction.addToBackStack(null);

        lTransaction.commit();
    }

    @SuppressLint("RestrictedApi")
    private void goToSeizureReporting() {
        android.support.v4.app.FragmentManager lFragmentManager = getSupportFragmentManager();
        FragmentTransaction lTransaction = lFragmentManager.beginTransaction();

        for(Fragment lFragment: lFragmentManager.getFragments()){
            if (lFragment != null) {
                lTransaction.remove(lFragment);
            }
        }

        lTransaction.addToBackStack(null);

        lTransaction.commit();
    }
}
