package com.skplanet.c3po.scheduler;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by 1002407 on 15. 6. 26..
 */
public class ARDetectService extends IntentService implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    protected static final String TAG = ARDetectService.class.getSimpleName();

    private static final long DEFAULT_INTERVAL = 5 * 1000l;

    public static final String ACTIVITY_CHANGED = ARDetectService.class.getName() + ".ACTIVITY_CHANGED";
    public static final String TIMEOUT_WITH_ACTIVITY = ARDetectService.class.getName() + ".TIMEOUT_WITH_ACTIVITY";

    private static final String PACKAKGE = "com.skplanet.c3po.scheduler";
    public static final String START_ACTION = PACKAKGE + ".START";
    public static final String STOP_ACTION = PACKAKGE + ".STOP";

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient = null;

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * ActivityRecognition API.
     */
    protected synchronized ConnectionResult connectGoogleApiClient() {
        Log.i(TAG, "buildGoogleApiClient()");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(ActivityRecognition.API)
                    .build();
        }

        return mGoogleApiClient.blockingConnect();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
    }

        /**
     * Used when requesting or removing activity detection updates.
     */
    private PendingIntent mActivityDetectionPendingIntent;

    private PendingIntent getActivityDetectionPendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mActivityDetectionPendingIntent != null) {
            return mActivityDetectionPendingIntent;
        }
        Intent intent = new Intent(this, ARDetectService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Registers for activity recognition updates using
     * {@link com.google.android.gms.location.ActivityRecognitionApi#requestActivityUpdates} which
     * returns a {@link com.google.android.gms.common.api.PendingResult}. Since this activity
     * implements the PendingResult interface, the activity itself receives the callback, and the
     * code within {@code onResult} executes. Note: once {@code requestActivityUpdates()} completes
     * successfully, the {@code ARDetectService} starts receiving callbacks when
     * activities are detected.
     */
    public void startDetect(final long detectionIntervalInMillies) {
        Log.i(TAG, "startDetect()");

        ConnectionResult result = connectGoogleApiClient();
        Log.v(TAG, "connectGoogleApiClient() returns " + result.toString());

        if (result.isSuccess()) {
            final Context context = this.getApplicationContext();

            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                    mGoogleApiClient,
                    detectionIntervalInMillies,
                    getActivityDetectionPendingIntent()
            ).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        Log.i(TAG, "Success adding activity detection.");

                        // Broadcast the list of detected activities.
                        Intent localIntent = new Intent(Constants.ACTIVITY_MONITORING_STATE_CHANGED);
                        localIntent.putExtra(Constants.ACTIVITY_EXTRA, "STARTED");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);

                    } else {
                        Log.e(TAG, "Error adding activity detection: " + status.getStatusMessage());
                    }
                }
            });
        }
    }

    /**
     * Removes activity recognition updates using
     * {@link com.google.android.gms.location.ActivityRecognitionApi#removeActivityUpdates} which
     * returns a {@link com.google.android.gms.common.api.PendingResult}. Since this activity
     * implements the PendingResult interface, the activity itself receives the callback, and the
     * code within {@code onResult} executes. Note: once {@code removeActivityUpdates()} completes
     * successfully, the {@code ARDetectService} stops receiving callbacks about
     * detected activities.
     */
    public void stopDetect() {
        Log.i(TAG, "stopDetect()");

        ConnectionResult result = connectGoogleApiClient();
        Log.v(TAG, "connectGoogleApiClient() returns " + result.toString());

        if (result.isSuccess()) {
            final Context context = this.getApplicationContext();

            // Remove all activity updates for the PendingIntent that was used to request activity
            // updates.
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
                    mGoogleApiClient,
                    getActivityDetectionPendingIntent()
            ).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        Log.i(TAG, "Success removing activity detection.");

                        // Broadcast the list of detected activities.
                        Intent localIntent = new Intent(Constants.ACTIVITY_MONITORING_STATE_CHANGED);
                        localIntent.putExtra(Constants.ACTIVITY_EXTRA, "STOPPED");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
                    } else {
                        Log.e(TAG, "Error removing activity detection: " + status.getStatusMessage());
                    }
                }
            });
        }
    }

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public ARDetectService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Handles incoming intents.
     * @param intent The Intent is provided (inside a PendingIntent) when requestActivityUpdates()
     *               is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent(): " + intent.getAction());

        if (!processDetectedActivities(intent)) {
            switch (intent.getAction()) {
                case START_ACTION:
                    long interval = DEFAULT_INTERVAL;
                    if (intent.hasExtra(Constants.ACTIVITY_UPDATES_INTERVAL)) {
                        interval = intent.getLongExtra(Constants.ACTIVITY_UPDATES_INTERVAL, DEFAULT_INTERVAL);
                    }
                    startDetect(interval);
                    break;
                case STOP_ACTION:
                    stopDetect();
                    break;
            }
        }
    }

    protected boolean processDetectedActivities(Intent intent) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

        if (result == null) {
            return false;
        }

        Log.i(TAG, "processDetectedActivities()");

        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.
        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

        // Log each activity.
        JSONObject activity = new JSONObject();

        int primeActivity = -1;
        int primeActivityConfidence = 0;

        try {
            for (DetectedActivity da : detectedActivities) {
                Log.i(TAG, Constants.getActivityString(
                                getApplicationContext(),
                                da.getType()) + " " + da.getConfidence() + "%"
                );

                activity.put(Constants.getActivityString(getApplicationContext(), da.getType()), da.getConfidence());

                switch (da.getType()) {
                    case DetectedActivity.IN_VEHICLE:
                    case DetectedActivity.ON_BICYCLE:
                    case DetectedActivity.STILL:
                    case DetectedActivity.TILTING:
                    case DetectedActivity.UNKNOWN:
                        if (primeActivityConfidence < da.getConfidence()) {
                            primeActivity = da.getType();
                            primeActivityConfidence = da.getConfidence();
                        }
                        break;

                    case DetectedActivity.ON_FOOT:
                        if (primeActivityConfidence <= da.getConfidence()) {
                            primeActivity = da.getType();
                            primeActivityConfidence = da.getConfidence();
                        }
                        break;

                    case DetectedActivity.RUNNING:
                    case DetectedActivity.WALKING:
                        break;
                }
            }

            {
                Intent batteryIntent = this.getApplicationContext().registerReceiver(null,
                        new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                int rawlevel = batteryIntent.getIntExtra("level", -1);
                double scale = batteryIntent.getIntExtra("scale", -1);
                double level = -1;
                if (rawlevel >= 0 && scale > 0) {
                    level = (double)rawlevel / scale;
                    activity.put("battery", (double)(level * 100.0f));
                }
            }

            if (primeActivity != -1) {
                StateMachine stateMachine = StateMachine.getInstance(this.getApplicationContext());
                if (stateMachine != null) {
                    int transitionState = stateMachine.doTransition(primeActivity);

                    activity.put("state", stateMachine.getStateString(transitionState));
                }
            }

            Log.i(TAG + "/Activities", activity.toString().replace("{", "").replace("}", "").replace(",", ", "));

            activity.put("timestamp", result.getTime());
        } catch (JSONException e) {
            Log.w(TAG, "Failed to process AR result.");
            return false;
        }

        // Broadcast the list of detected activities.
        Intent localIntent = new Intent(Constants.BROADCAST_ACTION);
        localIntent.putExtra(Constants.ACTIVITY_EXTRA, detectedActivities);

        StateMachine stateMachine = StateMachine.getInstance(this.getApplicationContext());
        localIntent.putExtra(Constants.STATE_EXTRA, stateMachine.getStateString(stateMachine.getState()));
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        PreferenceWriter.write(this.getApplicationContext(), "" + result.getTime(), activity.toString());

        return true;
    }
}
