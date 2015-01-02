package com.gcw.sapienza.places.remotesettings;

/**
 * Interface for handling RemoteSettings configuration callbacks
 */
public interface RemoteSettingsCallBacks {
    /**
     * fired when the configuration is read from an URL
     */
    public void onRemoteConfig();

    /**
     * Fired when an error occurs
     * @param error a string representing the error
     */
    public void onError(String error);
}
