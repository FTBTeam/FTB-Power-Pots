package dev.ftb.powerpots;

import dev.ftb.powerpots.pot.PowerPotBlock;
import dev.ftb.powerpots.pot.PowerPotRender;
import dev.ftb.powerpots.pot.PowerPotTile;
import net.darkhax.botanypots.block.tileentity.RendererBotanyPot;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(PowerPots.MOD_ID)
public class PowerPots {
    public static final String MOD_ID = "ftb-power-pots";

    private static final Logger LOGGER = LogManager.getLogger();

    public static final CreativeModeTab TAB = new CreativeModeTab(MOD_ID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(POWER_POT_MK1.get());
        }
    };

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, MOD_ID);

    public static final RegistryObject<Block> POWER_POT_MK1 = BLOCKS.register("power_pot_mk1", () -> new PowerPotBlock(Config.POT_MK1));
    public static final RegistryObject<Block> POWER_POT_MK2 = BLOCKS.register("power_pot_mk2", () -> new PowerPotBlock(Config.POT_MK2));
    public static final RegistryObject<Block> POWER_POT_MK3 = BLOCKS.register("power_pot_mk3", () -> new PowerPotBlock(Config.POT_MK3));
    public static final RegistryObject<Block> POWER_POT_MK4 = BLOCKS.register("power_pot_mk4", () -> new PowerPotBlock(Config.POT_MK4));

    public static final RegistryObject<Item> POWER_POT_MK4_ITEM = ITEMS.register("power_pot_mk4", () -> new BlockItem(POWER_POT_MK1.get(), new Item.Properties().tab(TAB)));
    public static final RegistryObject<Item> POWER_POT_MK3_ITEM = ITEMS.register("power_pot_mk3", () -> new BlockItem(POWER_POT_MK2.get(), new Item.Properties().tab(TAB)));
    public static final RegistryObject<Item> POWER_POT_MK2_ITEM = ITEMS.register("power_pot_mk2", () -> new BlockItem(POWER_POT_MK3.get(), new Item.Properties().tab(TAB)));
    public static final RegistryObject<Item> POWER_POT_MK1_ITEM = ITEMS.register("power_pot_mk1", () -> new BlockItem(POWER_POT_MK4.get(), new Item.Properties().tab(TAB)));

    public static final RegistryObject<BlockEntityType<PowerPotTile>> POWER_TILE_MK1 = BLOCK_ENTITY.register("power_pot_tile_mk1", () -> BlockEntityType.Builder.of(() -> new PowerPotTile(Config.POT_MK1), POWER_POT_MK1.get()).build(null));
    public static final RegistryObject<BlockEntityType<PowerPotTile>> POWER_TILE_MK2 = BLOCK_ENTITY.register("power_pot_tile_mk2", () -> BlockEntityType.Builder.of(() -> new PowerPotTile(Config.POT_MK2), POWER_POT_MK2.get()).build(null));
    public static final RegistryObject<BlockEntityType<PowerPotTile>> POWER_TILE_MK3 = BLOCK_ENTITY.register("power_pot_tile_mk3", () -> BlockEntityType.Builder.of(() -> new PowerPotTile(Config.POT_MK3), POWER_POT_MK3.get()).build(null));
    public static final RegistryObject<BlockEntityType<PowerPotTile>> POWER_TILE_MK4 = BLOCK_ENTITY.register("power_pot_tile_mk4", () -> BlockEntityType.Builder.of(() -> new PowerPotTile(Config.POT_MK4), POWER_POT_MK4.get()).build(null));

    public PowerPots() {
        IEventBus MOD_BUS = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(MOD_BUS);
        ITEMS.register(MOD_BUS);
        BLOCK_ENTITY.register(MOD_BUS);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);


        MOD_BUS.addListener(this::commonSetup);
        MOD_BUS.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {}

    private void clientSetup(final FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(POWER_POT_MK1.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(POWER_POT_MK2.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(POWER_POT_MK3.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(POWER_POT_MK4.get(), RenderType.cutout());

        ClientRegistry.bindTileEntityRenderer(POWER_TILE_MK1.get(), PowerPotRender::new);
        ClientRegistry.bindTileEntityRenderer(POWER_TILE_MK2.get(), PowerPotRender::new);
        ClientRegistry.bindTileEntityRenderer(POWER_TILE_MK3.get(), PowerPotRender::new);
        ClientRegistry.bindTileEntityRenderer(POWER_TILE_MK4.get(), PowerPotRender::new);
    }
}
