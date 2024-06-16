package engineer.skyouo.plugins.naturerevive.spigot.config.adapters;

import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.config.ReadonlyConfig;
import org.bukkit.Location;

import java.io.IOException;
import java.util.*;

import engineer.skyouo.plugins.naturerevive.spigot.config.DatabaseConfig;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

public class YamlDatabaseAdapter implements DatabaseConfig {
    private final File file = new File("plugins/NatureRevive/database.yml");

    private final YamlConfiguration configuration;

    public YamlDatabaseAdapter() {
        new File("plugins/NatureRevive").mkdirs();
        configuration = YamlConfiguration.loadConfiguration(file);
    }

    public void set(BukkitPositionInfo positionInfo) {
        if (positionInfo.getLocation().getWorld().getEnvironment() == World.Environment.THE_END){
            long ttl = positionInfo.getTTL();
            Random random = new Random();
            int value = random.nextInt(NatureRevivePlugin.readonlyConfig.end_random_day_offset + 1) + 1;
            BukkitPositionInfo positionInfo2 = new BukkitPositionInfo(positionInfo.getLocation(), ttl+value* 86400000L);
            configuration.set(formatLocation(positionInfo2.getLocation()), positionInfo2);
            return;
        }
        configuration.set(formatLocation(positionInfo.getLocation()), positionInfo);
    }
    public void set_ignore(BukkitPositionInfo positionInfo) {
        configuration.set(formatLocation(positionInfo.getLocation()), positionInfo);
    }

    public void unset(BukkitPositionInfo positionInfo) {
        configuration.set(safeFormatLocation(positionInfo), null);
    }

    public BukkitPositionInfo get(Location location) {
        return (BukkitPositionInfo) configuration.get(formatLocation(location));
    }

    public BukkitPositionInfo get(BukkitPositionInfo positionInfo) {
        return (BukkitPositionInfo) configuration.get(formatLocation(positionInfo.getLocation()), positionInfo);
    }

    public List<BukkitPositionInfo> values() {
        ArrayList<BukkitPositionInfo> positionInfos = new ArrayList<>();
        Set<String> keys = configuration.getKeys(false);

        for (String key : keys) {
            BukkitPositionInfo positionInfo = (BukkitPositionInfo) configuration.get(key);
            positionInfos.add(positionInfo);
        }

        return positionInfos;
    }

    public void save() throws IOException {
        configuration.save(file);
    }

    public void close() {
    }

    private String safeFormatLocation(BukkitPositionInfo location) {
        return new String(Base64.getEncoder().encode((location.getWorldName() + "|" + location.getLocation().getChunk().getX() + "|" + location.getLocation().getChunk().getZ()).getBytes()));
    }

    private String formatLocation(Location location) {
        return new String(Base64.getEncoder().encode((location.getWorld().getName() + "|" + location.getChunk().getX() + "|" + location.getChunk().getZ()).getBytes()));
    }
}