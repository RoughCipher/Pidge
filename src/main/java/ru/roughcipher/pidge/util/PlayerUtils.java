package ru.roughcipher.pidge.util;

import net.minecraft.server.entity.player.PlayerServer;

public class PlayerUtils {
	public static String getDisplayName(PlayerServer player) {
		if (player == null) return "";
		String nickname = player.nickname;
		if (nickname != null && !nickname.isEmpty()) {
			return MessageUtils.stripColorCodes(nickname);
		}
		return player.username;
	}
}
