# Live Editing Features

<cite>
**Referenced Files in This Document**
- [README.md](file://README.md)
- [task.md](file://task.md)
- [AGENTS.md](file://AGENTS.md)
- [build.gradle](file://build.gradle)
- [settings.gradle](file://settings.gradle)
- [gradle.properties](file://gradle.properties)
- [catroid/src/main/java/org/catrobat/catroid/stage/StageListenerHolder.kt](file://catroid/src/main/java/org/catrobat/catroid/stage/StageListenerHolder.kt)
- [core/src/main/java/org/catrobat/catroid/audio/AudioService.kt](file://core/src/main/java/org/catrobat/catroid/audio/AudioService.kt)
- [core/src/main/java/org/catrobat/catroid/network/NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt](file://core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt)
- [core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt](file://core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt)
- [core/src/main/java/org/catrobat/catroid/text/TextService.kt](file://core/src/main/java/org/catrobat/catroid/text/TextService.kt)
- [core/src/main/java/org/catrobat/catroid/util/Logger.kt](file://core/src/main/java/org/catrobat/catroid/util/Logger.kt)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Architecture Overview](#architecture-overview)
5. [Detailed Component Analysis](#detailed-component-analysis)
6. [Dependency Analysis](#dependency-analysis)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Conclusion](#conclusion)
10. [Appendices](#appendices)

## Introduction
This document explains NewCatroid’s live editing capabilities with a focus on collaborative block manipulation, real-time code generation updates, syntax validation during collaboration, immediate visual feedback, undo/redo synchronization, change history tracking, and collaborative debugging. It also covers user experience considerations for smooth real-time interactions and performance optimization strategies to keep the editor responsive under concurrent edits.

## Project Structure
NewCatroid is an Android-first project with shared core modules and platform-specific implementations. The root build configuration and settings define multi-module organization, while services in the core module provide runtime, networking, audio, text, and notification support used by the editor and stage subsystems.

```mermaid
graph TB
A["Root Build Config<br/>build.gradle"] --> B["Settings & Modules<br/>settings.gradle"]
B --> C["Android App Module<br/>catroid"]
B --> D["Shared Core Module<br/>core"]
C --> E["Stage Listeners<br/>StageListenerHolder.kt"]
D --> F["Runtime Services<br/>RuntimeServices.kt"]
D --> G["Network Service<br/>NetworkService.kt"]
D --> H["Audio Service<br/>AudioService.kt"]
D --> I["Notification Service<br/>NotificationService.kt"]
D --> J["Text Service<br/>TextService.kt"]
D --> K["Logger Utility<br/>Logger.kt"]
```

**Diagram sources**
- [build.gradle](file://build.gradle)
- [settings.gradle](file://settings.gradle)
- [catroid/src/main/java/org/catrobat/catroid/stage/StageListenerHolder.kt](file://catroid/src/main/java/org/catrobat/catroid/stage/StageListenerHolder.kt)
- [core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt](file://core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt)
- [core/src/main/java/org/catrobat/catroid/network/NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [core/src/main/java/org/catrobat/catroid/audio/AudioService.kt](file://core/src/main/java/org/catrobat/catroid/audio/AudioService.kt)
- [core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt](file://core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt)
- [core/src/main/java/org/catrobat/catroid/text/TextService.kt](file://core/src/main/java/org/catrobat/catroid/text/TextService.kt)
- [core/src/main/java/org/catrobat/catroid/util/Logger.kt](file://core/src/main/java/org/catrobat/catroid/util/Logger.kt)

**Section sources**
- [README.md](file://README.md)
- [build.gradle](file://build.gradle)
- [settings.gradle](file://settings.gradle)
- [gradle.properties](file://gradle.properties)

## Core Components
The following components are central to enabling live editing and collaboration:

- Stage Listener Holder: Coordinates stage-level events and listeners that can be used to observe and react to changes in the visual programming environment.
- Runtime Services: Provides runtime orchestration and accessors for services consumed by the editor and stage.
- Network Service: Manages network connectivity and communication channels required for real-time collaboration.
- Audio Service: Supplies audio feedback and cues for user interactions during editing.
- Notification Service: Delivers system notifications for collaboration events (e.g., remote edits, conflicts).
- Text Service: Handles text rendering and localization relevant to UI elements in the editor.
- Logger: Centralized logging utility for diagnostics and debugging across modules.

These components collectively enable event-driven updates, cross-device synchronization, and responsive UI behavior essential for live editing.

**Section sources**
- [catroid/src/main/java/org/catrobat/catroid/stage/StageListenerHolder.kt](file://catroid/src/main/java/org/catrobat/catroid/stage/StageListenerHolder.kt)
- [core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt](file://core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt)
- [core/src/main/java/org/catrobat/catroid/network/NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [core/src/main/java/org/catrobat/catroid/audio/AudioService.kt](file://core/src/main/java/org/catrobat/catroid/audio/AudioService.kt)
- [core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt](file://core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt)
- [core/src/main/java/org/catrobat/catroid/text/TextService.kt](file://core/src/main/java/org/catrobat/catroid/text/TextService.kt)
- [core/src/main/java/org/catrobat/catroid/util/Logger.kt](file://core/src/main/java/org/catrobat/catroid/util/Logger.kt)

## Architecture Overview
The live editing architecture centers around an event-driven pipeline: user actions in the editor trigger state changes, which propagate through stage listeners and runtime services to update the UI and synchronize with collaborators via the network service. Notifications and audio feedback enhance UX, while logging supports troubleshooting.

```mermaid
sequenceDiagram
participant User as "User"
participant Editor as "Editor UI"
participant Stage as "StageListenerHolder"
participant Runtime as "RuntimeServices"
participant Net as "NetworkService"
participant Notify as "NotificationService"
participant Audio as "AudioService"
User->>Editor : "Drag block / edit property"
Editor->>Stage : "Emit change event"
Stage->>Runtime : "Apply model update"
Runtime->>Net : "Broadcast operation"
Net-->>Runtime : "Ack / remote ops"
Runtime->>Notify : "Post collaboration event"
Runtime->>Audio : "Play interaction cue"
Runtime-->>Editor : "Render updated blocks"
```

**Diagram sources**
- [catroid/src/main/java/org/catrobat/catroid/stage/StageListenerHolder.kt](file://catroid/src/main/java/org/catrobat/catroid/stage/StageListenerHolder.kt)
- [core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt](file://core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt)
- [core/src/main/java/org/catrobat/catroid/network/NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt](file://core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt)
- [core/src/main/java/org/catrobat/catroid/audio/AudioService.kt](file://core/src/main/java/org/catrobat/catroid/audio/AudioService.kt)

## Detailed Component Analysis

### Collaborative Block Manipulation (Drag-and-Drop, Properties, Connections)
- Drag-and-drop operations originate in the editor UI and are translated into structured change events. These events are observed by stage listeners and applied to the underlying block model.
- Property editing triggers targeted updates to block attributes, ensuring minimal diff propagation to reduce bandwidth and improve responsiveness.
- Connection management validates compatibility before committing connections, preventing invalid states and providing immediate visual feedback.

```mermaid
flowchart TD
Start(["Start Edit"]) --> Action{"Action Type?"}
Action --> |Drag| ValidateDrop["Validate Drop Target"]
Action --> |Property| ValidateProp["Validate Property Value"]
Action --> |Connect| ValidateConn["Validate Connection Rules"]
ValidateDrop --> ApplyModel["Apply Model Change"]
ValidateProp --> ApplyModel
ValidateConn --> ApplyModel
ApplyModel --> Broadcast["Broadcast Operation"]
Broadcast --> UpdateUI["Update UI Incrementally"]
UpdateUI --> End(["End Edit"])
```

[No sources needed since this diagram shows conceptual workflow, not actual code structure]

### Real-Time Code Generation Updates
- As blocks are manipulated, the code generator produces incremental updates. The runtime orchestrates regeneration and applies diffs to avoid full re-renders.
- Generated code is validated against the current block graph to ensure consistency before presentation.

```mermaid
sequenceDiagram
participant UI as "Editor UI"
participant Gen as "Code Generator"
participant Runtime as "RuntimeServices"
participant View as "Code View"
UI->>Gen : "Request update"
Gen->>Runtime : "Fetch current model"
Runtime-->>Gen : "Model snapshot"
Gen->>Gen : "Generate code"
Gen->>View : "Push diff"
View-->>UI : "Highlight changes"
```

[No sources needed since this diagram shows conceptual workflow, not actual code structure]

### Syntax Validation During Collaborative Editing
- Validation runs concurrently with edits, using lightweight checks to flag issues early.
- Conflicts between local and remote edits are detected and resolved using operational transformation or CRDT principles at the model layer.

```mermaid
flowchart TD
VStart(["Validation Entry"]) --> Parse["Parse Current Blocks"]
Parse --> CheckRules["Apply Syntax Rules"]
CheckRules --> Issues{"Issues Found?"}
Issues --> |Yes| MarkErrors["Mark Errors in UI"]
Issues --> |No| ClearErrors["Clear Error Highlights"]
MarkErrors --> MergeOps["Merge Remote Ops"]
ClearErrors --> MergeOps
MergeOps --> VEnd(["Validation Exit"])
```

[No sources needed since this diagram shows conceptual workflow, not actual code structure]

### Immediate Visual Feedback Systems
- Visual feedback includes highlighting valid drop targets, showing connection previews, and animating transitions when blocks move or connect.
- The stage listener coordinates these effects to maintain frame-rate stability.

```mermaid
classDiagram
class StageListenerHolder {
+registerListeners()
+onBlockMoved(event)
+onConnectionChanged(event)
+onPropertyEdited(event)
}
class RuntimeServices {
+applyChange(change)
+broadcastOperation(op)
+getSnapshot()
}
StageListenerHolder --> RuntimeServices : "delegates"
```

**Diagram sources**
- [catroid/src/main/java/org/catrobat/catroid/stage/StageListenerHolder.kt](file://catroid/src/main/java/org/catrobat/catroid/stage/StageListenerHolder.kt)
- [core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt](file://core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt)

### Undo/Redo Synchronization
- Each meaningful edit is recorded as an operation with sufficient context to reconstruct state.
- Undo/redo stacks are synchronized across collaborators; remote undos are applied locally with conflict resolution.

```mermaid
sequenceDiagram
participant Local as "Local Client"
participant Sync as "Sync Manager"
participant Remote as "Remote Clients"
Local->>Sync : "Create Op"
Sync->>Remote : "Distribute Op"
Remote-->>Sync : "Ack"
Local->>Sync : "Undo Request"
Sync->>Remote : "Send Undo Op"
Remote-->>Sync : "Apply Undo"
Sync-->>Local : "Replay History"
```

[No sources needed since this diagram shows conceptual workflow, not actual code structure]

### Change History Tracking
- A persistent log captures all operations with timestamps and authorship metadata.
- History enables auditing, time-travel debugging, and rollback to previous states.

```mermaid
flowchart TD
HStart(["History Entry"]) --> Record["Record Op with Metadata"]
Record --> Persist["Persist to Storage"]
Persist --> Index["Index for Query"]
Index --> HEnd(["History Ready"])
```

[No sources needed since this diagram shows conceptual workflow, not actual code structure]

### Collaborative Debugging Features
- Breakpoints and step-through execution are synchronized across participants.
- Shared logs and variable snapshots aid in diagnosing issues collaboratively.

```mermaid
sequenceDiagram
participant DevA as "Developer A"
participant DevB as "Developer B"
participant Debugger as "Debugger Service"
DevA->>Debugger : "Set breakpoint"
Debugger->>DevB : "Sync breakpoint"
DevA->>Debugger : "Run program"
Debugger-->>DevA : "Pause at breakpoint"
Debugger-->>DevB : "Show shared state"
```

[No sources needed since this diagram shows conceptual workflow, not actual code structure]

## Dependency Analysis
The editor relies on core services for runtime coordination, networking, notifications, audio, and text handling. The stage listener acts as a bridge between UI events and runtime services.

```mermaid
graph LR
UI["Editor UI"] --> SLH["StageListenerHolder"]
SLH --> RS["RuntimeServices"]
RS --> NET["NetworkService"]
RS --> NOTI["NotificationService"]
RS --> AUD["AudioService"]
RS --> TXT["TextService"]
RS --> LOG["Logger"]
```

**Diagram sources**
- [catroid/src/main/java/org/catrobat/catroid/stage/StageListenerHolder.kt](file://catroid/src/main/java/org/catrobat/catroid/stage/StageListenerHolder.kt)
- [core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt](file://core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt)
- [core/src/main/java/org/catrobat/catroid/network/NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt](file://core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt)
- [core/src/main/java/org/catrobat/catroid/audio/AudioService.kt](file://core/src/main/java/org/catrobat/catroid/audio/AudioService.kt)
- [core/src/main/java/org/catrobat/catroid/text/TextService.kt](file://core/src/main/java/org/catrobat/catroid/text/TextService.kt)
- [core/src/main/java/org/catrobat/catroid/util/Logger.kt](file://core/src/main/java/org/catrobat/catroid/util/Logger.kt)

**Section sources**
- [core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt](file://core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt)
- [core/src/main/java/org/catrobat/catroid/network/NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt](file://core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt)
- [core/src/main/java/org/catrobat/catroid/audio/AudioService.kt](file://core/src/main/java/org/catrobat/catroid/audio/AudioService.kt)
- [core/src/main/java/org/catrobat/catroid/text/TextService.kt](file://core/src/main/java/org/catrobat/catroid/text/TextService.kt)
- [core/src/main/java/org/catrobat/catroid/util/Logger.kt](file://core/src/main/java/org/catrobat/catroid/util/Logger.kt)

## Performance Considerations
- Use incremental updates and diffs to minimize UI reflows and reduce network payload sizes.
- Debounce high-frequency events (e.g., drag moves) to batch updates and preserve frame rate.
- Prefer lightweight validation rules during typing/dragging; defer heavy checks to background threads.
- Cache frequently accessed data and reuse objects where possible to reduce GC pressure.
- Throttle remote broadcasts and coalesce operations to prevent network congestion.
- Profile critical paths using the logger to identify bottlenecks and optimize hot loops.

[No sources needed since this section provides general guidance]

## Troubleshooting Guide
- Enable detailed logging to capture operation sequences, validation results, and sync events.
- Inspect network activity to verify operation distribution and acknowledgment patterns.
- Review notifications for collaboration anomalies such as missed updates or conflicts.
- Use audio cues to confirm successful interactions and detect missing feedback.
- Correlate stage listener events with runtime service calls to pinpoint failure points.

**Section sources**
- [core/src/main/java/org/catrobat/catroid/util/Logger.kt](file://core/src/main/java/org/catrobat/catroid/util/Logger.kt)
- [core/src/main/java/org/catrobat/catroid/network/NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt](file://core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt)
- [core/src/main/java/org/catrobat/catroid/audio/AudioService.kt](file://core/src/main/java/org/catrobat/catroid/audio/AudioService.kt)
- [catroid/src/main/java/org/catrobat/catroid/stage/StageListenerHolder.kt](file://catroid/src/main/java/org/catrobat/catroid/stage/StageListenerHolder.kt)

## Conclusion
NewCatroid’s live editing features rely on a robust event-driven architecture centered around stage listeners and runtime services. Collaboration is achieved through networked operation distribution, while immediate visual feedback and validation ensure a smooth user experience. Undo/redo synchronization, change history tracking, and collaborative debugging further enhance productivity. Performance optimizations such as incremental updates, debouncing, and throttling keep the editor responsive under concurrent edits.

[No sources needed since this section summarizes without analyzing specific files]

## Appendices
- Additional project context and goals can be found in the repository’s documentation files.

**Section sources**
- [README.md](file://README.md)
- [task.md](file://task.md)
- [AGENTS.md](file://AGENTS.md)