package net.silvertide.structure_shield.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfigs {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<Boolean> PROTECT_ALL_STRUCTURES;
    public static final ModConfigSpec.ConfigValue<Integer> SANCTUMS_CURSE_EFFECT_DURATION;
    public static final ModConfigSpec.ConfigValue<Boolean> PROTECT_FROM_EXPLOSIONS;
    public static final ModConfigSpec.ConfigValue<Boolean> PROTECT_FROM_BUCKET_SCOOPING;
    public static final ModConfigSpec.ConfigValue<Boolean> PROTECT_FROM_FIRE_SPREAD;
    public static final ModConfigSpec.ConfigValue<Boolean> PROTECT_FROM_PISTONS;

    static {
        BUILDER.push("Structure Shield Config");

        BUILDER.comment("");
        BUILDER.comment(" --- Protect All Structures ---");
        BUILDER.comment("If all structures should be protected.");
        BUILDER.comment("This will bypass the structure_shield_protected structure tag and just make every structure protected.");
        PROTECT_ALL_STRUCTURES = BUILDER.define("protectAllStructures", false);

        BUILDER.comment("");
        BUILDER.comment(" --- Sanctums Curse Effect Duration ---");
        BUILDER.comment("How long in seconds a player will be affected by the Sanctums Curse when they try");
        BUILDER.comment("to place or break a protected block inside a protected structure.");
        BUILDER.comment("Sanctums Curse prevents players from placing or breaking blocks while they are under its effect.");
        BUILDER.comment("This is intended to prevent players from spam placing or breaking blocks inside protected structures.");
        BUILDER.comment("It is not necessary as performance should be great without it, but it will guarantee less structure checks.");
        BUILDER.comment("Default: 4 seconds. Set to 0 to disable this entirely.");
        SANCTUMS_CURSE_EFFECT_DURATION = BUILDER.define("sanctumsCurseEffectDuration", 4);

        BUILDER.comment("");
        BUILDER.comment(" --- Protect From Explosions ---");
        BUILDER.comment("If protected blocks inside protected structures should be immune to explosions (TNT, creepers, etc.).");
        BUILDER.comment("Blocks in the structure_shield_breakable tag are still destroyed by explosions.");
        PROTECT_FROM_EXPLOSIONS = BUILDER.define("protectFromExplosions", true);

        BUILDER.comment("");
        BUILDER.comment(" --- Protect From Bucket Scooping ---");
        BUILDER.comment("If fluids (water, lava, powder snow) inside protected structures should be protected from being scooped with a bucket.");
        BUILDER.comment("Add a fluid's block (e.g. minecraft:water) to the structure_shield_breakable tag to allow scooping that specific fluid.");
        BUILDER.comment("Set to false to allow all bucket scooping inside protected structures.");
        PROTECT_FROM_BUCKET_SCOOPING = BUILDER.define("protectFromBucketScooping", true);

        BUILDER.comment("");
        BUILDER.comment(" --- Protect From Fire Spread ---");
        BUILDER.comment("If fire should be prevented from spreading onto or burning away blocks inside protected structures.");
        BUILDER.comment("This stops natural fire spread within protected structures; it does not stop players from lighting fires.");
        PROTECT_FROM_FIRE_SPREAD = BUILDER.define("protectFromFireSpread", true);

        BUILDER.comment("");
        BUILDER.comment(" --- Protect From Pistons ---");
        BUILDER.comment("If pistons should be prevented from pushing blocks into, pulling blocks out of, or moving blocks within protected structures.");
        BUILDER.comment("Cancels the entire piston movement if any affected block's source or destination is inside a protected structure.");
        BUILDER.comment("Default: false. Piston-based grief is a niche, advanced technique, and this is the only check that inspects redstone activity, so it is opt-in.");
        PROTECT_FROM_PISTONS = BUILDER.define("protectFromPistons", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
