package io.intrepid.intrepiddoorbell;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    //minimum time between network calls, in milliseconds
    private static final int DELAY_MILLIS = 5000;

    //time the camera flash will stay on, in milliseconds
    private static final int FLASH_ON_TIME = 1000;

    private NfcAdapter nfcAdapter;
    private long timeOfLastRequest;
    private LedFlasher ledFlasher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ledFlasher = (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M) ? new NewLedFlasher(this) : new OldLedFlasher();
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
                    onNfcScanned();
                }
                break;
        }
    }

    private boolean enoughTimeHasPassed() {
        return System.currentTimeMillis() - timeOfLastRequest > DELAY_MILLIS;
    }

    private void onNfcScanned() {
        timeOfLastRequest = System.currentTimeMillis();
        // TODO Actually do something cool here - a web request, perhaps? Then...
        ledFlasher.flashLed(FLASH_ON_TIME);
    }
}
