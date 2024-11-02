package com.vagdedes.spartan.abstraction.configuration.implementation;

import com.vagdedes.spartan.abstraction.configuration.ConfigurationBuilder;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;

import java.io.File;
import java.util.List;

public class Settings extends ConfigurationBuilder {

    public Settings() {
        super("settings");
    }

    public static final String crossServerNotificationsName = "Important.server_name";

    @Override
    public void clear() {
        internalClear();
    }

    @Override
    public void create() {
        file = new File(directory);
        clear();

        addOption("Punishments.broadcast_on_punishment", true);

        addOption("Logs.log_file", true);
        addOption("Logs.log_console", true);

        addOption("Notifications.individual_only_notifications", false);
        addOption("Notifications.enable_notifications_on_login", true);
        addOption("Notifications.awareness_notifications", true);
        addOption("Notifications.message_clickable_command", "/teleport {player}");

        addOption("Important.max_supported_player_latency", TPS.tickTimeInteger * 100);
        addOption("Important.op_bypass", false);
        addOption(crossServerNotificationsName, "");
        addOption("Important.bedrock_client_permission", false);
        addOption("Important.bedrock_player_prefix", ".");
        addOption("Important.enable_developer_api", true);
        addOption("Important.enable_npc", true);
        addOption("Important.enable_watermark", true);

        addOption("Detections.ground_teleport_on_detection", true);
        addOption("Detections.fall_damage_on_teleport", false);

        addOption("Discord.webhook_hex_color", "4caf50");
        addOption("Discord.checks_webhook_url", "");
        addOption("Discord.punishments_webhook_url", "");
    }

    public void runOnLogin(SpartanProtocol p) {
        if (getBoolean("Notifications.enable_notifications_on_login")
                && DetectionNotifications.hasPermission(p)
                && !DetectionNotifications.isEnabled(p)) {
            DetectionNotifications.set(p, DetectionNotifications.defaultFrequency);
        }
    }

    public void runOnLogin() {
        List<SpartanProtocol> protocols = SpartanBukkit.getProtocols();

        if (!protocols.isEmpty()) {
            for (SpartanProtocol protocol : protocols) {
                runOnLogin(protocol);
            }
        }
    }

}
