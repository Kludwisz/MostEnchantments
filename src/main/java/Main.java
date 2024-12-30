import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.version.MCVersion;
import enchantsources.EndCityChest;
import enchantsources.JungleTempleChest;

import java.util.List;

@SuppressWarnings("unused")
public class Main {
    public static void main(String[] args) {
        // EnchantFinder.findNEnchantSeeds(11);
        // EnchantFinder.findNEnchantSeeds(12);
        // results -> seeds_11.txt, seeds_12.txt

        // MaxSwordFinder.graphSharpnessFromEffectiveLevel();
        // results -> sharpness_levels.txt
        // MaxSwordFinder.printAERForLevel(45);
        // conclusion: can't get a maxed-out sharpness sword from any chest,
        // because we're always missing sweeping edge when level >= 45.

        // MaxSwordFinder.findMaxSwordSeeds();
        // results -> max_swords.txt

        // findEndCity();
        // results -> world_seeds.txt

        // findJungleTemple();
        // results -> world_seeds.txt
    }

    private static void findEndCity() {
        List.of(
                122582799509215L,   // bane of arthropods
                113671614640449L,   // smite
                31862010966927L     // sharpness
        )
        .forEach(lootSeed -> {
            EndCityChest endCityChest = new EndCityChest(lootSeed);
            endCityChest.findWorldSeeds();
        });
    }

    private static void findJungleTemple() {
        JungleTempleChest.getValidLootSeedsFromFile("seeds_11.txt")
        .forEach(lootSeed -> {
            JungleTempleChest jungleTempleChest = new JungleTempleChest(lootSeed);
            jungleTempleChest.findWorldSeeds();
        });
    }
}
