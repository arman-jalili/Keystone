// Package event defines infrastructure-level event publisher contracts.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#components
//
// The event publisher interface abstracts over transport mechanisms
// (stdout, structured logs, HTTP callbacks, message queues). Domain
// events (defined in internal/domain/event/) are dispatched through
// this interface.
package event

import "github.com/arman-jalili/keystone-cli/internal/domain/event"

// Publisher dispatches domain events to interested consumers.
//
// The default implementation writes structured JSON events to stdout,
// enabling CI pipeline consumers to tail and react. Alternative
// implementations could publish to a message queue or HTTP webhook.
type Publisher interface {
	// Publish dispatches a single domain event.
	// Implementations MUST handle serialisation and transport.
	// Returns an error if the event cannot be published.
	Publish(e *event.Event) error

	// PublishBatch dispatches multiple events atomically (best-effort).
	PublishBatch(events []*event.Event) error
}
