package enchantsources;

import com.seedfinding.mcfeature.loot.LootContext;
import com.seedfinding.mcfeature.loot.LootPool;
import com.seedfinding.mcfeature.loot.LootTable;
import com.seedfinding.mcfeature.loot.entry.EmptyEntry;
import com.seedfinding.mcfeature.loot.entry.ItemEntry;
import com.seedfinding.mcfeature.loot.function.EnchantWithLevelsFunction;
import com.seedfinding.mcfeature.loot.function.SetCountFunction;
import com.seedfinding.mcfeature.loot.item.Items;
import com.seedfinding.mcfeature.loot.roll.ConstantRoll;
import com.seedfinding.mcseed.lcg.LCG;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Supplier;

public class StrongholdChests {

    public static void findCorridorChest(long lootSeed) {
        StrongholdSeedReverser reverser = new StrongholdSeedReverser()
                .withTargetResultCount(2000)
                .withFeatureSalt(50000);

        var results = reverser.reverseCorridorLootSeed(lootSeed);

        // write all the results to a file
        try (FileWriter fout = new FileWriter("src/main/resources/stronghold_corridor_chest.txt", true)) {
            for (var result : results) {
                fout.append(result.getFirst() + " " + result.getSecond().getX() + " " + result.getSecond().getZ() + "\n");
            }
        }
        catch (Exception ex) {
            System.err.println("StrongholdChests.findCorridorChest: " + ex.getMessage());
        }
    }


    public static List<Long> getLootSeedsForLootTable(String inputFilename, LootTable table) {
        ArrayList<Long> results = new ArrayList<>();
        List<Long> lootSeeds = parseLootSeeds(inputFilename);

        for (long lootSeed : lootSeeds) {
            LootContext ctx = new LootContext(lootSeed);
            Items.ENCHANTED_BOOK.getEnchantments().clear(); // workaround for mc_feature_java bug
            table.generate(ctx).forEach(is -> {
                if (is.getItem().getEnchantments().size() >= 11)
                    results.add(lootSeed);
            });
        }

        return results;
    }

    private static List<Long> parseLootSeeds(String inputFilename) {
        // get the resource at inputFilename
        URL resource = StrongholdChests.class.getClassLoader().getResource(inputFilename);
        if (resource == null) {
            System.err.println("JungleTempleChest.getValidLootSeedsFromFile: File not found");
            return List.of();
        }
        File file = new File(resource.getFile());

        try (Scanner fin = new Scanner(file)) {
            ArrayList<Long> lootSeeds = new ArrayList<>();
            LootContext ctx = new LootContext(0L);

            while (fin.hasNextLong()) {
                long iseed = fin.nextLong();
                fin.skip("  ->  ");
                fin.nextInt();

                // go back a couple of calls and check if the chest will generate the book
                // you could get a lot more loot seeds by repeating this in a loop over
                // different rng offsets, -2 just covers the case when it's the first generated item
                ctx.setSeed(iseed, false);
                ctx.advance(-2); // step back item choice and loot pool count choice
                long lootSeed = ctx.getSeed() ^ LCG.JAVA.multiplier;
                lootSeeds.add(lootSeed);
            }

            return lootSeeds;
        }
        catch (Exception ex) {
            System.err.println("StrongholdChests.parseLootSeeds: " + ex.getMessage());
        }
        return List.of();
    }

    // -----------------------------------------
    // loot tables copied from mc_feature_java
    // and modified to only generate one item

    // MCLootTables.STRONGHOLD_CORRIDOR_CHEST
    public static final Supplier<LootTable> CORRIDOR_ONE_ITEM = () -> new LootTable(
            new LootPool(new ConstantRoll(1),
                    new EmptyEntry(1), // skip 1 prng call
                    new EmptyEntry(1)
                    ),
            new LootPool(new ConstantRoll(1),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.ENDER_PEARL, 10),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.DIAMOND, 3).apply(version -> SetCountFunction.uniform(1.0F, 3.0F)),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.IRON_INGOT, 10).apply(version -> SetCountFunction.uniform(1.0F, 5.0F)),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.GOLD_INGOT, 5).apply(version -> SetCountFunction.uniform(1.0F, 3.0F)),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.REDSTONE, 5).apply(version -> SetCountFunction.uniform(4.0F, 9.0F)),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.BREAD, 15).apply(version -> SetCountFunction.uniform(1.0F, 3.0F)),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.APPLE, 15).apply(version -> SetCountFunction.uniform(1.0F, 3.0F)),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.IRON_PICKAXE, 5),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.IRON_SWORD, 5),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.IRON_CHESTPLATE, 5),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.IRON_HELMET, 5),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.IRON_LEGGINGS, 5),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.IRON_BOOTS, 5),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.GOLDEN_APPLE),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.SADDLE),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.IRON_HORSE_ARMOR),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.GOLDEN_HORSE_ARMOR),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.DIAMOND_HORSE_ARMOR),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.BOOK).apply(version -> new EnchantWithLevelsFunction(com.seedfinding.mcfeature.loot.item.Items.BOOK, 30, 30, true).apply(version)))
    );

    // MCLootTables.STRONGHOLD_CROSSING_CHEST
    public static final Supplier<LootTable> CROSSING_ONE_ITEM = () -> new LootTable(
            new LootPool(new ConstantRoll(1),
                    new EmptyEntry(1), // skip 1 prng call
                    new EmptyEntry(1)
            ),
            new LootPool(new ConstantRoll(1),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.IRON_INGOT, 10).apply(version -> SetCountFunction.uniform(1.0F, 5.0F)),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.GOLD_INGOT, 5).apply(version -> SetCountFunction.uniform(1.0F, 3.0F)),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.REDSTONE, 5).apply(version -> SetCountFunction.uniform(4.0F, 9.0F)),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.COAL, 10).apply(version -> SetCountFunction.uniform(3.0F, 8.0F)),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.BREAD, 15).apply(version -> SetCountFunction.uniform(1.0F, 3.0F)),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.APPLE, 15).apply(version -> SetCountFunction.uniform(1.0F, 3.0F)),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.IRON_PICKAXE),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.BOOK).apply(version -> new EnchantWithLevelsFunction(com.seedfinding.mcfeature.loot.item.Items.BOOK, 30, 30, true).apply(version)))
    );

    // MCLootTables.STRONGHOLD_LIBRARY_CHEST
    public static final Supplier<LootTable> LIBRARY_ONE_ITEM = () -> new LootTable(
            new LootPool(new ConstantRoll(1),
                    new EmptyEntry(1), // skip 1 prng call
                    new EmptyEntry(1)
            ),
            new LootPool(new ConstantRoll(1),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.BOOK, 20).apply(version -> SetCountFunction.uniform(1.0F, 3.0F)),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.PAPER, 20).apply(version -> SetCountFunction.uniform(2.0F, 7.0F)),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.MAP),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.COMPASS),
                    new ItemEntry(com.seedfinding.mcfeature.loot.item.Items.BOOK, 10).apply(version -> new EnchantWithLevelsFunction(Items.BOOK, 30, 30, true).apply(version)))
    );
}
