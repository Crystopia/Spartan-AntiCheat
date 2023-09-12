package me.vagdedes.spartan.features.chat;

import me.vagdedes.spartan.compatibility.semi.Authentication;
import me.vagdedes.spartan.configuration.Messages;
import me.vagdedes.spartan.configuration.Settings;
import me.vagdedes.spartan.features.important.Permissions;
import me.vagdedes.spartan.features.synchronicity.CrossServerInformation;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.system.Enums;
import me.vagdedes.spartan.system.SpartanBukkit;
import me.vagdedes.spartan.utils.server.ConfigUtils;

import java.util.List;

public class StaffChat {

    public static boolean run(SpartanPlayer p, String msg) {
        if (Permissions.has(p, Enums.Permission.STAFF_CHAT) && (!Authentication.isEnabled() || (System.currentTimeMillis() - p.getCreationTime()) > 60_000L)) {
            String character = Settings.getString("Chat.staff_chat_character");

            if (character != null && character.length() > 0 && msg.startsWith(character.toLowerCase())) {
                msg = msg.substring(1);
                String message = Messages.get("staff_chat_message");
                message = message.replace("{message}", msg);
                message = ConfigUtils.replaceWithSyntax(p, message, null);
                List<SpartanPlayer> players = SpartanBukkit.getPlayers();

                if (players.size() > 0) {
                    for (SpartanPlayer o : players) {
                        if (Permissions.has(o, Enums.Permission.STAFF_CHAT)) {
                            o.sendMessage(message);
                        }
                    }
                }

                SpartanLocation location = p.getLocation();
                CrossServerInformation.queueNotificationWithWebhook(p.getUniqueId(), p.getName(),
                        location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                        "Staff Chat", msg,
                        false);
                return true;
            }
        }
        return false;
    }
}
