package org.skydream.quizcraft;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument; // ★ 新增：物品自动补全支持
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class QuizCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        System.out.println("QuizCommands: 开始注册命令");

        // 主命令 - 帮助
        dispatcher.register(Commands.literal("qc")
                .executes(ctx -> {
                    showHelp(ctx.getSource());
                    return Command.SINGLE_SUCCESS;
                })
        );

        // 普通玩家命令
        dispatcher.register(Commands.literal("qc")
                .then(Commands.literal("question")
                        .requires(source -> source.hasPermission(0))
                        .executes(ctx -> askNewQuestion(ctx.getSource())))
        );

        // 重载命令
        dispatcher.register(Commands.literal("qc")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> reloadConfig(ctx.getSource())))
        );

        // 添加问题
        dispatcher.register(Commands.literal("qc")
                .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("question")
                                .then(Commands.argument("question", StringArgumentType.string())
                                        .then(Commands.argument("answer", StringArgumentType.string())
                                                .executes(ctx -> addQuestion(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "question"),
                                                        StringArgumentType.getString(ctx, "answer"))))))
                )
        );

        // ★★★ 添加奖励（物品自动补全）
        dispatcher.register(Commands.literal("qc")
                .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reward")
                                // ★ ItemArgument.item(context) 用于自动补全
                                .then(Commands.argument("item", ItemArgument.item(context))
                                        .then(Commands.argument("maxAmount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {

                                                    ItemStack stack = ItemArgument.getItem(ctx, "item").createItemStack(1, false);
                                                    Item item = stack.getItem();
                                                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);

                                                    return addReward(ctx.getSource(),
                                                            id.toString(),
                                                            IntegerArgumentType.getInteger(ctx, "maxAmount"));
                                                }))))
                )
        );

        // 列出问题
        dispatcher.register(Commands.literal("qc")
                .then(Commands.literal("list")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("question")
                                .executes(ctx -> listQuestions(ctx.getSource())))
                )
        );

        // 列出奖励
        dispatcher.register(Commands.literal("qc")
                .then(Commands.literal("list")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reward")
                                .executes(ctx -> listRewards(ctx.getSource())))
                )
        );

        // 排行榜
        dispatcher.register(Commands.literal("qc")
                .then(Commands.literal("list")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("ranking")
                                .executes(ctx -> listRanking(ctx.getSource())))
                )
        );

        // 重置排行榜
        dispatcher.register(Commands.literal("qc")
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("ranking")
                                .executes(ctx -> resetRanking(ctx.getSource())))
                )
        );

        // 移除问题
        dispatcher.register(Commands.literal("qc")
                .then(Commands.literal("remove")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("question")
                                .then(Commands.argument("question", StringArgumentType.string())
                                        .executes(ctx -> removeQuestion(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "question"))))
                        )
                )
        );

        // ★★★ 移除奖励（同样使用物品自动补全）
        dispatcher.register(Commands.literal("qc")
                .then(Commands.literal("remove")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reward")
                                .then(Commands.argument("item", ItemArgument.item(context))
                                        .executes(ctx -> {

                                            ItemStack stack = ItemArgument.getItem(ctx, "item").createItemStack(1, false);
                                            Item item = stack.getItem();
                                            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);

                                            return removeReward(ctx.getSource(), id.toString());
                                        }))
                        )
                )
        );

        System.out.println("QuizCommands: 所有命令注册完成");
    }

    private static void showHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== QuizCraft 命令帮助 ==="), false);
        source.sendSuccess(() -> Component.literal("§a/qc question §7- 发起新问题"), false);
        source.sendSuccess(() -> Component.literal("§e/qc add reward <物品> <数量> §7- 添加奖励"), false);
        source.sendSuccess(() -> Component.literal("§e/qc remove reward <物品> §7- 移除奖励"), false);
    }

    private static int reloadConfig(CommandSourceStack source) {
        QuizManager manager = Quizcraft.getQuizManager();
        if (manager != null) {
            manager.reload();
            source.sendSuccess(() -> Component.literal("§a已重载配置"), true);
            return Command.SINGLE_SUCCESS;
        }
        source.sendSuccess(() -> Component.literal("§cQuizManager 未初始化"), true);
        return 0;
    }

    private static int addQuestion(CommandSourceStack source, String question, String answer) {
        QuizManager manager = Quizcraft.getQuizManager();
        if (manager != null) {
            manager.addQuestion(new Question(question, answer));
            source.sendSuccess(() -> Component.literal("§a添加问题: " + question), true);
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }

    private static int addReward(CommandSourceStack source, String itemId, int maxAmount) {
        QuizManager manager = Quizcraft.getQuizManager();
        if (manager != null) {
            manager.addReward(new Reward(itemId, maxAmount));
            source.sendSuccess(() -> Component.literal("§a添加奖励: " + itemId + " *" + maxAmount), true);
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }

    private static int askNewQuestion(CommandSourceStack source) {
        QuizManager manager = Quizcraft.getQuizManager();
        if (manager != null) {
            manager.askRandomQuestion();
            source.sendSuccess(() -> Component.literal("§a已发布新问题"), true);
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }

    private static int listQuestions(CommandSourceStack source) {
        QuizManager manager = Quizcraft.getQuizManager();
        if (manager != null) {
            source.sendSuccess(() -> Component.literal("§6=== 问题列表 ==="), false);
            int i = 1;
            for (Question q : manager.getQuestions()) {
                int idx = i++;
                source.sendSuccess(() ->
                        Component.literal("§e" + idx + ". §f" + q.getQuestion() + " §7-> §a" + q.getAnswer()), false);
            }
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }

    private static int listRewards(CommandSourceStack source) {
        QuizManager manager = Quizcraft.getQuizManager();
        if (manager != null) {
            source.sendSuccess(() -> Component.literal("§6=== 奖励列表 ==="), false);
            int i = 1;
            for (Reward r : manager.getRewards()) {
                int idx = i++;
                source.sendSuccess(() ->
                        Component.literal("§e" + idx + ". §f" + r.getItemId() + " §7(最多: §a" + r.getMaxAmount() + "§7)"), false);
            }
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }

    private static int listRanking(CommandSourceStack source) {
        QuizManager manager = Quizcraft.getQuizManager();
        if (manager != null) {
            // 使用正确的方法调用
            Map<String, Integer> rankings = manager.getScoreboard().getRankings();
            source.sendSuccess(() -> Component.literal("§6=== 排行榜 ==="), false);

            if (rankings.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7暂无数据"), false);
            } else {
                int rank = 1;
                for (Map.Entry<String, Integer> entry : rankings.entrySet()) {
                    String color = rank == 1 ? "§6" : rank == 2 ? "§7" : rank == 3 ? "§c" : "§f";
                    int finalRank = rank;
                    source.sendSuccess(() -> Component.literal(color + finalRank + ". §f" + entry.getKey() + " §7- §a" + entry.getValue() + " 分"), false);
                    rank++;
                    if (rank > 10) break;
                }
            }
            return Command.SINGLE_SUCCESS;
        }
        source.sendSuccess(() -> Component.literal("§cQuizManager 未初始化"), true);
        return 0;
    }

    private static int resetRanking(CommandSourceStack source) {
        QuizManager manager = Quizcraft.getQuizManager();
        if (manager != null) {
            manager.getScoreboard().resetScoreboard();
            manager.updateScoreboardForAllPlayers();
            source.sendSuccess(() -> Component.literal("§a排行榜已重置"), true);
            return Command.SINGLE_SUCCESS;
        }
        source.sendSuccess(() -> Component.literal("§cQuizManager 未初始化"), true);
        return 0;
    }

    private static int removeQuestion(CommandSourceStack source, String text) {
        QuizManager manager = Quizcraft.getQuizManager();
        if (manager != null) {
            manager.removeQuestion(text);
            source.sendSuccess(() -> Component.literal("§a已移除问题: " + text), true);
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }

    private static int removeReward(CommandSourceStack source, String itemId) {
        QuizManager manager = Quizcraft.getQuizManager();
        if (manager != null) {
            manager.removeReward(itemId);
            source.sendSuccess(() -> Component.literal("§a已移除奖励: " + itemId), true);
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }
}
