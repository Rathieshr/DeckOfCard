package com.rathiesh.deckofcards.ble;

import com.rathiesh.deckofcards.businesslogiclayer.RankModel;
import com.rathiesh.deckofcards.businesslogiclayer.SuitModel;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Vector;

/**
 * Created for BLE Peripheral functionality
 * Handles conversion of card data to BLE-compatible byte arrays
 */
public class BleCardDataModel {
    
    private Random random;
    private int currentSuitIndex;
    private int currentRankIndex;
    
    public BleCardDataModel() {
        random = new Random();
        currentSuitIndex = 0;
        currentRankIndex = 0;
    }
    
    /**
     * Convert suit name to byte array for BLE transmission
     * @param suitName The name of the suit
     * @return byte array representation
     */
    public byte[] suitToByteArray(String suitName) {
        if (suitName == null) {
            suitName = "Unknown";
        }
        return suitName.getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * Convert rank to byte array for BLE transmission
     * @param rank The rank value
     * @return byte array representation
     */
    public byte[] rankToByteArray(int rank) {
        String rankString;
        switch (rank) {
            case 11:
                rankString = "Jack";
                break;
            case 12:
                rankString = "Queen";
                break;
            case 13:
                rankString = "King";
                break;
            case 14:
                rankString = "Ace";
                break;
            default:
                rankString = String.valueOf(rank);
                break;
        }
        return rankString.getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * Convert card count to byte array
     * @param count The number of cards
     * @return byte array representation
     */
    public byte[] cardCountToByteArray(int count) {
        return String.valueOf(count).getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * Get a random suit as byte array
     * @return random suit as byte array
     */
    public byte[] getRandomSuit() {
        String suit = BleConstants.SUITS[random.nextInt(BleConstants.SUITS.length)];
        return suitToByteArray(suit);
    }
    
    /**
     * Get a random rank as byte array
     * @return random rank as byte array
     */
    public byte[] getRandomRank() {
        int rank = random.nextInt(13) + 2; // Ranks 2-14 (2-Ace)
        return rankToByteArray(rank);
    }
    
    /**
     * Get next suit in sequence
     * @return next suit as byte array
     */
    public byte[] getNextSuit() {
        String suit = BleConstants.SUITS[currentSuitIndex];
        currentSuitIndex = (currentSuitIndex + 1) % BleConstants.SUITS.length;
        return suitToByteArray(suit);
    }
    
    /**
     * Get next rank in sequence
     * @return next rank as byte array
     */
    public byte[] getNextRank() {
        int rank = currentRankIndex + 2; // Start from 2
        currentRankIndex = (currentRankIndex + 1) % 13;
        return rankToByteArray(rank);
    }
    
    /**
     * Convert SuitModel to byte array
     * @param suitModel The suit model from existing business logic
     * @return byte array representation
     */
    public byte[] suitModelToByteArray(SuitModel suitModel) {
        if (suitModel != null && suitModel.getSuit() != null) {
            return suitToByteArray(suitModel.getSuit());
        }
        return suitToByteArray("Unknown");
    }
    
    /**
     * Convert RankModel to byte array
     * @param rankModel The rank model from existing business logic
     * @return byte array representation
     */
    public byte[] rankModelToByteArray(RankModel rankModel) {
        if (rankModel != null) {
            return rankToByteArray(rankModel.getRank());
        }
        return rankToByteArray(0);
    }
    
    /**
     * Get total card count from suit models
     * @param suitModels Vector of suit models
     * @return total card count as byte array
     */
    public byte[] getTotalCardCount(Vector<SuitModel> suitModels) {
        int totalCount = 0;
        if (suitModels != null) {
            for (SuitModel suitModel : suitModels) {
                if (suitModel.getRankModelVector() != null) {
                    totalCount += suitModel.getRankModelVector().size();
                }
            }
        }
        return cardCountToByteArray(totalCount);
    }
    
    /**
     * Convert byte array back to string for display
     * @param data byte array
     * @return string representation
     */
    public String byteArrayToString(byte[] data) {
        if (data != null && data.length > 0) {
            return new String(data, StandardCharsets.UTF_8);
        }
        return "";
    }
}

