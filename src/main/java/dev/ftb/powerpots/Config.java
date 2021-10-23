package dev.ftb.powerpots;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec COMMON_CONFIG;

    public static final PotMkConfig POT_MK1 = new PotMkConfig("mk1", .2, 100000, 500, 16);
    public static final PotMkConfig POT_MK2 = new PotMkConfig("mk2", .4, 200000, 1000, 32);
    public static final PotMkConfig POT_MK3 = new PotMkConfig("mk3", .6, 300000, 1500, 48);
    public static final PotMkConfig POT_MK4 = new PotMkConfig("mk4", .8, 400000, 2000, 64);

    static {
        COMMON_CONFIG = COMMON_BUILDER.build();
    }

    public static class PotMkConfig {
        public final ForgeConfigSpec.DoubleValue speedModifier;
        public final ForgeConfigSpec.IntValue maxEnergy;
        public final ForgeConfigSpec.IntValue perTickEnergy;
        public final ForgeConfigSpec.IntValue itemsPerOutput;

        public PotMkConfig(String tier, double speed, int energy, int cost, int itemsOut) {
            COMMON_BUILDER.push("tier-" + tier);

            this.speedModifier = COMMON_BUILDER
                    .comment(
                            "Modifies the processing time of taking to process a resource: 0 being no speed modifier and 1 being a single tick per resource",
                            "The way this works is by taking the processing time from the soil and crop, then dividing it by the value given here. 0 being 0% and 1 being 100%"
                    )
                    .defineInRange("speedModifier", speed, 0.0, 1.0);

            this.maxEnergy = COMMON_BUILDER
                    .comment("How much energy the pot will hold")
                    .defineInRange("maxEnergy", energy, 0, Integer.MAX_VALUE);

            this.perTickEnergy = COMMON_BUILDER
                    .comment("Energy consumed per tick")
                    .defineInRange("perTickEnergy", cost, 0, Integer.MAX_VALUE);

            this.itemsPerOutput = COMMON_BUILDER
                    .comment("Amount of items outputted per operation")
                    .defineInRange("itemsPerOutput", itemsOut, 1, 64 * 27);

            COMMON_BUILDER.pop();
        }
    }
}
