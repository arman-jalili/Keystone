// Canonical Reference: .pi/architecture/modules/notification-engine.md
// Module: notification-engine
package com.keystone.notification.interfaces.http;

import com.keystone.notification.application.dto.ChannelStatusResponse;
import com.keystone.notification.application.dto.DispatchNotificationRequest;
import com.keystone.notification.application.dto.ErrorResponse;
import com.keystone.notification.application.dto.NotificationResponse;
import com.keystone.notification.application.service.NotificationDispatcher;
import com.keystone.notification.domain.exception.NotificationDeliveryException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Notification Engine bounded context.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET  /api/v1/notifications/channels} — List registered channels and their status</li>
 *   <li>{@code POST /api/v1/notifications/dispatch} — Dispatch a notification event</li>
 *   <li>{@code GET  /api/v1/notifications/{notificationId}} — Get notification status</li>
 *   <li>{@code GET  /api/v1/notifications} — List all notifications (Dashboard)</li>
 * </ul>
 *
 * <p>This controller provides programmatic access to the notification engine.
 * The primary dispatch mechanism is event-driven via Spring's {@code @EventListener}
 * on the service implementation.
 *
 * <p>Base path: {@code /api/v1/notifications}
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationDispatcher notificationDispatcher;

    public NotificationController(NotificationDispatcher notificationDispatcher) {
        this.notificationDispatcher = notificationDispatcher;
    }

    // ---- List notifications endpoint ----

    /**
     * GET /api/v1/notifications
     *
     * <p>Returns all notifications, ordered by creation timestamp descending.
     * Used by the Dashboard Notifications view.
     *
     * @param limit       the maximum number of results to return (default: 50)
     * @param unreadFirst if true, unread notifications appear before read ones (default: true)
     * @return 200 OK with the list of notifications
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<NotificationResponse>> listNotifications(
            @RequestParam(defaultValue = "50") int limit, @RequestParam(defaultValue = "true") boolean unreadFirst) {
        return ResponseEntity.ok(notificationDispatcher.listNotifications(limit, unreadFirst));
    }

    // ---- Channel status endpoints ----

    /**
     * GET /api/v1/notifications/channels
     *
     * <p>Returns the status of all registered notification channels.
     *
     * @return 200 OK with channel status information
     */
    @GetMapping(path = "/channels", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChannelStatusResponse> getChannelStatus() {
        ChannelStatusResponse status = notificationDispatcher.getChannelStatus();
        return ResponseEntity.ok(status);
    }

    // ---- Dispatch endpoints ----

    /**
     * POST /api/v1/notifications/dispatch
     *
     * <p>Dispatches a notification event through the registered channels.
     * The request carries a serialized event payload that each channel
     * processes according to its type.
     *
     * @param request the dispatch request payload
     * @return 200 OK with the list of notification responses
     */
    @PostMapping(
            path = "/dispatch",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> dispatch(@Valid @RequestBody DispatchNotificationRequest request) {
        try {
            List<NotificationResponse> responses = notificationDispatcher.dispatchRequest(request);
            return ResponseEntity.ok(responses);
        } catch (NotificationDeliveryException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErrorResponse("DELIVERY_FAILED", e.getMessage()));
        }
    }

    // ---- Notification status endpoints ----

    /**
     * GET /api/v1/notifications/{notificationId}
     *
     * <p>Retrieves the delivery status of a specific notification.
     *
     * @param notificationId the UUID of the notification record
     * @return 200 OK with the notification status, or 404 if not found
     */
    @GetMapping(path = "/{notificationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NotificationResponse> getNotificationStatus(
            @PathVariable("notificationId") UUID notificationId) {
        return notificationDispatcher
                .getNotificationStatus(notificationId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
