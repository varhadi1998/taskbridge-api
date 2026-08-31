package com.taskbridge.projects.model;

/**
 * Strongly-typed project statuses to avoid string typos and allow efficient mapping.
 */
public enum ProjectStatus {
    NEW,
    IN_PROGRESS,
    ON_HOLD,
    COMPLETED,
    CANCELLED,
    CLOSED
}
