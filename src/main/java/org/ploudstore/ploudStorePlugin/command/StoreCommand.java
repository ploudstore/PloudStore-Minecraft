package org.ploudstore.ploudStorePlugin.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ploudstore.ploudStorePlugin.PloudStorePlugin;
import org.ploudstore.ploudStorePlugin.model.PinResponse;

public class StoreCommand implements CommandExecutor {

    // COPY_TO_CLIPBOARD was added in Minecraft 1.15
    private static final boolean SUPPORTS_CLIPBOARD = supportsClipboard();

    private final PloudStorePlugin plugin;

    public StoreCommand(PloudStorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando é apenas para jogadores.");
            return true;
        }

        final Player player = (Player) sender;

        player.spigot().sendMessage(new ComponentBuilder("[Loja] ").color(ChatColor.GOLD)
                .append("A obter o teu PIN...").color(ChatColor.GRAY)
                .create());

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            public void run() {
                PinResponse response = plugin.getApiClient().fetchPlayerPin(player.getName());

                if (response == null) {
                    player.spigot().sendMessage(new ComponentBuilder("[Loja] ").color(ChatColor.GOLD)
                            .append("Erro ao obter o PIN. Tenta novamente mais tarde.").color(ChatColor.RED)
                            .create());
                    return;
                }

                String pin = response.getPin();

                if (pin == null) {
                    player.spigot().sendMessage(new ComponentBuilder("[Loja] ").color(ChatColor.GOLD)
                            .append("Ainda não tens sessão iniciada na loja.").color(ChatColor.YELLOW)
                            .create());
                    player.spigot().sendMessage(new ComponentBuilder("[Loja] ").color(ChatColor.GOLD)
                            .append("Acede à loja e inicia sessão para obteres o teu PIN.").color(ChatColor.GRAY)
                            .create());
                    return;
                }

                sendPinMessage(player, pin);
            }
        });

        return true;
    }

    private void sendPinMessage(Player player, String pin) {
        ComponentBuilder builder = new ComponentBuilder("[Loja] ").color(ChatColor.GOLD)
                .append("O teu PIN: ").color(ChatColor.YELLOW)
                .append("[ " + pin + " ]").color(ChatColor.AQUA).bold(true);

        if (SUPPORTS_CLIPBOARD) {
            ClickEvent.Action copyAction = ClickEvent.Action.valueOf("COPY_TO_CLIPBOARD");
            BaseComponent[] hoverText = new ComponentBuilder("Clica para copiar o PIN")
                    .color(ChatColor.GRAY).create();
            builder.event(new ClickEvent(copyAction, pin))
                   .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));
        }

        player.spigot().sendMessage(builder.create());
    }

    private static boolean supportsClipboard() {
        try {
            ClickEvent.Action.valueOf("COPY_TO_CLIPBOARD");
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
