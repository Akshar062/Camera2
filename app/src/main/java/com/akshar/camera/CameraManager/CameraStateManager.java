package com.akshar.camera.CameraManager;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.RggbChannelVector;
import android.view.Surface;

import com.akshar.camera.Converters.ColorTemperatureConverter;
import com.akshar.camera.Mode;

public class CameraStateManager {
    private static final float maximumPreviewExposureTime = 0.15f;
    private static final float maximumCaptureExposureTime = 30;
    public CameraState autoState;
    private Mode exposureMode;
    private Mode focusMode;
    private Mode colorCorrectionMode;
    private boolean faceDetection;
    private CameraState manualState;
    private int exposureCompensation;

    public CameraStateManager() {
        exposureMode = Mode.AUTO;
        focusMode = Mode.AUTO;
        colorCorrectionMode = Mode.AUTO;
        faceDetection = false;

        manualState = new CameraState(0.01f, 1, 800, ColorTemperatureConverter.kelvinToRgb(6600));
        autoState = new CameraState();
        exposureCompensation = 0;
    }

    void applyToRequestBuilder(CaptureRequest.Builder request, float maximumExposureTime) {
        if (exposureMode == Mode.MANUAL) {
            request.set(CaptureRequest.CONTROL_AE_MODE, 0);
            float expTime = manualState.exposureTime > maximumExposureTime ? maximumExposureTime : manualState.exposureTime;
            long nanoseconds = (long) (expTime * 1000000000);
            request.set(CaptureRequest.SENSOR_EXPOSURE_TIME, nanoseconds);
        }

        if (focusMode == Mode.MANUAL) {
            request.set(CaptureRequest.CONTROL_AF_MODE, 0);
            request.set(CaptureRequest.LENS_FOCUS_DISTANCE, manualState.focusDistance);
        }

        if (exposureMode == Mode.MANUAL) {
            int isoValue = (int) manualState.ISO;
            request.set(CaptureRequest.SENSOR_SENSITIVITY, isoValue);
        } else {
            request.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, exposureCompensation);
        }

        if (colorCorrectionMode == Mode.MANUAL) {
            request.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
            request.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
            request.set(CaptureRequest.COLOR_CORRECTION_GAINS, manualState.colorCorrection);
        }

        if (faceDetection)
            request.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE);
    }

    CaptureRequest getPreviewRequest(CameraDevice device, Surface surface) throws CameraAccessException {
        int type = CameraDevice.TEMPLATE_PREVIEW;

        // Ensure the CameraDevice is not null
        if (device == null) {
            throw new IllegalStateException("CameraDevice is null");
        }

        CaptureRequest.Builder captureRequestBuilder = device.createCaptureRequest(type);
        captureRequestBuilder.addTarget(surface);

        applyToRequestBuilder(captureRequestBuilder, maximumPreviewExposureTime);

        return captureRequestBuilder.build();
    }

    CaptureRequest getCaptureRequest(CameraDevice device, Surface surface) throws CameraAccessException {
        int type = CameraDevice.TEMPLATE_STILL_CAPTURE;

        // Ensure the CameraDevice is not null
        if (device == null) {
            throw new IllegalStateException("CameraDevice is null");
        }

        CaptureRequest.Builder captureRequestBuilder = device.createCaptureRequest(type);
        captureRequestBuilder.addTarget(surface);

        applyToRequestBuilder(captureRequestBuilder, maximumCaptureExposureTime);

        return captureRequestBuilder.build();
    }
    public void onChange() {
    }

    public void setExposureCompensation(int exposureCompensation) {
        this.exposureCompensation = exposureCompensation;
        onChange();
    }

    public void setISO(float ISO) {
        manualState.ISO = ISO;
        exposureMode = Mode.MANUAL;
        onChange();
    }

    public void setColorCorrection(RggbChannelVector colorCorrection) {
        manualState.colorCorrection = colorCorrection;
        colorCorrectionMode = Mode.MANUAL;
        onChange();
    }

    public float getFocusDistanceInMeters() {
        return 1 / manualState.focusDistance;
    }

    public void setFocusDistanceInMeters(float focusDistance) {
        manualState.focusDistance = 1 / focusDistance;
        focusMode = Mode.MANUAL;
        onChange();
    }

    public Mode getExposureMode() {
        return exposureMode;
    }

    public void setExposureMode(Mode exposureMode) {
        this.exposureMode = exposureMode;
        onChange();
    }

    public void setFocusMode(Mode focusMode) {
        this.focusMode = focusMode;
        onChange();
    }

    public void setColorCorrectionMode(Mode colorCorrectionMode) {
        this.colorCorrectionMode = colorCorrectionMode;
        onChange();
    }

    public Mode getFocusMode() {
        return focusMode;
    }

    public float getExposureTime() {
        return manualState.exposureTime;
    }

    public void setExposureTime(float exposureTime) {
        manualState.exposureTime = exposureTime;
        exposureMode = Mode.MANUAL;
        onChange();
    }

    public CameraState getManualState() {
        return manualState;
    }

    public void setManualState(CameraState manualState) {
        this.manualState = manualState;
        onChange();
    }
}
