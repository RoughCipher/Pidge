package ru.roughcipher.pidge.discord;

import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.telegram.TelegramChatRelay;
import ru.roughcipher.pidge.util.BaseChatRelay;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.core.net.ChatEmotes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
						Commands.slash("list", "Show online players")
					).queue(
						success -> Pidge.LOGGER.info("Registered /list command on guild {}", guild.getName()),
						failure -> Pidge.LOGGER.error("Failed to register /list command on guild", failure)
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
				if (slash.getName().equals("list")) {
					slash.reply(BaseChatRelay.getPlayerListString()).queue();
				}
				return;
			}

			if (!(event instanceof MessageReceivedEvent msg)) return;
			if (msg.getAuthor().isBot() || msg.getAuthor().isSystem()) return;
			if (!msg.isFromGuild()) return;
			if (!msg.getMessage().getChannel().getId().equals(PidgeConfig.getDiscordChannel())) return;

			String raw = msg.getMessage().getContentRaw();
			if (raw.equalsIgnoreCase("/list")) {
				msg.getChannel().sendMessage(BaseChatRelay.getPlayerListString()).queue();
				return;
			}

			String author = msg.getAuthor().getName();
			String content = ChatEmotes.process(msg.getMessage().getContentStripped());
			DiscordChatRelay.INSTANCE.sendToMinecraft(author, content);
			TelegramChatRelay.INSTANCE.sendToTelegram("[D] " + author, content);
		}
	}
}
