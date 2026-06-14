// Package event provides a stdout-based implementation of the Publisher interface.
//
// Canonical Reference: .pi/architecture/modules/cli-orchestrator.md#components
//
// The StdoutPublisher writes structured JSON events to stderr, one per line.
// This enables CI pipeline consumers to tail and react to events without
// interfering with the CLI's stdout output (which is reserved for the
// analysis result).
package event

import (
	"encoding/json"
	"fmt"
	"log/slog"
	"os"

	"github.com/arman-jalili/keystone-cli/internal/domain/event"
)

// StdoutPublisher implements Publisher by writing JSON events to stderr.
type StdoutPublisher struct {
	encoder *json.Encoder
	logger  *slog.Logger
}

// NewStdoutPublisher creates a new StdoutPublisher that writes to stderr.
func NewStdoutPublisher(logger *slog.Logger) *StdoutPublisher {
	return &StdoutPublisher{
		encoder: json.NewEncoder(os.Stderr),
		logger:  logger,
	}
}

// Publish writes a single domain event as a JSON line to stderr.
func (p *StdoutPublisher) Publish(e *event.Event) error {
	if err := p.encoder.Encode(e); err != nil {
		return fmt.Errorf("publish event: %w", err)
	}
	return nil
}

// PublishBatch writes multiple events as JSON lines to stderr.
func (p *StdoutPublisher) PublishBatch(events []*event.Event) error {
	for _, e := range events {
		if err := p.Publish(e); err != nil {
			p.logger.Warn("publish batch: event failed", "type", e.Type, "error", err)
			// Continue with remaining events — best-effort
		}
	}
	return nil
}

// compile-time interface check
var _ Publisher = (*StdoutPublisher)(nil)
