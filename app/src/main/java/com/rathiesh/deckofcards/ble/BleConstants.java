package com.rathiesh.deckofcards.ble;

import java.util.UUID;

/**
 * Created for BLE Peripheral functionality
 * Contains all BLE-related constants including UUIDs for services and characteristics
 */
public class BleConstants {
    
    // Custom service UUID for Card Deck service
    public static final UUID CARD_DECK_SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc");
    
    // Characteristic UUIDs
    public static final UUID CARD_SUIT_CHARACTERISTIC_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abd");
    public static final UUID CARD_RANK_CHARACTERISTIC_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abe");
    public static final UUID CARD_COUNT_CHARACTERISTIC_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abf");
    
    // Descriptor UUIDs
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    
    // Advertisement settings
    public static final String DEVICE_NAME = "CardDeck_BLE";
    public static final int ADVERTISEMENT_TIMEOUT_MS = 0; // 0 means no timeout
    
    // GATT Server settings
    public static final int MAX_CONNECTED_DEVICES = 4;
    
    // Card data constants
    public static final String[] SUITS = {"Hearts", "Diamonds", "Clubs", "Spades"};
    public static final String[] RANKS = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King", "Ace"};
    
    // Error codes
    public static final int ERROR_BLUETOOTH_NOT_SUPPORTED = 1001;
    public static final int ERROR_BLE_NOT_SUPPORTED = 1002;
    public static final int ERROR_BLUETOOTH_NOT_ENABLED = 1003;
    public static final int ERROR_LOCATION_PERMISSION_DENIED = 1004;
    public static final int ERROR_ADVERTISEMENT_FAILED = 1005;
    public static final int ERROR_GATT_SERVER_FAILED = 1006;
}

