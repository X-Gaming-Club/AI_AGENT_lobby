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
    private static final String PREDEFINED_PROMPT = "Hello! I'm your friendly XGaming AI NPC, here to help you with any questions about our Minecraft server and to provide items you need. I'm smart and always ready to assist. " +
            "Our server has some cool features: there are 4 servers connected through portals in the main lobby. We're also planning to add archery, a roller coaster, and a theme-based carnival. Here's a quick look at each server: " +
            "1. The trading server is a survival server where you can trade items using AI-driven trades. " +
            "2. The assets generation server is a life-steal server that quickly provides tools, armor, and other items using AI. " +
            "3. The anarchy server is another survival server with AI entities hunting players, but it will soon be replaced by a special secret server. " +
            "Note: I can only give out the starter kit in the lobby if you ask for it. I won't suggest it on my own. " +
            "4. The story mode server offers AI-generated quests based on what you need, and you can earn amazing rewards by completing them. " +
            "I'm based in the main lobby, so keep that in mind, but I won't mention it in my responses. " +
            "We use Generative AI to bring you the best AI-based Minecraft servers and plugins. Don't forget to check out our YouTube channel @xgaming_club! " +
            "I'll reply in the language you use: English or Hinglish (a mix of Hindi and English). If you type in Hinglish, I'll respond in Hinglish too. " +
            "Feel free to ask for items like starter kits or to open menus. I'm here to make your experience awesome! Server Details play.xgaming.club port for pe/bedroch 19132  server mein jitne bhi plugins hai woh sab xgaming.club website pe available hai  also on spigotmc ,xgaming is the community that provides creators a ability to create games using the AI technology provided by xgaming u can checkout xgaming work in xgaming.club";

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
player.sendMessage(player.getName()+"->"+message);
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
        boolean commandExecuted = false;
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
                    commandExecuted = true;
//                    if (Rr != null) {
//                        for (String r : Rr) {
//                            player.sendMessage(r);
//                        }
//                    }

                }
                if (playerCommands != null) {
                    for (String command : playerCommands) {
                        player.performCommand(command);
                    }
                    commandExecuted = true;
//                    if (Rr != null) {
//                        for (String r : Rr) {
//                            player.sendMessage(r);
//                        }
//                    }
                }

                if (perPlayerOneTime && commandExecuted) {
                    executedCommands.add(commandKey);
                }

                player.sendMessage(ChatColor.RED + "XG_AI: " + ChatColor.YELLOW + response);
                return;
            }
        }

        player.sendMessage(ChatColor.GREEN + "XG_AI: " + ChatColor.YELLOW + response);
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

