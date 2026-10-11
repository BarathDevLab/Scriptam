# Scriptam ⚡

> **Native JavaScript Automation & Interactive Android Home-Screen Widgets**  
> *Write modern JavaScript on your Android device. Run background automations. Build rich, flicker-free home-screen widgets without writing a line of native Java or Kotlin.*

---

## 🌟 Overview

**Scriptam** is a high-performance Android automation and widget environment powered by a sandboxed **QuickJS** engine. It brings a Scriptable-like developer experience to Android with first-class Jetpack Compose tooling, surgical `RemoteViews` tree-diffing, hardware-backed secret vaults, and a full-featured code editor.

---

## ✨ Features

### 🖥️ 1. Pro In-App Code Editor
* **CodeMirror Engine**: Full JavaScript syntax highlighting, line numbers, active line highlight, bracket pairing, and comment toggling.
* **Trie Autocomplete**: Compressed prefix-matching Trie providing instant inline suggestions for `Native.*`, `Widget.*`, and JS keywords.
* **Keyboard Accessory Toolbar**: Quick programming symbol pills (`Tab`, `{}`, `()`, `[]`, `=`, `>`, `"`, `;`) plus `Undo` / `Redo`.
* **Snippet Engine**: Pre-built templates for REST API fetch widgets, battery notifications, crypto trackers, persistent storage, and secure keystore secrets.

### 🧩 2. Widget Engine & Live Simulator
* **Declarative Widget DSL**: Build custom home-screen widgets using declarative JavaScript functions (`Widget.column`, `Widget.row`, `Widget.text`, `Widget.progress`, etc.).
* **Live In-App Simulator**: Preview your widget live inside the app at Small (2x2), Medium (4x2), and Large (4x4) sizes before pinning to the home screen.
* **Surgical RemoteViews Tree-Diffing**: A diffing algorithm matches widget nodes and only pushes mutated elements, eliminating home-screen widget flickering.

### 🛡️ 3. Safety, Sandbox & Performance
* **Resource Sandboxing**: QuickJS instances enforce a **32 MB memory ceiling** and **1 MB stack limit** to prevent infinite loops, dispatcher stalling, or OOM crashes.
* **Bytecode Cache (`.qbc`)**: Scripts are compiled to QuickJS bytecode and cached on disk keyed by SHA-256 hash for cold-start execution speeds.
* **Hardware-Backed Secret Vault**: Securely store API keys and tokens with AES-256-GCM backed by the **Android KeyStore** (`Native.getSecret()` / `Native.setSecret()`).
* **Token-Bucket Rate Limiting**: Built-in HTTP client rate limiting with burst capacities and per-host throttling to protect against runaway API loops and HTTP 429 penalties.

### 🔄 4. Automation, Pipelines & Watcher
* **System Broadcast Triggers**: Automatically execute scripts on Android system events:
  * `ACTION_POWER_CONNECTED`
  * `ACTION_POWER_DISCONNECTED`
  * `ACTION_BATTERY_LOW`
  * `ACTION_BOOT_COMPLETED`
  * Custom app broadcasts (`com.scriptam.app.TRIGGER`)
* **Multi-Script DAG Chaining**: Chain scripts using Directed Acyclic Graphs with topological sorting, cycle detection, and automatic output-to-input parameter piping.
* **Widget Watcher Suite**: In-app diagnostic hub with execution logs, latency percentiles, failure rates, and the last 10 visual payload snapshots.
* **Portable Script Bundling**: Export and import scripts in portable `.scriptam` ZIP archives containing `manifest.json` metadata and source code.

---

## 🛠️ Architecture

Scriptam follows **Clean Architecture** with a strict boundary between untrusted JavaScript evaluation and Android OS services:

```text
com.scriptam.app/
├── core/
│   ├── ScriptEngineManager.kt    # QuickJS runtime & resource sandboxing
│   ├── BytecodeCache.kt          # Precompiled bytecode disk/memory cache
│   ├── ScriptConsole.kt          # Global console stream
│   ├── autocomplete/             # Trie autocomplete prefix engine
│   └── pipeline/                 # DAG pipeline topological executor
├── bridge/
│   ├── AndroidBridge.kt          # Native.* global bridge installer
│   ├── SecretsModule.kt          # AES-256-GCM Android KeyStore vault
│   ├── RateLimiter.kt            # Token-bucket network throttling
│   ├── NetworkModule.kt          # OkHttp fetch client
│   ├── StorageModule.kt          # Key-value persistent storage
│   ├── UIModule.kt               # Toasts and native dialogs
│   ├── ClipboardModule.kt        # Clipboard read/write
│   ├── NotificationModule.kt     # System notification dispatcher
│   └── DeviceInfoModule.kt       # Battery & device hardware info
├── data/
│   ├── db/                       # Room database & script entities
│   ├── file/                     # Sandboxed script file manager
│   └── bundle/                   # .scriptam archive import/export
├── trigger/
│   ├── ScriptBroadcastReceiver.kt# Broadcast receiver for system triggers
│   └── TriggerConfig.kt          # Trigger binding manager
├── widget/
│   ├── ScriptWidget.kt           # Home-screen AppWidgetProvider
│   ├── diff/WidgetTreeDiffer.kt  # Surgical tree-diffing for RemoteViews
│   ├── monitor/                  # Ring-buffer execution log & snapshot store
│   └── model/                    # Declarative widget payloads & parser
└── ui/
    ├── dashboard/                # Script list & telemetry cards
    ├── editor/                   # CodeMirror editor, accessory bar & snippets
    ├── monitor/                  # Widget Live Simulator & Watcher Suite
    └── runner/                   # In-app interactive run popup
```

---

## 📖 JavaScript API Reference

### 1. `Native.*`
```javascript
// Native Toasts & Alerts
Native.showToast("Script executed successfully!");
Native.alert("Status", "Process completed.");

// Persistent Storage
Native.storage.set("counter", "42");
const count = Native.storage.get("counter");
Native.storage.remove("counter");

// Hardware-Backed Secret Vault (AES-256-GCM)
Native.setSecret("api_key", "sk_live_secret_token_123");
const key = Native.getSecret("api_key");

// HTTP Networking (Rate-Limited OkHttp)
const res = await Native.fetch("https://api.github.com/zen");
const text = await res.text();

// Device & Battery Info
const battery = Native.getBatteryLevel();
const info = JSON.parse(Native.device());

// Clipboard & Notifications
Native.clipboard.copy("Hello from Scriptam!");
Native.notify("Alert", "Battery is running low.");
```

### 2. `Widget.*`
```javascript
// Declarative Home-Screen Widget
Widget.render(
  Widget.column({
    padding: 16,
    backgroundColor: "#1A1A24",
    children: [
      Widget.row({
        children: [
          Widget.text("BTC / USD", { size: 14, color: "#F7931A", bold: true }),
          Widget.spacer(8),
          Widget.text("$68,450", { size: 20, color: "#FFFFFF", bold: true })
        ]
      }),
      Widget.spacer(8),
      Widget.progress(72, 100),
      Widget.spacer(4),
      Widget.text("Updated at " + new Date().toLocaleTimeString(), { size: 10, color: "#8E8E93" })
    ]
  })
);
```

---

## 🧪 Testing & Verification

The codebase includes comprehensive unit tests verifying the core subsystems:

```bash
# Run all unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug
```

### Test Suites Included:
* **`TrieAutocompleteTest`**: Verifies prefix queries, case-insensitivity, and category priority sorting.
* **`RateLimiterTest`**: Validates token bucket exhaustion and per-host isolation.
* **`DagPipelineTest`**: Tests linear DAG ordering, multi-node resolution, and cycle detection errors.
* **`ScriptBundlerTest`**: Round-trip ZIP packaging and manifest/code integrity test.
* **`WidgetTreeDifferTest`**: Asserts surgical mutation detection on widget payloads.
* **`WidgetPayloadParserTest`**: Tests JSON deserialization of all widget types.
* **`ScriptConsoleTest`**: Tests thread-safe in-memory console buffering.

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).
