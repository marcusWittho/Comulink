package com.wittho.shortener.api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CreateUrlRateLimitFilterTest {

	private static MockHttpServletRequest post(String remoteAddr) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/urls");
		request.setRemoteAddr(remoteAddr);
		return request;
	}

	private static MockHttpServletResponse doFilter(CreateUrlRateLimitFilter filter, MockHttpServletRequest request)
			throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		return response;
	}

	@Test
	void allowsRequestsUpToCapacityThenRejects() throws Exception {
		CreateUrlRateLimitFilter filter = new CreateUrlRateLimitFilter(2, Duration.ofHours(1));

		assertThat(doFilter(filter, post("10.0.0.1")).getStatus()).isEqualTo(200);
		assertThat(doFilter(filter, post("10.0.0.1")).getStatus()).isEqualTo(200);

		MockHttpServletResponse rejected = doFilter(filter, post("10.0.0.1"));
		assertThat(rejected.getStatus()).isEqualTo(429);
		assertThat(Long.parseLong(rejected.getHeader("Retry-After"))).isGreaterThanOrEqualTo(1);
	}

	@Test
	void limitsEachClientIndependently() throws Exception {
		CreateUrlRateLimitFilter filter = new CreateUrlRateLimitFilter(1, Duration.ofHours(1));

		assertThat(doFilter(filter, post("10.0.0.1")).getStatus()).isEqualTo(200);
		assertThat(doFilter(filter, post("10.0.0.1")).getStatus()).isEqualTo(429);
		assertThat(doFilter(filter, post("10.0.0.2")).getStatus()).isEqualTo(200);
	}

	@Test
	void doesNotLimitOtherRequests() throws Exception {
		CreateUrlRateLimitFilter filter = new CreateUrlRateLimitFilter(1, Duration.ofHours(1));

		for (int i = 0; i < 5; i++) {
			MockHttpServletRequest get = new MockHttpServletRequest("GET", "/abc1234");
			get.setRemoteAddr("10.0.0.1");
			assertThat(doFilter(filter, get).getStatus()).isEqualTo(200);
		}
	}

}
