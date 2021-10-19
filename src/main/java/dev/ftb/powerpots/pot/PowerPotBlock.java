package dev.ftb.powerpots.pot;

import dev.ftb.powerpots.Config;
import net.darkhax.botanypots.BotanyPotHelper;
import net.darkhax.botanypots.block.BlockBotanyPot;
import net.darkhax.botanypots.block.tileentity.TileEntityBotanyPot;
import net.darkhax.botanypots.crop.CropInfo;
import net.darkhax.botanypots.fertilizer.FertilizerInfo;
import net.darkhax.botanypots.soil.SoilInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.stream.Stream;

public class PowerPotBlock extends BlockBotanyPot {
    private final VoxelShape NORTH = Stream.of(Block.box(4, 0, 4, 12, 1, 12), Block.box(3, 1, 3, 13, 6, 4), Block.box(3, 1, 12, 13, 6, 13), Block.box(12, 1, 4, 13, 6, 12), Block.box(3, 1, 4, 4, 6, 12), Block.box(2, 0, 5, 3, 2, 11), Block.box(3, 0, 5, 4, 1, 11), Block.box(2, 2, 6, 3, 4, 11)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private final VoxelShape SOUTH = Stream.of(Block.box(4, 0, 4, 12, 1, 12), Block.box(3, 1, 12, 13, 6, 13), Block.box(3, 1, 3, 13, 6, 4), Block.box(3, 1, 4, 4, 6, 12), Block.box(12, 1, 4, 13, 6, 12), Block.box(13, 0, 5, 14, 2, 11), Block.box(12, 0, 5, 13, 1, 11), Block.box(13, 2, 5, 14, 4, 10)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private final VoxelShape EAST = Stream.of(Block.box(4, 0, 4, 12, 1, 12), Block.box(12, 1, 3, 13, 6, 13), Block.box(3, 1, 3, 4, 6, 13), Block.box(4, 1, 12, 12, 6, 13), Block.box(4, 1, 3, 12, 6, 4), Block.box(5, 0, 2, 11, 2, 3), Block.box(5, 0, 3, 11, 1, 4), Block.box(5, 2, 2, 10, 4, 3)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private final VoxelShape WEST = Stream.of(Block.box(4, 0, 4, 12, 1, 12), Block.box(3, 1, 3, 4, 6, 13), Block.box(12, 1, 3, 13, 6, 13), Block.box(4, 1, 3, 12, 6, 4), Block.box(4, 1, 12, 12, 6, 13), Block.box(5, 0, 13, 11, 2, 14), Block.box(5, 0, 12, 11, 1, 13), Block.box(6, 2, 13, 11, 4, 14)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    private final Config.PotMkConfig tier;

    public PowerPotBlock(Config.PotMkConfig tier) {
        super(true);
        this.tier = tier;

        registerDefaultState(getStateDefinition().any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        Direction value = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (value == Direction.NORTH) return NORTH;
        if (value == Direction.EAST) return EAST;
        if (value == Direction.SOUTH) return SOUTH;
        return WEST;
    }

    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.hasTileEntity() && state.getBlock() != newState.getBlock()) {

            final BlockEntity tileEntity = worldIn.getBlockEntity(pos);
            if (tileEntity instanceof TileEntityBotanyPot) {
                final TileEntityBotanyPot pot = (TileEntityBotanyPot) tileEntity;
                if (pot.getSoil() != null) {
                    dropItem(pot.getSoilStack(), worldIn, pos);
                }

                if (pot.getCrop() != null) {
                    dropItem(pot.getCropStack(), worldIn, pos);
                }

                tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(e -> {
                    for (int i = 0; i < e.getSlots(); i++) {
                        if (!e.getStackInSlot(i).isEmpty()) {
                            dropItem(e.getStackInSlot(i), worldIn, pos);
                        }
                    }
                });
            }
        }

        if (state.hasTileEntity() && (!state.is(newState.getBlock()) || !newState.hasTileEntity())) {
            worldIn.removeBlockEntity(pos);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if (world.isClientSide) {

            // Forces all the logic to run on the server. Returning fail or pass on the
            // client will cause the click packet not to be sent to the server.
            return InteractionResult.SUCCESS;
        }

        final BlockEntity tile = world.getBlockEntity(pos);

        if (tile instanceof TileEntityBotanyPot) {
            final TileEntityBotanyPot pot = (TileEntityBotanyPot) tile;

            // Attempt removal
            if (player.isShiftKeyDown()) {
                final CropInfo crop = pot.getCrop();
                // If a crop exists, remove it.
                if (crop != null) {
                    final ItemStack seedStack = pot.getCropStack();
                    if (!seedStack.isEmpty() && pot.canSetCrop(null)) {
                        pot.setCrop(null, ItemStack.EMPTY);
                        dropItem(seedStack.copy(), world, pos);

                        tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(e -> {
                            for (int i = 0; i < e.getSlots(); i++) {
                                if (!e.getStackInSlot(i).isEmpty()) {
                                    dropItem(e.getStackInSlot(i), world, pos);
                                    e.extractItem(i, e.getStackInSlot(i).getCount(), false);
                                }
                            }
                        });

                        return InteractionResult.SUCCESS;
                    }

                }
                else {
                    final SoilInfo soil = pot.getSoil();
                    if (soil != null) {
                        final ItemStack soilStack = pot.getSoilStack();

                        if (!soilStack.isEmpty() && pot.canSetSoil(null)) {
                            pot.setSoil(null, ItemStack.EMPTY);
                            dropItem(soilStack.copy(), world, pos);
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }
            // Attempt to insert or harvest
            else {
                final ItemStack heldItem = player.getItemInHand(hand);

                // Attempt to insert the item. If something is inserted true is returned and
                // the method ends. If the item can't be inserted the method will continue and
                // the crop will try to be harvested.
                if (!heldItem.isEmpty()) {

                    // Attempt soil add first
                    if (pot.getSoil() == null) {
                        final SoilInfo soilForStack = BotanyPotHelper.getSoilForItem(heldItem);
                        if (soilForStack != null && pot.canSetSoil(soilForStack)) {
                            final ItemStack inStack = heldItem.copy();
                            inStack.setCount(1);
                            pot.setSoil(soilForStack, inStack);

                            if (!player.isCreative()) {
                                heldItem.shrink(1);
                            }

                            return InteractionResult.SUCCESS;
                        }
                    }
                    // Attempt crop add second.
                    else if (pot.getCrop() == null) {
                        final CropInfo cropForStack = BotanyPotHelper.getCropForItem(heldItem);

                        if (cropForStack != null && BotanyPotHelper.isSoilValidForCrop(pot.getSoil(), cropForStack) && pot.canSetCrop(cropForStack)) {
                            final ItemStack inStack = heldItem.copy();
                            inStack.setCount(1);

                            pot.setCrop(cropForStack, inStack);

                            if (!player.isCreative()) {
                                heldItem.shrink(1);
                            }

                            return InteractionResult.SUCCESS;
                        }
                    }
                    // Attempt fertilizer.
                    else if (!pot.canHarvest()) {
                        final FertilizerInfo fertilizerForStack = BotanyPotHelper.getFertilizerForItem(heldItem);

                        if (fertilizerForStack != null) {

                            final int ticksToGrow = fertilizerForStack.getTicksToGrow(world.random, pot.getSoil(), pot.getCrop());
                            pot.addGrowth(ticksToGrow);

                            if (!world.isClientSide) {
                                world.globalLevelEvent(2005, tile.getBlockPos(), 0);
                            }

                            if (!player.isCreative()) {
                                heldItem.shrink(1);
                            }

                            return InteractionResult.SUCCESS;
                        }
                    }
                }

                // Check if the pot can be harvested
                if (!this.isHopper() && pot.canHarvest()) {
                    pot.onCropHarvest();
                    pot.resetGrowthTime();

                    for (final ItemStack stack : BotanyPotHelper.generateDrop(world.random, pot.getCrop())) {
                        dropItem(stack, world, pos);
                    }

                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.FAIL;
    }

    @Override
    public BlockEntity createTileEntity(BlockState state, BlockGetter world) {
        return new PowerPotTile(this.getTier());
    }

    public Config.PotMkConfig getTier() {
        return tier;
    }
}
