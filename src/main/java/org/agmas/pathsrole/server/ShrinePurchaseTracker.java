package org.agmas.pathsrole.server;

import org.agmas.pathsrole.ShrineShopType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 阵营购买次数跟踪器
 * 每个阵营（杀手、中立偏杀手、特殊中立、无辜者）共享3次购买次数
 * 神社重建时重置
 */
public class ShrinePurchaseTracker {
    private static final int MAX_PURCHASES_PER_FACTION = 3;
    private static final Map<ShrineShopType, Integer> remainingPurchases = new EnumMap<>(ShrineShopType.class);

    /**
     * 重置所有阵营的购买次数为3
     */
    public static void reset() {
        remainingPurchases.clear();
        for (ShrineShopType type : ShrineShopType.values()) {
            remainingPurchases.put(type, MAX_PURCHASES_PER_FACTION);
        }
    }

    /**
     * 检查该阵营是否还有剩余购买次数
     */
    public static boolean hasRemaining(ShrineShopType type) {
        return remainingPurchases.getOrDefault(type, 0) > 0;
    }

    /**
     * 消耗一次购买次数
     * @return 如果消耗成功返回true，如果已无次数返回false
     */
    public static boolean consume(ShrineShopType type) {
        int remaining = remainingPurchases.getOrDefault(type, 0);
        if (remaining <= 0) return false;
        remainingPurchases.put(type, remaining - 1);
        return true;
    }

    /**
     * 获取该阵营剩余购买次数
     */
    public static int getRemaining(ShrineShopType type) {
        return remainingPurchases.getOrDefault(type, 0);
    }

    /**
     * 获取每个阵营的最大购买次数
     */
    public static int getMaxPurchases() {
        return MAX_PURCHASES_PER_FACTION;
    }
}