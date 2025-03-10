package finder;

import com.seedfinding.latticg.reversal.DynamicProgram;
import com.seedfinding.latticg.reversal.calltype.java.JavaCalls;
import com.seedfinding.latticg.util.LCG;
import com.seedfinding.mccore.util.data.Pair;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.loot.LootContext;
import com.seedfinding.mcfeature.loot.LootPool;
import com.seedfinding.mcfeature.loot.LootTable;
import com.seedfinding.mcfeature.loot.enchantment.Enchantments;
import com.seedfinding.mcfeature.loot.entry.EmptyEntry;
import com.seedfinding.mcfeature.loot.entry.ItemEntry;
import com.seedfinding.mcfeature.loot.function.EnchantWithLevelsFunction;
import com.seedfinding.mcfeature.loot.function.SetCountFunction;
import com.seedfinding.mcfeature.loot.item.Item;
import com.seedfinding.mcfeature.loot.item.ItemStack;
import com.seedfinding.mcfeature.loot.item.Items;
import com.seedfinding.mcfeature.loot.roll.ConstantRoll;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public class MaxSwordFinder {

    public static void graphSharpnessFromEffectiveLevel() {
        EnchantWithLevelsFunction fun = new EnchantWithLevelsFunction(Items.DIAMOND_SWORD, 20, 39);
        fun.applyEnchantment(Enchantments.getFor(MCVersion.v1_16_1));

        for (int lvl = 0; lvl < 2 * 39; lvl++) {
            var aer = fun.getAvailableEnchantmentResults(lvl);
            for (var enchantment : aer) {
                if (enchantment.getName().equals("sharpness")) {
                    System.out.println(lvl + " -> sharpness " + enchantment.getLevel());
                }
            }
        }
    }

    public static void printAERForLevel(int effectiveLevel) {
        EnchantWithLevelsFunction fun = new EnchantWithLevelsFunction(Items.DIAMOND_SWORD, 20, 39);
        fun.applyEnchantment(Enchantments.getFor(MCVersion.v1_16_1));
        var aer = fun.getAvailableEnchantmentResults(effectiveLevel);
        for (var enchantment : aer) {
            System.out.println(enchantment.getName() + " " + enchantment.getLevel());
        }
    }

    // ----------------------------------------------------------------------

    // maxed-out smite sword loot seed
    // 76343060228305
    // ItemStack{item=Item{name='diamond_sword', enchantments=[(smite, 5), (unbreaking, 3), (sweeping, 3), (looting, 3), (mending, 1), (knockback, 2), (fire_aspect, 2)], effects=[]}, count=1}
    public static void findMaxSwordSeeds() {
        DynamicProgram device = DynamicProgram.create(LCG.JAVA);

        // skip random level
        device.skip(1);

        // skip level bonus, assume it's "good"
        device.skip(2);

        // very good amplifier
        //device.add(JavaCalls.nextFloat().betweenII(0.9f, 1.0f));
        //device.add(JavaCalls.nextFloat().betweenII(0.9f, 1.0f));

        // any amplifier
        device.skip(2);

        // guaranteed enchantment
        device.skip(1);

        // make sure we get 6 more enchantments
        for (int i = 0; i < 6; i++) {
            device.add(JavaCalls.nextInt(50).betweenII(0, i >= 4 ? 0 : 1));
            device.skip(1);
        }

        device.reverse().forEach(seed -> {
            EnchantWithLevelsFunction fun = new EnchantWithLevelsFunction(Items.DIAMOND_SWORD, 20, 39);
            fun.applyEnchantment(Enchantments.getFor(MCVersion.v1_16_1));
            LootContext ctx = new LootContext(seed ^ LCG.JAVA.multiplier, MCVersion.v1_16_1);
            ItemStack sword = new ItemStack(new Item(Items.DIAMOND_SWORD.getName()));
            fun.process(sword, ctx);
            if (sword.getItem().getEnchantments().size() >= 7) {
                synchronized (System.out) {
                    boolean goodEnchant = false;
                    for (var ench : sword.getItem().getEnchantments()) {
                        if (ench.getFirst().equals("sharpness") && ench.getSecond() == 4) goodEnchant = true;
                        if (ench.getFirst().equals("bane_of_arthropods") && ench.getSecond() == 5) goodEnchant = true;
                        if (ench.getFirst().equals("smite") && ench.getSecond() == 5) goodEnchant = true;
                        if (ench.getFirst().equals("vanishing_curse")) return;
                    }
                    if (!goodEnchant) return;

                    // System.out.println(seed);
                    // test if we can get it from chest
                    ctx.setSeed(seed, false);
                    ctx.advance(-2);

                    final long lootSeed = ctx.getSeed() ^ LCG.JAVA.multiplier;
                    END_CITY_TREASURE_CHEST_KINDA.get().generate(ctx).forEach(itemStack -> {
                        if (itemStack.getItem().equalsName(Items.DIAMOND_SWORD)) {
                            // check enchantment order
                            for (int i = 0; i < itemStack.getItem().getEnchantments().size() - 1; i++) {
                                var ench = itemStack.getItem().getEnchantments().get(i);
                                var enchNext = itemStack.getItem().getEnchantments().get(i + 1);

                                if (ench.getSecond() < enchNext.getSecond())
                                    return;
//                                if (ench.getSecond().equals(enchNext.getSecond()) && getInGameLength(ench) < getInGameLength(enchNext))
//                                    return;
                            }

                            System.out.println(lootSeed);
                            System.out.println(itemStack.getItem().getEnchantments());
                        }
                    });

//                    for (var ench : sword.getItem().getEnchantments()) {
//                        System.out.println(ench.getFirst() + " " + ench.getSecond());
//                    }
                }
            }
            else {
                // System.out.println("fail");
            }
        });
    }

    // ----------------------------------------------------------------------

    private static final Supplier<LootTable> END_CITY_TREASURE_CHEST_KINDA = () -> new LootTable(
            // simulate uniform roll without adding any items to the loot table
            new LootPool(new ConstantRoll(1), new EmptyEntry(1), new EmptyEntry(1)),

            new LootPool(new ConstantRoll(1),
                    new ItemEntry(Items.DIAMOND, 5).apply(version -> SetCountFunction.uniform(2.0F, 7.0F)),
                    new ItemEntry(Items.IRON_INGOT, 10).apply(version -> SetCountFunction.uniform(4.0F, 8.0F)),
                    new ItemEntry(Items.GOLD_INGOT, 15).apply(version -> SetCountFunction.uniform(2.0F, 7.0F)),
                    new ItemEntry(Items.EMERALD, 2).apply(version -> SetCountFunction.uniform(2.0F, 6.0F)),
                    new ItemEntry(Items.BEETROOT_SEEDS, 5).apply(version -> SetCountFunction.uniform(1.0F, 10.0F)),
                    new ItemEntry(Items.SADDLE, 3),
                    new ItemEntry(Items.IRON_HORSE_ARMOR),
                    new ItemEntry(Items.GOLDEN_HORSE_ARMOR),
                    new ItemEntry(Items.DIAMOND_HORSE_ARMOR),
                    new ItemEntry(Items.DIAMOND_SWORD, 3).apply(version -> new EnchantWithLevelsFunction(Items.DIAMOND_SWORD, 20, 39, true).apply(version)),
                    new ItemEntry(Items.DIAMOND_BOOTS, 3).apply(version -> new EnchantWithLevelsFunction(Items.DIAMOND_BOOTS, 20, 39, true).apply(version)),
                    new ItemEntry(Items.DIAMOND_CHESTPLATE, 3).apply(version -> new EnchantWithLevelsFunction(Items.DIAMOND_CHESTPLATE, 20, 39, true).apply(version)),
                    new ItemEntry(Items.DIAMOND_LEGGINGS, 3).apply(version -> new EnchantWithLevelsFunction(Items.DIAMOND_LEGGINGS, 20, 39, true).apply(version)),
                    new ItemEntry(Items.DIAMOND_HELMET, 3).apply(version -> new EnchantWithLevelsFunction(Items.DIAMOND_HELMET, 20, 39, true).apply(version)),
                    new ItemEntry(Items.DIAMOND_PICKAXE, 3).apply(version -> new EnchantWithLevelsFunction(Items.DIAMOND_PICKAXE, 20, 39, true).apply(version)),
                    new ItemEntry(Items.DIAMOND_SHOVEL, 3).apply(version -> new EnchantWithLevelsFunction(Items.DIAMOND_SHOVEL, 20, 39, true).apply(version)),
                    new ItemEntry(Items.IRON_SWORD, 3).apply(version -> new EnchantWithLevelsFunction(Items.IRON_SWORD, 20, 39, true).apply(version)),
                    new ItemEntry(Items.IRON_BOOTS, 3).apply(version -> new EnchantWithLevelsFunction(Items.IRON_BOOTS, 20, 39, true).apply(version)),
                    new ItemEntry(Items.IRON_CHESTPLATE, 3).apply(version -> new EnchantWithLevelsFunction(Items.IRON_CHESTPLATE, 20, 39, true).apply(version)),
                    new ItemEntry(Items.IRON_LEGGINGS, 3).apply(version -> new EnchantWithLevelsFunction(Items.IRON_LEGGINGS, 20, 39, true).apply(version)),
                    new ItemEntry(Items.IRON_HELMET, 3).apply(version -> new EnchantWithLevelsFunction(Items.IRON_HELMET, 20, 39, true).apply(version)),
                    new ItemEntry(Items.IRON_PICKAXE, 3).apply(version -> new EnchantWithLevelsFunction(Items.IRON_PICKAXE, 20, 39, true).apply(version)),
                    new ItemEntry(Items.IRON_SHOVEL, 3).apply(version -> new EnchantWithLevelsFunction(Items.IRON_SHOVEL, 20, 39, true).apply(version))
            )
    );

    private static int getInGameLength(Pair<String, Integer> ench) {
        int len = ench.getFirst().length();
        if (ench.getFirst().equals("sweeping"))
            len = "sweeping_edge".length();

        if (ench.getSecond() == 3) len += 3;
        if (ench.getSecond() == 2 || ench.getSecond() == 4) len += 2;
        if (ench.getSecond() == 1 || ench.getSecond() == 5) len += 1;

        return len;
    }
}
