package finder;

import com.seedfinding.latticg.reversal.DynamicProgram;
import com.seedfinding.latticg.reversal.calltype.java.JavaCalls;
import com.seedfinding.latticg.util.LCG;
import com.seedfinding.mcmath.util.Mth;
import com.seedfinding.mcseed.rand.JRand;

public class EnchantFinder {
    public static int getEnchantCount(JRand rand, int level, int enchantability) {
        level += 1 + rand.nextInt(1 + enchantability / 4) + rand.nextInt(1 + enchantability / 4); // 2
        float amplifier = (rand.nextFloat() + rand.nextFloat() - 1.0f) * 0.15f; // 2
        level = Mth.clamp(Math.round((float)level + (float)level * amplifier), 1, Integer.MAX_VALUE);
        int enchCount = 0;

        rand.nextSeed(); // 1
        enchCount++;
        while(rand.nextInt(50) <= level) {
            rand.nextSeed();
            enchCount++;
            level /= 2;
        }

        return enchCount;
    }

    public static void findNEnchantSeeds(int n) {
        DynamicProgram device = DynamicProgram.create(LCG.JAVA);

        // pre-enchant picking calls, includes 1 guaranteed enchantment
        device.skip(5);

        // 5 enchantments that happen before the rare ones (32, 16, 8, 4, 2)
        device.skip(10);

        // 1 enchantment for nextInt(50) <= 1
        device.add(JavaCalls.nextInt(50).betweenII(0, 1));
        device.skip(1);

        // n - 7 enchantments remain, need nextInt(50) == 0 for each of them
        for (int i = 0; i < n - 7; i++) {
            device.add(JavaCalls.nextInt(50).equalTo(0));
            device.skip(1);
        }

        device.reverse().forEach(seed -> {
            JRand rand = JRand.ofInternalSeed(seed);
            int enchCount = getEnchantCount(rand, 30, 1);
            if (enchCount >= n) {
                System.out.println(seed + "  ->  " + enchCount);
            }
        });
    }
}
