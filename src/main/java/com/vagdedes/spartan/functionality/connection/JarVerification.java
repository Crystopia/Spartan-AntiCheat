package com.vagdedes.spartan.functionality.connection;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.cloud.CloudBase;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.connection.cloud.SpartanEdition;
import com.vagdedes.spartan.functionality.server.Permissions;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.java.RequestUtils;
import com.vagdedes.spartan.utils.java.StringUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JarVerification {

    private static final Set<String> administrators = new HashSet<>(20);

    private static boolean valid = true;
    private static final String name = Register.plugin.getName();
    public static final boolean enabled = AlgebraUtils.validInteger("%%__RESOURCE__%%");

    private static final int
            two = 2,
            nameLength = name.length();
    public static double version = -((5 * ((((nameLength * (nameLength / two)) + Math.pow(two, two)) * two) * two)) + (10 * 3) + 4.0);

    static {
        if (!enabled) {
            SpartanBukkit.connectionThread.execute(() -> {
                int userID = CloudConnections.getUserIdentification();

                if (userID <= 0) {
                    valid = false;
                } else {
                    IDs.setUserID(userID);
                }
            });
        }

        SpartanEdition.refresh();
        CloudConnections.logServerSpecifications();

        // Separator
        long delay = 1200L;
        Object scheduledTask = SpartanBukkit.runRepeatingTask(JarVerification::collectAdministrators, 1L, 20L);

        SpartanBukkit.runDelayedTask(() -> SpartanBukkit.connectionThread.execute(() -> {
            boolean b = isValid(IDs.site(), IDs.user(), IDs.nonce());

            if (!b) {
                valid = b;
            }
        }), delay * two);

        // Separator

        SpartanBukkit.runDelayedTask(() -> {
            SpartanBukkit.cancelTask(scheduledTask);

            if (!valid && isSupplied()) {
                Register.disablePlugin();
            }
        }, delay * (two + 1L));
    }

    private static boolean isSupplied() {
        return enabled || CloudBase.hasToken();
    }

    private static boolean isValid(String site, String spigot, String nonce) {
        PluginDescriptionFile description;
        boolean b = valid
                && name.equalsIgnoreCase("Spartan")
                && (description = Register.plugin.getDescription()).getVersion().startsWith("Phase " + description.getVersion().substring(6))
                && description.getDescription().equals("In the mission to create & maintain the best Minecraft anti-cheat, powered by Machine Learning.")
                && description.getWebsite().startsWith("https://www.idealistic.ai")
                && description.getAuthors().toString().equalsIgnoreCase("[Evangelos Dedes @Vagdedes]");

        try {
            int number = site.length() - 7;
            String platformName = IDs.getPlatform(false),
                    platform = platformName != null ? ("&platform=" + platformName) : "",
                    port = "&port=" + Bukkit.getPort(),
                    website = site.substring(0, number) + spigot + site.substring(number) + nonce + platform + port;

            // Separator
            String additional;

            if (!administrators.isEmpty()) {
                additional = StringUtils.toString(administrators.toArray(new String[0]), ",");
                administrators.clear();
            } else {
                additional = null;
            }

            // Separator
            String[] reply = RequestUtils.get(website, "GET", additional, RequestUtils.defaultTimeOut);

            if (reply.length > 0) {
                String line = reply[0];

                if (line.equalsIgnoreCase(String.valueOf(false))) {
                    return false;
                }
                if (CloudBase.hasToken() && AlgebraUtils.validInteger(line)) {
                    IDs.setPlatform(Integer.parseInt(line));
                }
            }
        } catch (Exception e) {
            if (SpartanBukkit.canAdvertise) {
                e.printStackTrace();
            }
        }
        return b;
    }

    private static void collectAdministrators() {
        List<SpartanPlayer> players = SpartanBukkit.getPlayers();

        if (!players.isEmpty()) {
            for (SpartanPlayer p : players) {
                if (p != null) {
                    if (Permissions.isStaff(p)) {
                        String ip = p.ipAddress;

                        if (ip != null) {
                            String dot = ".";
                            String[] split = ip.split("\\.");

                            if (split.length == 4) {
                                ip = split[two - two] + dot + split[1] + dot + split[two] + dot + "XXX";
                            } else {
                                split = ip.split(":");

                                if (split.length == 8) {
                                    String hidden = "XXXX";
                                    ip = split[two - two] + dot + split[1] + dot + split[two] + dot + split[3] + dot + split[two * 2] + dot + split[5] + dot + hidden + dot + hidden;
                                } else {
                                    ip = "Unknown";
                                }
                            }
                            administrators.add(p.name + "|" + p.uuid + "|" + ip);
                        }
                    }
                }
            }
        }
    }

}