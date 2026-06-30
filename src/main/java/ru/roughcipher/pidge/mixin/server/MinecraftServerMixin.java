package ru.roughcipher.pidge.mixin.server;

import ru.roughcipher.pidge.Pidge;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecraftServer.class, remap = false)
public class MinecraftServerMixin {
    @Inject(method = "initiateShutdown", at = @At("HEAD"))
    public void onShutdown(CallbackInfo ci) {
        Pidge.markShutdownSent();
        Pidge.sendShutdownMessages();
    }
}