package ru.roughcipher.pidge.discord.locale;

import net.dv8tion.jda.api.interactions.DiscordLocale;

public interface CommandLocale {
	DiscordLocale locale();

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
}
