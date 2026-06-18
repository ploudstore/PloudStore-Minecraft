package org.ploudstore.ploudStorePlugin.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.ploudstore.ploudStorePlugin.PloudStorePlugin;
import org.ploudstore.ploudStorePlugin.model.PinResponse;
import org.ploudstore.ploudStorePlugin.queue.ExecutedCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PloudStoreCommand implements CommandExecutor, TabCompleter {

    private static final long CONFIRM_TIMEOUT_MS = 30_000L;
    private static final UUID CONSOLE_UUID = new UUID(0L, 0L);

    private final PloudStorePlugin plugin;
    private final ExecutedCache executedCache;
    private final Map<UUID, Long> pendingSecretConfirm = new HashMap<>();

    public PloudStoreCommand(PloudStorePlugin plugin, ExecutedCache executedCache) {
        this.plugin = plugin;
        this.executedCache = executedCache;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "pin":
                handlePin(sender);
                break;
            case "secret":
                if (!sender.hasPermission("ploudstore.admin")) {
                    sender.sendMessage("§cSem permissão.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§c[PloudStore] Uso: /ploudstore secret <valor>");
                    return true;
                }
                handleSecret(sender, args[1]);
                break;
            case "reload":
                if (!sender.hasPermission("ploudstore.admin")) {
                    sender.sendMessage("§cSem permissão.");
                    return true;
                }
                plugin.reload();
                sender.sendMessage("§a[PloudStore] Configuração recarregada.");
                break;
            case "status":
                if (!sender.hasPermission("ploudstore.admin")) {
                    sender.sendMessage("§cSem permissão.");
                    return true;
                }
                sender.sendMessage("§e[PloudStore] Estado:");
                sender.sendMessage("§7  Cache em memória: §f" + executedCache.size() + " comando(s) recente(s)");
                break;
            case "forcecheck":
                if (!sender.hasPermission("ploudstore.admin")) {
                    sender.sendMessage("§cSem permissão.");
                    return true;
                }
                sender.sendMessage("§a[PloudStore] A forçar verificação de compras...");
                plugin.getPluginScheduler().runAsync(new Runnable() {
                    public void run() { plugin.getCommandProcessor().performCheck(); }
                });
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handlePin(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando é apenas para jogadores.");
            return;
        }
        if (!sender.hasPermission("ploudstore.player")) {
            sender.sendMessage("§cSem permissão.");
            return;
        }

        final Player player = (Player) sender;
        player.sendMessage(ChatColor.GOLD + "[Loja] " + ChatColor.GRAY + "A obter o teu PIN...");

        plugin.getPluginScheduler().runAsync(new Runnable() {
            public void run() {
                PinResponse response = plugin.getApiClient().fetchPlayerPin(player.getName());

                if (response == null) {
                    player.sendMessage(ChatColor.GOLD + "[Loja] " + ChatColor.RED + "Erro ao obter o PIN. Tenta novamente mais tarde.");
                    return;
                }

                String pin = response.getPin();

                if (pin == null) {
                    player.sendMessage(ChatColor.GOLD + "[Loja] " + ChatColor.YELLOW + "Ainda não tens sessão iniciada na loja.");
                    player.sendMessage(ChatColor.GOLD + "[Loja] " + ChatColor.GRAY + "Acede à loja e inicia sessão para obteres o teu PIN.");
                    return;
                }

                player.sendMessage(ChatColor.GOLD + "[Loja] " + ChatColor.YELLOW + "O teu PIN: "
                        + ChatColor.AQUA + ChatColor.BOLD + "[ " + pin + " ]");
                player.sendMessage(ChatColor.GOLD + "[Loja] " + ChatColor.GRAY + "Usa este PIN na loja para autenticares a tua conta.");
            }
        });
    }

    private void handleSecret(CommandSender sender, String newSecret) {
        String current = plugin.getConfig().getString("secret-key", "").trim();
        boolean isDefault = current.isEmpty() || current.equals("your-secret-key-here");

        if (isDefault) {
            applySecret(sender, newSecret);
            return;
        }

        UUID id = (sender instanceof Player) ? ((Player) sender).getUniqueId() : CONSOLE_UUID;

        Long pendingAt = pendingSecretConfirm.get(id);
        if (pendingAt != null && System.currentTimeMillis() - pendingAt <= CONFIRM_TIMEOUT_MS) {
            pendingSecretConfirm.remove(id);
            applySecret(sender, newSecret);
            return;
        }

        pendingSecretConfirm.put(id, System.currentTimeMillis());
        sender.sendMessage("§e[PloudStore] Já tens um secret configurado.");
        sender.sendMessage("§e[PloudStore] Corre o comando novamente em §f30 segundos §epara confirmar a alteração.");
    }

    private void applySecret(CommandSender sender, String secret) {
        plugin.getConfig().set("secret-key", secret);
        plugin.saveConfig();
        plugin.reload();
        sender.sendMessage("§a[PloudStore] Secret atualizado com sucesso.");
    }

    private void sendHelp(CommandSender sender) {
        boolean isAdmin = sender.hasPermission("ploudstore.admin");
        sender.sendMessage("§e[PloudStore] Comandos disponíveis:");
        sender.sendMessage("§7  /ploudstore pin §f— Obtém o teu PIN de sessão da loja");
        if (isAdmin) {
            sender.sendMessage("§7  /ploudstore secret <secret> §f— Configura ou atualiza o secret key");
            sender.sendMessage("§7  /ploudstore reload §f— Recarrega a configuração");
            sender.sendMessage("§7  /ploudstore status §f— Estado da cache de comandos");
            sender.sendMessage("§7  /ploudstore forcecheck §f— Força verificação imediata de compras");
        }
    }

    public void clearPendingConfirmations() {
        pendingSecretConfirm.clear();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("help", "pin"));
            if (sender.hasPermission("ploudstore.admin")) {
                subs.addAll(Arrays.asList("secret", "reload", "status", "forcecheck"));
            }
            return subs;
        }
        return Collections.emptyList();
    }
}
