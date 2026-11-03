package com.wittho.shortener.domain.repository;

import java.util.Optional;

import com.wittho.shortener.domain.model.Url;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UrlRepository extends MongoRepository<Url, String> {

	Optional<Url> findByShortCode(String shortCode);

}
