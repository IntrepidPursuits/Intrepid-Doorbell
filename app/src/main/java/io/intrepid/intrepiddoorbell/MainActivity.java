package io.intrepid.intrepiddoorbell;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    //minimum time between network calls, in milliseconds
    private static final int DELAY_MILLIS = 5000;

    //time the camera flash will stay on, in milliseconds
    private static final int FLASH_ON_TIME = 1000;

    private NfcAdapter nfcAdapter;
    private long timeOfLastRequest;
    private Handler handler = new Handler();
    private String cameraWithFlashId;
    private CameraManager cameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeCameraManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        //start NFC here, when app is foregrounded
        initNfc();
    }

    @Override
    public void onPause() {
        super.onPause();
        // kill NFC as app is backgrounded
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    //this whole method was ripped pretty much directly from: http://goo.gl/upS8b1
    private void initNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());

        PendingIntent mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(
                this, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndef2 = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter ndef3 = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);

        try {
            ndef2.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Failed to initialize NFC", e);
        }

        IntentFilter[] mFilters = new IntentFilter[] { ndef, ndef2, ndef3 };

        String[][] mTechLists = new String[][] { new String[] {
                android.nfc.tech.NfcA.class.getName(),
                android.nfc.tech.IsoDep.class.getName() } };

        nfcAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        switch (intent.getAction()) {
            case "android.nfc.action.TAG_DISCOVERED":
            case "android.nfc.action.NDEF_DISCOVERED":
            case "android.nfc.action.TECH_DISCOVERED":
                // Will be called when NFC is tapped, but only if we haven't made a call recently
                if (enoughTimeHasPassed()) {
                    post();
                }
                break;
        }
    }

    private boolean enoughTimeHasPassed() {
        return System.currentTimeMillis() - timeOfLastRequest > DELAY_MILLIS;
    }

    private void setCameraTorchMode(boolean enabled) {
        try {
            cameraManager.setTorchMode(cameraWithFlashId, enabled);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to set camera torch mode.", e);
            onErrorLoadingCameraFlash();
        }
    }

    // you may have to use this deprecated version on older devices
//    private void notifyUserWithFlash() {
//        //blah, blah deprecated blah, blah
//        final Camera camera = Camera.open();
//        Camera.Parameters p = camera.getParameters();
//        p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
//        camera.setParameters(p);
//        camera.startPreview();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                camera.stopPreview();
//                camera.release();
//            }
//        }, FLASH_ON_TIME);
//    }

    // turn flash on for FLASH_ON_TIME, then back off
    private void notifyUserWithFlash() {
        if (TextUtils.isEmpty(cameraWithFlashId)) {
            onErrorLoadingCameraFlash();
        } else {
            setCameraTorchMode(true);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setCameraTorchMode(false);
                }
            }, FLASH_ON_TIME);
        }
    }

    // this method is probably not necessary on older devices using the Camera API's, see above
    private void initializeCameraManager() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
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

    private void onErrorLoadingCameraFlash() {
        Toast.makeText(this, R.string.error_no_camera, Toast.LENGTH_SHORT).show();
    }

    private void post() {
        timeOfLastRequest = System.currentTimeMillis();
        // TODO Actually do something cool here - a web request, perhaps? Then...
        notifyUserWithFlash();
    }
}
