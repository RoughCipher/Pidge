package ru.roughcipher.pidge.mixin.server;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.lang.I18n;
import net.minecraft.core.net.command.TextFormatting;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.roughcipher.pidge.discord.DiscordChatRelay;
import ru.roughcipher.pidge.telegram.TelegramChatRelay;

@Mixin(value = Mob.class, remap = false)
public abstract class MobMixin {

	@Unique
	private static final boolean HAS_SENDS_DEATH_MESSAGE;

	static {
		boolean has = false;
		try {
			Mob.class.getMethod("sendsDeathMessage", Entity.class);
			has = true;
		} catch (NoSuchMethodException ignored) {
		}
		HAS_SENDS_DEATH_MESSAGE = has;
	}

	//Для BTA 8.0pre1-pre2 (до sendsDeathMessage)
	@SuppressWarnings("all")
	@Redirect(
		method = "onDeath",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/core/world/World;sendGlobalMessageTranslated(Lnet/minecraft/core/net/command/TextFormatting$Base;Ljava/lang/String;[Ljava/lang/String;)V"
		)
	)
	private void redirectDeathMessage(World world, TextFormatting.Base color, String format, String[] args) {
		world.sendGlobalMessage(color + String.format(I18n.getInstance().translateKey(format), (Object[]) args));
		if ((Object) this instanceof Player) {
			DiscordChatRelay.INSTANCE.sendDeathMessage(format, args);
			TelegramChatRelay.INSTANCE.sendDeathMessage(format, args);
		}
	}

	//Для BTA 8.0pre3+ (sendsDeathMessage)
	@Inject(method = "onDeath", at = @At("TAIL"))
	private void onDeath(Entity entityKilledBy, CallbackInfo ci) {
		if (!HAS_SENDS_DEATH_MESSAGE) return;

		Mob self = (Mob) (Object) this;
		if (!(self instanceof Player)) return;

		if (self.sendsDeathMessage(entityKilledBy)) {
			String key = self.getDeathMessageKey(entityKilledBy);
			Object[] args;
			if (entityKilledBy != null) {
				args = new String[]{
					TextFormatting.scoped(Entity.getNameFromEntity(self, true)),
					TextFormatting.scoped(Entity.getNameFromEntity(entityKilledBy, true))
				};
			} else {
				args = new String[]{TextFormatting.scoped(Entity.getNameFromEntity(self, true))};
			}
			DiscordChatRelay.INSTANCE.sendDeathMessage(key, args);
			TelegramChatRelay.INSTANCE.sendDeathMessage(key, args);
		}
	}
}
