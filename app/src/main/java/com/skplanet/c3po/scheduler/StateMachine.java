package com.skplanet.c3po.scheduler;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.location.DetectedActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by 1002407 on 15. 6. 30..
 */
public class StateMachine {
    private static final String TAG = StateMachine.class.getSimpleName();

    private static String PREF_NAME = "stateMachine";
    private static String PREF_KEY_STATE = "state";

    public static void write(Context con, String variable, Integer data) {
        SharedPreferences prefs = con.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(variable, data).commit();
    }

    public static Integer getInt(Context con, String variable, Integer defaultValue) {
        SharedPreferences prefs = con.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Integer data = prefs.getInt(variable, defaultValue);
        return data;
    }

    private static StateMachine stateMachineInstance = null;

    private Context mContext;
    private int mCurrentState;

    private Map<Integer, Map<Integer, Integer>> stateDescription;

    public static final int STATE_KEEP_SCAN = 0;
    public static final int STATE_SCAN_SHORTLY = 1;
    public static final int STATE_STOP_SCAN = 2;

    private static final String[] STATES = {
        "STATE_KEEP_SCAN",
        "STATE_SCAN_SHORTLY",
        "STATE_STOP_SCAN"
    };

    public static StateMachine getInstance(Context context) {
        if (stateMachineInstance == null) {
            stateMachineInstance = new StateMachine(context);
        }

        return stateMachineInstance;
    }

    protected StateMachine(Context context) {
        super();

        mContext = context;

        mCurrentState = getInt(context, PREF_KEY_STATE, Integer.valueOf(STATE_KEEP_SCAN));
    }

    public int getState() {
        return mCurrentState;
    }

    public String getStateString(int state) {
        if (0 <= state && state <= STATES.length) {
            return STATES[state];
        }

        return "UNKNOWN";
    }

    public boolean loadDescription() {
        // Inputs:
        //  IN_VEHICLE
        //  ON_BICYCLE
        //  ON_FOOT
        //  STILL
        //  UNKNOWN
        //  TILTING

        if (stateDescription != null) {
            return true;
        }

        Log.i(TAG, "Set stateDescription as default.");
        // STATE_KEEP_SCAN
        Map<Integer, Integer> keepScanDescription = new HashMap<Integer, Integer>();

        keepScanDescription.put(DetectedActivity.IN_VEHICLE, STATE_SCAN_SHORTLY);
        keepScanDescription.put(DetectedActivity.ON_BICYCLE, STATE_SCAN_SHORTLY);
        keepScanDescription.put(DetectedActivity.STILL, STATE_SCAN_SHORTLY);

        keepScanDescription.put(DetectedActivity.ON_FOOT, STATE_KEEP_SCAN);
        keepScanDescription.put(DetectedActivity.UNKNOWN, STATE_KEEP_SCAN);
        keepScanDescription.put(DetectedActivity.TILTING, STATE_KEEP_SCAN);

        // STATE_SCAN_SHORTLY
        Map<Integer, Integer> scanShortlyDescription = new HashMap<Integer, Integer>();

        scanShortlyDescription.put(DetectedActivity.IN_VEHICLE, STATE_STOP_SCAN);
        scanShortlyDescription.put(DetectedActivity.ON_BICYCLE, STATE_STOP_SCAN);
        scanShortlyDescription.put(DetectedActivity.STILL, STATE_STOP_SCAN);

        scanShortlyDescription.put(DetectedActivity.ON_FOOT, STATE_KEEP_SCAN);

        scanShortlyDescription.put(DetectedActivity.UNKNOWN, STATE_SCAN_SHORTLY);
        scanShortlyDescription.put(DetectedActivity.TILTING, STATE_SCAN_SHORTLY);

        // STATE_STOP_SCAN
        Map<Integer, Integer> stopScanDescription = new HashMap<Integer, Integer>();

        stopScanDescription.put(DetectedActivity.ON_FOOT, STATE_SCAN_SHORTLY);
        stopScanDescription.put(DetectedActivity.TILTING, STATE_SCAN_SHORTLY);

        stateDescription = new HashMap<Integer, Map<Integer, Integer>>();

        stateDescription.put(STATE_KEEP_SCAN, keepScanDescription);
        stateDescription.put(STATE_SCAN_SHORTLY, scanShortlyDescription);
        stateDescription.put(STATE_STOP_SCAN, stopScanDescription);

        return true;
    }

    public boolean storeDescription() {
        Log.i(TAG, stateDescription.toString());

        return false;
    }

    public int doTransition(int detectedActivity) {
        if (loadDescription()) {
            Log.i(TAG + "/Activities", "Got " + Constants.getActivityString(mContext, detectedActivity) + ", Current state: " + getStateString(mCurrentState));
            Map<Integer, Integer> stateMap = stateDescription.get(mCurrentState);

            if (stateMap.containsKey(detectedActivity)) {
                int transitionState = stateMap.get(detectedActivity);

                switch(transitionState) {
                    case STATE_KEEP_SCAN:
                    case STATE_SCAN_SHORTLY:
                        // Set Coin
                        Log.i(TAG, "Set new coin.");
                        break;

                    case STATE_STOP_SCAN:
                        break;

                    default:
                        Log.w(TAG + "/Activities", "Keep '" + getStateString(mCurrentState)
                                + "', because of unknown transition state: " + getStateString(transitionState));
                        return mCurrentState;
                }

                Log.i(TAG + "/Activities", "Transition from '" + getStateString(mCurrentState) + "' to '" + getStateString(transitionState) + "'.");

                mCurrentState = transitionState;
                write(mContext, PREF_KEY_STATE, mCurrentState);
            }

            return mCurrentState;
        }

        return STATE_KEEP_SCAN;
    }
}
