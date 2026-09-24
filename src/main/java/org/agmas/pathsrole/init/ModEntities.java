package org.agmas.pathsrole.init;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.content.entity.ThrownFlowerDoll;
import org.agmas.pathsrole.content.entity.YinYangOrbEntity;

public class ModEntities {
    public static final EntityType<YinYangOrbEntity> YIN_YANG_ORB = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        PathsRoleMod.id("yin_yang_orb"),
        FabricEntityTypeBuilder.create(MobCategory.MISC, YinYangOrbEntity::new)
            .dimensions(EntityDimensions.fixed(0.5f, 0.5f))
            .trackRangeBlocks(4)
            .trackedUpdateRate(1)
            .build()
    );

    public static final EntityType<ThrownFlowerDoll> THROWN_FLOWER_DOLL = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        PathsRoleMod.id("thrown_flower_doll"),
        FabricEntityTypeBuilder.<ThrownFlowerDoll>create(MobCategory.MISC, ThrownFlowerDoll::new)
            .dimensions(EntityDimensions.fixed(0.5f, 0.5f))
            .trackRangeBlocks(8)
            .trackedUpdateRate(2)
            .build()
    );

    public static void init() {
    }
}