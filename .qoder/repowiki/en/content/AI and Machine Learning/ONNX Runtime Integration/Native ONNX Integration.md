# Native ONNX Integration

<cite>
**Referenced Files in This Document**
- [catroid/src/main/cpp/onnxruntime_c_api.h](file://catroid/src/main/cpp/onnxruntime_c_api.h)
- [catroid/src/main/cpp/onnxruntime_cxx_api.h](file://catroid/src/main/cpp/onnxruntime_cxx_api.h)
- [catroid/src/main/cpp/onnxruntime_cxx_inline.h](file://catroid/src/main/cpp/onnxruntime_cxx_inline.h)
- [catroid/src/main/cpp/onnxruntime_float16.h](file://catroid/src/main/cpp/onnxruntime_float16.h)
- [catroid/src/main/cpp/onnxruntime_lite_custom_op.h](file://catroid/src/main/cpp/onnxruntime_lite_custom_op.h)
- [catroid/src/main/cpp/onnxruntime_run_options_config_keys.h](file://catroid/src/main/cpp/onnxruntime_run_options_config_keys.h)
- [catroid/src/main/cpp/onnxruntime_session_options_config_keys.h](file://catroid/src/main/cpp/onnxruntime_session_options_config_keys.h)
- [catroid/src/main/cpp/cpu_provider_factory.h](file://catroid/src/main/cpp/cpu_provider_factory.h)
- [catroid/src/main/cpp/nnapi_provider_factory.h](file://catroid/src/main/cpp/nnapi_provider_factory.h)
- [catroid/src/main/cpp/onnxtest.cpp](file://catroid/src/main/cpp/onnxtest.cpp)
- [catroid/src/main/cpp/CMakeLists.txt](file://catroid/src/main/cpp/CMakeLists.txt)
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
This document explains the native ONNX Runtime integration in NewCatroid, focusing on how models are loaded, sessions are created and executed, and how custom operators and execution providers (CPU and NNAPI) are wired into the runtime. It also covers memory management strategies for large models, tensor allocation patterns, resource cleanup procedures, practical usage examples for loading .onnx files, configuring execution providers, handling inputs/outputs with proper data type conversions, and performance optimization techniques such as quantization, operator fusion, and hardware-specific optimizations.

## Project Structure
The ONNX integration is implemented under the Android module’s native layer:
- C++ headers and wrappers for ONNX Runtime APIs
- Provider factory headers for CPU and NNAPI acceleration
- A sample test harness to exercise model loading and inference
- Build configuration that ties everything together

```mermaid
graph TB
subgraph "Native Layer"
ORT_API["ONNX Runtime C API<br/>onnxruntime_c_api.h"]
ORT_CXX["ONNX Runtime C++ API<br/>onnxruntime_cxx_api.h"]
ORT_INLINE["C++ Inline Helpers<br/>onnxruntime_cxx_inline.h"]
FP16["Float16 Utilities<br/>onnxruntime_float16.h"]
CUSTOM_OP["Lite Custom Op Interface<br/>onnxruntime_lite_custom_op.h"]
RUN_OPTS["Run Options Config Keys<br/>onnxruntime_run_options_config_keys.h"]
SESS_OPTS["Session Options Config Keys<br/>onnxruntime_session_options_config_keys.h"]
CPU_PF["CPU Provider Factory<br/>cpu_provider_factory.h"]
NNAPI_PF["NNAPI Provider Factory<br/>nnapi_provider_factory.h"]
TEST["ONNX Test Harness<br/>onnxtest.cpp"]
BUILD["Build Configuration<br/>CMakeLists.txt"]
end
TEST --> ORT_CXX
TEST --> CPU_PF
TEST --> NNAPI_PF
ORT_CXX --> ORT_API
ORT_CXX --> ORT_INLINE
ORT_CXX --> FP16
ORT_CXX --> CUSTOM_OP
ORT_CXX --> RUN_OPTS
ORT_CXX --> SESS_OPTS
BUILD --> TEST
BUILD --> CPU_PF
BUILD --> NNAPI_PF
```

**Diagram sources**
- [catroid/src/main/cpp/onnxruntime_c_api.h](file://catroid/src/main/cpp/onnxruntime_c_api.h)
- [catroid/src/main/cpp/onnxruntime_cxx_api.h](file://catroid/src/main/cpp/onnxruntime_cxx_api.h)
- [catroid/src/main/cpp/onnxruntime_cxx_inline.h](file://catroid/src/main/cpp/onnxruntime_cxx_inline.h)
- [catroid/src/main/cpp/onnxruntime_float16.h](file://catroid/src/main/cpp/onnxruntime_float16.h)
- [catroid/src/main/cpp/onnxruntime_lite_custom_op.h](file://catroid/src/main/cpp/onnxruntime_lite_custom_op.h)
- [catroid/src/main/cpp/onnxruntime_run_options_config_keys.h](file://catroid/src/main/cpp/onnxruntime_run_options_config_keys.h)
- [catroid/src/main/cpp/onnxruntime_session_options_config_keys.h](file://catroid/src/main/cpp/onnxruntime_session_options_config_keys.h)
- [catroid/src/main/cpp/cpu_provider_factory.h](file://catroid/src/main/cpp/cpu_provider_factory.h)
- [catroid/src/main/cpp/nnapi_provider_factory.h](file://catroid/src/main/cpp/nnapi_provider_factory.h)
- [catroid/src/main/cpp/onnxtest.cpp](file://catroid/src/main/cpp/onnxtest.cpp)
- [catroid/src/main/cpp/CMakeLists.txt](file://catroid/src/main/cpp/CMakeLists.txt)

**Section sources**
- [catroid/src/main/cpp/CMakeLists.txt](file://catroid/src/main/cpp/CMakeLists.txt)

## Core Components
- ONNX Runtime C/C++ API bindings: Provide the core interfaces for session creation, input/output binding, and execution.
- Float16 utilities: Support half-precision types used by many modern models.
- Lite custom operator interface: Enables registering custom ops required by some models.
- Execution provider factories: Configure CPU and NNAPI backends for accelerated inference.
- Session/run options keys: Control graph optimization, memory arena sizing, and runtime behavior.
- Test harness: Demonstrates end-to-end usage including model loading, session setup, and inference.

Key responsibilities:
- Model loading from .onnx files
- Session initialization with selected providers
- Input tensor preparation with correct shapes and data types
- Inference execution and output retrieval
- Resource cleanup and error propagation

**Section sources**
- [catroid/src/main/cpp/onnxruntime_cxx_api.h](file://catroid/src/main/cpp/onnxruntime_cxx_api.h)
- [catroid/src/main/cpp/onnxruntime_cxx_inline.h](file://catroid/src/main/cpp/onnxruntime_cxx_inline.h)
- [catroid/src/main/cpp/onnxruntime_float16.h](file://catroid/src/main/cpp/onnxruntime_float16.h)
- [catroid/src/main/cpp/onnxruntime_lite_custom_op.h](file://catroid/src/main/cpp/onnxruntime_lite_custom_op.h)
- [catroid/src/main/cpp/onnxruntime_run_options_config_keys.h](file://catroid/src/main/cpp/onnxruntime_run_options_config_keys.h)
- [catroid/src/main/cpp/onnxruntime_session_options_config_keys.h](file://catroid/src/main/cpp/onnxruntime_session_options_config_keys.h)
- [catroid/src/main/cpp/cpu_provider_factory.h](file://catroid/src/main/cpp/cpu_provider_factory.h)
- [catroid/src/main/cpp/nnapi_provider_factory.h](file://catroid/src/main/cpp/nnapi_provider_factory.h)
- [catroid/src/main/cpp/onnxtest.cpp](file://catroid/src/main/cpp/onnxtest.cpp)

## Architecture Overview
The runtime architecture centers around a single inference pipeline:
- Application code loads an .onnx file into an Ort::Session
- Inputs are prepared as Ort::Value tensors with matching shapes and dtypes
- The session runs with configured providers (CPU, NNAPI)
- Outputs are read back and converted to application types

```mermaid
sequenceDiagram
participant App as "Application Code"
participant Env as "Ort : : Env"
participant Sess as "Ort : : Session"
participant IO as "Input/Output Binding"
participant Prov as "Execution Providers"
App->>Env : "Create environment"
App->>Sess : "Construct session with model path"
Sess->>Prov : "Initialize providers (CPU/NNAPI)"
App->>IO : "Prepare input tensors (shape, dtype)"
App->>Sess : "Run(session_options, inputs, outputs)"
Sess-->>App : "Return output tensors"
App->>IO : "Read outputs and convert types"
App->>Sess : "Release resources"
```

**Diagram sources**
- [catroid/src/main/cpp/onnxtest.cpp](file://catroid/src/main/cpp/onnxtest.cpp)
- [catroid/src/main/cpp/onnxruntime_cxx_api.h](file://catroid/src/main/cpp/onnxruntime_cxx_api.h)
- [catroid/src/main/cpp/cpu_provider_factory.h](file://catroid/src/main/cpp/cpu_provider_factory.h)
- [catroid/src/main/cpp/nnapi_provider_factory.h](file://catroid/src/main/cpp/nnapi_provider_factory.h)

## Detailed Component Analysis

### ONNX Runtime C/C++ API Bindings
- Purpose: Encapsulate low-level C API calls into RAII-style C++ classes for safer resource management.
- Key capabilities:
  - Environment and session lifecycle
  - Tensor creation and binding
  - Run options and session options
  - Error handling via exceptions or status codes

```mermaid
classDiagram
class OrtEnv {
+create()
+release()
}
class OrtSession {
+load_model(path)
+run(inputs, outputs)
+get_input_names()
+get_output_names()
+release()
}
class OrtValue {
+from_buffer(data, shape, dtype)
+to_buffer()
+get_shape()
+get_dtype()
}
class OrtRunOptions {
+set_config_key(key, value)
}
class OrtSessionOptions {
+enable_cpu_mem_arena()
+set_graph_optimization_level()
+append_execution_provider(provider)
}
OrtSession --> OrtEnv : "uses"
OrtSession --> OrtRunOptions : "configures"
OrtSession --> OrtSessionOptions : "configures"
OrtSession --> OrtValue : "consumes/produces"
```

**Diagram sources**
- [catroid/src/main/cpp/onnxruntime_cxx_api.h](file://catroid/src/main/cpp/onnxruntime_cxx_api.h)
- [catroid/src/main/cpp/onnxruntime_cxx_inline.h](file://catroid/src/main/cpp/onnxruntime_cxx_inline.h)

**Section sources**
- [catroid/src/main/cpp/onnxruntime_cxx_api.h](file://catroid/src/main/cpp/onnxruntime_cxx_api.h)
- [catroid/src/main/cpp/onnxruntime_cxx_inline.h](file://catroid/src/main/cpp/onnxruntime_cxx_inline.h)

### Float16 Utilities
- Purpose: Provide helpers for half-precision data conversion and storage, commonly used in quantized or mixed-precision models.
- Typical usage: Convert between float32 buffers and float16 buffers before feeding inputs or after reading outputs.

**Section sources**
- [catroid/src/main/cpp/onnxruntime_float16.h](file://catroid/src/main/cpp/onnxruntime_float16.h)

### Lite Custom Operator Interface
- Purpose: Allow registration of custom operators required by certain models not covered by built-in ops.
- Responsibilities:
  - Define op schema and kernel implementations
  - Register kernels with the runtime during session initialization

**Section sources**
- [catroid/src/main/cpp/onnxruntime_lite_custom_op.h](file://catroid/src/main/cpp/onnxruntime_lite_custom_op.h)

### Execution Provider Factories (CPU and NNAPI)
- Purpose: Enable hardware-accelerated execution paths.
- CPU provider: General-purpose fallback optimized for ARM/x86 CPUs.
- NNAPI provider: Leverages Android Neural Networks API for GPU/NPU acceleration when available.

```mermaid
flowchart TD
Start(["Select Provider"]) --> CheckNNAPI{"NNAPI Available?"}
CheckNNAPI --> |Yes| UseNNAPI["Use NNAPI Provider"]
CheckNNAPI --> |No| UseCPU["Use CPU Provider"]
UseNNAPI --> InitSess["Initialize Session with Provider"]
UseCPU --> InitSess
InitSess --> RunInfer["Run Inference"]
```

**Diagram sources**
- [catroid/src/main/cpp/cpu_provider_factory.h](file://catroid/src/main/cpp/cpu_provider_factory.h)
- [catroid/src/main/cpp/nnapi_provider_factory.h](file://catroid/src/main/cpp/nnapi_provider_factory.h)

**Section sources**
- [catroid/src/main/cpp/cpu_provider_factory.h](file://catroid/src/main/cpp/cpu_provider_factory.h)
- [catroid/src/main/cpp/nnapi_provider_factory.h](file://catroid/src/main/cpp/nnapi_provider_factory.h)

### Session and Run Options
- Purpose: Tune runtime behavior for performance and memory usage.
- Common keys:
  - Graph optimization level
  - Memory arena sizing
  - Execution provider settings
  - Logging verbosity

```mermaid
flowchart TD
A["Create SessionOptions"] --> B["Set Optimization Level"]
B --> C["Configure Memory Arenas"]
C --> D["Append Execution Providers"]
D --> E["Create RunOptions"]
E --> F["Run Session"]
```

**Diagram sources**
- [catroid/src/main/cpp/onnxruntime_session_options_config_keys.h](file://catroid/src/main/cpp/onnxruntime_session_options_config_keys.h)
- [catroid/src/main/cpp/onnxruntime_run_options_config_keys.h](file://catroid/src/main/cpp/onnxruntime_run_options_config_keys.h)

**Section sources**
- [catroid/src/main/cpp/onnxruntime_session_options_config_keys.h](file://catroid/src/main/cpp/onnxruntime_session_options_config_keys.h)
- [catroid/src/main/cpp/onnxruntime_run_options_config_keys.h](file://catroid/src/main/cpp/onnxruntime_run_options_config_keys.h)

### End-to-End Inference Flow (Test Harness)
- Demonstrates:
  - Loading an .onnx model
  - Creating a session with providers
  - Preparing inputs with correct shapes and dtypes
  - Running inference and retrieving outputs
  - Cleaning up resources

```mermaid
sequenceDiagram
participant T as "onnxtest.cpp"
participant O as "Ort : : Session"
participant P as "Providers"
participant I as "Inputs/Outputs"
T->>O : "Load model (.onnx)"
O->>P : "Initialize CPU/NNAPI"
T->>I : "Allocate input tensors"
T->>O : "Run with inputs"
O-->>T : "Return outputs"
T->>I : "Convert outputs to app types"
T->>O : "Release session"
```

**Diagram sources**
- [catroid/src/main/cpp/onnxtest.cpp](file://catroid/src/main/cpp/onnxtest.cpp)
- [catroid/src/main/cpp/onnxruntime_cxx_api.h](file://catroid/src/main/cpp/onnxruntime_cxx_api.h)

**Section sources**
- [catroid/src/main/cpp/onnxtest.cpp](file://catroid/src/main/cpp/onnxtest.cpp)

## Dependency Analysis
The native build wires the ONNX Runtime components together:
- Headers define interfaces and configuration keys
- Provider factories enable hardware acceleration
- The test harness exercises the full stack
- CMake config ensures all pieces are compiled and linked

```mermaid
graph LR
H1["onnxruntime_c_api.h"] --> H2["onnxruntime_cxx_api.h"]
H2 --> H3["onnxruntime_cxx_inline.h"]
H2 --> H4["onnxruntime_float16.h"]
H2 --> H5["onnxruntime_lite_custom_op.h"]
H2 --> H6["onnxruntime_session_options_config_keys.h"]
H2 --> H7["onnxruntime_run_options_config_keys.h"]
H8["cpu_provider_factory.h"] --> H2
H9["nnapi_provider_factory.h"] --> H2
T["onnxtest.cpp"] --> H2
T --> H8
T --> H9
B["CMakeLists.txt"] --> T
B --> H8
B --> H9
```

**Diagram sources**
- [catroid/src/main/cpp/onnxruntime_c_api.h](file://catroid/src/main/cpp/onnxruntime_c_api.h)
- [catroid/src/main/cpp/onnxruntime_cxx_api.h](file://catroid/src/main/cpp/onnxruntime_cxx_api.h)
- [catroid/src/main/cpp/onnxruntime_cxx_inline.h](file://catroid/src/main/cpp/onnxruntime_cxx_inline.h)
- [catroid/src/main/cpp/onnxruntime_float16.h](file://catroid/src/main/cpp/onnxruntime_float16.h)
- [catroid/src/main/cpp/onnxruntime_lite_custom_op.h](file://catroid/src/main/cpp/onnxruntime_lite_custom_op.h)
- [catroid/src/main/cpp/onnxruntime_session_options_config_keys.h](file://catroid/src/main/cpp/onnxruntime_session_options_config_keys.h)
- [catroid/src/main/cpp/onnxruntime_run_options_config_keys.h](file://catroid/src/main/cpp/onnxruntime_run_options_config_keys.h)
- [catroid/src/main/cpp/cpu_provider_factory.h](file://catroid/src/main/cpp/cpu_provider_factory.h)
- [catroid/src/main/cpp/nnapi_provider_factory.h](file://catroid/src/main/cpp/nnapi_provider_factory.h)
- [catroid/src/main/cpp/onnxtest.cpp](file://catroid/src/main/cpp/onnxtest.cpp)
- [catroid/src/main/cpp/CMakeLists.txt](file://catroid/src/main/cpp/CMakeLists.txt)

**Section sources**
- [catroid/src/main/cpp/CMakeLists.txt](file://catroid/src/main/cpp/CMakeLists.txt)

## Performance Considerations
- Quantization: Prefer INT8 or FP16 models where supported to reduce memory bandwidth and improve throughput.
- Operator fusion: Ensure graph optimizations are enabled so compatible ops are fused at load time.
- Hardware-specific tuning:
  - NNAPI: Enable NNAPI provider for devices with capable accelerators; fall back to CPU otherwise.
  - CPU: Adjust memory arenas and thread pools if needed via session options.
- Memory management:
  - Reuse input/output buffers across frames to avoid allocations.
  - Use arena-based memory for large intermediate tensors.
- Data layout:
  - Match expected NHWC/NCHW layouts to minimize copies.
  - Avoid unnecessary type conversions; use FP16 directly when possible.

[No sources needed since this section provides general guidance]

## Troubleshooting Guide
Common issues and resolutions:
- Model loading failures:
  - Verify .onnx file integrity and compatibility with the runtime version.
  - Check for unsupported ops; consider adding custom op registrations.
- Provider initialization errors:
  - Confirm NNAPI availability; ensure CPU fallback is configured.
  - Validate provider-specific options and device capabilities.
- Shape/dtype mismatches:
  - Inspect input/output names and metadata from the session.
  - Ensure buffer strides and element types match the model expectations.
- Out-of-memory conditions:
  - Reduce batch sizes or model complexity.
  - Tune arena sizes and disable unnecessary optimizations.
- Cleanup problems:
  - Ensure sessions and environments are released in reverse order of creation.
  - Validate no dangling references to allocated buffers.

**Section sources**
- [catroid/src/main/cpp/onnxtest.cpp](file://catroid/src/main/cpp/onnxtest.cpp)
- [catroid/src/main/cpp/onnxruntime_cxx_api.h](file://catroid/src/main/cpp/onnxruntime_cxx_api.h)

## Conclusion
NewCatroid’s native ONNX integration leverages the ONNX Runtime C++ API to provide a robust inference pipeline with flexible provider selection (CPU and NNAPI). By carefully managing memory, aligning data types and layouts, and enabling appropriate optimizations, developers can achieve efficient and scalable AI inference on Android devices. The provided headers and test harness serve as a foundation for integrating diverse ONNX models while maintaining performance and reliability.

[No sources needed since this section summarizes without analyzing specific files]

## Appendices

### Practical Examples

- Loading a .onnx file and creating a session
  - Steps:
    - Initialize environment
    - Construct session with model path
    - Optionally configure session options (optimization level, providers)
  - References:
    - [catroid/src/main/cpp/onnxtest.cpp](file://catroid/src/main/cpp/onnxtest.cpp)
    - [catroid/src/main/cpp/onnxruntime_cxx_api.h](file://catroid/src/main/cpp/onnxruntime_cxx_api.h)

- Configuring execution providers
  - Steps:
    - Create session options
    - Append CPU provider
    - Conditionally append NNAPI provider if available
  - References:
    - [catroid/src/main/cpp/cpu_provider_factory.h](file://catroid/src/main/cpp/cpu_provider_factory.h)
    - [catroid/src/main/cpp/nnapi_provider_factory.h](file://catroid/src/main/cpp/nnapi_provider_factory.h)
    - [catroid/src/main/cpp/onnxruntime_session_options_config_keys.h](file://catroid/src/main/cpp/onnxruntime_session_options_config_keys.h)

- Handling inputs/outputs with proper data type conversions
  - Steps:
    - Query input/output names and shapes
    - Allocate buffers with correct dtypes (FP32/FP16/INT8)
    - Populate inputs and run session
    - Read outputs and convert to application types
  - References:
    - [catroid/src/main/cpp/onnxruntime_cxx_api.h](file://catroid/src/main/cpp/onnxruntime_cxx_api.h)
    - [catroid/src/main/cpp/onnxruntime_float16.h](file://catroid/src/main/cpp/onnxruntime_float16.h)

- Resource cleanup procedures
  - Steps:
    - Release outputs and inputs
    - Destroy session
    - Release environment
  - References:
    - [catroid/src/main/cpp/onnxtest.cpp](file://catroid/src/main/cpp/onnxtest.cpp)
    - [catroid/src/main/cpp/onnxruntime_cxx_api.h](file://catroid/src/main/cpp/onnxruntime_cxx_api.h)