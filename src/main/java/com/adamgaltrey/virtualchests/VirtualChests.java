package com.adamgaltrey.virtualchests;

import com.adamgaltrey.virtualchests.data.Loc;
import com.adamgaltrey.virtualchests.data.VirtualInventory;
import com.adamgaltrey.virtualchests.managers.ChestManager;
import com.adamgaltrey.virtualchests.managers.VirtualLocations;
import com.adamgaltrey.virtualchests.listeners.ChestOpenListener;
import com.adamgaltrey.virtualchests.sql.SQLConfig;
import com.adamgaltrey.virtualchests.sql.SyncSQL;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created by Adam on 06/11/2014.
 */
public class VirtualChests extends JavaPlugin {

    public static Logger logger;
    public static SyncSQL sql;
    private static WorldGuardPlugin wgp;

    public static Plugin plugin;
    private static Economy economy = null;

    public static final File root = new File("plugins" + File.separator + "VirtualChests"),
            sqlConfig = new File(root + File.separator + "sqlconfig.yml"),
            generalConfig = new File(root + File.separator + "config.yml");

    public static final String table = "virtualchests", openPermission = "virtualchests.open.cmd";
    private static int pageCost = 100;


    @Override
    public void onEnable() {
        logger = getLogger();
        plugin = this;

        root.mkdir();

        Bukkit.getPluginManager().addPermission(new Permission(openPermission));
        getLogger().info("Registered permission [" + openPermission + "]");

        SQLConfig sqlC = new SQLConfig(sqlConfig);
        if (sqlC.createDefault()) {
            getLogger().info("Created default SQL configuration file.");
        }
        sql = sqlC.load();

        FileConfiguration io = YamlConfiguration.loadConfiguration(generalConfig);

        if (!generalConfig.exists()) {
            try {
                generalConfig.createNewFile();
               /*
                    <extra page cost> = <page.cost> * <# pages>

                    So for their first extra page
                        cost = 100 * 1 = 100 gold

                    And for their second additional page
                        cost = 100 * 2 = 200 gold
                */
                io.set("page.cost", 100);
                /*
                    virtual:
                      chests:
                        <tag>: <loc>
                        <tag>: <loc>
                 */
                io.set("virtual.chests.sample", "world,0,0,0,0,0");
                io.save(generalConfig);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        pageCost = io.getInt("page.cost");

        int count = 0;

        for (String tag : io.getConfigurationSection("virtual.chests").getKeys(false)) {
            String loc = io.getString("virtual.chests." + tag);
            VirtualLocations.addLocation(tag, loc);
            count++;
        }

        getLogger().info("Registered " + count + " virtual chest location(s).");

        if (sql.initialise()) {
            getLogger().info("Connected to MySQL server successfully.");

            /*
                Table Format:
                -------------
                <uuid>, <size> (3/6), <pages>, <data>
             */

            try {
                if (!sql.doesTableExist(table)) {
                    sql.standardQuery("CREATE TABLE " + table + " (`id` INTEGER PRIMARY KEY AUTO_INCREMENT, `uuid` VARCHAR(100) UNIQUE," +
                            "`size` INTEGER DEFAULT 3, `pages` INTEGER DEFAULT 1, `data` TEXT);");
                    getLogger().info("Created MySQL table `" + table + "` successfully.");
                }

                // load all player chests
                count = 0;
                ResultSet set = sql.sqlQuery("SELECT * FROM " + table + ";");
                while (set.next()) {
                    String uuid = set.getString("uuid"), data = set.getString("data");
                    int size = set.getInt("size"), pages = set.getInt("pages");
                    VirtualInventory vi = new VirtualInventory(uuid, size, pages, data);
                    ChestManager.addChest(uuid, vi);
                    count++;
                }

                getLogger().info("Pre-loaded virtual inventories for " + count + " player(s).");

                ChestManager.initSaverTask();

                getLogger().info("Initiated virtual inventories saving task.");

            } catch (SQLException e) {
                e.printStackTrace();
            }

        } else {
            getLogger().severe("Failed to connect to MySQL server.");
        }

        new ChestOpenListener(this);

        if (setupEconomy()) {
            getLogger().info("Hooked into Vault economy successfully.");
        } else {
            getLogger().severe("Failed to hook into Vault economy!");
        }

        wgp = getWorldGuard();

        if (wgp == null) {
            getLogger().severe("Failed to hook world guard!");
        }
    }

    public static boolean isInPvPRegion(Player p) {
        /*ApplicableRegionSet set = wgp.getRegionManager(p.getWorld()).getApplicableRegions(p.getLocation());
        for(ProtectedRegion pr : set.getRegions()){
            boolean pvp = Boolean.parseBoolean(pr.getFlag(DefaultFlag.PVP).toString());
            if(pvp){
                return true;
            }
        }*/
        return false;
        /*for(ProtectedRegion pr : set.getRegions()){
            if(pr.getFlag(DefaultFlag.PVP)){

            }
        }*/
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }

    private WorldGuardPlugin getWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }

        return (WorldGuardPlugin) plugin;
    }

    @Override
    public void onDisable() {
        ChestManager.onDisable();
        if (sql != null && sql.isActive()) {
            sql.closeConnection();
        }
    }

    private UUID getUUID(String in) {
        UUID uuid = null;

        if (in.length() > 16) {
            uuid = UUID.fromString(in);
        } else {
            //try obtain
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().equalsIgnoreCase(in)) {
                    uuid = online.getUniqueId();
                    break;
                }
            }

            if (uuid == null) {
                uuid = Bukkit.getOfflinePlayer(in).getUniqueId();
            }
        }

        return uuid;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (label.equalsIgnoreCase("virtualchests") || label.equalsIgnoreCase("vc")) {

            boolean isAdmin = false;

            if (!(sender instanceof Player)) {
                isAdmin = true;
            } else {
                isAdmin = ((Player) sender).isOp();
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("upgradeuser")) {

                if (!isAdmin) {
                    sender.sendMessage(ChatColor.RED + "You do not have permissions.");
                    return true;
                }

                String in = args[1];
                UUID uuid = getUUID(in);

                if (uuid == null) {
                    sender.sendMessage(ChatColor.RED + "Failed to obtain user UUID.");
                } else {
                    final UUID actual = uuid;
                    BukkitRunnable run = new BukkitRunnable() {
                        public void run() {
                            try {
                                sql.standardQuery("UPDATE " + table + " SET size=6 WHERE uuid='" + actual.toString() + "';");
                                ChestManager.getChest(actual.toString()).updateSize();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    run.runTaskAsynchronously(this);
                    sender.sendMessage(ChatColor.GREEN + "Chest size upgraded successfully.");
                }
                return true;
            } else if (args.length == 3 && args[0].equalsIgnoreCase("setpages")) {

                if (!isAdmin) {
                    sender.sendMessage(ChatColor.RED + "You do not have permissions.");
                    return true;
                }

                UUID uuid = getUUID(args[1]);

                if (uuid == null) {
                    sender.sendMessage(ChatColor.RED + "Failed to obtain user UUID.");
                } else {
                    final int pages;

                    try {
                        pages = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Number of pages must be an integer.");
                        return true;
                    }

                    final UUID finalUUID = uuid;
                    BukkitRunnable run = new BukkitRunnable() {
                        public void run() {
                            try {
                                sql.standardQuery("UPDATE " + table + " SET pages=" + pages + " WHERE uuid='" + finalUUID.toString() + "';");
                                ChestManager.getChest(finalUUID.toString()).setPages(pages);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    run.runTaskAsynchronously(this);

                    sender.sendMessage(ChatColor.GREEN + "User now has access to " + pages + " pages.");

                }

                return true;
            }

            if (sender instanceof Player) {
                final Player p = (Player) sender;

                if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
                    //adding chests
                    if (p.isOp()) {
                        Block target = p.getTargetBlock(null, 30);
                        if (target != null && target.getType().equals(Material.CHEST)) {
                            if (VirtualLocations.saveTag(args[1], new Loc(target.getLocation()).toString())) {
                                p.sendMessage(ChatColor.GREEN + "Virtual inventory location saved successfully.");
                            } else {
                                p.sendMessage(ChatColor.RED + "Failed to save. There is either a tag by this name, or a" +
                                        "\nvirtual chest is already configured for this location.");
                            }
                        } else {
                            p.sendMessage(ChatColor.RED + "You must be looking at a chest block.");
                        }
                    } else {
                        p.sendMessage(ChatColor.RED + "You have insufficient permissions.");
                    }
                    return true;
                } else if (args.length == 1 && args[0].equalsIgnoreCase("open")) {
                    if (p.hasPermission(openPermission)) {

                        if (isInPvPRegion(p)) {
                            p.sendMessage(ChatColor.RED + "Virtual chests cannot be accessed inside PvP areas.");
                            return true;
                        }

                        VirtualInventory vi = ChestManager.getChest(p.getUniqueId().toString());
                        vi.displayInventory(p, 1);
                    } else {
                        p.sendMessage(ChatColor.RED + "You have insufficient permissions.");
                    }
                    return true;
                } else if (args.length == 1 && args[0].equalsIgnoreCase("buypage")) {

                    VirtualInventory vi = ChestManager.getChest(p.getUniqueId().toString());
                    int curPages = vi.getPages();
                    int nextCost = pageCost * curPages;

                    if (economy.has(p, nextCost)) {
                        if (economy.withdrawPlayer(p, nextCost).transactionSuccess()) {
                            vi.addPages(1);
                            BukkitRunnable save = new BukkitRunnable() {
                                public void run() {
                                    try {
                                        sql.standardQuery("UPDATE " + table + " SET pages=pages+1 WHERE uuid='" + p.getUniqueId().toString() + "';");
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                }
                            };
                            save.runTaskAsynchronously(this);
                            p.sendMessage(ChatColor.GREEN + "Page purcahsed for $" + nextCost);
                        } else {
                            p.sendMessage(ChatColor.RED + "Unknown transaction error, purchase cancelled!");
                        }
                    } else {
                        p.sendMessage(ChatColor.RED + "You need $" + nextCost + " to buy another page.");
                    }

                    return true;
                }

                p.sendMessage(ChatColor.YELLOW + "----- Virtual Chest Commands -----");
                p.sendMessage(ChatColor.YELLOW + "/vc buypage " + ChatColor.GRAY + "buy an additional page");
                p.sendMessage(ChatColor.YELLOW + "/vc open " + ChatColor.GRAY + "opens your virtual chest");
                if (p.isOp()) {
                    p.sendMessage(ChatColor.LIGHT_PURPLE + "/vc add <tag> " + ChatColor.GRAY + "adds a virtual" +
                            " chest location.");
                    p.sendMessage(ChatColor.LIGHT_PURPLE + "/vc upgradeuser <user> " + ChatColor.GRAY + "upgrades a users chest size.");
                    p.sendMessage(ChatColor.LIGHT_PURPLE + "/v setpages <user> <pages>");
                }

            } else {
                sender.sendMessage(ChatColor.RED + "Commands must be issued in-game.");
            }

            return true;
        }

        return false;
    }

}
