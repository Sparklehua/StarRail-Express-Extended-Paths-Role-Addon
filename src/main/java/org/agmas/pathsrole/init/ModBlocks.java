package org.agmas.pathsrole.init;

import net.minecraft.world.level.block.Block;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import org.agmas.pathsrole.content.block.DonationBoxBlock;
import org.agmas.pathsrole.content.block.FlowerDollBlock;
import org.agmas.pathsrole.content.block.ShrineDonationBoxBlock;

public class ModBlocks {
    public static final Block DONATION_BOX = new DonationBoxBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.WOOD).noOcclusion());
    public static final Block SHRINE_DONATION_BOX = new ShrineDonationBoxBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.WOOD).noOcclusion());
    public static final Block FLOWER_DOLL = new FlowerDollBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(0.5f).sound(SoundType.WOOL).noOcclusion().instabreak());

    public static void init() {
        Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath("pathsrole", "donation_box"), DONATION_BOX);
        Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath("pathsrole", "shrine_donation_box"), SHRINE_DONATION_BOX);
        Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath("pathsrole", "flower_doll"), FLOWER_DOLL);
    }
}