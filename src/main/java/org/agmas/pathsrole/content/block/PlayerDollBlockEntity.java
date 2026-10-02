package org.agmas.pathsrole.content.block;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.agmas.pathsrole.init.ModBlockEntities;
import org.agmas.pathsrole.util.GameProfileHelper;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PlayerDollBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private GameProfile ownerProfile = null;
    private String playerName = "";
    private String skinUrl = "";
    private ResourceLocation skinTexture = null;
    private boolean skinLoadingAttempted = false;
    public double squash;
    public float[] boneData = new float[0];
    public float modelScale = 1.0f;
    public String hatItemId = null;
    public float[] hatTransform = null;
    public String handItemId = null;
    public float[] handTransform = null;

    public PlayerDollBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLAYER_DOLL, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PlayerDollBlockEntity doll) {
        if (doll.squash > 0) {
            doll.squash /= 3.0;
            if (doll.squash < 0.01) {
                doll.squash = 0;
                if (level != null) {
                    level.updateNeighborsAt(pos, state.getBlock());
                }
            }
        }
    }

    public void squish(int amount) {
        this.squash += amount;
        if (this.level != null) {
            this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
        this.setChanged();
    }

    public GameProfile getOwnerProfile() {
        return ownerProfile;
    }

    public void setOwnerProfile(GameProfile profile) {
        this.ownerProfile = profile;
        this.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String name) {
        this.playerName = name;
        this.skinTexture = null;
        this.skinLoadingAttempted = false;
        this.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public String getSkinUrl() {
        return skinUrl;
    }

    public void setSkinUrl(String url) {
        this.skinUrl = url;
        this.skinTexture = null;
        this.skinLoadingAttempted = false;
        this.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ResourceLocation getSkinTexture() {
        return skinTexture;
    }

    public void setSkinTexture(ResourceLocation texture) {
        this.skinTexture = texture;
        this.setChanged();
    }

    public boolean isSkinLoadingAttempted() {
        return skinLoadingAttempted;
    }

    public void setSkinLoadingAttempted(boolean attempted) {
        this.skinLoadingAttempted = attempted;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("squash", this.squash);
        if (this.ownerProfile != null) {
            GameProfileHelper.writeGameProfile(tag, this.ownerProfile);
        }
        if (this.playerName != null && !this.playerName.isEmpty()) {
            tag.putString("PlayerName", this.playerName);
        }
        if (this.skinUrl != null && !this.skinUrl.isEmpty()) {
            tag.putString("SkinUrl", this.skinUrl);
        }
        if (this.boneData != null && this.boneData.length > 0) {
            CompoundTag poses = new CompoundTag();
            for (int i = 0; i < this.boneData.length; i++) {
                poses.putFloat("b" + i, this.boneData[i]);
            }
            tag.put("Poses", poses);
        }
        if (this.hatItemId != null && !this.hatItemId.isEmpty()) {
            tag.putString("HatItem", this.hatItemId);
            if (this.hatTransform != null && this.hatTransform.length >= 6) {
                CompoundTag ht = new CompoundTag();
                ht.putFloat("rx", this.hatTransform[0]);
                ht.putFloat("ry", this.hatTransform[1]);
                ht.putFloat("rz", this.hatTransform[2]);
                ht.putFloat("px", this.hatTransform[3]);
                ht.putFloat("py", this.hatTransform[4]);
                ht.putFloat("pz", this.hatTransform[5]);
                tag.put("HatTransform", ht);
            }
        }
        if (this.handItemId != null && !this.handItemId.isEmpty()) {
            tag.putString("HandItem", this.handItemId);
            if (this.handTransform != null && this.handTransform.length >= 6) {
                CompoundTag ht = new CompoundTag();
                ht.putFloat("rx", this.handTransform[0]);
                ht.putFloat("ry", this.handTransform[1]);
                ht.putFloat("rz", this.handTransform[2]);
                ht.putFloat("px", this.handTransform[3]);
                ht.putFloat("py", this.handTransform[4]);
                ht.putFloat("pz", this.handTransform[5]);
                tag.put("HandTransform", ht);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.squash = tag.getDouble("squash");
        try {
            this.ownerProfile = GameProfileHelper.readGameProfile(tag);
        } catch (Exception e) {
            this.ownerProfile = null;
        }
        if (tag.contains("PlayerName")) {
            String name = tag.getString("PlayerName");
            if (!name.isEmpty()) {
                this.playerName = name;
            }
        }
        if (tag.contains("SkinUrl")) {
            String url = tag.getString("SkinUrl");
            if (!url.isEmpty()) {
                this.skinUrl = url;
            }
        }
        this.skinLoadingAttempted = false;
        this.skinTexture = null;
        if (tag.contains("Poses")) {
            CompoundTag poses = tag.getCompound("Poses");
            int count = poses.getAllKeys().size();
            if (count > 0) {
                this.boneData = new float[count];
                for (String key : poses.getAllKeys()) {
                    try {
                        int idx = Integer.parseInt(key.substring(1));
                        this.boneData[idx] = poses.getFloat(key);
                    } catch (Exception ignored) {
                    }
                }
                if (count > 15) {
                    this.modelScale = Mth.clamp(this.boneData[15], 0.1f, 3.0f);
                }
            }
        }
        if (tag.contains("HatItem")) {
            this.hatItemId = tag.getString("HatItem");
            if (tag.contains("HatTransform")) {
                CompoundTag ht = tag.getCompound("HatTransform");
                this.hatTransform = new float[] {
                        ht.getFloat("rx"), ht.getFloat("ry"), ht.getFloat("rz"),
                        ht.getFloat("px"), ht.getFloat("py"), ht.getFloat("pz")
                };
            }
        }
        if (tag.contains("HandItem")) {
            this.handItemId = tag.getString("HandItem");
            if (tag.contains("HandTransform")) {
                CompoundTag ht = tag.getCompound("HandTransform");
                this.handTransform = new float[] {
                        ht.getFloat("rx"), ht.getFloat("ry"), ht.getFloat("rz"),
                        ht.getFloat("px"), ht.getFloat("py"), ht.getFloat("pz")
                };
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}