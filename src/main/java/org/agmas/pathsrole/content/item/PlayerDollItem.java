package org.agmas.pathsrole.content.item;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.agmas.pathsrole.content.block.PlayerDollBlockEntity;
import org.agmas.pathsrole.util.GameProfileHelper;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class PlayerDollItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PlayerDollItem(Block block, Properties settings) {
        super(block, settings);
    }

    public static GameProfile getGameProfile(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            CompoundTag nbt = customData.copyTag();
            if (nbt.contains("Owner", 10)) {
                return GameProfileHelper.readGameProfile(nbt);
            }
        }
        return null;
    }

    public static String getPlayerName(ItemStack stack) {
        GameProfile profile = getGameProfile(stack);
        if (profile != null && profile.getName() != null && !profile.getName().isEmpty()) {
            return profile.getName();
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            CompoundTag nbt = customData.copyTag();
            if (nbt.contains("PlayerName")) {
                return nbt.getString("PlayerName");
            }
        }
        return null;
    }

    public static String getSkinUrl(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            CompoundTag nbt = customData.copyTag();
            if (nbt.contains("SkinUrl")) {
                return nbt.getString("SkinUrl");
            }
        }
        return null;
    }

    @Override
    public Component getName(ItemStack stack) {
        String playerName = getPlayerName(stack);
        if (playerName != null && !playerName.isEmpty()) {
            return Component.translatable("item.pathsrole.player_doll.owner", playerName);
        }
        return Component.translatable("item.pathsrole.player_doll");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        String playerName = getPlayerName(stack);
        if (playerName != null && !playerName.isEmpty()) {
            tooltip.add(Component.translatable("item.pathsrole.player_doll.tooltip", playerName));
            tooltip.add(Component.translatable("item.pathsrole.player_doll.tooltip2"));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult result = super.useOn(context);
        if (result.consumesAction() && !context.getLevel().isClientSide()) {
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            if (context.getLevel().getBlockEntity(pos) instanceof PlayerDollBlockEntity dollEntity) {
                CustomData customData = context.getItemInHand().get(DataComponents.CUSTOM_DATA);
                if (customData != null && !customData.isEmpty()) {
                    CompoundTag tag = customData.copyTag();

                    GameProfile profile = getGameProfile(context.getItemInHand());
                    String playerName = null;
                    String skinUrl = null;

                    if (profile != null) {
                        dollEntity.setOwnerProfile(profile);
                        if (profile.getName() != null && !profile.getName().isEmpty()) {
                            playerName = profile.getName();
                        }
                    }

                    if (playerName == null || playerName.isEmpty()) {
                        if (tag.contains("PlayerName")) {
                            playerName = tag.getString("PlayerName");
                        }
                    }

                    if (tag.contains("SkinUrl")) {
                        skinUrl = tag.getString("SkinUrl");
                    }

                    if (playerName != null && !playerName.isEmpty()) {
                        dollEntity.setPlayerName(playerName);
                    }

                    if (skinUrl != null && !skinUrl.isEmpty()) {
                        dollEntity.setSkinUrl(skinUrl);
                    }

                    if (tag.contains("Poses")) {
                        CompoundTag poses = tag.getCompound("Poses");
                        int count = poses.getAllKeys().size();
                        if (count > 0) {
                            float[] boneData = new float[count];
                            for (String key : poses.getAllKeys()) {
                                try {
                                    int idx = Integer.parseInt(key.substring(1));
                                    boneData[idx] = poses.getFloat(key);
                                } catch (Exception ignored) {}
                            }
                            dollEntity.boneData = boneData;
                            if (count > 15) {
                                dollEntity.modelScale = net.minecraft.util.Mth.clamp(boneData[15], 0.1f, 3.0f);
                            }
                        }
                    }

                    if (tag.contains("HandItem")) {
                        dollEntity.handItemId = tag.getString("HandItem");
                        if (tag.contains("HandTransform")) {
                            CompoundTag ht = tag.getCompound("HandTransform");
                            dollEntity.handTransform = new float[] {
                                    ht.getFloat("rx"), ht.getFloat("ry"), ht.getFloat("rz"),
                                    ht.getFloat("px"), ht.getFloat("py"), ht.getFloat("pz")
                            };
                        }
                    }

                    if (tag.contains("HatItem")) {
                        dollEntity.hatItemId = tag.getString("HatItem");
                        if (tag.contains("HatTransform")) {
                            CompoundTag ht = tag.getCompound("HatTransform");
                            dollEntity.hatTransform = new float[] {
                                    ht.getFloat("rx"), ht.getFloat("ry"), ht.getFloat("rz"),
                                    ht.getFloat("px"), ht.getFloat("py"), ht.getFloat("pz")
                            };
                        }
                    }

                    dollEntity.setChanged();
                }
            }
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack currentHelmet = player.getItemBySlot(EquipmentSlot.HEAD);

        if (currentHelmet.isEmpty()) {
            player.setItemSlot(EquipmentSlot.HEAD, stack.copy());
            player.setItemInHand(hand, ItemStack.EMPTY);
        } else {
            player.setItemInHand(hand, currentHelmet);
            player.setItemSlot(EquipmentSlot.HEAD, stack.copy());
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}