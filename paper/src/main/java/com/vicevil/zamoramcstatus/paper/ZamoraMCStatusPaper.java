package com.vicevil.zamoramcstatus.paper;

import com.vicevil.zamoramcstatus.common.StatusProtocol;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class ZamoraMCStatusPaper extends JavaPlugin implements Listener, TabExecutor {
    private static final String CHANNEL = StatusProtocol.CHANNEL;
    private static final String PERMISSION = "zamoramcstatus.reload";

    private final StatusCache cache = new StatusCache();
    private PaperConfig config;
    private BukkitTask requestTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLocalConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, (channel, player, message) -> {
            if (!CHANNEL.equals(channel)) {
                return;
            }
            Optional<Map<String, Boolean>> snapshot = StatusProtocol.decodeSnapshot(message);
            snapshot.ifPresent(cache::replace);
        });
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("zamoramc-status").setExecutor(this);
        getCommand("zamoramc-status").setTabCompleter(this);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ZamoraStatusExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered: %zamoramcstatus_<server>%");
        } else {
            getLogger().warning("PlaceholderAPI is not installed; the ZamoraMC Status placeholder is unavailable.");
        }
        scheduleRequests();
    }

    @Override
    public void onDisable() {
        if (requestTask != null) {
            requestTask.cancel();
        }
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().unregisterIncomingPluginChannel(this, CHANNEL);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, this::requestSnapshot, 20L);
    }

    boolean status(String serverName) {
        return cache.get(serverName, unknownStatus());
    }

    boolean unknownStatus() {
        return config.unknownStatus();
    }

    private void loadLocalConfig() {
        reloadConfig();
        config = PaperConfig.from(getConfig());
    }

    private void scheduleRequests() {
        if (requestTask != null) {
            requestTask.cancel();
        }
        long period = config.requestIntervalSeconds() * 20L;
        requestTask = Bukkit.getScheduler().runTaskTimer(this, this::requestSnapshot, 1L, period);
    }

    private void requestSnapshot() {
        Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (player != null) {
            player.sendPluginMessage(this, CHANNEL, StatusProtocol.request());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }
        if (arguments.length != 1 || !arguments[0].equalsIgnoreCase("reload")) {
            sender.sendMessage("§eUso: /zamoramc-status reload");
            return true;
        }
        loadLocalConfig();
        cache.clear();
        scheduleRequests();
        requestSnapshot();
        sender.sendMessage("§aZamoraMC Status se ha recargado correctamente.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] arguments) {
        if (arguments.length == 1 && "reload".startsWith(arguments[0].toLowerCase())) {
            return List.of("reload");
        }
        return List.of();
    }
}
