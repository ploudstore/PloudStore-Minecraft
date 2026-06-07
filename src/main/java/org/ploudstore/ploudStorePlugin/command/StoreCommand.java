package org.ploudstore.ploudStorePlugin.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ploudstore.ploudStorePlugin.PloudStorePlugin;
import org.ploudstore.ploudStorePlugin.model.PinResponse;

public class StoreCommand implements CommandExecutor {

    private final PloudStorePlugin plugin;

    public StoreCommand(PloudStorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando é apenas para jogadores.");
            return true;
        }

        player.sendMessage(Component.text("[Loja] ", NamedTextColor.GOLD)
                .append(Component.text("A obter o teu PIN...", NamedTextColor.GRAY)));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            PinResponse response = plugin.getApiClient().fetchPlayerPin(player.getName());

            if (response == null) {
                player.sendMessage(Component.text("[Loja] ", NamedTextColor.GOLD)
                        .append(Component.text("Erro ao obter o PIN. Tenta novamente mais tarde.", NamedTextColor.RED)));
                return;
            }

            String pin = response.getPin();

            if (pin == null) {
                player.sendMessage(Component.text("[Loja] ", NamedTextColor.GOLD)
                        .append(Component.text("Ainda não tens sessão iniciada na loja.", NamedTextColor.YELLOW)));
                player.sendMessage(Component.text("[Loja] ", NamedTextColor.GOLD)
                        .append(Component.text("Acede à loja e inicia sessão para obteres o teu PIN.", NamedTextColor.GRAY)));
                return;
            }

            Component pinComponent = Component.text("[ " + pin + " ]")
                    .color(NamedTextColor.AQUA)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.copyToClipboard(pin))
                    .hoverEvent(HoverEvent.showText(
                            Component.text("Clica para copiar o PIN", NamedTextColor.GRAY)));

            player.sendMessage(Component.text("[Loja] ", NamedTextColor.GOLD)
                    .append(Component.text("O teu PIN: ", NamedTextColor.YELLOW))
                    .append(pinComponent));
        });

        return true;
    }
}
