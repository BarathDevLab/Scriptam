# Agent Execution Guidelines & Skills for Scriptam

## Engineering Principles
1. **Safety First:** Never allow JavaScript scripts direct memory or system access. All system operations must pass through the `AndroidBridge` validation boundary.
2. **Main Thread Protection:** Script execution and file I/O must always execute on background dispatchers. The Jetpack Compose UI thread must remain unblocked.
3. **Memory Hygiene:** Ensure QuickJS contexts are properly closed (`quickjs.close()`) in `finally` blocks to prevent native C-heap memory leaks.

## Coding Conventions
* Use Kotlin idioms (coroutines, Flow, immutability where appropriate).
* Follow Material 3 design guidelines for Jetpack Compose UI components.
* Maintain clean separation of concerns across `core`, `bridge`, `data`, and `ui` packages under `com.scriptam.app`.