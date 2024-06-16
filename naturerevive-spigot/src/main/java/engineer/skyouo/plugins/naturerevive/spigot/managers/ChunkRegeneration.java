package engineer.skyouo.plugins.naturerevive.spigot.managers;

//import com.bekvon.bukkit.residence.protection.ClaimedResidence;
//import com.bekvon.bukkit.residence.protection.ResidenceManager;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.RegenOptions;
import engineer.skyouo.plugins.naturerevive.common.IPosCalculate;
import engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin;
import engineer.skyouo.plugins.naturerevive.spigot.catDebug.MySQL;
import engineer.skyouo.plugins.naturerevive.spigot.config.DatabaseConfig;
import engineer.skyouo.plugins.naturerevive.spigot.config.adapters.YamlDatabaseAdapter;
import engineer.skyouo.plugins.naturerevive.spigot.constants.OreBlocksCompat;
import engineer.skyouo.plugins.naturerevive.spigot.listeners.ObfuscateLootListener;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BlockDataChangeWithPos;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BlockStateWithPos;
import engineer.skyouo.plugins.naturerevive.spigot.structs.BukkitPositionInfo;
import engineer.skyouo.plugins.naturerevive.spigot.structs.NbtWithPos;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.Date;

import static engineer.skyouo.plugins.naturerevive.spigot.NatureRevivePlugin.*;

public class ChunkRegeneration {
    private static final UUID emptyUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static void regenerateFA(Chunk chunk, boolean regenBiomes) {
        long o = System.currentTimeMillis();
        com.sk89q.worldedit.world.World world2 = BukkitAdapter.adapt(chunk.getWorld());
        EditSession session = WorldEdit.getInstance().newEditSession(world2);
        Mask mask = session.getMask();
        BlockVector3 one = BlockVector3.at(chunk.getX()*16,-64,chunk.getZ()*16);
        BlockVector3 tow = BlockVector3.at(chunk.getX()*16+15,256,chunk.getZ()*16+15);
        Region region = new CuboidRegion(world2,one,tow);



        boolean success;
        try {
            session.setMask(null);
            //FAWE start
            session.setSourceMask(null);
            //FAWE end
            RegenOptions options = RegenOptions.builder()
                    .seed(chunk.getWorld().getSeed())
                    .regenBiomes(regenBiomes)
                    .build();
            success = world2.regenerate(region,session,options);
        } finally {
            session.setMask(mask);
            //FAWE start
            session.setSourceMask(mask);
            //FAWE end
        }
        if (success) {
            world2.refreshChunk(chunk.getX(),chunk.getZ());
            Bukkit.getLogger().info("[NatureRevive] 區快再生成功(FAWE-API)花費" + (System.currentTimeMillis()-o) +"ms " + chunk);
        } else {
            Bukkit.getLogger().warning("[NatureRevive] 區快再生失敗 (FAWE-API) " + chunk);
        }
        session.close();
    }
    public static void regenerateChunk_FAWE(BukkitPositionInfo bukkitPositionInfo) {
        Location location = bukkitPositionInfo.getLocation();

        List<NbtWithPos> nbtWithPos = new ArrayList<>();

        Chunk chunk = location.getChunk();

        if (!chunk.isLoaded()) {
            chunk.load();
        }

        ChunkSnapshot oldChunkSnapshot = chunk.getChunkSnapshot();

        // todo: make this asynchronous.
//        if (residenceAPI != null && NatureRevivePlugin.readonlyConfig.residenceStrictCheck) {
//            List<ClaimedResidence> residences = ((ResidenceManager) residenceAPI).getByChunk(chunk);
//            if (residences.size() > 0) {
//
//                for (BlockState blockState : chunk.getTileEntities()) {
//                    if (residenceAPI.getByLoc(new Location(location.getWorld(), blockState.getX(), blockState.getY(), blockState.getZ())) != null) {
//                        String nbt = nmsWrapper.getNbtAsString(chunk.getWorld(), blockState);
//
//                        nbtWithPos.add(new NbtWithPos(nbt, chunk.getWorld(), blockState.getX(), blockState.getY(), blockState.getZ()));
//                    }
//                }
//            }
//        }

        if (griefPreventionAPI != null && NatureRevivePlugin.readonlyConfig.griefPreventionStrictCheck) {
            Collection<me.ryanhamshire.GriefPrevention.Claim> griefPrevention = griefPreventionAPI.getClaims(chunk.getX(), chunk.getZ());
            if (griefPrevention.size() > 0) {
                for (BlockState blockState : chunk.getTileEntities()) {
                    if (griefPreventionAPI.getClaimAt(new Location(location.getWorld(), blockState.getX(), blockState.getY(), blockState.getZ()), true, null) != null){
                        String nbt = nmsWrapper.getNbtAsString(chunk.getWorld(), blockState);

                        nbtWithPos.add(new NbtWithPos(nbt, chunk.getWorld(), blockState.getX(), blockState.getY(), blockState.getZ()));
                    }
                }
            }
        }

//        if (griefDefenderAPI != null && readonlyConfig.griefDefenderStrictCheck) {
//            for (BlockState blockState : chunk.getTileEntities()){
//                UUID uuid = griefDefenderAPI.getClaimAt(new Location(location.getWorld(), blockState.getX(), blockState.getY(), blockState.getZ())).getOwnerUniqueId();
//                if (!uuid.equals(emptyUUID)) {
//                    String nbt = nmsWrapper.getNbtAsString(chunk.getWorld(), blockState);
//
//                    nbtWithPos.add(new NbtWithPos(nbt, chunk.getWorld(), blockState.getX(), blockState.getY(), blockState.getZ()));
//                }
//            }
//        }
        new BukkitRunnable() {
            @Override
            public void run() {
                regenerateFA(chunk,true);
                if (is_end_ship(chunk)){
                    Bukkit.getLogger().info("[NatureRevive] 該區塊為中界船頭，已新增鞘翅至該區塊 " + chunk.getBlock(0,0,0).getLocation());
                    MySQL.create_log_data( "["+readonlyConfig.hub_name+"] 已於終界世界 X: " + chunk.getX()*16 + " Z: " + chunk.getZ()*16 + "重生船頭+鞘翅",new Date());
                }
                //async重生完畢 接回Main third
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ObfuscateLootListener.randomizeChunkOre(chunk);
                        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
                            savingMovableStructure(chunk, oldChunkSnapshot);
                            if (griefPreventionAPI != null && readonlyConfig.griefPreventionStrictCheck)
                                griefPreventionOldStateRevert(chunk, oldChunkSnapshot, nbtWithPos);

                            if (coreProtectAPI != null && readonlyConfig.coreProtectLogging)
                                coreProtectAPILogging(chunk, oldChunkSnapshot);

                        });
                        Bukkit.getScheduler().runTaskLater(instance, () -> Bukkit.getPluginManager().callEvent(new ChunkPopulateEvent(chunk)), 4L);
                    }
                }.runTask(instance);
            }
        }.runTaskAsynchronously(instance);

        /*
        nmsWrapper.regenerateChunk(chunk.getWorld(), chunk.getX(), chunk.getZ(), (x, y, z) -> {
            if (residenceAPI != null && NatureRevivePlugin.readonlyConfig.residenceStrictCheck) {
                List<ClaimedResidence> residences = ((ResidenceManager) residenceAPI).getByChunk(chunk);
                if (residences.size() > 0) {
                    if (residenceAPI.getByLoc(new Location(location.getWorld(), x, y, z)) != null)
                        return true;
                }
            }

            if (griefPreventionAPI != null && NatureRevivePlugin.readonlyConfig.griefPreventionStrictCheck) {
                Collection<me.ryanhamshire.GriefPrevention.Claim> griefPrevention = griefPreventionAPI.getClaims(chunk.getX(), chunk.getZ());
                if (griefPrevention.size() > 0) {
                     if (griefPreventionAPI.getClaimAt(new Location(location.getWorld(), x, y, z), true, null) != null)
                         return true;
                }
            }

            if (griefDefenderAPI != null && readonlyConfig.griefDefenderStrictCheck) {
                UUID uuid = griefDefenderAPI.getClaimAt(new Location(location.getWorld(), x, y, z)).getOwnerUniqueId();
                if (!uuid.equals(emptyUUID))
                    return true;
            }

            return false;
        });
        */

//        ObfuscateLootListener.randomizeChunkOre(chunk);

        /*if (blocks.size() > 0) {
            for (BlockStateWithPos blockWithPos : blocks) {
                BlockPos bp = new BlockPos(blockWithPos.getLocation().getBlockX(), blockWithPos.getLocation().getBlockY(), blockWithPos.getLocation().getBlockZ());
                (((CraftWorld) location.getWorld()).getHandle()).setBlock(bp, blockWithPos.getBlockState(), 3);
            }
        }

        if (nbtWithPos.size() > 0) {
            for (NbtWithPos tileEntityPos : nbtWithPos) {
                BlockEntity tileEntity = (((CraftWorld) location.getWorld()).getHandle()).getBlockEntity(new BlockPos(tileEntityPos.getLocation().getBlockX(), tileEntityPos.getLocation().getBlockY(), tileEntityPos.getLocation().getBlockZ()));
                try {
                    tileEntity.load(tileEntityPos.getNbt());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }*/

        // location.getWorld().refreshChunk(chunk.getX(), chunk.getZ());

//        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
//            savingMovableStructure(chunk, oldChunkSnapshot);
//
////            if (residenceAPI != null && readonlyConfig.residenceStrictCheck)
////                residenceOldStateRevert(chunk, oldChunkSnapshot, nbtWithPos);
//
//            if (griefPreventionAPI != null && readonlyConfig.griefPreventionStrictCheck)
//                griefPreventionOldStateRevert(chunk, oldChunkSnapshot, nbtWithPos);
//
////            if (griefDefenderAPI != null && readonlyConfig.griefDefenderStrictCheck)
////                griefDefenderOldStateRevert(chunk, oldChunkSnapshot, nbtWithPos);
//
//            if (coreProtectAPI != null && readonlyConfig.coreProtectLogging)
//                coreProtectAPILogging(chunk, oldChunkSnapshot);
//        });
//
//        // location.getChunk().unload(true);
//        Bukkit.getScheduler().runTaskLater(instance, () -> Bukkit.getPluginManager().callEvent(new ChunkPopulateEvent(chunk)), 4L);
    }
    private static boolean is_end_ship(Chunk chunk){
        if (!chunk.getWorld().getEnvironment().equals(World.Environment.THE_END)){
            return false;
        }
        Biome biome = chunk.getBlock(0,0,0).getBiome();
        if (biome == Biome.THE_END){
            return false;
        }
        ChunkSnapshot chunkSnapshot = chunk.getChunkSnapshot();
        int c = 0;
        int p = 0;
        int pl =0;
        int ps =0;
        List<Location> l = new ArrayList<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    Material block1 = chunkSnapshot.getBlockType(x,y,z);
                    if (block1 == Material.CHEST) {
                        c++;
                        l.add(getLocationFromBlockType(chunkSnapshot,x,y,z));
                    }else if (block1 == Material.PURPUR_BLOCK){
                        p++;
                    }
                    else if (block1 == Material.PURPUR_PILLAR){
                        pl++;
                    }
                    else if (block1 == Material.PURPUR_STAIRS){
                        ps++;
                    }
                }
            }
        }
        // 可能為剛好被切割的船頭，開始進行鄰近區快判斷
        if (c == 1){
            World world = chunk.getWorld();
            Bukkit.getLogger().info("[NatureRevive] 疑似切割 相鄰區快檢查中...");
            Directional directional = (Directional) l.get(0).getBlock().getBlockData();
            BlockFace blockFace = directional.getFacing();

            List<Chunk> ship_chunks = new ArrayList<>();
            if (blockFace.equals(BlockFace.WEST) || blockFace.equals(BlockFace.EAST)){
                // 面相X方位 相對位置為Z
                ship_chunks.add(world.getChunkAt(chunk.getX(),chunk.getZ()+1));
                ship_chunks.add(world.getChunkAt(chunk.getX(),chunk.getZ()-1));
            }
            else if (blockFace.equals(BlockFace.SOUTH) || blockFace.equals(BlockFace.NORTH)){
                // 面相Z方位 相對位置為X
                ship_chunks.add(world.getChunkAt(chunk.getX()+1,chunk.getZ()));
                ship_chunks.add(world.getChunkAt(chunk.getX()-1,chunk.getZ()));
            }
            // 檢查要檢查的區塊是否包含領地
            for (Chunk chunk1 : ship_chunks) {
                if (griefPreventionAPI.getClaims(chunk1.getX(), chunk1.getZ()).size() != 0) {
                    Bukkit.getLogger().info("[NatureRevive] 相鄰的檢查區快具有領地 判定結束");
                    return false;
                } else {
                    regenerateFA(chunk1, true);
                    ChunkSnapshot chunkSnapshot1 = chunk1.getChunkSnapshot();
                    int chest = 0;
                    List<Location> chests = new ArrayList<>();
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = 0; y < 256; y++) {
                                Material block1 = chunkSnapshot1.getBlockType(x, y, z);
                                if (block1 == Material.CHEST) {
                                    chest++;
                                    chests.add(getLocationFromBlockType(chunkSnapshot1, x, y, z));
                                }
                            }
                        }
                    }
                    if (chest != 1){
                        continue;
                    }
                    if (!Chest_loc_check(l.get(0),chests.get(0))){
                        continue;
                    }
                    if (ely_amount >= readonlyConfig.ely_amount){
                        Bukkit.getLogger().info("[NatureRevive] 今天已重生" + ely_amount + "個鞘翅，將不再重生新鞘翅");
                        BukkitPositionInfo positionInfo = new BukkitPositionInfo(chunk1.getBlock(0,0,0).getLocation(), System.currentTimeMillis() + NatureRevivePlugin.readonlyConfig.parseDuration("1d"));
                        NatureRevivePlugin.databaseConfig.set_ignore(positionInfo);
                        return false;
                    }
                    Place_ship_ely(l.get(0),chests.get(0),blockFace);
                    ely_amount++;
                    return true;
                }
            }
            return false;
        }
        // 正常判斷法
        if (c != 2 || p < 14 || pl < 2 || ps < 2){
            return false;
        }
        if  (!Chest_loc_check(l.get(0),l.get(1))){
            return false;
        }
        // 放置鞘翅 (sync)
        if (ely_amount >= readonlyConfig.ely_amount){
            Bukkit.getLogger().info("[NatureRevive] 今天已重生" + ely_amount + "個鞘翅，將不再重生新鞘翅");
            BukkitPositionInfo positionInfo = new BukkitPositionInfo(chunk.getBlock(0,0,0).getLocation(), System.currentTimeMillis() + NatureRevivePlugin.readonlyConfig.parseDuration("1d"));
            NatureRevivePlugin.databaseConfig.set_ignore(positionInfo);
            return false;
        }
        Directional directional = (Directional) l.get(0).getBlock().getBlockData();
        Place_ship_ely(l.get(0),l.get(1),directional.getFacing());
        ely_amount++;
        return true;
    }
    private static int ely_amount = 0;


    private static void Place_ship_ely(Location chest1,Location chest2,BlockFace eylyFace){
        Location location = get_new_flame_loc(chest1,chest2).add(0,1,0);
        new BukkitRunnable() {
            @Override
            public void run() {
                Block block = location.getBlock();
                for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 1, 1, 1)) {
                    if (entity instanceof ItemFrame) {
                        ((ItemFrame) entity).setItem(new ItemStack(Material.AIR));
                        entity.remove();
                        break;
                    }
                }
                location.getBlock().setType(Material.AIR);
                coreProtectAPI.logRemoval(readonlyConfig.coreProtectUserName, location, Material.AIR, location.getBlock().getBlockData());
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ItemFrame itemFrame = location.getWorld().spawn(location, ItemFrame.class);
                        itemFrame.setFacingDirection(eylyFace, true);
                        itemFrame.setItem(new ItemStack(Material.ELYTRA));
                        coreProtectAPI.logPlacement(readonlyConfig.coreProtectUserName, location, Material.ITEM_FRAME, location.getBlock().getBlockData());
                    }
                }.runTaskLater(instance, 1L);
            }
        }.runTask(instance);
    }


    private static boolean Chest_loc_check(Location loc1,Location loc2){
        if (loc1.getY() != loc2.getY()) {
            return false;
        }
        if (loc1.distance(loc2) != 2.0) {
            return false;
        }
        Directional directional1 = (Directional) loc1.getBlock().getBlockData();
        Directional directional2 = (Directional) loc2.getBlock().getBlockData();
        return directional1.getFacing() == directional2.getFacing();
    }
    private static Location getLocationFromBlockType(ChunkSnapshot chunkSnapshot, int x, int y, int z) {
        World world = Bukkit.getWorld(chunkSnapshot.getWorldName());
        int worldX = (chunkSnapshot.getX() << 4) + x;
        int worldZ = (chunkSnapshot.getZ() << 4) + z;
        return new Location(world, worldX, y, worldZ);
    }
    private static Location get_new_flame_loc(Location loc1,Location loc2){
        if (loc1.getX() == loc2.getX()){
            double Z = (loc1.getZ() + loc2.getZ()) / 2;
            return new Location(loc1.getWorld(),loc1.getX(),loc1.getY(),Z);
        }else {
            double X = (loc1.getX() + loc2.getX()) / 2;
            return new Location(loc1.getWorld(),X,loc1.getY(),loc1.getZ());
        }
    }

    public static void regenerateChunk(BukkitPositionInfo bukkitPositionInfo) {
        Location location = bukkitPositionInfo.getLocation();

        List<NbtWithPos> nbtWithPos = new ArrayList<>();

        Chunk chunk = location.getChunk();

        if (!chunk.isLoaded()) {
            chunk.load();
        }

        ChunkSnapshot oldChunkSnapshot = chunk.getChunkSnapshot();

        // todo: make this asynchronous.
//        if (residenceAPI != null && NatureRevivePlugin.readonlyConfig.residenceStrictCheck) {
//            List<ClaimedResidence> residences = ((ResidenceManager) residenceAPI).getByChunk(chunk);
//            if (residences.size() > 0) {
//
//                for (BlockState blockState : chunk.getTileEntities()) {
//                    if (residenceAPI.getByLoc(new Location(location.getWorld(), blockState.getX(), blockState.getY(), blockState.getZ())) != null) {
//                        String nbt = nmsWrapper.getNbtAsString(chunk.getWorld(), blockState);
//
//                        nbtWithPos.add(new NbtWithPos(nbt, chunk.getWorld(), blockState.getX(), blockState.getY(), blockState.getZ()));
//                    }
//                }
//            }
//        }

        if (griefPreventionAPI != null && NatureRevivePlugin.readonlyConfig.griefPreventionStrictCheck) {
            Collection<me.ryanhamshire.GriefPrevention.Claim> griefPrevention = griefPreventionAPI.getClaims(chunk.getX(), chunk.getZ());
            if (griefPrevention.size() > 0) {
                for (BlockState blockState : chunk.getTileEntities()) {
                    if (griefPreventionAPI.getClaimAt(new Location(location.getWorld(), blockState.getX(), blockState.getY(), blockState.getZ()), true, null) != null){
                       String nbt = nmsWrapper.getNbtAsString(chunk.getWorld(), blockState);

                       nbtWithPos.add(new NbtWithPos(nbt, chunk.getWorld(), blockState.getX(), blockState.getY(), blockState.getZ()));
                    }
                }
            }
        }

//        if (griefDefenderAPI != null && readonlyConfig.griefDefenderStrictCheck) {
//            for (BlockState blockState : chunk.getTileEntities()){
//                UUID uuid = griefDefenderAPI.getClaimAt(new Location(location.getWorld(), blockState.getX(), blockState.getY(), blockState.getZ())).getOwnerUniqueId();
//                if (!uuid.equals(emptyUUID)) {
//                    String nbt = nmsWrapper.getNbtAsString(chunk.getWorld(), blockState);
//
//                    nbtWithPos.add(new NbtWithPos(nbt, chunk.getWorld(), blockState.getX(), blockState.getY(), blockState.getZ()));
//                }
//            }
//        }

        chunk.getWorld().regenerateChunk(chunk.getX(), chunk.getZ());
        /*
        nmsWrapper.regenerateChunk(chunk.getWorld(), chunk.getX(), chunk.getZ(), (x, y, z) -> {
            if (residenceAPI != null && NatureRevivePlugin.readonlyConfig.residenceStrictCheck) {
                List<ClaimedResidence> residences = ((ResidenceManager) residenceAPI).getByChunk(chunk);
                if (residences.size() > 0) {
                    if (residenceAPI.getByLoc(new Location(location.getWorld(), x, y, z)) != null)
                        return true;
                }
            }

            if (griefPreventionAPI != null && NatureRevivePlugin.readonlyConfig.griefPreventionStrictCheck) {
                Collection<me.ryanhamshire.GriefPrevention.Claim> griefPrevention = griefPreventionAPI.getClaims(chunk.getX(), chunk.getZ());
                if (griefPrevention.size() > 0) {
                     if (griefPreventionAPI.getClaimAt(new Location(location.getWorld(), x, y, z), true, null) != null)
                         return true;
                }
            }

            if (griefDefenderAPI != null && readonlyConfig.griefDefenderStrictCheck) {
                UUID uuid = griefDefenderAPI.getClaimAt(new Location(location.getWorld(), x, y, z)).getOwnerUniqueId();
                if (!uuid.equals(emptyUUID))
                    return true;
            }

            return false;
        });
        */

        ObfuscateLootListener.randomizeChunkOre(chunk);

        /*if (blocks.size() > 0) {
            for (BlockStateWithPos blockWithPos : blocks) {
                BlockPos bp = new BlockPos(blockWithPos.getLocation().getBlockX(), blockWithPos.getLocation().getBlockY(), blockWithPos.getLocation().getBlockZ());
                (((CraftWorld) location.getWorld()).getHandle()).setBlock(bp, blockWithPos.getBlockState(), 3);
            }
        }

        if (nbtWithPos.size() > 0) {
            for (NbtWithPos tileEntityPos : nbtWithPos) {
                BlockEntity tileEntity = (((CraftWorld) location.getWorld()).getHandle()).getBlockEntity(new BlockPos(tileEntityPos.getLocation().getBlockX(), tileEntityPos.getLocation().getBlockY(), tileEntityPos.getLocation().getBlockZ()));
                try {
                    tileEntity.load(tileEntityPos.getNbt());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }*/

        // location.getWorld().refreshChunk(chunk.getX(), chunk.getZ());

        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
            savingMovableStructure(chunk, oldChunkSnapshot);

//            if (residenceAPI != null && readonlyConfig.residenceStrictCheck)
//                residenceOldStateRevert(chunk, oldChunkSnapshot, nbtWithPos);

            if (griefPreventionAPI != null && readonlyConfig.griefPreventionStrictCheck)
                griefPreventionOldStateRevert(chunk, oldChunkSnapshot, nbtWithPos);

//            if (griefDefenderAPI != null && readonlyConfig.griefDefenderStrictCheck)
//                griefDefenderOldStateRevert(chunk, oldChunkSnapshot, nbtWithPos);

            if (coreProtectAPI != null && readonlyConfig.coreProtectLogging)
                coreProtectAPILogging(chunk, oldChunkSnapshot);
        });

        // location.getChunk().unload(true);
        Bukkit.getScheduler().runTaskLater(instance, () -> Bukkit.getPluginManager().callEvent(new ChunkPopulateEvent(chunk)), 4L);
    }

    private static void coreProtectAPILogging(Chunk chunk, ChunkSnapshot oldChunkSnapshot) {
        synchronized (blockDataChangeWithPos) {
            for (int x = 0; x < 16; x++) {
                for (int y = nmsWrapper.getWorldMinHeight(chunk.getWorld()); y < chunk.getWorld().getMaxHeight(); y++) {
                    for (int z = 0; z < 16; z++) {
                        Block newBlock = chunk.getBlock(x, y, z);

                        Material oldBlockType = oldChunkSnapshot.getBlockType(x, y, z);
                        Material newBlockType = newBlock.getType();

                        if (OreBlocksCompat.contains(oldBlockType)) continue;

                        if (!oldBlockType.equals(newBlockType)) {
                            Location location = new Location(chunk.getWorld(), (chunk.getX() << 4) + x, y, (chunk.getZ() << 4) + z);
                            BlockData oldBlockData = oldChunkSnapshot.getBlockData(x, y, z);
                            BlockData newBlockData = newBlock.getBlockData();
                            if (oldBlockType.equals(Material.AIR)) {
                                // new block put
                                //coreProtectAPI.logPlacement(readonlyConfig.coreProtectUserName, location, newBlockType, newBlock.getBlockData());
                                blockDataChangeWithPos.add(new BlockDataChangeWithPos(location, oldBlockData, newBlockData, BlockDataChangeWithPos.Type.PLACEMENT));
                            } else {
                                // Block break

                                //coreProtectAPI.logRemoval(readonlyConfig.coreProtectUserName, location, oldBlockType, oldBlockData);
                                if (!newBlockType.equals(Material.AIR)) {
                                    blockDataChangeWithPos.add(new BlockDataChangeWithPos(location, oldBlockData, newBlockData, BlockDataChangeWithPos.Type.REPLACE));
                                    //coreProtectAPI.logPlacement(readonlyConfig.coreProtectUserName, location, newBlockType, newBlock.getBlockData());
                                } else {
                                    blockDataChangeWithPos.add(new BlockDataChangeWithPos(location, oldBlockData, newBlockData, BlockDataChangeWithPos.Type.REMOVAL));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void residenceOldStateRevert(Chunk chunk, ChunkSnapshot oldChunkSnapshot, List<NbtWithPos> tileEntities) {
        Map<Location, BlockData> perversedBlocks = new HashMap<>();

//        List<ClaimedResidence> residences = ((ResidenceManager) residenceAPI).getByChunk(chunk);
//        if (residences.size() > 0) {
//            for (int x = 0; x < 16; x++) {
//                for (int y = nmsWrapper.getWorldMinHeight(chunk.getWorld()); y <= chunk.getWorld().getMaxHeight(); y++) {
//                    for (int z = 0; z < 16; z++) {
//                        Location targetLocation = new Location(chunk.getWorld(), (chunk.getX() << 4) + x, y, (chunk.getZ() << 4) + z);
//                        if (residenceAPI.getByLoc(targetLocation) != null) {
//                            BlockData block = oldChunkSnapshot.getBlockData(x, y, z);
//                            if (!chunk.getBlock(x, y, z).getBlockData().equals(block)) {
//                                perversedBlocks.put(targetLocation, block);
//                            }
//                        }
//                    }
//                }
//            }
//
//            setBlocksSynchronous(perversedBlocks, tileEntities);
//
//            /*if (tileEntities.size() > 0) {
//                for (NbtWithPos tileEntityPos : tileEntities) {
//                    BlockEntity tileEntity = (((CraftWorld) location.getWorld()).getHandle()).getBlockEntity(new BlockPos(tileEntityPos.getLocation().getBlockX(), tileEntityPos.getLocation().getBlockY(), tileEntityPos.getLocation().getBlockZ()));
//                    try {
//                        tileEntity.load(tileEntityPos.getNbt());
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }*/
//        }
    }

    private static void griefPreventionOldStateRevert(Chunk chunk, ChunkSnapshot oldChunkSnapshot, List<NbtWithPos> tileEntities){
        Map<Location, BlockData> perversedBlocks = new HashMap<>();

        Collection<me.ryanhamshire.GriefPrevention.Claim> GriefPrevention = griefPreventionAPI.getClaims(chunk.getX(), chunk.getZ());
        if (GriefPrevention.size() > 0) {
            for (int x = 0; x < 16; x++) {
                for (int y = nmsWrapper.getWorldMinHeight(chunk.getWorld()); y <= chunk.getWorld().getMaxHeight() - 1; y++) {
                    for (int z = 0; z < 16; z++) {
                        Location targetLocation = new Location(chunk.getWorld(), (chunk.getX() << 4) + x, y, (chunk.getZ() << 4) + z);
                        if (griefPreventionAPI.getClaimAt(targetLocation, true, null) != null){
                            try {
                                BlockData block = oldChunkSnapshot.getBlockData(x, y, z);
                                if (!chunk.getBlock(x, y, z).getBlockData().equals(block)) {
                                    perversedBlocks.put(targetLocation, block);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            setBlocksSynchronous(perversedBlocks, tileEntities);
        }
    }

    private static void griefDefenderOldStateRevert(Chunk chunk, ChunkSnapshot oldChunkSnapshot, List<NbtWithPos> tileEntities) {
//        Map<Location, BlockData> perversedBlocks = new HashMap<>();
//
//        List<UUID> claimUUIDList = new ArrayList<>();for (int x = 0; x < 16; x++) {
//            for (int y = nmsWrapper.getWorldMinHeight(chunk.getWorld()); y < chunk.getWorld().getMaxHeight() - 1; y++) {
//                for (int z = 0; z < 16; z++) {
//                    Location claimLocation = chunk.getBlock(x, y, z).getLocation();
//                    UUID uuid = griefDefenderAPI.getClaimAt(claimLocation).getOwnerUniqueId();
//
//                    if (!uuid.equals(emptyUUID)) {
//                        com.griefdefender.api.claim.Claim claim = griefDefenderAPI.getClaimAt(claimLocation);
//                        UUID claimUUID = claim.getUniqueId();
//                        if (!claimUUIDList.contains(claimUUID)) {
//                            claimUUIDList.add(claimUUID);
//                        }
//                    }
//                }
//            }
//        }
//
//        if (claimUUIDList.size() > 0) {
//            for (int x = 0; x < 16; x++) {
//                for (int y = nmsWrapper.getWorldMinHeight(chunk.getWorld()); y < chunk.getWorld().getMaxHeight() - 1 ; y++) {
//                    for (int z = 0; z < 16; z++) {
//                        Location targetLocation = new Location(chunk.getWorld(), (chunk.getX() << 4) + x, y, (chunk.getZ() << 4) + z);
//                        UUID uuid = griefDefenderAPI.getClaimAt(targetLocation).getOwnerUniqueId();
//                        if (!uuid.equals(emptyUUID)) {
//                            try {
//                                BlockData block = oldChunkSnapshot.getBlockData(x, y, z);
//                                if (!chunk.getBlock(x, y, z).getBlockData().equals(block)) {
//                                    perversedBlocks.put(targetLocation, block);
//                                }
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        setBlocksSynchronous(perversedBlocks, tileEntities);
    }

    private static void savingMovableStructure(Chunk chunk, ChunkSnapshot oldChunkSnapshot) {
        Map<Location, BlockData> perversedBlocks = new HashMap<>();
        for (int x = 0; x < 16; x++) {
            for (int y = nmsWrapper.getWorldMinHeight(chunk.getWorld()); y < chunk.getWorld().getMaxHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    Material blockType = oldChunkSnapshot.getBlockType(x, y, z);
                    Location originLocation = new Location(chunk.getWorld(), (chunk.getX() << 4) + x, y, (chunk.getZ() << 4) + z);

                    if ((blockType.equals(Material.END_PORTAL) || blockType.equals(Material.END_GATEWAY)) && !perversedBlocks.containsKey(originLocation)) {

                        for (int i = -2; i <= 2; i++)
                            for (int j = -2; j <= 2; j++)
                                for (int k = -2; k <= 2; k++) {
                                    Location neighborLocation = originLocation.clone().add(i, j, k);
                                    if (isNotInTheChunk(chunk, neighborLocation)) continue;

                                    int[] xyz = convertLocationToInChunkXYZ(neighborLocation);

                                    Material targetType = oldChunkSnapshot.getBlockType(xyz[0], xyz[1], xyz[2]);

                                    if (targetType.equals(Material.END_PORTAL) || targetType.equals(Material.END_GATEWAY) || targetType.equals(Material.BEDROCK))
                                        perversedBlocks.put(neighborLocation, oldChunkSnapshot.getBlockData(xyz[0], xyz[1], xyz[2]));
                                }
                    } else if (blockType.equals(Material.NETHER_PORTAL) && !perversedBlocks.containsKey(originLocation)) {
                        for (int i = -1; i <= 1; i++)
                            for (int j = -1; j <= 1; j++)
                                for (int k = -1; k <= 1; k++) {
                                    Location neighborLocation = originLocation.clone().add(i, j, k);
                                    if (isNotInTheChunk(chunk, neighborLocation)) continue;

                                    int[] xyz = convertLocationToInChunkXYZ(neighborLocation);

                                    Material targetType = oldChunkSnapshot.getBlockType(xyz[0], xyz[1], xyz[2]);

                                    if (targetType.equals(Material.NETHER_PORTAL) || targetType.equals(Material.OBSIDIAN))
                                        perversedBlocks.put(neighborLocation, oldChunkSnapshot.getBlockData(xyz[0], xyz[1], xyz[2]));
                                }
                    } else if (blockType.equals(Material.BEDROCK)) {
                        if (!chunk.getWorld().getEnvironment().equals(World.Environment.THE_END))
                            continue;

                        if (isInSpecialChunks(chunk)) {
                            perversedBlocks.put(originLocation, oldChunkSnapshot.getBlockData(x, y, z));

                            Location neighborLocation = originLocation.clone().add(0, 1, 0);
                            perversedBlocks.put(neighborLocation, oldChunkSnapshot.getBlockData(x, y + 1, z));
                            continue;
                        }

                        for (int i = -2; i <= 2; i++)
                            for (int j = -2; j <= 2; j++)
                                for (int k = -2; k <= 2; k++) {
                                    Location neighborLocation = originLocation.clone().add(i, j, k);
                                    Material neighborBlockType = neighborLocation.getBlock().getType();

                                    if (perversedBlocks.containsKey(neighborLocation) || (neighborBlockType.equals(Material.END_GATEWAY)) || neighborBlockType.equals(Material.END_PORTAL)) {
                                        perversedBlocks.put(originLocation, oldChunkSnapshot.getBlockData(x, y, z));
                                        break;
                                    }
                                }
                    } else if (blockType.equals(Material.OBSIDIAN)) {
                        for (int i = -1; i <= 1; i++)
                            for (int j = -1; j <= 1; j++)
                                for (int k = -1; k <= 1; k++) {
                                    Location neighborLocation = originLocation.clone().add(i, j, k);
                                    if (isNotInTheChunk(chunk, neighborLocation)) continue;

                                    if (perversedBlocks.containsKey(neighborLocation)) {
                                        perversedBlocks.put(originLocation, oldChunkSnapshot.getBlockData(x, y, z));
                                        break;
                                    }
                                }
                    } else if (blockType.equals(Material.DRAGON_EGG)) {
                        Location neighborLocation = originLocation.clone().add(0, -1, 0);
                        if (perversedBlocks.containsKey(neighborLocation)) {
                            perversedBlocks.put(originLocation, oldChunkSnapshot.getBlockData(x, y, z));
                        }
                    } else if (blockType.equals(Material.WALL_TORCH)) {
                        if (!chunk.getWorld().getEnvironment().equals(World.Environment.THE_END))
                            continue;

                        if (isInSpecialChunks(chunk)) {
                            perversedBlocks.put(originLocation, oldChunkSnapshot.getBlockData(x, y, z));
                            continue;
                        }

                        for (int i = -1; i <= 1; i++)
                            for (int k = -1; k <= 1; k++) {
                                Location neighborLocation = originLocation.clone().add(i, 0, k);
                                if (isNotInTheChunk(chunk, neighborLocation)) continue;

                                if (perversedBlocks.containsKey(neighborLocation)) {
                                    perversedBlocks.put(originLocation, oldChunkSnapshot.getBlockData(x, y, z));
                                    break;
                                }
                            }
                    }
                }
            }
        }

        setBlocksSynchronous(perversedBlocks, Collections.EMPTY_LIST);
    }

    private static void setBlocksSynchronous(Map<Location, BlockData> perversedBlocks, List<NbtWithPos> tileEntities) {
        synchronized (blockStateWithPosQueue) {
            for (Location location : perversedBlocks.keySet()) {
                boolean findTheNbt = isFindTheNbt(perversedBlocks, tileEntities, location);

                if (!findTheNbt)
                    blockStateWithPosQueue.add(new BlockStateWithPos(nmsWrapper.convertBlockDataToBlockState(perversedBlocks.get(location)), location));
            }
        }
    }

    private static boolean isFindTheNbt(Map<Location, BlockData> perversedBlocks, List<NbtWithPos> tileEntities, Location location) {
        boolean findTheNbt = false;
        for (NbtWithPos nbtWithPos : tileEntities) {
            if (nbtWithPos.getLocation().equals(location)) {
                blockStateWithPosQueue.add(new BlockStateWithPos(nmsWrapper.convertBlockDataToBlockState(perversedBlocks.get(location)), location, nbtWithPos.getNbt()));
                findTheNbt = true;
                break;
            }
        }
        return findTheNbt;
    }

    public static File takeSnapshot(Chunk chunk) throws IOException {

        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        out.writeUTF(chunk.getX() + "," + chunk.getZ());

        for (int x = 0; x < 16; x++) {
            for (int y = nmsWrapper.getWorldMinHeight(chunk.getWorld()); y <= chunk.getWorld().getMaxHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    String nbt = block.getBlockData().getAsString();

                    out.writeUTF(x + ";" + y + ";" + z + ";" + block.getType() + ";" + nbt);
                }
            }
        }

        for (BlockState blockState : chunk.getTileEntities()) {
            String nbt = nmsWrapper.getNbtAsString(chunk.getWorld(), blockState);
            out.writeUTF(blockState.getX() + ";" + blockState.getY() + ";" + blockState.getZ() + ";" + nbt);
        }

        new File("plugins/NatureRevive/snapshots").mkdirs();

        FileOutputStream outputStream = new FileOutputStream("plugins/NatureRevive/snapshots/" + chunk.hashCode() + ".snapshot");

        outputStream.write(out.toByteArray());
        outputStream.close();

        return new File("plugins/NatureRevive/snapshots/" + chunk.hashCode() + ".snapshot");
    }

    public static Chunk revertSnapshot(World world, File file) throws IOException {
        ByteArrayDataInput inputStream = ByteStreams.newDataInput(Files.readAllBytes(file.toPath()));
        String[] coordsInString = inputStream.readUTF().split(",");

        Chunk chunk = world.getChunkAt(Integer.parseInt(coordsInString[0]), Integer.parseInt(coordsInString[1]));

        if (!chunk.isLoaded())
            chunk.load();

        List<BlockStateWithPos> blockList = new ArrayList<>();
        List<NbtWithPos> nbtList = new ArrayList<>();

        while (true) {
            String[] argument = null;
            try {
                String data = inputStream.readUTF();
                if (data.equals("\n")) continue;

                argument = data.split(";");

                if (argument.length == 5) {
                    blockList.add(new BlockStateWithPos(nmsWrapper.convertBlockDataToBlockState(Bukkit.createBlockData(argument[4])), new Location(world, Integer.parseInt(argument[0]), Integer.parseInt(argument[1]), Integer.parseInt(argument[2]))));
                } else {
                    nbtList.add(new NbtWithPos(argument[3], new Location(world, Integer.parseInt(argument[0]), Integer.parseInt(argument[1]), Integer.parseInt(argument[2]))));
                }
            } catch (Exception e) {
                break;
            }
        }
        
        for (BlockStateWithPos block : blockList) {
            nmsWrapper.setBlockNMS(chunk.getWorld(), (chunk.getX() << 4) + block.getLocation().getBlockX(), block.getLocation().getBlockY(), (chunk.getZ() << 4) + block.getLocation().getBlockZ(), block.getBlockState().getBlockData());
        }

        for (NbtWithPos nbtWithPos : nbtList) {
            try {
                nmsWrapper.loadTileEntity(chunk.getWorld(), nbtWithPos.getLocation().getBlockX(), nbtWithPos.getLocation().getBlockY(), nbtWithPos.getLocation().getBlockZ(), nbtWithPos.getNbt());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return chunk;
    }

    private static Location getMiddleOfLocation(Location location) {
        Chunk chunk = location.getChunk();
        return new Location(location.getWorld(), (chunk.getX() << 4) + 8, location.getBlockY(), (chunk.getZ() << 4) + 8);
    }

    private static boolean isNotInTheChunk(Chunk chunk, Location location) {
        return location.getChunk().getX() != chunk.getX() || location.getChunk().getZ() != chunk.getZ();
    }

    // The method is hardcoded to detect the end gateway.
    private static boolean isInSpecialChunks(Chunk chunk) {
        return (chunk.getX() == 0 || chunk.getX() == -1) && (chunk.getZ() == 0 || chunk.getZ() == -1);
    }

    private static int[] convertLocationToInChunkXYZ(Location location) {
        return new int[] { (location.getBlockX() - ((location.getBlockX() >> 4) << 4)), location.getBlockY(), (location.getBlockZ() - ((location.getBlockZ() >> 4) << 4)) };
    };

    private class BlockDataWithPos {
        private final BlockData blockData;
        private final Location location;

        public BlockDataWithPos(BlockData blockState, Location location) {
            this.blockData = blockState;
            this.location = location;
        }

        public Location getLocation() {
            return location;
        }

        public BlockData getBlockData() {
            return blockData;
        }
    }
}
