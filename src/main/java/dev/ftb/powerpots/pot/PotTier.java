package dev.ftb.powerpots.pot;

import dev.ftb.powerpots.Config;

public enum PotTier {
    MK1(Config.POT_MK1),
    MK2(Config.POT_MK2),
    MK3(Config.POT_MK3),
    MK4(Config.POT_MK4);

    final Config.PotMkConfig config;
    PotTier(Config.PotMkConfig configProvider) {
        this.config = configProvider;
    }

}
