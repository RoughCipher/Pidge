package ru.roughcipher.pidge.locale;

import net.dv8tion.jda.api.interactions.DiscordLocale;

public final class UkrainianLocale implements CommandLocale {
	public static final UkrainianLocale INSTANCE = new UkrainianLocale();

	private UkrainianLocale() {}

	@Override
	public DiscordLocale discordLocale() {
		return DiscordLocale.UKRAINIAN;
	}

	@Override
	public String languageCode() {
		return "uk";
	}

	@Override
	public String listDescription() {
		return "Список гравців на сервері";
	}

	@Override
	public String whitelistDescription() {
		return "Керування білим списком сервера";
	}

	@Override
	public String whitelistAddDescription() {
		return "Додати гравця до білого списку";
	}

	@Override
	public String whitelistReloadDescription() {
		return "Перезавантажити білий список";
	}

	@Override
	public String whitelistRemoveDescription() {
		return "Видалити гравця з білого списку";
	}

	@Override
	public String whitelistOnDescription() {
		return "Увімкнути білий список";
	}

	@Override
	public String whitelistOffDescription() {
		return "Вимкнути білий список";
	}

	@Override
	public String banDescription() {
		return "Заблокувати гравця";
	}

	@Override
	public String unbanDescription() {
		return "Розблокувати гравця";
	}

	@Override
	public String playerOption() {
		return "Ім'я гравця";
	}

	@Override
	public String backendOption() {
		return "Бекенд авторизації (ely або mojang)";
	}
}
