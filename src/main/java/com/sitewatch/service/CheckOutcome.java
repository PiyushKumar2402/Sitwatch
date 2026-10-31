package com.sitewatch.service;

/** Result of one HTTP probe. statusCode is null when no HTTP response was received. */
public record CheckOutcome(boolean up, Integer statusCode, long responseMs, String error) {}
