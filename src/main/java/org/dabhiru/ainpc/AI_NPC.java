package org.dabhiru.ainpc;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.TrackMessage;
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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class AI_NPC extends JavaPlugin implements Listener {
    NPCRegistry registry;
    NPC npc;
    private Set<String> executedCommands = new HashSet<>();
    private Map<UUID, NPC> playerNpcInteraction = new HashMap<>();
    private Map<UUID, Long> interactionTimestamps = new HashMap<>();
    private FileConfiguration promptsConfig;
    public static boolean test;
    private static String segmentkey;
    private Set<NPC> pluginSpawnedNPCs = new HashSet<>();

    private static final String FILE_NAME = "spawned_npcs.dat";
    //    private static final String PREDEFINED_PROMPT = "Hello! I'm your friendly XGaming AI NPC, here to help you with any questions about our Minecraft server and to provide items you need. Iam Expert in Maintaing Players Data I'm smart and always ready to assist. " +
//            "Our server has some cool features: there are 4 servers connected through portals in the main lobby. We're also planning to add archery, a roller coaster, and a theme-based carnival. Here's a quick look at each server: " +
//            "1. The trading server is a survival server where you can trade items using AI-driven trades. " +
//            "2. The assets generation server is a life-steal server that quickly provides tools, armor, and other items using AI. " +
//            "3. The anarchy server is another survival server with AI entities hunting players, but it will soon be replaced by a special secret server. " +
//            "Note: I can only give out the starter kit in the lobby if you ask for it. I won't suggest it on my own. " +
//            "4. The story mode server offers AI-generated quests based on what you need, and you can earn amazing rewards by completing them. " +
//            "I'm based in the main lobby, so keep that in mind, but I won't mention it in my responses. " +
//            "We use Generative AI to bring you the best AI-based Minecraft servers and plugins. Don't forget to check out our YouTube channel @xgaming_club! " +
//            "I'll reply in the language you use: English or Hinglish (a mix of Hindi and English). If you type in Hinglish, I'll respond in Hinglish too. " +
//            "Feel free to ask for items like starter kits or to open menus. I'm here to make your experience awesome! Server Details play.xgaming.club port for pe/bedrock 19132  server mein jitne bhi plugins hai woh sab xgaming.club website pe available hai  also on spigotmc ,xgaming is the community that provides creators a ability to create games using the AI technology provided by xgaming u can checkout xgaming work in xgaming.club";



    private Set<String> loadNPCNamesFromConfig() {
        File file = new File(getDataFolder(), "npc.yml");
        if (!file.exists()) {
            return new HashSet<>(); // Return empty set if file does not exist
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        return new HashSet<>(config.getStringList("npcs"));
    }


    @Override
    public void onEnable() {
        if (!getServer().getPluginManager().isPluginEnabled("Citizens")) {
            getLogger().severe("Citizens plugin not found! Disabling AI-NPC plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registry = CitizensAPI.getNPCRegistry();
        getServer().getPluginManager().registerEvents(this, this);
        segmentkey= getConfig().getString("segmentkey",null);
        saveDefaultConfig();
        //loadSpawnedNPCs(registry);
        //pluginSpawnedNPCs.addAll(loadSpawnedNPCs());


getLogger().warning("key: "+segmentkey);
        test = getConfig().getBoolean("test", true);
        loadPromptsConfig();
        Bukkit.getScheduler().runTaskTimer(this, this::checkPlayerDistances, 0L, 20L);
    }

    private void loadPromptsConfig() {
        File promptsFile = new File(getDataFolder(), "prompts.yml");
        if (!promptsFile.exists()) {
            promptsFile.getParentFile().mkdirs();
            saveResource("prompts.yml", false);
        }
        promptsConfig = YamlConfiguration.loadConfiguration(promptsFile);
    }
    public static String getSegmentkey(){
        return segmentkey;
    }
    public static boolean isTest(){
        return test;
    }
    @Override
    public void onDisable() {
        // Save spawned NPCs to file
        //saveSpawnedNPCs();

        // Other cleanup code
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("spawnnpc") && sender instanceof Player) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Please specify the NPC name.");
                return false;
            }
            Player player = (Player) sender;
            Location location = player.getLocation();

            String npcName = args[0];
            npc = registry.createNPC(org.bukkit.entity.EntityType.PLAYER, npcName);
            npc.spawn(location);
            //pluginSpawnedNPCs.add(npc);



  //          saveSpawnedNPCs();
            saveNPCNameToConfig(npcName); // Save NPC name to npc.yml
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc select " + npc.getId());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc look --perplayer true --range 10 --linkedbody true --targetnpcs false");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc look");
            //playerNpcInteraction.put(player.getUniqueId(), npc);

            return true;
        }
        return false;
    }

    private void saveNPCNameToConfig(String npcName) {
        File file = new File(getDataFolder(), "npc.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> npcNames = config.getStringList("npcs");
        if (!npcNames.contains(npcName)) {
            npcNames.add(npcName);
        }
        config.set("npcs", npcNames);
        try {
            config.save(file);
        } catch (IOException e) {
            getLogger().severe("Error occurred while saving npc.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) {
            NPC nNpc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
            Set<String> savedNPCNames = loadNPCNamesFromConfig(); // Load NPC names from config

            if (savedNPCNames.contains(nNpc.getName())) {
                // Your logic for interacting with NPCs spawned by your plugin
                getLogger().info("Right Clicked " + nNpc.getName());
                Player player = event.getPlayer();
                double distance = player.getLocation().distance(nNpc.getEntity().getLocation());
                if (distance <= 5 && !playerNpcInteraction.containsKey(player.getUniqueId())) {

                        player.sendMessage("Welcome to XGaming Lobby! Here I am, ready to assist you in any language.");
                        UUID uuid = player.getUniqueId();
                        String name = player.getName();

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("username", name);

                        String eventName = "AI NPC Interact";

                        sendEventToSegment(uuid.toString(), eventName, jsonObject);
                        playerNpcInteraction.put(player.getUniqueId(), nNpc);
                        interactionTimestamps.put(player.getUniqueId(), System.currentTimeMillis());

                }
            } else {
                getLogger().info("Interaction with unrecognized NPC: " + nNpc.getName());
            }
        }
    }


    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        Set<String> savedNPCNames = loadNPCNamesFromConfig();
        if (playerNpcInteraction.containsKey(player.getUniqueId())) {

            event.setCancelled(true); // Cancel the chat event to prevent public chat
player.sendMessage(player.getName()+"-> "+message);
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    NPC interactingNpc = playerNpcInteraction.get(player.getUniqueId());
                    String npcName = interactingNpc.getName();
                    if (savedNPCNames.contains(npcName)){
                        String aiPrompt = getPromptForNPC(npcName) + "\n\nPlayer: " + message + "\nProvide Only The Asked Appropriate Answer In very Few Words like in 10 words etc.";
                        String response = openai.getChatResponse(aiPrompt);

                        // Debug logging
                        getLogger().info("Player message: " + message);
                        getLogger().info("AI response: " + response);

                        Bukkit.getScheduler().runTask(this, () -> processPlayerMessage(player, message, response, npcName));
                    }
                } catch (Exception e) {
                    player.sendMessage("An error occurred while processing your request.");
                    e.printStackTrace();
                }
            });
        }
    }

    private String getPromptForNPC(String npcName) {
        // Extract the relevant word from the NPC name
        String[] parts = npcName.split("_");
        String relevantWord = parts.length > 1 ? parts[1] : "";

        // Check if a prompt exists for the relevant word, otherwise default to lobby
        return promptsConfig.getString("prompts." + relevantWord.toLowerCase(), promptsConfig.getString("prompts.lobby"));
    }

    private void processPlayerMessage(Player player, String message, String response,String npcname) {
        FileConfiguration config = getConfig();
        List<Map<?, ?>> responses = config.getMapList("responses");
        boolean commandExecuted = false;
        for (Map<?, ?> res : responses) {
            String keyword = (String) res.get("keyword");

            if (message.toLowerCase().contains(keyword.toLowerCase())) {
                List<String> consoleCommands = (List<String>) ((Map<?, ?>) res.get("command")).get("console");
                List<String> playerCommands = (List<String>) ((Map<?, ?>) res.get("command")).get("player");
                boolean perPlayerOneTime = (boolean) res.get("perplayer1time");

                String commandKey = player.getUniqueId() + ":" + keyword;

                if (perPlayerOneTime && executedCommands.contains(commandKey)) {
                    //player.sendMessage("You can get this item 1 time only");
                    response = "You already got this item";
                    player.sendMessage(ChatColor.RED+response);
                    return;
                }

                if (consoleCommands != null) {
                    for (String command : consoleCommands) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                    }
                    commandExecuted = true;
                }
                if (playerCommands != null) {
                    for (String command : playerCommands) {
                        player.performCommand(command);
                    }
                    commandExecuted = true;
                }

                if (perPlayerOneTime && commandExecuted) {
                    executedCommands.add(commandKey);
                }

                player.sendMessage(ChatColor.RED + npcname + ": " + ChatColor.YELLOW + response);
                UUID uuid = player.getUniqueId();
                String name = player.getName();

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("username", name);

                String eventName = "AI NPC Chat";

                sendEventToSegment(uuid.toString(), eventName, jsonObject);
                return;
            }
        }

        player.sendMessage(ChatColor.GREEN + npcname + ": " + ChatColor.YELLOW + response);
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", name);

        String eventName = "AI NPC Chat";

        sendEventToSegment(uuid.toString(), eventName, jsonObject);
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
                    player.sendMessage(ChatColor.RED + "You are too far from the NPC. Interaction closed.");
                }
            }
        }
    }
    public void sendEventToSegment(String distinctId, String eventName, JSONObject properties) {
        if (isTest()) {
            getLogger().info(eventName + " " + properties.toString());
            return;
        }

        String segmentKey = getSegmentkey();
        if (segmentKey == null || segmentKey.isEmpty()) {
            getLogger().info(eventName + " " + properties.toString());
            return;
        }

        getLogger().info("Sending event to Segment: " + eventName);
        Bukkit.getScheduler().runTask(this, () -> {
            Analytics analytics = Analytics.builder(segmentKey).build();

            analytics.enqueue(TrackMessage.builder(eventName)
                    .userId(distinctId)
                    .properties(properties.toMap()));

            // Close the client to make sure all events are sent before app exits
            analytics.shutdown();
        });
    }

}


// OpenAI Class

