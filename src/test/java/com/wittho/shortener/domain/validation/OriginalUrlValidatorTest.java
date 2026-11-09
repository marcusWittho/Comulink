package com.wittho.shortener.domain.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class OriginalUrlValidatorTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"http://example.com",
			"https://example.com/caminho?q=1#frag",
			"HTTPS://EXAMPLE.COM",
			"https://localhost:8080/x"
	})
	void acceptsAbsoluteHttpAndHttpsUrls(String value) {
		assertThat(OriginalUrlValidator.isValid(value)).isTrue();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
			"   ",
			"example.com",
			"/relativo/caminho",
			"ftp://example.com/arquivo",
			"javascript:alert(1)",
			"mailto:alguem@example.com",
			"https://",
			"http://exa mple.com",
			"not a url"
	})
	void rejectsBlankRelativeNonHttpAndMalformedUrls(String value) {
		assertThat(OriginalUrlValidator.isValid(value)).isFalse();
	}

	@Test
	void rejectsUrlLongerThanMaxLength() {
		String tooLong = "https://example.com/" + "a".repeat(OriginalUrlValidator.MAX_LENGTH);

		assertThat(tooLong.length()).isGreaterThan(OriginalUrlValidator.MAX_LENGTH);
		assertThat(OriginalUrlValidator.isValid(tooLong)).isFalse();
	}

	@Test
	void acceptsUrlExactlyAtMaxLength() {
		String prefix = "https://example.com/";
		String exact = prefix + "a".repeat(OriginalUrlValidator.MAX_LENGTH - prefix.length());

		assertThat(exact).hasSize(OriginalUrlValidator.MAX_LENGTH);
		assertThat(OriginalUrlValidator.isValid(exact)).isTrue();
	}

}
