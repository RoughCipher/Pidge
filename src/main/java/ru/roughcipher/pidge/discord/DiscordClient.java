package ru.roughcipher.pidge.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.core.lang.I18n;
import net.minecraft.core.net.ChatEmotes;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.telegram.TelegramChatRelay;
import ru.roughcipher.pidge.util.BaseChatRelay;
import ru.roughcipher.pidge.util.MessageUtils;
import ru.roughcipher.pidge.util.AdminCommands;
import ru.roughcipher.pidge.util.ProxyUtils;
import ru.roughcipher.pidge.locale.CommandLocales;

import okhttp3.OkHttpClient;

import java.util.EnumSet;

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

			//HTTP/HTTPS/SOCKS-прокси
			ProxyUtils.ParsedProxy parsedProxy = ProxyUtils.parse(PidgeConfig.getProxy());
			if (parsedProxy != null) {
				OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
				ProxyUtils.applyTo(parsedProxy, httpBuilder, "Discord");
				builder.setHttpClientBuilder(httpBuilder);
			}

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

			jda.updateCommands().queue(
				success -> Pidge.LOGGER.info("Cleared global Discord commands"),
				failure -> Pidge.LOGGER.error("Failed to clear global Discord commands", failure)
			);

			StandardGuildMessageChannel ch = getChannel();
			if (ch != null) {
				Guild guild = ch.getGuild();
				guild.updateCommands()
					.addCommands(CommandLocales.buildDiscordCommands())
					.queue(
						success -> Pidge.LOGGER.info("Registered {} commands on guild {}", success.size(), guild.getName()),
						failure -> Pidge.LOGGER.error("Failed to register commands on guild", failure)
					);
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
						String response = BaseChatRelay.getPlayerListString();
						slash.reply(MessageUtils.escapeDiscordMarkdown(response))
							.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
							.queue();
						return;
					}
					case "whitelist": {
						String sub = slash.getSubcommandName();
						if (sub == null) return;
						String authorId = slash.getUser().getId();
						if (!PidgeConfig.getDiscordAdminIds().contains(authorId)) {
							Pidge.LOGGER.warn("Discord unauthorized /whitelist {} by {} ({})", sub, slash.getUser().getName(), authorId);
							slash.reply(MessageUtils.escapeDiscordMarkdown("You are not authorized to use this command."))
								.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
								.setEphemeral(true)
								.queue();
							return;
						}

						MinecraftServer server = MinecraftServer.getInstance();
						if (server == null || server.playerList == null) {
							Pidge.LOGGER.error("Discord /whitelist {} failed: server not ready", sub);
							slash.reply(MessageUtils.escapeDiscordMarkdown("Server not ready."))
								.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
								.setEphemeral(true)
								.queue();
							return;
						}

						switch (sub) {
							case "add": {
								var option = slash.getOption("player");
								if (option == null) {
									slash.reply(MessageUtils.escapeDiscordMarkdown("Please specify a player name."))
										.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
										.setEphemeral(true)
										.queue();
									break;
								}
								String playerName = option.getAsString();
								if (playerName.length() > 16) {
									slash.reply(MessageUtils.escapeDiscordMarkdown("Player name must be 16 characters or less."))
										.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
										.setEphemeral(true)
										.queue();
									break;
								}
								String backend = slashBackend(slash);
								Pidge.LOGGER.info("Discord /whitelist add {} {} by {}", playerName, backend, slash.getUser().getName());
								AdminCommands.whitelistAdd(playerName, backend, slashReply(slash));
								break;
							}
							case "reload": {
								Pidge.LOGGER.info("Discord /whitelist reload requested by {} ({})", slash.getUser().getName(), authorId);
								try {
									server.playerList.reloadWhiteList();
									String msg = I18n.getInstance().translateKey("command.commands.whitelist.reload");
									slash.reply(MessageUtils.escapeDiscordMarkdown(msg))
										.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
										.queue();
									Pidge.LOGGER.info("Discord /whitelist reload succeeded");
								} catch (Exception e) {
									Pidge.LOGGER.error("Discord /whitelist reload failed", e);
									slash.reply(MessageUtils.escapeDiscordMarkdown("Failed to reload whitelist: " + e.getMessage()))
										.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
										.setEphemeral(true)
										.queue();
								}
								break;
							}
							case "remove": {
								var option = slash.getOption("player");
								if (option == null) {
									slash.reply(MessageUtils.escapeDiscordMarkdown("Please specify a player name."))
										.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
										.setEphemeral(true)
										.queue();
									break;
								}
								String playerName = option.getAsString();
								if (playerName.length() > 16) {
									slash.reply(MessageUtils.escapeDiscordMarkdown("Player name must be 16 characters or less."))
										.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
										.setEphemeral(true)
										.queue();
									break;
								}
								String backend = slashBackend(slash);
								Pidge.LOGGER.info("Discord /whitelist remove {} {} by {}", playerName, backend, slash.getUser().getName());
								AdminCommands.whitelistRemove(playerName, backend, slashReply(slash));
								break;
							}
							case "on": {
								server.propertyManager.setProperty("white-list", true);
								server.playerList.whitelistEnforced = true;
								String msg = I18n.getInstance().translateKey("command.commands.whitelist.on.success");
								slash.reply(MessageUtils.escapeDiscordMarkdown(msg))
									.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
									.queue();
								Pidge.LOGGER.info("Discord /whitelist on executed by {} ({})", slash.getUser().getName(), authorId);
								break;
							}
							case "off": {
								server.propertyManager.setProperty("white-list", false);
								server.playerList.whitelistEnforced = false;
								String msg = I18n.getInstance().translateKey("command.commands.whitelist.off.success");
								slash.reply(MessageUtils.escapeDiscordMarkdown(msg))
									.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
									.queue();
								Pidge.LOGGER.info("Discord /whitelist off executed by {} ({})", slash.getUser().getName(), authorId);
								break;
							}
						}
						return;
					}

					case "ban": {
						String authorId = slash.getUser().getId();
						if (!PidgeConfig.getDiscordAdminIds().contains(authorId)) {
							Pidge.LOGGER.warn("Discord unauthorized /ban by {} ({})", slash.getUser().getName(), authorId);
							slash.reply(MessageUtils.escapeDiscordMarkdown("You are not authorized to use this command."))
								.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
								.setEphemeral(true)
								.queue();
							return;
						}
						var option = slash.getOption("player");
						if (option == null) {
							slash.reply(MessageUtils.escapeDiscordMarkdown("Please specify a player name."))
								.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
								.setEphemeral(true)
								.queue();
							return;
						}
						String playerName = option.getAsString();
						if (playerName.length() > 16) {
							slash.reply(MessageUtils.escapeDiscordMarkdown("Player name must be 16 characters or less."))
								.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
								.setEphemeral(true)
								.queue();
							return;
						}
						String backend = slashBackend(slash);
						Pidge.LOGGER.info("Discord /ban {} {} by {}", playerName, backend, slash.getUser().getName());
						AdminCommands.ban(playerName, backend, slashReply(slash));
						return;
					}
					case "unban": {
						String authorId = slash.getUser().getId();
						if (!PidgeConfig.getDiscordAdminIds().contains(authorId)) {
							Pidge.LOGGER.warn("Discord unauthorized /unban by {} ({})", slash.getUser().getName(), authorId);
							slash.reply(MessageUtils.escapeDiscordMarkdown("You are not authorized to use this command."))
								.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
								.setEphemeral(true)
								.queue();
							return;
						}
						var option = slash.getOption("player");
						if (option == null) {
							slash.reply(MessageUtils.escapeDiscordMarkdown("Please specify a player name."))
								.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
								.setEphemeral(true)
								.queue();
							return;
						}
						String playerName = option.getAsString();
						if (playerName.length() > 16) {
							slash.reply(MessageUtils.escapeDiscordMarkdown("Player name must be 16 characters or less."))
								.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
								.setEphemeral(true)
								.queue();
							return;
						}
						String backend = slashBackend(slash);
						Pidge.LOGGER.info("Discord /unban {} {} by {}", playerName, backend, slash.getUser().getName());
						AdminCommands.unban(playerName, backend, slashReply(slash));
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
				String response = BaseChatRelay.getPlayerListString();
				msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown(response))
					.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
					.queue();
				return;
			}

			// /whitelist on / off
			if (raw.equalsIgnoreCase("/whitelist on") || raw.equalsIgnoreCase("/whitelist off")) {
				if (!PidgeConfig.getDiscordAdminIds().contains(authorId)) {
					Pidge.LOGGER.warn("Discord unauthorized text /whitelist {} by {} ({})", raw, msg.getAuthor().getName(), authorId);
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("You are not authorized to use this command."))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				boolean enable = raw.equalsIgnoreCase("/whitelist on");
				MinecraftServer server = MinecraftServer.getInstance();
				if (server == null || server.playerList == null) {
					Pidge.LOGGER.error("Discord text /whitelist {} failed: server not ready", raw);
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("Server not ready."))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				server.propertyManager.setProperty("white-list", enable);
				server.playerList.whitelistEnforced = enable;
				String key = enable ? "command.commands.whitelist.on.success" : "command.commands.whitelist.off.success";
				String response = I18n.getInstance().translateKey(key);
				msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown(response))
					.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
					.queue();
				Pidge.LOGGER.info("Discord text /whitelist {} executed by {} ({})", raw, msg.getAuthor().getName(), authorId);
				return;
			}

			// /whitelist add
			if (raw.toLowerCase().startsWith("/whitelist add ")) {
				if (!PidgeConfig.getDiscordAdminIds().contains(authorId)) {
					Pidge.LOGGER.warn("Discord unauthorized text /whitelist add by {} ({})", msg.getAuthor().getName(), authorId);
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("You are not authorized to use this command."))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				String[] parts = raw.split(" ");
				if (parts.length < 3) {
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("Usage: /whitelist add <player> [ely|mojang]"))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				String playerName = parts[2];
				if (playerName.length() > 16) {
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("Player name must be 16 characters or less."))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				String backend = AdminCommands.parseBackendArg(parts, 3);
				handleWhitelistAdd(msg, playerName, backend);
				return;
			}

			// /whitelist reload
			if (raw.equalsIgnoreCase("/whitelist reload")) {
				if (!PidgeConfig.getDiscordAdminIds().contains(authorId)) {
					Pidge.LOGGER.warn("Discord unauthorized text /whitelist reload by {} ({})", msg.getAuthor().getName(), authorId);
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("You are not authorized to use this command."))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				Pidge.LOGGER.info("Discord text /whitelist reload requested by {} ({})", msg.getAuthor().getName(), authorId);
				MinecraftServer server = MinecraftServer.getInstance();
				if (server == null || server.playerList == null) {
					Pidge.LOGGER.error("Discord text /whitelist reload failed: server not ready");
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("Server not ready."))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				try {
					server.playerList.reloadWhiteList();
					String successMsg = I18n.getInstance().translateKey("command.commands.whitelist.reload");
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown(successMsg))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					Pidge.LOGGER.info("Discord text /whitelist reload succeeded");
				} catch (Exception e) {
					Pidge.LOGGER.error("Discord text /whitelist reload failed", e);
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("Failed to reload whitelist: " + e.getMessage()))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
				}
				return;
			}

			// /whitelist remove
			if (raw.toLowerCase().startsWith("/whitelist remove ")) {
				if (!PidgeConfig.getDiscordAdminIds().contains(authorId)) {
					Pidge.LOGGER.warn("Discord unauthorized text /whitelist remove by {} ({})", msg.getAuthor().getName(), authorId);
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("You are not authorized to use this command."))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				String[] parts = raw.split(" ");
				if (parts.length < 3) {
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("Usage: /whitelist remove <player> [ely|mojang]"))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				String playerName = parts[2];
				if (playerName.length() > 16) {
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("Player name must be 16 characters or less."))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				String backend = AdminCommands.parseBackendArg(parts, 3);
				handleWhitelistRemove(msg, playerName, backend);
				return;
			}

			// /ban <player>
			if (raw.toLowerCase().startsWith("/ban ")) {
				if (!PidgeConfig.getDiscordAdminIds().contains(authorId)) {
					Pidge.LOGGER.warn("Discord unauthorized text /ban by {} ({})", msg.getAuthor().getName(), authorId);
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("You are not authorized to use this command."))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				String[] parts = raw.split(" ");
				if (parts.length < 2) {
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("Usage: /ban <player> [ely|mojang]"))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				String playerName = parts[1];
				if (playerName.length() > 16) {
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("Player name must be 16 characters or less."))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				String backend = AdminCommands.parseBackendArg(parts, 2);
				Pidge.LOGGER.info("Discord text /ban {} {} requested by {} ({})", playerName, backend, msg.getAuthor().getName(), authorId);
				handleBan(msg, playerName, backend);
				return;
			}

			// /unban <player>
			if (raw.toLowerCase().startsWith("/unban ")) {
				if (!PidgeConfig.getDiscordAdminIds().contains(authorId)) {
					Pidge.LOGGER.warn("Discord unauthorized text /unban by {} ({})", msg.getAuthor().getName(), authorId);
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("You are not authorized to use this command."))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				String[] parts = raw.split(" ");
				if (parts.length < 2) {
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("Usage: /unban <player> [ely|mojang]"))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				String playerName = parts[1];
				if (playerName.length() > 16) {
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown("Player name must be 16 characters or less."))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
					return;
				}
				String backend = AdminCommands.parseBackendArg(parts, 2);
				Pidge.LOGGER.info("Discord text /unban {} {} requested by {} ({})", playerName, backend, msg.getAuthor().getName(), authorId);
				handleUnban(msg, playerName, backend);
				return;
			}

			String author = msg.getAuthor().getName();
			String content = ChatEmotes.process(msg.getMessage().getContentStripped());
			DiscordChatRelay.INSTANCE.sendToMinecraft(author, content);
			TelegramChatRelay.INSTANCE.sendToTelegram("[D] " + author, content);
		}

		private static AdminCommands.Reply slashReply(SlashCommandInteractionEvent slash) {
			return new AdminCommands.Reply() {
				@Override
				public void success(String message) {
					slash.reply(MessageUtils.escapeDiscordMarkdown(message))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
				}
				@Override
				public void failure(String message) {
					slash.reply(MessageUtils.escapeDiscordMarkdown(message))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.setEphemeral(true)
						.queue();
				}
			};
		}

		private static AdminCommands.Reply textReply(MessageReceivedEvent msg) {
			return new AdminCommands.Reply() {
				@Override
				public void success(String message) {
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown(message))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
				}
				@Override
				public void failure(String message) {
					msg.getChannel().sendMessage(MessageUtils.escapeDiscordMarkdown(message))
						.setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
						.queue();
				}
			};
		}

		@Nullable
		private static String slashBackend(SlashCommandInteractionEvent slash) {
			var opt = slash.getOption("backend");
			return opt != null ? opt.getAsString() : null;
		}

		private void handleWhitelistAdd(MessageReceivedEvent msg, String playerName, String backend) {
			AdminCommands.whitelistAdd(playerName, backend, textReply(msg));
		}

		private void handleWhitelistRemove(MessageReceivedEvent msg, String playerName, String backend) {
			AdminCommands.whitelistRemove(playerName, backend, textReply(msg));
		}

		private void handleBan(MessageReceivedEvent msg, String playerName, String backend) {
			AdminCommands.ban(playerName, backend, textReply(msg));
		}

		private void handleUnban(MessageReceivedEvent msg, String playerName, String backend) {
			AdminCommands.unban(playerName, backend, textReply(msg));
		}
	}
}

