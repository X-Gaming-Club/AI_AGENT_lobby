package org.dabhiru.ainpc;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
//import net.citizensnpcs.api.npc.NPCRegistry;
//import net.citizensnpcs.api.trait.Trait;
//import net.citizensnpcs.api.trait.TraitInfo;
//import net.citizensnpcs.trait;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class AI_NPC extends JavaPlugin implements Listener {
    NPCRegistry registry;
    NPC npc;
    private Set<String> executedCommands = new HashSet<>();
    private Map<UUID, NPC> playerNpcInteraction = new HashMap<>();
    private Map<UUID, Long> interactionTimestamps = new HashMap<>();
    private static final String PREDEFINED_PROMPT = "You are an XGaming AI NPC that answers questions about This Minecraft server and can provide stuff according to the response also. Your Nature is Clever " +
            "The server has the following features: There are 4 servers connected via portals in the main lobby. The upcoming features include archery, a roller coaster, and a theme-based carnival in this lobby. Now, let me tell you about each server connected through the portals: " +
            "The first server is a trading server where you can trade according to the inventory used AI to get Trades. It's a survival server. The second server is an assets generation server, a life-steal server where it generates assets like tools, armor, and other Minecraft items with the power of AI. It can provide assets within 1 second. " +
            "The third server is an anarchy server, similar to the second server but a survival server with AI entities that come to hunt you. This server will soon be replaced, and a new server will come with a special secret. " +
            "The fourth server is a story mode server where you will get AI quests based on player needs. Completing quests will give you amazing rewards. It can generate unlimited quests with the power of AI. " + "This NPC IS IN MAIN LOBBY SO RESPONSE ACCORDING TO THAT BUT DONT INCLUDE IN Response!!"+
            "We used Generative AI to create the best AI-based Minecraft servers and Plugins youtube channel @xgaming_club. If a player types in English, respond in English. If a player types in Hinglish (a mix of Hindi and English), respond accordingly. if player message is in hinglish response in hinglish, hinglish is a type of language used when we want to say anything in hindi but typed in english eg bhai mujhe khana do etc"+"You Can Also Provide Items Which Are Under Your System like Starter kits,opening menus,etc";

    @Override
    public void onEnable() {
        if (!getServer().getPluginManager().isPluginEnabled("Citizens")) {
            getLogger().severe("Citizens plugin not found! Disabling AI-NPC plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        registry = CitizensAPI.getNPCRegistry();
        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        Bukkit.getScheduler().runTaskTimer(this, this::checkPlayerDistances, 0L, 20L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("spawnnpc") && sender instanceof Player) {
            Player player = (Player) sender;
            Location location = player.getLocation();

            npc = registry.createNPC(org.bukkit.entity.EntityType.PLAYER, "XG_AI");
            npc.spawn(location);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc select "+npc.getId());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc look --perplayer true --range 10 --linkedbody true --targetnpcs false");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc lookclose --range 10 --perplayer true");
            playerNpcInteraction.put(player.getUniqueId(), npc);

            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) {
            NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
            Player player = event.getPlayer();

            if (npc.getName().equals("XG_AI")) {
                player.sendMessage("Welcome To XGaming Lobby Here I am ready to assist you in any language");
                double distance = player.getLocation().distance(npc.getEntity().getLocation());
                if (distance <= 5) {
                    playerNpcInteraction.put(player.getUniqueId(), npc);
                    interactionTimestamps.put(player.getUniqueId(), System.currentTimeMillis());
                   //player.sendMessage(ChatColor.YELLOW+"Oh No You Went Out Of Range");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
player.sendMessage(message);
        if (playerNpcInteraction.containsKey(player.getUniqueId())) {
            event.setCancelled(true); // Cancel the chat event to prevent public chat

            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    String aiPrompt = PREDEFINED_PROMPT + "\n\nPlayer: " + message + "\nProvide Only The Asked Appropriate Answer In very Few Words like in 10 words etc.";
                    String response = openai.getChatResponse(aiPrompt);

                    // Debug logging
                    getLogger().info("Player message: " + message);
                    getLogger().info("AI response: " + response);

                    Bukkit.getScheduler().runTask(this, () -> processPlayerMessage(player, message, response));
                    reloadConfig();
                } catch (Exception e) {
                    player.sendMessage("An error occurred while processing your request.");
                    e.printStackTrace();
                }
            });
        }
    }

    private void processPlayerMessage(Player player, String message, String response) {
        FileConfiguration config = getConfig();
        List<Map<?, ?>> responses = config.getMapList("responses");

        for (Map<?, ?> res : responses) {
            String keyword = (String) res.get("keyword");

            if (message.toLowerCase().contains(keyword.toLowerCase())) {
                List<String> consoleCommands = (List<String>) ((Map<?, ?>) res.get("command")).get("console");
                List<String> playerCommands = (List<String>) ((Map<?, ?>) res.get("command")).get("player");
                //List<String> Rr = (List<String>) ((Map<?, ?>) res.get("response"));
                boolean perPlayerOneTime = (boolean) res.get("perplayer1time");

                String commandKey = player.getUniqueId() + ":" + keyword;

                if (perPlayerOneTime && executedCommands.contains(commandKey)) {
                    player.sendMessage("You can get this item 1 time only");
                    response="You already Got This Item";
                    player.sendMessage(response);
                    return;
                }

                if (consoleCommands != null) {
                    for (String command : consoleCommands) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                    }
//                    if (Rr != null) {
//                        for (String r : Rr) {
//                            player.sendMessage(r);
//                        }
//                    }
                    return;
                }
                if (playerCommands != null) {
                    for (String command : playerCommands) {
                        player.performCommand(command);
                    }
//                    if (Rr != null) {
//                        for (String r : Rr) {
//                            player.sendMessage(r);
//                        }
//                    }
                    return;
                }

                if (perPlayerOneTime) {
                    executedCommands.add(commandKey);
                }

                player.sendMessage(ChatColor.RED + "AI NPC: " + ChatColor.YELLOW + response);
                return;
            }
        }

        player.sendMessage(ChatColor.GREEN + "AI NPC: " + ChatColor.YELLOW + response);
    }

    private void checkPlayerDistances() {
        for (UUID playerId : playerNpcInteraction.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            NPC npc = playerNpcInteraction.get(playerId);
            if (player != null && npc != null) {
                double distance = player.getLocation().distance(npc.getEntity().getLocation());
                if (distance > 5) {
                    playerNpcInteraction.remove(playerId);
                    interactionTimestamps.remove(playerId);
                    player.sendMessage(ChatColor.RED+"You are too far from the NPC. Interaction closed.");
                }
            }
        }
    }
}

// OpenAI Class

