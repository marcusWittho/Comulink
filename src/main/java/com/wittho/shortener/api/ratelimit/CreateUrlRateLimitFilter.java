package com.wittho.shortener.api.ratelimit;

import java.io.IOException;
import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CreateUrlRateLimitFilter extends OncePerRequestFilter {

	private static final String TARGET_METHOD = "POST";
	private static final String TARGET_PATH = "/api/urls";
	private static final long MAX_TRACKED_CLIENTS = 100_000;
	private static final Duration CLIENT_IDLE_EXPIRATION = Duration.ofHours(1);

	private final long capacity;
	private final Duration refillPeriod;
	private final Cache<String, Bucket> buckets;

	public CreateUrlRateLimitFilter(
			@Value("${shortener.rate-limit.create-url.capacity:10}") long capacity,
			@Value("${shortener.rate-limit.create-url.refill-period:1m}") Duration refillPeriod) {
		this.capacity = capacity;
		this.refillPeriod = refillPeriod;
		this.buckets = Caffeine.newBuilder()
				.maximumSize(MAX_TRACKED_CLIENTS)
				.expireAfterAccess(CLIENT_IDLE_EXPIRATION)
				.build();
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !(TARGET_METHOD.equals(request.getMethod()) && TARGET_PATH.equals(request.getRequestURI()));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Bucket bucket = buckets.get(request.getRemoteAddr(), key -> newBucket());
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

		if (probe.isConsumed()) {
			chain.doFilter(request, response);
			return;
		}

		long retryAfterSeconds = Math.max(1, Math.ceilDiv(probe.getNanosToWaitForRefill(), 1_000_000_000L));
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
		response.setContentType("application/problem+json");
		response.getWriter().write("{\"title\":\"Too Many Requests\",\"status\":429,"
				+ "\"detail\":\"Limite de requisições excedido. Tente novamente em breve.\"}");
	}

	private Bucket newBucket() {
		Bandwidth limit = Bandwidth.builder()
				.capacity(capacity)
				.refillGreedy(capacity, refillPeriod)
				.build();
		return Bucket.builder().addLimit(limit).build();
	}

}
