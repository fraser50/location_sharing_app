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
}
