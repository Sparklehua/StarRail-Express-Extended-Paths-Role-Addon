package org.agmas.pathsrole.content.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.List;

public final class FlowerDollSavedData extends SavedData {
    private static final String DATA_NAME = "pathsrole_flower_doll_restore";

    public static final class Entry {
        public final ResourceLocation dimension;
        public final BlockPos pos;
        public final BlockState state;
        public final long restoreAtGameTime;

        public Entry(ResourceLocation dimension, BlockPos pos, BlockState state, long restoreAtGameTime) {
            this.dimension = dimension;
            this.pos = pos;
            this.state = state;
            this.restoreAtGameTime = restoreAtGameTime;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public static FlowerDollSavedData get(MinecraftServer server) {
        if (server == null) {
            throw new IllegalStateException("Cannot get FlowerDollSavedData: server is null");
        }
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) {
            throw new IllegalStateException("Cannot get FlowerDollSavedData: overworld level is null");
        }
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(FlowerDollSavedData::new, FlowerDollSavedData::load, null),
                DATA_NAME);
    }

    public List<Entry> entries() {
        return entries;
    }

    public void add(Entry entry) {
        entries.add(entry);
        setDirty(true);
    }

    public void remove(Entry entry) {
        entries.remove(entry);
        setDirty(true);
    }

    public static FlowerDollSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        FlowerDollSavedData data = new FlowerDollSavedData();
        HolderLookup.RegistryLookup<net.minecraft.world.level.block.Block> blocks =
                provider.lookupOrThrow(Registries.BLOCK);
        for (Tag element : tag.getList("Entries", Tag.TAG_COMPOUND)) {
            CompoundTag entryTag = (CompoundTag) element;
            ResourceLocation dim = ResourceLocation.parse(entryTag.getString("Dim"));
            BlockPos pos = new BlockPos(entryTag.getInt("X"), entryTag.getInt("Y"), entryTag.getInt("Z"));
            BlockState state = NbtUtils.readBlockState(blocks, entryTag.getCompound("State"));
            long restoreAt = entryTag.getLong("RestoreAt");
            data.entries.add(new Entry(dim, pos, state, restoreAt));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Entry entry : entries) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Dim", entry.dimension.toString());
            entryTag.putInt("X", entry.pos.getX());
            entryTag.putInt("Y", entry.pos.getY());
            entryTag.putInt("Z", entry.pos.getZ());
            entryTag.put("State", NbtUtils.writeBlockState(entry.state));
            entryTag.putLong("RestoreAt", entry.restoreAtGameTime);
            list.add(entryTag);
        }
        tag.put("Entries", list);
        return tag;
    }
}