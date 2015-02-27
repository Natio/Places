package com.gcw.sapienza.places.activities;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.fragments.PlacesLoginFragment;
import com.gcw.sapienza.places.utils.GPlusUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.parse.FunctionCallback;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.parse.ui.ParseLoginActivity;
import com.google.android.gms.common.ConnectionResult;
import java.io.IOException;
import java.util.HashMap;


/**
 * Created by paolo on 23/02/15.
 */
public class PlacesLoginActivity extends ParseLoginActivity implements  com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks,
                                                                        com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener,
                                                                        View.OnClickListener {

    private static final String TAG = "PlacesLoginActivity";

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    private static final int REQUEST_CODE_TOKEN_AUTH = 1;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* A flag indicating that a PendingIntent is in progress and prevents
     * us from starting further intents.
     */
    private boolean mIntentInProgress;

    private final int fragmentContainer = android.R.id.content;
    private Bundle configOptions;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Combine options from incoming intent and the activity metadata
        configOptions = getMergedOptions();

        // Show the login form
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(fragmentContainer,
                    PlacesLoginFragment.newInstance(configOptions)).commit();
        }

        // Check if there is a currently logged in user
        // and they are linked to a Facebook account.
        ParseUser currentUser = ParseUser.getCurrentUser();
        if ((currentUser != null) && ParseFacebookUtils.isLinked(currentUser)) {
            // Go to the user info activity
            setResult(RESULT_OK);
            finish();
        }
    }

    private Bundle getMergedOptions() {
        // Read activity metadata from AndroidManifest.xml
        ActivityInfo activityInfo = null;
        try {
            activityInfo = getPackageManager().getActivityInfo(
                    this.getComponentName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            if (Parse.getLogLevel() <= Parse.LOG_LEVEL_ERROR &&
                    Log.isLoggable(LOG_TAG, Log.WARN)) {
                Log.w(LOG_TAG, e.getMessage());
            }
        }

        // The options specified in the Intent (from ParseLoginBuilder) will
        // override any duplicate options specified in the activity metadata
        Bundle mergedOptions = new Bundle();
        if (activityInfo != null && activityInfo.metaData != null) {
            mergedOptions.putAll(activityInfo.metaData);
        }
        if (getIntent().getExtras() != null) {
            mergedOptions.putAll(getIntent().getExtras());
        }

        return mergedOptions;
    }

    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

        @Override
    public void onBackPressed() {
        //DO NOT call super.onBackPressed
        //to avoid dismissing login
        //view without logging in
    }

    public void onConnectionFailed(ConnectionResult result) {
        if (!mIntentInProgress && result.hasResolution()) {
            try {
                mIntentInProgress = true;
                startIntentSenderForResult(result.getResolution().getIntentSender(),
                        RC_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    public void onConnected(Bundle connectionHint) {
        // We've resolved any connection errors.  mGoogleApiClient can be used to
        // access Google APIs on behalf of the user.

        getGPlusAccessToken();
    }

    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onClick(View v)
    {
        if(v.getId() == R.id.gplus_sign_in_button) signinWithGPlus();
    }

    public void signinWithGPlus()
    {
        mGoogleApiClient.connect();
    }

    private void getGPlusAccessToken()
    {
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String accessToken = null;
                try {
                    accessToken = GoogleAuthUtil.getToken(getApplicationContext(),
                            Plus.AccountApi.getAccountName(mGoogleApiClient),
                            "oauth2:" + Scopes.PLUS_LOGIN);
                } catch (IOException transientEx) {
                    // network or server error, the call is expected to succeed if you try again later.
                    // Don't attempt to call again immediately - the request is likely to
                    // fail, you'll hit quotas or back-off.
                    return null;
                } catch (UserRecoverableAuthException e) {
                    // Recover
                    accessToken = null;
                } catch (GoogleAuthException authEx) {
                    // Failure. The call is not expected to ever succeed so it should not be
                    // retried.
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                return accessToken;
            }

            @Override
            protected void onPostExecute(String token)
            {
                Log.i(TAG, "Google+ access token retrieved: " + token);

                completeLoginWithParse(token);
            }

        };

        task.execute();
    }

    private void completeLoginWithParse(String token)
    {
        String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
        final HashMap<String, Object> params = new HashMap();
        params.put("code", token);
        params.put("email", email);

        //loads the Cloud function to create a Google user
        ParseCloud.callFunctionInBackground("accessGoogleUser", params, new FunctionCallback<Object>()
        {
            @Override
            public void done(Object returnObj, ParseException e)
            {
                if (e == null)
                {
                    ParseUser.becomeInBackground(returnObj.toString(), new LogInCallback()
                    {
                        public void done(ParseUser user, ParseException e)
                        {
                            if (user != null && e == null)
                            {
                                Log.i(TAG, "The Google user validated");

                                GPlusUtils.getInstance().setGoogleApiClient(mGoogleApiClient);

                                setResult(RESULT_OK);
                                finish();
                            }
                            else if (e != null)
                            {
                                Toast.makeText(getApplicationContext(), "There was a problem creating your account.", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                                mGoogleApiClient.disconnect();
                            }
                            else Log.i(TAG, "The Google token could not be validated");
                        }
                    });
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "There was a problem creating your account.", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    mGoogleApiClient.disconnect();
                }
            }
        });
    }
}
