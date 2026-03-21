# Sorter Interface Contract

## Purpose

This document explains the contract between the public `Sorter` interface and the internal
`LoadStationObserver` interface.

The goal of the contract is to make the sorter code understandable, testable, and stable even as
sensor tuning and mechanism tuning change over time.

This document is about **how the interfaces interact** and **what behaviors they must provide**.
A separate document discusses how the implementation work can be divided across contributors.

---

## Why this contract fits the DECODE sorter problem

For this project, the sorter has to do four different jobs reliably:

- establish a trusted angular reference for the spinner,
- determine what each slot currently contains,
- position slots at the load and shooter stations,
- update inventory only when the robot has enough evidence to do so safely.

That means the software needs a clean separation between:

- **mechanism geometry and slot ownership**, and
- **dual color sensor interpretation at the load station**.

The current contract keeps that separation clear:

- `Sorter` is the subsystem contract used by the rest of the robot.
- `LoadStationObserver` is the internal collaborator that interprets the dual color sensors.

---

## Interface boundary

### `Sorter` is the public subsystem contract

The rest of the robot should depend on `Sorter` and its public data types.

`Sorter` is responsible for:

- command semantics,
- non-blocking subsystem actions,
- calibration state,
- current mechanism action/state,
- authoritative slot inventory,
- station mapping for `LOAD` and `SHOOT`,
- inventory scan and recovery actions,
- applying confirmed shot completion to the slot table.

`Sorter` is the **single source of truth** for long-lived slot contents.

### `LoadStationObserver` is an internal collaborator

`LoadStationObserver` is not the robot-facing API. It is the interface between sorter control logic
and the dual color sensors mounted over the load station.

`LoadStationObserver` is responsible for:

- reading and fusing the two color sensors,
- separator detection during calibration,
- distinguishing separator / slot base / artifact-like readings,
- latching evidence during a slot pass,
- returning one final `SlotReadResult` at the end of that pass,
- exposing debug information for tuning.

The observer does **not** own permanent slot memory. It only produces the result of the
**current calibration sample** or the **current slot pass**.

### `ObserverDebug` is for tuning, not subsystem control

`ObserverDebug` is useful for telemetry, tuning, and test op-modes. It should not be treated as the
control interface for the rest of the robot.

The robot should make control decisions from:

- `SorterStatus`,
- `getSlotContent(...)`,
- `getSlotAt(...)`,
- `canLoad()`,
- `canFeed()`,
- `CommandResult` from sorter commands.

---

## Core contract rules

### 1. `Sorter` owns all long-lived slot truth

Only `Sorter` stores and updates the committed slot table.

`LoadStationObserver` may say:

- separator detected,
- slot pass ended empty,
- slot pass ended purple,
- slot pass ended green,
- slot pass ended occupied but color unknown.

But only `Sorter` commits that result into inventory.

### 2. `Sorter` owns coarse geometry

`Sorter` owns:

- spinner angle/reference,
- whether calibration is trusted,
- which slot is at `LOAD`,
- which slot is at `SHOOT`,
- whether a slot is inside the coarse read zone at the load station.

The observer should not be responsible for spinner geometry or station mapping.

### 3. `LoadStationObserver` owns within-pass sensor interpretation

During an active slot pass, the observer decides things like:

- whether the current frames look like separator, slot base, or artifact,
- whether artifact presence should latch,
- whether a color should latch,
- what the final pass result should be.

The observer retains transient state only for the **active pass**.

### 4. Commands are non-blocking

Sorter command methods do not wait for completion.

A call such as `startCalibration()`, `tryPrepareLoad()`, `tryPresentSlot(...)`, `tryFeed()`, or
`startInventoryScan()` should:

- either accept the request and begin the action, or
- reject the request with a clear reason.

Actual progress happens later through repeated calls to `update()`.

### 5. Inventory may be unknown

The interface must support cases where inventory is not currently trusted.

That includes:

- initial startup before a scan,
- post-jam recovery,
- manual intervention,
- any loss of synchronization between physical slots and software state.

This is why `SlotContent` includes `UNKNOWN` and `OCCUPIED_UNKNOWN_COLOR`.

### 6. Shot completion is confirmed, not assumed

A feed action should not automatically clear a slot.

Instead:

- `tryFeed()` starts the feed action,
- another subsystem or sensor confirms the artifact actually exited,
- `confirmShotComplete()` updates the committed slot table.

That avoids accidental double-clears when feed motion fails.

---

## Main interaction flows

### 1. Calibration / reference finding

1. Higher-level code calls `startCalibration()`.
2. `Sorter` rotates the spinner according to its calibration routine.
3. During that routine, `Sorter` repeatedly calls `sampleForCalibration()` on the observer.
4. The observer looks for separator evidence from the dual sensors.
5. Once the reference is trusted, `Sorter` locks its angular reference and derives slot geometry.
6. `Sorter` transitions to `CalibrationState.CALIBRATED` and returns to `SorterAction.IDLE`.

Important contract point:

- the observer reports **sensor evidence**,
- the sorter turns that evidence into **trusted geometry**.

### 2. Passive load-side observation

1. `Sorter` tracks spinner angle and determines when a slot has entered the coarse read zone.
2. `Sorter` calls `beginSlotPass(slotIndex)`.
3. While the slot remains in the read zone, `Sorter` repeatedly calls `sampleSlotPass()`.
4. The observer latches artifact presence and color evidence over time.
5. When the slot leaves the read zone, `Sorter` calls `endSlotPass()`.
6. The observer returns one `SlotReadResult`.
7. `Sorter` commits that result into the authoritative slot table.

Important contract point:

- `Sorter` decides **when a pass starts and ends**.
- `LoadStationObserver` decides **what the pass meant**.

### 3. Inventory scan

`startInventoryScan()` exists so the slot table can be built or rebuilt intentionally.

Typical use cases:

- startup with preloaded artifacts,
- recovery after a jam,
- recovery after manual intervention,
- sanity-checking the sorter after desynchronization.

Expected flow:

1. `Sorter` rotates each slot to the load station in turn.
2. Each slot is observed using the same pass lifecycle:
   - `beginSlotPass(slotIndex)`
   - repeated `sampleSlotPass()`
   - `endSlotPass()`
3. `Sorter` updates all slot entries from the returned results.
4. `Sorter` returns to `IDLE`.

### 4. Presentation and feed

1. Higher-level code chooses which slot should go to the shooter.
2. It calls `tryPresentSlot(slotIndex)`.
3. `Sorter` moves that slot to `Station.SHOOT`.
4. Once aligned and occupied, `canFeed()` becomes true.
5. Higher-level code calls `tryFeed()`.
6. After the artifact is confirmed to have exited, higher-level code calls
   `confirmShotComplete()`.
7. `Sorter` clears only that confirmed slot.

### 5. Recovery

Two recovery tools are intentionally separate:

- `markInventoryUnknown()` means “the mechanism contents are no longer trusted.”
- `clearInventory()` means “the mechanism is known to be physically empty.”

That distinction matters because those two states should not be treated the same by autonomous or
teleop logic.

---

## Why the observer uses a pass lifecycle

The observer interface uses:

- `beginSlotPass(slotIndex)`
- repeated `sampleSlotPass()`
- `endSlotPass()`

instead of a single call like `sampleSlot(slotIndex, slotOffsetDegrees)`.

This keeps the contract simpler and clearer.

### What this buys us

- `Sorter` keeps ownership of station geometry.
- `LoadStationObserver` keeps ownership of sensor interpretation.
- the observer is clearly stateful only for the duration of the active pass,
- the final `SlotReadResult` is produced only once the pass is finished.

This is especially useful when:

- one sensor sees a hole in the artifact,
- the second sensor sees color,
- the artifact rolls while the slot moves under the station,
- the best color evidence appears only in part of the pass.

The observer can latch evidence across many frames without taking ownership of global slot state.

---

## Command semantics

`CommandResult` describes whether a sorter command request was accepted.
It does **not** mean the requested action has already finished.

### Interpretation of `CommandResult`

- `ACCEPTED`: the action was started.
- `REJECTED_BUSY`: another sorter action is already running.
- `REJECTED_NOT_CALIBRATED`: trusted geometry is required but not available.
- `REJECTED_INVALID_SLOT`: the requested slot index is out of range.
- `REJECTED_NO_EMPTY_SLOT`: no empty slot exists for the requested load-prep action.
- `REJECTED_NOT_READY`: the mechanism state is otherwise not suitable.

The caller should then monitor:

- `SorterStatus`,
- station alignment,
- `canLoad()`,
- `canFeed()`,
- slot contents,

rather than expecting the command call itself to block.

---

## Testing and reliability

This section describes **what should be evaluated** to have confidence in the interfaces and their
implementations.

Detailed procedures can live in a separate test-plan document later.

### A. Key dimensions to evaluate for `LoadStationObserver`

#### 1. Separator detection reliability

The observer should reliably distinguish separator readings from slot-base and artifact readings.

Key questions:

- Does it detect separators consistently over repeated rotations?
- Does it avoid false separator detections on purple, green, or empty-base views?
- Does detection remain stable across the expected motor speed range?

Suggested evaluation idea:

- Run an independent test op-mode with the spinner motor turning at a fixed RPM.
- Place a known separator-colored target where the observer should recognize the separator.
- Compare observed separator detections per second against the mechanically expected rate.

For a 3-slot spinner rotating at `RPM`, the expected separator pass rate at one station is:

`3 * RPM / 60` passes per second.

Any persistent mismatch suggests missed detections, double-detections, or mechanical slip.

#### 2. Empty/base rejection

The observer should treat the slot base/background as empty rather than as an artifact.

Key questions:

- What is the false-positive rate when the spinner is empty?
- Does lighting change the empty/background classification too much?
- Does a partially shadowed empty slot stay classified as empty?

#### 3. Artifact presence detection

The observer should reliably determine that an artifact is present even when one sensor is over a
hole or edge.

Key questions:

- Does dual-sensor fusion improve robustness when one sensor misses color?
- Does the system still detect presence while the artifact rolls inside the slot?
- Does detection remain stable at the slowest and fastest expected pass speeds?

#### 4. Color latching quality

The observer should latch purple or green when valid evidence appears at any point during the pass.

Key questions:

- Does the correct color latch when only part of the pass shows clean color?
- Does the latch resist flipping repeatedly between colors?
- When color is ambiguous, does it conservatively return `OCCUPIED_UNKNOWN_COLOR` instead of a
  wrong color?

#### 5. Read-pass accounting

The observer should produce exactly one final result per pass.

Key questions:

- Is there one `SlotReadResult` per real slot pass?
- Are there any missing pass results?
- Are there any duplicate pass results?

This is a useful place for test-only counters in debug telemetry.

### B. Key dimensions to evaluate for `Sorter`

#### 1. Calibration repeatability

After repeated calibration attempts, the same physical slot positions should map to the same logical
results.

Key questions:

- Does calibration land in the same reference frame every time?
- After calibration, does `getSlotAt(LOAD)` remain repeatable?
- After calibration, does `getSlotAt(SHOOT)` remain repeatable?

#### 2. Station alignment correctness

`Sorter` should position slots correctly at both stations.

Key questions:

- Does `tryPrepareLoad()` place a known-empty slot at `LOAD`?
- Does `tryPresentSlot(slotIndex)` place the requested slot at `SHOOT`?
- Do `canLoad()` and `canFeed()` become true only in the intended physical states?

#### 3. Inventory ownership and updates

`Sorter` should update slot contents only from valid events.

Key questions:

- Does a finished slot pass update only the intended slot?
- Does `startInventoryScan()` correctly rebuild all three slots?
- Does `confirmShotComplete()` clear only the shot slot and only after confirmation?
- Do recovery methods (`markInventoryUnknown()`, `clearInventory()`) produce the intended state?

#### 4. Non-blocking action behavior

The sorter state machine should make progress over repeated `update()` calls without freezing the
robot loop.

Key questions:

- Do commands return immediately?
- Do accepted actions progress correctly over time?
- Are conflicting commands rejected with the right `CommandResult`?
- Does the sorter return to `IDLE` cleanly after each action?

### C. Key dimensions to evaluate for integration

#### 1. End-to-end slot truth

With known physical contents placed in the spinner, the public slot table should match reality after
scan and after passive operation.

Key questions:

- After `startInventoryScan()`, do all three slots match the real contents?
- After loading a new artifact, does the next pass update the correct slot?
- After a shot, does the cleared slot match the physically emptied slot?

#### 2. Preloaded and recovery scenarios

The integrated system should behave safely when the slot table is not initially trusted.

Key questions:

- Can the sorter rebuild inventory from startup with preloaded contents?
- After `markInventoryUnknown()`, is normal behavior conservative until a scan rebuilds truth?
- After `clearInventory()`, do all public APIs behave as if the spinner is empty?

#### 3. Speed and robustness envelope

The system should remain reliable across the expected operational range.

Key questions:

- How does accuracy change with spinner speed?
- How does accuracy change with lighting?
- How does accuracy change with artifact wear, hole alignment, or sensor mounting tolerance?

---

## Suggested test setups

These are suggestions, not a full test plan.

### 1. Observer-only fixed-RPM op-mode

A dedicated op-mode can instantiate only the load-station observer and a motorized spinner fixture.

Useful features:

- motor running at fixed RPM,
- known color targets presented to the sensors,
- separator-colored target for calibration testing,
- telemetry for raw sensor values, interpreted surface, latched color, and event counters.

This is the setup where comparing detection counts against expected pass frequency is most useful.

### 2. Slow step-through station test

A slow, manual, or stepwise rotation test is useful for verifying:

- where the sorter believes `LOAD` is,
- where the sorter believes `SHOOT` is,
- exactly when a slot pass begins and ends,
- whether the intended slot index is being updated.

### 3. Known-inventory integration test

Load the three slots with a known pattern such as:

- empty / purple / green,
- purple / purple / green,
- empty / empty / purple.

Then verify:

- scan result correctness,
- presentation correctness,
- feed confirmation behavior,
- recovery behavior.

### 4. Lighting and robustness sweep

Repeat observer and integration tests under:

- brighter and dimmer conditions,
- different sensor gains,
- intentionally imperfect artifact orientation,
- slightly varied spinner speeds.

---

## Telemetry that is worth exposing during testing

### From `LoadStationObserver`

Useful debug outputs include:

- raw RGB values for both sensors,
- derived HSV values,
- current interpreted surface kind,
- whether a pass is active,
- whether artifact presence has latched,
- currently latched color,
- calibration/separator event counters,
- slot-pass counters.

### From `Sorter`

Useful debug outputs include:

- `CalibrationState`,
- `SorterAction`,
- current slot table,
- slot at `LOAD`,
- slot at `SHOOT`,
- command acceptance/rejection reason,
- scan progress,
- shot confirmation state.

---

## Acceptance mindset

The point of the contract is not just to make the code compile.
It is to make the sorter reliable enough that higher-level robot logic can trust it.

A good implementation should therefore prioritize:

- repeatability,
- conservative failure behavior,
- observability through telemetry,
- explicit recovery paths,
- clear ownership boundaries between mechanism logic and sensor interpretation.
