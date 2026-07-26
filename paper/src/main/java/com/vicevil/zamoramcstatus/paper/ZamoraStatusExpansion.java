package com.vicevil.zamoramcstatus.paper;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

final class ZamoraStatusExpansion extends PlaceholderExpansion {
    private final ZamoraMCStatusPaper plugin;

    ZamoraStatusExpansion(ZamoraMCStatusPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "zamoramcstatus";
    }

    @Override
    public String getAuthor() {
        return "ZamoraMC";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null || params.isBlank()) {
            return String.valueOf(plugin.unknownStatus());
        }
        return String.valueOf(plugin.status(params));
    }
}
