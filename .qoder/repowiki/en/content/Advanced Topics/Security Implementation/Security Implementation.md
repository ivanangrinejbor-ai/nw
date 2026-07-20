# Security Implementation

<cite>
**Referenced Files in This Document**
- [AndroidManifest.xml](file://catroid/src/main/AndroidManifest.xml)
- [build.gradle](file://catroid/build.gradle)
- [proguard-project.txt](file://catroid/proguard-project.txt)
- [proguard-runtime.pro](file://catroid/proguard-runtime.pro)
- [trustedDomains.json](file://catroid/src/main/assets/trustedDomains.json)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [Jenkinsfile.releaseAPK](file://Jenkinsfile.releaseAPK)
- [Fastfile](file://fastlane/Fastfile)
- [Appfile](file://fastlane/Appfile)
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
This document describes the security implementation for NewCatroid, focusing on code signing and certificate management, APK hardening with ProGuard/R8, permission management following least privilege, secure storage using Android Keystore, secure communication (HTTPS/TLS), input validation, SQL injection prevention, XSS protection, secure API design, network security configuration, OAuth authentication flow, session management, encryption at rest and in transit, OWASP compliance, and security testing methodologies. It maps these practices to concrete files and build artifacts where applicable.

## Project Structure
NewCatroid is an Android application with multiple modules and flavors. Security-relevant configurations are primarily located in:
- Application manifest for permissions and network security policy references
- Build scripts for signing and hardening
- Network layer for HTTPS/TLS handling
- Assets for trusted domain lists
- CI/CD pipelines for release signing and artifact generation

```mermaid
graph TB
A["AndroidManifest.xml"] --> B["Permissions & Network Policy"]
C["build.gradle"] --> D["Signing Configs"]
E["proguard-project.txt"] --> F["R8/ProGuard Rules"]
G["proguard-runtime.pro"] --> F
H["NetworkService.kt"] --> I["HTTP Client Setup"]
J["NeoCatroidApi.java"] --> K["API Endpoints"]
L["trustedDomains.json"] --> M["Trusted Domains List"]
N["Jenkinsfile.releaseAPK"] --> O["Release Signing"]
P["Fastfile"] --> Q["Fastlane Automation"]
R["Appfile"] --> Q
```

**Diagram sources**
- [AndroidManifest.xml](file://catroid/src/main/AndroidManifest.xml)
- [build.gradle](file://catroid/build.gradle)
- [proguard-project.txt](file://catroid/proguard-project.txt)
- [proguard-runtime.pro](file://catroid/proguard-runtime.pro)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [trustedDomains.json](file://catroid/src/main/assets/trustedDomains.json)
- [Jenkinsfile.releaseAPK](file://Jenkinsfile.releaseAPK)
- [Fastfile](file://fastlane/Fastfile)
- [Appfile](file://fastlane/Appfile)

**Section sources**
- [AndroidManifest.xml](file://catroid/src/main/AndroidManifest.xml)
- [build.gradle](file://catroid/build.gradle)
- [proguard-project.txt](file://catroid/proguard-project.txt)
- [proguard-runtime.pro](file://catroid/proguard-runtime.pro)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [trustedDomains.json](file://catroid/src/main/assets/trustedDomains.json)
- [Jenkinsfile.releaseAPK](file://Jenkinsfile.releaseAPK)
- [Fastfile](file://fastlane/Fastfile)
- [Appfile](file://fastlane/Appfile)

## Core Components
- Code signing and certificate management:
  - Release signing configured in build scripts and orchestrated by CI/CD pipelines.
  - Fastlane automates signing and distribution tasks.
- APK hardening:
  - R8/ProGuard rules defined in dedicated rule files to shrink, optimize, and obfuscate code.
- Permission management:
  - Permissions declared in the Android manifest; follow least privilege by requesting only necessary ones.
- Secure storage:
  - Use Android Keystore for cryptographic keys and secrets.
- Secure communication:
  - HTTPS/TLS enforced via HTTP client configuration and optional domain allowlists.
- Input validation and sanitization:
  - Validate and sanitize all user inputs before processing or rendering.
- SQL injection prevention:
  - Parameterized queries and ORM usage to avoid string concatenation.
- XSS protection:
  - Escape output when rendering HTML or executing scripts.
- Secure API design:
  - Centralized API endpoints with consistent error handling and authentication headers.
- Network security:
  - Network security policy configuration and trusted domains list.
- OAuth and sessions:
  - Implement PKCE-based OAuth flows and short-lived tokens with refresh mechanisms.
- Encryption:
  - Encrypt sensitive data at rest using Keystore-backed algorithms; enforce TLS for data in transit.

**Section sources**
- [build.gradle](file://catroid/build.gradle)
- [proguard-project.txt](file://catroid/proguard-project.txt)
- [proguard-runtime.pro](file://catroid/proguard-runtime.pro)
- [AndroidManifest.xml](file://catroid/src/main/AndroidManifest.xml)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [trustedDomains.json](file://catroid/src/main/assets/trustedDomains.json)
- [Jenkinsfile.releaseAPK](file://Jenkinsfile.releaseAPK)
- [Fastfile](file://fastlane/Fastfile)
- [Appfile](file://fastlane/Appfile)

## Architecture Overview
The security architecture integrates build-time protections (signing, hardening), runtime protections (permissions, Keystore, TLS), and pipeline automation (CI/CD). The network layer centralizes secure communications and enforces policies.

```mermaid
graph TB
subgraph "Build & CI"
S["Signing Configs<br/>build.gradle"]
P["ProGuard/R8 Rules<br/>proguard*.pro"]
J["Release Pipeline<br/>Jenkinsfile.releaseAPK"]
F["Fastlane<br/>Fastfile + Appfile"]
end
subgraph "App Runtime"
M["AndroidManifest<br/>Permissions & Policy"]
K["Keystore Usage<br/>Secure Storage"]
N["Network Layer<br/>NetworkService.kt"]
A["API Layer<br/>NeoCatroidApi.java"]
T["Trusted Domains<br/>trustedDomains.json"]
end
S --> J
P --> J
J --> F
M --> N
K --> N
N --> A
T --> N
```

**Diagram sources**
- [build.gradle](file://catroid/build.gradle)
- [proguard-project.txt](file://catroid/proguard-project.txt)
- [proguard-runtime.pro](file://catroid/proguard-runtime.pro)
- [Jenkinsfile.releaseAPK](file://Jenkinsfile.releaseAPK)
- [Fastfile](file://fastlane/Fastfile)
- [Appfile](file://fastlane/Appfile)
- [AndroidManifest.xml](file://catroid/src/main/AndroidManifest.xml)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [trustedDomains.json](file://catroid/src/main/assets/trustedDomains.json)

## Detailed Component Analysis

### Code Signing and Certificate Management
- Build-time signing configuration is defined in the module’s build script.
- CI/CD pipelines orchestrate signing during release builds.
- Fastlane automates signing steps and artifact publishing.

```mermaid
sequenceDiagram
participant Dev as "Developer"
participant CI as "Jenkinsfile.releaseAPK"
participant Gradle as "build.gradle"
participant Fastlane as "Fastfile"
participant Store as "Keystore"
Dev->>CI : Trigger release build
CI->>Gradle : Assemble release variant
Gradle->>Store : Load signing credentials
Gradle-->>CI : Signed APK/AAB
CI->>Fastlane : Upload/Deploy signed artifact
```

**Diagram sources**
- [Jenkinsfile.releaseAPK](file://Jenkinsfile.releaseAPK)
- [build.gradle](file://catroid/build.gradle)
- [Fastfile](file://fastlane/Fastfile)
- [Appfile](file://fastlane/Appfile)

**Section sources**
- [build.gradle](file://catroid/build.gradle)
- [Jenkinsfile.releaseAPK](file://Jenkinsfile.releaseAPK)
- [Fastfile](file://fastlane/Fastfile)
- [Appfile](file://fastlane/Appfile)

### APK Hardening with ProGuard/R8
- Obfuscation, shrinking, and optimization are controlled by ProGuard/R8 rule files.
- Separate rules exist for core app and runtime components.

```mermaid
flowchart TD
Start(["Build Entry"]) --> ReadRules["Load ProGuard/R8 Rules<br/>proguard-project.txt<br/>proguard-runtime.pro"]
ReadRules --> Shrink["Shrink Unused Code"]
Shrink --> Optimize["Optimize Bytecode"]
Optimize --> Obfuscate["Obfuscate Names"]
Obfuscate --> Output["Generate Hardened APK"]
```

**Diagram sources**
- [proguard-project.txt](file://catroid/proguard-project.txt)
- [proguard-runtime.pro](file://catroid/proguard-runtime.pro)

**Section sources**
- [proguard-project.txt](file://catroid/proguard-project.txt)
- [proguard-runtime.pro](file://catroid/proguard-runtime.pro)

### Permission Management (Least Privilege)
- Declare only required permissions in the manifest.
- Request runtime permissions dynamically when needed.
- Audit permissions regularly to remove unused ones.

```mermaid
flowchart TD
A["User Action"] --> B{"Permission Required?"}
B -- No --> C["Proceed Without Permission"]
B -- Yes --> D["Check Granted"]
D -- Granted --> E["Execute Feature"]
D -- Not Granted --> F["Request Permission"]
F --> G{"User Allows?"}
G -- Yes --> E
G -- No --> H["Deny Feature / Show Guidance"]
```

[No sources needed since this diagram shows conceptual workflow, not actual code structure]

**Section sources**
- [AndroidManifest.xml](file://catroid/src/main/AndroidManifest.xml)

### Secure Storage Using Android Keystore
- Store cryptographic keys and secrets in Android Keystore.
- Avoid storing plaintext secrets in SharedPreferences or assets.
- Use alias-based key retrieval and associate keys with hardware-backed backends when available.

```mermaid
classDiagram
class KeyManager {
+generateKey(alias, params) void
+getKey(alias) SecretKey
+deleteKey(alias) boolean
}
class CryptoUtils {
+encrypt(data, key) byte[]
+decrypt(cipherText, key) byte[]
}
class SecureStorage {
+saveSecret(alias, data) void
+loadSecret(alias) byte[]
}
KeyManager <.. CryptoUtils : "uses"
CryptoUtils <.. SecureStorage : "uses"
```

[No sources needed since this diagram shows conceptual workflow, not actual code structure]

**Section sources**
- [AndroidManifest.xml](file://catroid/src/main/AndroidManifest.xml)

### Secure Communication (HTTPS/TLS)
- Enforce HTTPS for all network calls.
- Configure trust anchors and certificate pinning if required.
- Maintain a trusted domains list to restrict outbound connections.

```mermaid
sequenceDiagram
participant UI as "UI Layer"
participant Net as "NetworkService.kt"
participant API as "NeoCatroidApi.java"
participant Server as "Remote Server"
UI->>Net : Initiate request
Net->>Net : Validate URL against trustedDomains.json
Net->>API : Build authenticated request
API->>Server : HTTPS/TLS call
Server-->>API : Encrypted response
API-->>Net : Parse response
Net-->>UI : Deliver result
```

**Diagram sources**
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [trustedDomains.json](file://catroid/src/main/assets/trustedDomains.json)

**Section sources**
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [trustedDomains.json](file://catroid/src/main/assets/trustedDomains.json)

### Input Validation, SQL Injection Prevention, XSS Protection
- Validate inputs at boundaries (network, UI, file parsing).
- Use parameterized queries or ORM to prevent SQL injection.
- Escape or sanitize outputs to prevent XSS.

```mermaid
flowchart TD
In["User Input"] --> V["Validate Format & Range"]
V --> Sanitize["Sanitize / Escape"]
Sanitize --> Query["Parameterized DB Query"]
Query --> Render["Safe Rendering / Output"]
```

[No sources needed since this diagram shows conceptual workflow, not actual code structure]

**Section sources**
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)

### Secure API Design Patterns
- Centralize endpoints and request/response models.
- Apply consistent authentication headers and error codes.
- Version APIs and deprecate insecure versions.

```mermaid
classDiagram
class NeoCatroidApi {
+getProjects() Response
+createProject(data) Response
+updateProject(id, data) Response
+deleteProject(id) Response
}
class AuthInterceptor {
+attachToken(request) Request
}
NeoCatroidApi --> AuthInterceptor : "uses"
```

**Diagram sources**
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)

**Section sources**
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)

### Network Security Configuration
- Define network security policy to block cleartext traffic and enforce TLS.
- Optionally whitelist specific domains for development or legacy services.

```mermaid
flowchart TD
Start(["Outbound Request"]) --> CheckPolicy["Apply Network Security Policy"]
CheckPolicy --> Allowed{"Allowed by Policy?"}
Allowed -- No --> Block["Block Request"]
Allowed -- Yes --> TrustList["Check trustedDomains.json"]
TrustList --> DomainOK{"Domain Trusted?"}
DomainOK -- No --> Block
DomainOK -- Yes --> Proceed["Send HTTPS/TLS Request"]
```

**Diagram sources**
- [AndroidManifest.xml](file://catroid/src/main/AndroidManifest.xml)
- [trustedDomains.json](file://catroid/src/main/assets/trustedDomains.json)

**Section sources**
- [AndroidManifest.xml](file://catroid/src/main/AndroidManifest.xml)
- [trustedDomains.json](file://catroid/src/main/assets/trustedDomains.json)

### OAuth Authentication Flow and Session Management
- Implement PKCE-based OAuth for secure authorization.
- Use short-lived access tokens and refresh tokens securely stored in Keystore.
- Invalidate sessions on logout and handle token expiration gracefully.

```mermaid
sequenceDiagram
participant App as "NewCatroid"
participant Auth as "OAuth Provider"
participant Store as "Keystore"
App->>Auth : Authorization request (PKCE)
Auth-->>App : Authorization code
App->>Auth : Exchange code for tokens
Auth-->>App : Access token + Refresh token
App->>Store : Save tokens securely
App->>Auth : Use access token for API calls
Note over App,Auth : On expiry, use refresh token to obtain new access token
```

[No sources needed since this diagram shows conceptual workflow, not actual code structure]

**Section sources**
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)

### Data Encryption at Rest and in Transit
- At rest: Encrypt sensitive files and preferences using Keystore-backed ciphers.
- In transit: Enforce HTTPS/TLS across all network requests; validate certificates.

```mermaid
flowchart TD
Data["Sensitive Data"] --> EncryptAtRest["Encrypt with Keystore Key"]
EncryptAtRest --> Persist["Persist Encrypted Blob"]
Data --> TLS["TLS Channel"]
TLS --> Transmit["Transmit Over Network"]
```

[No sources needed since this diagram shows conceptual workflow, not actual code structure]

**Section sources**
- [AndroidManifest.xml](file://catroid/src/main/AndroidManifest.xml)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)

## Dependency Analysis
Security-related dependencies span build-time, runtime, and CI/CD layers. The diagram highlights how signing, hardening, and network policies interact.

```mermaid
graph TB
Gradle["build.gradle"] --> Signing["Signing Config"]
Gradle --> R8["R8/ProGuard Rules"]
R8 --> APK["Hardened APK"]
Manifest["AndroidManifest.xml"] --> Perm["Permissions"]
Manifest --> NetSec["Network Security Policy"]
NetSvc["NetworkService.kt"] --> TLS["TLS Enforcement"]
Api["NeoCatroidApi.java"] --> Auth["Auth Headers"]
Domains["trustedDomains.json"] --> Allowlist["Domain Allowlist"]
Jenkins["Jenkinsfile.releaseAPK"] --> CI["Release Pipeline"]
Fastlane["Fastfile"] --> Deploy["Distribution"]
```

**Diagram sources**
- [build.gradle](file://catroid/build.gradle)
- [proguard-project.txt](file://catroid/proguard-project.txt)
- [proguard-runtime.pro](file://catroid/proguard-runtime.pro)
- [AndroidManifest.xml](file://catroid/src/main/AndroidManifest.xml)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [trustedDomains.json](file://catroid/src/main/assets/trustedDomains.json)
- [Jenkinsfile.releaseAPK](file://Jenkinsfile.releaseAPK)
- [Fastfile](file://fastlane/Fastfile)

**Section sources**
- [build.gradle](file://catroid/build.gradle)
- [proguard-project.txt](file://catroid/proguard-project.txt)
- [proguard-runtime.pro](file://catroid/proguard-runtime.pro)
- [AndroidManifest.xml](file://catroid/src/main/AndroidManifest.xml)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [trustedDomains.json](file://catroid/src/main/assets/trustedDomains.json)
- [Jenkinsfile.releaseAPK](file://Jenkinsfile.releaseAPK)
- [Fastfile](file://fastlane/Fastfile)

## Performance Considerations
- Minimize overhead from frequent cryptographic operations by caching encrypted blobs and reusing cipher instances where safe.
- Keep TLS handshake costs low by enabling connection reuse and appropriate timeouts.
- Balance R8 optimizations with feature stability; review logs after enabling aggressive shrinking.

[No sources needed since this section provides general guidance]

## Troubleshooting Guide
- If network requests fail due to policy, verify that the target domain is present in the trusted domains list and that HTTPS is enforced.
- If signing fails during release, check keystore paths and credentials in build and CI configurations.
- If features require permissions, ensure runtime permission prompts are handled and fallback behavior is clear.

**Section sources**
- [trustedDomains.json](file://catroid/src/main/assets/trustedDomains.json)
- [build.gradle](file://catroid/build.gradle)
- [Jenkinsfile.releaseAPK](file://Jenkinsfile.releaseAPK)

## Conclusion
NewCatroid’s security posture combines robust build-time protections (signing, hardening), strict runtime controls (permissions, Keystore, TLS), and automated CI/CD processes. By enforcing least privilege, securing storage and communications, validating inputs, and following secure API patterns, the application mitigates common vulnerabilities and aligns with OWASP guidelines. Continuous security testing and monitoring further strengthen resilience.

[No sources needed since this section summarizes without analyzing specific files]

## Appendices

### OWASP Compliance Checklist
- A01 Broken Access Control: Enforce server-side checks; limit client-side enforcement.
- A02 Cryptographic Failures: Use Keystore and TLS; rotate keys periodically.
- A03 Injection: Parameterized queries; sanitize inputs.
- A04 Insecure Design: Threat model and secure defaults.
- A05 Security Misconfiguration: Harden manifests, disable debug flags in release.
- A06 Vulnerable Components: Update dependencies; monitor advisories.
- A07 Identification and Authentication Flaws: PKCE OAuth; short-lived tokens.
- A08 Software and Data Integrity Failures: Verify signatures; integrity checks.
- A09 Security Logging and Monitoring: Log security events; alert on anomalies.
- A10 SSRF: Validate URLs; restrict outbound calls.

[No sources needed since this section provides general guidance]

### Security Testing Methodologies
- Static analysis: Integrate lint, PMD, Detekt into CI.
- Dynamic analysis: Fuzz inputs; test TLS and certificate validation.
- Penetration testing: Focus on auth flows, API endpoints, and storage.
- Dependency scanning: Track known vulnerabilities in third-party libraries.

[No sources needed since this section provides general guidance]