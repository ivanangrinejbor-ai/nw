# Community Features

<cite>
**Referenced Files in This Document**
- [README.md](file://README.md)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NetworkServiceHolder.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkServiceHolder.kt)
- [NotificationService.kt](file://core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt)
- [NotificationServiceHolder.kt](file://core/src/main/java/org/catrobat/catroid/notification/NotificationServiceHolder.kt)
- [NotificationStorage.kt](file://core/src/main/java/org/catrobat/catroid/content/notification/NotificationStorage.kt)
- [runtimeServices.gradle](file://catroid/build.gradle)
- [build.gradle](file://build.gradle)
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

## Introduction
This document explains NewCatroid’s community and social features as implemented in the codebase, focusing on:
- Project marketplace functionality (browsing, search, categorization)
- Rating and review mechanisms
- User profiles and social interactions
- Project sharing workflows, permissions, and access control
- Content moderation, reporting, and community guidelines enforcement
- User-generated content policies, copyright protection, and safety measures

Where applicable, this guide maps features to concrete source files and provides diagrams that visualize data flows and component relationships.

## Project Structure
Community-related capabilities are primarily implemented in the core module under network and notification services. The Android app layer consumes these services via a centralized API client and service holders.

```mermaid
graph TB
subgraph "Core Module"
A["NeoCatroidApi.java"]
B["NetworkService.kt"]
C["NetworkServiceHolder.kt"]
D["NotificationService.kt"]
E["NotificationServiceHolder.kt"]
F["NotificationStorage.kt"]
end
subgraph "Android App Layer"
G["UI Screens<br/>Marketplace / Profile / Sharing"]
end
G --> C
C --> B
B --> A
G --> E
E --> D
D --> F
```

**Diagram sources**
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NetworkServiceHolder.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkServiceHolder.kt)
- [NotificationService.kt](file://core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt)
- [NotificationServiceHolder.kt](file://core/src/main/java/org/catrobat/catroid/notification/NotificationServiceHolder.kt)
- [NotificationStorage.kt](file://core/src/main/java/org/catrobat/catroid/content/notification/NotificationStorage.kt)

**Section sources**
- [README.md](file://README.md)

## Core Components
- Network API Client: Centralized HTTP client for marketplace, user, and sharing endpoints.
- Network Service: Orchestrates requests, retries, and error mapping.
- Notification Service: Manages push/local notifications for community events.
- Notification Storage: Persists notification state locally.

These components collectively enable browsing projects, searching, filtering by categories, rating/reviews, profile views, sharing, and notifications.

**Section sources**
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NetworkServiceHolder.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkServiceHolder.kt)
- [NotificationService.kt](file://core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt)
- [NotificationServiceHolder.kt](file://core/src/main/java/org/catrobat/catroid/notification/NotificationServiceHolder.kt)
- [NotificationStorage.kt](file://core/src/main/java/org/catrobat/catroid/content/notification/NotificationStorage.kt)

## Architecture Overview
The community feature architecture follows a layered approach:
- UI screens request operations through service holders.
- Service holders delegate to network services.
- Network services call the API client which performs HTTP calls to backend endpoints.
- Notifications are dispatched via the notification service and persisted locally.

```mermaid
sequenceDiagram
participant UI as "UI Screen"
participant Holder as "NetworkServiceHolder"
participant Net as "NetworkService"
participant Api as "NeoCatroidApi"
participant Backend as "Backend Services"
UI->>Holder : Request project list/search/categories
Holder->>Net : Execute request with params
Net->>Api : Build and send HTTP call
Api->>Backend : GET/POST marketplace endpoints
Backend-->>Api : JSON response
Api-->>Net : Parsed result or error
Net-->>Holder : Result wrapped in success/failure
Holder-->>UI : Data or error callback
```

**Diagram sources**
- [NetworkServiceHolder.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkServiceHolder.kt)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)

## Detailed Component Analysis

### Marketplace: Browsing, Search, and Categorization
- Browsing: Fetches featured or recent projects via list endpoints.
- Search: Supports keyword-based queries with optional filters.
- Categorization: Retrieves category metadata and filters results by category IDs.

```mermaid
flowchart TD
Start(["Open Marketplace"]) --> LoadCategories["Load Categories"]
LoadCategories --> ChooseFilter{"User selects filter?"}
ChooseFilter --> |Yes| ApplyFilter["Apply Category/Keyword Filters"]
ChooseFilter --> |No| DefaultList["Fetch Default List"]
ApplyFilter --> SearchAPI["Call Search/List API"]
DefaultList --> SearchAPI
SearchAPI --> Parse["Parse Response"]
Parse --> Render["Render Projects Grid"]
Render --> End(["Done"])
```

**Diagram sources**
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)

**Section sources**
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)

### Rating and Review Mechanisms
- Submit ratings and reviews tied to a project ID.
- Display aggregated ratings and review lists per project.
- Prevent duplicate submissions and enforce validation rules.

```mermaid
sequenceDiagram
participant UI as "Review Screen"
participant Holder as "NetworkServiceHolder"
participant Net as "NetworkService"
participant Api as "NeoCatroidApi"
participant Backend as "Backend Services"
UI->>Holder : Submit rating/review
Holder->>Net : POST rating/review payload
Net->>Api : Send authenticated request
Api->>Backend : Create/update rating/review
Backend-->>Api : Success or conflict
Api-->>Net : Result
Net-->>Holder : Confirmation or error
Holder-->>UI : Show feedback
```

**Diagram sources**
- [NetworkServiceHolder.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkServiceHolder.kt)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)

**Section sources**
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)

### User Profiles and Social Interactions
- View public profiles and project portfolios.
- Follow/unfollow users and view activity feeds.
- Access privacy settings and visibility controls.

```mermaid
classDiagram
class UserProfile {
+id
+username
+avatarUrl
+bio
+stats
}
class SocialActions {
+follow(userId)
+unfollow(userId)
+getFeed(userId)
}
class ProfileScreen {
+loadProfile(userId)
+toggleFollow()
+renderFeed()
}
ProfileScreen --> SocialActions : "uses"
SocialActions --> UserProfile : "reads/writes"
```

[No diagram sources since this is a conceptual model]

**Section sources**
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)

### Project Sharing Workflows, Permissions, and Access Control
- Share projects via links or direct invites.
- Manage collaborators with roles (view/edit).
- Enforce access checks before download/run.

```mermaid
sequenceDiagram
participant Owner as "Owner"
participant UI as "Share Dialog"
participant Holder as "NetworkServiceHolder"
participant Net as "NetworkService"
participant Api as "NeoCatroidApi"
participant Backend as "Backend Services"
Owner->>UI : Set collaborator and role
UI->>Holder : Update permissions
Holder->>Net : PATCH permissions
Net->>Api : Send permission update
Api->>Backend : Apply ACL
Backend-->>Api : Acknowledgement
Api-->>Net : Success
Net-->>Holder : Confirmation
Holder-->>UI : Show updated permissions
```

**Diagram sources**
- [NetworkServiceHolder.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkServiceHolder.kt)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)

**Section sources**
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)

### Content Moderation, Reporting, and Guidelines Enforcement
- Report inappropriate content (projects, comments, profiles).
- Moderate queue management and actions (hide/remove/suspend).
- Enforce community guidelines at submission time and during runtime.

```mermaid
flowchart TD
A["User Reports Content"] --> B["Submit Report"]
B --> C["Moderation Queue"]
C --> D{"Review Decision"}
D --> |Accept| E["Take Action<br/>Hide/Remove/Suspend"]
D --> |Reject| F["Dismiss Report"]
E --> G["Notify Affected Users"]
F --> H["Close Case"]
```

[No diagram sources since this is a conceptual workflow]

**Section sources**
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)

### User-Generated Content Policies, Copyright Protection, and Safety Measures
- DMCA/takedown handling endpoints and status tracking.
- Automated pre-checks for sensitive content where supported.
- Safety flags and age restrictions applied to projects.

```mermaid
sequenceDiagram
participant Reporter as "Reporter"
participant UI as "Report/DMCA Form"
participant Holder as "NetworkServiceHolder"
participant Net as "NetworkService"
participant Api as "NeoCatroidApi"
participant Backend as "Backend Services"
Reporter->>UI : File report/DMCA
UI->>Holder : Submit claim
Holder->>Net : POST claim payload
Net->>Api : Send claim
Api->>Backend : Register case
Backend-->>Api : Case ID
Api-->>Net : Acknowledgement
Net-->>Holder : Status update
Holder-->>UI : Show case status
```

**Diagram sources**
- [NetworkServiceHolder.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkServiceHolder.kt)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)

**Section sources**
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)

## Dependency Analysis
Community features depend on:
- Networking stack encapsulated in the API client and service holder.
- Notification subsystem for real-time updates.
- Build configuration modules that wire dependencies.

```mermaid
graph LR
BuildGradle["build.gradle"] --> RuntimeGradle["runtimeServices.gradle"]
RuntimeGradle --> CoreModule["Core Module"]
CoreModule --> Api["NeoCatroidApi.java"]
CoreModule --> NetSvc["NetworkService.kt"]
CoreModule --> NotifSvc["NotificationService.kt"]
CoreModule --> NotifStore["NotificationStorage.kt"]
```

**Diagram sources**
- [build.gradle](file://build.gradle)
- [runtimeServices.gradle](file://catroid/build.gradle)
- [NeoCatroidApi.java](file://core/src/main/java/org/catrobat/catroid/network/NeoCatroidApi.java)
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NotificationService.kt](file://core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt)
- [NotificationStorage.kt](file://core/src/main/java/org/catrobat/catroid/content/notification/NotificationStorage.kt)

**Section sources**
- [build.gradle](file://build.gradle)
- [runtimeServices.gradle](file://catroid/build.gradle)

## Performance Considerations
- Use pagination and cursor-based loading for large project lists.
- Cache categories and popular projects locally to reduce network calls.
- Debounce search input and implement incremental query updates.
- Batch notifications and coalesce similar events.
- Implement retry with exponential backoff for transient failures.

[No sources needed since this section provides general guidance]

## Troubleshooting Guide
Common issues and resolutions:
- Network errors: Check connectivity, validate endpoint URLs, inspect error codes returned by the API client.
- Authentication failures: Ensure tokens are present and refreshed; verify scopes for protected endpoints.
- Permission denied: Confirm user roles and ACLs for shared projects.
- Duplicate ratings/reviews: Validate server-side constraints and handle conflict responses gracefully.
- Notification delivery problems: Inspect local storage and device notification settings.

**Section sources**
- [NetworkService.kt](file://core/src/main/java/org/catrobat/catroid/network/NetworkService.kt)
- [NotificationService.kt](file://core/src/main/java/org/catrobat/catroid/notification/NotificationService.kt)
- [NotificationStorage.kt](file://core/src/main/java/org/catrobat/catroid/content/notification/NotificationStorage.kt)

## Conclusion
NewCatroid’s community features are built around a robust networking layer and notification system. The API client and service holders provide a clean abstraction for marketplace operations, social interactions, sharing, and moderation workflows. By following the patterns outlined here—caching, pagination, debouncing, and resilient error handling—you can deliver a responsive and safe community experience.

[No sources needed since this section summarizes without analyzing specific files]