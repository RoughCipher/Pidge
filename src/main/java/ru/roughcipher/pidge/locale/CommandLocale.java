package ru.roughcipher.pidge.locale;

import net.dv8tion.jda.api.interactions.DiscordLocale;

public interface CommandLocale {
	DiscordLocale discordLocale();

	String languageCode();

	String listDescription();

	String whitelistDescription();

	String whitelistAddDescription();

	String whitelistReloadDescription();

	String whitelistRemoveDescription();

	String whitelistOnDescription();

	String whitelistOffDescription();

	String banDescription();

	String unbanDescription();

	String playerOption();

	String backendOption();
}
