package com.f1news.model.dto;

import java.util.List;

public record RacePredictionDto(
        String raceName,
        String circuitName,
        String country,
        String date,
        List<PredictedFinisher> predictions,
        String methodology
) {
    public record PredictedFinisher(
            int predictedPosition,
            String driverCode,
            String driverName,
            String team,
            double score,
            String reasoning
    ) {}
}
