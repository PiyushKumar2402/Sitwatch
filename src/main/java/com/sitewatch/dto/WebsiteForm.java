package com.sitewatch.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WebsiteForm {
    @Size(max = 100, message = "Name can be at most 100 characters.")
    private String name;

    @NotBlank(message = "Enter a website URL.")
    @Size(max = 2000, message = "URL is too long.")
    private String url;

    @Min(value = 1, message = "Enter a check interval in seconds.")
    @Max(value = 86400, message = "Interval can be at most 24 hours.")
    private Integer intervalSeconds = 60;
}
