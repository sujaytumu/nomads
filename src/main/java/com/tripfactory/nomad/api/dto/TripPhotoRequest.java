package com.tripfactory.nomad.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TripPhotoRequest {

    /** A small image as a data URL, e.g. data:image/jpeg;base64,... */
    @NotBlank
    @Size(max = 700_000)
    private String dataUrl;

    @Size(max = 120)
    private String caption;
}
