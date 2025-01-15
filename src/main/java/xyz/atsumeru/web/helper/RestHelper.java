package xyz.atsumeru.web.helper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import xyz.atsumeru.web.model.AtsumeruMessage;

public class RestHelper {

    public static ResponseEntity<AtsumeruMessage> createResponseMessage(String message, HttpStatus status) {
        return new ResponseEntity<>(new AtsumeruMessage(status.value(), message), status);
    }

    public static ResponseEntity<AtsumeruMessage> createResponseMessage(String message, int code, HttpStatus status) {
        return new ResponseEntity<>(new AtsumeruMessage(code, message), status);
    }
}
