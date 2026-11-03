package com.wittho.shortener.api.controller;

import java.security.SecureRandom;
import java.time.Instant;

import com.wittho.shortener.domain.repository.UrlRepository;
import com.wittho.shortener.domain.model.Url;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/urls")
public class UrlController {

	private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int CODE_LENGTH = 7;

	private final UrlRepository urlRepository;
	private final SecureRandom random = new SecureRandom();

	public UrlController(UrlRepository urlRepository) {
		this.urlRepository = urlRepository;
	}

	@PostMapping
	public ResponseEntity<Url> create(@RequestBody CreateUrlRequest request) {
		if (request.originalUrl() == null || request.originalUrl().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		Url url = new Url(null, generateShortCode(), request.originalUrl(), Instant.now());
		return ResponseEntity.status(HttpStatus.CREATED).body(urlRepository.save(url));
	}

	@GetMapping("/{shortCode}")
	public ResponseEntity<Url> findByShortCode(@PathVariable String shortCode) {
		return urlRepository.findByShortCode(shortCode)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	private String generateShortCode() {
		StringBuilder code = new StringBuilder(CODE_LENGTH);
		for (int i = 0; i < CODE_LENGTH; i++) {
			code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
		}
		return code.toString();
	}

	public record CreateUrlRequest(String originalUrl) {
	}

}
