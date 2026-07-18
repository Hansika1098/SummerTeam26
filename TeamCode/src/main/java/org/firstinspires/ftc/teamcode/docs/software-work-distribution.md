# Sorter Student Work Plan

## Goal

This plan gives two students separate 3–5 hour assignments on the sensor-to-sorter pipeline.

The assignments intentionally keep the current architecture and coarse three-sector scan. They
are bring-up exercises, not a complete or competition-ready sorter implementation.

```text
Both color sensors
        |
        v
Student A: interpret one slot pass
        |
        v
SlotReadResult
        |
        v
Student B: commit three passes to sorter inventory
```

Each student should edit only their assigned files. If a shared interface appears insufficient,
stop and discuss it with the mentor before changing it.

## Student A: dual-sensor observation

### Goal

Make readings from both sensors easy to compare and replace the fake separator result with the
observer's real classification.

### Assigned files

Only Student A edits:

- `testing/SensorReadTest.java`
- `Mechanisms/Sorter/DualColorLoadStationObserver.java`
- `Mechanisms/Sorter/ObserverTeleop.java`

### Suggested time

- Read the observer and Phoenix sensor flow: 30 minutes
- Display both sensors independently: 45–60 minutes
- Record stationary readings: 45–60 minutes
- Enable and, only if measurements justify it, tune separator classification: 30–60 minutes
- Test complete manual passes and clean up: 45–60 minutes

### Tasks

1. Follow the existing Phoenix pattern in `SensorReadTest` to read the configured
   `"color sensor 2"` alongside sensor 1.
2. Keep gain `6.0` on both sensors so test readings match production.
3. Show each sensor's alpha, chroma, and red/green/blue ratios separately.
4. Record readings for an empty slot, separator, green artifact, and purple artifact.
5. In `sampleForCalibration(...)`, replace the unconditional result with the separator evidence
   already present in the merged observation.
6. Adjust classifier constants only when the recorded evidence shows a clear need.
7. Use `ObserverTeleop` to test complete passes by moving the spinner manually. Do not power the
   spinner motor during this assignment.

### Acceptance criteria

- Both sensors have separate telemetry.
- Both sensors and production code use gain `6.0`.
- Separator is classified as separator, while empty, green, and purple samples are not.
- Two manual passes each for empty, green, and purple return the expected content.
- Starting a new pass clears evidence from the preceding pass.
- Conflicting or weak evidence remains conservative rather than guessing a color.
- The TeamCode Java compile succeeds.
- Hardware measurements and compile-only checks are reported separately.

## Student B: three-slot inventory scan

### Goal

Starting from a manually aligned reference, make the current scan sequence understandable, safe,
and repeatable: begin, sample, finish, and commit exactly three slot passes.

### Assigned files

Only Student B edits:

- `testing/RotateDegTest.java`
- `Mechanisms/Sorter/SorterImpl.java`
- `Mechanisms/Sorter/sorterTestTeleop.java`

### Suggested time

- Trace the scan and observer lifecycle: 30 minutes
- Measure direction and encoder movement: 45–60 minutes
- Inspect and repair the scan lifecycle and stop paths: 60–90 minutes
- Verify or add focused telemetry: 30–45 minutes
- Run repeated scans and clean up: 45–60 minutes

### Tasks

1. Use the low-power, hold-to-run `RotateDegTest` to record:

   - which physical direction positive motor power turns the spinner,
   - whether encoder ticks increase or decrease,
   - the observed ticks for approximately 120 degrees.

2. For this assignment only, manually align the beginning of physical slot 0 with the 0-degree
   observer before pressing A in `sorterTestTeleop`. Treat that encoder reset as a test-only manual
   reference, not real automatic calibration.
3. Focus sorter changes on `updateInventoryScan()`.
4. Keep the existing non-blocking lifecycle:

   - begin one pass,
   - sample once per OpMode loop while in the same 120-degree sector,
   - end and commit when the sector changes,
   - repeat until exactly three results have been committed,
   - stop the motor and return to idle.

5. Ensure every completion or early-exit path in the scan sets spinner power to zero.
6. Use the provided sorter-test telemetry to trace the scan. Add only a missing diagnostic that
   proves or explains a behavior.
7. Run the scan twice from the same manual starting reference.

### Acceptance criteria

- `sorter.update()` remains non-blocking and runs once per OpMode loop.
- One scan begins and ends exactly three passes.
- Each completed result is committed once; no fourth pass is started.
- Spinner power is zero on scan completion and every scan abort path.
- The action returns to idle after the third pass.
- Telemetry makes the scan sequence and inventory visible.
- Two repeated scans use the same logical slot ordering.
- The TeamCode Java compile succeeds.
- Measured direction and ticks are recorded without presenting provisional constants as calibrated
  geometry.

Accurate color inventory is an integration check with Student A, not a blocker for Student B's
independent pass-count and motor-stop checks.

## Frozen shared seams

Students must not change these files for this assignment:

- `Mechanisms/Sorter/Sorter.java`
- `Mechanisms/Sorter/LoadStationObserver.java`
- `Mechanisms/Sorter/SlotReadResult.java`
- `Mechanisms/Sorter/SlotContent.java`
- `Mechanisms/Sorter/SeparatorDetection.java`
- `Mechanisms/Sorter/ObserverDebug.java`
- `Mechanisms/Sorter/SorterStatus.java`
- `AGENTS.md`
- everything under `edu/ftcphoenix/fw/**`
- everything under `FtcRobotController/**`

Do not edit the other student's assigned files. Discuss a needed seam change with the mentor before
coding it.

## Integration check

After both branches work independently:

1. Merge Student A first.
2. Rebase Student B's branch onto the merged branch.
3. Compile TeamCode.
4. Test one manually moved observer pass with the motor disabled.
5. Clear the spinner and test one low-power three-slot scan.
6. Test a labeled physical arrangement such as slot 0 empty, slot 1 green, and slot 2 purple.
7. Compare the physical arrangement with `getSlotContent(0..2)`.
8. Change sensor thresholds only in Student A's files and scan behavior only in Student B's files.
9. Have each student review the other's PR and explain the complete data flow.

## Out of scope

Do not include these in this assignment:

- automatic separator-based calibration,
- precise observer leading- and trailing-edge window detection,
- replacement of the current station offsets,
- loader or shooter positioning,
- `tryPrepareLoad()`, `tryPresentSlot(...)`, or `tryFeed()` repairs,
- feed-cycle behavior,
- a major `SorterImpl` rewrite,
- new Phoenix abstractions or framework changes.

## Mentoring expectation

Students should make the implementation decisions. Codex and mentors should lead with questions,
physical reasoning, measurements, and small hints. Point students to the relevant existing
function and help them inspect telemetry before offering code. Do not provide a complete solution
unless the mentor explicitly asks for one.

## Hardware safety

- Student A keeps the spinner motor disabled and moves the spinner by hand only when safe.
- Before Student B powers the spinner, keep hands, hair, wires, tools, and loose items clear.
- Begin with low power and verify direction before attempting a scan.
- Keep the Driver Station stop control immediately available.
- Stop on unexpected motion, noise, binding, or encoder direction.
- Never infer hardware success from a successful compile.

## Git workflow note

These assignments modify existing tracked files and should not add files. The repository workflow
automatically closes a pull request containing an added or copied file. If a new file is genuinely
required, discuss it with the mentor first. The preferred approach is for the mentor to add the
scaffold to the shared base branch before student work begins; otherwise the PR instructions allow
one `/not-a-mistake` comment to request reopening.
