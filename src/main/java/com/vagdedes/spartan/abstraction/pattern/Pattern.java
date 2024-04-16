package com.vagdedes.spartan.abstraction.pattern;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.performance.ResearchEngine;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class Pattern {

    private static final int storageLimit = 1024 * 1024 * 1024;
    static final int
            testingNotificationDivisor = 69,
            globalDataLimit = 10_000,
            globalDataLimitBase = 625,
            individualGlobalDataLimit = 1_024,
            individualGlobalDataLimitBase = 2;
    static final String
            profileOption = "profile",
            patternOption = "pattern",
            situationOption = "situation";
    private static final String[] options = {profileOption, patternOption, situationOption};
    private static final Collection<Pattern> instances = Collections.synchronizedList(new ArrayList<>());

    static int hash(PlayerProfile playerProfile) {
        return playerProfile.getName().hashCode();
    }

    public static void deleteFromFile(PlayerProfile profile, boolean completely) {
        synchronized (instances) {
            for (Pattern instance : instances) {
                SpartanBukkit.dataThread.executeWithPriority(() -> {
                    File[] files = instance.getLearningFiles();

                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile()
                                    && file.getName().endsWith(".yml")) {
                                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                                Set<String> keys = configuration.getKeys(false);

                                if (!keys.isEmpty()) {
                                    int profileHash = hash(profile);
                                    boolean changed = false;

                                    for (String key : keys) {
                                        ConfigurationSection section = configuration.getConfigurationSection(key);

                                        if (section == null) {
                                            continue;
                                        }
                                        Map<String, Object> data = section.getValues(false);

                                        if (data.size() != 3) {
                                            instance.deleteFromFile(configuration, key);
                                            changed = true;
                                            continue;
                                        }
                                        Object patternObject = data.get(Pattern.patternOption),
                                                situation = data.get(Pattern.situationOption),
                                                profileObject = data.get(Pattern.profileOption);

                                        if (!(patternObject instanceof Double || patternObject instanceof Float)
                                                || !(situation instanceof Integer || situation instanceof Long || situation instanceof Short)
                                                || !(profileObject instanceof String)) {
                                            instance.deleteFromFile(configuration, key);
                                            changed = true;
                                            continue;
                                        }
                                        try {
                                            if (profileObject.toString().equals(profile.getName())) {
                                                double pattern = (double) patternObject;

                                                synchronized (instance.generalizations) {
                                                    for (PatternGeneralization patternGeneralization : instance.generalizations.values()) {
                                                        if (patternGeneralization.scissors != 0) {
                                                            patternGeneralization.deletePattern(
                                                                    (int) situation,
                                                                    profileHash,
                                                                    (float) AlgebraUtils.cut(pattern, patternGeneralization.scissors)
                                                            );
                                                        } else {
                                                            patternGeneralization.deletePattern(
                                                                    (int) situation,
                                                                    profileHash,
                                                                    (float) pattern
                                                            );
                                                        }
                                                    }
                                                }
                                                instance.deleteFromFile(configuration, key);
                                                changed = true;
                                            }
                                        } catch (Exception ignored) {
                                        }
                                    }

                                    if (changed) {
                                        try {
                                            configuration.save(file);
                                        } catch (Exception ignored) {
                                        }
                                    }
                                    if (completely) {
                                        synchronized (instance.generalizations) {
                                            for (PatternGeneralization patternGeneralization : instance.generalizations.values()) {
                                                patternGeneralization.deleteProfile(profile);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    public static void reload() {
        synchronized (instances) {
            for (Pattern patternStorage : instances) {
                patternStorage.loadFromFiles();
            }
        }
    }

    public static void clear() {
        synchronized (instances) {
            for (Pattern pattern : instances) {
                synchronized (pattern.generalizations) {
                    for (PatternGeneralization generalization : pattern.generalizations.values()) {
                        generalization.clear();
                    }
                    pattern.generalizations.clear();
                }
            }
            instances.clear();
        }
    }

    // Separator

    public final String key;
    private final String learningFolder;
    private final Map<Short, PatternGeneralization> generalizations;
    private boolean loaded, loading;
    private short count;
    private File file;
    private YamlConfiguration configuration;

    public Pattern(String key,
                   short[] generalizations,
                   boolean linear,
                   boolean ranked) {
        this.key = key;
        this.learningFolder = Register.plugin.getDataFolder()
                + "/learning/"
                + key;
        this.loaded = false;
        this.loading = false;
        this.generalizations = Collections.synchronizedMap(new HashMap<>(generalizations.length));

        for (short generalization : generalizations) {
            this.generalizations.put(
                    generalization,
                    linear ? new LinearPatternGeneralization(this, generalization)
                            : (ranked
                            ? new NonLinearRankedPatternGeneralization(this, generalization)
                            : new NonLinearPatternGeneralization(this, generalization))
            );
        }
        this.reloadLearningFile();

        synchronized (instances) {
            instances.add(this);
        }
    }

    // Separator

    private void reloadLearningFile() {
        File[] files = this.getLearningFiles();

        if (files != null) {
            long length = 0;

            for (int i = 0; i < files.length; i++) {
                File file = files[i];

                if (file.isFile() && file.getName().endsWith(".yml")) {
                    length += file.length();
                } else {
                    files[i] = null;
                }
            }

            if (length >= storageLimit) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));

                for (File file : files) {
                    if (file != null) {
                        long fileLength = file.length();

                        if (file.delete()) {
                            length -= fileLength;

                            if (length < storageLimit) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        int hash = Objects.hash(this.key, System.currentTimeMillis());
        this.file = new File(this.learningFolder + "/" + hash + ".yml");

        if (this.file.exists()) {
            this.reloadLearningFile();
        } else {
            this.configuration = YamlConfiguration.loadConfiguration(this.file);
        }
    }

    void setToFile(PlayerProfile profile, long time, int situation, double pattern) {
        SpartanBukkit.dataThread.executeIfSyncElseHere(() -> {
            this.count++;
            this.configuration.set(time + "." + profileOption, profile.getName());
            this.configuration.set(time + "." + situationOption, situation);
            this.configuration.set(time + "." + patternOption, pattern);
        });
    }

    void storeFiles() {
        SpartanBukkit.dataThread.executeIfSyncElseHere(() -> {
            try {
                this.configuration.save(this.file);

                if (this.count < 0) { // Short has overflowed (32767)
                    this.count = 0;
                    this.reloadLearningFile();
                }
            } catch (Exception ignored) {
            }
        });
    }

    private synchronized void loadFromFiles() {
        if (this.loading) {
            return;
        }
        this.loading = true;
        this.loaded = false;
        this.storeFiles();

        SpartanBukkit.dataThread.executeWithPriority(() -> {
            Map<Short, PatternGeneralization> generalizations = new HashMap<>();
            File[] files = this.getLearningFiles();

            if (files != null) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));
                Set<Map.Entry<Short, PatternGeneralization>> entries;

                synchronized (this.generalizations) {
                    entries = new HashSet<>(this.generalizations.entrySet());
                }

                for (Map.Entry<Short, PatternGeneralization> entry : entries) {
                    generalizations.put(
                            entry.getKey(),
                            entry.getValue() instanceof LinearPatternGeneralization
                                    ? new LinearPatternGeneralization(this, entry.getValue().scissors)
                                    : (entry.getValue() instanceof NonLinearPatternGeneralization
                                    ? new NonLinearPatternGeneralization(this, entry.getValue().scissors)
                                    : new NonLinearRankedPatternGeneralization(this, entry.getValue().scissors))
                    );
                }
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".yml")) {
                        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                        Set<String> keys = configuration.getKeys(false);

                        if (!keys.isEmpty()) {
                            boolean changed = false;

                            for (String key : keys) {
                                ConfigurationSection section = configuration.getConfigurationSection(key);

                                if (section == null) {
                                    continue;
                                }
                                Map<String, Object> data = section.getValues(false);

                                if (data.size() != 3) {
                                    this.deleteFromFile(configuration, key);
                                    changed = true;
                                    continue;
                                }
                                Object pattern = data.get(patternOption),
                                        situation = data.get(situationOption),
                                        profileObject = data.get(profileOption);

                                if (!(pattern instanceof Double || pattern instanceof Float)
                                        || !(situation instanceof Integer || situation instanceof Long || situation instanceof Short)
                                        || !(profileObject instanceof String)) {
                                    this.deleteFromFile(configuration, key);
                                    changed = true;
                                    continue;
                                }
                                try {
                                    String profileName = profileObject.toString();

                                    if (profileName.isEmpty()) {
                                        this.deleteFromFile(configuration, key);
                                        changed = true;
                                        continue;
                                    }
                                    long time = Long.parseLong(key);
                                    PlayerProfile profile = ResearchEngine.getPlayerProfile(profileName);

                                    for (PatternGeneralization generalization : generalizations.values()) {
                                        generalization.learn(
                                                profile,
                                                time,
                                                (int) situation,
                                                (double) pattern,
                                                false
                                        );
                                    }
                                } catch (Exception ignored) {
                                }
                            }

                            if (changed) {
                                try {
                                    configuration.save(file);
                                } catch (Exception ignored) {
                                }
                            }

                        }
                    }
                }
            }

            if (!generalizations.isEmpty()) {
                synchronized (this.generalizations) {
                    this.generalizations.clear();
                    this.generalizations.putAll(generalizations);
                }
            }
            this.loaded = true;
            this.loading = false;
        });
    }

    boolean isLoaded() {
        return this.loaded;
    }

    private void deleteFromFile(YamlConfiguration configuration, String key) {
        for (String option : Pattern.options) {
            configuration.set(key + "." + option, null);
        }
        configuration.set(key, null);
    }

    private File[] getLearningFiles() {
        File directory = new File(this.learningFolder);

        if (directory.exists()) {
            if (directory.isDirectory()) {
                return directory.listFiles();
            } else {
                directory.delete();
                return null;
            }
        } else {
            return null;
        }
    }

    // Separator

    public void learn(SpartanPlayer player, int situation, Number pattern) {
        long time = System.currentTimeMillis();
        Integer divisor = DetectionNotifications.getDivisor(player, false);
        boolean notifications = divisor != null,
                found = false,
                stored = false,
                include = !SpartanBukkit.testMode || !player.isWhitelisted()
                        && (!notifications || divisor != testingNotificationDivisor);

        synchronized (this.generalizations) {
            for (PatternGeneralization generalization : this.generalizations.values()) {
                if (notifications) {
                    found |= generalization.hasPatterns(situation);
                }
                stored |= generalization.learn(
                        player.getProfile(),
                        time,
                        situation,
                        pattern.doubleValue(),
                        include && !stored
                );
            }
        }

        if (!found && notifications) {
            String message = AwarenessNotifications.getOptionalNotification(
                    "Parts of Spartan's Machine Learning algorithm have insufficient data to check you. "
                            + (player.getProfile().isLegitimate()
                            ? "Continue playing LEGITIMATELY to train the algorithm and get better results."
                            : "Since you are " + player.getProfile().evidence.getType() + ", either clear your data via '/spartan info'"
                            + " and play LEGITIMATELY or find a legitimate player to help train the algorithm and get better results.")
            );

            if (message != null
                    && AwarenessNotifications.canSend(player.uuid, "pattern-data", 60)) {
                player.sendMessage(message);
            }
        }
    }

    public PatternGeneralization getGeneralization(Number generalization) {
        synchronized (this.generalizations) {
            return this.generalizations.get(generalization.shortValue());
        }
    }

}
