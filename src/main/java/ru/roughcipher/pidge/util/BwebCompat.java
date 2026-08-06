package ru.roughcipher.pidge.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.util.helper.UUIDHelper;
import org.jetbrains.annotations.Nullable;
import ru.roughcipher.pidge.Pidge;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class BwebCompat {
	private BwebCompat() {}

	public static final String MOD_ID = "bweb";

	public static boolean isLoaded() {
		return FabricLoader.getInstance().isModLoaded(MOD_ID);
	}

	public static boolean isValidBackend(@Nullable String backend) {
		if (backend == null) return false;
		String s = backend.trim().toLowerCase(Locale.ROOT);
		return "ely".equals(s) || "mojang".equals(s);
	}

	public static void resolveUuid(
		String name,
		@Nullable String backend,
		UUIDHelper.UUIDFunction onSuccess,
		UUIDHelper.StringFunction onFail
	) {
		if (backend == null || backend.isEmpty()) {
			UUIDHelper.runConversionAction(name, onSuccess, onFail);
			return;
		}
		if (!isLoaded()) {
			Pidge.LOGGER.warn("Backend '{}' requested but mod '{}' is not loaded", backend, MOD_ID);
			onFail.run(name);
			return;
		}
		if (!isValidBackend(backend)) {
			onFail.run(name);
			return;
		}
		final String backendKey = backend.trim().toLowerCase(Locale.ROOT);
		CompletableFuture.runAsync(() -> {
			try {
				Class<?> authSourceClass = Class.forName("ru.roughcipher.better_with_elyby.auth.AuthSource");
				@SuppressWarnings({"unchecked", "rawtypes"})
				Object authSource = Enum.valueOf((Class) authSourceClass, backendKey.toUpperCase(Locale.ROOT));

				Class<?> resolver = Class.forName("ru.roughcipher.better_with_elyby.auth.UuidResolver");
				Method resolve = resolver.getMethod("resolve", String.class, authSourceClass);
				String uuidStr = (String) resolve.invoke(null, name, authSource);
				if (uuidStr == null || uuidStr.isEmpty()) {
					onFail.run(name);
					return;
				}
				Method formatUuid = resolver.getMethod("formatUuid", String.class);
				String formatted = (String) formatUuid.invoke(null, uuidStr);
				onSuccess.run(UUID.fromString(formatted));
			} catch (Exception e) {
				Pidge.LOGGER.error("bweb UUID resolve failed for {} ({})", name, backendKey, e);
				onFail.run(name);
			}
		});
	}
}
