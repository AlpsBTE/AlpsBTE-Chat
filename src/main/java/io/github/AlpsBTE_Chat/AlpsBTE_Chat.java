package github.AlpsBTE_Chat;

import me.clip.placeholderapi.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class AlpsBTE_Chat extends JavaPlugin implements Listener, PluginMessageListener {

    private FileConfiguration config;
    private File configFile;

    private static AlpsBTE_Chat plugin;

    @Override
    public void onEnable() {
        plugin = this;

        reloadConfig();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerIncomingPluginChannel(this, "AlpsBTE-Chat", this);

        getCommand("chatReload").setExecutor(new CMD_Reload());

        getLogger().log(Level.INFO, "Successfully enabled AlpsBTE-Chat plugin!");
    }

    public static AlpsBTE_Chat getPlugin() {
        return plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChatEvent(AsyncPlayerChatEvent event) {
        event.setFormat(getFormattedMessage(event.getPlayer(), event.getMessage()));

        broadcastPlayerMessage(event.getPlayer(), event.getMessage());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Bukkit.broadcastMessage("§7[§6+§7] > " + event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLeaveEvent(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Bukkit.broadcastMessage("§7[§c-§7] > " + event.getPlayer().getName());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("AlpsBTE-Chat")) {
            return;
        }

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
        try {
            if(in.available() >= 1) {
                Bukkit.broadcastMessage(in.readUTF());
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not send global message to players!", ex);
        }
    }

    public void broadcastPlayerMessage(Player player, String message) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                Socket socket = new Socket(config.getString("server.IP"), config.getInt("server.port"));
                OutputStream output = socket.getOutputStream();
                ObjectOutputStream objectOutput = new ObjectOutputStream(output);

                // Send player message
                objectOutput.writeObject(getFormattedMessage(player, message));
                objectOutput.writeObject(getConfig().getString("name"));
                objectOutput.flush();

                objectOutput.close();
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Could not broadcast player message to Bungeecord!", ex);
            }
        });
    }

    public String getFormattedMessage(Player player, String message) {
        return PlaceholderAPI.setPlaceholders(player, "§7[§a" + player.getWorld().getName() + "§7] [%luckperms_prefix%§7] %player_name% &7&l> &7" + message);
    }

    @Override
    public void reloadConfig() {
        configFile = new File(getDataFolder(), "config.yml");
        if (configFile.exists()) {
            config = YamlConfiguration.loadConfiguration(configFile);
        } else {
            // Look for default configuration file
            Reader defConfigStream = new InputStreamReader(this.getResource("defaultConfig.yml"), StandardCharsets.UTF_8);

            config = YamlConfiguration.loadConfiguration(defConfigStream);
        }

        saveConfig();
    }

    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    @Override
    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }

        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }
}
