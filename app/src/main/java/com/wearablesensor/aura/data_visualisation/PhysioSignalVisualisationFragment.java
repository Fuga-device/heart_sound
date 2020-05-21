/**
 * @file PhysioSignalVisualisationFragment.java
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
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.wearablesensor.aura.R;
import com.wearablesensor.aura.data_repository.models.PhysioSignalModel;
import com.wearablesensor.aura.data_repository.models.RRIntervalModel;
import com.xw.repo.BubbleSeekBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PhysioSignalVisualisationFragment extends Fragment implements DataVisualisationContract.View {
    private final String TAG = this.getClass().getSimpleName();

    @BindView(R.id.current_hr) TextView mCurrentHRText;
    @BindView(R.id.start_heart_beat_player) Button mStartHeartBeatPlayerButton;
    @OnClick(R.id.start_heart_beat_player)
    public void clickOnStartHeartBeat(){
        mPresenter.startHeartBeat();
    }

    @OnClick(R.id.stop_heart_beat_player)
    public void clickOnStopHeartBeat(){
        mPresenter.stopHeartBeat();
    }


    @BindView(R.id.stop_heart_beat_player) Button mStopHeartBeatPlayerButton;
    @BindView(R.id.speed_amplify) com.xw.repo.BubbleSeekBar mSpeedAmplifySeekBar;

    private RealTimePhysioSignalListAdapter mPhysioSignalListAdapter;

    private HashMap<String, PhysioSignalModel> mCurrentPhysioSignals;

    private OnFragmentInteractionListener mListener;

    private DataVisualisationContract.Presenter mPresenter;

    public PhysioSignalVisualisationFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment RRSamplesVisualisationFragment.
     */
    public static PhysioSignalVisualisationFragment newInstance() {
        PhysioSignalVisualisationFragment fragment = new PhysioSignalVisualisationFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {}
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_hrv_realtime_display, container, false);
        ButterKnife.bind(this, view);

        mSpeedAmplifySeekBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                Log.d("SeekBAr", "OnProgressChanged " + progressFloat);
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
                Log.d("SeekBAr", "OnProgressChanged Action UP " + progressFloat);
                mPresenter.setSpeedAmplify(progressFloat);
            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                Log.d("SeekBAr", "OnProgressChanged Finally " + progressFloat);
            }
        });


        //Some audio may be explicitly marked as not being music
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION
        };

        Cursor cursor = getActivity().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

        List<String> songs = new ArrayList<String>();
        while(cursor.moveToNext()) {
            songs.add(cursor.getString(0) + "||"
                    + cursor.getString(1) + "||"
                    + cursor.getString(2) + "||"
                    + cursor.getString(3) + "||"
                    + cursor.getString(4) + "||"
                    + cursor.getString(5));
        }

        for(int i = 0; i < songs.size(); i++){
            Log.d("SONGS", songs.get(i));
        }

        cursor.close();

        mCurrentPhysioSignals = new HashMap<>();
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onHRVRealTimeDisplayFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.start();
    }

    @Override
    /**
     * @brief enable data visualisation on app
     */
    public void enablePhysioSignalVisualisation() {
        mCurrentHRText.setVisibility(View.VISIBLE);
    }

    @Override
    /**
     * @brief disable data visualisation on app
     */
    public void disablePhysioSignalVisualisation() {
        mCurrentHRText.setVisibility(View.GONE);
    }

    @Override
    /**
     * @brief  refresh data visualisation when receiving a new data sample
     *
     * @param physiological data sample
     */
    public void refreshPhysioSignalVisualisation(PhysioSignalModel iPhysioSignal) {

        Log.d(TAG, "BEAT BEAT" + iPhysioSignal.getType());
        int lCurrentHR = 0;

        // filter invalid heart beat
        if( ((RRIntervalModel) iPhysioSignal).getRrInterval() < 300 || ((RRIntervalModel) iPhysioSignal).getRrInterval() > 2000){
            return;
        }

        if(mCurrentPhysioSignals.containsKey(iPhysioSignal.getDeviceAdress() + "-" + iPhysioSignal.getType())){
            mCurrentPhysioSignals.put(iPhysioSignal.getDeviceAdress() + "-" + iPhysioSignal.getType(), iPhysioSignal);
            for(PhysioSignalModel lPhysio : mCurrentPhysioSignals.values()){
                lCurrentHR += 60 * 1000 * 1.0/ ((RRIntervalModel) lPhysio).getRrInterval();
            }
        }
        else{
            mCurrentPhysioSignals.put(iPhysioSignal.getDeviceAdress() + "-" + iPhysioSignal.getType(), iPhysioSignal);

            for(PhysioSignalModel lPhysio : mCurrentPhysioSignals.values()){
                lCurrentHR += 60 * 1000 * 1.0/ ((RRIntervalModel) lPhysio).getRrInterval();
            }
        }

        Log.d(TAG, mCurrentPhysioSignals.size() + " ");

        mCurrentHRText.setText(String.valueOf(lCurrentHR));
    }

    @Override
    public void enterHeartBeatAnomalyMode() {
        //mPhysioSignalListAdapter.enterHeartBeatAnomalyMode();
    }

    @Override
    public void leavHeartBeatAnomalyMode() {
        //mPhysioSignalListAdapter.leavHeartBeatAnomalyMode();
    }

    @Override
    public void setPresenter(DataVisualisationContract.Presenter iPresenter) {
        mPresenter = iPresenter;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onHRVRealTimeDisplayFragmentInteraction(Uri uri);
    }
}
