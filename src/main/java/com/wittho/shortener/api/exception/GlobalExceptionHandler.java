package com.wittho.shortener.api.exception;

import com.wittho.shortener.domain.exception.DomainException;
import com.wittho.shortener.domain.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(DomainException.class)
	public ResponseEntity<ProblemDetail> handleDomainException(DomainException ex) {
		ErrorCode errorCode = ex.getErrorCode();

		log.warn("Erro de domínio [{}]: {}", errorCode.getCode(), errorCode.getMessage());

		return buildResponse(errorCode.getHttpStatus(), errorCode.getMessage(), errorCode.getCode());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
		log.error("Erro inesperado", ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno.", null);
	}

	private static ResponseEntity<ProblemDetail> buildResponse(HttpStatus httpStatus, String detail, String code) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(httpStatus, detail);
		problem.setTitle(httpStatus.getReasonPhrase());

		if (code != null) {
			problem.setProperty("code", code);
		}

		return ResponseEntity.status(httpStatus).body(problem);
	}

}
