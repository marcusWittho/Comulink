package com.wittho.shortener.api.controller;

import com.wittho.shortener.domain.model.Url;
import com.wittho.shortener.domain.service.UrlService;
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

	private final UrlService urlService;

	public UrlController(UrlService urlService) {
		this.urlService = urlService;
	}

	@PostMapping
	public ResponseEntity<Url> create(@RequestBody CreateUrlRequest request) {
		Url url = urlService.create(request.originalUrl());
		return ResponseEntity.status(HttpStatus.CREATED).body(url);
	}

	@GetMapping("/{shortCode}")
	public ResponseEntity<Url> findByShortCode(@PathVariable String shortCode) {
		return urlService.findByShortCode(shortCode)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	public record CreateUrlRequest(String originalUrl) {
	}

}
