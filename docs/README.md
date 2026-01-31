# EDRS Documentation

## Sequence Diagrams

This directory contains PlantUML sequence diagrams that illustrate the event flows in the EDRS system.

### Files

- `sequence-diagrams.puml` - Contains three sequence diagrams:
  1. **Make Reservation - Success Flow**: Shows the complete flow when a reservation is successfully created
  2. **Make Reservation - Failure Flow**: Shows the flow when a reservation fails due to insufficient inventory
  3. **Cancel Reservation Flow**: Shows the complete flow when a reservation is cancelled

### Viewing the Diagrams

#### Option 1: Online (Recommended)
1. Copy the contents of `sequence-diagrams.puml`
2. Paste into [PlantUML Online Server](http://www.plantuml.com/plantuml/uml/)
3. The diagrams will be rendered automatically

#### Option 2: VS Code Extension
1. Install the "PlantUML" extension in VS Code
2. Open `sequence-diagrams.puml`
3. Press `Alt+D` (or `Cmd+D` on Mac) to preview

#### Option 3: Command Line
1. Install PlantUML: `brew install plantuml` (Mac) or download from [plantuml.com](https://plantuml.com/starting)
2. Generate images:
   ```bash
   plantuml -tsvg docs/sequence-diagrams.puml
   ```
   This will generate SVG files for each diagram.

#### Option 4: IntelliJ IDEA
1. Install the "PlantUML integration" plugin
2. Open `sequence-diagrams.puml`
3. Right-click and select "Preview PlantUML Diagram"

### Diagram Notes

- **Kafka** is shown as a participant to illustrate the event bus, but in reality it's a message broker
- **Correlation IDs** are used throughout to trace events across services
- **Idempotency checks** are performed by the Persistence Service before processing events
- **Event sourcing** is implemented via the `event_log` table in PostgreSQL
- All events are logged by the Logging Service for observability

### Key Concepts Illustrated

1. **Choreography Pattern**: Services react to events without a central orchestrator
2. **Event-Driven Architecture**: Services communicate asynchronously via Kafka events
3. **Idempotency**: Events can be safely retried without side effects
4. **Event Sourcing**: All events are logged for audit and replay capabilities
5. **Distributed Tracing**: Correlation IDs enable tracing requests across services
