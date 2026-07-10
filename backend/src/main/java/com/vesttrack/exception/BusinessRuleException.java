package com.vesttrack.exception;

import org.springframework.http.HttpStatus;

/** Naruszenie reguly biznesowej, np. przekroczenie limitu wplat IKE, sprzedaz wiekszej liczby jednostek niz posiadane itp. */
public class BusinessRuleException extends ApiException {
    public BusinessRuleException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
