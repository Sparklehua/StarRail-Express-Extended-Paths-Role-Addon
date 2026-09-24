package org.agmas.pathsrole.init;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.agmas.pathsrole.content.item.AsIWriteItem;
import org.agmas.pathsrole.content.item.BanListItem;
import org.agmas.pathsrole.content.item.BroomItem;
import org.agmas.pathsrole.content.item.GoheiItem;
import org.agmas.pathsrole.content.item.FlowerDollItem;
import org.agmas.pathsrole.content.item.PeepingEyeItem;
import org.agmas.pathsrole.content.item.RageShipperItem;
import org.agmas.pathsrole.content.item.ShipperBookItem;
import org.agmas.pathsrole.content.item.ShipperPistolItem;
import org.agmas.pathsrole.content.item.SpellCardItem;
import org.agmas.pathsrole.content.item.ShrineItem;
import org.agmas.pathsrole.content.item.TargetRevolverItem;
import org.agmas.pathsrole.content.item.WantedPosterItem;
import org.agmas.pathsrole.init.ModBlocks;

public class ModItems {
    public static final BlockItem DONATION_BOX = new BlockItem(ModBlocks.DONATION_BOX, new Item.Properties().stacksTo(3));
    public static final BlockItem SHRINE_DONATION_BOX = new BlockItem(ModBlocks.SHRINE_DONATION_BOX, new Item.Properties().stacksTo(64));
    public static final ShipperBookItem SHIPPER_BOOK = new ShipperBookItem(new Item.Properties().stacksTo(1));
    public static final RageShipperItem RAGE_SHIPPER = new RageShipperItem(new Item.Properties().stacksTo(1));
    public static final ShipperPistolItem SHIPPER_PISTOL = new ShipperPistolItem(new Item.Properties().stacksTo(1));
    public static final WantedPosterItem WANTED_POSTER = new WantedPosterItem(new Item.Properties().stacksTo(1));
    public static final TargetRevolverItem TARGET_REVOLVER = new TargetRevolverItem(new Item.Properties().stacksTo(1));
    public static final Item HUNT_TEMP_SHIELD = new Item(new Item.Properties().stacksTo(1));
    public static final GoheiItem REIMU_GOHEI = new GoheiItem(new Item.Properties().stacksTo(1));
    public static final AsIWriteItem AS_I_WRITE = new AsIWriteItem(new Item.Properties().stacksTo(1));
    public static final SpellCardItem REIMU_SPELL_CARD = new SpellCardItem(new Item.Properties().stacksTo(1));
    public static final ShrineItem SHRINE = new ShrineItem(new Item.Properties().stacksTo(1));
    public static final BroomItem BROOM = new BroomItem(new Item.Properties().durability(3));
    public static final BanListItem BAN_LIST = new BanListItem(new Item.Properties().stacksTo(1));
    public static final PeepingEyeItem PEEPING_EYE = new PeepingEyeItem(new Item.Properties().stacksTo(1));
    public static final FlowerDollItem FLOWER_DOLL = new FlowerDollItem(ModBlocks.FLOWER_DOLL, new Item.Properties().stacksTo(1));

    public static final ResourceKey<CreativeModeTab> FLOWER_ROLE_ITEMS_KEY = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        ResourceLocation.fromNamespaceAndPath("pathsrole", "flower_role_items")
    );

    public static final CreativeModeTab FLOWER_ROLE_ITEMS = FabricItemGroup.builder()
        .icon(() -> new ItemStack(SHIPPER_BOOK))
        .title(Component.translatable("itemGroup.pathsrole.flower_role_items"))
        .displayItems((parameters, output) -> {
            output.accept(DONATION_BOX);
            output.accept(SHRINE_DONATION_BOX);
            output.accept(REIMU_GOHEI);
            output.accept(SHIPPER_BOOK);
            output.accept(RAGE_SHIPPER);
            output.accept(SHIPPER_PISTOL);
            output.accept(WANTED_POSTER);
            output.accept(TARGET_REVOLVER);
            output.accept(HUNT_TEMP_SHIELD);
            output.accept(AS_I_WRITE);
            output.accept(REIMU_SPELL_CARD);
            output.accept(SHRINE);
            output.accept(BROOM);
            output.accept(BAN_LIST);
            output.accept(PEEPING_EYE);
            output.accept(FLOWER_DOLL);
        })
        .build();

    public static void init() {
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "donation_box"), DONATION_BOX);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "shrine_donation_box"), SHRINE_DONATION_BOX);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "shipper_book"), SHIPPER_BOOK);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "rage_shipper"), RAGE_SHIPPER);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "shipper_pistol"), SHIPPER_PISTOL);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "wanted_poster"), WANTED_POSTER);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "target_revolver"), TARGET_REVOLVER);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "hunt_temp_shield"), HUNT_TEMP_SHIELD);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "reimu_gohei"), REIMU_GOHEI);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "as_i_write"), AS_I_WRITE);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "reimu_spell_card"), REIMU_SPELL_CARD);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "shrine"), SHRINE);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "broom"), BROOM);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "ban_list"), BAN_LIST);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "peeping_eye"), PEEPING_EYE);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("pathsrole", "flower_doll"), FLOWER_DOLL);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, FLOWER_ROLE_ITEMS_KEY, FLOWER_ROLE_ITEMS);
    }
}