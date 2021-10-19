package dev.ftb.powerpots;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import net.darkhax.botanypots.BotanyPots;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.client.model.generators.*;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeLootTableProvider;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = PowerPots.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PowerPotsData {
    @SubscribeEvent
    public static void dataGenEvent(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();

        if (event.includeClient()) {
            gen.addProvider(new LangGen(gen, PowerPots.MOD_ID, "en_us"));
            gen.addProvider(new ItemModelGens(gen, PowerPots.MOD_ID, event.getExistingFileHelper()));
            gen.addProvider(new BlockStateGen(gen, PowerPots.MOD_ID, event.getExistingFileHelper()));
        }

        if (event.includeServer()) {
            gen.addProvider(new BlockTagGen(gen, event.getExistingFileHelper()));
            gen.addProvider(new RecipesGen(gen));
            gen.addProvider(new LootTableGen(gen));
        }
    }

    private static class LangGen extends LanguageProvider {
        public LangGen(DataGenerator gen, String modid, String locale) {
            super(gen, modid, locale);
        }

        @Override
        protected void addTranslations() {
            this.add("itemGroup." + PowerPots.MOD_ID, "Power Pots");

            this.addBlock(PowerPots.POWER_POT_MK1, "PowerPot MK1");
            this.addBlock(PowerPots.POWER_POT_MK2, "PowerPot MK2");
            this.addBlock(PowerPots.POWER_POT_MK3, "PowerPot MK3");
            this.addBlock(PowerPots.POWER_POT_MK4, "PowerPot MK4");
        }
    }

    private static class BlockStateGen extends BlockStateProvider {
        public BlockStateGen(DataGenerator generator, String modid, ExistingFileHelper existingFileHelper) {
            super(generator, modid, existingFileHelper);
        }

        @Override
        protected void registerStatesAndModels() {
            block(PowerPots.POWER_POT_MK1, modLoc("blocks/t1"));
            block(PowerPots.POWER_POT_MK2, modLoc("blocks/t2"));
            block(PowerPots.POWER_POT_MK3, modLoc("blocks/t3"));
            block(PowerPots.POWER_POT_MK4, modLoc("blocks/t4"));
        }

        private void block(Supplier<Block> block, ResourceLocation texture) {
            horizontalBlock(block.get(), models().getBuilder(block.get().getRegistryName().getPath())
                    .parent(new ModelFile.ExistingModelFile(modLoc("block/power-pot"), this.models().existingFileHelper)).texture("base", texture).texture("particle", texture));
        }
    }

    private static class ItemModelGens extends ItemModelProvider {
        public ItemModelGens(DataGenerator generator, String modid, ExistingFileHelper existingFileHelper) {
            super(generator, modid, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            this.simpleItem(PowerPots.POWER_POT_MK1_ITEM);
            this.simpleItem(PowerPots.POWER_POT_MK2_ITEM);
            this.simpleItem(PowerPots.POWER_POT_MK3_ITEM);
            this.simpleItem(PowerPots.POWER_POT_MK4_ITEM);
        }

        private void simpleItem(Supplier<Item> item) {
            String path = item.get().getRegistryName().getPath();
            getBuilder(path).parent(new ModelFile.UncheckedModelFile(this.modLoc("block/" + path)));
        }
    }

    private static class RecipesGen extends RecipeProvider {
        public RecipesGen(DataGenerator generatorIn) {
            super(generatorIn);
        }

        @Override
        protected void buildShapelessRecipes(Consumer<FinishedRecipe> consumer) {
//            ShapedRecipeBuilder.shaped(PowerPots.POWER_POT_MK1_ITEM.get())
//                    .unlockedBy("has_item", has(BotanyPots.instance.getContent().basicBotanyPot))
//                    .group(PowerPots.MOD_ID + ":pot")
//                    .pattern("SSS")
//                    .pattern("SCS")
//                    .pattern("SSS")
//                    .define('S', Items.COBBLESTONE)
//                    .define('C', BotanyPots.instance.getContent().basicBotanyPot)
//                    .save(consumer);
        }
    }

    private static class LootTableGen extends ForgeLootTableProvider {
        private final List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> lootTables = Lists.newArrayList(
                com.mojang.datafixers.util.Pair.of(BlockLootGen::new, LootContextParamSets.BLOCK)
        );

        public LootTableGen(DataGenerator dataGeneratorIn) {
            super(dataGeneratorIn);
        }

        @Override
        protected List<com.mojang.datafixers.util.Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> getTables() {
            return this.lootTables;
        }
    }

    private static class BlockTagGen extends BlockTagsProvider {
        public BlockTagGen(DataGenerator generatorIn, @Nullable ExistingFileHelper existingFileHelper) {
            super(generatorIn, PowerPots.MOD_ID, existingFileHelper);
        }

        @Override
        protected void addTags() {
            Tags.IOptionalNamedTag<Block> botanyPots = BlockTags.createOptional(new ResourceLocation("botanypots:botany_pots"));
            tag(botanyPots)
                    .add(PowerPots.POWER_POT_MK1.get(), PowerPots.POWER_POT_MK2.get(), PowerPots.POWER_POT_MK3.get(), PowerPots.POWER_POT_MK4.get());
        }
    }


    public static class BlockLootGen extends BlockLoot {
        private final Map<ResourceLocation, LootTable.Builder> tables = Maps.newHashMap();

        @Override
        protected void addTables() {
            this.dropSelf(PowerPots.POWER_POT_MK1.get());
            this.dropSelf(PowerPots.POWER_POT_MK2.get());
            this.dropSelf(PowerPots.POWER_POT_MK3.get());
            this.dropSelf(PowerPots.POWER_POT_MK4.get());
        }

        @Override
        public void accept(BiConsumer<ResourceLocation, LootTable.Builder> consumer) {
            this.addTables();

            for (ResourceLocation rs : new ArrayList<>(this.tables.keySet())) {
                if (rs != BuiltInLootTables.EMPTY) {
                    LootTable.Builder builder = this.tables.remove(rs);

                    if (builder == null) {
                        throw new IllegalStateException(String.format("Missing loottable '%s'", rs));
                    }

                    consumer.accept(rs, builder);
                }
            }

            if (!this.tables.isEmpty()) {
                throw new IllegalStateException("Created block loot tables for non-blocks: " + this.tables.keySet());
            }
        }

        @Override
        protected void add(Block blockIn, LootTable.Builder table) {
            this.tables.put(blockIn.getLootTable(), table);
        }
    }
}
