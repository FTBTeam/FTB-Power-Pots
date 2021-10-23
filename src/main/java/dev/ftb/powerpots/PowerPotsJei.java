package dev.ftb.powerpots;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.darkhax.botanypots.addons.jei.CategoryCrop;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class PowerPotsJei implements IModPlugin {
    public PowerPotsJei() {
    }

    public ResourceLocation getPluginUid() {
        return new ResourceLocation("ftb-power-pots", "jei");
    }

    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(PowerPots.POWER_POT_MK1.get()), CategoryCrop.ID);
        registration.addRecipeCatalyst(new ItemStack(PowerPots.POWER_POT_MK2.get()), CategoryCrop.ID);
        registration.addRecipeCatalyst(new ItemStack(PowerPots.POWER_POT_MK3.get()), CategoryCrop.ID);
        registration.addRecipeCatalyst(new ItemStack(PowerPots.POWER_POT_MK4.get()), CategoryCrop.ID);
    }
}