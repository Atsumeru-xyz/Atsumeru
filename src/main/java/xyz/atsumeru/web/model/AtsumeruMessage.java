package xyz.atsumeru.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AtsumeruMessage {
    private int code;
    private String message;
}