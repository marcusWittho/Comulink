package com.wittho.shortener.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.wittho.shortener.domain.model.Url;
import com.wittho.shortener.support.PostgresTestcontainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestcontainerConfig.class)
class UrlRepositoryIntegrationTest {

	@Autowired
	private UrlRepository repository;

	@Test
	void findsByShortCode() {
		repository.saveAndFlush(new Url(null, "abc1234", "https://example.com", Instant.now()));

		assertThat(repository.findByShortCode("abc1234"))
				.hasValueSatisfying(url -> assertThat(url.getOriginalUrl()).isEqualTo("https://example.com"));
		assertThat(repository.findByShortCode("zzz9999")).isEmpty();
	}

	@Test
	void duplicateShortCodeViolatesNamedUniqueConstraint() {
		repository.saveAndFlush(new Url(null, "dup0001", "https://example.com/a", Instant.now()));

		// O UrlService identifica colisão procurando o nome da constraint na mensagem do banco.
		// Este teste garante que o PostgreSQL real realmente a reporta com esse nome.
		assertThatThrownBy(() -> repository.saveAndFlush(
				new Url(null, "dup0001", "https://example.com/b", Instant.now())))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasStackTraceContaining("uk_urls_short_code");
	}

}
