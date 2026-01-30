package com.tripfactory.nomad.api.error;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApiError {

    private String message;
    private int status;
    private LocalDateTime timestamp;
}