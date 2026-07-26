package com.vicevil.zamoramcstatus.paper;

import org.bukkit.configuration.file.FileConfiguration;

record PaperConfig(int requestIntervalSeconds, boolean unknownStatus) {
    static PaperConfig from(FileConfiguration configuration) {
        int interval = Math.max(1, Math.min(3_600, configuration.getInt("request-interval-seconds", 5)));
        return new PaperConfig(interval, configuration.getBoolean("unknown-status", false));
    }
}
