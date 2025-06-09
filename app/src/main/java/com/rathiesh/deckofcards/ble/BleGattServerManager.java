package com.rathiesh.deckofcards.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created for BLE Peripheral functionality
 * Manages GATT server for card deck data services
 */
public class BleGattServerManager {
    
    private static final String TAG = "BleGattServerManager";
    
    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothGattServer gattServer;
    private BleCardDataModel cardDataModel;
    private List<BluetoothDevice> connectedDevices;
    private BleGattServerListener listener;
    
    // Characteristics
    private BluetoothGattCharacteristic suitCharacteristic;
    private BluetoothGattCharacteristic rankCharacteristic;
    private BluetoothGattCharacteristic cardCountCharacteristic;
    
    public interface BleGattServerListener {
        void onDeviceConnected(BluetoothDevice device);
        void onDeviceDisconnected(BluetoothDevice device);
        void onCharacteristicRead(BluetoothDevice device, String characteristicName, byte[] data);
        void onCharacteristicWrite(BluetoothDevice device, String characteristicName, byte[] data);
        void onServerStarted();
        void onServerStopped();
        void onServerError(String error);
    }
    
    public BleGattServerManager(Context context) {
        this.context = context;
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.cardDataModel = new BleCardDataModel();
        this.connectedDevices = new ArrayList<>();
    }
    
    public void setListener(BleGattServerListener listener) {
        this.listener = listener;
    }
    
    /**
     * Start the GATT server
     * @return true if server started successfully, false otherwise
     */
    public boolean startServer() {
        if (bluetoothManager == null) {
            Log.e(TAG, "BluetoothManager is null");
            if (listener != null) {
                listener.onServerError("BluetoothManager is not available");
            }
            return false;
        }
        
        try {
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback);
            if (gattServer == null) {
                Log.e(TAG, "Failed to create GATT server");
                if (listener != null) {
                    listener.onServerError("Failed to create GATT server");
                }
                return false;
            }
            
            setupGattServices();
            Log.d(TAG, "GATT server started successfully");
            if (listener != null) {
                listener.onServerStarted();
            }
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start GATT server", e);
            if (listener != null) {
                listener.onServerError("Failed to start GATT server: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Stop the GATT server
     */
    public void stopServer() {
        if (gattServer != null) {
            try {
                gattServer.close();
                gattServer = null;
                connectedDevices.clear();
                Log.d(TAG, "GATT server stopped");
                if (listener != null) {
                    listener.onServerStopped();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error stopping GATT server", e);
            }
        }
    }
    
    /**
     * Get list of connected devices
     * @return list of connected devices
     */
    public List<BluetoothDevice> getConnectedDevices() {
        return new ArrayList<>(connectedDevices);
    }
    
    /**
     * Get number of connected devices
     * @return number of connected devices
     */
    public int getConnectedDeviceCount() {
        return connectedDevices.size();
    }
    
    /**
     * Update card data and notify connected devices
     */
    public void updateCardData() {
        if (gattServer == null) return;
        
        // Update characteristics with new data
        if (suitCharacteristic != null) {
            byte[] suitData = cardDataModel.getNextSuit();
            suitCharacteristic.setValue(suitData);
            notifyCharacteristicChanged(suitCharacteristic);
        }
        
        if (rankCharacteristic != null) {
            byte[] rankData = cardDataModel.getNextRank();
            rankCharacteristic.setValue(rankData);
            notifyCharacteristicChanged(rankCharacteristic);
        }
    }
    
    private void setupGattServices() {
        // Create Card Deck Service
        BluetoothGattService cardDeckService = new BluetoothGattService(
                BleConstants.CARD_DECK_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
        );
        
        // Create Suit Characteristic
        suitCharacteristic = new BluetoothGattCharacteristic(
                BleConstants.CARD_SUIT_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
        );
        
        // Create Rank Characteristic
        rankCharacteristic = new BluetoothGattCharacteristic(
                BleConstants.CARD_RANK_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
        );
        
        // Create Card Count Characteristic
        cardCountCharacteristic = new BluetoothGattCharacteristic(
                BleConstants.CARD_COUNT_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
        );
        
        // Add Client Characteristic Configuration Descriptor for notifications
        BluetoothGattDescriptor suitDescriptor = new BluetoothGattDescriptor(
                BleConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE
        );
        suitCharacteristic.addDescriptor(suitDescriptor);
        
        BluetoothGattDescriptor rankDescriptor = new BluetoothGattDescriptor(
                BleConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE
        );
        rankCharacteristic.addDescriptor(rankDescriptor);
        
        // Set initial values
        suitCharacteristic.setValue(cardDataModel.getRandomSuit());
        rankCharacteristic.setValue(cardDataModel.getRandomRank());
        cardCountCharacteristic.setValue(cardDataModel.cardCountToByteArray(52)); // Standard deck
        
        // Add characteristics to service
        cardDeckService.addCharacteristic(suitCharacteristic);
        cardDeckService.addCharacteristic(rankCharacteristic);
        cardDeckService.addCharacteristic(cardCountCharacteristic);
        
        // Add service to server
        gattServer.addService(cardDeckService);
    }
    
    private void notifyCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        for (BluetoothDevice device : connectedDevices) {
            try {
                gattServer.notifyCharacteristicChanged(device, characteristic, false);
            } catch (Exception e) {
                Log.e(TAG, "Failed to notify device: " + device.getAddress(), e);
            }
        }
    }
    
    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d(TAG, "Device connected: " + device.getAddress());
                if (!connectedDevices.contains(device)) {
                    connectedDevices.add(device);
                }
                if (listener != null) {
                    listener.onDeviceConnected(device);
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(TAG, "Device disconnected: " + device.getAddress());
                connectedDevices.remove(device);
                if (listener != null) {
                    listener.onDeviceDisconnected(device);
                }
            }
        }
        
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            
            Log.d(TAG, "Characteristic read request from: " + device.getAddress());
            
            byte[] data = characteristic.getValue();
            if (data != null && offset < data.length) {
                byte[] response = new byte[data.length - offset];
                System.arraycopy(data, offset, response, 0, response.length);
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response);
                
                String characteristicName = getCharacteristicName(characteristic.getUuid());
                if (listener != null) {
                    listener.onCharacteristicRead(device, characteristicName, response);
                }
            } else {
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
            }
        }
        
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor, boolean preparedWrite,
                                             boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            
            Log.d(TAG, "Descriptor write request from: " + device.getAddress());
            
            if (BleConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID.equals(descriptor.getUuid())) {
                descriptor.setValue(value);
                if (responseNeeded) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }
            } else {
                if (responseNeeded) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
                }
            }
        }
    };
    
    private String getCharacteristicName(java.util.UUID uuid) {
        if (BleConstants.CARD_SUIT_CHARACTERISTIC_UUID.equals(uuid)) {
            return "Card Suit";
        } else if (BleConstants.CARD_RANK_CHARACTERISTIC_UUID.equals(uuid)) {
            return "Card Rank";
        } else if (BleConstants.CARD_COUNT_CHARACTERISTIC_UUID.equals(uuid)) {
            return "Card Count";
        }
        return "Unknown";
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        stopServer();
        listener = null;
    }
}

