package com.wittho.shortener.domain.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

import com.wittho.shortener.domain.exception.DomainException;
import com.wittho.shortener.domain.exception.UrlErrorCode;
import com.wittho.shortener.domain.model.Url;
import com.wittho.shortener.domain.repository.UrlRepository;
import com.wittho.shortener.domain.validation.OriginalUrlValidator;
import org.springframework.stereotype.Service;

@Service
public class UrlService {

	private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int CODE_LENGTH = 7;

	private final UrlRepository urlRepository;
	private final SecureRandom random = new SecureRandom();

	public UrlService(UrlRepository urlRepository) {
		this.urlRepository = urlRepository;
	}

	public Url create(String originalUrl) {
		String normalized = originalUrl == null ? null : originalUrl.trim();
		if (!OriginalUrlValidator.isValid(normalized)) {
			throw new DomainException(UrlErrorCode.INVALID_URL);
		}
		Url url = new Url(null, generateShortCode(), normalized, Instant.now());
		return urlRepository.save(url);
	}

	public Optional<Url> findByShortCode(String shortCode) {
		return urlRepository.findByShortCode(shortCode);
	}

	private String generateShortCode() {
		StringBuilder code = new StringBuilder(CODE_LENGTH);
		for (int i = 0; i < CODE_LENGTH; i++) {
			code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
		}
		return code.toString();
	}

}
