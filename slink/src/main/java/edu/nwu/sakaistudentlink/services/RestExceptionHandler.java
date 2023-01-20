package edu.nwu.sakaistudentlink.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(IntegrationException.class)
	public ResponseEntity<Object> integrationExceptionHandler(IntegrationException integrationException) {
		IntegrationError error = null;
		List<IntegrationError> errors = integrationException.getErrors();
		
		if (errors.isEmpty()) {
			error = new IntegrationError();
			error.setErrorMessage("Something went wrong, please contact you administrator.");
		} else {
			error = errors.get(0);
		}
		
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}
}
