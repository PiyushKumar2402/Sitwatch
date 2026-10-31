package com.sitewatch.dto;

import lombok.Value;

/** One chart point returned by /api/websites/{id}/history. */
@Value
public class Point {
    String time;
    Long ms;
    boolean up;
}
