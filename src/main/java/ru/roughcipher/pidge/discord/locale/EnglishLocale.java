package ru.roughcipher.pidge.discord.locale;

import net.dv8tion.jda.api.interactions.DiscordLocale;

public final class EnglishLocale implements CommandLocale {
	public static final EnglishLocale INSTANCE = new EnglishLocale();

	private EnglishLocale() {}

	@Override
	public DiscordLocale locale() {
		return DiscordLocale.ENGLISH_US;
	}

	@Override
	public String listDescription() {
		return "Show online players";
	}

	@Override
	public String whitelistDescription() {
		return "Manage server whitelist";
	}

	@Override
	public String whitelistAddDescription() {
		return "Add a player to whitelist";
	}

	@Override
	public String whitelistReloadDescription() {
		return "Reload whitelist";
	}

	@Override
	public String whitelistRemoveDescription() {
		return "Remove a player from whitelist";
	}

	@Override
	public String whitelistOnDescription() {
		return "Enable whitelist";
	}

	@Override
	public String whitelistOffDescription() {
		return "Disable whitelist";
	}

	@Override
	public String banDescription() {
		return "Ban a player";
	}

	@Override
	public String unbanDescription() {
		return "Unban a player";
	}

	@Override
	public String playerOption() {
		return "Player name";
	}

	@Override
	public String backendOption() {
		return "Auth backend (ely or mojang)";
	}
}
