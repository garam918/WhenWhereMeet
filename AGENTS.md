# AGENTS.md

# Project Agent Instructions

This repository is a Compose Multiplatform mobile app project for Android and iOS.

The app helps groups decide:

1. when to meet, by collecting available dates from participants,
2. where to meet, by recommending fair meeting areas based on each participant's travel time,
3. which exact place to choose, by letting participants vote on place candidates.

The assistant working in this repository must prioritize correctness, maintainability, privacy, and incremental delivery.

---

## Product Summary

The app is a group meeting scheduler.

Core product concept:

> Pick the best date and the least inconvenient meeting place for everyone.

Main user flow:

1. Host creates a meeting room.
2. Participants join by link or room code.
3. Participants enter their available dates.
4. The app recommends the best dates.
5. Host confirms the meeting date.
6. Participants enter their start locations and transport modes.
7. The app recommends fair meeting areas.
8. Host selects a meeting area.
9. The app suggests real place candidates.
10. Participants vote.
11. Host confirms the final date and place.

---

## Development Phases

The project must be developed incrementally.

### Phase 1: Date Scheduling MVP

Allowed scope:

* Meeting room creation
* Guest participation with nickname
* Date range or month selection
* Availability input
* Availability calendar
* Recommended date TOP 3
* Date confirmation
* Basic share text
* Fake or real repository depending on current project setup

Not allowed in Phase 1:

* Real map API
* Real place search
* Travel time calculation
* Push notifications
* Calendar integration
* Paid features

### Phase 2: Fair Meeting Area Recommendation

Allowed scope:

* Participant start location input
* Transport mode selection
* Location privacy model
* Fake location search provider
* Fake travel time provider
* Meeting area candidate model
* Fairness score calculation
* Recommended meeting area TOP 3
* Host area selection

Not allowed in Phase 2:

* Real Kakao/Naver/Google API calls unless explicitly requested
* Exact restaurant/cafe recommendations
* Place voting
* Final place confirmation

### Phase 3: Place Candidate Search and Voting

Allowed scope:

* PlaceSearchProvider interface
* FakePlaceSearchProvider
* Optional real API implementation only if project configuration already supports it
* Place candidates around selected area
* MeetingType to PlaceCategory mapping
* Place scoring
* Place voting
* Final place confirmation
* Map URL opening
* Final share text

Not allowed in Phase 3 unless explicitly requested:

* Push notifications
* Calendar integration
* Friend graph
* Subscription/paywall
* Admin dashboard

---

## Mandatory Work Protocol

Before changing code, always do the following:

1. Read this `AGENTS.md`.
2. Inspect the current repository structure.
3. Identify the current architecture, package structure, dependencies, and build system.
4. Summarize:

   * current structure,
   * files to add or modify,
   * implementation plan,
   * risks,
   * testing strategy.
5. Then implement the smallest safe change set.

Do not rewrite the entire project unless explicitly requested.

Do not introduce a new architecture if the existing architecture is already reasonable.

Prefer small, composable changes.

---

## Architecture Rules

Prefer a clean layered structure.

Recommended structure:

```text
shared/
  src/commonMain/kotlin/
    domain/
      model/
      repository/
      usecase/
    data/
      repository/
      source/
      provider/
    presentation/
      screen/
      component/
      state/
      viewmodel/
    platform/
      share/
      location/
      map/
```

If the existing project has a different but reasonable structure, follow the existing structure.

Business logic must not live inside Composable functions.

Use cases should contain pure logic where possible.

Repository interfaces should live in the domain layer.

External API or SDK implementations should live behind interfaces.

---

## Compose Multiplatform Rules

Use shared Compose UI when possible.

Use platform-specific implementations only when necessary.

Platform-specific functionality must be hidden behind interfaces or expect/actual declarations.

Examples:

* ShareService
* LocationProvider
* MapLauncher
* PlaceSearchProvider
* TravelTimeProvider
* DeepLinkHandler

Do not place Android-only or iOS-only code inside commonMain.

Do not hardcode Android Context into shared code.

---

## State Management Rules

Use explicit UI state models.

Recommended pattern:

```kotlin
data class SomeUiState(...)
sealed interface SomeUiAction
sealed interface SomeUiEvent
```

Composable functions should render state and send actions.

ViewModels or state holders should handle actions.

Avoid complex mutable state scattered across multiple Composables.

Avoid putting network, database, or scoring logic inside UI files.

---

## Domain Model Guidelines

Use clear domain models.

Core models may include:

* MeetingRoom
* Participant
* Availability
* AvailabilityStatus
* MeetingStatus
* MeetingType
* UserStartLocation
* TransportMode
* MeetingAreaCandidate
* AreaRecommendation
* ParticipantTravelTime
* PlaceCandidate
* ScoredPlaceCandidate
* PlaceVote
* PlaceVoteType

Use `LocalDate` for dates where possible.

Use `Instant` for createdAt/updatedAt timestamps where possible.

If the project already standardizes another date/time type, follow the existing standard.

---

## Date Recommendation Rules

Date recommendation logic must be testable as pure Kotlin logic.

Default scoring:

* AVAILABLE = 2 points
* MAYBE = 1 point
* UNAVAILABLE = 0 points

Recommended sorting:

1. Higher total score
2. More AVAILABLE participants
3. Fewer MAYBE participants
4. Required participants available
5. Fewer missing responses

The recommendation result should include a human-readable reason.

Example:

```text
5명 중 5명이 가능한 날이에요.
필수 참석자가 모두 가능한 날이에요.
```

---

## Fair Area Recommendation Rules

Do not use straight-line distance as the final product concept.

The app should be designed around estimated travel time.

In early phases, FakeTravelTimeProvider may estimate travel time from distance.

The interface must allow replacement with a real provider later.

Area scoring should consider:

* average travel time,
* maximum travel time,
* travel time variance,
* meeting type bonus,
* penalty when one participant has a much worse travel time than others.

The recommendation result must include explanation text.

Example:

```text
5명 중 4명이 45분 이내 도착할 수 있어요.
가장 오래 걸리는 사람도 50분 이내예요.
```

---

## Place Recommendation Rules

Place search must be provider-based.

Do not bind UI directly to Kakao, Naver, or Google response models.

Map external API responses into internal domain models first.

Place scoring may consider:

* distance from selected area center,
* category match,
* rating,
* review count,
* opening hours availability,
* map URL availability,
* vote score.

Place voting must be separated from place search.

---

## Privacy Rules

Location privacy is critical.

Never expose exact participant coordinates to other participants.

Do not show exact home addresses to other users.

Show only:

* participant nickname,
* approximate area label,
* estimated travel time.

Never log:

* API keys,
* exact coordinates,
* full address,
* personal location data.

Do not store sensitive personal information unless necessary.

If precise location is stored for route calculation, isolate it in the data layer and avoid exposing it directly to UI state.

---

## API Key and Secret Rules

Never hardcode API keys.

Never commit secrets.

Use one of the following:

* local properties,
* environment variables,
* Gradle secrets,
* Firebase Remote Config only if appropriate,
* backend proxy if needed.

If a real API integration requires secrets and the project does not already provide a safe configuration, create an interface and Fake implementation instead.

Leave clear TODO comments for required environment variables.

---

## Firebase Rules

Use Firebase only if the project is already configured or the user explicitly asks for it.

When using Firebase:

* Keep Firestore models separate from domain models when necessary.
* Map DTOs to domain models.
* Avoid leaking Firebase APIs into presentation code.
* Handle loading, empty, and error states.
* Avoid excessive reads and writes.
* Batch writes where reasonable.

Do not assume Firebase Auth is required.

Guest participation must work without mandatory sign-up.

---

## Testing Rules

Prefer pure Kotlin tests for business logic.

Must test:

* date recommendation usecase,
* availability aggregation,
* required participant logic,
* area recommendation scoring,
* travel time statistics,
* place vote aggregation,
* final confirmation state transitions.

If UI testing is too heavy, prioritize domain/usecase tests.

When adding tests, explain how to run them.

---

## Harness Engineering Rules

Use harness engineering to make agent-driven development safer and more repeatable.

For every complex feature, create or maintain small test harnesses or fixtures that allow the feature to be verified without real external services.

Examples:

* FakeMeetingRoomRepository
* FakeLocationSearchProvider
* FakeTravelTimeProvider
* FakePlaceSearchProvider
* SampleMeetingRooms
* SampleParticipants
* SampleAvailabilityResponses
* SampleAreaCandidates
* SamplePlaceCandidates

Business logic should be verifiable with deterministic sample data.

Do not depend on real map APIs, real Firebase, or real network calls for core logic tests.

When implementing a feature, prefer this flow:

1. Define domain model.
2. Define interface.
3. Add fake implementation.
4. Add deterministic sample data.
5. Add usecase.
6. Add tests.
7. Connect to UI.
8. Add real implementation only if configuration is safe.

---

## UI/UX Rules

Use Korean text for user-facing copy unless the project already supports localization.

Keep screens simple and mobile-first.

Important UX principles:

* Guest users should be able to join quickly.
* Date selection should require minimal taps.
* Recommendation reasons should be visible.
* Host-only actions should be clearly marked.
* Empty states should explain what to do next.
* Error states should be recoverable.

Recommended main screens:

* HomeScreen
* CreateMeetingRoomScreen
* JoinRoomScreen
* MeetingRoomScreen
* AvailabilityCalendar
* LocationInputScreen or bottom sheet
* AreaRecommendationSection
* PlaceCandidateSection
* ConfirmedMeetingCard

---

## Design System Rules

Follow the existing design system if present.

If no design system exists, use Material 3.

Use reusable components for:

* primary button,
* secondary button,
* empty state,
* error state,
* loading state,
* date cell,
* recommendation card,
* participant row,
* place card.

Avoid duplicating UI code across screens.

---

## Error Handling Rules

All async operations must handle:

* loading,
* success,
* empty,
* error.

Do not silently fail.

Show user-friendly Korean error messages.

Examples:

```text
약속방을 불러오지 못했어요. 다시 시도해주세요.
위치 정보를 저장하지 못했어요.
추천 장소를 계산할 수 없어요. 출발 위치를 확인해주세요.
```

---

## Code Style Rules

Follow the existing code style.

Prefer clear names over abbreviations.

Avoid over-engineering.

Avoid very large files.

Avoid large Composable functions.

Split components when a Composable becomes hard to read.

Do not leave unused code.

Do not introduce unnecessary dependencies.

---

## Dependency Rules

Before adding a new dependency:

1. Check whether an existing dependency can solve the problem.
2. Explain why the new dependency is needed.
3. Prefer stable, widely used libraries.
4. Avoid adding heavy dependencies for small utilities.

Do not add map SDKs or Firebase dependencies unless the phase and task explicitly require them.

---

## Build and Verification

After making changes, run the smallest relevant verification available.

Examples:

```bash
./gradlew build
./gradlew test
./gradlew :shared:test
./gradlew :composeApp:assembleDebug
```

Use commands that match the current repository.

If a command fails because of environment limitations, report:

* command,
* failure reason,
* whether the failure is caused by code or environment.

---

## Output Format After Each Task

After completing a task, always report:

1. Summary
2. Changed files
3. Key implementation details
4. Tests or verification run
5. Known limitations
6. Next recommended step

Do not claim tests passed if they were not run.

Do not claim real API integration works if only Fake implementation was added.

---

## Prohibited Actions

Do not:

* hardcode API keys,
* expose exact user coordinates to other participants,
* rewrite the entire project without permission,
* add unnecessary dependencies,
* introduce real network calls into tests,
* mix platform-specific code into commonMain,
* put business logic inside Composables,
* remove existing features without explanation,
* change package names broadly without need,
* make destructive changes without explicit instruction.

---

## Preferred Implementation Strategy

For new features, follow this order:

1. Domain model
2. Repository or provider interface
3. Fake implementation
4. Usecase
5. Unit tests
6. UI state
7. ViewModel/state holder
8. Compose UI
9. Navigation
10. Documentation or TODOs for real API integration

This project should stay easy to extend from Phase 1 to Phase 3.
