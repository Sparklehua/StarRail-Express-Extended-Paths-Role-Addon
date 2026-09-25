package org.agmas.pathsrole.server;

import org.agmas.pathsrole.ShrineShopType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 阵营购买次数跟踪器
 * 普通阵营（杀手、中立偏杀手、无辜者）共享3次购买次数
 * 特殊中立阵营：每名玩家独立2次购买次数，且隐身药水/速度药水每名玩家独立锁一次
 * 神社重建时重置
 */
public class ShrinePurchaseTracker {
    private static final int MAX_PURCHASES_PER_FACTION = 3;
    private static final int SPECIAL_NEUTRAL_MAX_PER_PLAYER = 2;
    private static final Map<ShrineShopType, Integer> remainingPurchases = new EnumMap<>(ShrineShopType.class);

    private static final Map<UUID, Integer> specialNeutralPlayerPurchases = new HashMap<>();
    private static final Set<UUID> specialNeutralPotionPurchasedPlayers = new HashSet<>();

    public static void reset() {
        remainingPurchases.clear();
        for (ShrineShopType type : ShrineShopType.values()) {
            remainingPurchases.put(type, MAX_PURCHASES_PER_FACTION);
        }
        specialNeutralPlayerPurchases.clear();
        specialNeutralPotionPurchasedPlayers.clear();
    }

    public static boolean hasRemaining(ShrineShopType type) {
        if (type == ShrineShopType.SPECIAL_NEUTRAL) {
            return true;
        }
        return remainingPurchases.getOrDefault(type, 0) > 0;
    }

    public static boolean consume(ShrineShopType type) {
        if (type == ShrineShopType.SPECIAL_NEUTRAL) {
            return true;
        }
        int remaining = remainingPurchases.getOrDefault(type, 0);
        if (remaining <= 0) return false;
        remainingPurchases.put(type, remaining - 1);
        return true;
    }

    public static int getRemaining(ShrineShopType type) {
        if (type == ShrineShopType.SPECIAL_NEUTRAL) {
            return SPECIAL_NEUTRAL_MAX_PER_PLAYER;
        }
        return remainingPurchases.getOrDefault(type, 0);
    }

    public static int getMaxPurchases() {
        return MAX_PURCHASES_PER_FACTION;
    }

    public static int getSpecialNeutralMaxPerPlayer() {
        return SPECIAL_NEUTRAL_MAX_PER_PLAYER;
    }

    public static boolean canPlayerPurchase(UUID playerId) {
        int used = specialNeutralPlayerPurchases.getOrDefault(playerId, 0);
        return used < SPECIAL_NEUTRAL_MAX_PER_PLAYER;
    }

    public static void recordPlayerPurchase(UUID playerId) {
        int used = specialNeutralPlayerPurchases.getOrDefault(playerId, 0);
        specialNeutralPlayerPurchases.put(playerId, used + 1);
    }

    public static int getPlayerRemaining(UUID playerId) {
        int used = specialNeutralPlayerPurchases.getOrDefault(playerId, 0);
        return Math.max(0, SPECIAL_NEUTRAL_MAX_PER_PLAYER - used);
    }

    public static boolean isSpecialNeutralPotionAvailable(UUID playerId) {
        return !specialNeutralPotionPurchasedPlayers.contains(playerId);
    }

    public static void markSpecialNeutralPotionPurchased(UUID playerId) {
        specialNeutralPotionPurchasedPlayers.add(playerId);
    }

    public static boolean isSpecialNeutralPotionPurchased(UUID playerId) {
        return specialNeutralPotionPurchasedPlayers.contains(playerId);
    }
}