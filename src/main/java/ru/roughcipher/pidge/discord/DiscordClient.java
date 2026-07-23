package ru.roughcipher.pidge.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.core.lang.I18n;
import net.minecraft.core.net.ChatEmotes;
import net.minecraft.core.util.helper.UUIDHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.player.PlayerServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.telegram.TelegramChatRelay;
import ru.roughcipher.pidge.util.BaseChatRelay;

public class DiscordClient {
	private static volatile JDA jda;
	private static volatile StandardGuildMessageChannel channel;
	private static final Object lock = new Object();

	public static boolean init() {
		if (!PidgeConfig.isDiscordEnabled()) return false;
		try {
			JDABuilder builder = JDABuilder.create(
				PidgeConfig.getDiscordToken(),
				GatewayIntent.GUILD_MESSAGES,
				GatewayIntent.MESSAGE_CONTENT
			);
			builder.disableCache(
				net.dv8tion.jda.api.utils.cache.CacheFlag.ACTIVITY,
				net.dv8tion.jda.api.utils.cache.CacheFlag.VOICE_STATE,
				net.dv8tion.jda.api.utils.cache.CacheFlag.EMOJI,
				net.dv8tion.jda.api.utils.cache.CacheFlag.STICKER,
				net.dv8tion.jda.api.utils.cache.CacheFlag.SOUNDBOARD_SOUNDS,
				net.dv8tion.jda.api.utils.cache.CacheFlag.CLIENT_STATUS,
				net.dv8tion.jda.api.utils.cache.CacheFlag.ONLINE_STATUS,
				net.dv8tion.jda.api.utils.cache.CacheFlag.SCHEDULED_EVENTS
			);
			builder.addEventListeners(new Listener());
			jda = builder.build().awaitReady();
			Pidge.LOGGER.info("Discord client started");

			StandardGuildMessageChannel ch = getChannel();
			if (ch != null) {
				Guild guild = ch.getGuild();
				guild.retrieveCommands().queue(commands -> {
					for (net.dv8tion.jda.api.interactions.commands.Command command : commands) {
						command.delete().queue();
					}
					guild.updateCommands().addCommands(
						Commands.slash("list", "Show online players"),
						Commands.slash("whitelist", "Manage server whitelist")
							.addSubcommands(
								new SubcommandData("add", "Add a player to whitelist")
									.addOption(OptionType.STRING, "player", "Player name", true),
								new SubcommandData("reload", "Reload whitelist"),
								new SubcommandData("remove", "Remove a player from whitelist")
									.addOption(OptionType.STRING, "player", "Player name", true)
							)
					).queue(
						success -> Pidge.LOGGER.info("Registered commands on guild {}", guild.getName()),
						failure -> Pidge.LOGGER.error("Failed to register commands on guild", failure)
					);
				});
			} else {
				Pidge.LOGGER.warn("Discord channel not found, cannot register commands");
			}

			return true;
		} catch (Throwable t) {
			Pidge.LOGGER.error("Discord init failed", t);
			return false;
		}
	}

	public static void shutdown() {
		if (jda != null) {
			try { jda.shutdown(); } catch (Exception e) { Pidge.LOGGER.error("JDA shutdown error", e); }
		}
	}

	@Nullable
	public static StandardGuildMessageChannel getChannel() {
		if (jda == null) return null;
		if (channel == null) {
			synchronized (lock) {
				if (channel == null) {
					channel = jda.getChannelById(StandardGuildMessageChannel.class, PidgeConfig.getDiscordChannel());
				}
			}
		}
		return channel;
	}

	public static class Listener implements EventListener {
		@Override
		public void onEvent(@NotNull GenericEvent event) {
			if (event instanceof SlashCommandInteractionEvent slash) {
				if (!slash.getChannel().getId().equals(PidgeConfig.getDiscordChannel())) return;

				switch (slash.getName()) {
					case "list": {
						String author = slash.getUser().getName() + " (" + slash.getUser().getId() + ")";
						Pidge.LOGGER.info("Discord /list command by {}", author);
						slash.reply(BaseChatRelay.getPlayerListString()).queue();
						return;
					}
					case "whitelist": {
						String sub = slash.getSubcommandName();
						if (sub == null) return;
						String authorId = slash.getUser().getId();
						if (!PidgeConfig.getDiscordAdminIds().contains(authorId)) {
							Pidge.LOGGER.warn("Discord unauthorized /whitelist {} by {} ({})", sub, slash.getUser().getName(), authorId);
							slash.reply("You are not authorized to use this command.").setEphemeral(true).queue();
							return;
						}

						MinecraftServer server = MinecraftServer.getInstance();
						if (server == null || server.playerList == null) {
							Pidge.LOGGER.error("Discord /whitelist {} failed: server not ready", sub);
							slash.reply("Server not ready.").setEphemeral(true).queue();
							return;
						}

						switch (sub) {
							case "add": {
								var option = slash.getOption("player");
								if (option == null) {
									slash.reply("Please specify a player name.").setEphemeral(true).queue();
									return;
								}
								String playerName = option.getAsString();
								if (playerName.length() > 16) {
									slash.reply("Player name must be 16 characters or less.").setEphemeral(true).queue();
									return;
								}
								String authorName = slash.getUser().getName() + " (" + authorId + ")";
								Pidge.LOGGER.info("Discord /whitelist add {} requested by {}", playerName, authorName);

								PlayerServer player = server.playerList.getPlayerEntity(playerName);
								if (player != null) {
									server.playerList.addToWhiteList(player.uuid);
									String successMsg = I18n.getInstance().translateKeyAndFormat("command.commands.whitelist.add.success", playerName);
									Pidge.LOGGER.info("Discord /whitelist add {} succeeded (online)", playerName);
									slash.reply(successMsg).queue();
								} else {
									try {
										UUIDHelper.runConversionAction(playerName,
											(uuid) -> {
												server.playerList.addToWhiteList(uuid);
												String successMsg2 = I18n.getInstance().translateKeyAndFormat("command.commands.whitelist.add.success", playerName);
												Pidge.LOGGER.info("Discord /whitelist add {} succeeded (offline)", playerName);
												slash.reply(successMsg2).queue();
											},
											(username) -> {
												String failMsg = I18n.getInstance().translateKeyAndFormat("command.commands.whitelist.add.fail.wrong_name", username);
												Pidge.LOGGER.warn("Discord /whitelist add {} failed: wrong name", playerName);
												slash.reply(failMsg).setEphemeral(true).queue();
											}
										);
									} catch (Exception e) {
										Pidge.LOGGER.error("Discord /whitelist add {} failed with exception", playerName, e);
										slash.reply("Failed to add player: " + e.getMessage()).setEphemeral(true).queue();
									}
								}
								break;
							}
							case "reload": {
								Pidge.LOGGER.info("Discord /whitelist reload requested by {} ({})", slash.getUser().getName(), authorId);
								try {
									server.playerList.reloadWhiteList();
									String msg = I18n.getInstance().translateKey("command.commands.whitelist.reload");
									slash.reply(msg).queue();
									Pidge.LOGGER.info("Discord /whitelist reload succeeded");
								} catch (Exception e) {
									Pidge.LOGGER.error("Discord /whitelist reload failed", e);
									slash.reply("Failed to reload whitelist: " + e.getMessage()).setEphemeral(true).queue();
								}
								break;
							}
							case "remove": {
								var option = slash.getOption("player");
								if (option == null) {
									slash.reply("Please specify a player name.").setEphemeral(true).queue();
									return;
								}
								String playerName = option.getAsString();
								if (playerName.length() > 16) {
									slash.reply("Player name must be 16 characters or less.").setEphemeral(true).queue();
									return;
								}
								String authorName = slash.getUser().getName() + " (" + authorId + ")";
								Pidge.LOGGER.info("Discord /whitelist remove {} requested by {}", playerName, authorName);

								PlayerServer player = server.playerList.getPlayerEntity(playerName);
								if (player != null) {
									server.playerList.removeFromWhiteList(player.uuid);
									String successMsg = I18n.getInstance().translateKeyAndFormat("command.commands.whitelist.remove.success", playerName);
									Pidge.LOGGER.info("Discord /whitelist remove {} succeeded (online)", playerName);
									slash.reply(successMsg).queue();
								} else {
									try {
										UUIDHelper.runConversionAction(playerName,
											(uuid) -> {
												server.playerList.removeFromWhiteList(uuid);
												String successMsg2 = I18n.getInstance().translateKeyAndFormat("command.commands.whitelist.remove.success", playerName);
												Pidge.LOGGER.info("Discord /whitelist remove {} succeeded (offline)", playerName);
												slash.reply(successMsg2).queue();
											},
											(username) -> {
												String failMsg = I18n.getInstance().translateKeyAndFormat("command.commands.whitelist.remove.fail.wrong_name", username);
												Pidge.LOGGER.warn("Discord /whitelist remove {} failed: wrong name", playerName);
												slash.reply(failMsg).setEphemeral(true).queue();
											}
										);
									} catch (Exception e) {
										Pidge.LOGGER.error("Discord /whitelist remove {} failed with exception", playerName, e);
										slash.reply("Failed to remove player: " + e.getMessage()).setEphemeral(true).queue();
									}
								}
								break;
							}
						}
						return;
					}
				}
				return;
			}

			if (!(event instanceof MessageReceivedEvent msg)) return;
			if (msg.getAuthor().isBot() || msg.getAuthor().isSystem()) return;
			if (!msg.isFromGuild()) return;
			if (!msg.getMessage().getChannel().getId().equals(PidgeConfig.getDiscordChannel())) return;

			String raw = msg.getMessage().getContentRaw();
			String authorId = msg.getAuthor().getId();

			// /list
			if (raw.equalsIgnoreCase("/list")) {
				String author = msg.getAuthor().getName() + " (" + authorId + ")";
				Pidge.LOGGER.info("Discord text /list command by {}", author);
				msg.getChannel().sendMessage(BaseChatRelay.getPlayerListString()).queue();
				return;
			}

			// /whitelist add
			if (raw.toLowerCase().startsWith("/whitelist add ")) {
				if (!PidgeConfig.getDiscordAdminIds().contains(authorId)) {
					Pidge.LOGGER.warn("Discord unauthorized text /whitelist add by {} ({})", msg.getAuthor().getName(), authorId);
					msg.getChannel().sendMessage("You are not authorized to use this command.").queue();
					return;
				}
				String[] parts = raw.split(" ");
				if (parts.length < 3) {
					msg.getChannel().sendMessage("Usage: /whitelist add <player>").queue();
					return;
				}
				String playerName = parts[2];
				if (playerName.length() > 16) {
					msg.getChannel().sendMessage("Player name must be 16 characters or less.").queue();
					return;
				}
				handleWhitelistAdd(msg, playerName);
				return;
			}

			// /whitelist reload
			if (raw.equalsIgnoreCase("/whitelist reload")) {
				if (!PidgeConfig.getDiscordAdminIds().contains(authorId)) {
					Pidge.LOGGER.warn("Discord unauthorized text /whitelist reload by {} ({})", msg.getAuthor().getName(), authorId);
					msg.getChannel().sendMessage("You are not authorized to use this command.").queue();
					return;
				}
				Pidge.LOGGER.info("Discord text /whitelist reload requested by {} ({})", msg.getAuthor().getName(), authorId);
				MinecraftServer server = MinecraftServer.getInstance();
				if (server == null || server.playerList == null) {
					Pidge.LOGGER.error("Discord text /whitelist reload failed: server not ready");
					msg.getChannel().sendMessage("Server not ready.").queue();
					return;
				}
				try {
					server.playerList.reloadWhiteList();
					String successMsg = I18n.getInstance().translateKey("command.commands.whitelist.reload");
					msg.getChannel().sendMessage(successMsg).queue();
					Pidge.LOGGER.info("Discord text /whitelist reload succeeded");
				} catch (Exception e) {
					Pidge.LOGGER.error("Discord text /whitelist reload failed", e);
					msg.getChannel().sendMessage("Failed to reload whitelist: " + e.getMessage()).queue();
				}
				return;
			}

			// /whitelist remove
			if (raw.toLowerCase().startsWith("/whitelist remove ")) {
				if (!PidgeConfig.getDiscordAdminIds().contains(authorId)) {
					Pidge.LOGGER.warn("Discord unauthorized text /whitelist remove by {} ({})", msg.getAuthor().getName(), authorId);
					msg.getChannel().sendMessage("You are not authorized to use this command.").queue();
					return;
				}
				String[] parts = raw.split(" ");
				if (parts.length < 3) {
					msg.getChannel().sendMessage("Usage: /whitelist remove <player>").queue();
					return;
				}
				String playerName = parts[2];
				if (playerName.length() > 16) {
					msg.getChannel().sendMessage("Player name must be 16 characters or less.").queue();
					return;
				}
				handleWhitelistRemove(msg, playerName);
				return;
			}

			String author = msg.getAuthor().getName();
			String content = ChatEmotes.process(msg.getMessage().getContentStripped());
			DiscordChatRelay.INSTANCE.sendToMinecraft(author, content);
			TelegramChatRelay.INSTANCE.sendToTelegram("[D] " + author, content);
		}

		private void handleWhitelistAdd(MessageReceivedEvent msg, String playerName) {
			MinecraftServer server = MinecraftServer.getInstance();
			if (server == null || server.playerList == null) {
				msg.getChannel().sendMessage("Server not ready.").queue();
				return;
			}
			PlayerServer player = server.playerList.getPlayerEntity(playerName);
			if (player != null) {
				server.playerList.addToWhiteList(player.uuid);
				String successMsg = I18n.getInstance().translateKeyAndFormat("command.commands.whitelist.add.success", playerName);
				msg.getChannel().sendMessage(successMsg).queue();
			} else {
				try {
					UUIDHelper.runConversionAction(playerName,
						(uuid) -> {
							server.playerList.addToWhiteList(uuid);
							String successMsg2 = I18n.getInstance().translateKeyAndFormat("command.commands.whitelist.add.success", playerName);
							msg.getChannel().sendMessage(successMsg2).queue();
						},
						(username) -> {
							String failMsg = I18n.getInstance().translateKeyAndFormat("command.commands.whitelist.add.fail.wrong_name", username);
							msg.getChannel().sendMessage(failMsg).queue();
						}
					);
				} catch (Exception e) {
					msg.getChannel().sendMessage("Failed to add player: " + e.getMessage()).queue();
				}
			}
		}

		private void handleWhitelistRemove(MessageReceivedEvent msg, String playerName) {
			MinecraftServer server = MinecraftServer.getInstance();
			if (server == null || server.playerList == null) {
				msg.getChannel().sendMessage("Server not ready.").queue();
				return;
			}
			PlayerServer player = server.playerList.getPlayerEntity(playerName);
			if (player != null) {
				server.playerList.removeFromWhiteList(player.uuid);
				String successMsg = I18n.getInstance().translateKeyAndFormat("command.commands.whitelist.remove.success", playerName);
				msg.getChannel().sendMessage(successMsg).queue();
			} else {
				try {
					UUIDHelper.runConversionAction(playerName,
						(uuid) -> {
							server.playerList.removeFromWhiteList(uuid);
							String successMsg2 = I18n.getInstance().translateKeyAndFormat("command.commands.whitelist.remove.success", playerName);
							msg.getChannel().sendMessage(successMsg2).queue();
						},
						(username) -> {
							String failMsg = I18n.getInstance().translateKeyAndFormat("command.commands.whitelist.remove.fail.wrong_name", username);
							msg.getChannel().sendMessage(failMsg).queue();
						}
					);
				} catch (Exception e) {
					msg.getChannel().sendMessage("Failed to remove player: " + e.getMessage()).queue();
				}
			}
		}
	}
}
