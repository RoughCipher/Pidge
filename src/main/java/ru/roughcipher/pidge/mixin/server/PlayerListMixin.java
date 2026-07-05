package ru.roughcipher.pidge.mixin.server;

import ru.roughcipher.pidge.discord.DiscordChatRelay;
import ru.roughcipher.pidge.telegram.TelegramChatRelay;
import net.minecraft.server.entity.player.PlayerServer;
import net.minecraft.server.net.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerList.class, remap = false)
public class PlayerListMixin {
	@Inject(
		method = "playerLoggedIn",
		at = @At("HEAD")
	)
	public void onPlayerLoggedIn(PlayerServer player, CallbackInfo ci) {
		String username = player.username;
		DiscordChatRelay.INSTANCE.sendJoinLeaveMessage(username, true);
		TelegramChatRelay.INSTANCE.sendJoinLeaveMessage(username, true);
	}
}
