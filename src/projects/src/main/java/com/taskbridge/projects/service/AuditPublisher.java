package com.taskbridge.projects.service;

import com.taskbridge.projects.model.Project;

import java.util.Map;

/**
 * Simple abstraction for publishing audit events / notifications.
 * Implementation could write to outbox table, call Notification Service, or publish to a broker.
 */
public interface AuditPublisher {

    /**
     * Publish an audit entry for a project change.
     * Implementations must be idempotent (use eventId or DB uniqueness).
     *
     * @param tenantId tenant id
     * @param projectId project id
     * @param eventType human-readable event type
     * @param actorId id of the actor
     * @param changes map describing field-level changes
     */
    void publishAudit(String tenantId, Long projectId, String eventType, String actorId, Map<String, Object> changes);
}
