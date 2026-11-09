package com.wittho.shortener.domain.validation;

import java.net.URI;
import java.net.URISyntaxException;

public final class OriginalUrlValidator {

	public static final int MAX_LENGTH = 2048;

	private OriginalUrlValidator() {
	}

	public static boolean isValid(String value) {
		if (value == null || value.length() > MAX_LENGTH) {
			return false;
		}
		try {
			URI uri = new URI(value);
			String scheme = uri.getScheme();
			return uri.isAbsolute()
					&& ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
					&& uri.getHost() != null;
		} catch (URISyntaxException e) {
			return false;
		}
	}

}
