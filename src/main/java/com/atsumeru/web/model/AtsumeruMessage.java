package com.atsumeru.web.model;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AtsumeruMessage {
    @Expose
    private int code;

    @Expose
    private String message;
}