package com.wittho.shortener.api.controller;

import java.net.URI;

import com.wittho.shortener.domain.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

	private final UrlService urlService;

	public RedirectController(UrlService urlService) {
		this.urlService = urlService;
	}

	@GetMapping("/{shortCode}")
	public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
		return urlService.findByShortCode(shortCode)
				.map(url -> ResponseEntity.status(HttpStatus.FOUND)
						.location(URI.create(url.getOriginalUrl()))
						.<Void>build())
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

}
