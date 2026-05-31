package com.newbiewood.nbw.weather;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.newbiewood.nbw.ScorchAndFrostConfig;
import net.dries007.tfc.util.climate.KoppenClimateClassification;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class ModCommands
{
    private static final DynamicCommandExceptionType INVALID_GROUP = new DynamicCommandExceptionType(
            name -> Component.literal("Unknown climate zone group: " + name));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("tfcscorch")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("trigger")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("heatwave");
                                    builder.suggest("coldwave");
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (ClimateZoneGroup g : ClimateZoneGroup.values())
                                                builder.suggest(g.name());
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> trigger(ctx, StringArgumentType.getString(ctx, "type"), StringArgumentType.getString(ctx, "group"), 0f, -1))
                                        .then(Commands.argument("offset", FloatArgumentType.floatArg())
                                                .executes(ctx -> trigger(ctx, StringArgumentType.getString(ctx, "type"), StringArgumentType.getString(ctx, "group"), FloatArgumentType.getFloat(ctx, "offset"), -1))
                                                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 30))
                                                        .executes(ctx -> trigger(ctx, StringArgumentType.getString(ctx, "type"), StringArgumentType.getString(ctx, "group"), FloatArgumentType.getFloat(ctx, "offset"), IntegerArgumentType.getInteger(ctx, "duration")))
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("list")
                        .executes(ModCommands::list)
                )
                .then(Commands.literal("clear")
                        .executes(ctx -> clear(ctx, null))
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (ClimateZoneGroup g : ClimateZoneGroup.values())
                                        builder.suggest(g.name());
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> clear(ctx, StringArgumentType.getString(ctx, "group")))
                        )
                )
                .then(Commands.literal("whereami")
                        .executes(ModCommands::whereami)
                )
        );
    }

    private static int trigger(CommandContext<CommandSourceStack> ctx, String typeStr, String groupStr, float offset, int duration) throws CommandSyntaxException
    {
        WeatherEventType type;
        if (typeStr.equalsIgnoreCase("heatwave")) type = WeatherEventType.HEAT_WAVE;
        else if (typeStr.equalsIgnoreCase("coldwave")) type = WeatherEventType.COLD_WAVE;
        else
        {
            ctx.getSource().sendFailure(Component.literal("Invalid type. Use 'heatwave' or 'coldwave'."));
            return 0;
        }

        ClimateZoneGroup group;
        try {
            group = ClimateZoneGroup.valueOf(groupStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw INVALID_GROUP.create(groupStr);
        }

        ServerLevel level = ctx.getSource().getLevel();
        WeatherEventManager manager = WeatherEventManager.get(level);

        if (duration < 0)
        {
            duration = level.getRandom().nextIntBetweenInclusive(
                    ScorchAndFrostConfig.MIN_EVENT_DURATION_DAYS.get(),
                    ScorchAndFrostConfig.MAX_EVENT_DURATION_DAYS.get());
        }

        if (offset == 0f)
        {
            ClimateZoneConfig config = ClimateZoneConfig.forGroup(group);
            if (type == WeatherEventType.HEAT_WAVE) offset = config.selectHeatWaveOffset(level.getRandom());
            else offset = config.selectColdWaveOffset(level.getRandom());
        }

        String eventType = typeStr;
        ClimateZoneGroup targetGroup = group;
        float finalOffset = offset;
        int finalDuration = duration;

        manager.forceEvent(group, new WeatherEvent(type, offset, duration));
        ctx.getSource().sendSuccess(() -> Component.literal("Triggered " + eventType + " in " + targetGroup + " (offset=" + String.format("%.1f", finalOffset) + "\u00B0C, " + finalDuration + " days)"), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx)
    {
        ServerLevel level = ctx.getSource().getLevel();
        WeatherEventManager manager = WeatherEventManager.get(level);
        var events = manager.getActiveEvents();
        if (events.isEmpty())
        {
            ctx.getSource().sendSuccess(() -> Component.literal("No active weather events."), false);
        }
        else
        {
            ctx.getSource().sendSuccess(() -> Component.literal("=== Active Weather Events ==="), false);
            for (var entry : events.entrySet())
            {
                WeatherEvent e = entry.getValue();
                ctx.getSource().sendSuccess(() -> Component.literal(
                        entry.getKey() + ": " + e.getType() + " offset=" + String.format("%.1f", e.getCurrentOffset())
                                + "\u00B0C remaining=" + e.getRemainingDays() + "/" + e.getTotalDays() + " phase=" + e.getCurrentPhase()), false);
            }
        }
        return 1;
    }

    private static int whereami(CommandContext<CommandSourceStack> ctx)
    {
        ServerLevel level = ctx.getSource().getLevel();
        WeatherEventManager manager = WeatherEventManager.get(level);
        KoppenClimateClassification koppen = manager.classifyAt(ctx.getSource().getPlayer().blockPosition());
        ClimateZoneGroup group = ClimateZoneGroup.fromKoppen(koppen);
        ctx.getSource().sendSuccess(() -> Component.literal("Koppen: " + koppen + " -> Group: " + group), false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx, String groupStr)
    {
        ServerLevel level = ctx.getSource().getLevel();
        WeatherEventManager manager = WeatherEventManager.get(level);
        if (groupStr == null)
        {
            manager.clearEvents();
            ctx.getSource().sendSuccess(() -> Component.literal("Cleared all weather events."), true);
        }
        else
        {
            ClimateZoneGroup group;
            try {
                group = ClimateZoneGroup.valueOf(groupStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                ctx.getSource().sendFailure(Component.literal("Unknown climate zone group: " + groupStr));
                return 0;
            }
            manager.clearEvent(group);
            ctx.getSource().sendSuccess(() -> Component.literal("Cleared weather event for " + group), true);
        }
        return 1;
    }
}
