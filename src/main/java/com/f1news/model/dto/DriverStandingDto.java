package com.f1news.model.dto;

public record DriverStandingDto(
        String position,
        String driverCode,
        String driverName,
        String team,
        String points,
        String wins,
        String nationality
) {}
