package com.wittho.shortener.domain.repository;

import java.util.Optional;

import com.wittho.shortener.domain.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlRepository extends JpaRepository<Url, Long> {

	Optional<Url> findByShortCode(String shortCode);

}
