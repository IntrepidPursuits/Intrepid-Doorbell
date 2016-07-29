package io.intrepid.intrepiddoorbell;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.M)
public class NewLedFlasher implements LedFlasher {
    private static final String TAG = "NewLedFlasher";
    private Handler handler = new Handler();
    private final Context context;
    private CameraManager cameraManager;
    private String cameraWithFlashId;

    public NewLedFlasher(Context context) {
        this.context = context;
        initializeCameraManager();
    }

    private void initializeCameraManager() {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String currentCameraId : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(currentCameraId);
                Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (hasFlash != null && hasFlash) {
                    cameraWithFlashId = currentCameraId;
                    break;
                }
            }
            if (TextUtils.isEmpty(cameraWithFlashId)) {
                onErrorLoadingCameraFlash();
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to initiate camera flash", e);
            onErrorLoadingCameraFlash();
        }
    }

    private void setCameraTorchMode(boolean enabled) {
        try {
            cameraManager.setTorchMode(cameraWithFlashId, enabled);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to set camera torch mode.", e);
            onErrorLoadingCameraFlash();
        }
    }

    private void onErrorLoadingCameraFlash() {
        Toast.makeText(context, R.string.error_no_camera, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void flashLed(long durationMillis) {
        if (TextUtils.isEmpty(cameraWithFlashId)) {
            onErrorLoadingCameraFlash();
        } else {
            setCameraTorchMode(true);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setCameraTorchMode(false);
                }
            }, durationMillis);
        }
    }
}
