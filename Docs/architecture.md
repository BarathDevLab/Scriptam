# Scriptam Architecture Specification

Scriptam follows a strict **Clean Architecture** combined with a modular **Bridge Pattern** to ensure safety, isolation, and high performance.

## 1. Architectural Layers (`com.scriptam.app`)

```text
com.scriptam.app/
├── core/             <-- Script Engine & Runtime Execution
├── bridge/           <-- Native API Bridge (JS <-> Kotlin boundary)
├── data/             <-- Room DB Persistence & Local File I/O
├── ui/               <-- Jetpack Compose Presentation Layer
└── worker/           <-- Background Execution & Automation WorkManager

Component Breakdown
A. Core Engine (core/)
ScriptEngineManager.kt: Manages the QuickJS virtual machine lifecycle. Ensures all script executions are confined to a single background thread (Dispatchers.Default.limitedParallelism(1)) to prevent data races and main thread stuttering.

ScriptConsole.kt: Injects a global console object into the QuickJS context to capture console.log() and console.error() outputs back to the UI.

B. Bridge Layer (bridge/)
AndroidBridge.kt: The root entry point exposed to JavaScript as a global object (e.g., Native).

Modules: Granular capability classes (UIModule, StorageModule, NetworkModule) that wrap Android system services securely so untrusted JavaScript cannot access unauthorized hardware or raw memory pointers.

C. Data Layer (data/)
File System Sandbox: Physical .js script files reside in context.filesDir/scripts/.

Room Database: Tracks script metadata (id, fileName, title, accentColor, lastModified) for fast UI indexing.

D. Presentation & Worker Layers (ui/, worker/)
Jetpack Compose handles the declarative UI (Dashboard list, Monaco Editor view, Output console).

WorkManager enables scheduled background script execution for data monitoring tasks.