package ru.roughcipher.pidge.mixin.server;

import ru.roughcipher.pidge.discord.DiscordChatRelay;
import ru.roughcipher.pidge.telegram.TelegramChatRelay;
import ru.roughcipher.pidge.config.PidgeConfig;
import net.minecraft.core.net.ChatEmotes;
import net.minecraft.server.entity.player.PlayerServer;
import net.minecraft.server.net.handler.PacketHandlerServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PacketHandlerServer.class, remap = false)
public class PacketHandlerServerMixin {
	@Shadow private PlayerServer playerEntity;

	@Redirect(
		method = "handleMessage",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/core/net/ChatEmotes;process(Ljava/lang/String;)Ljava/lang/String;"
		)
	)
	String redirectChatHandle(String s) {
		String message = ChatEmotes.process(s);

		if (!PidgeConfig.discord_enable) {
			return message;
		}

		String username = playerEntity.username;

		DiscordChatRelay.sendToDiscord(username, message);
		TelegramChatRelay.sendToTelegram(username, message);

		return message;
	}

	@Inject(
		method = "handleErrorMessage",
		at = @At("HEAD")
	)
	void sendLeaveMessage(String s, Object[] aobj, CallbackInfo ci) {
		String username = playerEntity.username;
		DiscordChatRelay.sendJoinLeaveMessage(username, false);
		TelegramChatRelay.sendJoinLeaveMessage(username, false);
	}
}
