package com.wittho.shortener.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wittho.shortener.domain.exception.DomainException;
import com.wittho.shortener.domain.exception.UrlErrorCode;
import com.wittho.shortener.domain.model.Url;
import com.wittho.shortener.domain.repository.UrlRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class UrlServiceTest {

	private static final String ORIGINAL = "https://example.com/pagina";

	private static DataIntegrityViolationException shortCodeCollision() {
		return new DataIntegrityViolationException("constraint",
				new RuntimeException("duplicate key value violates unique constraint \"uk_urls_short_code\""));
	}

	@Test
	void createSavesOnFirstAttemptWhenNoCollision() {
		UrlRepository repository = mock(UrlRepository.class);
		when(repository.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));
		UrlService service = new UrlService(repository);

		Url url = service.create(ORIGINAL);

		assertThat(url.getOriginalUrl()).isEqualTo(ORIGINAL);
		assertThat(url.getShortCode()).hasSize(7);
		verify(repository, times(1)).save(any(Url.class));
	}

	@Test
	void createRetriesAfterShortCodeCollision() {
		UrlRepository repository = mock(UrlRepository.class);
		when(repository.save(any(Url.class)))
				.thenThrow(shortCodeCollision())
				.thenThrow(shortCodeCollision())
				.thenAnswer(inv -> inv.getArgument(0));
		UrlService service = new UrlService(repository);

		Url url = service.create(ORIGINAL);

		assertThat(url.getOriginalUrl()).isEqualTo(ORIGINAL);
		verify(repository, times(3)).save(any(Url.class));
	}

	@Test
	void createFailsAfterMaxCollisionAttempts() {
		UrlRepository repository = mock(UrlRepository.class);
		when(repository.save(any(Url.class))).thenThrow(shortCodeCollision());
		UrlService service = new UrlService(repository);

		assertThatThrownBy(() -> service.create(ORIGINAL))
				.isInstanceOf(DomainException.class)
				.satisfies(ex -> assertThat(((DomainException) ex).getErrorCode())
						.isEqualTo(UrlErrorCode.SHORT_CODE_GENERATION_FAILED));
		verify(repository, times(5)).save(any(Url.class));
	}

	@Test
	void createDoesNotRetryOnUnrelatedIntegrityViolation() {
		UrlRepository repository = mock(UrlRepository.class);
		DataIntegrityViolationException other = new DataIntegrityViolationException("outra constraint",
				new RuntimeException("null value in column \"original_url\" violates not-null constraint"));
		when(repository.save(any(Url.class))).thenThrow(other);
		UrlService service = new UrlService(repository);

		assertThatThrownBy(() -> service.create(ORIGINAL)).isSameAs(other);
		verify(repository, times(1)).save(any(Url.class));
	}

}
