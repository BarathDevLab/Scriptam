# Dynamic Widget UI (`widgetUI()`) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Provide a rich, declarative JavaScript Widget DSL (`widgetUI()`) supporting custom styling (padding, colors, backgrounds, typography, alignment, spacers) and dynamic updates, with seamless fallback to console output when no UI function is defined.

**Architecture:** 
1. **JavaScript `Widget` DSL**: A client-side DSL in QuickJS offering both high-level templates (`Widget.Stat`, `Widget.List`, `Widget.Progress`) and composable primitive elements (`Widget.Card`, `Widget.Row`, `Widget.Column`, `Widget.Text`, `Widget.Badge`, `Widget.ProgressBar`, `Widget.Spacer`, `Widget.Divider`).
2. **Styling Engine**: Each element supports styling attributes:
   - `color`: text/accent color (`#RRGGBB` or `#AARRGGBB`)
   - `bg`: background color of the card or container
   - `padding`: `{ top, bottom, left, right }` or scalar `dp`
   - `font`: `fontSize` (sp), `bold` (boolean), `fontFamily` (`sans-serif`, `monospace`, `serif`, `sans-serif-medium`)
   - `align`: text alignment (`left`, `center`, `right`)
   - `spacer`: dynamic vertical spacing (`Spacer(dp)`)
3. **Dynamic Updates**:
   - `widget.setUpdateInterval(minutes)`: allows scripts to set their own periodic update interval (minimum 15 mins for WorkManager).
   - Tap-to-refresh: tapping the widget's Run button (or body) triggers immediate re-evaluation and UI push.
4. **Renderer**: `ScriptWidget` parses the JSON tree and applies styles to Android `RemoteViews` using `RemoteViews.setViewPadding()`, `setTextColor()`, `setInt("setBackgroundColor")`, `setTextViewTextSize()`, and dynamic view inflation via `addView()`.
5. **Fallback**: If `widgetUI` is not defined, the widget automatically renders the captured console logs (`console.log(...)`) in monospace format.

**Tech Stack:** Kotlin, Android RemoteViews, QuickJS (via Dokar QuickJS Android), WorkManager, SharedPreferences.

---

### Task 1: Define Widget UI Domain Models & Parser with Styling Attributes

**Files:**
- Create: `app/src/main/java/com/scriptam/app/widget/model/WidgetModels.kt`
- Create: `app/src/main/java/com/scriptam/app/widget/model/WidgetPayloadParser.kt`
- Create: `app/src/test/java/com/scriptam/app/widget/WidgetPayloadParserTest.kt`

**Step 1: Write unit tests for JSON payload parser**
Verify parsing of:
1. `WidgetStyle`: padding (top, bottom, left, right), color, bg, fontSize, bold, fontFamily, align.
2. `StatPayload`: `{ type: "stat", title: "BTC", value: "$64,000", subtitle: "+5%", color: "#00E676", bg: "#1A1A24", padding: 16 }`
3. `ListPayload`: `{ type: "list", title: "Servers", items: [...], bg: "#121218" }`
4. `CardPayload`: `{ type: "card", bg: "#1A1A24", padding: 12, children: [...] }`
5. `ProgressPayload`: `{ type: "progress", value: "85%", progress: 85, color: "#6C63FF" }`
6. Edge cases: invalid hex colors, missing fields, null payloads.

**Step 2: Run test to verify it fails**
Run: `./gradlew.bat testDebugUnitTest --tests com.scriptam.app.widget.WidgetPayloadParserTest`

**Step 3: Implement domain models & `WidgetPayloadParser`**
Implement the models in `WidgetModels.kt` and parse methods in `WidgetPayloadParser.kt`.

**Step 4: Run test to verify it passes**
Run: `./gradlew.bat testDebugUnitTest --tests com.scriptam.app.widget.WidgetPayloadParserTest`

**Step 5: Commit**
```bash
git add app/src/main/java/com/scriptam/app/widget/model/ app/src/test/java/com/scriptam/app/widget/
git commit -m "feat(widget): add widget UI models with styling attributes and tests"
```

---

### Task 2: Inject Comprehensive JavaScript `Widget` Helper DSL into QuickJS

**Files:**
- Modify: `app/src/main/java/com/scriptam/app/core/ScriptEngineManager.kt`
- Test: `app/src/test/java/com/scriptam/app/core/ScriptEngineWidgetTest.kt`

**Step 1: Write test verifying `Widget` DSL in JavaScript environment**
Test that:
- `Widget.Text("Hello", { color: "#FFF", fontSize: 16, bold: true })` produces expected JSON node.
- `Widget.Card({ bg: "#111", padding: 16, children: [...] })` builds tree.
- `Widget.Stat({ title: "CPU", value: "45%", color: "#00E676" })` builds stat structure.
- `Widget.List({ title: "Tasks", items: [...] })` builds list structure.
- `Widget.Progress({ title: "Battery", value: "80%", progress: 80 })` builds progress structure.
- `Widget.Spacer(12)` builds spacer node.
- `Widget.Badge("NEW", { color: "#000", bg: "#00E676" })` builds badge node.

**Step 2: Run test to verify it fails**
Run: `./gradlew.bat testDebugUnitTest --tests com.scriptam.app.core.ScriptEngineWidgetTest`

**Step 3: Implement `installWidgetDsl` in `ScriptEngineManager.kt`**
Inject the complete JS library definition into QuickJS before script execution.

**Step 4: Run test to verify it passes**
Run: `./gradlew.bat testDebugUnitTest --tests com.scriptam.app.core.ScriptEngineWidgetTest`

**Step 5: Commit**
```bash
git add app/src/main/java/com/scriptam/app/core/ app/src/test/java/com/scriptam/app/core/
git commit -m "feat(engine): inject comprehensive Widget DSL into QuickJS"
```

---

### Task 3: Add Dynamic Update Scheduling & Interval Support

**Files:**
- Modify: `app/src/main/java/com/scriptam/app/widget/WidgetPrefs.kt`
- Modify: `app/src/main/java/com/scriptam/app/worker/ScriptWorker.kt`
- Modify: `app/src/main/java/com/scriptam/app/bridge/AndroidBridge.kt`

**Step 1: Add update interval setting to `WidgetPrefs`**
Allow storing `updateIntervalMinutes` per widget (e.g. 15, 30, 60 mins).

**Step 2: Add `widget.setUpdateInterval(minutes)` JS bridge**
Allow scripts to request their own periodic background run schedule via `ScriptWorker.enqueueRepeating()`.

**Step 3: Update `ScriptWorker.doWork()` to extract UI payload or fallback to console**
Execute script:
```javascript
(function() {
    if (typeof widgetUI === 'function') {
        var res = widgetUI();
        return JSON.stringify(res);
    }
    return null;
})()
```
If non-null: store with `payloadType = "ui"`.
If null: gather console logs and store with `payloadType = "console"`.

**Step 4: Verify with Gradle**
Run: `./gradlew.bat compileDebugSources`

**Step 5: Commit**
```bash
git add app/src/main/java/com/scriptam/app/widget/ app/src/main/java/com/scriptam/app/worker/
git commit -m "feat(widget): add dynamic update intervals and bridge support"
```

---

### Task 4: Design RemoteViews Layouts for Cards, Dynamic Lists, and Spacers

**Files:**
- Modify: `app/src/main/res/layout/widget_script.xml`
- Create: `app/src/main/res/layout/widget_row_item.xml` (dynamic row for lists/keyValue)
- Create: `app/src/main/res/layout/widget_spacer.xml` (flexible spacer for custom margins)
- Create: `app/src/main/res/drawable/widget_progress_bar.xml` (custom styled progress drawable)

**Step 1: Update `widget_script.xml`**
Support dynamic container layout:
- Root card layout with customizable padding and background color.
- `@+id/widget_content_container`: container `LinearLayout` where dynamic rows, cards, or console output are injected.
- Stat container with title, big value, subtitle badge.
- Progress bar container with progress bar view.
- Monospace console output view.

**Step 2: Create `widget_row_item.xml`**
Includes label text, value text, and optional status dot/badge with customizable colors.

**Step 3: Verify resources build**
Run: `./gradlew.bat processDebugResources`

**Step 4: Commit**
```bash
git add app/src/main/res/layout/ app/src/main/res/drawable/
git commit -m "feat(widget): create RemoteViews templates with styling capabilities"
```

---

### Task 5: Implement `ScriptWidget` Dynamic RemoteViews Styler

**Files:**
- Modify: `app/src/main/java/com/scriptam/app/widget/ScriptWidget.kt`

**Step 1: Implement styling applicators in `ScriptWidget`**
Apply styling to `RemoteViews`:
- `RemoteViews.setViewPadding(viewId, left, top, right, bottom)` for custom padding.
- `RemoteViews.setTextColor(viewId, colorInt)` for custom text color.
- `RemoteViews.setInt(viewId, "setBackgroundColor", bgColorInt)` for background colors.
- `RemoteViews.setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, fontSize)` for font sizes.
- Alignment (`Gravity.START`, `Gravity.CENTER`, `Gravity.END`).

**Step 2: Handle both template and composable types**
- If payload is `Stat`: render stat with user's `color`, `bg`, `padding`, `fontSize`.
- If payload is `Progress`: render progress bar + percentage with user's `accentColor`.
- If payload is `List`: iterate `items` and dynamically `addView` row RemoteViews with custom label, value, and color.
- If payload is `Console`: show monospace console output.

**Step 3: Verify compilation & unit test suite**
Run: `./gradlew.bat testDebugUnitTest`

**Step 4: Commit**
```bash
git add app/src/main/java/com/scriptam/app/widget/ScriptWidget.kt
git commit -m "feat(widget): implement RemoteViews styling and dynamic component renderer"
```

---

### Task 6: Add Sample Scripts Showcasing Custom Styling & Dynamic Updates

**Files:**
- Modify: `app/src/main/java/com/scriptam/app/data/repository/ScriptRepositoryImpl.kt`

**Step 1: Add preloaded sample scripts demonstrating `widgetUI()`**
1. **Crypto Price Tracker** (custom colors, bold value, green/red badges, 15m update interval).
2. **Device Stats Monitor** (battery %, storage, key-value rows, progress bar).
3. **Quick Notes / Console Script** (demonstrating the automatic fallback to console).

**Step 2: Build & install APK on emulator**
Run: `./gradlew.bat assembleDebug` and `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

**Step 3: Commit**
```bash
git add app/src/main/java/com/scriptam/app/data/
git commit -m "feat(samples): add rich styled widgetUI example scripts"
```
