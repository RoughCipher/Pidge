package ru.roughcipher.pidge.util;

import net.minecraft.core.lang.I18n;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.player.PlayerServer;
import org.jetbrains.annotations.Nullable;

public final class AdminCommands {
	private AdminCommands() {}

	public interface Reply {
		void success(String message);
		void failure(String message);
	}

	public static void ban(String playerName, @Nullable String backend, Reply reply) {
		MinecraftServer server = MinecraftServer.getInstance();
		if (server == null || server.playerList == null) {
			reply.failure("Server not ready.");
			return;
		}
		if (backend == null || backend.isEmpty()) {
			PlayerServer online = server.playerList.getPlayerEntity(playerName);
			if (online != null) {
				server.playerList.banPlayer(online.uuid);
				online.playerNetServerHandler.kickPlayer("Banned by admin");
				reply.success(I18n.getInstance().translateKeyAndFormat("command.commands.ban.success", online.username));
				return;
			}
		} else if (!BwebCompat.isLoaded()) {
			reply.failure("Backend requires mod 'bweb' (better_with_elyby).");
			return;
		} else if (!BwebCompat.isValidBackend(backend)) {
			reply.failure("Backend must be ely or mojang.");
			return;
		}

		BwebCompat.resolveUuid(playerName, emptyToNull(backend),
			uuid -> {
				server.playerList.banPlayer(uuid);
				PlayerServer online = server.playerList.getPlayerEntity(playerName);
				if (online != null && online.uuid.equals(uuid)) {
					online.playerNetServerHandler.kickPlayer("Banned by admin");
				}
				reply.success(I18n.getInstance().translateKeyAndFormat(
					"command.commands.ban.username.success", playerName));
			},
			name -> reply.failure(I18n.getInstance().translateKeyAndFormat(
				"command.commands.ban.username.fail.wrong_name", playerName))
		);
	}

	public static void unban(String playerName, @Nullable String backend, Reply reply) {
		MinecraftServer server = MinecraftServer.getInstance();
		if (server == null || server.playerList == null) {
			reply.failure("Server not ready.");
			return;
		}
		if (backend != null && !backend.isEmpty()) {
			if (!BwebCompat.isLoaded()) {
				reply.failure("Backend requires mod 'bweb' (better_with_elyby).");
				return;
			}
			if (!BwebCompat.isValidBackend(backend)) {
				reply.failure("Backend must be ely or mojang.");
				return;
			}
		}
		BwebCompat.resolveUuid(playerName, emptyToNull(backend),
			uuid -> {
				server.playerList.pardonPlayer(uuid);
				reply.success(I18n.getInstance().translateKeyAndFormat(
					"command.commands.unban.username.success", playerName));
			},
			name -> reply.failure(I18n.getInstance().translateKeyAndFormat(
				"command.commands.unban.username.fail.wrong_name", playerName))
		);
	}

	public static void whitelistAdd(String playerName, @Nullable String backend, Reply reply) {
		MinecraftServer server = MinecraftServer.getInstance();
		if (server == null || server.playerList == null) {
			reply.failure("Server not ready.");
			return;
		}
		if (backend == null || backend.isEmpty()) {
			PlayerServer online = server.playerList.getPlayerEntity(playerName);
			if (online != null) {
				server.playerList.addToWhiteList(online.uuid);
				reply.success(I18n.getInstance().translateKeyAndFormat(
					"command.commands.whitelist.add.success", playerName));
				return;
			}
		} else if (!BwebCompat.isLoaded()) {
			reply.failure("Backend requires mod 'bweb' (better_with_elyby).");
			return;
		} else if (!BwebCompat.isValidBackend(backend)) {
			reply.failure("Backend must be ely or mojang.");
			return;
		}
		BwebCompat.resolveUuid(playerName, emptyToNull(backend),
			uuid -> {
				server.playerList.addToWhiteList(uuid);
				reply.success(I18n.getInstance().translateKeyAndFormat(
					"command.commands.whitelist.add.success", playerName));
			},
			name -> reply.failure(I18n.getInstance().translateKeyAndFormat(
				"command.commands.whitelist.add.fail.wrong_name", playerName))
		);
	}

	public static void whitelistRemove(String playerName, @Nullable String backend, Reply reply) {
		MinecraftServer server = MinecraftServer.getInstance();
		if (server == null || server.playerList == null) {
			reply.failure("Server not ready.");
			return;
		}
		if (backend == null || backend.isEmpty()) {
			PlayerServer online = server.playerList.getPlayerEntity(playerName);
			if (online != null) {
				server.playerList.removeFromWhiteList(online.uuid);
				reply.success(I18n.getInstance().translateKeyAndFormat(
					"command.commands.whitelist.remove.success", playerName));
				return;
			}
		} else if (!BwebCompat.isLoaded()) {
			reply.failure("Backend requires mod 'bweb' (better_with_elyby).");
			return;
		} else if (!BwebCompat.isValidBackend(backend)) {
			reply.failure("Backend must be ely or mojang.");
			return;
		}
		BwebCompat.resolveUuid(playerName, emptyToNull(backend),
			uuid -> {
				server.playerList.removeFromWhiteList(uuid);
				reply.success(I18n.getInstance().translateKeyAndFormat(
					"command.commands.whitelist.remove.success", playerName));
			},
			name -> reply.failure(I18n.getInstance().translateKeyAndFormat(
				"command.commands.whitelist.remove.fail.wrong_name", playerName))
		);
	}

	@Nullable
	public static String parseBackendArg(String[] parts, int backendIndex) {
		if (parts.length <= backendIndex) return null;
		String b = parts[backendIndex];
		if (BwebCompat.isValidBackend(b)) return b.trim().toLowerCase();
		return null;
	}

	@Nullable
	private static String emptyToNull(@Nullable String s) {
		return s == null || s.isEmpty() ? null : s;
	}
}
