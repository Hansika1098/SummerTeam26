# Sorter Software Work Distribution

## Purpose

This document explains one reasonable way to divide the sorter software work into two balanced
workstreams.

Unlike the main contract document, this file is about **implementation planning** rather than the
interface behavior itself.

---

## Recommended split

### Workstream A: load-station observation

This workstream centers on the internal `LoadStationObserver` interface and the concrete
`DualColorLoadStationObserver` implementation.

Primary responsibilities:

- reading both color sensors,
- separator detection for calibration support,
- distinguishing separator / slot base / artifact views,
- tuning sensor thresholds or heuristics,
- latching artifact presence across a slot pass,
- latching color across a slot pass,
- producing one final `SlotReadResult` for each completed pass,
- exposing useful debug telemetry for tuning and test op-modes.

Primary files:

- `LoadStationObserver.java`
- `DualColorLoadStationObserver.java`
- `ReferenceDetection.java`
- `SlotReadResult.java`
- `ObserverDebug.java`
- `SurfaceKind.java`

Typical deliverables:

- stable sensor interpretation behavior,
- repeatable pass results,
- tuned thresholds and debug outputs,
- test results for separator, empty/base, and color detection.

### Workstream B: sorter control and inventory

This workstream centers on the public `Sorter` interface and the concrete `SorterImpl`
implementation.

Primary responsibilities:

- public subsystem behavior,
- calibration state machine,
- spinner reference management,
- mapping angle to logical slot/station positions,
- load and shoot station alignment,
- authoritative slot inventory,
- inventory scan flow,
- feed and presentation actions,
- recovery actions,
- interpreting `CommandResult` and `SorterStatus` semantics correctly.

Primary files:

- `Sorter.java`
- `SorterImpl.java`
- `SorterStatus.java`
- `SlotContent.java`
- `Station.java`
- `CalibrationState.java`
- `SorterAction.java`
- `CommandResult.java`

Typical deliverables:

- stable non-blocking subsystem behavior,
- repeatable calibration and station mapping,
- correct slot ownership and update rules,
- reliable integration with intake and shooter logic.

---

## Why this split is balanced

The two workstreams are different in character, but both are substantial.

### Observation work is not a thin hardware wrapper

It includes:

- real-time signal interpretation,
- robustness to artifact holes and rolling,
- robustness to lighting and gain changes,
- calibration support through separator detection,
- pass-level state management and latching,
- conservative fallback behavior when evidence is ambiguous.

### Sorter control work is more than a motion helper

It includes:

- state-machine design,
- calibration flow,
- persistent inventory ownership,
- station mapping correctness,
- command rejection semantics,
- scan/recovery behavior,
- shooter/load integration semantics.

The split is therefore fairly even in depth, testing burden, and implementation complexity.

---

## Shared seam that should stay stable

These files define the boundary between the two workstreams and should be treated carefully:

- `Sorter.java`
- `LoadStationObserver.java`
- `SlotReadResult.java`
- `ReferenceDetection.java`
- `SlotContent.java`
- `CommandResult.java`

Changes here should be discussed before either implementation diverges from the shared contract.

---

## Collaboration model

A practical way to work in parallel is:

1. agree on the seam interfaces first,
2. keep the seam stable unless there is a strong reason to change it,
3. let each workstream build and test independently,
4. integrate first with simple or fake implementations,
5. only then tune thresholds and mechanism behavior together.

### Useful early integration trick

A fake observer implementation can unblock sorter-control development before real sensor tuning is
finished.

For example, a test double can return scripted results such as:

- slot 0 empty,
- slot 1 purple,
- slot 2 green.

That lets sorter-control code be developed and tested independently of the real dual-sensor logic.

Likewise, the observer workstream can be tested in its own dedicated op-mode without requiring the
full sorter state machine to be finished.

---

## Suggested milestone order

### Milestone 1: seam and telemetry

- finalize the shared interfaces,
- confirm method semantics,
- expose enough debug telemetry for both workstreams.

### Milestone 2: independent bring-up

- observer workstream validates separator / empty / artifact / color behavior,
- sorter workstream validates calibration flow, slot mapping, and command semantics.

### Milestone 3: end-to-end scan

- integrate both implementations,
- verify `startInventoryScan()` and passive pass updates,
- confirm that slot truth matches known physical contents.

### Milestone 4: shooter and recovery integration

- verify `tryPresentSlot(...)`, `tryFeed()`, and `confirmShotComplete()`,
- verify `markInventoryUnknown()` and `clearInventory()` behavior.

---

## Acceptance criteria for each workstream

### Observation workstream

Done means:

- separator detection is repeatable,
- empty/base false positives are acceptably low,
- color latching is repeatable,
- one pass produces one final result,
- debug telemetry is sufficient to diagnose failures.

### Sorter control workstream

Done means:

- commands are non-blocking and correctly gated,
- calibration is repeatable,
- slot/station mapping is correct,
- inventory scan works,
- slot table updates only on valid events,
- recovery paths behave conservatively.

---

## What someone reading only the code contract can safely ignore

Someone who just wants to understand how the sorter code works does not need this file.

They only need:

- the Javadocs in the source files,
- `sorter-interface-contract.md`.

This file is only for planning, ownership clarity, and keeping the implementation workload balanced.
