package com.wittho.shortener;

import com.wittho.shortener.support.PostgresTestcontainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestcontainerConfig.class)
class ShortenerApplicationTests {

	@Test
	void contextLoads() {
	}

}
