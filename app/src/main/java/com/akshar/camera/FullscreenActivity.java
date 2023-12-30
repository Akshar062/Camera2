package com.akshar.camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.akshar.camera.Buttons.Button;
import com.akshar.camera.Buttons.ButtonManager;
import com.akshar.camera.CameraManager.Camera;
import com.akshar.camera.CameraManager.ImageSaver;
import com.akshar.camera.Converters.ColorTemperatureConverter;
import com.akshar.camera.Converters.ExposureTimeConverter;
import com.akshar.camera.Sliders.CameraValueSlider;
import com.akshar.camera.Sliders.ColorCorrectionSlider;
import com.akshar.camera.Sliders.ExposureCompensationSlider;
import com.akshar.camera.Sliders.ExposureSlider;
import com.akshar.camera.Sliders.FocusSlider;
import com.akshar.camera.Sliders.ISOSlider;

import java.io.File;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class FullscreenActivity extends AppCompatActivity {
    private final int infoUpdateDelay = 500;
    CameraValueSlider[] sliders;
    FrameLayout sliderLayout;
    ButtonManager buttonManager;
    Camera camera;
    private TextView whiteBalanceInfo;
    private TextView focusInfo;
    private TextView isoInfo;
    private TextView shutterInfo;
    private TextView apertureInfo;
    private ImageView galleryButton;

    @Override
    protected void onResume() {
        super.onResume();
        ImageSaver.startBackgroundThread();
        try {
            if (camera != null)
                camera.open();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        if (camera != null)
            camera.close();
        ImageSaver.stopBackgroundThread();
        super.onPause();
    }

    private void initialize() {
        sliders = createSliders();
        sliderLayout = (FrameLayout) findViewById(R.id.valueSlider);
        buttonManager = new ButtonManager(this, sliders, sliderLayout) {
            @Override
            public void unlockAe() {
                super.unlockAe();
                if (camera.isReady())
                    camera.state.setExposureMode(Mode.AUTO);
            }

            @Override
            public void lockAe() {
                super.lockAe();
                if (camera.isReady()) {
                    camera.state.setManualState(camera.state.autoState);
                    camera.state.setExposureMode(Mode.MANUAL);
                }
            }
        };

        ImageButton autoButton = (ImageButton) findViewById(R.id.autoButton);
        autoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (camera.isReady()) {
                    Button activeButton = buttonManager.getActiveSliderButton();
                    if (activeButton.getInactiveResourceId() == R.mipmap.mfbutton) {
                        camera.state.setFocusMode(Mode.AUTO);
                    } else if (activeButton.getInactiveResourceId() == R.mipmap.wbbutton) {
                        camera.state.setColorCorrectionMode(Mode.AUTO);
                    }
                }
                buttonManager.deactivateAllSliderButtons();
            }
        });

        whiteBalanceInfo = findViewById(R.id.wbInfo);
        focusInfo = findViewById(R.id.focusInfo);
        isoInfo = findViewById(R.id.isoInfo);
        shutterInfo = findViewById(R.id.expInfo);
        apertureInfo = findViewById(R.id.apertureInfo);

        whiteBalanceInfo.setShadowLayer(3, 1, 1, Color.BLACK);
        focusInfo.setShadowLayer(3, 1, 1, Color.BLACK);
        isoInfo.setShadowLayer(3, 1, 1, Color.BLACK);
        shutterInfo.setShadowLayer(3, 1, 1, Color.BLACK);
        apertureInfo.setShadowLayer(3, 1, 1, Color.BLACK);

        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                int k = ColorTemperatureConverter.rgbNormalizedToKelvin(camera.state.autoState.colorCorrection);
                final int kelvin = Math.round(k / 100) * 100;
                final int focusDistance = (int) (100 / camera.state.autoState.focusDistance);
                final int iso = Math.round(camera.state.autoState.ISO / 50) * 50;
                final String expTime = ExposureTimeConverter.secondsToFraction(camera.state.autoState.exposureTime);
                final float aperture = camera.state.autoState.aperture;

                runOnUiThread(() -> {
                    whiteBalanceInfo.setText(kelvin + "K");
                    focusInfo.setText(focusDistance + "cm");
                    isoInfo.setText("ISO" + iso);
                    shutterInfo.setText(expTime + "s");
                    apertureInfo.setText("F" + aperture);
                });
            }
        }, 0, infoUpdateDelay);

        setOnClickListeners();
    }

    public CameraValueSlider[] createSliders() {
        return new CameraValueSlider[]{
                new ColorCorrectionSlider(this, camera),
                new FocusSlider(this, camera),
                new ExposureCompensationSlider(this, camera),
                new ISOSlider(this, camera),
                new ExposureSlider(this, camera),
        };
    }

    void setOnClickListeners() {
        //Capture Button
        ImageButton captureButton = findViewById(R.id.capture);
        captureButton.setOnClickListener(view -> {
            if (camera.isReady()) {
                camera.captureStillImage();
            }
        });

        galleryButton = findViewById(R.id.galleryButton);
        updateGalleryButton();

        galleryButton.setOnClickListener(v -> {
            Log.d("DEBUG", "OPEN PHOTOS NOW");
            if (ImageSaver.lastTakenImage != null) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                String path = ImageSaver.lastTakenImage.getAbsolutePath();
                intent.setDataAndType(Uri.parse(path), "image/*");
                startActivity(intent);
            }
            String last = getLatestImage();
            Log.d("DEBUG", last);
        });
    }

    void updateGalleryButton() {
        Bitmap bmp = BitmapFactory.decodeFile(getLatestImage());
        galleryButton.setImageBitmap(bmp);
    }

    String getLatestImage() {
        File cameraFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");

        if (cameraFolder.exists()) {
            File[] files = cameraFolder.listFiles();

            if (files != null && files.length > 0) {
                File last = files[files.length - 1];
                return last.getAbsolutePath();
            } else {
                // Handle the case where no files are found
                return ""; // or throw an exception, show a message, etc.
            }
        } else {
            // Handle the case where the folder doesn't exist
            return ""; // or throw an exception, show a message, etc.
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        SurfaceHolder holder = surfaceView.getHolder();
        final Activity context = this;

        SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                camera = new Camera(context, holder.getSurface()) {
                    @Override
                    public void onCaptureDone() {
                        super.onCaptureDone();
                        Timer t = new Timer();
                        t.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateGalleryButton();
                                    }
                                });
                            }
                        }, 200);
                    }
                };
                initialize();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        };

        holder.addCallback(callback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Objects.equals(permissions[0], "android.permission.CAMERA")) {
            try {
                camera.permissionGranted();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
