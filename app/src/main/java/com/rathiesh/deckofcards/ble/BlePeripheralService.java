package com.rathiesh.deckofcards.ble;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.rathiesh.deckofcards.R;
import com.rathiesh.deckofcards.activities.BlePeripheralActivity;

/**
 * Created for BLE Peripheral functionality
 * Service that manages BLE peripheral operations in the background
 */
public class BlePeripheralService extends Service implements 
        BleAdvertisementManager.BleAdvertisementListener,
        BleGattServerManager.BleGattServerListener {
    
    private static final String TAG = "BlePeripheralService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "BLE_PERIPHERAL_CHANNEL";
    private static final long CARD_UPDATE_INTERVAL = 5000; // 5 seconds
    
    // Service states
    public static final String ACTION_START_PERIPHERAL = "START_PERIPHERAL";
    public static final String ACTION_STOP_PERIPHERAL = "STOP_PERIPHERAL";
    public static final String ACTION_UPDATE_CARD_DATA = "UPDATE_CARD_DATA";
    
    // Broadcast actions
    public static final String BROADCAST_PERIPHERAL_STATE_CHANGED = "PERIPHERAL_STATE_CHANGED";
    public static final String BROADCAST_DEVICE_CONNECTED = "DEVICE_CONNECTED";
    public static final String BROADCAST_DEVICE_DISCONNECTED = "DEVICE_DISCONNECTED";
    public static final String BROADCAST_ERROR = "PERIPHERAL_ERROR";
    
    // Extra keys
    public static final String EXTRA_STATE = "state";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final String EXTRA_DEVICE_NAME = "device_name";
    public static final String EXTRA_ERROR_MESSAGE = "error_message";
    public static final String EXTRA_CONNECTED_COUNT = "connected_count";
    
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BleAdvertisementManager advertisementManager;
    private BleGattServerManager gattServerManager;
    private Handler cardUpdateHandler;
    private Runnable cardUpdateRunnable;
    
    private boolean isPeripheralActive = false;
    private int connectedDeviceCount = 0;
    
    // Binder for activity communication
    private final IBinder binder = new LocalBinder();
    
    public class LocalBinder extends Binder {
        public BlePeripheralService getService() {
            return BlePeripheralService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        initializeBluetooth();
        createNotificationChannel();
        setupCardUpdateHandler();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_PERIPHERAL.equals(action)) {
                startPeripheral();
            } else if (ACTION_STOP_PERIPHERAL.equals(action)) {
                stopPeripheral();
            } else if (ACTION_UPDATE_CARD_DATA.equals(action)) {
                updateCardData();
            }
        }
        
        return START_STICKY; // Restart service if killed
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        stopPeripheral();
        cleanup();
    }
    
    private void initializeBluetooth() {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        
        if (bluetoothAdapter != null) {
            advertisementManager = new BleAdvertisementManager(this, bluetoothAdapter);
            advertisementManager.setListener(this);
        }
        
        gattServerManager = new BleGattServerManager(this);
        gattServerManager.setListener(this);
    }
    
    private void setupCardUpdateHandler() {
        cardUpdateHandler = new Handler();
        cardUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPeripheralActive && gattServerManager != null) {
                    gattServerManager.updateCardData();
                    cardUpdateHandler.postDelayed(this, CARD_UPDATE_INTERVAL);
                }
            }
        };
    }
    
    /**
     * Start BLE peripheral mode
     */
    public void startPeripheral() {
        Log.d(TAG, "Starting BLE peripheral");
        
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            broadcastError("Bluetooth is not enabled");
            return;
        }
        
        if (isPeripheralActive) {
            Log.w(TAG, "Peripheral already active");
            return;
        }
        
        // Start GATT server first
        if (gattServerManager.startServer()) {
            // Then start advertising
            if (advertisementManager.startAdvertising()) {
                isPeripheralActive = true;
                startForeground(NOTIFICATION_ID, createNotification());
                startCardUpdates();
                broadcastStateChange(true);
                Log.d(TAG, "BLE peripheral started successfully");
            } else {
                gattServerManager.stopServer();
                broadcastError("Failed to start advertising");
            }
        } else {
            broadcastError("Failed to start GATT server");
        }
    }
    
    /**
     * Stop BLE peripheral mode
     */
    public void stopPeripheral() {
        Log.d(TAG, "Stopping BLE peripheral");
        
        if (!isPeripheralActive) {
            Log.w(TAG, "Peripheral not active");
            return;
        }
        
        stopCardUpdates();
        
        if (advertisementManager != null) {
            advertisementManager.stopAdvertising();
        }
        
        if (gattServerManager != null) {
            gattServerManager.stopServer();
        }
        
        isPeripheralActive = false;
        connectedDeviceCount = 0;
        stopForeground(true);
        broadcastStateChange(false);
        
        Log.d(TAG, "BLE peripheral stopped");
    }
    
    /**
     * Update card data manually
     */
    public void updateCardData() {
        if (isPeripheralActive && gattServerManager != null) {
            gattServerManager.updateCardData();
        }
    }
    
    /**
     * Check if peripheral is active
     */
    public boolean isPeripheralActive() {
        return isPeripheralActive;
    }
    
    /**
     * Get connected device count
     */
    public int getConnectedDeviceCount() {
        return connectedDeviceCount;
    }
    
    private void startCardUpdates() {
        cardUpdateHandler.postDelayed(cardUpdateRunnable, CARD_UPDATE_INTERVAL);
    }
    
    private void stopCardUpdates() {
        cardUpdateHandler.removeCallbacks(cardUpdateRunnable);
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "BLE Peripheral Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Manages BLE peripheral operations");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, BlePeripheralActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        
        String contentText = "Connected devices: " + connectedDeviceCount;
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Card Deck BLE Peripheral")
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.cards)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
    
    private void updateNotification() {
        if (isPeripheralActive) {
            NotificationManager notificationManager = 
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, createNotification());
            }
        }
    }
    
    private void broadcastStateChange(boolean isActive) {
        Intent intent = new Intent(BROADCAST_PERIPHERAL_STATE_CHANGED);
        intent.putExtra(EXTRA_STATE, isActive);
        intent.putExtra(EXTRA_CONNECTED_COUNT, connectedDeviceCount);
        sendBroadcast(intent);
    }
    
    private void broadcastDeviceConnected(BluetoothDevice device) {
        Intent intent = new Intent(BROADCAST_DEVICE_CONNECTED);
        intent.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        intent.putExtra(EXTRA_DEVICE_NAME, device.getName());
        intent.putExtra(EXTRA_CONNECTED_COUNT, connectedDeviceCount);
        sendBroadcast(intent);
    }
    
    private void broadcastDeviceDisconnected(BluetoothDevice device) {
        Intent intent = new Intent(BROADCAST_DEVICE_DISCONNECTED);
        intent.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
        intent.putExtra(EXTRA_DEVICE_NAME, device.getName());
        intent.putExtra(EXTRA_CONNECTED_COUNT, connectedDeviceCount);
        sendBroadcast(intent);
    }
    
    private void broadcastError(String errorMessage) {
        Intent intent = new Intent(BROADCAST_ERROR);
        intent.putExtra(EXTRA_ERROR_MESSAGE, errorMessage);
        sendBroadcast(intent);
    }
    
    private void cleanup() {
        if (cardUpdateHandler != null) {
            cardUpdateHandler.removeCallbacks(cardUpdateRunnable);
        }
        
        if (advertisementManager != null) {
            advertisementManager.cleanup();
        }
        
        if (gattServerManager != null) {
            gattServerManager.cleanup();
        }
    }
    
    // Advertisement Manager Callbacks
    @Override
    public void onAdvertisementStarted() {
        Log.d(TAG, "Advertisement started");
    }
    
    @Override
    public void onAdvertisementStopped() {
        Log.d(TAG, "Advertisement stopped");
    }
    
    @Override
    public void onAdvertisementFailed(int errorCode, String errorMessage) {
        Log.e(TAG, "Advertisement failed: " + errorMessage);
        broadcastError("Advertisement failed: " + errorMessage);
        stopPeripheral();
    }
    
    // GATT Server Manager Callbacks
    @Override
    public void onDeviceConnected(BluetoothDevice device) {
        connectedDeviceCount++;
        Log.d(TAG, "Device connected: " + device.getAddress() + ", total: " + connectedDeviceCount);
        updateNotification();
        broadcastDeviceConnected(device);
    }
    
    @Override
    public void onDeviceDisconnected(BluetoothDevice device) {
        connectedDeviceCount--;
        Log.d(TAG, "Device disconnected: " + device.getAddress() + ", total: " + connectedDeviceCount);
        updateNotification();
        broadcastDeviceDisconnected(device);
    }
    
    @Override
    public void onCharacteristicRead(BluetoothDevice device, String characteristicName, byte[] data) {
        Log.d(TAG, "Characteristic read: " + characteristicName + " by " + device.getAddress());
    }
    
    @Override
    public void onCharacteristicWrite(BluetoothDevice device, String characteristicName, byte[] data) {
        Log.d(TAG, "Characteristic write: " + characteristicName + " by " + device.getAddress());
    }
    
    @Override
    public void onServerStarted() {
        Log.d(TAG, "GATT server started");
    }
    
    @Override
    public void onServerStopped() {
        Log.d(TAG, "GATT server stopped");
    }
    
    @Override
    public void onServerError(String error) {
        Log.e(TAG, "GATT server error: " + error);
        broadcastError("GATT server error: " + error);
        stopPeripheral();
    }
}

