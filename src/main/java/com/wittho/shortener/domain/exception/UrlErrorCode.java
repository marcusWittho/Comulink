package com.wittho.shortener.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UrlErrorCode implements ErrorCode {

	INVALID_URL("S001", "A URL original deve ser absoluta, com esquema http ou https.", HttpStatus.BAD_REQUEST),
	SHORT_CODE_GENERATION_FAILED("S002", "Não foi possível gerar um código curto único.", HttpStatus.INTERNAL_SERVER_ERROR)

	;

	private final String code;
	private final String message;
	private final HttpStatus httpStatus;

}
