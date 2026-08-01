package ru.roughcipher.pidge.util;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.Nullable;
import ru.roughcipher.pidge.Pidge;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Pattern;

public final class ProxyUtils {
	private ProxyUtils() {}

	private static final Pattern SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*$");
	private static final Pattern REG_NAME = Pattern.compile(
		"^(?:[A-Za-z0-9._~!$&'()*+,;=-]|%[0-9A-Fa-f]{2})+$"
	);
	private static final Pattern IPV4 = Pattern.compile(
		"^(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$"
	);
	private static final Pattern IPV6 = Pattern.compile(
		"^[0-9A-Fa-f:]+(?:::[0-9A-Fa-f:]*)?(?:\\d{1,3}(?:\\.\\d{1,3}){3})?$"
	);
	private static final Pattern PCT_ENCODED_OK = Pattern.compile("^(?:[^%]|%[0-9A-Fa-f]{2})*$");

	public static final class ParsedProxy {
		public final Proxy proxy;
		@Nullable public final String username;
		@Nullable public final String password;
		public final String scheme;
		public final String host;
		public final int port;

		ParsedProxy(Proxy proxy, @Nullable String username, @Nullable String password,
					String scheme, String host, int port) {
			this.proxy = proxy;
			this.username = username;
			this.password = password;
			this.scheme = scheme;
			this.host = host;
			this.port = port;
		}

		public boolean hasAuth() {
			return username != null && !username.isEmpty();
		}

		public String toSafeString() {
			String auth = hasAuth() ? "****:****@" : "";
			return scheme + "://" + auth + host + ":" + port;
		}
	}

	@Nullable
	public static ParsedProxy parse(@Nullable String proxyUrl) {
		if (proxyUrl == null) return null;
		String trimmed = proxyUrl.trim();
		if (trimmed.isEmpty()) return null;

		int schemeSep = trimmed.indexOf("://");
		if (schemeSep <= 0) {
			throw new IllegalArgumentException(
				"Proxy URL must include a scheme (http://, https://, socks://). Examples: "
					+ "http://user:pass@host:8080 , socks://host:1080");
		}

		String scheme = trimmed.substring(0, schemeSep);
		if (!SCHEME.matcher(scheme).matches()) {
			throw new IllegalArgumentException("Invalid proxy scheme per RFC 3986: \"" + scheme + "\"");
		}
		scheme = scheme.toLowerCase();

		String rest = trimmed.substring(schemeSep + 3);
		int cut = indexOfPathQueryFragment(rest);
		if (cut >= 0) rest = rest.substring(0, cut);

		Proxy.Type type = switch (scheme) {
			case "http", "https" -> Proxy.Type.HTTP;
			case "socks", "socks5", "socks4" -> Proxy.Type.SOCKS;
			default -> throw new IllegalArgumentException(
				"Unsupported proxy scheme \"" + scheme + "\". "
					+ "Use http://, https://, socks://, socks5://, or socks4://");
		};

		String userInfo = null;
		String hostPort = rest;
		int at = rest.lastIndexOf('@');
		if (at >= 0) {
			userInfo = rest.substring(0, at);
			hostPort = rest.substring(at + 1);
		}
		if (hostPort.isEmpty()) {
			throw new IllegalArgumentException("Proxy URL is missing host: " + redact(trimmed));
		}

		String host;
		int port = -1;

		if (hostPort.startsWith("[")) {
			int close = hostPort.indexOf(']');
			if (close < 0) {
				throw new IllegalArgumentException(
					"Proxy URL has invalid IP-literal host (missing ']'): " + redact(trimmed));
			}
			String literal = hostPort.substring(1, close);
			if (!isValidIpLiteral(literal)) {
				throw new IllegalArgumentException(
					"Proxy URL has invalid IP-literal host per RFC 3986: " + literal);
			}
			host = literal;
			String after = hostPort.substring(close + 1);
			if (after.startsWith(":")) {
				port = parsePort(after.substring(1), trimmed);
			} else if (!after.isEmpty()) {
				throw new IllegalArgumentException(
					"Proxy URL has invalid characters after IP-literal: " + redact(trimmed));
			}
		} else {
			int colon = hostPort.lastIndexOf(':');
			if (colon >= 0 && isAllDigits(hostPort.substring(colon + 1))) {
				host = hostPort.substring(0, colon);
				port = parsePort(hostPort.substring(colon + 1), trimmed);
			} else if (colon >= 0) {
				throw new IllegalArgumentException(
					"Proxy URL has invalid port per RFC 3986: " + redact(trimmed));
			} else {
				host = hostPort;
			}
			if (host.isEmpty()) {
				throw new IllegalArgumentException("Proxy URL is missing host: " + redact(trimmed));
			}
			if (!isValidHost(host)) {
				throw new IllegalArgumentException(
					"Proxy URL has invalid host per RFC 3986: \"" + host + "\"");
			}
		}

		if (port < 0) {
			port = type == Proxy.Type.HTTP
				? ("https".equals(scheme) ? 443 : 80)
				: 1080;
		}

		String username = null;
		String password = null;
		if (userInfo != null && !userInfo.isEmpty()) {
			if (!PCT_ENCODED_OK.matcher(userInfo).matches()) {
				throw new IllegalArgumentException(
					"Proxy URL userinfo has invalid percent-encoding per RFC 3986");
			}
			int colon = userInfo.indexOf(':');
			if (colon >= 0) {
				username = decodeOnce(userInfo.substring(0, colon));
				password = decodeOnce(userInfo.substring(colon + 1));
			} else {
				username = decodeOnce(userInfo);
				password = "";
			}
			if (containsControls(username) || containsControls(password)) {
				throw new IllegalArgumentException(
					"Proxy URL credentials must not contain control characters");
			}
		}

		return new ParsedProxy(
			new Proxy(type, new InetSocketAddress(host, port)),
			username, password, scheme, host, port
		);
	}

	public static OkHttpClient buildClient(@Nullable ParsedProxy parsed, String purpose) {
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		applyTo(parsed, builder, purpose);
		return builder.build();
	}

	public static void applyTo(@Nullable ParsedProxy parsed, OkHttpClient.Builder builder, String purpose) {
		if (parsed == null) return;

		builder.proxy(parsed.proxy);

		if (parsed.hasAuth()) {
			final String user = Objects.requireNonNull(parsed.username);
			final String pass = parsed.password != null ? parsed.password : "";
			final String proxyHost = parsed.host;
			final int proxyPort = parsed.port;

			builder.proxyAuthenticator((route, response) -> {
				if (response.request().header("Proxy-Authorization") != null) {
					return null;
				}
				String credential = Credentials.basic(user, pass, StandardCharsets.UTF_8);
				return response.request().newBuilder()
					.header("Proxy-Authorization", credential)
					.build();
			});

			if (parsed.proxy.type() == Proxy.Type.SOCKS) {
				java.net.Authenticator.setDefault(new java.net.Authenticator() {
					@Override
					protected java.net.PasswordAuthentication getPasswordAuthentication() {
						if (getRequestorType() != RequestorType.PROXY) return null;
						if (proxyHost.equalsIgnoreCase(getRequestingHost())
							&& proxyPort == getRequestingPort()) {
							return new java.net.PasswordAuthentication(user, pass.toCharArray());
						}
						return null;
					}
				});
			}
		}

		Pidge.LOGGER.info("Using proxy {} ({})", parsed.toSafeString(), purpose);
	}

	private static boolean isValidHost(String host) {
		return IPV4.matcher(host).matches() || REG_NAME.matcher(host).matches();
	}

	private static boolean isValidIpLiteral(String literal) {
		if (literal.isEmpty()) return false;
		char first = literal.charAt(0);
		if (first == 'v' || first == 'V') {
			return literal.matches("^[vV][0-9A-Fa-f]+\\.[A-Za-z0-9._~!$&'()*+,;=:-]+$");
		}
		if (!IPV6.matcher(literal).matches()) return false;
		long colons = literal.chars().filter(c -> c == ':').count();
		if (colons < 2 && !literal.contains(".") && !literal.contains("::")) return false;
		int idx = literal.indexOf("::");
		return idx < 0 || literal.indexOf("::", idx + 1) < 0;
	}

	private static String decodeOnce(String s) {
		if (s == null || s.isEmpty() || s.indexOf('%') < 0) return s;
		try {
			return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
		} catch (Exception e) {
			return s;
		}
	}

	private static boolean containsControls(String s) {
		if (s == null) return false;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) < 0x20) return true;
		}
		return false;
	}

	private static int parsePort(String s, String fullUrl) {
		if (s == null || s.isEmpty() || !isAllDigits(s)) {
			throw new IllegalArgumentException("Proxy URL has invalid port per RFC 3986: " + redact(fullUrl));
		}
		try {
			int p = Integer.parseInt(s);
			if (p < 1 || p > 65535) {
				throw new IllegalArgumentException("Proxy port out of range (1-65535): " + p);
			}
			return p;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Proxy URL has invalid port per RFC 3986: " + redact(fullUrl));
		}
	}

	private static boolean isAllDigits(String s) {
		if (s == null || s.isEmpty()) return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c < '0' || c > '9') return false;
		}
		return true;
	}

	private static int indexOfPathQueryFragment(String s) {
		int best = -1;
		for (char c : new char[]{'/', '?', '#'}) {
			int idx = s.indexOf(c);
			if (idx >= 0 && (best < 0 || idx < best)) best = idx;
		}
		return best;
	}

	private static String redact(String url) {
		int schemeSep = url.indexOf("://");
		if (schemeSep < 0) return url;
		String rest = url.substring(schemeSep + 3);
		int at = rest.lastIndexOf('@');
		if (at < 0) return url;
		return url.substring(0, schemeSep + 3) + "****:****@" + rest.substring(at + 1);
	}
}
