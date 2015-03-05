package com.gcw.sapienza.places.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.model.GraphUser;
import com.gcw.sapienza.places.R;
import com.gcw.sapienza.places.activities.PlacesLoginActivity;
import com.google.android.gms.common.SignInButton;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseTwitterUtils;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.twitter.Twitter;
import com.parse.ui.ParseLoginConfig;
import com.parse.ui.ParseLoginFragment;
import com.parse.ui.ParseOnLoadingListener;
import com.parse.ui.ParseOnLoginSuccessListener;

/**
 * Created by mic_head on 27/02/15.
 */
public class PlacesLoginFragment extends ParseLoginFragment {

    private static final String TAG = "PlacesLoginFragment";

    private static final String USER_OBJECT_NAME_FIELD = "name";

    private View parseLogin;
    private EditText usernameField;
    private EditText passwordField;
    private TextView parseLoginHelpButton;
    private Button parseLoginButton;
    private Button parseSignupButton;
    private Button facebookLoginButton;
    private Button twitterLoginButton;
    private SignInButton gPlusSigninButton;
    private ParseLoginFragmentListener loginFragmentListener;
    private ParseOnLoginSuccessListener onLoginSuccessListener;

    private boolean canChoose;

    private ParseLoginConfig config;

    public static PlacesLoginFragment newInstance(Bundle configOptions) {
        PlacesLoginFragment loginFragment = new PlacesLoginFragment();
        loginFragment.setArguments(configOptions);
        return loginFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle savedInstanceState) {

        View v = null;

        Log.d(TAG, "canChoose: " + canChoose);

        if(!canChoose) v = inflater.inflate(R.layout.blank_screen, parent, false);
        else
        {
            config = ParseLoginConfig.fromBundle(getArguments(), getActivity());

            v = inflater.inflate(R.layout.places_login_fragment, parent, false);
            ImageView appLogo = (ImageView) v.findViewById(R.id.app_logo);
            parseLogin = v.findViewById(R.id.parse_login);
            usernameField = (EditText) v.findViewById(R.id.login_username_input);
            passwordField = (EditText) v.findViewById(R.id.login_password_input);
            parseLoginHelpButton = (Button) v.findViewById(R.id.parse_login_help);
            parseLoginButton = (Button) v.findViewById(R.id.parse_login_button);
            parseSignupButton = (Button) v.findViewById(R.id.parse_signup_button);
            facebookLoginButton = (Button) v.findViewById(R.id.facebook_login);
            twitterLoginButton = (Button) v.findViewById(R.id.twitter_login);
            gPlusSigninButton = (SignInButton) v.findViewById(R.id.gplus_sign_in_button);
            gPlusSigninButton.setColorScheme(SignInButton.COLOR_LIGHT);
            gPlusSigninButton.setSize(SignInButton.SIZE_ICON_ONLY);

            gPlusSigninButton.setOnClickListener((View.OnClickListener) getActivity());

            if (appLogo != null && config.getAppLogo() != null) {
                appLogo.setImageResource(config.getAppLogo());
            }
            if (allowParseLoginAndSignup()) {
                setUpParseLoginAndSignup();
            }
            if (allowFacebookLogin()) {
                setUpFacebookLogin();
            }
            if (allowTwitterLogin()) {
                setUpTwitterLogin();
            }
        }

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.canChoose = ((PlacesLoginActivity)activity).canChoose;

        if (activity instanceof ParseLoginFragmentListener) {
            loginFragmentListener = (ParseLoginFragmentListener) activity;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implemement ParseLoginFragmentListener");
        }

        if (activity instanceof ParseOnLoginSuccessListener) {
            onLoginSuccessListener = (ParseOnLoginSuccessListener) activity;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implemement ParseOnLoginSuccessListener");
        }

        if (activity instanceof ParseOnLoadingListener) {
            onLoadingListener = (ParseOnLoadingListener) activity;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implemement ParseOnLoadingListener");
        }
    }

    private void setUpParseLoginAndSignup() {
        parseLogin.setVisibility(View.VISIBLE);

        if (config.isParseLoginEmailAsUsername()) {
            usernameField.setHint(com.parse.ui.R.string.com_parse_ui_email_input_hint);
            usernameField.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        }

        if (config.getParseLoginButtonText() != null) {
            parseLoginButton.setText(config.getParseLoginButtonText());
        }

        parseLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameField.getText().toString();
                String password = passwordField.getText().toString();

                if (username.length() == 0) {
                    showToast(com.parse.ui.R.string.com_parse_ui_no_username_toast);
                } else if (password.length() == 0) {
                    showToast(com.parse.ui.R.string.com_parse_ui_no_password_toast);
                } else {
                    loadingStart(true);
                    ParseUser.logInInBackground(username, password, new LogInCallback() {
                        @Override
                        public void done(ParseUser user, ParseException e) {
                            if (isActivityDestroyed()) {
                                return;
                            }

                            if (user != null) {
                                loadingFinish();
                                loginSuccess();
                            } else {
                                loadingFinish();
                                if (e != null) {
                                    debugLog(getString(com.parse.ui.R.string.com_parse_ui_login_warning_parse_login_failed) +
                                            e.toString());
                                    if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {
                                        if (config.getParseLoginInvalidCredentialsToastText() != null) {
                                            showToast(config.getParseLoginInvalidCredentialsToastText());
                                        } else {
                                            showToast(com.parse.ui.R.string.com_parse_ui_parse_login_invalid_credentials_toast);
                                        }
                                        passwordField.selectAll();
                                        passwordField.requestFocus();
                                    } else {
                                        showToast(com.parse.ui.R.string.com_parse_ui_parse_login_failed_unknown_toast);
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });

        if (config.getParseSignupButtonText() != null) {
            parseSignupButton.setText(config.getParseSignupButtonText());
        }

        parseSignupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameField.getText().toString();
                String password = passwordField.getText().toString();

                loginFragmentListener.onSignUpClicked(username, password);
            }
        });

        if (config.getParseLoginHelpText() != null) {
            parseLoginHelpButton.setText(config.getParseLoginHelpText());
        }

        parseLoginHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginFragmentListener.onLoginHelpClicked();
            }
        });
    }

    private void setUpFacebookLogin() {
        facebookLoginButton.setVisibility(View.VISIBLE);

        if (config.getFacebookLoginButtonText() != null) {
            facebookLoginButton.setText(config.getFacebookLoginButtonText());
        }

        facebookLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingStart(true);
                ParseFacebookUtils.logIn(config.getFacebookLoginPermissions(),
                        getActivity(), new LogInCallback() {
                            @Override
                            public void done(ParseUser user, ParseException e) {
                                if (isActivityDestroyed()) {
                                    return;
                                }

                                if (user == null) {
                                    loadingFinish();
                                    if (e != null) {
                                        showToast(com.parse.ui.R.string.com_parse_ui_facebook_login_failed_toast);
                                        debugLog(getString(com.parse.ui.R.string.com_parse_ui_login_warning_facebook_login_failed) +
                                                e.toString());
                                    }
                                } else if (user.isNew()) {
                                    Request.newMeRequest(ParseFacebookUtils.getSession(),
                                            new Request.GraphUserCallback() {
                                                @Override
                                                public void onCompleted(GraphUser fbUser,
                                                                        Response response) {
                      /*
                        If we were able to successfully retrieve the Facebook
                        user's name, let's set it on the fullName field.
                      */
                                                    ParseUser parseUser = ParseUser.getCurrentUser();
                                                    if (fbUser != null && parseUser != null
                                                            && fbUser.getName().length() > 0) {
                                                        parseUser.put(USER_OBJECT_NAME_FIELD, fbUser.getName());
                                                        parseUser.saveInBackground(new SaveCallback() {
                                                            @Override
                                                            public void done(ParseException e) {
                                                                if (e != null) {
                                                                    debugLog(getString(
                                                                            com.parse.ui.R.string.com_parse_ui_login_warning_facebook_login_user_update_failed) +
                                                                            e.toString());
                                                                }
                                                                loginSuccess();
                                                            }
                                                        });
                                                    }
                                                    loginSuccess();
                                                }
                                            }
                                    ).executeAsync();
                                } else {
                                    loginSuccess();
                                }
                            }
                        });
            }
        });
    }

    private void setUpTwitterLogin() {
        twitterLoginButton.setVisibility(View.VISIBLE);

        if (config.getTwitterLoginButtonText() != null) {
            twitterLoginButton.setText(config.getTwitterLoginButtonText());
        }

        twitterLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingStart(false); // Twitter login pop-up already has a spinner
                ParseTwitterUtils.logIn(getActivity(), new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException e) {
                        if (isActivityDestroyed()) {
                            return;
                        }

                        if (user == null) {
                            loadingFinish();
                            if (e != null) {
                                showToast(com.parse.ui.R.string.com_parse_ui_twitter_login_failed_toast);
                                debugLog(getString(com.parse.ui.R.string.com_parse_ui_login_warning_twitter_login_failed) +
                                        e.toString());
                            }
                        } else if (user.isNew()) {
                            Twitter twitterUser = ParseTwitterUtils.getTwitter();
                            if (twitterUser != null
                                    && twitterUser.getScreenName().length() > 0) {
                /*
                  To keep this example simple, we put the users' Twitter screen name
                  into the name field of the Parse user object. If you want the user's
                  real name instead, you can implement additional calls to the
                  Twitter API to fetch it.
                */
                                user.put(USER_OBJECT_NAME_FIELD, twitterUser.getScreenName());
                                user.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e != null) {
                                            debugLog(getString(
                                                    com.parse.ui.R.string.com_parse_ui_login_warning_twitter_login_user_update_failed) +
                                                    e.toString());
                                        }
                                        loginSuccess();
                                    }
                                });
                            }
                        } else {
                            loginSuccess();
                        }
                    }
                });
            }
        });
    }

    private boolean allowParseLoginAndSignup() {
        if (!config.isParseLoginEnabled()) {
            return false;
        }

        if (usernameField == null) {
            debugLog(com.parse.ui.R.string.com_parse_ui_login_warning_layout_missing_username_field);
        }
        if (passwordField == null) {
            debugLog(com.parse.ui.R.string.com_parse_ui_login_warning_layout_missing_password_field);
        }
        if (parseLoginButton == null) {
            debugLog(com.parse.ui.R.string.com_parse_ui_login_warning_layout_missing_login_button);
        }
        if (parseSignupButton == null) {
            debugLog(com.parse.ui.R.string.com_parse_ui_login_warning_layout_missing_signup_button);
        }
        if (parseLoginHelpButton == null) {
            debugLog(com.parse.ui.R.string.com_parse_ui_login_warning_layout_missing_login_help_button);
        }

        boolean result = (usernameField != null) && (passwordField != null)
                && (parseLoginButton != null) && (parseSignupButton != null)
                && (parseLoginHelpButton != null);

        if (!result) {
            debugLog(com.parse.ui.R.string.com_parse_ui_login_warning_disabled_username_password_login);
        }
        return result;
    }

    private boolean allowFacebookLogin() {
        if (!config.isFacebookLoginEnabled()) {
            return false;
        }

        if (facebookLoginButton == null) {
            debugLog(com.parse.ui.R.string.com_parse_ui_login_warning_disabled_facebook_login);
            return false;
        } else {
            return true;
        }
    }

    private boolean allowTwitterLogin() {
        if (!config.isTwitterLoginEnabled()) {
            return false;
        }

        if (twitterLoginButton == null) {
            debugLog(com.parse.ui.R.string.com_parse_ui_login_warning_disabled_twitter_login);
            return false;
        } else {
            return true;
        }
    }

    private void loginSuccess() {
        onLoginSuccessListener.onLoginSuccess();
    }
}
