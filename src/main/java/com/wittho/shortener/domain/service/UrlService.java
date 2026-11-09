package com.wittho.shortener.domain.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

import com.wittho.shortener.domain.exception.DomainException;
import com.wittho.shortener.domain.exception.UrlErrorCode;
import com.wittho.shortener.domain.model.Url;
import com.wittho.shortener.domain.repository.UrlRepository;
import com.wittho.shortener.domain.validation.OriginalUrlValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UrlService {

	private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int CODE_LENGTH = 7;
	private static final int MAX_SHORT_CODE_ATTEMPTS = 5;
	private static final String SHORT_CODE_CONSTRAINT = "uk_urls_short_code";

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
		for (int attempt = 1; attempt <= MAX_SHORT_CODE_ATTEMPTS; attempt++) {
			Url url = new Url(null, generateShortCode(), normalized, Instant.now());

			try {
				return urlRepository.save(url);
			} catch (DataIntegrityViolationException ex) {
				if (!isShortCodeCollision(ex)) {
					throw ex;
				}

				log.warn("Colisão de short_code na tentativa {} de {}", attempt, MAX_SHORT_CODE_ATTEMPTS);
			}
		}

		throw new DomainException(UrlErrorCode.SHORT_CODE_GENERATION_FAILED);
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

	private static boolean isShortCodeCollision(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {

			String message = cause.getMessage();
			if (message != null && message.contains(SHORT_CODE_CONSTRAINT)) {
				return true;
			}
		}

		return false;
	}

}
