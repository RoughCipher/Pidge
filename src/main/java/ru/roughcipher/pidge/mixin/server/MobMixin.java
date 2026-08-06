package ru.roughcipher.pidge.mixin.server;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.net.command.TextFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.roughcipher.pidge.discord.DiscordChatRelay;
import ru.roughcipher.pidge.telegram.TelegramChatRelay;

@Mixin(value = Mob.class, remap = false)
public abstract class MobMixin {

	@Inject(method = "onDeath", at = @At("TAIL"))
	private void onDeath(Entity entityKilledBy, CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		if (!self.sendsDeathMessage(entityKilledBy)) return;

		boolean isPlayer = self instanceof Player;
		String key = self.getDeathMessageKey(entityKilledBy);
		if (isPlayer
			&& entityKilledBy instanceof Player
			&& "messages.death.player.generic".equals(key)) {
			key = "messages.death.player.killed_by";
		}
		Object[] args;
		if (entityKilledBy != null) {
			args = new String[]{
				TextFormatting.scoped(Entity.getNameFromEntity(self, true)),
				TextFormatting.scoped(Entity.getNameFromEntity(entityKilledBy, true))
			};
		} else {
			args = new String[]{TextFormatting.scoped(Entity.getNameFromEntity(self, true))};
		}
		DiscordChatRelay.INSTANCE.sendDeathMessage(key, args, isPlayer);
		TelegramChatRelay.INSTANCE.sendDeathMessage(key, args, isPlayer);
	}
}
