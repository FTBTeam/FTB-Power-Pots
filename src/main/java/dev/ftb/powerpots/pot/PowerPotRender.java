package dev.ftb.powerpots.pot;

import com.mojang.blaze3d.vertex.PoseStack;
import net.darkhax.bookshelf.block.DisplayableBlockState;
import net.darkhax.botanypots.BotanyPots;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PowerPotRender extends BlockEntityRenderer<PowerPotTileBase> {

    private static final Direction[] SOIL_SIDES = new Direction[]{Direction.UP};
    private static final Direction[] CROP_SIDES = new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public PowerPotRender(BlockEntityRenderDispatcher dispatcher) {

        super(dispatcher);
    }

    @Override
    public void render(PowerPotTileBase tile, float partial, PoseStack matrix, MultiBufferSource buffer, int light, int overlay) {

        if (tile.getSoil() != null && BotanyPots.CLIENT_CONFIG.shouldRenderSoil()) {

            matrix.pushPose();
            matrix.scale(0.5f, 0.3f, 0.5f);
            matrix.translate(0.5, 0.01, 0.5);
            tile.getSoil().getRenderState().render(tile.getLevel(), tile.getBlockPos(), matrix, buffer, light, overlay, SOIL_SIDES);
            matrix.popPose();
        }

        if (tile.getCrop() != null && BotanyPots.CLIENT_CONFIG.shouldRenderCrop()) {

            matrix.pushPose();
            matrix.translate(0.5, 0.30, 0.5);

            if (BotanyPots.CLIENT_CONFIG.shouldDoGrowthAnimation()) {
                final float partialOffset = tile.getCurrentGrowthTicks() < tile.getTotalGrowthTicks() ? partial : 0f;
                final float progressScale = 0.25f + (tile.getCurrentGrowthTicks() + partialOffset) / tile.getTotalGrowthTicks() * 0.75f;
                final float growth = Mth.clamp(progressScale * 0.55f, 0, 1.0F);
                matrix.scale(growth, growth, growth);
            }

            matrix.translate(-0.5, 0, -0.5);

            final DisplayableBlockState[] cropStates = tile.getCrop().getDisplayState();

            for (int i = 0; i < cropStates.length; i++) {

                matrix.translate(0, i, 0);
                cropStates[i].render(tile.getLevel(), tile.getBlockPos(), matrix, buffer, light, overlay, CROP_SIDES);
                matrix.translate(0, -i, 0);
            }

            matrix.popPose();
        }
    }
}