package com.vicevil.zamoramcstatus.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.vicevil.zamoramcstatus.common.StatusProtocol;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;

@Plugin(
        id = "zamoramc-status",
        name = "ZamoraMC Status",
        version = "1.0.0",
        description = "Publishes the online status of Velocity backend servers to Paper placeholders.",
        authors = {"ZamoraMC"})
public final class ZamoraMCStatusVelocity {
    private static final String COMMAND = "zamoramc-status";
    private static final String PERMISSION = "zamoramcstatus.reload";
    private static final ChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("zamoramc:status");

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final AtomicLong checkGeneration = new AtomicLong();
    private volatile Map<String, Boolean> statuses = Map.of();
    private volatile VelocityConfig config;
    private ScheduledTask scheduledTask;

    @Inject
    public ZamoraMCStatusVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        proxy.getChannelRegistrar().register(CHANNEL);
        proxy.getCommandManager().register(COMMAND, new ReloadCommand());
        reload(true);
        logger.info("ZamoraMC Status enabled; monitoring " + proxy.getAllServers().size() + " Velocity servers.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (scheduledTask != null) {
            checkGeneration.incrementAndGet();
            scheduledTask.cancel();
        }
        proxy.getChannelRegistrar().unregister(CHANNEL);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!CHANNEL.equals(event.getIdentifier()) || !(event.getSource() instanceof ServerConnection connection)) {
            return;
        }
        if (!StatusProtocol.isRequest(event.getData())) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        Map<String, Boolean> current = snapshot();
        connection.sendPluginMessage(CHANNEL, StatusProtocol.snapshot(current));
    }

    private void reload(boolean initialLoad) {
        try {
            config = VelocityConfig.load(dataDirectory);
        } catch (IOException exception) {
            logger.severe("Unable to load config.yml: " + exception.getMessage());
            config = new VelocityConfig(5, 2_000);
        }
        if (scheduledTask != null) {
            scheduledTask.cancel();
        }
        scheduledTask = proxy.getScheduler()
                .buildTask(this, this::checkAllServers)
                .repeat(Duration.ofSeconds(config.checkIntervalSeconds()))
                .schedule();
        if (!initialLoad) {
            logger.info("Configuration reloaded; checking Velocity servers now.");
        }
        checkAllServers();
    }

    private void checkAllServers() {
        long generation = checkGeneration.incrementAndGet();
        Map<String, RegisteredServer> servers = new LinkedHashMap<>();
        for (RegisteredServer server : proxy.getAllServers()) {
            servers.put(server.getServerInfo().getName(), server);
        }
        Map<String, CompletableFuture<Boolean>> checks = new LinkedHashMap<>();
        for (Map.Entry<String, RegisteredServer> entry : servers.entrySet()) {
            CompletableFuture<Boolean> check;
            try {
                check = entry.getValue().ping()
                        .orTimeout(config.pingTimeoutMilliseconds(), TimeUnit.MILLISECONDS)
                        .handle((ignored, throwable) -> throwable == null);
            } catch (RuntimeException exception) {
                check = CompletableFuture.completedFuture(false);
            }
            checks.put(entry.getKey(), check);
        }
        CompletableFuture.allOf(checks.values().toArray(CompletableFuture[]::new)).whenComplete((ignored, throwable) -> {
            if (generation != checkGeneration.get()) {
                return;
            }
            Map<String, Boolean> result = new LinkedHashMap<>();
            checks.forEach((name, future) -> result.put(name, future.getNow(false)));
            Map<String, Boolean> normalized = new LinkedHashMap<>();
            result.forEach((name, online) -> normalized.put(normalize(name), online));
            statuses = Map.copyOf(normalized);
        });
    }

    private Map<String, Boolean> snapshot() {
        return statuses;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private final class ReloadCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] arguments = invocation.arguments();
            if (!source.hasPermission(PERMISSION)) {
                source.sendMessage(Component.text("No tienes permiso para usar este comando."));
                return;
            }
            if (arguments.length != 1 || !arguments[0].equalsIgnoreCase("reload")) {
                source.sendMessage(Component.text("Uso: /zamoramc-status reload"));
                return;
            }
            reload(false);
            source.sendMessage(Component.text("ZamoraMC Status se ha recargado correctamente."));
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            if (invocation.arguments().length == 0) {
                return List.of("reload");
            }
            return List.of();
        }
    }
}
