package ru.roughcipher.pidge.discord.locale;

import net.dv8tion.jda.api.interactions.DiscordLocale;

public final class RussianLocale implements CommandLocale {
	public static final RussianLocale INSTANCE = new RussianLocale();

	private RussianLocale() {}

	@Override
	public DiscordLocale locale() {
		return DiscordLocale.RUSSIAN;
	}

	@Override
	public String listDescription() {
		return "Список игроков на сервере";
	}

	@Override
	public String whitelistDescription() {
		return "Управление белым списком сервера";
	}

	@Override
	public String whitelistAddDescription() {
		return "Добавить игрока в белый список";
	}

	@Override
	public String whitelistAddPlayerOption() {
		return "Имя игрока";
	}

	@Override
	public String whitelistReloadDescription() {
		return "Перезагрузить белый список";
	}

	@Override
	public String whitelistRemoveDescription() {
		return "Удалить игрока из белого списка";
	}

	@Override
	public String whitelistRemovePlayerOption() {
		return "Имя игрока";
	}

	@Override
	public String whitelistOnDescription() {
		return "Включить белый список";
	}

	@Override
	public String whitelistOffDescription() {
		return "Выключить белый список";
	}
}
