package com.rathiesh.deckofcards.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

/**
 * Created for BLE Peripheral functionality
 * Manages BLE advertisement for the card deck peripheral
 */
public class BleAdvertisementManager {
    
    private static final String TAG = "BleAdvertisementManager";
    
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private AdvertiseCallback advertiseCallback;
    private boolean isAdvertising = false;
    private BleAdvertisementListener listener;
    
    public interface BleAdvertisementListener {
        void onAdvertisementStarted();
        void onAdvertisementStopped();
        void onAdvertisementFailed(int errorCode, String errorMessage);
    }
    
    public BleAdvertisementManager(Context context, BluetoothAdapter bluetoothAdapter) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        if (bluetoothAdapter != null) {
            this.bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        }
        initializeAdvertiseCallback();
    }
    
    public void setListener(BleAdvertisementListener listener) {
        this.listener = listener;
    }
    
    private void initializeAdvertiseCallback() {
        advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d(TAG, "Advertisement started successfully");
                isAdvertising = true;
                if (listener != null) {
                    listener.onAdvertisementStarted();
                }
            }
            
            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                String errorMessage = getAdvertiseErrorMessage(errorCode);
                Log.e(TAG, "Advertisement failed to start: " + errorMessage);
                isAdvertising = false;
                if (listener != null) {
                    listener.onAdvertisementFailed(errorCode, errorMessage);
                }
            }
        };
    }
    
    /**
     * Start BLE advertisement
     * @return true if advertisement started successfully, false otherwise
     */
    public boolean startAdvertising() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth adapter is null or not enabled");
            if (listener != null) {
                listener.onAdvertisementFailed(BleConstants.ERROR_BLUETOOTH_NOT_ENABLED, 
                    "Bluetooth is not enabled");
            }
            return false;
        }
        
        if (bluetoothLeAdvertiser == null) {
            Log.e(TAG, "BLE advertiser is not available");
            if (listener != null) {
                listener.onAdvertisementFailed(BleConstants.ERROR_BLE_NOT_SUPPORTED, 
                    "BLE advertising is not supported");
            }
            return false;
        }
        
        if (isAdvertising) {
            Log.w(TAG, "Already advertising");
            return true;
        }
        
        AdvertiseSettings settings = buildAdvertiseSettings();
        AdvertiseData data = buildAdvertiseData();
        
        try {
            bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback);
            Log.d(TAG, "Starting advertisement...");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start advertising", e);
            if (listener != null) {
                listener.onAdvertisementFailed(BleConstants.ERROR_ADVERTISEMENT_FAILED, 
                    "Failed to start advertising: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Stop BLE advertisement
     */
    public void stopAdvertising() {
        if (bluetoothLeAdvertiser != null && isAdvertising) {
            try {
                bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
                Log.d(TAG, "Advertisement stopped");
                isAdvertising = false;
                if (listener != null) {
                    listener.onAdvertisementStopped();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to stop advertising", e);
            }
        }
    }
    
    /**
     * Check if currently advertising
     * @return true if advertising, false otherwise
     */
    public boolean isAdvertising() {
        return isAdvertising;
    }
    
    private AdvertiseSettings buildAdvertiseSettings() {
        return new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .setTimeout(BleConstants.ADVERTISEMENT_TIMEOUT_MS)
                .build();
    }
    
    private AdvertiseData buildAdvertiseData() {
        return new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(new ParcelUuid(BleConstants.CARD_DECK_SERVICE_UUID))
                .build();
    }
    
    private String getAdvertiseErrorMessage(int errorCode) {
        switch (errorCode) {
            case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                return "Advertisement already started";
            case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                return "Advertisement data too large";
            case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                return "Advertisement feature unsupported";
            case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                return "Advertisement internal error";
            case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                return "Too many advertisers";
            default:
                return "Unknown error: " + errorCode;
        }
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        stopAdvertising();
        listener = null;
    }
}

