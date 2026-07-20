# Phiro Robot Programming

<cite>
**Referenced Files in This Document**
- [README.md](file://README.md)
- [catblocks/phiro.xml](file://catroid/src/main/assets/catblocks/phiro.xml)
- [PhiroRobot.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroRobot.java)
- [PhiroCamera.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCamera.java)
- [PhiroLEDMatrix.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroLEDMatrix.java)
- [PhiroSensors.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroSensors.java)
- [PhiroMovementController.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroMovementController.java)
- [PhiroCalibration.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCalibration.java)
- [PhiroProjectExamples.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroProjectExamples.java)
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
This document explains how to program Phiro robots using NewCatroid’s visual programming blocks and runtime. It focuses on Phiro-specific capabilities such as camera-based vision, LED matrix display, and advanced sensors, and provides tutorials for popular projects like face tracking, gesture recognition, and interactive games. It also covers calibration procedures to ensure accurate sensor readings and optimal robot performance.

## Project Structure
NewCatroid organizes Phiro support across several areas:
- Visual block definitions for Phiro are provided in the catblocks assets.
- Runtime classes implement Phiro device control (movement, camera, LEDs, sensors).
- Example projects demonstrate common use cases.

```mermaid
graph TB
subgraph "Blocks"
B1["phiro.xml<br/>Block definitions"]
end
subgraph "Runtime"
R1["PhiroRobot.java<br/>Device lifecycle"]
R2["PhiroMovementController.java<br/>Motors and wheels"]
R3["PhiroCamera.java<br/>Vision pipeline"]
R4["PhiroLEDMatrix.java<br/>Pixel patterns"]
R5["PhiroSensors.java<br/>Sensor readings"]
R6["PhiroCalibration.java<br/>Calibration routines"]
end
subgraph "Examples"
E1["PhiroProjectExamples.java<br/>Tutorials"]
end
B1 --> R1
R1 --> R2
R1 --> R3
R1 --> R4
R1 --> R5
R1 --> R6
E1 --> R1
```

**Diagram sources**
- [catblocks/phiro.xml](file://catroid/src/main/assets/catblocks/phiro.xml)
- [PhiroRobot.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroRobot.java)
- [PhiroMovementController.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroMovementController.java)
- [PhiroCamera.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCamera.java)
- [PhiroLEDMatrix.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroLEDMatrix.java)
- [PhiroSensors.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroSensors.java)
- [PhiroCalibration.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCalibration.java)
- [PhiroProjectExamples.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroProjectExamples.java)

**Section sources**
- [README.md](file://README.md)
- [catblocks/phiro.xml](file://catroid/src/main/assets/catblocks/phiro.xml)

## Core Components
- PhiroRobot: Central controller that manages connection, initialization, and lifecycle of Phiro subsystems.
- PhiroMovementController: Encapsulates motor commands, wheel speeds, turning, and motion primitives.
- PhiroCamera: Provides image capture, frame access, and basic vision operations used by higher-level features.
- PhiroLEDMatrix: Controls the onboard LED matrix for displaying patterns and animations.
- PhiroSensors: Reads data from onboard sensors (e.g., line-following, obstacle detection, orientation).
- PhiroCalibration: Implements calibration workflows to normalize sensor inputs and improve accuracy.
- PhiroProjectExamples: Contains example programs and step-by-step guidance for common tasks.

Key responsibilities:
- Movement control with smooth acceleration and deceleration.
- Camera-driven behaviors such as color or shape detection.
- LED feedback synchronized with actions.
- Sensor fusion and thresholding for robust behavior.

**Section sources**
- [PhiroRobot.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroRobot.java)
- [PhiroMovementController.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroMovementController.java)
- [PhiroCamera.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCamera.java)
- [PhiroLEDMatrix.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroLEDMatrix.java)
- [PhiroSensors.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroSensors.java)
- [PhiroCalibration.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCalibration.java)
- [PhiroProjectExamples.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroProjectExamples.java)

## Architecture Overview
The Phiro runtime integrates visual blocks with device drivers through a layered architecture. Blocks invoke high-level APIs exposed by PhiroRobot, which delegates to specialized controllers.

```mermaid
classDiagram
class PhiroRobot {
+initialize()
+connect()
+disconnect()
+getMovementController()
+getCamera()
+getLEDMatrix()
+getSensors()
}
class PhiroMovementController {
+setSpeed(left,right)
+forward(distance)
+turn(degrees)
+stop()
}
class PhiroCamera {
+startCapture()
+stopCapture()
+getFrame()
+detectColor(color)
+detectShape(shape)
}
class PhiroLEDMatrix {
+clear()
+setPixel(x,y,color)
+showPattern(pattern)
+animate(sequence)
}
class PhiroSensors {
+readLineSensors()
+readObstacle()
+readOrientation()
+calibrate()
}
class PhiroCalibration {
+runProcedure()
+applyOffsets()
+validateAccuracy()
}
PhiroRobot --> PhiroMovementController : "provides"
PhiroRobot --> PhiroCamera : "provides"
PhiroRobot --> PhiroLEDMatrix : "provides"
PhiroRobot --> PhiroSensors : "provides"
PhiroSensors --> PhiroCalibration : "uses"
```

**Diagram sources**
- [PhiroRobot.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroRobot.java)
- [PhiroMovementController.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroMovementController.java)
- [PhiroCamera.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCamera.java)
- [PhiroLEDMatrix.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroLEDMatrix.java)
- [PhiroSensors.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroSensors.java)
- [PhiroCalibration.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCalibration.java)

## Detailed Component Analysis

### Movement Control
Movement is handled via PhiroMovementController, which exposes intuitive methods for driving the robot. Typical usage includes setting wheel speeds, moving forward/backward, and turning with specified angles.

```mermaid
sequenceDiagram
participant User as "User Program"
participant Blocks as "Phiro Blocks"
participant Robot as "PhiroRobot"
participant Move as "PhiroMovementController"
User->>Blocks : "Start movement sequence"
Blocks->>Robot : "getMovementController()"
Robot-->>Blocks : "Move instance"
Blocks->>Move : "setSpeed(left,right)"
Move-->>Blocks : "ack"
Blocks->>Move : "forward(distance)"
Move-->>Blocks : "done"
Blocks->>Move : "turn(degrees)"
Move-->>Blocks : "done"
Blocks->>Move : "stop()"
Move-->>Blocks : "stopped"
```

**Diagram sources**
- [PhiroRobot.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroRobot.java)
- [PhiroMovementController.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroMovementController.java)

**Section sources**
- [PhiroMovementController.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroMovementController.java)

### Camera Operations
PhiroCamera enables capturing frames and performing simple vision tasks such as color and shape detection. These capabilities power behaviors like line following and object tracking.

```mermaid
flowchart TD
Start(["Start Camera"]) --> Init["Initialize Capture"]
Init --> Loop{"Loop"}
Loop --> |Yes| Frame["Get Current Frame"]
Frame --> Detect["Detect Color/Shape"]
Detect --> Decide{"Target Found?"}
Decide --> |Yes| Act["Adjust Movement"]
Decide --> |No| Wait["Wait/Scan"]
Act --> Loop
Wait --> Loop
Loop --> |No| Stop["Stop Capture"]
```

**Diagram sources**
- [PhiroCamera.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCamera.java)

**Section sources**
- [PhiroCamera.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCamera.java)

### LED Matrix Display
PhiroLEDMatrix allows drawing pixels and animating patterns to provide user feedback or display messages.

```mermaid
sequenceDiagram
participant Blocks as "Phiro Blocks"
participant LED as "PhiroLEDMatrix"
Blocks->>LED : "clear()"
Blocks->>LED : "setPixel(x,y,color)"
Blocks->>LED : "showPattern(pattern)"
Blocks->>LED : "animate(sequence)"
```

**Diagram sources**
- [PhiroLEDMatrix.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroLEDMatrix.java)

**Section sources**
- [PhiroLEDMatrix.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroLEDMatrix.java)

### Sensors and Calibration
PhiroSensors reads data from multiple onboard sensors. PhiroCalibration provides routines to calibrate thresholds and offsets for improved accuracy.

```mermaid
flowchart TD
CalStart(["Begin Calibration"]) --> ReadRaw["Read Raw Sensors"]
ReadRaw --> Compute["Compute Thresholds/Offsets"]
Compute --> Apply["Apply Calibration Values"]
Apply --> Validate["Validate Accuracy"]
Validate --> Pass{"Within Tolerance?"}
Pass --> |Yes| Done(["Calibration Complete"])
Pass --> |No| Adjust["Adjust Parameters"]
Adjust --> Compute
```

**Diagram sources**
- [PhiroSensors.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroSensors.java)
- [PhiroCalibration.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCalibration.java)

**Section sources**
- [PhiroSensors.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroSensors.java)
- [PhiroCalibration.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCalibration.java)

### Popular Projects Tutorials
Step-by-step guides for common Phiro projects are implemented in PhiroProjectExamples. Each tutorial combines movement, sensors, camera, and LED feedback to create interactive behaviors.

- Face Tracking: Uses camera detection to steer toward faces and displays expressions on the LED matrix.
- Gesture Recognition: Interprets sensor inputs and gestures to trigger actions and animations.
- Interactive Games: Combines obstacle detection, movement, and LED feedback for engaging gameplay.

```mermaid
sequenceDiagram
participant Tutorial as "Tutorial Code"
participant Robot as "PhiroRobot"
participant Cam as "PhiroCamera"
participant Move as "PhiroMovementController"
participant LED as "PhiroLEDMatrix"
Tutorial->>Robot : "initialize()"
Tutorial->>Cam : "startCapture()"
loop Behavior Loop
Tutorial->>Cam : "getFrame()"
Tutorial->>Tutorial : "Process Vision/Sensors"
Tutorial->>Move : "Adjust Speed/Turn"
Tutorial->>LED : "Update Pattern"
end
Tutorial->>Cam : "stopCapture()"
Tutorial->>Robot : "disconnect()"
```

**Diagram sources**
- [PhiroProjectExamples.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroProjectExamples.java)
- [PhiroCamera.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCamera.java)
- [PhiroMovementController.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroMovementController.java)
- [PhiroLEDMatrix.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroLEDMatrix.java)

**Section sources**
- [PhiroProjectExamples.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroProjectExamples.java)

## Dependency Analysis
Phiro components exhibit clear separation of concerns:
- PhiroRobot orchestrates subsystems and exposes unified APIs.
- Controllers depend on PhiroRobot but not on each other directly.
- Calibration depends on sensors; examples depend on all subsystems.

```mermaid
graph LR
A["PhiroRobot.java"] --> B["PhiroMovementController.java"]
A --> C["PhiroCamera.java"]
A --> D["PhiroLEDMatrix.java"]
A --> E["PhiroSensors.java"]
E --> F["PhiroCalibration.java"]
G["PhiroProjectExamples.java"] --> A
```

**Diagram sources**
- [PhiroRobot.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroRobot.java)
- [PhiroMovementController.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroMovementController.java)
- [PhiroCamera.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCamera.java)
- [PhiroLEDMatrix.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroLEDMatrix.java)
- [PhiroSensors.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroSensors.java)
- [PhiroCalibration.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCalibration.java)
- [PhiroProjectExamples.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroProjectExamples.java)

**Section sources**
- [PhiroRobot.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroRobot.java)
- [PhiroProjectExamples.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroProjectExamples.java)

## Performance Considerations
- Limit camera processing frequency to balance responsiveness and CPU load.
- Use calibrated sensor thresholds to reduce false positives and unnecessary corrections.
- Prefer batched LED updates to minimize rendering overhead.
- Smooth acceleration profiles improve stability and battery life.

[No sources needed since this section provides general guidance]

## Troubleshooting Guide
Common issues and resolutions:
- Connection failures: Ensure proper initialization and reconnection logic in PhiroRobot.
- Erratic movement: Verify calibration values and adjust speed limits.
- Poor detection accuracy: Re-run calibration and refine thresholds in PhiroSensors.
- LED flicker: Consolidate pixel updates and avoid excessive redraws.

**Section sources**
- [PhiroRobot.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroRobot.java)
- [PhiroCalibration.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroCalibration.java)
- [PhiroSensors.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroSensors.java)
- [PhiroLEDMatrix.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroLEDMatrix.java)

## Conclusion
NewCatroid’s Phiro integration provides a cohesive set of blocks and runtime APIs for movement, vision, LED feedback, and sensor interactions. By leveraging calibration routines and structured project examples, users can build robust behaviors such as face tracking, gesture recognition, and interactive games. The modular architecture supports extensibility and maintainability while keeping complexity accessible for learners.

[No sources needed since this section summarizes without analyzing specific files]

## Appendices
- Block Reference: See phiro.xml for available Phiro blocks and parameters.
- API Quick Start: Initialize PhiroRobot, then access controllers for movement, camera, LEDs, and sensors.
- Example Gallery: Explore PhiroProjectExamples for complete tutorials and inspiration.

**Section sources**
- [catblocks/phiro.xml](file://catroid/src/main/assets/catblocks/phiro.xml)
- [PhiroProjectExamples.java](file://catroid/src/main/java/org/catrobat/catroid/robotics/phiro/PhiroProjectExamples.java)