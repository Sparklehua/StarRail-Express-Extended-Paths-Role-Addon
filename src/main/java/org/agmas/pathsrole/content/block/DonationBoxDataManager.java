package org.agmas.pathsrole.content.block;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class DonationBoxDataManager {
    private static final Map<BlockPos, BoxInfo> BOXES = new ConcurrentHashMap<BlockPos, BoxInfo>();
    private static final Set<BlockPos> CLIENT_BOXES = new HashSet<BlockPos>();
    private static final Set<BlockPos> CLIENT_OWNED_BOXES = new HashSet<BlockPos>();
    private static final Set<UUID> REIMU_MARKED_PLAYERS = new HashSet<UUID>();
    private static boolean amnesiacEffectGiven = false;

    public static boolean isMarked(UUID playerUuid) {
        return REIMU_MARKED_PLAYERS.contains(playerUuid);
    }

    public static void markPlayer(UUID playerUuid) {
        REIMU_MARKED_PLAYERS.add(playerUuid);
    }

    public static void clearMarks() {
        REIMU_MARKED_PLAYERS.clear();
    }

    public static boolean isAmnesiacEffectGiven() {
        return amnesiacEffectGiven;
    }

    public static void markAmnesiacEffectGiven() {
        amnesiacEffectGiven = true;
    }

    public static void registerBox(BlockPos pos, UUID ownerUuid) {
        BoxInfo info = new BoxInfo(ownerUuid, pos);
        BOXES.put(pos, info);
    }

    public static void unregisterBox(BlockPos pos) {
        BOXES.remove(pos);
    }

    public static boolean isDonationBox(BlockPos pos) {
        return BOXES.containsKey(pos);
    }

    public static BoxInfo getBoxInfo(BlockPos pos) {
        return BOXES.get(pos);
    }

    public static Map<BlockPos, BoxInfo> getAllBoxes() {
        return BOXES;
    }

    public static void reset() {
        BOXES.clear();
        CLIENT_BOXES.clear();
        CLIENT_OWNED_BOXES.clear();
        REIMU_MARKED_PLAYERS.clear();
        amnesiacEffectGiven = false;
    }

    public static int countOwnedBoxes(UUID owner) {
        if (owner == null) {
            return 0;
        }
        int count = 0;
        for (BoxInfo info : BOXES.values()) {
            if (!owner.equals(info.ownerUuid)) continue;
            ++count;
        }
        return count;
    }

    public static boolean isOwnBoxClient(BlockPos pos) {
        return CLIENT_OWNED_BOXES.contains(pos);
    }

    public static Set<BlockPos> getClientOwnedBoxes() {
        return CLIENT_OWNED_BOXES;
    }

    public static void readClientSync(CompoundTag tag) {
        CLIENT_BOXES.clear();
        CLIENT_OWNED_BOXES.clear();
        ListTag boxList = tag.getList("DonationBoxes", 10);
        for (int i = 0; i < boxList.size(); ++i) {
            CompoundTag boxTag = boxList.getCompound(i);
            BlockPos pos = new BlockPos(boxTag.getInt("X"), boxTag.getInt("Y"), boxTag.getInt("Z"));
            boolean owned = boxTag.getBoolean("Owned");
            CLIENT_BOXES.add(pos);
            if (!owned) continue;
            CLIENT_OWNED_BOXES.add(pos);
        }
    }

    public static void writeServerSync(CompoundTag tag, UUID playerUuid) {
        ListTag boxList = new ListTag();
        for (BoxInfo info : BOXES.values()) {
            CompoundTag boxTag = new CompoundTag();
            boxTag.putInt("X", info.pos.getX());
            boxTag.putInt("Y", info.pos.getY());
            boxTag.putInt("Z", info.pos.getZ());
            boxTag.putBoolean("Owned", info.ownerUuid.equals(playerUuid));
            boxList.add(boxTag);
        }
        tag.put("DonationBoxes", boxList);
    }

    public static class BoxInfo {
        public final UUID ownerUuid;
        public final BlockPos pos;
        public int destructionProgress;
        public UUID lastDestroyer;
        public int tickCounter;

        public BoxInfo(UUID ownerUuid, BlockPos pos) {
            this.ownerUuid = ownerUuid;
            this.pos = pos;
            this.destructionProgress = 0;
            this.lastDestroyer = null;
            this.tickCounter = 0;
        }
    }
}