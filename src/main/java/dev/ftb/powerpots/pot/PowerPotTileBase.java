package dev.ftb.powerpots.pot;

import net.darkhax.bookshelf.util.InventoryUtils;
import net.darkhax.botanypots.BotanyPotHelper;
import net.darkhax.botanypots.BotanyPots;
import net.darkhax.botanypots.block.BlockBotanyPot;
import net.darkhax.botanypots.block.tileentity.TileEntityBotanyPot;
import net.darkhax.botanypots.crop.CropInfo;
import net.darkhax.botanypots.network.BreakEffectsMessage;
import net.darkhax.botanypots.soil.SoilInfo;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Literally all of this code excluding the power modifications are from the original BotanyPot Tile code,
 * all this has done is allowed me to inject my own logic in to the default flow to support power modifications.
 * <p>
 * Complete credit to Darkhax for 80% of this classes code.
 */
public class PowerPotTileBase extends TileEntityBotanyPot {
    static class InternalInventoryHandler extends ItemStackHandler {
        PowerPotTileBase tile;

        public InternalInventoryHandler(PowerPotTileBase tile) {
            super(6);
            this.tile = tile;
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }

        @NotNull
        public ItemStack internalInsertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            tile.sync(false);
        }
    }

    /**
     * A useless inventory used to allow other blocks to visually connect to the botany pot.
     */
    private InternalInventoryHandler inventory;
    private final LazyOptional<InternalInventoryHandler> inventoryLazy;

    private final PotEnergyStorage energy;
    private final LazyOptional<PotEnergyStorage> energyLazy;

    /**
     * The current soil in the botany pot. Can be null.
     */
    @Nullable
    private SoilInfo soil;

    private ItemStack soilStack = ItemStack.EMPTY;

    /**
     * The current crop in the botany pot. Can be null.
     */
    @Nullable
    private CropInfo crop;

    private ItemStack cropStack = ItemStack.EMPTY;

    /**
     * The total growth ticks for the crop to mature. -1 means it's not growing.
     */
    private int totalGrowthTicks;

    /**
     * The total growth ticks for the crop. -1 means it's not growing.
     */
    private int currentGrowthTicks;

    /**
     * A cooldown for the auto harvest.
     */
    private int autoHarvestCooldown;

    /**
     * The current chunk pos. Not saved to nbt.
     */
    private ChunkPos chunkPos;

    private PotTier tier;

    public PowerPotTileBase(PotTier tier) {
        super();

        this.tier = tier;

        inventory = new InternalInventoryHandler(this);
        inventoryLazy = LazyOptional.of(() -> this.inventory);

        this.energy = new PotEnergyStorage(this, tier.config.maxEnergy.get());
        this.energyLazy = LazyOptional.of(() -> this.energy);
    }

    /**
     * Checks if the soil can be set. This is always true if the new soil is null. Otherwise it
     * is only true if there isn't already soil.
     *
     * @param newSoil The soil to set. Null will delete the existing soil.
     * @return Whether or not the soil can be set.
     */
    @Override
    public boolean canSetSoil(@Nullable SoilInfo newSoil) {
        return newSoil == null || this.getSoil() == null;
    }

    /**
     * Sets the soil in the pot. If the new soil is null it will set the pot to having no soil.
     * This will also reset the growth timer. When called on the server the pot will be synced
     * to the client.
     *
     * @param newSoil The new soil to set in the pot.
     */
    @Override
    public void setSoil(@Nullable SoilInfo newSoil, ItemStack stack) {
        this.soil = newSoil;
        this.soilStack = stack;
        this.resetGrowthTime();

        if (!this.level.isClientSide) {

            this.sync(false);
            this.level.getChunkSource().getLightEngine().checkBlock(this.worldPosition);
        }
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        super.onDataPacket(net, packet);
        this.level.getLightEngine().checkBlock(this.worldPosition);
    }

    /**
     * Checks if a crop can be set in the pot. You can always set a crop to null. Otherwise
     * there must be a soil, and there must not be an existing crop.
     *
     * @param newCrop The new crop to set.
     * @return Whether or not the crop can be set.
     */
    @Override
    public boolean canSetCrop(@Nullable CropInfo newCrop) {

        return newCrop == null || this.getSoil() != null && this.getCrop() == null;
    }

    /**
     * Sets the crop inside the pot. If set to null the crop will be set to nothing. This will
     * reset the growth time. If called on the server the tile will be synced to the client.
     *
     * @param newCrop The new crop to set.
     */
    @Override
    public void setCrop(@Nullable CropInfo newCrop, ItemStack stack) {

        this.crop = newCrop;
        this.cropStack = stack;
        this.resetGrowthTime();

        if (!this.level.isClientSide) {

            this.sync(false);
        }
    }

    @Nullable
    @Override
    public SoilInfo getSoil() {
        return this.soil;
    }

    /**
     * Gets the crop in the pot. Null means no crop.
     *
     * @return The crop in the pot.
     */
    @Nullable
    @Override
    public CropInfo getCrop() {
        return this.crop;
    }

    /**
     * Gets the total required growth ticks For the crop to become mature enough to harvest..
     *
     * @return The total required growth ticks. -1 means no growth.
     */
    @Override
    public int getTotalGrowthTicks() {
        return this.totalGrowthTicks;
    }

    /**
     * Gets the current amount of ticks the crop has grown.
     *
     * @return The current amount of ticks the crop has grown. -1 means no growth.
     */
    @Override
    public int getCurrentGrowthTicks() {

        return this.currentGrowthTicks;
    }

    /**
     * Checks if the crop in the pot can be harvested.
     *
     * @return Whether or not the crop can be harvested.
     */
    @Override
    public boolean canHarvest() {

        return this.crop != null && this.getTotalGrowthTicks() > 0 && this.getCurrentGrowthTicks() >= this.getTotalGrowthTicks();
    }

    /**
     * Resets the current growth ticks and the total growth ticks for the crop in the pot.
     */
    @Override
    public void resetGrowthTime() {

        // Recalculate total growth ticks to account for any data changes
        int requiredGrowthTicks = BotanyPotHelper.getRequiredGrowthTicks(this.getCrop(), this.getSoil());

        this.totalGrowthTicks = requiredGrowthTicks - (int) (requiredGrowthTicks * this.tier.config.speedModifier.get());
//        System.out.println(requiredGrowthTicks);
//        System.out.println(requiredGrowthTicks - (requiredGrowthTicks * this.tier.config.speedModifier.get()));

        // Reset the growth time.
        this.currentGrowthTicks = 0;

        // To help deal with desyncs caused by reload, every reset will also reset the
        // cached
        // soila and crop references.
        if (this.soil != null) {

            this.soil = BotanyPotHelper.getSoil(this.soil.getId());

            // Check if the soil was removed. If so kill the crop, because crop needs a
            // soil.
            if (this.soil == null) {

                this.crop = null;
            }
        }

        if (this.crop != null) {

            this.crop = BotanyPotHelper.getCrop(this.crop.getId());
        }

        this.autoHarvestCooldown = 5;
        this.level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());

        if (!this.level.isClientSide) {

            this.sync(false);
        }
    }

    /**
     * Adds growth to the crop. This is primarily used for things like bone meal. If this is
     * called on the server it will cause a client sync.
     *
     * @param ticksToGrow The amount of ticks to add.
     */
    @Override
    public void addGrowth(int ticksToGrow) {
        this.currentGrowthTicks += ticksToGrow;
        if (this.currentGrowthTicks > this.totalGrowthTicks) {
            this.currentGrowthTicks = this.totalGrowthTicks;
        }

        if (!this.level.isClientSide) {
            this.sync(false);
        }
    }

    @Override
    public float getGrowthPercent() {
        if (this.totalGrowthTicks == -1 || this.currentGrowthTicks == -1) {
            return 0f;
        }

        return (float) this.currentGrowthTicks / this.totalGrowthTicks;
    }

    @Override
    public void onTileTick() {
        if (this.level.isClientSide) {
            return;
        }

        if (hasInternalItems()) {
            final IItemHandler foundInventory = InventoryUtils.getInventory(this.level, this.worldPosition.below(), Direction.UP);
            if (foundInventory != EmptyHandler.INSTANCE) {
                for (int i = 0; i < this.inventory.getSlots(); i++) {
                    ItemStack stackInSlot = this.inventory.getStackInSlot(i);
                    ItemStack itemStack = ItemHandlerHelper.insertItemStacked(foundInventory, stackInSlot, false);
                    this.inventory.extractItem(i, stackInSlot.getCount() - itemStack.getCount(), false);
                }
            }
        }

        if (this.hasSoilAndCrop()) {
            if (this.isDoneGrowing()) {
                this.level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
                this.attemptAutoHarvest();
            }
            // It's not done growing
            else {
                boolean hasSpace = false;
                for (int i = 0; i < this.inventory.getSlots(); i++) {
                    if (this.inventory.getStackInSlot(i).isEmpty()) {
                        hasSpace = true;
                        break;
                    }
                }

                int energyAvailable = this.energy.consumeEnergy(this.tier.config.perTickEnergy.get(), true);
                if (energyAvailable >= this.tier.config.perTickEnergy.get() && hasSpace) {
                    this.energy.consumeEnergy(this.tier.config.perTickEnergy.get(), false);
                    this.currentGrowthTicks++;
                }
            }
        } else if (this.totalGrowthTicks != -1 || this.currentGrowthTicks != 0) {
            // Reset tick counts
            this.resetGrowthTime();
        }
    }

    @Override
    public boolean hasSoilAndCrop() {
        return this.soil != null && this.crop != null;
    }

    @Override
    public boolean isDoneGrowing() {
        return this.hasSoilAndCrop() && this.totalGrowthTicks > 0 && this.currentGrowthTicks >= this.totalGrowthTicks;
    }

    private boolean hasInternalItems() {
        boolean hasItems = false;
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            if (!this.inventory.getStackInSlot(i).isEmpty()) {
                hasItems = true;
                break;
            }
        }

        return hasItems;
    }

    private void attemptAutoHarvest() {
        final Block block = this.getBlockState().getBlock();
        if (block instanceof BlockBotanyPot && ((BlockBotanyPot) block).isHopper()) {
            if (this.autoHarvestCooldown > 0) {
                this.autoHarvestCooldown--;
                return;
            }

            final IItemHandler foundInventory = InventoryUtils.getInventory(this.level, this.worldPosition.below(), Direction.UP);

            if (!this.level.isClientSide) {
                boolean didAutoHarvest = false;
                // Support for auto dumping into chests and other direct inventories that aren't pipes
                if (foundInventory != EmptyHandler.INSTANCE) {
                    didAutoHarvest = extractToInventory(foundInventory);
                }

                // If the above operation failed, use internal inventory for pipes
                if (!didAutoHarvest) {
                    for (final ItemStack item : BotanyPotHelper.generateDrop(this.level.random, this.getCrop())) {
                        item.setCount(this.tier.config.itemsPerOutput.get());

                        for (int i = 0; i < this.inventory.getSlots(); i++) {
                            ItemStack itemStack = this.inventory.internalInsertItem(i, item, false);
                            if (itemStack.isEmpty()) {
                                didAutoHarvest = true;
                                break;
                            }
                        }
                    }
                }

                if (didAutoHarvest) {
                    this.onCropHarvest();
                    this.resetGrowthTime();
                }
            }
        }
    }

    private boolean extractToInventory(IItemHandler inventory) {
        boolean didExtract = false;

        for (final ItemStack item : BotanyPotHelper.generateDrop(this.level.random, this.getCrop())) {
            item.setCount(this.tier.config.itemsPerOutput.get());
            boolean extracted = extractItemsToInventory(inventory, item);
            if (extracted && !didExtract) {
                // mark it as done, even if we have more to drop, voiding is fine here
                didExtract = true;
            }
        }

        return didExtract;
    }

    public static boolean extractItemsToInventory(IItemHandler inventory, ItemStack item) {
        boolean didInsert = false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            // Check if the simulated insert stack can be accepted into the
            // inventory.
            if (inventory.isItemValid(slot, item) && (inventory.insertItem(slot, item, true).getCount() != item.getCount() || (inventory instanceof InternalInventoryHandler && ((InternalInventoryHandler) inventory).internalInsertItem(slot, item, true).getCount() != item.getCount()))) {
                // Actually insert the stack.
                // Insert the items. We don't care about the remainder and
                // it can be safely voided.
                if (inventory instanceof InternalInventoryHandler) {
                    ((InternalInventoryHandler) inventory).internalInsertItem(slot, item, false);
                } else {
                    inventory.insertItem(slot, item, false);
                }

                // Set auto harvest to true. This will cause a reset for
                // the next growth cycle.
                didInsert = true;

                // Exit the inventory for this loop. Will then move on to
                // the next item and start over.
                break;
            }
        }
        return didInsert;
    }

    @Override
    public void onCropHarvest() {

        if (this.hasSoilAndCrop()) {

            final ChunkAccess chunk = this.level.getChunk(this.worldPosition);

            if (chunk instanceof LevelChunk) {

                // TODO remove the need to cast this.
                BotanyPots.NETWORK.sendToChunk((LevelChunk) chunk, new BreakEffectsMessage(this.worldPosition, this.crop.getDisplayState()[0].getState()));
            }
        }

        MinecraftForge.EVENT_BUS.post(new BlockEvent.CropGrowEvent.Post(this.level, this.worldPosition, this.getState(), this.getState()));
    }

    @Override
    public void serialize(CompoundTag dataTag) {

        if (this.soil != null) {

            dataTag.putString("Soil", this.soil.getId().toString());

            // Crop is only saved if there is a soil
            if (this.crop != null) {

                dataTag.putString("Crop", this.crop.getId().toString());

                // Tick info is only saved if there is a crop and a soil.
                dataTag.putInt("GrowthTicks", this.currentGrowthTicks);
            }
        }

        // This is stupid but I'm lazy
        energyLazy.ifPresent(e -> dataTag.putInt("energy", e.serializeNBT().getInt("energy")));
        inventoryLazy.ifPresent(e -> dataTag.put("inventory", e.serializeNBT()));

        dataTag.put("CropStack", this.cropStack.serializeNBT());
        dataTag.put("SoilStack", this.soilStack.serializeNBT());
    }

    @Override
    public void deserialize(CompoundTag dataTag) {
        this.soil = null;
        this.crop = null;

        energyLazy.ifPresent(e -> e.deserializeNBT(dataTag));
        inventoryLazy.ifPresent(e -> e.deserializeNBT(dataTag));

        if (dataTag.contains("CropStack")) {

            this.cropStack = ItemStack.of(dataTag.getCompound("CropStack"));
        }

        if (dataTag.contains("SoilStack")) {

            this.soilStack = ItemStack.of(dataTag.getCompound("SoilStack"));
        }

        if (dataTag.contains("Soil")) {

            final String rawSoilId = dataTag.getString("Soil");
            final ResourceLocation soilId = ResourceLocation.tryParse(rawSoilId);

            if (soilId != null) {

                final SoilInfo foundSoil = BotanyPotHelper.getSoil(soilId);

                if (foundSoil != null) {

                    this.soil = foundSoil;

                    // Crops are only loaded if the soil exists.
                    if (dataTag.contains("Crop")) {

                        final String rawCropId = dataTag.getString("Crop");
                        final ResourceLocation cropId = ResourceLocation.tryParse(rawCropId);

                        if (cropId != null) {

                            final CropInfo cropInfo = BotanyPotHelper.getCrop(cropId);

                            if (cropInfo != null) {

                                this.crop = cropInfo;

                                // Growth ticks are only loaded if a crop and soil exist.
                                this.currentGrowthTicks = dataTag.getInt("GrowthTicks");

                                // Reset total growth ticks on tile load to account for data
                                // changes.

                                int growthTicksForSoil = this.crop.getGrowthTicksForSoil(this.soil);
                                this.totalGrowthTicks = growthTicksForSoil - (int) (growthTicksForSoil * this.tier.config.speedModifier.get());
                            } else {

                                BotanyPots.LOGGER.error("Botany Pot at {} had a crop of type {} but that crop does not exist. The crop will be discarded.", this.worldPosition, rawCropId);
                            }
                        } else {

                            BotanyPots.LOGGER.error("Botany Pot at {} has an invalid crop Id of {}. The crop will be discarded.", this.worldPosition, rawCropId);
                        }
                    }
                } else {

                    BotanyPots.LOGGER.error("Botany Pot at {} had a soil of type {} which no longer exists. Soil and crop will be discarded.", this.worldPosition, rawSoilId);
                }
            } else {

                BotanyPots.LOGGER.error("Botany Pot at {} has invalid soil type {}. Soil and crop will be discarded.", this.worldPosition, rawSoilId);
            }
        }
    }

    @Override
    public ItemStack getSoilStack() {
        return this.soilStack;
    }

    @Override
    public ItemStack getCropStack() {
        return this.cropStack;
    }

    @Override
    public ChunkPos getChunkPos() {
        if (this.chunkPos == null) {
            this.chunkPos = new ChunkPos(this.worldPosition);
        }

        return this.chunkPos;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public double getViewDistance() {
        return BotanyPots.CLIENT_CONFIG.getRenderDistance();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            return energyLazy.cast();
        } else if (!this.isRemoved() && cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return inventoryLazy.cast();
        }
        return super.getCapability(cap, side);
    }
}
