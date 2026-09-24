package org.agmas.pathsrole.init;

import net.minecraft.world.level.block.Block;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import org.agmas.pathsrole.content.block.DonationBoxBlockEntity;
import org.agmas.pathsrole.content.block.FlowerDollBlockEntity;
import org.agmas.pathsrole.content.block.ShrineDonationBoxBlockEntity;
import org.agmas.pathsrole.init.ModBlocks;

public class ModBlockEntities {
    public static final BlockEntityType<DonationBoxBlockEntity> DONATION_BOX = BlockEntityType.Builder.of(DonationBoxBlockEntity::new, new Block[]{ModBlocks.DONATION_BOX}).build(null);
    public static final BlockEntityType<ShrineDonationBoxBlockEntity> SHRINE_DONATION_BOX = BlockEntityType.Builder.of(ShrineDonationBoxBlockEntity::new, new Block[]{ModBlocks.SHRINE_DONATION_BOX}).build(null);
    public static final BlockEntityType<FlowerDollBlockEntity> FLOWER_DOLL = BlockEntityType.Builder.of(FlowerDollBlockEntity::new, new Block[]{ModBlocks.FLOWER_DOLL}).build(null);

    public static void init() {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("pathsrole", "donation_box"), DONATION_BOX);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("pathsrole", "shrine_donation_box"), SHRINE_DONATION_BOX);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("pathsrole", "flower_doll"), FLOWER_DOLL);
    }
}