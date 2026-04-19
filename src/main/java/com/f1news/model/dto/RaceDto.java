package com.f1news.model.dto;

public record RaceDto(
        String season,
        String round,
        String raceName,
        String circuitName,
        String country,
        String locality,
        String date,
        String time,
        boolean isCompleted
) {}
