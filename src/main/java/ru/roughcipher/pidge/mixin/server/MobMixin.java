package ru.roughcipher.pidge.mixin.server;

import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.lang.I18n;
import net.minecraft.core.net.command.TextFormatting;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.roughcipher.pidge.discord.DiscordChatRelay;
import ru.roughcipher.pidge.telegram.TelegramChatRelay;

@Mixin(value = Mob.class, remap = false)
public abstract class MobMixin {

	@Redirect(
		method = "onDeath",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/core/world/World;sendGlobalMessageTranslated(Lnet/minecraft/core/net/command/TextFormatting$Base;Ljava/lang/String;[Ljava/lang/String;)V"
		)
	)
	private void redirectDeathMessage(World world, TextFormatting.Base color, String translationKey, String[] args) {
		world.sendGlobalMessage(color + String.format(I18n.getInstance().translateKey(translationKey), (Object[]) args));
		if ((Object) this instanceof Player) {
			DiscordChatRelay.sendDeathMessage(translationKey, args);
			TelegramChatRelay.sendDeathMessage(translationKey, args);
		}
	}
}
