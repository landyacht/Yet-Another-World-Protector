package de.z0rdak.regionshield.handler;

import de.z0rdak.regionshield.RegionShield;
import de.z0rdak.regionshield.util.StickType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static de.z0rdak.regionshield.util.StickUtil.*;

@Mod.EventBusSubscriber(modid = RegionShield.MODID)
public class StickInteractionHandler {

    private StickInteractionHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getWorld().isClientSide) {
            PlayerEntity player = event.getPlayer();
            ItemStack involvedItemStack = event.getItemStack();
            if (!involvedItemStack.equals(ItemStack.EMPTY) && isVanillaStick(involvedItemStack)) {
                StickType stickType = getStickType(involvedItemStack);

                boolean isShiftPressed = player.isShiftKeyDown();
                BlockRayTraceResult blockRayTraceResult = event.getHitVec();
                BlockPos pos = blockRayTraceResult.getBlockPos();
                RayTraceResult.Type traceResultType = blockRayTraceResult.getType();

                switch (stickType) {
                    case MARKER:
                        MarkerStickHandler.onMarkBlock(involvedItemStack, event);
                        break;
                    case REGION_STICK:
                        break;
                    case FLAG_STICK:
                        break;
                    default:
                        break;
                }
            }
        }
        // TODO: check block and handle stick action accordingly
        // TODO: rendering and charge use needs to be implemented in stickitem mixin
    }

    public static boolean hasNonNullTag(ItemStack itemStack){
        return itemStack.hasTag() && itemStack.getTag() != null;
    }

    @SubscribeEvent
    public static void onCycleMode(PlayerInteractEvent.RightClickItem event) {
        if (!event.getWorld().isClientSide) {
            ItemStack involvedItemStack = event.getItemStack();
            // is some valid mod stick
            if (!involvedItemStack.equals(ItemStack.EMPTY)
                    && hasNonNullTag(involvedItemStack)
                    && involvedItemStack.getTag().contains(STICK)) {
                RayTraceResult blockLookingAt = event.getPlayer().pick(20.0d, 0.0f, false);
                boolean targetIsAir;
                if (blockLookingAt.getType() == RayTraceResult.Type.BLOCK) {
                    BlockPos blockpos = ((BlockRayTraceResult) blockLookingAt).getBlockPos();
                    BlockState blockstate = event.getWorld().getBlockState(blockpos);
                    targetIsAir = blockstate.getBlock().equals(Blocks.AIR);
                } else {
                    targetIsAir = blockLookingAt.getType() == RayTraceResult.Type.MISS;
                }

                if (event.getPlayer().isShiftKeyDown() && targetIsAir) {
                    StickType stickType = getStickType(involvedItemStack);
                    switch (stickType) {
                        case REGION_STICK:
                            RegionStickHandler.onCycleRegionStick(involvedItemStack);
                            break;
                        case FLAG_STICK:
                            FlagStickHandler.onCycleFlagStick(involvedItemStack);
                            break;
                        case MARKER:
                            MarkerStickHandler.onCycleRegionMarker(involvedItemStack);
                            break;
                        case UNKNOWN:
                        default:
                            break;
                    }
                }
            }
        }
    }

    /**
     * Handles action when renaming mod sticks in an anvil.
     * This is used to create a mod stick or to define a region by renaming a valid RegionMarker stick.
     */
    @SubscribeEvent
    public static void onStickRename(AnvilRepairEvent event) {
        PlayerEntity player = event.getPlayer();
        if (!player.getCommandSenderWorld().isClientSide) {
            ItemStack outputItem = event.getItemResult();
            ItemStack inputItem = event.getItemInput();
            ItemStack ingredientInput = event.getIngredientInput();
            boolean hasStickTag = outputItem.hasTag() && outputItem.getTag() != null && outputItem.getTag().contains(STICK);
            if (hasStickTag) {
                MarkerStickHandler.onCreateRegion(event);
            }
            boolean isInputAndOutputStick = ItemStack.isSame(outputItem, Items.STICK.getDefaultInstance())
                    && ItemStack.isSame(inputItem, Items.STICK.getDefaultInstance());
            if (isInputAndOutputStick && ingredientInput.isEmpty()) {
                onCreateStick(event);
            }
        }
    }

    /**
     * Edits the NBT data of the renamed stick to "transform" it to the corresponding mod stick.
     * @param event the event data from renaming the stick item
     */
    private static void onCreateStick(AnvilRepairEvent event) {
        PlayerEntity player = event.getPlayer();
        ItemStack outputItem = event.getItemResult();
        ItemStack inputItem = event.getItemInput();
        StickType type = StickType.of(outputItem.getHoverName().getString());
        if (type != StickType.UNKNOWN) {
            // split stack and only create one stick, also refund xp
            inputItem.setCount(outputItem.getCount() - 1);
            player.addItem(inputItem);
            // TODO: Send network packet to force inventory sync
            player.giveExperienceLevels(1);
            outputItem.setCount(1);
            // init NBT
            initStickTag(outputItem, type, event.getPlayer().getCommandSenderWorld().dimension());
            setStickName(outputItem, type);
            setStickToolTip(outputItem, type);
            applyEnchantmentGlint(outputItem);
        }
    }
}
