package com.tripfactory.nomad.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PushSubscribeRequest {

    @NotBlank
    @Size(max = 700)
    private String endpoint;

    /** Only needed when subscribing; ignored when unsubscribing. */
    @Size(max = 200)
    private String p256dh;

    @Size(max = 100)
    private String auth;
}
