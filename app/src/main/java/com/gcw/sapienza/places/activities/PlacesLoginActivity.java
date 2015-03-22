package com.gcw.sapienza.places.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.fragments.PlacesLoginFragment;
import com.gcw.sapienza.places.utils.GPlusUtils;
import com.gcw.sapienza.places.utils.PlacesLoginUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.PersonBuffer;
import com.parse.FunctionCallback;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.parse.ui.ParseLoginActivity;

import java.io.IOException;
import java.util.HashMap;


/**
 * Created by paolo on 23/02/15.
 */
public class PlacesLoginActivity extends ParseLoginActivity implements com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks,
        com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener,
        ResultCallback<People.LoadPeopleResult> {

    private static final String TAG = "PlacesLoginActivity";

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    private static final int REQUEST_CODE_TOKEN_AUTH = 1;
    private final int fragmentContainer = android.R.id.content;
    public boolean canChoose = true;
    /* A flag indicating that a PendingIntent is in progress and prevents
     * us from starting further intents.
     */
    private boolean mIntentInProgress;
    private Bundle configOptions;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();

        if (extras != null && extras.containsKey("canChoose")) {
            canChoose = extras.getBoolean("canChoose");
            if (!canChoose) {
                signinWithGPlus();
            }
        }

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

    @Override
    protected void onStop() {
        super.onStop();

        // if (GPlusUtils.getInstance().getGoogleApiClient() != null &&
        //         GPlusUtils.getInstance().getGoogleApiClient().isConnected()) {
        //     GPlusUtils.getInstance().getGoogleApiClient().disconnect();
        // }
    }

    @Override
    public void onBackPressed() {
        //DO NOT call super.onBackPressed
        //to avoid dismissing login
        //view without logging in
    }

    @Override
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
                GPlusUtils.getInstance().getGoogleApiClient().connect();
            }
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // We've resolved any connection errors.  mGoogleApiClient can be used to
        // access Google APIs on behalf of the user.

        getGPlusUsername();
        getGPlusFriends();
        getGPlusAccessToken();
    }

    @Override
    public void onResult(People.LoadPeopleResult loadPeopleResult) {
        Log.d(TAG, "Result from People request:" + loadPeopleResult.getStatus());

        if (loadPeopleResult.getStatus().getStatusCode() == CommonStatusCodes.SUCCESS) {
            PersonBuffer personBuffer = loadPeopleResult.getPersonBuffer();
            try {
                int count = personBuffer.getCount();
                for (int i = 0; i < count; i++) {
                    Person person = personBuffer.get(i);
                    Log.d(TAG, "Display name: " + person.getDisplayName());
                    PlacesLoginUtils.getInstance().addEntryToUserIdMap(person.getId(), person.getDisplayName());
                    PlacesLoginUtils.getInstance().addFriend(person.getDisplayName());
                }
            } finally {
                personBuffer.close();
            }
        } else {
            Log.e(TAG, "Error requesting people data: " + loadPeopleResult.getStatus());
        }

    }

    public void getGPlusUsername() {
        Plus.PeopleApi.loadVisible(GPlusUtils.getInstance().getGoogleApiClient(), null).setResultCallback(this);

        Log.d(TAG, "Is mGoogleApiClient null in getPlusUsername? " + (GPlusUtils.getInstance().getGoogleApiClient() == null));
        Log.d(TAG, "Does getCurrentPerson return null? " + (Plus.PeopleApi.getCurrentPerson(GPlusUtils.getInstance().getGoogleApiClient()) == null));

        if (Plus.PeopleApi.getCurrentPerson(GPlusUtils.getInstance().getGoogleApiClient()) != null) {
            GPlusUtils.getInstance().setCurrentPerson(Plus.PeopleApi.getCurrentPerson(GPlusUtils.getInstance().getGoogleApiClient()));
            String personName = GPlusUtils.getInstance().getCurrentPerson().getDisplayName();
            String personId = GPlusUtils.getInstance().getCurrentPerson().getId();

            PlacesLoginUtils.getInstance().setCurrentUserId(personId);
            PlacesLoginUtils.getInstance().addEntryToUserIdMap(personId, personName);
        }
    }

    public void getGPlusFriends() {

        Log.d(TAG, "Is mGoogleApiClient null? " + (GPlusUtils.getInstance().getGoogleApiClient() == null));
        Log.d(TAG, "Is currentPerson null? " + (GPlusUtils.getInstance().getCurrentPerson() == null));

        if (GPlusUtils.getInstance().getGoogleApiClient() != null && GPlusUtils.getInstance().getCurrentPerson() != null)
            Plus.PeopleApi.loadVisible(GPlusUtils.getInstance().getGoogleApiClient(), GPlusUtils.getInstance().getCurrentPerson().getId()).setResultCallback(this);
        else {
            Toast.makeText(this, "Cannot retrieve G+ info, please login again.", Toast.LENGTH_LONG).show();
            Log.d(TAG, "G+ friends cannot be retrieved.");
        }
    }

    public void onConnectionSuspended(int cause) {
        GPlusUtils.getInstance().getGoogleApiClient().connect();
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        // if (requestCode == RC_SIGN_IN) {
        mIntentInProgress = false;

        if (GPlusUtils.getInstance().getGoogleApiClient() != null &&
                !GPlusUtils.getInstance().getGoogleApiClient().isConnecting()) {
            GPlusUtils.getInstance().getGoogleApiClient().connect();
        }
        // Required for making Facebook login work
        else super.onActivityResult(requestCode, responseCode, intent);
        // }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.gplus_sign_in_button) signinWithGPlus();
    }

    public void signinWithGPlus() {
        GPlusUtils.getInstance().setGoogleApiClient(new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .build());
        GPlusUtils.getInstance().getGoogleApiClient().connect();

        progressDialog = ProgressDialog.show(this, null,
                getString(com.parse.ui.R.string.com_parse_ui_progress_dialog_text), true, false);
    }

    private void getGPlusAccessToken() {
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String accessToken = null;
                try {
                    accessToken = GoogleAuthUtil.getToken(getApplicationContext(),
                            Plus.AccountApi.getAccountName(GPlusUtils.getInstance().getGoogleApiClient()),
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
            protected void onPostExecute(String token) {
                Log.i(TAG, "Google+ access token retrieved: " + token);

                completeLoginWithParse(token);
            }

        };

        task.execute();
    }

    private void completeLoginWithParse(final String token) {
        final String email = Plus.AccountApi.getAccountName(GPlusUtils.getInstance().getGoogleApiClient());
        final HashMap<String, Object> params = new HashMap();
        params.put("code", token);
        params.put("email", email);

        Log.d(TAG, "Calling cloud code for authentication...");

        //loads the Cloud function to create a Google user
        ParseCloud.callFunctionInBackground("accessGoogleUser", params, new FunctionCallback<Object>() {
            @Override
            public void done(final Object returnObj, ParseException e) {
                if (e == null) {
                    ParseUser.becomeInBackground(returnObj.toString(), new LogInCallback() {
                        public void done(ParseUser user, ParseException e) {
                            if (user != null && e == null) {
                                Log.i(TAG, "The Google user validated");

                                GPlusUtils.getInstance().setGoogleApiClient(GPlusUtils.getInstance().getGoogleApiClient());

                                /*
                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                                SharedPreferences.Editor prefsEditor = preferences.edit();
                                Gson gson = new Gson();
                                String json = gson.toJson(GPlusUtils.getInstance().getGoogleApiClient());
                                prefsEditor.putString("GoogleApiClient", json);
                                prefsEditor.commit();
                                */

                                if (progressDialog != null) progressDialog.dismiss();

                                setResult(RESULT_OK);
                                // finish();

                                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString(PlacesLoginUtils.GPLUS_TOKEN_SP, token).commit();
                                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString(PlacesLoginUtils.EMAIL_SP, email).commit();

                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(intent);
                            } else if (e != null) {
                                Toast.makeText(getApplicationContext(), "There was a problem creating your account.", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                                GPlusUtils.getInstance().getGoogleApiClient().disconnect();
                            } else Log.i(TAG, "The Google token could not be validated");
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(), "There was a problem creating your account.", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    GPlusUtils.getInstance().getGoogleApiClient().disconnect();
                }
            }
        });
    }
}
