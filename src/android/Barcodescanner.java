package cn.wishpost.wishwms.wmsbarcode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.device.scanner.configuration.PropertyID;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Vibrator;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * This class echoes a string called from JavaScript.
 */
public class Barcodescanner extends CordovaPlugin {

    private static String TAG = Barcodescanner.class.getSimpleName();

    private Vibrator mVibrator;
    private ScanManager mScanManager;
    private SoundPool soundpool = null;
    private int soundid;

    private final static String SCAN_ACTION = ScanManager.ACTION_DECODE;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mVibrator = (Vibrator) cordova.getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        Log.d(TAG, "initialize");
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        initScan();
        Start();
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        Stop();
    }

    private void fireEvent(final String eventName, final Object data) {

        cordova.getActivity().runOnUiThread(() -> {
            String method;

            if (data == null) {
                method = String.format("javascript:cordova.plugins.wmsbarcode.fireEvent( '%s', null );", eventName);
            } else if (data instanceof JSONObject) {
                method = String.format("javascript:cordova.plugins.wmsbarcode.fireEvent( '%s', %s );", eventName, data.toString());
            } else {
                method = String.format("javascript:cordova.plugins.wmsbarcode.fireEvent( '%s', '%s' );", eventName, data.toString());
            }
            Barcodescanner.this.webView.loadUrl(method);
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "coolMethod":
                String message = args.getString(0);
                this.coolMethod(message, callbackContext);
                return true;
            case "startScan":
                this.Start();
                return true;
            case "stopScan":
                this.Stop();
                return true;
        }
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private BroadcastReceiver mScanReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            soundpool.play(soundid, 1, 1, 0, 0, 1);
            mVibrator.vibrate(100);

            byte[] barcode = intent.getByteArrayExtra(ScanManager.DECODE_DATA_TAG);
            int barcodelen = intent.getIntExtra(ScanManager.BARCODE_LENGTH_TAG, 0);
            byte temp = intent.getByteExtra(ScanManager.BARCODE_TYPE_TAG, (byte) 0);
            Log.d(TAG, "----codetype--" + temp + "----code--" + Arrays.toString(barcode));
            String barcodeStr = new String(barcode, 0, barcodelen);

            JSONObject res = new JSONObject();
            try {

                if (barcodelen == 0) {

                    res.put("success", false);

                    res.put("data", "");
                } else {
                    res.put("success", true);
                    res.put("data", barcodeStr);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            fireEvent("didScan", res);
        }
    };

    private void initScan() {
        if (mScanManager != null) return;
        mScanManager = new ScanManager();
        mScanManager.openScanner();

        mScanManager.switchOutputMode(0);
        soundpool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 100);
        soundid = soundpool.load("/etc/Scan_new.ogg", 1);
    }

    private void Stop() {
        if (mScanManager != null) {
            mScanManager.stopDecode();
        }
        try {
            cordova.getActivity().unregisterReceiver(mScanReceiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void Start() {
        try {
            IntentFilter filter = new IntentFilter();
            int[] idbuf = new int[]{PropertyID.WEDGE_INTENT_ACTION_NAME, PropertyID.WEDGE_INTENT_DATA_STRING_TAG};
            String[] value_buf = mScanManager.getParameterString(idbuf);
            if (value_buf != null && value_buf[0] != null && !value_buf[0].equals("")) {
                filter.addAction(value_buf[0]);
            } else {
                filter.addAction(SCAN_ACTION);
            }
            cordova.getActivity().registerReceiver(mScanReceiver, filter);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

}
