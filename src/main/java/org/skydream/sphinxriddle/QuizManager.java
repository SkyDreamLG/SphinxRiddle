package org.skydream.sphinxriddle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class QuizManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuizManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final MinecraftServer server;
    private final File questionsFile;
    private final File rewardsFile;

    private List<Question> questions = new ArrayList<>();
    private List<Reward> rewards = new ArrayList<>();
    private ScheduledExecutorService scheduler;
    private final QuizScoreboard scoreboard;

    private ActiveQuestion currentQuestion;
    private int tickCounter = 0;
    private final Set<UUID> answeredPlayers = ConcurrentHashMap.newKeySet();

    public QuizManager(MinecraftServer server) {
        this.server = server;
        File configDir = new File("config/sphinxriddle");
        this.questionsFile = new File(configDir, "questions.json");
        this.rewardsFile = new File(configDir, "rewards.json");
        this.scoreboard = new QuizScoreboard(configDir);

        if (!configDir.exists()) {
            configDir.mkdirs();
        }
    }

    public void loadConfig() {
        loadQuestions();
        loadRewards();
        scoreboard.loadScoreboard();
        LOGGER.info("Loaded {} questions and {} rewards", questions.size(), rewards.size());
    }

    private void loadQuestions() {
        if (!questionsFile.exists()) {
            createDefaultQuestions();
            return;
        }

        try (FileReader reader = new FileReader(questionsFile)) {
            Question[] questionArray = GSON.fromJson(reader, Question[].class);
            questions = new ArrayList<>(Arrays.asList(questionArray));
        } catch (IOException e) {
            LOGGER.error("Failed to load questions", e);
            createDefaultQuestions();
        }
    }

    private void loadRewards() {
        if (!rewardsFile.exists()) {
            createDefaultRewards();
            return;
        }

        try (FileReader reader = new FileReader(rewardsFile)) {
            Reward[] rewardArray = GSON.fromJson(reader, Reward[].class);
            rewards = new ArrayList<>(Arrays.asList(rewardArray));
        } catch (IOException e) {
            LOGGER.error("Failed to load rewards", e);
            createDefaultRewards();
        }
    }

    private void createDefaultQuestions() {
        questions = new ArrayList<>();
        questions.add(new Question("Minecraft中哪种生物会爆炸？", "苦力怕"));
        questions.add(new Question("用来合成火把的两种材料是什么？", "煤炭和木棍"));
        saveQuestions();
    }

    private void createDefaultRewards() {
        rewards = new ArrayList<>();
        rewards.add(new Reward("minecraft:diamond", 3));
        rewards.add(new Reward("minecraft:emerald", 5));
        rewards.add(new Reward("minecraft:iron_ingot", 10));
        saveRewards();
    }

    public void saveQuestions() {
        try (FileWriter writer = new FileWriter(questionsFile)) {
            GSON.toJson(questions, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save questions", e);
        }
    }

    public void saveRewards() {
        try (FileWriter writer = new FileWriter(rewardsFile)) {
            GSON.toJson(rewards, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save rewards", e);
        }
    }

    public void startAutoQuestionTimer() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }

        if (Config.AUTO_QUESTION_ENABLED.get()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            int interval = Config.QUESTION_INTERVAL.get();
            scheduler.scheduleAtFixedRate(this::askRandomQuestion, interval, interval, TimeUnit.SECONDS);
            LOGGER.info("Auto question timer started with {} second interval", interval);
        }
    }

    public void stopAutoQuestionTimer() {
        if (scheduler != null) {
            scheduler.shutdown();
            LOGGER.info("Auto question timer stopped");
        }
    }

    public void askRandomQuestion() {
        if (questions.isEmpty()) {
            LOGGER.warn("No questions available");
            return;
        }

        Question question = questions.get(new Random().nextInt(questions.size()));
        askQuestion(question);
    }

    public void askQuestion(Question question) {
        if (currentQuestion != null) {
            currentQuestion = null;
        }

        currentQuestion = new ActiveQuestion(question, System.currentTimeMillis());
        answeredPlayers.clear();

        String message = Config.NEW_QUESTION_MESSAGE.get()
                .replace("%question%", question.getQuestion());

        broadcastMessage(message);
        LOGGER.info("New question: {}", question.getQuestion());
    }

    public void handlePlayerAnswer(ServerPlayer player, String message) {
        if (currentQuestion == null || answeredPlayers.contains(player.getUUID())) {
            return;
        }

        String answer = currentQuestion.getQuestion().getAnswer().toLowerCase();
        if (message.toLowerCase().contains(answer)) {
            answeredPlayers.add(player.getUUID());
            rewardPlayer(player);
            currentQuestion = null;
        }
    }

    private void rewardPlayer(ServerPlayer player) {
        if (rewards.isEmpty()) {
            LOGGER.warn("No rewards available");
            return;
        }

        Reward reward = rewards.get(new Random().nextInt(rewards.size()));

        Item item = BuiltInRegistries.ITEM.get(reward.getItemResourceLocation());

        int amount = new Random().nextInt(reward.getMaxAmount()) + 1;
        ItemStack itemStack = new ItemStack(item, amount);

        if (player.getInventory().add(itemStack)) {
            String itemId = reward.getItemId();
            String[] parts = itemId.split(":");
            String itemName = parts.length > 1 ? parts[1] : itemId;

            itemName = itemName.replace("_", " ");
            String[] words = itemName.split(" ");
            StringBuilder displayName = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    displayName.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1)).append(" ");
                }
            }

            String finalName = displayName.toString().trim();

            scoreboard.addScore(player.getScoreboardName(), 1);
            updateScoreboardForAllPlayers();

            String rewardMessage = Config.REWARD_MESSAGE.get()
                    .replace("%player%", player.getScoreboardName())
                    .replace("%reward%", amount + "x " + finalName);

            broadcastMessage(rewardMessage);
            LOGGER.info("Player {} received reward: {}x {} and 1 point", player.getScoreboardName(), amount, finalName);
        }
    }

    public void tick() {
        if (currentQuestion != null) {
            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;
                long currentTime = System.currentTimeMillis();
                long elapsed = (currentTime - currentQuestion.getStartTime()) / 1000;

                if (elapsed >= Config.QUESTION_TIMEOUT.get()) {
                    currentQuestion = null;
                    answeredPlayers.clear();
                    LOGGER.info("Question expired due to timeout");
                }
            }
        }
    }

    private void broadcastMessage(String message) {
        String cleanMessage = message.replaceAll("&[0-9a-f]", "");
        Component component = Component.literal(message.replace("&", "§"));

        server.getPlayerList().getPlayers().forEach(player ->
                player.sendSystemMessage(component));

        LOGGER.info(cleanMessage);
    }

    // 计分板相关方法
    public void initializeScoreboard() {
        scoreboard.initializeScoreboard(server);
        updateScoreboardForAllPlayers();
    }

    public void updateScoreboardForAllPlayers() {
        scoreboard.updateScoreboardDisplay(server);
    }

    // Getters and setters
    public List<Question> getQuestions() { return questions; }
    public List<Reward> getRewards() { return rewards; }

    public QuizScoreboard getScoreboard() { return scoreboard; } // 添加这个方法

    public void addQuestion(Question question) {
        questions.add(question);
        saveQuestions();
    }

    public void addReward(Reward reward) {
        rewards.add(reward);
        saveRewards();
    }

    public void removeQuestion(String questionText) {
        questions.removeIf(question -> question.getQuestion().equals(questionText));
        saveQuestions();
    }

    public void removeReward(String itemId) {
        rewards.removeIf(reward -> reward.getItemId().equals(itemId));
        saveRewards();
    }

    public void reload() {
        loadConfig();
        stopAutoQuestionTimer();
        startAutoQuestionTimer();
        currentQuestion = null;
        answeredPlayers.clear();
        updateScoreboardForAllPlayers();
    }
}