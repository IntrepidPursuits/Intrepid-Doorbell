package io.intrepid.intrepiddoorbell;

import android.hardware.Camera;
import android.os.Handler;

public class OldLedFlasher implements LedFlasher {
    private Handler handler = new Handler();

    @Override
    public void flashLed(long durationMillis) {
        //blah, blah deprecated blah, blah
        final Camera camera = Camera.open();
        Camera.Parameters p = camera.getParameters();
        p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        camera.setParameters(p);
        camera.startPreview();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                camera.stopPreview();
                camera.release();
            }
        }, durationMillis);
    }
}
