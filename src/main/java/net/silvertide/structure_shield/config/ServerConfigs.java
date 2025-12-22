package net.silvertide.structure_shield.config;


import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfigs {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Boolean> PROTECT_ALL_STRUCTURES;
    public static final ForgeConfigSpec.ConfigValue<Integer> SANCTUMS_CURSE_EFFECT_DURATION;

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

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
