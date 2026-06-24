package io.github.isquyet.entropybreath.entropy;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

final class EventDrivenEntropyLookup implements EntropyLookup, Listener {
    private final JavaPlugin plugin;
    private EntropyLookup delegate = NoEntropyLookup.INSTANCE;
    private Class<?> entropyServiceType;
    private boolean bound;
    private boolean permanentFallback;
    private boolean listening;
    private boolean missingPluginLogged;
    private boolean disabledPluginLogged;
    private boolean missingClasspathLogged;
    private boolean missingServiceLogged;
    private boolean missingMethodLogged;

    EventDrivenEntropyLookup(JavaPlugin plugin) {
        this.plugin = plugin;
        bindIfAvailable();
    }

    @Override
    public int getEntropy(Location location) {
        return delegate.getEntropy(location);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        if (!EntropyLookupFactory.ENTROPY_CORE_PLUGIN_NAME.equals(event.getPlugin().getName())) {
            return;
        }
        bindIfAvailable();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (!EntropyLookupFactory.ENTROPY_CORE_PLUGIN_NAME.equals(event.getPlugin().getName())) {
            return;
        }
        unbind();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (entropyServiceType == null && !resolveEntropyServiceType()) {
            return;
        }
        if (event.getProvider().getService().equals(entropyServiceType)) {
            bindIfAvailable();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServiceUnregister(ServiceUnregisterEvent event) {
        if (entropyServiceType == null || !event.getProvider().getService().equals(entropyServiceType)) {
            return;
        }
        unbind();
        bindIfAvailable();
    }

    private void bindIfAvailable() {
        if (bound || permanentFallback) {
            return;
        }

        Plugin entropyCore = plugin.getServer().getPluginManager().getPlugin(EntropyLookupFactory.ENTROPY_CORE_PLUGIN_NAME);
        if (entropyCore == null) {
            logMissingPluginOnce();
            permanentFallback = true;
            unregisterListener();
            return;
        }
        if (!entropyCore.isEnabled()) {
            logDisabledPluginOnce();
            registerListener();
            return;
        }
        if (!resolveEntropyServiceType()) {
            return;
        }

        RegisteredServiceProvider<?> provider = plugin.getServer().getServicesManager().getRegistration(entropyServiceType);
        if (provider == null) {
            logMissingServiceOnce();
            registerListener();
            return;
        }

        try {
            delegate = ReflectiveEntropyLookup.create(entropyServiceType, provider.getProvider(), plugin.getLogger());
            bound = true;
            registerListener();
            plugin.getLogger().info("Using EntropyCore for entropy-based air loss.");
        } catch (NoSuchMethodException | SecurityException exception) {
            logMissingMethodOnce();
            permanentFallback = true;
            unregisterListener();
        }
    }

    private void unbind() {
        if (!bound) {
            return;
        }
        delegate = NoEntropyLookup.INSTANCE;
        bound = false;
    }

    private boolean resolveEntropyServiceType() {
        if (entropyServiceType != null) {
            return true;
        }
        try {
            entropyServiceType = Class.forName(EntropyLookupFactory.ENTROPY_SERVICE_CLASS_NAME, false, plugin.getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException exception) {
            logMissingClasspathOnce();
            permanentFallback = true;
            unregisterListener();
            return false;
        }
    }

    private void registerListener() {
        if (listening) {
            return;
        }
        listening = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void unregisterListener() {
        if (!listening) {
            return;
        }
        HandlerList.unregisterAll(this);
        listening = false;
    }

    private void logMissingPluginOnce() {
        if (missingPluginLogged) {
            return;
        }
        missingPluginLogged = true;
        plugin.getLogger().info("EntropyCore is not installed; entropy-based air loss is disabled for this server session.");
    }

    private void logDisabledPluginOnce() {
        if (disabledPluginLogged) {
            return;
        }
        disabledPluginLogged = true;
        plugin.getLogger().info("EntropyCore is installed but not enabled yet; entropy-based air loss will start when it becomes available.");
    }

    private void logMissingClasspathOnce() {
        if (missingClasspathLogged) {
            return;
        }
        missingClasspathLogged = true;
        plugin.getLogger().warning("EntropyCore is installed, but EntropyService is not on the plugin classpath; entropy-based air loss is disabled for this server session.");
    }

    private void logMissingServiceOnce() {
        if (missingServiceLogged) {
            return;
        }
        missingServiceLogged = true;
        plugin.getLogger().warning("EntropyCore is installed, but EntropyService is unavailable; entropy-based air loss will start if the service is registered later.");
    }

    private void logMissingMethodOnce() {
        if (missingMethodLogged) {
            return;
        }
        missingMethodLogged = true;
        plugin.getLogger().warning("EntropyService#getEntropy(Location) is unavailable; entropy-based air loss is disabled.");
    }
}
