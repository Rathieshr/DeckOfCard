package com.rathiesh.deckofcards.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.rathiesh.deckofcards.R;
import com.rathiesh.deckofcards.base.BaseActivity;
import com.rathiesh.deckofcards.ble.BleConstants;
import com.rathiesh.deckofcards.ble.BlePeripheralService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created for BLE Peripheral functionality
 * Activity that provides UI controls for BLE peripheral operations
 */
public class BlePeripheralActivity extends BaseActivity {
    
    private static final String TAG = "BlePeripheralActivity";
    private static final int REQUEST_ENABLE_BT = 1001;
    private static final int REQUEST_PERMISSIONS = 1002;
    
    // UI Components
    private Toolbar toolbar;
    private TextView tvStatus;
    private TextView tvConnectedCount;
    private TextView tvCurrentSuit;
    private TextView tvCurrentRank;
    private TextView tvConnectedDevices;
    private Button btnStartStop;
    private Button btnUpdateCards;
    
    // BLE Components
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BlePeripheralService peripheralService;
    private boolean isServiceBound = false;
    
    // State
    private boolean isPeripheralActive = false;
    private int connectedDeviceCount = 0;
    private List<String> connectedDevicesList = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_peripheral);
        
        initializeViews();
        setupToolbar();
        initializeBluetooth();
        checkPermissions();
        bindToService();
        registerBroadcastReceiver();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindFromService();
        unregisterBroadcastReceiver();
    }
    
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tvStatus = findViewById(R.id.tv_status);
        tvConnectedCount = findViewById(R.id.tv_connected_count);
        tvCurrentSuit = findViewById(R.id.tv_current_suit);
        tvCurrentRank = findViewById(R.id.tv_current_rank);
        tvConnectedDevices = findViewById(R.id.tv_connected_devices);
        btnStartStop = findViewById(R.id.btn_start_stop);
        btnUpdateCards = findViewById(R.id.btn_update_cards);
        
        setupClickListeners();
    }
    
    private void setupToolbar() {
        toolbar.setTitle("BLE Peripheral");
        toolbar.setSubtitle("Card Deck Service");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }
    
    private void setupClickListeners() {
        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPeripheralActive) {
                    stopPeripheral();
                } else {
                    startPeripheral();
                }
            }
        });
        
        btnUpdateCards.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCardData();
            }
        });
    }
    
    private void initializeBluetooth() {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        
        if (bluetoothAdapter == null) {
            showError("Bluetooth is not supported on this device");
            finish();
            return;
        }
        
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showError("BLE is not supported on this device");
            finish();
            return;
        }
    }
    
    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                    permissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }
    
    private void bindToService() {
        Intent serviceIntent = new Intent(this, BlePeripheralService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    private void unbindFromService() {
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
    
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BlePeripheralService.LocalBinder binder = (BlePeripheralService.LocalBinder) service;
            peripheralService = binder.getService();
            isServiceBound = true;
            updateUI();
            Log.d(TAG, "Service connected");
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            peripheralService = null;
            isServiceBound = false;
            Log.d(TAG, "Service disconnected");
        }
    };
    
    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BlePeripheralService.BROADCAST_PERIPHERAL_STATE_CHANGED);
        filter.addAction(BlePeripheralService.BROADCAST_DEVICE_CONNECTED);
        filter.addAction(BlePeripheralService.BROADCAST_DEVICE_DISCONNECTED);
        filter.addAction(BlePeripheralService.BROADCAST_ERROR);
        registerReceiver(broadcastReceiver, filter);
    }
    
    private void unregisterBroadcastReceiver() {
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
    }
    
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            
            switch (action) {
                case BlePeripheralService.BROADCAST_PERIPHERAL_STATE_CHANGED:
                    boolean isActive = intent.getBooleanExtra(BlePeripheralService.EXTRA_STATE, false);
                    int count = intent.getIntExtra(BlePeripheralService.EXTRA_CONNECTED_COUNT, 0);
                    updatePeripheralState(isActive, count);
                    break;
                    
                case BlePeripheralService.BROADCAST_DEVICE_CONNECTED:
                    String connectedAddress = intent.getStringExtra(BlePeripheralService.EXTRA_DEVICE_ADDRESS);
                    String connectedName = intent.getStringExtra(BlePeripheralService.EXTRA_DEVICE_NAME);
                    int connectedCount = intent.getIntExtra(BlePeripheralService.EXTRA_CONNECTED_COUNT, 0);
                    onDeviceConnected(connectedAddress, connectedName, connectedCount);
                    break;
                    
                case BlePeripheralService.BROADCAST_DEVICE_DISCONNECTED:
                    String disconnectedAddress = intent.getStringExtra(BlePeripheralService.EXTRA_DEVICE_ADDRESS);
                    String disconnectedName = intent.getStringExtra(BlePeripheralService.EXTRA_DEVICE_NAME);
                    int disconnectedCount = intent.getIntExtra(BlePeripheralService.EXTRA_CONNECTED_COUNT, 0);
                    onDeviceDisconnected(disconnectedAddress, disconnectedName, disconnectedCount);
                    break;
                    
                case BlePeripheralService.BROADCAST_ERROR:
                    String errorMessage = intent.getStringExtra(BlePeripheralService.EXTRA_ERROR_MESSAGE);
                    showError(errorMessage);
                    break;
            }
        }
    };
    
    private void startPeripheral() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }
        
        if (peripheralService != null) {
            peripheralService.startPeripheral();
        } else {
            Intent serviceIntent = new Intent(this, BlePeripheralService.class);
            serviceIntent.setAction(BlePeripheralService.ACTION_START_PERIPHERAL);
            startService(serviceIntent);
        }
    }
    
    private void stopPeripheral() {
        if (peripheralService != null) {
            peripheralService.stopPeripheral();
        } else {
            Intent serviceIntent = new Intent(this, BlePeripheralService.class);
            serviceIntent.setAction(BlePeripheralService.ACTION_STOP_PERIPHERAL);
            startService(serviceIntent);
        }
    }
    
    private void updateCardData() {
        if (peripheralService != null) {
            peripheralService.updateCardData();
            Toast.makeText(this, "Card data updated", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updatePeripheralState(boolean isActive, int count) {
        isPeripheralActive = isActive;
        connectedDeviceCount = count;
        updateUI();
    }
    
    private void onDeviceConnected(String address, String name, int count) {
        connectedDeviceCount = count;
        String deviceInfo = (name != null ? name : "Unknown") + " (" + address + ")";
        if (!connectedDevicesList.contains(deviceInfo)) {
            connectedDevicesList.add(deviceInfo);
        }
        updateConnectedDevicesDisplay();
        Toast.makeText(this, "Device connected: " + deviceInfo, Toast.LENGTH_SHORT).show();
    }
    
    private void onDeviceDisconnected(String address, String name, int count) {
        connectedDeviceCount = count;
        String deviceInfo = (name != null ? name : "Unknown") + " (" + address + ")";
        connectedDevicesList.remove(deviceInfo);
        updateConnectedDevicesDisplay();
        Toast.makeText(this, "Device disconnected: " + deviceInfo, Toast.LENGTH_SHORT).show();
    }
    
    private void updateUI() {
        if (isPeripheralActive) {
            tvStatus.setText("Running");
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            btnStartStop.setText("Stop Peripheral");
            btnStartStop.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            btnUpdateCards.setEnabled(true);
        } else {
            tvStatus.setText("Stopped");
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            btnStartStop.setText("Start Peripheral");
            btnStartStop.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            btnUpdateCards.setEnabled(false);
        }
        
        tvConnectedCount.setText(String.valueOf(connectedDeviceCount));
        updateConnectedDevicesDisplay();
    }
    
    private void updateConnectedDevicesDisplay() {
        if (connectedDevicesList.isEmpty()) {
            tvConnectedDevices.setText("No devices connected");
            tvConnectedDevices.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < connectedDevicesList.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append("• ").append(connectedDevicesList.get(i));
            }
            tvConnectedDevices.setText(sb.toString());
            tvConnectedDevices.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }
    }
    
    private void showError(String message) {
        Toast.makeText(this, "Error: " + message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startPeripheral();
            } else {
                showError("Bluetooth must be enabled to use BLE peripheral");
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                showError("All permissions are required for BLE peripheral functionality");
                finish();
            }
        }
    }
}

