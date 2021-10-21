package dev.ftb.powerpots.pot;

import dev.ftb.powerpots.Config;
import dev.ftb.powerpots.PowerPots;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class PowerPotTile {
    public static class MK1Tile extends PowerPotTileBase {
        public MK1Tile() {
            super(PotTier.MK1);
        }

        @Override
        public BlockEntityType<?> getType() {
            return PowerPots.POWER_TILE_MK1.get();
        }
    }

    public static class MK2Tile extends PowerPotTileBase {
        public MK2Tile() {
            super(PotTier.MK2);
        }

        @Override
        public BlockEntityType<?> getType() {
            return PowerPots.POWER_TILE_MK2.get();
        }
    }

    public static class MK3Tile extends PowerPotTileBase {
        public MK3Tile() {
            super(PotTier.MK3);
        }

        @Override
        public BlockEntityType<?> getType() {
            return PowerPots.POWER_TILE_MK3.get();
        }
    }

    public static class MK4Tile extends PowerPotTileBase {
        public MK4Tile() {
            super(PotTier.MK4);
        }

        @Override
        public BlockEntityType<?> getType() {
            return PowerPots.POWER_TILE_MK4.get();
        }
    }
}
