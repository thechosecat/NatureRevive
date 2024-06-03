package engineer.skyouo.plugins.naturerevive.spigot.commands;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.RegenOptions;
import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.config.adapters.MySQLDatabaseAdapter;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;

public class DebugCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 0){
            if (!(sender instanceof Player)){
                sender.sendMessage("player onely");
                return true;
            }
            if (args[0].equals("regenchunk")){
                Player player = (Player) sender;
                BukkitPositionInfo bukkitPositionInfo = new BukkitPositionInfo(player.getLocation(),1);
                bukkitPositionInfo.regenerateChunk();
                sender.sendMessage("[NatureRevive] 已嘗試重生當前區塊(Bukkit API)");
                return true;
            } else if (args[0].equals("regenchunk_weg")) {
                Player player = (Player) sender;
                BukkitPositionInfo bukkitPositionInfo = new BukkitPositionInfo(player.getLocation(),1);
                bukkitPositionInfo.regenerateChunk_FAWE();
                sender.sendMessage("[NatureRevive] 已嘗試重生當前區塊(FAWE API)");
                return true;
            }
            sender.sendMessage("[NatureRevive] /<cmd> regenchunk ,regenchunk_weg");
            return true;
        }


        sender.sendMessage("============================");
        sender.sendMessage("Queue tasks: ");
        for (Iterator<BukkitPositionInfo> it = NatureRevivePlugin.queue.iterator(); it.hasNext(); ) {
            BukkitPositionInfo task = it.next();
            sender.sendMessage(task.getLocation().toString() + " - " + task.getTTL());
        }

        sender.sendMessage(" ");

        sender.sendMessage("Database tasks: ");
        for (BukkitPositionInfo positionInfo : NatureRevivePlugin.databaseConfig.values()) {
            sender.sendMessage(positionInfo.getLocation().toString() + " - " + positionInfo.getTTL());
        }

        sender.sendMessage(" ");
        sender.sendMessage("Database no cache tasks: ");
        try {
            for (BukkitPositionInfo positionInfo : NatureRevivePlugin.databaseConfig.values()) {
                BukkitPositionInfo positionInfoNoCache = ((MySQLDatabaseAdapter) NatureRevivePlugin.databaseConfig).getNoCache(positionInfo);
                sender.sendMessage(positionInfoNoCache.getLocation().toString() + " - " + positionInfoNoCache.getTTL());
            }
        } catch (Exception ignored) {}

        sender.sendMessage("Time now is: " + System.currentTimeMillis());
        sender.sendMessage("============================");
        return true;
    }
}
