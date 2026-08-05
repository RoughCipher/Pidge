package ru.roughcipher.pidge.mixin.server;

import ru.roughcipher.pidge.discord.DiscordChatRelay;
import ru.roughcipher.pidge.telegram.TelegramChatRelay;
import ru.roughcipher.pidge.util.PlayerUtils;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.world.World;
import net.minecraft.server.entity.player.PlayerServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = World.class, remap = false)
public class WorldMixin {
	@Inject(method = "wakeUpAllPlayers", at = @At("HEAD"))
	private void onNightSkipped(CallbackInfo ci) {
		World self = (World) (Object) this;
		List<String> names = new ArrayList<>();
		for (Player p : self.players) {
			if (!p.isPlayerSleeping()) continue;
			if (p instanceof PlayerServer) {
				names.add(PlayerUtils.getDisplayName((PlayerServer) p));
			} else {
				names.add(p.username);
			}
		}
		if (names.isEmpty()) return;
		String joined = String.join(", ", names);
		DiscordChatRelay.INSTANCE.sendServerSleepMessage(joined);
		TelegramChatRelay.INSTANCE.sendServerSleepMessage(joined);
	}
}
