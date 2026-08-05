package ru.roughcipher.pidge.discord.locale;

import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CommandLocales {
	private CommandLocales() {}

	public static final List<CommandLocale> ALL = List.of(
		EnglishLocale.INSTANCE,
		RussianLocale.INSTANCE
	);

	public static List<SlashCommandData> buildCommands() {
		EnglishLocale en = EnglishLocale.INSTANCE;

		Map<DiscordLocale, String> listDesc = map(CommandLocale::listDescription);
		Map<DiscordLocale, String> wlDesc = map(CommandLocale::whitelistDescription);
		Map<DiscordLocale, String> addDesc = map(CommandLocale::whitelistAddDescription);
		Map<DiscordLocale, String> reloadDesc = map(CommandLocale::whitelistReloadDescription);
		Map<DiscordLocale, String> removeDesc = map(CommandLocale::whitelistRemoveDescription);
		Map<DiscordLocale, String> onDesc = map(CommandLocale::whitelistOnDescription);
		Map<DiscordLocale, String> offDesc = map(CommandLocale::whitelistOffDescription);
		Map<DiscordLocale, String> banDesc = map(CommandLocale::banDescription);
		Map<DiscordLocale, String> unbanDesc = map(CommandLocale::unbanDescription);
		Map<DiscordLocale, String> playerOpt = map(CommandLocale::playerOption);

		SlashCommandData list = Commands.slash("list", en.listDescription())
			.setDescriptionLocalizations(listDesc);

		OptionData playerOption = new OptionData(OptionType.STRING, "player", en.playerOption(), true)
			.setDescriptionLocalizations(playerOpt);

		SubcommandData add = new SubcommandData("add", en.whitelistAddDescription())
			.setDescriptionLocalizations(addDesc)
			.addOptions(playerOption);
		SubcommandData reload = new SubcommandData("reload", en.whitelistReloadDescription())
			.setDescriptionLocalizations(reloadDesc);
		SubcommandData remove = new SubcommandData("remove", en.whitelistRemoveDescription())
			.setDescriptionLocalizations(removeDesc)
			.addOptions(new OptionData(OptionType.STRING, "player", en.playerOption(), true)
				.setDescriptionLocalizations(playerOpt));
		SubcommandData on = new SubcommandData("on", en.whitelistOnDescription())
			.setDescriptionLocalizations(onDesc);
		SubcommandData off = new SubcommandData("off", en.whitelistOffDescription())
			.setDescriptionLocalizations(offDesc);

		SlashCommandData whitelist = Commands.slash("whitelist", en.whitelistDescription())
			.setDescriptionLocalizations(wlDesc)
			.addSubcommands(add, reload, remove, on, off);

		SlashCommandData ban = Commands.slash("ban", en.banDescription())
			.setDescriptionLocalizations(banDesc)
			.addOptions(new OptionData(OptionType.STRING, "player", en.playerOption(), true)
				.setDescriptionLocalizations(playerOpt));

		SlashCommandData unban = Commands.slash("unban", en.unbanDescription())
			.setDescriptionLocalizations(unbanDesc)
			.addOptions(new OptionData(OptionType.STRING, "player", en.playerOption(), true)
				.setDescriptionLocalizations(playerOpt));

		return List.of(list, whitelist, ban, unban);
	}

	private static Map<DiscordLocale, String> map(java.util.function.Function<CommandLocale, String> getter) {
		Map<DiscordLocale, String> result = new HashMap<>();
		for (CommandLocale loc : ALL) {
			if (loc.locale() == DiscordLocale.ENGLISH_US) continue;
			result.put(loc.locale(), getter.apply(loc));
		}
		return result;
	}
}
