package com.akshar.camera.Sliders;

import android.content.Context;

import com.akshar.camera.CameraManager.Camera;
import com.akshar.camera.Converters.ExposureTimeConverter;

/**
 * Gemaakt door ruurd op 11-3-2017.
 */

public class ExposureSlider extends CameraStringSlider {
    public ExposureSlider(Context context, Camera camera) {
        super(context, camera, ExposureTimeConverter.exposureTimeFractions);
    }

    public void applyToCamera(String value) {
        float time = stringToValue(value);
        camera.state.setExposureTime(time);
    }
}
