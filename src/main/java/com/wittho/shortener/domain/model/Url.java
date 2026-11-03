package com.wittho.shortener.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("urls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Url {

	@Id
	private String id;

	@Indexed(unique = true)
	private String shortCode;

	private String originalUrl;

	private Instant createdAt;

}
