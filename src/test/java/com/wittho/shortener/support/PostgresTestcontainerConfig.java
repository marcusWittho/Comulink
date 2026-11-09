package com.wittho.shortener.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Sobe um PostgreSQL 17 descartável para os testes que usam banco real.
 * O Spring Boot configura automaticamente datasource e Liquibase a partir do container.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainerConfig {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgres() {
		return new PostgreSQLContainer("postgres:17");
	}

}
