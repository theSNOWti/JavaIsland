package com.javaisland.model;

public record TaskDto(
    long id,
    long levelId,
    String title,
    String description,
    int orderIndex,
    String validation,
    String starterCode,
    String story,
    String successText,
    String captureJson,
    String captureMode,
    String captureParams
) {}