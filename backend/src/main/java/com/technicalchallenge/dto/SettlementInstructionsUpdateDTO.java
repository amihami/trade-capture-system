package com.technicalchallenge.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class SettlementInstructionsUpdateDTO {

    private String performedBy;

    @Size(min = 10, max = 500, message = "Settlement instructions must be between 10 and 500 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9 .,:/()\\-\\n]+$",
        message = "Settlement instructions contain invalid characters"
    )
    private String settlementInstructions;
}
