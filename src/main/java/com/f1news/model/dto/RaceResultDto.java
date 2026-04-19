package com.f1news.model.dto;

import java.util.List;

public record RaceResultDto(
        String raceName,
        String circuitName,
        String country,
        String date,
        String round,
        List<FinisherDto> results
) {
    public record FinisherDto(
            String position,
            String driverCode,
            String driverName,
            String team,
            String points,
            String time,
            String status
    ) {}
}
