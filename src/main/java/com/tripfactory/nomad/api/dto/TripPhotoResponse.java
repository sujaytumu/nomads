package com.tripfactory.nomad.api.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TripPhotoResponse {

    private Long id;
    private String caption;
    private String dataUrl;
    private LocalDateTime createdAt;
}
