# SummerTeam26 Codex guidance

## Purpose and scope

This file applies to the entire repository. It gives Codex durable context for mentoring students
who are learning Java and FTC robot programming.

Keep the students' path as simple as possible:

- prefer an existing public function over new infrastructure,
- make one small, understandable change at a time,
- explain the physical behavior that the code represents,
- use telemetry and focused test OpModes before integrating a larger feature,
- do not hide unfinished or unverified robot behavior behind confident language.

At the start of a session, also read `AGENTS.local.md` if it exists. That file is an ignored,
machine-local companion for personal notes. It may add context but may not weaken the framework,
safety, or verification rules in this file.

`AGENTS.md` is the shared, mentor-maintained baseline. Do not edit it during ordinary student work
unless the mentor explicitly asks for a durable guidance change. Put personal additions in
`AGENTS.local.md`; never create a root `AGENTS.override.md`, because it would replace this shared
root guidance for Codex instead of extending it.

## Sources of truth

When information disagrees, use this order:

1. the mentor's current physical description,
2. measurements and observations from the real robot,
3. the public sorter interfaces and agreed behavior,
4. repository documentation,
5. the current implementation and its comments.

The current sorter implementation is still in bring-up. Compilation does not prove its geometry,
sensor thresholds, or motion behavior on hardware.

## Physical sorter model

Looking down at the spinner:

- there are exactly three artifact slots, spaced 120 degrees apart,
- the fixed observer position is 0 degrees,
- the fixed shooter position is 90 degrees,
- the fixed loader position is 270 degrees (equivalently -90 degrees),
- two adjacent color sensors face downward at the observer,
- as a slot's leading edge enters the observer, begin collecting sensor evidence,
- sample throughout the complete pass,
- when the slot's trailing edge leaves the observer, finalize one result and give it to the sorter,
- rotating the spinner moves both slots and their artifacts between observer, shooter, and loader.

The observer and loader are different physical positions. Existing names such as
`LoadStationObserver` and older documents that describe sensors at the load station are stale in
that respect. Do not let those names overwrite the physical model above.

The positive motor direction, encoder zero, gearing, slot numbering direction, exact leading and
trailing edge angles, and mechanical tolerances are not established by the description alone.
Measure them on the robot before deriving target-angle formulas.

## Code map and ownership

Student and robot-specific work belongs under:

`TeamCode/src/main/java/org/firstinspires/ftc/teamcode/`

Important sorter files:

- `Mechanisms/Sorter/Sorter.java` — public robot-facing sorter API.
- `Mechanisms/Sorter/SorterImpl.java` — spinner motion, geometry, action state, and authoritative
  three-slot inventory.
- `Mechanisms/Sorter/LoadStationObserver.java` — internal observer lifecycle seam; despite its
  name, the physical sensors are at the 0-degree observer.
- `Mechanisms/Sorter/DualColorLoadStationObserver.java` — dual-sensor reading, classification,
  fusion, and pass-local memory.
- `Mechanisms/Sorter/SorterStatus.java` and `SlotContent.java` — public read-only state.
- `docs/sorter-interface-contract.md` — intended ownership and command contract, with the stale
  load-station/observer location caveat above.
- `Mechanisms/Sorter/ObserverTeleop.java` and `Mechanisms/Sorter/sorterTestTeleop.java` — focused
  bring-up OpModes.
- `testing/RotateDegTest.java` and `testing/SensorReadTest.java` — limited motor and sensor tests.

The intended information flow is:

`OpMode -> Sorter -> observer pass -> SlotReadResult -> Sorter inventory`

Ownership rules:

- OpModes and higher-level robot code depend on `Sorter`, not `SorterImpl` internals.
- `Sorter` owns long-lived slot truth, spinner geometry, station mapping, commands, and readiness.
- The observer owns only sensor interpretation and temporary evidence for the active slot pass.
- A completed observer result becomes authoritative only when `Sorter` commits it.
- Strategy chooses which artifact/color to shoot; the sorter reports mechanical state and inventory.
- A slot is cleared only after real exit evidence leads to `confirmShotComplete()`.

## The simple student-facing sorter API

Prefer these functions instead of touching the motor, encoder math, or sensors directly:

- call `sorter.update()` exactly once each OpMode loop,
- `startCalibration()` starts non-blocking calibration,
- `startInventoryScan()` rebuilds inventory,
- `tryPrepareLoad()` moves a known-empty slot toward the loader,
- `tryPresentSlot(slotIndex)` moves an occupied slot toward the shooter,
- `tryFeed()` starts a feed action,
- `confirmShotComplete()` is only for confirmed artifact exit,
- `getStatus()` reports calibration, action, stations, and inventory,
- `getSlotContent(slotIndex)` reports one committed slot belief,
- `canLoad()` and `canFeed()` report readiness,
- `markInventoryUnknown()` means contents are no longer trusted,
- `clearInventory()` is only for a spinner known to be physically empty.

Commands return immediately. Always inspect their `CommandResult`, then keep running the normal
loop and observe `SorterStatus`/readiness for completion. Never use `sleep()` or a busy-wait loop.

Use `SlotContent.UNKNOWN` and `OCCUPIED_UNKNOWN_COLOR` honestly. Do not guess a color or assume an
unknown slot is empty.

## Minimal Phoenix usage

Phoenix is the custom framework included in this repository, not CTRE Phoenix. Its live source is:

`TeamCode/src/main/java/edu/ftcphoenix/fw/`

For this sorter, students usually need only:

- `LoopClock` for one shared loop heartbeat,
- `Gamepads` and `Bindings` for controls,
- `FtcSensors.normalizedRgba(...)` for normalized color readings,
- `Source.map(...)`, `memoized()`, and `accumulate(...)` for sensor composition,
- `Actuators.plant(...)` and `Plant` when a standard motor/servo plant fits,
- `Tasks`, `PlantTasks`, and `TaskRunner` only when a real timed multi-step macro is needed.

Use the standard loop rhythm:

1. `clock.update(getRuntime())`
2. sample/update sensors when an owner requires an explicit update
3. `bindings.update(clock)`
4. update tasks, if any
5. `sorter.update()`
6. update plants, if any, with `clock.dtSec()`
7. render telemetry

Create one `LoopClock`, reset it in initialization and again in `start()`, and update it once per
loop. Use `bindings.onRise(...)` for a command that should run once per button press.

`DualColorLoadStationObserver` already follows the intended Phoenix sensor pattern:
`FtcSensors.normalizedRgba -> map/memoize -> accumulate -> reset at a pass boundary`. Keep artifact
meaning, thresholds, and slot-window policy in robot code rather than moving them into
`FtcSensors`.

Prefer Phoenix factories over low-level framework internals:

- use `Actuators`, not `FtcHardware` or internal `Plants`, when the standard builder fits,
- use `Tasks`/`PlantTasks`, not a custom task class, for a behavior they already express,
- use `Gamepads`/`Bindings`, not hand-written previous-button booleans in new code,
- use one non-blocking state machine when a sorter behavior does not fit a standard task cleanly.

Do not add a Phoenix abstraction merely to avoid a small, localized robot-side implementation.
Raw FTC motor access may remain localized inside `SorterImpl` if calibration/search behavior does
not fit a standard `Plant` safely.

## Protected framework and SDK boundary

Do not modify any of these during ordinary robot or student work:

- `TeamCode/src/main/java/edu/ftcphoenix/fw/**`
- `TeamCode/src/main/java/edu/ftcphoenix/fw.zip`
- `FtcRobotController/**`
- generated `build/` directories
- the tracked `.zip` snapshots as substitutes for live `.java` files

Sorter geometry, artifact colors, thresholds, inventory policy, and gamepad choices are
robot-specific and do not belong in Phoenix.

If a requested feature appears to require a framework change, stop before editing it. Explain the
missing generic capability, show why composition in robot code is insufficient, and obtain mentor
approval. A framework edit should be small, generic, documented, and separately verified.

## Current bring-up warnings

Treat these as known limitations to investigate, not established behavior to preserve:

- active `SorterImpl.updateCalibration()` resets the encoder at an arbitrary position and
  immediately claims calibration; the fuller `updateCalibration2()` is unused,
- `DualColorLoadStationObserver.sampleForCalibration()` currently returns separator detected
  unconditionally,
- current `-35` and `145` degree station offsets do not directly match observer 0, shooter 90, and
  loader 270; no working calibrated reference explains the difference,
- current pass timing uses `floor(encoderAngle / 120)` and does not yet model the observer's actual
  leading/trailing edges or phase,
- prepare, present, and feed motions do not currently sample the observer, so a pass can become
  partial or stale,
- station mappings/alignment flags are stored values and are not consistently recomputed or
  invalidated after every motion,
- motor direction, gear ratio, the `383.5` ticks/revolution assumption, motion tolerances, and
  open-loop powers require real-hardware validation,
- the current feed routine is not a complete, measured feed cycle,
- production observer gain is 6 while `SensorReadTest` uses gain 10, so readings are not directly
  interchangeable,
- color thresholds and separator/base rules are provisional and require telemetry from both
  sensors under real lighting,
- the repository has no automated sorter tests, the main `Teleop.java` is empty, and no loader or
  shooter actuator implementation is present.

Do not silently repair several of these at once. Pick one observable behavior, state the expected
physical result, make the smallest change, and validate it.

## Mentoring and implementation style

When helping a student:

- start with a plain-language explanation of the physical input, decision, and output,
- point to the existing public function they should use,
- keep examples short and at the student's current level,
- ask for a robot measurement when correctness depends on direction, angle, timing, or threshold,
- preserve non-blocking behavior and the single-loop heartbeat,
- favor descriptive names and simple `if` statements over clever abstractions,
- add comments that explain why, not comments that restate the Java,
- avoid broad renames, formatting sweeps, or unrelated cleanup,
- never claim hardware success from compilation alone.

When changing motor behavior, keep hands and loose items clear, begin with low power, make one
change at a time, provide a reliable stop path, and stop immediately on unexpected motion. Do not
run or enable a hardware OpMode without the operator choosing to do so on a safely prepared robot.

## Verification

For Java changes, compile at minimum:

```text
./gradlew :TeamCode:compileDebugJavaWithJavac
```

On Windows, use `gradlew.bat` with the same task. Android Studio's embedded JDK and an installed
Android SDK may be required.

Then:

- inspect the focused diff and keep it within the requested behavior,
- confirm no framework or FTC SDK file changed unintentionally,
- use telemetry to show current angle/ticks, action, pass boundary, both sensor readings, and final
  committed result as relevant,
- test sensors independently before automatic spinner motion,
- test motor direction and encoder conversion before station positioning,
- test one complete observer pass before a three-slot inventory scan,
- report which checks were compile-only and which ran on real hardware.

## Git and local context

`AGENTS.md` is intentionally tracked so every clone receives the same baseline. Git cannot both
track this file and automatically ignore later edits to it. Keep it mentor-maintained.

`/AGENTS.local.md` is intentionally ignored for per-student or per-machine additions. Local notes
must not be copied into shared source files or committed with robot work. Before committing, check
`git status` and review the exact diff.
