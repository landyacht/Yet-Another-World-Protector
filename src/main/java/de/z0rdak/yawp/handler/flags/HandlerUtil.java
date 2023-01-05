package de.z0rdak.yawp.handler.flags;

import de.z0rdak.yawp.core.flag.IFlag;
import de.z0rdak.yawp.core.flag.RegionFlag;
import de.z0rdak.yawp.core.region.DimensionalRegion;
import de.z0rdak.yawp.core.region.IMarkableRegion;
import de.z0rdak.yawp.util.LocalRegions;
import de.z0rdak.yawp.util.MessageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FlyingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.ShulkerEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;
import java.util.stream.Collectors;

import static de.z0rdak.yawp.util.LocalRegions.getInvolvedRegionFor;

public final class HandlerUtil {

    private HandlerUtil(){}

    public static RegistryKey<World> getEntityDim(Entity entity){
        return entity.getCommandSenderWorld().dimension();
    }

    public static boolean isAnimal(Entity entity){
        return entity instanceof AnimalEntity || entity instanceof WaterMobEntity;
    }

    public static boolean isServerSide(EntityEvent event) {
        return isServerSide(event.getEntity());
    }

    public static boolean isServerSide(BlockEvent event) {
        return !event.getWorld().isClientSide();
    }

    public static boolean isServerSide(Entity entity) {
        return !entity.getCommandSenderWorld().isClientSide();
    }

    public static boolean isVillager(Entity entity) {
        return entity instanceof AbstractVillagerEntity;
    }

    public static boolean isPlayer(Entity entity) {
        return entity instanceof PlayerEntity;
    }

    public static boolean isMonster(Entity entity) {
        return entity instanceof MonsterEntity
                || entity instanceof SlimeEntity
                || entity instanceof FlyingEntity
                || entity instanceof EnderDragonEntity
                || entity instanceof ShulkerEntity;
    }

    /**
     * Checks is any region contains the specified flag
     * @param regions regions to check for
     * @param flag flag to be checked for
     * @return true if any region contains the specified flag, false otherwise
     */
    public static boolean anyRegionContainsFlag(List<IMarkableRegion> regions, RegionFlag flag){
        return regions.stream()
                .anyMatch(region -> region.containsFlag(flag));
    }

    /**
     * Filters affected blocks from explosion event which are in a region with the specified flag.
     * @param event detonation event
     * @param flag flag to be filtered for
     * @return list of block positions which are in a region with the specified flag
     */
    // TODO: rework
    public static List<BlockPos> filterExplosionAffectedBlocks(ExplosionEvent.Detonate event, RegionFlag flag){
        return event.getAffectedBlocks().stream()
                .filter(blockPos -> anyRegionContainsFlag(
                        LocalRegions.getRegionsFor(flag, blockPos, event.getWorld().dimension()), flag))
                .collect(Collectors.toList());
    }

    // TODO: rework
    public static List<Entity> filterAffectedEntities(ExplosionEvent.Detonate event, RegionFlag flag) {
        return event.getAffectedEntities().stream()
                .filter(entity -> anyRegionContainsFlag(
                        LocalRegions.getRegionsFor(flag, entity.blockPosition(), event.getWorld().dimension()), flag))
                .collect(Collectors.toList());
    }


    public static boolean handleAndSendMsg(Event event, FlagCheckEvent.PlayerFlagEvent flagCheck) {
        if (flagCheck.getLocalRegion() == null && flagCheck.isDeniedInDim()) {
            MessageUtil.sendDimFlagNotification(flagCheck.getPlayer(), flagCheck.getFlag());
        }
        if (flagCheck.isDeniedLocal()) {
            if (!flagCheck.getLocalRegion().isMuted()) {
                MessageUtil.sendFlagNotification(flagCheck.getPlayer(), flagCheck.getLocalRegion(), flagCheck.getFlag());
            }
        }
        event.setCanceled(flagCheck.isDenied());
        return flagCheck.isDenied();
    }

    public static boolean sendFlagDeniedMsg(FlagCheckEvent.PlayerFlagEvent flagCheck) {
        if (flagCheck.getLocalRegion() == null && flagCheck.isDeniedInDim()) {
            // TODO: Muted property for dimensions | config option | don't display ?
            MessageUtil.sendDimFlagNotification(flagCheck.getPlayer(), flagCheck.getFlag());
        }
        if (flagCheck.isDeniedLocal()) {
            if (!flagCheck.getLocalRegion().isMuted()) {
                MessageUtil.sendFlagNotification(flagCheck.getPlayer(), flagCheck.getLocalRegion(), flagCheck.getFlag());
            }
        }
        return flagCheck.isDenied();
    }

    public static boolean sendFlagDeniedMsg(FlagCheckEvent flagCheck, PlayerEntity player) {
        if (flagCheck.getLocalRegion() == null && flagCheck.isDeniedInDim()) {
            // TODO: Muted property for dimensions | config option | don't display ?
            MessageUtil.sendDimFlagNotification(player, flagCheck.getFlag());
        }
        if (flagCheck.isDeniedLocal()) {
            if (!flagCheck.getLocalRegion().isMuted()) {
                MessageUtil.sendFlagNotification(player, flagCheck.getLocalRegion(), flagCheck.getFlag());
            }
        }
        return flagCheck.isDenied();
    }

    public static FlagCheckEvent.PlayerFlagEvent checkPlayerEvent(PlayerEntity player, BlockPos target, RegionFlag regionFlag, DimensionalRegion dimRegion) {
        IMarkableRegion involvedRegion = getInvolvedRegionFor(regionFlag, target, player, player.level.dimension());
        FlagCheckEvent.PlayerFlagEvent flagCheck = new FlagCheckEvent.PlayerFlagEvent(player, dimRegion, involvedRegion, regionFlag);
        if (involvedRegion == null) {
            flagCheck.setDeniedLocal(false);
        } else {
            IFlag flag = involvedRegion.getFlag(regionFlag.name);
            // TODO: Check state with allowed
            flagCheck.setDeniedLocal(flag.isActive());
        }
        if (dimRegion.isActive()) {
            if (dimRegion.containsFlag(regionFlag) && !dimRegion.permits(player)) {
                IFlag flag = dimRegion.getFlag(regionFlag.name);
                // TODO: Check state with allowed
                flagCheck.setDeniedInDim(flag.isActive());
            } else {
                flagCheck.setDeniedInDim(false);
            }
        } else {
            flagCheck.setDeniedInDim(false);
        }

        if (flagCheck.getLocalRegion() == null) {
            flagCheck.setDenied(flagCheck.isDeniedInDim());
            return flagCheck;
        } else {
            boolean deniedResult = flagCheck.isDeniedInDim() && flagCheck.isDeniedLocal() || (!flagCheck.isDeniedInDim() || flagCheck.isDeniedLocal()) && (!flagCheck.isDeniedInDim() && (flagCheck.isDeniedLocal()) || !flagCheck.isDeniedInDim() && !flagCheck.isDeniedLocal());
            flagCheck.setDenied(deniedResult);
            return flagCheck;
        }
    }

    public static FlagCheckEvent checkTargetEvent(BlockPos target, RegionFlag regionFlag, DimensionalRegion dimRegion) {
        IMarkableRegion involvedRegion = getInvolvedRegionFor(regionFlag, target, dimRegion.getDim());
        FlagCheckEvent flagCheck = new FlagCheckEvent(dimRegion, involvedRegion, regionFlag);
        if (involvedRegion == null) {
            flagCheck.setDeniedLocal(false);
        } else {
            IFlag flag = involvedRegion.getFlag(regionFlag.name);
            // TODO: Check state with allowed
            flagCheck.setDeniedLocal(flag.isActive());
        }
        if (dimRegion.isActive()) {
            if (dimRegion.containsFlag(regionFlag)) {
                IFlag flag = dimRegion.getFlag(regionFlag.name);
                // TODO: Check state with allowed
                flagCheck.setDeniedInDim(flag.isActive());
            } else {
                flagCheck.setDeniedInDim(false);
            }
        } else {
            flagCheck.setDeniedInDim(false);
        }

        if (flagCheck.getLocalRegion() == null) {
            flagCheck.setDenied(flagCheck.isDeniedInDim());
            return flagCheck;
        } else {
            boolean deniedResult = flagCheck.isDeniedInDim() && flagCheck.isDeniedLocal() || (!flagCheck.isDeniedInDim() || flagCheck.isDeniedLocal()) && (!flagCheck.isDeniedInDim() && (flagCheck.isDeniedLocal()) || !flagCheck.isDeniedInDim() && !flagCheck.isDeniedLocal());
            flagCheck.setDenied(deniedResult);
            return flagCheck;
        }
    }
}
