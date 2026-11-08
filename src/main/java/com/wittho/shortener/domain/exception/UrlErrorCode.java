package com.wittho.shortener.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UrlErrorCode implements ErrorCode {

	INVALID_URL("S001", "A URL original deve ser absoluta, com esquema http ou https.", HttpStatus.BAD_REQUEST)

	;

	private final String code;
	private final String message;
	private final HttpStatus httpStatus;

}
