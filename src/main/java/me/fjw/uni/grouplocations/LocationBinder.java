package me.fjw.uni.grouplocations;

import android.os.Binder;
import android.webkit.JavascriptInterface;

public class LocationBinder extends Binder {
    private LocationService service;

    public LocationBinder(LocationService service) {
        this.service = service;
    }

    @JavascriptInterface
    public boolean isLocationServicesEnabled() {
        return service.isLocationServicesEnabled();
    }

    @JavascriptInterface
    public void setLocationServicesEnabled(boolean locationServicesEnabled) {
        service.setLocationServicesEnabled(locationServicesEnabled);
    }

    @JavascriptInterface
    public boolean isExtendedTracking() {
        return service.isExtendedTracking();
    }

    @JavascriptInterface
    public void setExtendedTracking(boolean extendedTracking) {
        service.setExtendedTracking(extendedTracking);
    }

    public void sendWS(String message) {
        if (service.getClient() != null) {
            service.getClient().send(message);
        }
    }

    /**
     * Set the callback that is invoked when non-auth messages are received.
     * @param r The runnable to call when a message is received, worth noting: the callback will run on the service thread, i.e. it should return as soon as possible (perform long-running work on another thread)
     */
    public void setReceiveCallback(Runnable r) {
        service.setReceiveCallback(r);
    }

    public LocationService getService() {
        return service;
    }
}
