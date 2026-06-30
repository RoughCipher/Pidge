package ru.roughcipher.pidge.discord;

import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.telegram.TelegramChatRelay;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
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
			if (!(event instanceof MessageReceivedEvent)) return;
			MessageReceivedEvent msg = (MessageReceivedEvent) event;
			if (msg.getAuthor().isBot() || msg.getAuthor().isSystem()) return;
			if (!msg.isFromGuild()) return;
			if (!msg.getMessage().getChannel().getId().equals(PidgeConfig.getDiscordChannel())) return;

			String author = msg.getAuthor().getName();
			String content = ChatEmotes.process(msg.getMessage().getContentStripped());
			DiscordChatRelay.sendToMinecraft(author, content);
			TelegramChatRelay.sendToTelegram("[D] " + author, content);
		}
	}
}
