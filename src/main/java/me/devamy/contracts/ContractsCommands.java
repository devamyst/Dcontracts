package me.devamy.contracts;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.devamy.contracts.config.Config;
import me.devamy.contracts.gui.AdminToolGUI;
import me.devamy.contracts.gui.ContractAdminGUI;
import me.devamy.contracts.gui.MainGUI;
import me.devamy.contracts.gui.NewOrderDialog;
import me.devamy.contracts.storage.DatabaseConverter;
import me.devamy.contracts.utils.PlayerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import static me.devamy.contracts.Contracts.plugin;
import static me.devamy.contracts.config.Config.config;

@SuppressWarnings("UnstableApiUsage")
public class ContractsCommands {

    /** Checks a permission on the source — works for both console and entity executors */
    private static @NotNull Predicate<CommandSourceStack> permission(@NotNull String permission) {
        return predicate -> {
            final Entity executor = predicate.getExecutor();
            final String perm = "contracts." + permission;
            return executor == null ? predicate.getSender().hasPermission(perm) : executor.hasPermission(perm);
        };
    }

    /** Checks that the executor is a Player AND has the given permission */
    private static @NotNull Predicate<CommandSourceStack> playerAndPermission(@NotNull String permission) {
        return predicate ->
                predicate.getExecutor() instanceof final Player player &&
                        player.hasPermission("contracts." + permission);
    }

    // ─────────────────────────────────────────────────────────────
    // /contracts (and every alias: /contract /order /orders /c)
    // ─────────────────────────────────────────────────────────────
    private static LiteralCommandNode<CommandSourceStack> buildMainContractsCmd(String cmdName) {
        return Commands.literal(cmdName)
                .requires(playerAndPermission("use"))
                .executes(ctx -> {
                    if (!(ctx.getSource().getExecutor() instanceof Player player)) return 2;
                    MainGUI mainGUI = new MainGUI(player, 0);
                    PlayerUtils.openGUI(player, mainGUI.getGUI(), true);
                    return 1;
                })
                // /contracts <search>
                .then(Commands.argument("search", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof Player player)) return 2;
                            final String search = StringArgumentType.getString(ctx, "search");
                            MainGUI mainGUI = new MainGUI(player, 0, search);
                            PlayerUtils.openGUI(player, mainGUI.getGUI(), true);
                            return 1;
                        })
                )
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // /contracts (main command with subcommands)
    // ─────────────────────────────────────────────────────────────
    private static LiteralCommandNode<CommandSourceStack> buildContractsRootCmd() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("contracts")
                .requires(playerAndPermission("use"))

                // /contracts (no args) → open main GUI
                .executes(ctx -> {
                    if (!(ctx.getSource().getExecutor() instanceof Player player)) return 2;
                    MainGUI mainGUI = new MainGUI(player, 0);
                    PlayerUtils.openGUI(player, mainGUI.getGUI(), true);
                    return 1;
                })

                // /contracts <search>
                .then(Commands.argument("search", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof Player player)) return 2;
                            // Check if first word is a subcommand — handled below, so this is only search
                            final String search = StringArgumentType.getString(ctx, "search");
                            MainGUI mainGUI = new MainGUI(player, 0, search);
                            PlayerUtils.openGUI(player, mainGUI.getGUI(), true);
                            return 1;
                        })
                )

                // /contracts create → open new contract dialog
                .then(Commands.literal("create")
                        .requires(playerAndPermission("create"))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof Player player)) return 2;
                            NewOrderDialog.open(player);
                            return 1;
                        })
                )

                // /contracts admin → admin panel
                .then(Commands.literal("admin")
                        .requires(permission("admin"))

                        // /contracts admin (no args) → admin GUI
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof Player player)) return 2;
                            ContractAdminGUI.open(player, 0);
                            return 1;
                        })

                        // /contracts admin reload
                        .then(Commands.literal("reload")
                                .requires(permission("admin.reload"))
                                .executes(ctx -> {
                                    final Entity executor = ctx.getSource().getExecutor();
                                    final CommandSender sender = executor == null ? ctx.getSource().getSender() : executor;
                                    Config.reloadAsync().thenAccept(ignored ->
                                            sender.sendRichMessage("<green>Contracts reloaded successfully."));
                                    return 1;
                                })
                        )

                        // /contracts admin blacklist
                        .then(Commands.literal("blacklist")
                                .requires(playerAndPermission("admin.blacklist"))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getExecutor() instanceof final Player p)) return 2;
                                    PlayerUtils.openGUI(p, AdminToolGUI.getBlacklistGUI(), true);
                                    return 1;
                                })
                        )

                        // /contracts admin custom_items
                        .then(Commands.literal("custom_items")
                                .requires(playerAndPermission("admin.custom-items"))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getExecutor() instanceof final Player p)) return 2;
                                    PlayerUtils.openGUI(p, AdminToolGUI.getCustomItemsGUI(), true);
                                    return 1;
                                })
                        )

                        // /contracts admin convert → DB converter
                        .then(Commands.literal("convert")
                                .requires(permission("admin.convert"))
                                .executes(ctx -> {
                                    final Entity executor = ctx.getSource().getExecutor();
                                    final CommandSender sender = executor == null ? ctx.getSource().getSender() : executor;
                                    if (executor instanceof Player player) {
                                        DatabaseConverter.openConverterGUI(player);
                                    } else {
                                        sender.sendRichMessage("<yellow>Database converter is only available in-game.");
                                    }
                                    return 1;
                                })
                        )
                );

        return builder.build();
    }

    // ─────────────────────────────────────────────────────────────
    // Registration
    // ─────────────────────────────────────────────────────────────
    public static void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            // Main command with all subcommands
            event.registrar().register(buildContractsRootCmd());

            // Aliases: /contract, /order, /orders, /c — all open the main GUI
            for (final String alias : java.util.List.of("contract", "order", "orders", "c")) {
                event.registrar().register(buildMainContractsCmd(alias));
            }

            // Also honour any extra aliases from config
            for (final String alias : config.orderCommandAliases) {
                // Skip ones we already registered
                if (alias.equals("contracts") || alias.equals("contract") ||
                        alias.equals("order") || alias.equals("orders") || alias.equals("c")) continue;
                event.registrar().register(buildMainContractsCmd(alias));
            }
        });
    }
}
