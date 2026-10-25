package com.tripfactory.nomad.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PollCreateRequest {

    @NotBlank
    @Size(max = 200)
    private String question;

    /** Two to six choices. */
    @NotNull
    @Size(min = 2, max = 6)
    private List<@NotBlank @Size(max = 80) String> options;
}
