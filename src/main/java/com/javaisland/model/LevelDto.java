package com.javaisland.model;

public record LevelDto(
    long id,
    String code,
    String title,
    int orderIndex,
    String introText,
    String outroText
) {}