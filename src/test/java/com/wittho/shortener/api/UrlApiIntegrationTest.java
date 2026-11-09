package com.wittho.shortener.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.wittho.shortener.domain.repository.UrlRepository;
import com.wittho.shortener.support.PostgresTestcontainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "shortener.rate-limit.create-url.capacity=1000")
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfig.class)
class UrlApiIntegrationTest {

	private static final String ORIGINAL = "https://example.com/pagina?q=1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UrlRepository urlRepository;

	@BeforeEach
	void cleanDatabase() {
		urlRepository.deleteAll();
	}

	@Test
	void createdUrlCanBeFetchedAndRedirected() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/urls")
						.contentType("application/json")
						.content("{\"originalUrl\": \"" + ORIGINAL + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.shortCode").isString())
				.andExpect(jsonPath("$.originalUrl").value(ORIGINAL))
				.andReturn();

		String shortCode = JsonPath.read(created.getResponse().getContentAsString(), "$.shortCode");
		assertThat(shortCode).hasSize(7);
		assertThat(urlRepository.findByShortCode(shortCode)).hasValueSatisfying(
				url -> assertThat(url.getOriginalUrl()).isEqualTo(ORIGINAL));

		mockMvc.perform(get("/api/urls/{shortCode}", shortCode))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.originalUrl").value(ORIGINAL));

		mockMvc.perform(get("/{shortCode}", shortCode))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", ORIGINAL));
	}

	@Test
	void trimsSurroundingWhitespaceBeforeStoring() throws Exception {
		mockMvc.perform(post("/api/urls")
						.contentType("application/json")
						.content("{\"originalUrl\": \"  " + ORIGINAL + "  \"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.originalUrl").value(ORIGINAL));
	}

	@Test
	void rejectsInvalidUrlWithProblemDetail() throws Exception {
		mockMvc.perform(post("/api/urls")
						.contentType("application/json")
						.content("{\"originalUrl\": \"ftp://example.com/arquivo\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("S001"))
				.andExpect(jsonPath("$.status").value(400));

		assertThat(urlRepository.count()).isZero();
	}

	@Test
	void rejectsBlankUrl() throws Exception {
		mockMvc.perform(post("/api/urls")
						.contentType("application/json")
						.content("{\"originalUrl\": \"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("S001"));
	}

	@Test
	void rejectsRequestWithoutBody() throws Exception {
		mockMvc.perform(post("/api/urls")
						.contentType("application/json"))
				.andExpect(status().isBadRequest());

		assertThat(urlRepository.count()).isZero();
	}

	@Test
	void rejectsEmptyJsonObjectAsMissingUrl() throws Exception {
		mockMvc.perform(post("/api/urls")
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("S001"));

		assertThat(urlRepository.count()).isZero();
	}

	@Test
	void unknownShortCodeReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/urls/{shortCode}", "naoexis"))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/{shortCode}", "naoexis"))
				.andExpect(status().isNotFound());
	}

}
