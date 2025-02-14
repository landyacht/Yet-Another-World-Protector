package de.z0rdak.yawp.config.server;

import de.z0rdak.yawp.YetAnotherWorldProtector;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;
import java.util.stream.Collectors;

public class FlagConfig {

    public static final ForgeConfigSpec CONFIG_SPEC;
    public static final String CONFIG_NAME = YetAnotherWorldProtector.MODID + "-flags.toml";
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BREAK_FLAG_ENTITIES;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BREAK_FLAG_ENTITY_TAGS;

    static {
        final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.push("YetAnotherWorldProtector mod flag configuration").build();

        BREAK_FLAG_ENTITIES = BUILDER
                .comment("Entities included/protected by the break block flag. Includes entities like armor stands and pictures by default")
                .defineListAllowEmpty(Collections.singletonList("break_flag_entities"), FlagConfig::defaultEntityBreakFlagEntries, FlagConfig::isValidEntityEntry);

        BREAK_FLAG_ENTITY_TAGS = BUILDER
                .comment("Entity tags included/protected by the break block flag.")
                .defineListAllowEmpty(Collections.singletonList("break_flag_entity_tags"), () -> Collections.singletonList(""), FlagConfig::isValidTagEntry);

        BUILDER.pop();
        CONFIG_SPEC = BUILDER.build();

    }

    private static List<String> defaultEntityBreakFlagEntries() {
        return Arrays.asList("minecraft:armor_stand", "minecraft:painting", "minecraft:item_frame", "minecraft:glow_item_frame", "minecraft:leash_knot");
    }

    public static Set<String> getBreakFlagEntities() {
        return FlagConfig.BREAK_FLAG_ENTITIES.get().stream()
                .filter(Objects::nonNull)
                .map(String::toString).collect(Collectors.toSet());
    }

    public static Set<String> getBreakFlagEntityTags() {
        return FlagConfig.BREAK_FLAG_ENTITY_TAGS.get().stream()
                .filter(Objects::nonNull)
                .map(String::toString).collect(Collectors.toSet());
    }

    private static boolean isValidEntityEntry(Object entity) {
        return true;
    }

    private static boolean isValidTagEntry(Object entity) {
        return true;
    }
}
