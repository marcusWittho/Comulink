package com.wittho.shortener.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import com.wittho.shortener.domain.exception.DomainException;
import com.wittho.shortener.domain.exception.UrlErrorCode;
import com.wittho.shortener.domain.model.Url;
import com.wittho.shortener.domain.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {UrlController.class, RedirectController.class},
		properties = "shortener.rate-limit.create-url.capacity=1000")
class UrlControllerTest {

	private static final String ORIGINAL = "https://example.com/pagina";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UrlService urlService;

	@Test
	void createReturnsCreatedWithBody() throws Exception {
		when(urlService.create(ORIGINAL)).thenReturn(new Url(1L, "abc1234", ORIGINAL, Instant.now()));

		mockMvc.perform(post("/api/urls")
						.contentType("application/json")
						.content("{\"originalUrl\": \"" + ORIGINAL + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.shortCode").value("abc1234"))
				.andExpect(jsonPath("$.originalUrl").value(ORIGINAL));
	}

	@Test
	void createMapsDomainExceptionToProblemDetail() throws Exception {
		when(urlService.create(any())).thenThrow(new DomainException(UrlErrorCode.INVALID_URL));

		mockMvc.perform(post("/api/urls")
						.contentType("application/json")
						.content("{\"originalUrl\": \"nao-e-url\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("S001"))
				.andExpect(jsonPath("$.title").value("Bad Request"));
	}

	@Test
	void createMapsCodeGenerationFailureToInternalError() throws Exception {
		when(urlService.create(any())).thenThrow(new DomainException(UrlErrorCode.SHORT_CODE_GENERATION_FAILED));

		mockMvc.perform(post("/api/urls")
						.contentType("application/json")
						.content("{\"originalUrl\": \"" + ORIGINAL + "\"}"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("S002"));
	}

	@Test
	void createMapsUnexpectedExceptionToGenericInternalError() throws Exception {
		when(urlService.create(any())).thenThrow(new RuntimeException("detalhe interno sensível"));

		mockMvc.perform(post("/api/urls")
						.contentType("application/json")
						.content("{\"originalUrl\": \"" + ORIGINAL + "\"}"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.detail").value("Erro interno."));
	}

	@Test
	void findReturnsOkWhenPresent() throws Exception {
		when(urlService.findByShortCode("abc1234"))
				.thenReturn(Optional.of(new Url(1L, "abc1234", ORIGINAL, Instant.now())));

		mockMvc.perform(get("/api/urls/{shortCode}", "abc1234"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.originalUrl").value(ORIGINAL));
	}

	@Test
	void findReturnsNotFoundWhenAbsent() throws Exception {
		when(urlService.findByShortCode("naoexis")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/urls/{shortCode}", "naoexis"))
				.andExpect(status().isNotFound());
	}

	@Test
	void redirectSendsFoundWithLocation() throws Exception {
		when(urlService.findByShortCode("abc1234"))
				.thenReturn(Optional.of(new Url(1L, "abc1234", ORIGINAL, Instant.now())));

		mockMvc.perform(get("/{shortCode}", "abc1234"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", ORIGINAL));
	}

	@Test
	void redirectReturnsNotFoundWhenAbsent() throws Exception {
		when(urlService.findByShortCode("naoexis")).thenReturn(Optional.empty());

		mockMvc.perform(get("/{shortCode}", "naoexis"))
				.andExpect(status().isNotFound());
	}

}
