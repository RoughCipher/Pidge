package ru.roughcipher.pidge.mixin.server;

import net.minecraft.core.net.packet.Packet;
import net.minecraft.core.net.packet.PacketMessage;
import net.minecraft.server.net.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.roughcipher.pidge.discord.DiscordChatRelay;
import ru.roughcipher.pidge.telegram.TelegramChatRelay;
import ru.roughcipher.pidge.util.MessageUtils;

@Mixin(value = PlayerList.class, remap = false)
public class PlayerListSayMeMixin {

	@Inject(method = "sendPacketToAllPlayers", at = @At("HEAD"))
	private void onSendPacketToAllPlayers(Packet packet, CallbackInfo ci) {
		if (!(packet instanceof PacketMessage msg)) return;
		if (msg.encrypted) return;

		String raw = msg.message;
		String clean = MessageUtils.stripColorCodes(raw);

		if (clean.startsWith("* ")) {
			String withoutStar = clean.substring(2);
			int firstSpace = withoutStar.indexOf(' ');
			if (firstSpace > 0) {
				String playerName = withoutStar.substring(0, firstSpace);
				String message = withoutStar.substring(firstSpace + 1);
				DiscordChatRelay.INSTANCE.sendMeMessage(playerName, message);
				TelegramChatRelay.INSTANCE.sendMeMessage(playerName, message);
			}
			return;
		}

		if (clean.startsWith("[") && clean.contains("]")) {
			int bracketIndex = clean.indexOf(']');
			if (bracketIndex > 0 && bracketIndex + 1 < clean.length()) {
				String senderName = clean.substring(1, bracketIndex).trim();
				String textPart = clean.substring(bracketIndex + 1).trim();
				DiscordChatRelay.INSTANCE.sendSayMessage(senderName, textPart);
				TelegramChatRelay.INSTANCE.sendSayMessage(senderName, textPart);
			}
		}
	}
}
