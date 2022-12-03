package com.atsumeru.web.helper;

import com.atsumeru.web.model.AtsumeruMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RestHelper {

    public static ResponseEntity<AtsumeruMessage> createResponseMessage(String message, HttpStatus status) {
        return new ResponseEntity<>(new AtsumeruMessage(status.value(), message), status);
    }

    public static ResponseEntity<AtsumeruMessage> createResponseMessage(String message, int code, HttpStatus status) {
        return new ResponseEntity<>(new AtsumeruMessage(code, message), status);
    }
}
