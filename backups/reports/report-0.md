# UltimatePvPUtils — Deep-Scan Code Quality Audit (report-0 backup)

**Date:** 2026-06-03
**Scope:** 13 Java source files, 3 resource JSONs, 3 Gradle build files
**Total Lines Audited:** ~1,500 (Java) + ~70 (resources/config)

---

## Severity Legend

| Level | Meaning |
|-------|---------|
| **Critical** | Will cause crashes, data loss, or fundamental broken behavior |
| **High** | Major quality issue — buggy, laggy, or architecturally unsound |
| **Medium** | Notable smell or missing feature that degrades maintainability or UX |
| **Low** | Minor inconsistency or cosmetic issue |
| **Info** | Observation, no immediate action required |

---

## 1. BUGS & POTENTIAL CRASHES

### 1.1 [Critical] Keyboard CPS is fundamentally broken — inverted parameter mapping

**File:** `KeyboardHandlerMixin.java:16`

```java
CpsTracker.INSTANCE.onKeyPress(keyInput.key(), key);
```

The `onKey` callback signature is `(long window, int key, KeyInput keyInput, CallbackInfo)`. The call passes `keyInput.key()` as `keyCode` and the raw `int key` as the `action` parameter. `CpsTracker.onKeyPress(int keyCode, int action)` checks `if (action == 1)` to filter press events. The raw GLFW key code (e.g., 65 for 'A') will almost never equal `1`, so keyboard CPS tracking is effectively dead. Even if it did equal 1 by coincidence (key code 1 = GLFW_KEY_2), it would count the wrong event.

**Impact:** The entire keyboard CPS feature does not work. Tokens like `%cps_keyboard.w%` always show 0.

---

### 1.2 [High] Dead `onKeyPress(int)` overload is a double-counting trap

**File:** `CpsTracker.java:27-28`

```java
public void onKeyPress(int keyCode) {
    record("keyboard." + keyCode);
}
```

This single-argument overload has no action filter — it records every call as a click, including key releases and repeats. It is never called in the current codebase, but its existence in the public API is a trap. Any future caller using this overload will get wildly inflated CPS counts.

**Impact:** Latent API misuse risk.

---

### 1.3 [High] Non-atomic config file write — data loss on crash

**File:** `ModConfig.java:73-78`

```java
Files.writeString(CONFIG_PATH, GSON.toJson(this));
```

`Files.writeString` is not atomic. If the game crashes or the process is killed mid-write, the config file will be truncated or contain partial JSON. On next launch, `load()` will hit a `JsonSyntaxException`, back up the corrupt file, and overwrite with defaults — destroying all user configuration. There is no write-to-temp-then-rename strategy.

**Impact:** Complete config data loss on crash during write. Combined with issue 6.1 (saves on every keystroke), this is a live risk.

---

### 1.4 [Medium] `pendingDeleteIndex` can become stale

**File:** `ModConfigScreen.java:363-394`

Clicking outside all cards does not reset `pendingDeleteIndex`. If a user clicks the "Confirm?" delete prompt on card 2, then adds a new card, the stale index could point at a different card. The 3-second timeout partially mitigates this, but the state management is fragile.

**Impact:** "Confirm?" prompt could appear on the wrong card after list mutations.

---

### 1.5 [Medium] `ModConfig.load()` replaces the singleton instance, breaking cached references

**File:** `ModConfig.java:56`

```java
instance = loaded;
```

If any code caches the result of `getConfig()`, calling `load()` again (e.g., for a "reload config" feature) will orphan the old reference. The HUD overlay, config screen, and other components would continue reading from the stale object.

**Impact:** Config hot-reload would silently break all active screens and HUD rendering.

---

### 1.6 [Medium] `CounterEntryAdapter.nextInt()` crashes on hex string color values

**File:** `ModConfig.java:116-117`

If a user hand-edits the config JSON and writes a color as `"#80000000"` instead of `-2147483648`, `nextInt()` throws `IllegalStateException`. The outer try-catch backs up the file and resets to defaults — destroying the entire config for a single typo in one field.

**Impact:** User loses all config data for a single color formatting mistake.

---

### 1.7 [Medium] Unknown key aliases in `CpsVariable` produce unmatchable map keys

**File:** `CpsVariable.java:182-187`

When `KEY_ALIASES.get()` returns null and `Integer.parseInt()` fails, the fallback produces `"keyboard." + key` (e.g., `"keyboard.f26"`). Actual keyboard events use integer GLFW codes like `"keyboard.293"`. These will never match, so the token `%cps_keyboard.f26%` will silently always show 0.

**Impact:** Format tokens with unknown key names produce 0 CPS with no warning.

---

### 1.8 [Low] `HudPositionScreen` resize drag produces jarring snap when dragging left

**File:** `HudPositionScreen.java:148-153`

Dragging to the left of the resize anchor produces a negative `dx`, making `newScale` negative, which is then clamped to 0.5. This causes an instant snap to minimum scale.

**Impact:** Janky resize UX.

---

## 2. PERFORMANCE BOTTLENECKS

### 2.1 [Critical] ~730 GPU draw calls per frame for color picker

**File:** `ColorPickerScreen.java:113-165`

The SB field renders ~380 `fill()` calls (8px blocks), the hue bar renders 150 1-pixel-wide fills, the alpha bar renders 150 fills + 150 checkerboard fills, and the preview checkerboard renders ~50 fills. Total: ~730 individual quad submissions per frame. Each `dc.fill()` is a separate GPU draw command.

**Impact:** Severe frame-rate degradation on low-end and mid-range GPUs while the color picker is open — precisely when the user is dragging sliders, making it a continuous hot path.

---

### 2.2 [High] Regex execution every frame for every counter

**File:** `CpsVariable.java:159-170`, called from `CpsHudOverlay.java:49`

`CpsVariable.resolve()` creates a `Matcher` and runs regex against the format string on every HUD render frame for every counter. At 60 FPS with 5 counters, that is 300 regex evaluations per second on strings that only change when CPS values change (~16ms intervals).

**Impact:** Measurable CPU overhead on the render thread. Could cause frame drops on low-end hardware.

---

### 2.3 [Medium] `resolve()` called 3x per counter per frame during position editing

**File:** `CpsHudOverlay.java:49,144,170`

During position editing, `HudPositionScreen.render()` calls `renderPreview()` + `getHudWidth()`/`getHudHeight()`, each of which calls `CpsVariable.resolve()`. The same format string is regex-evaluated 2-3 times per counter per frame.

**Impact:** 2-3x redundant regex + HashMap overhead on every frame during drag operations.

---

### 2.4 [Low] `Long` autoboxing on every click/keypress in `CpsTracker`

**File:** `CpsTracker.java:47`

`deque.addLast(System.currentTimeMillis())` autoboxes the `long` return value into a `Long` object. Each mouse click or key press allocates a `Long` object on the heap.

**Impact:** Minor GC pressure. Not a showstopper, but unnecessary for a mod that should aim for zero allocations in hot paths.

---

## 3. CODE SMELLS

### 3.1 [High] HUD rendering logic duplicated 3+ times

**File:** `CpsHudOverlay.java:88-112` (main render), `142-166` (renderPreview), `52-56/93-95` (inline measurement)

The pushMatrix/translate/scale/fill-background/fill-border/drawText/popMatrix pattern for a single counter is written out fully in both `onHudRender()` and `renderPreview()`. The scale clamping `Math.max(0.5, Math.min(2.0, entry.scale))` appears 5 times. The text width formula `textWidth + (INNER_PADDING + BORDER_WIDTH) * 2` appears 4 times.

**Impact:** Any rendering change (e.g., shadow style, padding) must be replicated in multiple places. Bug-prone divergence.

---

### 3.2 [High] Text field editing logic duplicated 6 ways

**File:** `ModConfigScreen.java:519-528` (fieldIndex 0 keyPressed), `529-538` (fieldIndex 1 keyPressed), `549-553`/`554-558` (charTyped)

The entire backspace/left/right/delete/enter handling is copy-pasted for name vs displayFormat fields. The only differences are which `List` and which `CounterEntry` field is updated.

**Impact:** 6x code repetition. Any new editing feature (Ctrl+A, Home/End, clipboard) requires changes in 6 places.

---

### 3.3 [High] God class: `ModConfigScreen` at 569 lines

**File:** `ModConfigScreen.java`

This single class handles: sidebar rendering, card rendering, text field editing with cursors, toggle/slider/color-swatch/add-button rendering, delete with confirmation, scroll management, animation ticking, preset cycling, custom position launching, color picker launching, and all corresponding click/drag/scroll/key/char event handling. It completely ignores Minecraft's `addDrawableChild()` widget system.

**Impact:** Extremely difficult to maintain, test, or extend. No focus management, no tab navigation, no accessibility.

---

### 3.4 [Medium] `drawBorder()` duplicated across files

**File:** `ColorPickerScreen.java:175-179`, `ModConfigScreen.java:324-329`

Identical four-line `drawBorder()` method. Should be a shared utility.

**Impact:** Any rendering change (e.g., shadow style, padding) must be replicated in multiple places.

---

### 3.5 [Medium] Fragile magic-number layout in `getExpandedCardHeight()`

**File:** `ModConfigScreen.java:68-81`

```java
return COLLAPSED_HEIGHT + CARD_PADDING
+ 14 + 20 + 4 // Name label + field + gap
+ 14 + 20 + 4 // Format label + field + gap
+ 18 // Variables hint
...
```

These raw integers must exactly mirror the rendering code's `cy +=` increments, but there is no structural coupling. One missed update and the card height is wrong, causing clipping or empty space.

**Impact:** Ticking time bomb — every UI change requires manually recalculating this method.

---

### 3.6 [Medium] Hardcoded accent color `0xFF4FC3F7` in 8+ locations

**Files:** `ColorPickerScreen.java`, `ModConfigScreen.java`, `HudPositionScreen.java`

The entire dark-theme palette (`0xFF1E1E1E`, `0xFF2A2A2A`, `0xFF3A3A3A`, `0xFF555555`, `0xFF4FC3F7`, etc.) is hardcoded inline. No named constants, no theme system.

**Impact:** No way to theme the mod. Accent color changes require finding and updating 8+ locations.

---

### 3.7 [Medium] Duplicate startup banner

**File:** `UltimatePvPUtils.java:16-20`, `UltimatePvPUtilsClient.java:39-43`

Identical 5-line ASCII-art banner with hardcoded `"v1.0"`. The `[INFO] Initializing...` followed immediately by `[INFO] Initialized!` with nothing between them is misleading.

**Impact:** Version skew risk. Log noise with no actual diagnostic value.

---

### 3.8 [Medium] Inconsistent key naming: semantic vs numeric

**File:** `CpsTracker.java:22-28`, `CpsVariable.java:173-188`

Mouse clicks use semantic names (`"mouse.left"`, `"mouse.right"`). Keyboard presses use raw integer GLFW codes (`"keyboard.65"`). The naming scheme is inconsistent — users writing `%cps_keyboard.65%` have no idea what key that is.

**Impact:** Poor UX for format string authoring. No reverse-lookup from code to name.

---

### 3.9 [Low] `CpsVariable` is not a variable

**File:** `CpsVariable.java` (entire class)

The class is a stateless utility with one `resolve()` method. A more accurate name would be `CpsFormatter` or `CpsTokenResolver`.

**Impact:** Code bloat. 30+ entries in a static initializer that run whether or not they're used.

---

### 3.10 [Low] 30+ dead `KEY_ALIASES` entries

**File:** `CpsVariable.java:106-156`

`print_screen`, `scroll_lock`, `pause`, `left_super`, `right_super`, `num_lock`, `world1`, `world2`, `menu`, F13-F25 — no user will ever need a CPS counter for these keys.

**Impact:** Code bloat. 30+ entries in a static initializer that run whether or not they're used.

---

## 4. ARCHITECTURAL ISSUES

### 4.1 [High] Direct mutation + save on every keystroke

**File:** `ModConfigScreen.java` (8+ call sites)

The config screen directly writes to `CounterEntry` fields and calls `config.save()` after every single mutation. Every character typed in a text field triggers a full JSON serialization + disk write. There is no undo/cancel mechanism — changes are persisted immediately and irrevocably.

**Impact:** Excessive disk I/O (11 file writes for typing "CPS Counter"). No way to discard changes. If the game crashes mid-edit, the partially modified config is already saved. Violates the standard "Apply/Done vs Cancel" workflow.

---

### 4.2 [Medium] No `HudElement` abstraction

**File:** `CpsHudOverlay.java` (entire class)

The overlay directly knows about `CounterEntry`, `CpsVariable`, `ModConfig`, and rendering details. If a future HUD element is added (FPS counter, ping display, kill counter), there is no interface to implement. Everything is hardcoded for CPS counters.

**Impact:** Adding any new HUD feature requires modifying `CpsHudOverlay` rather than adding a new class.

---

### 4.3 [Medium] Global mutable `snappingActive` flag

**File:** `CpsHudOverlay.java:20`

`snappingActive` is a `private static` boolean controlled by `HudPositionScreen`. If that screen is replaced without `close()` being called (e.g., by another mod or an exception), snapping remains permanently active.

**Impact:** HUD counters will snap to each other during normal gameplay until the mod is reloaded.

---

### 4.4 [Medium] Parallel shadow state in `ModConfigScreen`

**File:** `ModConfigScreen.java:34-39`

Five parallel lists (`cardFieldTexts`, `cardFieldCursors`, `nameFieldTexts`, `nameFieldCursors`, `cardAnimProgress`) must be manually synchronized with `syncFieldTexts()`. Adding more fields to `CounterEntry` requires updating this fragile sync mechanism.

**Impact:** Error-prone state management. The sync method is already complex.

---

### 4.5 [Medium] `CpsTracker` manual singleton — untestable

**File:** `CpsTracker.java:10`

`public static final CpsTracker INSTANCE = new CpsTracker()` makes it impossible to test, reset, or create alternate instances. No reason this can't be a single shared instance managed by the mod initializer.

**Impact:** Cannot write unit tests without affecting global state.

---

### 4.6 [Medium] Keybinding category key mismatch

**File:** `UltimatePvPUtilsClient.java:20`

`KeyBinding.Category.create(Identifier.of("ultimatepvputils", "keybindings"))` creates category key `"ultimatepvputils:keybindings"`, but the lang file has `"key.categories.ultimatepvputils"`. These don't match, so the category may display as a raw identifier string instead of "Ultimate PvP Utils" in the controls screen.

**Impact:** Keybinding category may show as `ultimatepvputils:keybindings` instead of the translated name.

---

## 5. INCOMPLETE / HALF-BAKED FEATURES

### 5.1 [High] No validation on any `CounterEntry` field

**File:** `CounterEntry.java` (entire class)

All fields are `public` with no validation. `name` can be null/empty, `scale` can be negative/zero/oversized, `posX`/`posY` can be outside 0-1, `presetName` can be arbitrary. `CpsHudOverlay` defensively clamps some values, but this treats symptoms rather than enforcing invariants at the data level.

**Impact:** Corrupted or manually-edited config files can cause rendering crashes or visual glitches.

---

### 5.2 [Medium] Sidebar with one tab — wasted space

**File:** `ModConfigScreen.java:141-156`

The sidebar renders a single hardcoded "CPS Counter" item. With only one tab, the sidebar serves no navigational purpose and consumes 120px of horizontal space.

**Impact:** Wasted screen space on smaller displays. The sidebar adds visual clutter without functionality.

---

### 5.3 [Medium] No "Reset to Defaults" functionality

**Files:** `ModConfig.java`, `ModConfigScreen.java`

If a user misconfigures a counter, their only option is to manually edit the JSON file or delete it and restart. No in-game reset button exists.

**Impact:** Poor recovery UX from misconfiguration.

---

### 5.4 [Medium] Non-clickable documentation URL

**File:** `ModConfigScreen.java:247`

`"Available Variables can be found in https://notnawfas.qzz.io/blog/ultimatepvputils"` is rendered as plain text — not clickable, not selectable, not copyable.

**Impact:** Users must manually retype the URL from the screen. Effectively useless.

---

### 5.5 [Medium] Custom text fields lack basic editing features

**File:** `ModConfigScreen.java:516-563`

Only supports: typing, backspace, delete, left/right arrow, enter. Missing: Ctrl+A, Home, End, Ctrl+Backspace, Ctrl+C/V/X, double-click selection, Shift+arrow selection. This is severely degraded compared to standard Minecraft text fields.

**Impact:** Extremely frustrating text editing, especially for long variable tokens like `%cps_mouse.left%`.

---

### 5.6 [Medium] No text overflow / horizontal scrolling in text fields

**File:** `ModConfigScreen.java:223-244`

Long format strings that exceed the visible width of the text field overflow past the right edge. No clipping, no scrolling.

**Impact:** Long display format strings become unreadable and uneditable past the visible width.

---

### 5.7 [Medium] Missing "Center" preset position

**File:** `CounterEntry.java:27-35`, `ModConfigScreen.java:27`

Presets cover four corners and "Custom", but no "Center", "Top-Center", or "Bottom-Center" — common HUD positions.

**Impact:** Users must manually position counters at center positions using the drag screen.

---

### 5.8 [Info] Unused translation keys in `en_us.json`

`ultimatepvputils.config.option.useMinecraftFont`, `ultimatepvputils.config.option.dragToReposition`, `ultimatepvputils.screen.reposition.title`, `ultimatepvputils.config.category.position` — remnants from an earlier design or planned features never implemented.

---

## 6. UI/UX ISSUES

### 6.1 [High] Config saved to disk on every keystroke

**File:** `ModConfigScreen.java:523,527,533,537,553,558`

Every character typed in name or format field immediately serializes and writes the entire config JSON. Typing "CPS Counter" triggers 11 disk writes.

**Impact:** Excessive disk I/O. On HDDs, this causes noticeable stutter. On SSDs, contributes to write amplification.

---

### 6.2 [Medium] Color swatch discards alpha — users can't see transparency

**File:** `ModConfigScreen.java:249`

```java
dc.fill(x, y, x + w, y + h, color | 0xFF000000);
```

The `| 0xFF000000` forces alpha to 255, so the swatch always appears fully opaque. Users setting a semi-transparent background color have no visual feedback.

**Impact:** Users cannot see what transparency they've set for background color.

---

### 6.3 [Medium] SB field renders in 8px blocks — low visual resolution

**File:** `ColorPickerScreen.java:112-119`

The saturation-brightness field renders in 8x8 pixel blocks. Mouse interaction maps to continuous values, but the visual rendering shows a blocky grid. The selected color may differ visually from what is shown.

**Impact:** Misleading visual feedback. Fine-grained color selection is difficult.

---

### 6.4 [Medium] No visual feedback on color picker "Done" button

**File:** `ColorPickerScreen.java:225`

Clicking "Done" calls `onComplete` and closes immediately — no press animation, no sound, no confirmation flash.

**Impact:** Users may click "Done" multiple times, unsure if it worked.

---

### 6.5 [Low] Toggle switch hit area extends beyond visual bounds

**File:** `ModConfigScreen.java:288-295`

The toggle visual is drawn at 16px height but the hover/click area is 20px. The 2px invisible extension above and below creates slight visual inconsistency.

---

### 6.6 [Low] No keyboard shortcut hint for F7 config key

**File:** `UltimatePvPUtilsClient.java:26-31`

The F7 keybinding is registered but never communicated to the user in-game. No tooltip, no first-launch chat message.

**Impact:** Reduced feature discoverability.

---

## 7. BUILD & CONFIGURATION

### 7.1 [Info] Hardcoded absolute Java path in `gradle.properties`

**File:** `gradle.properties:2`

`org.gradle.java.home=/home/notnawfas/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/java/java-runtime-delta`

Non-portable. Other developers cannot build without modifying this.

---

### 7.2 [Info] Missing `LICENSE` file referenced by `build.gradle`

**File:** `build.gradle:43`

`jar { from 'LICENSE' }` references a LICENSE file that does not exist in the project root. The JAR will ship without one, contradicting `fabric.mod.json`'s `"license": "All Rights Reserved"`.

---

### 7.3 [Info] `fabric.mod.json` contact field is empty

**File:** `fabric.mod.json:10`

`"contact": {}` is empty. ModMenu uses this for homepage and issue tracker links. Available metadata (`github.com/notnawfas`, `contact@notnawfas.qzz.io`) is not referenced.

---

### 7.4 [Info] `fabric-api: "*"` wildcard dependency

**File:** `fabric.mod.json:32`

Accepts ANY version of Fabric API, including potentially incompatible future versions.

---

### 7.5 [Info] Version `"v1.0"` hardcoded in log output

**File:** `UltimatePvPUtils.java:17`, `UltimatePvPUtilsClient.java:40`

Should reference the build version from `gradle.properties` rather than hardcoding.

---

## SUMMARY

| Severity | Count | Key Themes |
|----------|-------|------------|
| **Critical** | 2 | Keyboard CPS broken; color picker renders ~730 GPU calls/frame |
| **High** | 9 | God class; regex/frame; non-atomic saves; no validation; duplicate logic; save-on-keystroke |
| **Medium** | 20 | Fragile layout math; global state; singleton misuse; missing features; UX gaps |
| **Low** | 5 | Magic numbers; naming; hit areas; discoverability |
| **Info** | 5 | Build portability; missing files; dead translation keys |

### Top 5 Priority Fixes

1. **Fix `KeyboardHandlerMixin` parameter order** — keyboard CPS is completely non-functional
2. **Cache/batch `ColorPickerScreen` rendering** — pre-bake SB field/hue bar to textures, eliminate per-pixel `fill()` calls
3. **Implement deferred config saves** — batch mutations, save only on screen close or explicit "Apply", write atomically via temp-file-rename
4. **Extract duplicated rendering and editing logic** — shared `drawCounter()`, parameterized text field handler, shared `DrawUtils.drawBorder()`
5. **Add field validation to `CounterEntry`** — enforce invariants at the data layer, validate on config load
