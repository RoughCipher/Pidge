package ru.roughcipher.pidge.locale;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pengrad.telegrambot.response.BaseResponse;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.util.BwebCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CommandLocales {
	private CommandLocales() {}

	public static final List<CommandLocale> ALL = List.of(
		EnglishLocale.INSTANCE,
		RussianLocale.INSTANCE,
		UkrainianLocale.INSTANCE
	);

	public static List<SlashCommandData> buildDiscordCommands() {
		EnglishLocale en = EnglishLocale.INSTANCE;
		boolean bweb = BwebCompat.isLoaded();

		Map<DiscordLocale, String> listDesc = mapDiscord(CommandLocale::listDescription);
		Map<DiscordLocale, String> wlDesc = mapDiscord(CommandLocale::whitelistDescription);
		Map<DiscordLocale, String> addDesc = mapDiscord(CommandLocale::whitelistAddDescription);
		Map<DiscordLocale, String> reloadDesc = mapDiscord(CommandLocale::whitelistReloadDescription);
		Map<DiscordLocale, String> removeDesc = mapDiscord(CommandLocale::whitelistRemoveDescription);
		Map<DiscordLocale, String> onDesc = mapDiscord(CommandLocale::whitelistOnDescription);
		Map<DiscordLocale, String> offDesc = mapDiscord(CommandLocale::whitelistOffDescription);
		Map<DiscordLocale, String> banDesc = mapDiscord(CommandLocale::banDescription);
		Map<DiscordLocale, String> unbanDesc = mapDiscord(CommandLocale::unbanDescription);
		Map<DiscordLocale, String> playerOpt = mapDiscord(CommandLocale::playerOption);
		Map<DiscordLocale, String> backendOpt = mapDiscord(CommandLocale::backendOption);

		SlashCommandData list = Commands.slash("list", en.listDescription())
			.setDescriptionLocalizations(listDesc);

		SubcommandData add = new SubcommandData("add", en.whitelistAddDescription())
			.setDescriptionLocalizations(addDesc)
			.addOptions(playerOption(en, playerOpt));
		SubcommandData reload = new SubcommandData("reload", en.whitelistReloadDescription())
			.setDescriptionLocalizations(reloadDesc);
		SubcommandData remove = new SubcommandData("remove", en.whitelistRemoveDescription())
			.setDescriptionLocalizations(removeDesc)
			.addOptions(playerOption(en, playerOpt));
		SubcommandData on = new SubcommandData("on", en.whitelistOnDescription())
			.setDescriptionLocalizations(onDesc);
		SubcommandData off = new SubcommandData("off", en.whitelistOffDescription())
			.setDescriptionLocalizations(offDesc);

		if (bweb) {
			add.addOptions(backendOption(en, backendOpt));
			remove.addOptions(backendOption(en, backendOpt));
		}

		SlashCommandData whitelist = Commands.slash("whitelist", en.whitelistDescription())
			.setDescriptionLocalizations(wlDesc)
			.addSubcommands(add, reload, remove, on, off);

		SlashCommandData ban = Commands.slash("ban", en.banDescription())
			.setDescriptionLocalizations(banDesc)
			.addOptions(playerOption(en, playerOpt));
		SlashCommandData unban = Commands.slash("unban", en.unbanDescription())
			.setDescriptionLocalizations(unbanDesc)
			.addOptions(playerOption(en, playerOpt));

		if (bweb) {
			ban.addOptions(backendOption(en, backendOpt));
			unban.addOptions(backendOption(en, backendOpt));
		}

		return List.of(list, whitelist, ban, unban);
	}

	public static void registerTelegramCommands(TelegramBot bot) {
		if (bot == null) return;

		BaseResponse def = bot.execute(new SetMyCommands(telegramCommands(EnglishLocale.INSTANCE)));
		if (!def.isOk()) {
			Pidge.LOGGER.warn("Telegram setMyCommands (default) failed: {}", def.description());
		} else {
			Pidge.LOGGER.info("Telegram commands registered (default/en)");
		}

		for (CommandLocale loc : ALL) {
			String code = loc.languageCode();
			if (code == null || code.isEmpty() || "en".equalsIgnoreCase(code)) continue;

			BaseResponse resp = bot.execute(
				new SetMyCommands(telegramCommands(loc)).languageCode(code)
			);
			if (!resp.isOk()) {
				Pidge.LOGGER.warn("Telegram setMyCommands ({}) failed: {}", code, resp.description());
			} else {
				Pidge.LOGGER.info("Telegram commands registered ({})", code);
			}
		}
	}

	private static BotCommand[] telegramCommands(CommandLocale loc) {
		boolean bweb = BwebCompat.isLoaded();
		String banDesc = loc.banDescription();
		String unbanDesc = loc.unbanDescription();
		String wlDesc = loc.whitelistDescription();
		if (bweb) {
			banDesc = banDesc + " [ely|mojang]";
			unbanDesc = unbanDesc + " [ely|mojang]";
			wlDesc = wlDesc + " [ely|mojang]";
		}
		return new BotCommand[] {
			new BotCommand("list", truncate(loc.listDescription())),
			new BotCommand("whitelist", truncate(wlDesc)),
			new BotCommand("ban", truncate(banDesc)),
			new BotCommand("unban", truncate(unbanDesc))
		};
	}

	private static String truncate(String s) {
		if (s == null) return "";
		return s.length() <= 256 ? s : s.substring(0, 256);
	}

	private static OptionData playerOption(EnglishLocale en, Map<DiscordLocale, String> locs) {
		return new OptionData(OptionType.STRING, "player", en.playerOption(), true)
			.setDescriptionLocalizations(locs);
	}

	private static OptionData backendOption(EnglishLocale en, Map<DiscordLocale, String> locs) {
		return new OptionData(OptionType.STRING, "backend", en.backendOption(), false)
			.setDescriptionLocalizations(locs)
			.addChoices(
				new Command.Choice("ely", "ely"),
				new Command.Choice("mojang", "mojang")
			);
	}

	private static Map<DiscordLocale, String> mapDiscord(java.util.function.Function<CommandLocale, String> getter) {
		Map<DiscordLocale, String> result = new HashMap<>();
		for (CommandLocale loc : ALL) {
			if (loc.discordLocale() == DiscordLocale.ENGLISH_US) continue;
			result.put(loc.discordLocale(), getter.apply(loc));
		}
		return result;
	}
}
