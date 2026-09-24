package org.agmas.pathsrole.mixin;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.event.AllowGameEnd;
import io.wifi.starrailexpress.game.GameUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.agmas.noellesroles.utils.RoleUtils;
import org.agmas.pathsrole.init.ModRoles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Predicate;

@Mixin(targets = "com.habi.roleport.role.event.PortRoleEvents", remap = false)
public abstract class PortRoleEventsMixin {

    private static final ResourceLocation CORRUPT_COP_ID = ResourceLocation.tryParse("habi_role_port:corrupt_cop");
    private static final ResourceLocation VAMPIRE_ID = ResourceLocation.tryParse("habi_role_port:vampire");
    private static final ResourceLocation BLOOD_SERVANT_ID = ResourceLocation.tryParse("habi_role_port:blood_servant");

    @Inject(method = "registerCorruptCopWin", at = @At("HEAD"), cancellable = true, require = 0)
    private static void replaceCorruptCopWin(CallbackInfo ci) {
        ci.cancel();

        AllowGameEnd.EVENT.register((serverLevel, winStatus, isLooseEndsMode) -> {
            if (serverLevel == null || winStatus == null || isLooseEndsMode || CORRUPT_COP_ID == null) {
                return GameUtils.WinStatus.NOT_MODIFY;
            }
            SREGameWorldComponent game = SREGameWorldComponent.KEY.get(serverLevel);
            if (game == null || !game.isRunning()) {
                return GameUtils.WinStatus.NOT_MODIFY;
            }
            ServerPlayer cop = null;
            int alive = 0;
            for (ServerPlayer p : serverLevel.players()) {
                if (!GameUtils.isPlayerAliveAndSurvival(p)) {
                    continue;
                }
                if (ModRoles.REIMU != null && game.isRole(p, ModRoles.REIMU)) {
                    continue;
                }
                alive++;
                var role = game.getRole(p);
                if (role != null && role.identifier() != null && role.identifier().equals(CORRUPT_COP_ID)) {
                    cop = p;
                }
            }
            if (cop != null && alive == 1) {
                Predicate<Map.Entry<Player, String>> pred = entry -> {
                    if (!(entry.getKey() instanceof ServerPlayer player)) {
                        return false;
                    }
                    SREGameWorldComponent g = SREGameWorldComponent.KEY.get(serverLevel);
                    if (g == null) {
                        return false;
                    }
                    var role = g.getRole(player);
                    return role != null && role.identifier() != null && role.identifier().equals(CORRUPT_COP_ID);
                };
                GameUtils.CustomWinnersPredicates.add(pred);
                var copRole = game.getRole(cop);
                int color = copRole != null ? copRole.color() : 0xFF0000;
                RoleUtils.customWinnerWin(
                        serverLevel,
                        GameUtils.WinStatus.CUSTOM,
                        CORRUPT_COP_ID.getPath(),
                        OptionalInt.of(color));
                return GameUtils.WinStatus.CUSTOM;
            }
            if (cop != null
                    && (winStatus == GameUtils.WinStatus.KILLERS || winStatus == GameUtils.WinStatus.PASSENGERS)) {
                return GameUtils.WinStatus.NONE;
            }
            return GameUtils.WinStatus.NOT_MODIFY;
        });
    }

    @Inject(method = "registerVampireBloodServantWin", at = @At("HEAD"), cancellable = true, require = 0)
    private static void replaceVampireBloodServantWin(CallbackInfo ci) {
        ci.cancel();

        AllowGameEnd.EVENT.register((serverLevel, winStatus, isLooseEndsMode) -> {
            try {
                if (serverLevel == null || winStatus == null || isLooseEndsMode 
                    || VAMPIRE_ID == null || BLOOD_SERVANT_ID == null) {
                    return GameUtils.WinStatus.NOT_MODIFY;
                }
                SREGameWorldComponent game = SREGameWorldComponent.KEY.get(serverLevel);
                if (game == null || !game.isRunning()) {
                    return GameUtils.WinStatus.NOT_MODIFY;
                }

                var players = serverLevel.players();
                if (players == null || players.isEmpty()) {
                    return GameUtils.WinStatus.NOT_MODIFY;
                }

                int vampires = 0;
                int servants = 0;
                int others = 0;

                for (ServerPlayer p : players) {
                    if (p == null) continue;
                    
                    try {
                        if (!GameUtils.isPlayerAliveAndSurvival(p)) {
                            continue;
                        }
                    } catch (Exception e) {
                        continue;
                    }
                    
                    try {
                        if (ModRoles.REIMU != null && game.isRole(p, ModRoles.REIMU)) {
                            continue;
                        }
                    } catch (Exception e) {
                    }
                    
                    try {
                        var role = game.getRole(p);
                        if (role == null || role.identifier() == null) {
                            others++;
                        } else if (VAMPIRE_ID.equals(role.identifier())) {
                            vampires++;
                        } else if (BLOOD_SERVANT_ID.equals(role.identifier())) {
                            servants++;
                        } else {
                            others++;
                        }
                    } catch (Exception e) {
                        others++;
                    }
                }

                if (others == 0 && (vampires > 0 || servants > 0)) {
                    try {
                        Predicate<Map.Entry<Player, String>> pred = entry -> {
                            try {
                                if (entry == null || !(entry.getKey() instanceof ServerPlayer player)) {
                                    return false;
                                }
                                if (player == null) return false;
                                
                                SREGameWorldComponent g = SREGameWorldComponent.KEY.get(serverLevel);
                                if (g == null) {
                                    return false;
                                }
                                var playerRole = g.getRole(player);
                                if (playerRole == null || playerRole.identifier() == null) {
                                    return false;
                                }
                                return VAMPIRE_ID.equals(playerRole.identifier()) 
                                       || BLOOD_SERVANT_ID.equals(playerRole.identifier());
                            } catch (Exception e) {
                                return false;
                            }
                        };
                        
                        if (GameUtils.CustomWinnersPredicates != null) {
                            GameUtils.CustomWinnersPredicates.add(pred);
                        }
                        
                        RoleUtils.customWinnerWin(
                                serverLevel,
                                GameUtils.WinStatus.CUSTOM,
                                VAMPIRE_ID.getPath(),
                                OptionalInt.of(0xFF8B0000));
                        return GameUtils.WinStatus.CUSTOM;
                    } catch (Exception e) {
                        return GameUtils.WinStatus.NOT_MODIFY;
                    }
                }

                if ((vampires > 0 || servants > 0)
                        && (winStatus == GameUtils.WinStatus.KILLERS || winStatus == GameUtils.WinStatus.PASSENGERS)) {
                    return GameUtils.WinStatus.NONE;
                }

                return GameUtils.WinStatus.NOT_MODIFY;
            } catch (Exception e) {
                return GameUtils.WinStatus.NOT_MODIFY;
            }
        });
    }
}