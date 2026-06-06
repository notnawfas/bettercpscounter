# UltimatePvPUtils — Deep-Scan Code Quality Audit (UI-Focused)

**Date:** 2026-06-04
**Scope:** 14 Java source files, 3 resource JSONs, 3 Gradle build files, 1 icon asset
**Total Lines Audited:** ~1,700 (Java) + ~70 (resources/config)
**Focus:** UI/UX layer — all rendering, interaction, screen, overlay, and visual-output code
**Previous Audit:** report.md (2026-06-03) — backed up to `backups/reports/report-0.md`

---

## Severity Legend

| Level | Meaning |
|-------|---------|
| **Critical** | Will cause crashes, data loss, or fundamental broken behavior |
| **High** | Major quality issue — buggy, laggy, or architecturally unsound |
| **Medium** | Notable smell or missing feature that degrades maintainability or UX |
| **Low** | Minor inconsistency or cosmetic issue |
| **Info** | Observation, no immediate action required |
| **Δ** | Delta — new finding not in previous report |

---

## 1. BUGS & POTENTIAL CRASHES (UI-Related)

### 1.1 [Critical] Keyboard CPS is fundamentally broken — inverted parameter mapping

**File:** `KeyboardHandlerMixin.java:15-16`

```java
private void onKey(long window, int action, KeyInput keyInput, CallbackInfo ci) {
    CpsTracker.INSTANCE.onKeyPress(keyInput.key(), action);
}
```

The mixin handler signature is `(long window, int action, KeyInput keyInput, CallbackInfo)`. The call passes `keyInput.key()` (the GLFW key code, e.g. 65 for 'A') as the first argument and the raw `int action` as the second. But `CpsTracker.onKeyPress(int keyCode, int action)` checks `if (action == 1)` to filter press events. The GLFW key code will almost never equal 1 (only GLFW_KEY_2 = 1), so keyboard CPS tracking is **completely dead**. Any format token like `%cps_keyboard.w%` always renders as `0`.

**Impact:** The entire keyboard CPS feature is non-functional. Users adding keyboard CPS tokens see permanently zero values — a core advertised feature simply does not work.

---

### 1.2 [Critical Δ] `handleCpsTabClick` enters dead code after empty-list early return

**File:** `ModConfigScreen.java:392-438`

```java
if (counters.isEmpty()) {
    if (mx >= x && mx < x + w && my >= cy + 40 && my < cy + 62) {
        counters.add(new CounterEntry(...));
        syncFieldTexts(); config.requestSave(); return;
    }
    // ↓ DEAD CODE — this loop is inside the `if (counters.isEmpty())` block
    for (int i = 0; i < counters.size(); i++) {   // counters is EMPTY here
        ...
    }
    ...
}
```

The `for` loop and the add-button click target at the bottom of `handleCpsTabClick` are placed *inside* the `if (counters.isEmpty())` block. Since `counters.isEmpty()` is true, the loop body never executes, and the second add-button target (lines 435-438) is the only one that can fire. The logic is clearly a copy-paste error — the loop should be in an `else` block. This means:

- When the list is empty, only the first add button (inside the `if`) works.
- The second add button click target (lines 435-438) is dead code for the empty case.
- When the list is non-empty, the loop correctly iterates, but the add button logic at lines 434-438 is now outside the `if` block and works correctly.

**Impact:** While the add button technically works in both cases (by different code paths), the structural error means any future change to the empty-state layout will likely break click handling. The dead loop is misleading and wastes developer time during debugging.

---

### 1.3 [High Δ] `CardAnimProgress` map stores stale entries for deleted counters

**File:** `ModConfigScreen.java:409, 116`

When a counter is deleted at index `i`, the code calls `cardAnimProgress.remove(i)` but does NOT re-index the remaining entries. If counters at indices 2, 3, 4 exist and counter 2 is deleted, indices 3 and 4 shift to 2 and 3, but `cardAnimProgress` still has entries keyed by 3 and 4 (now orphaned) while missing keys for the shifted 2 and 3. The `removeIf` on line 116 cleans up out-of-range keys, but between the delete and the next render, the animation state is misaligned — the expanded card may flash to the wrong animation progress.

**Impact:** After deleting a counter, the next counter's animation state is briefly wrong, causing a visual glitch (collapsed card briefly showing expanded height or vice versa).

---

### 1.4 [High Δ] `drawCard` renders outside scissor when card is partially scrolled off-screen

**File:** `ModConfigScreen.java:209-326`

The scissor region is set as `(x, y + COLLAPSED_HEIGHT, x + w, y + COLLAPSED_HEIGHT + visibleBodyH)`, where `visibleBodyH = cardH - COLLAPSED_HEIGHT`. When `scrollOffset` makes `y` negative, the scissor's bottom edge can exceed the viewport, and the top edge can be negative. Minecraft's `enableScissor` uses `GL11.glScissor` which clips to window bounds, so negative coordinates are clamped to 0 — but the scissor region calculation doesn't account for this, meaning expanded card bodies may render partially outside the visible scroll area.

**Impact:** Expanded card content bleeds visually above/below the intended scroll region when scrolled near edges.

---

### 1.5 [High Δ] `drawSlider` knob position inverts when value is at minimum

**File:** `ModConfigScreen.java:351-358`

```java
double ratio = (value - 0.5) / 1.5;
int filledW = (int) (w * ratio);
int knobCenter = Math.max(4, Math.min(w - 4, filledW));
```

When `value = 0.5` (minimum), `ratio = 0.0`, `filledW = 0`, `knobCenter = 4`. The knob renders at pixel 4 from the left, not at pixel 0. The visual position doesn't match the actual value — the slider appears to represent ~0.6 instead of 0.5. The `Math.max(4, ...)` clamps the knob to a minimum offset of 4px regardless of the actual value, creating a visual offset at the lower bound.

**Impact:** Slider knob position is visually inaccurate at the lower end — users see the knob at a position that doesn't match the displayed "0.5x" value.

---

### 1.6 [Medium] `pendingDeleteIndex` can become stale after list mutations

**File:** `ModConfigScreen.java:49-51, 405-416`

The `pendingDeleteVersion` field (storing `counters.size()` at the time of first click) partially mitigates stale-index issues, but there's a subtle bug: if a counter is *added* (increasing `counters.size()`), the version check `pendingDeleteVersion == counters.size()` will never match, so the confirm-click silently does nothing. The user sees the "Confirm?" prompt but clicking it has no effect — they must click the delete icon again to start over.

**Impact:** Confusing UX — clicking "Confirm?" after an add operation is a no-op with no feedback.

---

### 1.7 [Medium Δ] `mouseDragged` slider uses hardcoded layout coordinates instead of actual card position

**File:** `ModConfigScreen.java:536-544`

```java
int cx = SIDEBAR_WIDTH + PADDING + CARD_PADDING;
int cw = width - SIDEBAR_WIDTH - PADDING * 2 - CARD_PADDING * 2;
double ratio = Math.max(0, Math.min(1, (double) ((int) click.x() - cx) / cw));
```

The slider drag handler uses a fixed `cx` derived from `SIDEBAR_WIDTH + PADDING + CARD_PADDING`, but the actual card's x-position depends on scroll state and card index. The slider's mouse click handler in `handleExpandedCardClick` (line 488-493) uses the card's actual `cx` and `cw` values, but `mouseDragged` uses the simplified formula. If the card's content area width differs (e.g., due to scrollbar presence), the drag ratio will differ from the click ratio, causing the slider to "jump" when the user starts dragging.

**Impact:** Slider value jumps slightly when starting a drag, creating a jarring snap.

---

### 1.8 [Medium Δ] `resolveCached` cache key uses mouse-only CPS sum — cache misses for keyboard-only tokens

**File:** `CpsHudOverlay.java:40-48`

```java
int currentCps = CpsTracker.INSTANCE.getCps("mouse.left") + CpsTracker.INSTANCE.getCps("mouse.right");
String cacheKey = format + "@" + currentCps;
```

The cache key only includes the sum of left+right mouse CPS. If a counter's format contains only keyboard tokens (e.g., `%cps_keyboard.w%`), the cache will never invalidate when keyboard CPS changes — because `currentCps` only tracks mouse. The counter will display stale keyboard CPS values until the mouse CPS changes.

**Impact:** Keyboard-only CPS counters show outdated values, updating only when mouse buttons are clicked. This directly undermines the keyboard CPS feature and makes it look even more broken than it already is (due to bug 1.1).

---

### 1.9 [Medium Δ] `ColorPickerScreen` Cancel button does NOT call `onComplete` — user changes are silently discarded

**File:** `ColorPickerScreen.java:221`

```java
if (mx >= px && mx < px + btnW && my >= btnY && my < btnY + btnH) { close(); return true; }
```

The Cancel button closes the screen without calling `onComplete`, which is correct behavior for cancel. However, since `ModConfigScreen` uses `requestSave()` (deferred save) and the color picker's `onComplete` callback is what triggers `config.requestSave()`, cancelling means the color change is NOT saved. But the `CounterEntry` in memory was already mutated at the time the color picker was opened — no, actually it wasn't. The `onComplete` lambda writes to `counters.get(idx).backgroundColor = c`. So cancel correctly avoids mutation.

**Revised Impact:** Cancel works correctly. However, there is NO visual distinction between Cancel and Done buttons — both look identical (same `drawTextButton` call). Users may click the wrong one.

---

### 1.10 [Low Δ] `drawCard` delete hit area includes the confirm prompt area

**File:** `ModConfigScreen.java:404-418`

When the "Confirm?" prompt is showing, clicking anywhere in the `mx >= x + w - 18` region triggers the confirm logic. But the "Confirm?" text and its background are rendered at `confirmX - 3` which could be 50+ pixels to the LEFT of the delete icon. The click hit area doesn't cover the "Confirm?" visual — it only covers the delete icon's 18px region. Users clicking the word "Confirm?" get no response.

**Impact:** Clicking the visible "Confirm?" text does nothing — only clicking the `✕` icon confirms the delete. Counter-intuitive.

---

## 2. PERFORMANCE BOTTLENECKS (UI-Related)

### 2.1 [Critical] ~730 GPU draw calls per frame for color picker

**File:** `ColorPickerScreen.java:117-163`

Breakdown per frame:
- SB field: 10×10 = 100 `fill()` calls at `step=16` for a 150×150 field → actually `ceil(150/16)^2 = 10*10 = 100` fills
- Hue bar: `ceil(150/10) = 15` fills
- Alpha bar: 150 pixel-width fills + 150 `drawCheckerboard` calls (each with `ceil(1/4) * ceil(14/4) ≈ 1*4 = 4` fills per call) = 150 + 600 = 750 fills
- Preview checkerboard: `ceil(40/4)^2 = 100` fills
- Preview color: 1 fill

**Revised total: ~100 + 15 + 750 + 100 + 1 ≈ 966 GPU draw calls per frame**, significantly worse than the original report's estimate of ~730.

Each `dc.fill()` is a separate immediate-mode GPU quad submission with no batching. At 60 FPS while dragging, that's ~58,000 draw calls/second.

**Impact:** Severe frame-rate degradation on any GPU while the color picker is open. The alpha bar is the worst offender — per-pixel rendering with per-pixel checkerboard underlays.

---

### 2.2 [High] Regex execution every frame for every counter

**File:** `CpsVariable.java:73-85`, called via `CpsHudOverlay.resolveCached()`

The `resolveCache` mitigates this (added since original report), but the cache key is flawed (see bug 1.8). On cache misses, `CpsVariable.resolve()` still creates a `Matcher` and runs regex against the format string. With the mouse-only cache key, keyboard-token counters will cache-miss on every frame where mouse CPS changes (which is every frame during active clicking).

**Impact:** During active mouse clicking with keyboard-token counters present, regex runs every frame per keyboard counter.

---

### 2.3 [High Δ] `drawCheckerboard` is O(cells) per call — called ~650 times per frame in color picker alpha bar

**File:** `DrawUtils.java:38-47`, `ColorPickerScreen.java:150-155`

```java
// ColorPickerScreen.drawAlphaBar — per pixel:
for (int px = 0; px < BAR_WIDTH; px++) {
    DrawUtils.drawCheckerboard(dc, x + px, y, 1, BAR_HEIGHT);  // 1px wide, 14px tall
    ...
}
```

Each `drawCheckerboard` call for a 1×14 area renders ~4 fill calls. Called 150 times for the alpha bar = 600 fill calls. This is the single biggest performance offender in the entire codebase.

**Impact:** The alpha bar alone generates 600+ GPU draw calls per frame. Combined with the rest of the color picker, total draw calls approach ~1000/frame.

---

### 2.4 [Medium] `resolve()` called redundantly during position editing

**File:** `CpsHudOverlay.java:60-67`, `HudPositionScreen.java:44-47`

The `rc()` method in `HudPositionScreen` calls `CpsHudOverlay.resolve(counter)` on every invocation, and it's called multiple times per render frame (for position calculation, for rendering, for resize handle detection). The `cached` field is overwritten each time, providing no actual caching benefit.

**Impact:** 2-3x redundant resolve calls per frame during position editing. With the cache, this is mitigated to HashMap lookups, but the redundant `computeHudWidth`/`computeHudHeight` math still runs.

---

### 2.5 [Low Δ] `elideText` does linear-scan character removal — O(n²) worst case

**File:** `ModConfigScreen.java:518-527`

```java
while (pos > 0 && textRenderer.getWidth(text.substring(0, pos)) > limit) pos--;
```

Each `textRenderer.getWidth()` call is non-trivial (it iterates glyphs). For a string that needs significant truncation, this loops `n` times with an O(m) width calculation each time → O(n·m). In practice, format strings are short, so this is not a live performance issue, but it's amateurish.

**Impact:** Negligible for current use, but structurally inefficient.

---

## 3. UI/UX ISSUES — COMPREHENSIVE DEEP-DIVE

### 3.1 [High Δ] Entire custom UI widget system re-implemented from scratch — zero accessibility

**File:** `ModConfigScreen.java`, `ColorPickerScreen.java`, `HudPositionScreen.java`

All three screens completely bypass Minecraft's `addDrawableChild()` widget system. There are no `ClickableWidget` instances, no focus management, no tab navigation, no screen reader support. Every element is manually positioned, manually hit-tested, and manually rendered. This means:

- **No keyboard navigation** between fields (Tab/Shift+Tab don't work)
- **No screen reader / Narrator support** — completely invisible to accessibility tools
- **No focus cycling** — the only "focus" concept is the custom `focusedCardIndex`/`focusedFieldIndex`, which doesn't integrate with Minecraft's focus system
- **No tooltip system** — hover states are manually implemented per-element

**Impact:** The mod is entirely inaccessible to visually impaired players or keyboard-only users. This is a significant UX regression compared to using vanilla widgets.

---

### 3.2 [High Δ] No visual distinction between "Cancel" and "Done" buttons in color picker

**File:** `ColorPickerScreen.java:108-109`

```java
drawTextButton(dc, "Cancel", px, cy, btnW, btnH, mouseX, mouseY);
drawTextButton(dc, "Done", px + btnW + 4, cy, btnW, btnH, mouseX, mouseY);
```

Both buttons use the identical `drawTextButton` method with identical styling. There is no color differentiation, no accent on the "Done" button, no hover sound, and no press animation. The only differentiation is the text label, which requires reading. This violates the UX principle of visual hierarchy for destructive vs. confirmatory actions.

**Impact:** Users can easily click "Cancel" when they mean "Done", losing their color selection with no undo. No visual feedback on which is the "primary" action.

---

### 3.3 [High Δ] Card expand/collapse click target is the entire collapsed header — no distinction from text field click

**File:** `ModConfigScreen.java:402-423`

When clicking on a collapsed card, the entire `COLLAPSED_HEIGHT` (28px) region is the expand/collapse target. But when the card is expanded, clicking the same header region does NOT collapse it — instead, clicking the arrow (24px wide) is the only way to collapse. This asymmetry is confusing:

- Click header → expands
- Click header again → does NOT collapse (falls through to expanded body click handling)
- Must click the small ▶/▼ arrow to collapse

**Impact:** Users expect clicking the header again to collapse the card (standard accordion behavior). Instead, nothing visible happens, and they must hunt for the tiny arrow target.

---

### 3.4 [High Δ] Text field cursor is invisible half the time — blink rate tied to system time, not render ticks

**File:** `ModConfigScreen.java:256-258, 271-273`

```java
if (focusedCardIndex == index && focusedFieldIndex == 1 && (System.currentTimeMillis() / 500) % 2 == 0) {
```

The cursor blink uses `System.currentTimeMillis() / 500 % 2`, giving a 500ms on / 500ms off cycle. This is:
1. **Too slow** — standard text cursors blink at ~530ms but feel responsive because they sync with typing. Here, typing a character doesn't reset the blink timer, so the cursor may stay invisible for up to 500ms after typing.
2. **Frame-unaware** — at low FPS, the blink may skip the "on" frame entirely, making the cursor disappear for full seconds.

**Impact:** Cursor feels unresponsive and "laggy." After typing, the cursor may remain invisible for half a second, giving the impression that the field lost focus.

---

### 3.5 [High Δ] No cursor reset when switching focus between fields

**File:** `ModConfigScreen.java:449, 455`

When clicking on a text field to focus it, the cursor position is NOT set to the click location. The cursor stays at whatever position it was in the previous session with that field. There is no `setCursorFromMouseX()` logic at all.

**Impact:** Clicking on a text field places the cursor at a seemingly random position (wherever it was last time), not where the user clicked. This is the #1 most clunky interaction in the entire UI.

---

### 3.6 [High Δ] No clipping on text field rendering — text overflows past field bounds

**File:** `ModConfigScreen.java:253-261, 267-276`

Text fields render via `dc.drawTextWithShadow` with no scissor clipping. Long strings overflow past the right edge of the field, overlapping adjacent UI elements. The `elideText` helper only applies to the *displayed* text, but the cursor-insertion pipe `|` is added BEFORE elision, meaning the pipe position in the elided text doesn't match the actual cursor position.

```java
nameDisplay = nameText.substring(0, Math.min(cursor, nameText.length())) + "|"
    + nameText.substring(Math.min(cursor, nameText.length()));
String visibleName = elideText(nameDisplay, fieldW - 8);
```

If `cursor` is at position 30 and the field shows 15 characters + "...", the pipe `|` is at position 30 in the full string, but `elideText` truncates at position ~12 + "...". The pipe is included in the elided text at position 30, which is past the truncation point — so it gets cut off by `elideText`. The cursor is invisible when it's past the visible text range.

**Impact:** (1) Long text overflows visually. (2) Cursor is invisible when positioned past the visible truncation point. No horizontal scrolling to bring the cursor into view.

---

### 3.7 [High Δ] No horizontal text scrolling — cursor can escape the visible field area entirely

**File:** `ModConfigScreen.java:251-276`

Standard text field implementations scroll the text horizontally to keep the cursor visible. This custom implementation has no scroll offset. As the user types past the visible width:

1. New characters are added but invisible (off-screen to the right)
2. The cursor `|` is invisible (elided away)
3. The user has no idea what they're typing or where the cursor is
4. Backspace appears to do nothing (deleting invisible characters)

**Impact:** Text fields become completely unusable for strings longer than ~25 characters. Format strings like `[LMB: %cps_mouse.left% | RMB: %cps_mouse.right% | W: %cps_keyboard.w%]` are 63 characters and impossible to edit correctly.

---

### 3.8 [High Δ] Sidebar "HUD Enabled" toggle has wrong Y-coordinate hit area

**File:** `ModConfigScreen.java:377-379`

```java
int toggleX = 8, toggleY = 28, toggleW = SIDEBAR_WIDTH - 16, toggleH = 20;
if (mx >= toggleX && mx < toggleX + toggleW && my >= toggleY && my < toggleY + toggleH) {
```

But in `drawSidebar` (line 157-161), the toggle is drawn at `y = 24 + 4 = 28` for the toggle row, which starts at `y = 24` with a `y + 4` offset for the toggle widget. The rendering uses `drawToggleRow(dc, "HUD Enabled", config.enabled, x + 8, y + 4, ...)` where `y` starts at 24, so the toggle is at Y=28. The click handler uses `toggleY = 28`. This actually matches — but the `drawToggleRow` method positions the toggle at `y + 2` to `y + toggleH - 2` (i.e., Y=30 to Y=48 for the visual), while the click target is Y=28 to Y=48. The 2px mismatch means the click area extends 2px above the visual toggle.

**Impact:** Minor — clicking 2px above the visible toggle still registers. Not game-breaking but contributes to the "slightly off" feel throughout the UI.

---

### 3.9 [High Δ] `drawToggleRow` toggle visual and hit area are misaligned

**File:** `ModConfigScreen.java:328-335`

```java
int toggleW = 40, toggleH = 20, toggleX = x + w - toggleW;
boolean hovered = mx >= toggleX && mx < toggleX + toggleW && my >= y + 2 && my < y + toggleH - 2;
dc.fill(toggleX, y + 2, toggleX + toggleW, y + toggleH - 2, value ? ACCENT : TOGGLE_OFF);
dc.fill(value ? toggleX + toggleW - 12 : toggleX + 2, y + 3, value ? toggleX + toggleW - 2 : toggleX + 12, y + toggleH - 3, 0xFFFFFFFF);
```

The toggle background renders from `y+2` to `y+18` (16px visual height). The knob renders from `y+3` to `y+17` (14px). The hit area checks `my >= y + 2 && my < y + toggleH - 2` = `y+2` to `y+18` — this matches the visual. BUT the `drawSidebar` call passes `y + 4` as the Y parameter, and the click handler in `mouseClicked` uses absolute Y=28. The rendering Y for the toggle background = 24 + 4 + 2 = 30. The click hit area Y = 28. There's a 2px gap between the top of the click area and the top of the visual.

**Impact:** The toggle click region extends 2px above its visual boundary. On the sidebar, this overlaps with the title area.

---

### 3.10 [Medium Δ] Sidebar provides zero navigation value — single tab with no alternative views

**File:** `ModConfigScreen.java:156-171`

The entire sidebar renders:
1. A "HUD Enabled" toggle
2. A separator line
3. A single "CPS Counter" tab item with an active indicator

With only one category, the sidebar is pure chrome — 120px of horizontal space consumed for a toggle that could be at the top of the main content area. On a 1280px-wide window, this wastes ~9.4% of horizontal space.

**Impact:** Significant horizontal space waste. The sidebar is prototype-grade scaffolding for a multi-tab design that was never implemented.

---

### 3.11 [Medium Δ] No scroll indicator when content overflows — users can't tell if scrolling is available

**File:** `ModConfigScreen.java:196-206`

The scrollbar only renders when `maxScroll > 0`, but there is NO visual indicator that content is scrollable when the user hasn't scrolled yet (scrollbar is at the top, the thumb overlaps the track, making it nearly invisible). The scrollbar track is `0x20FFFFFF` (very transparent) and the thumb is `0x60FFFFFF` (semi-transparent). At the top position, both are nearly invisible against the dark background.

**Impact:** Users with multiple counters may not realize they can scroll. The first counter below the fold is effectively hidden.

---

### 3.12 [Medium Δ] Scroll offset resets to 0 when adding a new counter — counter appears at the bottom, but view stays at top

**File:** `ModConfigScreen.java:394-397, 436-438`

Adding a counter appends to the list, but `scrollOffset` is not adjusted to show the new counter. The user clicks "Add New Counter" and nothing visible happens — the new counter is created below the fold with no auto-scroll.

**Impact:** Adding a counter appears to do nothing. The user must manually scroll down to find and configure it. Extremely confusing.

---

### 3.13 [Medium Δ] Delete confirmation has no animation — "Confirm?" appears instantly with no visual emphasis

**File:** `ModConfigScreen.java:227-236`

When the delete flow triggers, the `✕` icon instantly turns orange and a "Confirm?" label appears next to it. There is no fade-in, no scale animation, no background pulse — just an abrupt text swap. The "Confirm?" text is 50px wide and appears at `deleteX - 50`, which may overlap with the counter name text, creating visual confusion.

**Impact:** The delete confirmation feels like a visual glitch rather than an intentional prompt. Easy to miss.

---

### 3.14 [Medium Δ] Preset cycling button provides no preview of where the counter will move

**File:** `ModConfigScreen.java:301-314, 500-504`

Clicking the preset position button cycles through presets (Top-Left → Top-Center → Top-Right → ...) and immediately applies the position to the `CounterEntry`. But since the HUD is not visible behind the config screen (the screen fills the entire viewport with its dark overlay), the user cannot see where the counter moved. They must close the config screen, check the HUD, then re-open the config to adjust.

**Impact:** Position preset changes are blind — no feedback until the user exits the config screen.

---

### 3.15 [Medium Δ] "Edit Custom Position" button only appears for "Custom" preset — but preset auto-detection fights manual positioning

**File:** `ModConfigScreen.java:317-323`, `CounterEntry.java:77-86`

When the user drags a counter to a position in `HudPositionScreen`, `CounterEntry.detectPreset()` is called, which may snap the preset back to a named preset (e.g., "Top-Left") if the position is within `POS_EPSILON = 0.01` of a preset. This means:

1. User drags counter to (0.005, 0.005) → preset detected as "Top-Left"
2. User re-opens config → "Edit Custom Position" button is GONE (preset is "Top-Left", not "Custom")
3. User can't re-enter the drag screen without first cycling the preset to "Custom"

**Impact:** The position editor is inaccessible after any drag that ends near a preset position. The user must manually cycle presets back to "Custom" to re-open the drag screen.

---

### 3.16 [Medium Δ] Color swatch shows alpha in hex label but checkerboard is hidden behind full-opacity fill

**File:** `ModConfigScreen.java:337-349`

The `drawColorSwatch` method:
1. Fills the swatch background with `0xFFAAAAAA` (gray)
2. Draws a checkerboard (for transparency visualization)
3. Fills the entire swatch with the color value (including alpha)

Step 3 overlays the color (with alpha) on top of the checkerboard. This is correct for showing transparency — the checkerboard shows through semi-transparent colors. However, the original report claimed `color | 0xFF000000` was used — this has been FIXED. The current code correctly renders `color` without forcing alpha. The hex label correctly shows the alpha channel.

**Revised Impact:** This is now working correctly. Downgrading from the previous report's assessment.

---

### 3.17 [Medium Δ] SB field step size changed from 8px to 16px — blocky rendering is worse than originally reported

**File:** `ColorPickerScreen.java:119`

```java
int step = 16;
```

The original report stated 8px blocks. The actual value is `step = 16`, meaning the SB field renders in 16×16 pixel blocks. For a 150×150 field, that's only ~10×10 = 100 quads. The visual resolution is **extremely** coarse — each "pixel" of the color picker represents a 16×16 area, making fine color selection visually impossible. The mouse interaction is continuous (sub-16px precision), but what the user sees doesn't match what they're selecting.

**Impact:** The SB field looks like a 10×10 pixel art grid, not a smooth color gradient. This is amateurish and makes precise color selection nearly impossible.

---

### 3.18 [Medium Δ] Alpha bar renders per-pixel but SB field renders in 16px blocks — inconsistent visual resolution

**File:** `ColorPickerScreen.java:117-163`

The hue bar uses `step = 10` (15 segments), the alpha bar renders per-pixel (`step = 1`), but the SB field uses `step = 16`. This creates a jarring inconsistency: the hue and alpha bars look relatively smooth, but the SB field looks blocky. The user expects the same visual quality across all interactive areas.

**Impact:** The SB field's blocky appearance stands out against the smoother bars, creating an unprofessional, cobbled-together feel.

---

### 3.19 [Medium Δ] "Done" button in color picker has no sound effect or press animation

**File:** `ColorPickerScreen.java:108-109, 165-170`

The `drawTextButton` method renders a hover state but no press state. There is no click sound (`MinecraftClient.getInstance().getSoundManager().play()` is never called). Minecraft's vanilla buttons produce both a visual press animation and a click sound — the absence makes the color picker feel disconnected from the game's UI language.

**Impact:** The button provides insufficient feedback. Users may click multiple times or wonder if their click registered.

---

### 3.20 [Medium Δ] `HudPositionScreen` renders the world behind it — but the dark overlay makes the world barely visible

**File:** `HudPositionScreen.java:51-53, 83-84`

```java
if (this.client != null && this.client.world != null) {
    this.client.gameRenderer.renderWorld(this.client.getRenderTickCounter());
}
// ... later ...
drawContext.fill(0, 0, width, height, 0x30000000);  // 19% opacity overlay
```

The world is rendered but then a 19% opacity dark overlay is drawn on top. This means the world is dimmed but visible — which is the correct behavior for a position editor. However, the "ESC to confirm" text is rendered at the screen center with `0xFFFFFFFF` (full white), which may be hard to read against bright world backgrounds (e.g., snow, desert sand).

**Impact:** "ESC to confirm" text can be hard to read against bright in-game backgrounds.

---

### 3.21 [Medium Δ] Resize handle in `HudPositionScreen` is only 8px — difficult to target on high-DPI displays

**File:** `HudPositionScreen.java:26, 71-81`

The `EDGE_ZONE = 8` defines the resize grab area. On a 4K display at 2x scaling, this is effectively 4 physical pixels — extremely difficult to hit with a mouse. There is no cursor change (no resize cursor icon) to indicate the resize zone.

**Impact:** Resize functionality is nearly undiscoverable without reading documentation. No visual affordance for the resize handle.

---

### 3.22 [Medium Δ] No tooltip or description for any config option

**File:** `ModConfigScreen.java` (entire class)

Every config option (Show Background, Background Color, Text Color, Scale, Position, Display Format) has a label but no description, tooltip, or help text. The `en_us.json` file contains descriptions (`ultimatepvputils.config.option.enabled.description`, `ultimatepvputils.config.option.displayFormat.description`) but they are NEVER used in the UI. Users unfamiliar with the mod must guess what each option does.

**Impact:** The variables documentation link is the only help, but it's non-clickable (issue 5.4 from original report). New users are effectively locked out of understanding the mod's features.

---

### 3.23 [Medium Δ] Animation easing uses raw lerp — feels linear and mechanical

**File:** `ModConfigScreen.java:110`

```java
float next = current + (target - current) * Math.min(ANIM_SPEED * 60f * dt, 0.999f);
```

The animation uses an exponential ease-out (lerp towards target), which is reasonable, but `ANIM_SPEED = 0.18f` combined with `* 60f` means the per-frame factor is `0.18 * 60 * dt ≈ 0.18` at 60 FPS. This produces a ~5-frame animation that feels snappy but abrupt. There is no ease-in or cubic bezier curve — the animation starts at maximum velocity and decelerates.

**Impact:** Card expand/collapse animations feel slightly mechanical rather than smooth. Not a showstopper, but contributes to the "prototype" feel.

---

### 3.24 [Medium Δ] `drawAnimatedBorder` in `HudPositionScreen` uses 4 separate methods for 4 edges — excessive per-frame draw calls

**File:** `HudPositionScreen.java:87-118`

The animated dashed border calls `drawDashedLineH` twice and `drawDashedLineV` twice, each of which loops through the dash/gap pattern calling `dc.fill()` per dash segment. For a 200×30 HUD counter with dash=6, gap=4, period=10:
- Top/bottom edges: ~20 + ~20 = 40 fill calls
- Left/right edges: ~3 + ~3 = 6 fill calls
- Total: ~46 fill calls per frame for the selection border alone

**Impact:** Not as severe as the color picker, but adds unnecessary draw calls for a simple visual indicator. Could be replaced with a pre-baked texture.

---

### 3.25 [Low Δ] HUD counter background border color is always derived from background — no customization

**File:** `CpsHudOverlay.java:180-188`

`deriveBorderColor` darkens the background color by subtracting 50 from each RGB channel and boosting alpha by 40. This means:
- Semi-transparent backgrounds get a MORE opaque border (alpha + 40), which looks inconsistent
- The border is always darker than the background — no option for a light border on a dark background
- The border derivation is invisible to the user — there's no "Border Color" option

**Impact:** Limited visual customization. The derived border can look odd with certain color combinations (e.g., very dark backgrounds produce nearly invisible borders).

---

### 3.26 [Low Δ] Color picker knob rendering uses hard-coded sizes — too small on high-DPI

**File:** `ColorPickerScreen.java:130-133, 143-145, 159-162`

The SB field knob is 8×9 pixels, the hue bar knob is 5×18 pixels, the alpha bar knob is 5×18 pixels. These are fixed pixel sizes that don't scale with the UI. On high-DPI displays, they become tiny and difficult to grab.

**Impact:** Fiddly interaction on high-resolution displays.

---

### 3.27 [Low Δ] No cursor change on hover over interactive elements

**File:** All screen classes

None of the custom screens change the mouse cursor. Minecraft supports `GLFW_CURSOR_HAND` via `GLFW.glfwSetCursor`, but it's never used. Users must discover interactive elements by hovering and watching for color changes.

**Impact:** No visual affordance for clickable elements. Reduces discoverability of interactive features like the resize handle, color swatches, and toggle switches.

---

### 3.28 [Low Δ] "Docs" link underline is manually drawn — renders incorrectly at non-integer scales

**File:** `ModConfigScreen.java:279-283`

```java
dc.drawTextWithShadow(textRenderer, docsText, cx, cy, DrawUtils.ACCENT);
int docsW = textRenderer.getWidth(docsText);
dc.fill(cx, cy + 11, cx + docsW, cy + 12, 0x804FC3F7);
```

The underline is drawn as a 1px `fill` at `cy + 11`. This position is hardcoded relative to the text position, but the text's actual descent varies by font. If a resource pack changes the font, the underline may be too close or too far from the text. Additionally, at non-integer GUI scales, the 1px underline may align between pixels, causing flickering.

**Impact:** The underline is a hacky visual stand-in for a proper clickable link component. It looks slightly off under most conditions.

---

### 3.29 [Low Δ] `drawCard` renders the delete `✕` character — glyph availability depends on font

**File:** `ModConfigScreen.java:219, 228, 235`

The `✕` (U+2715) and `▶`/`▼` (U+25B6/U+25BC) characters are rendered using Minecraft's text renderer, which uses a bitmap font that only supports a subset of Unicode. These characters may render as `☐` (replacement glyph) with some language/font configurations.

**Impact:** Card expand/collapse arrows and delete icons may display as empty boxes on certain system configurations.

---

### 3.30 [Low Δ] `HudPositionScreen` overlay is rendered AFTER the HUD counter — dimming covers the counter

**File:** `HudPositionScreen.java:65, 83`

```java
CpsHudOverlay.drawCounter(drawContext, rc, hudX, hudY);  // Draw counter
// ... draw selection border ...
drawContext.fill(0, 0, width, height, 0x30000000);  // Dim everything
```

The 19% opacity overlay is drawn AFTER the counter, which means the counter itself is dimmed. The "ESC to confirm" text is rendered on top of the overlay. The counter should be rendered AFTER the overlay so it appears at full brightness while the world is dimmed.

**Impact:** The counter the user is trying to position is dimmed, making it harder to see against the world background. This contradicts the purpose of the position editor.

---

## 4. CODE SMELLS & ARCHITECTURAL ISSUES (UI-Related)

### 4.1 [High] God class: `ModConfigScreen` at 611 lines

**File:** `ModConfigScreen.java`

This single class handles: sidebar rendering, card rendering, text field editing with cursors, toggle/slider/color-swatch/add-button rendering, delete with confirmation, scroll management, animation ticking, preset cycling, custom position launching, color picker launching, and all corresponding click/drag/scroll/key/char event handling. No separation of concerns. No widget abstraction.

**Previous report noted 569 lines; the class has since grown to 611.**

**Impact:** Extremely difficult to maintain or extend. Adding a single new widget type requires changes in 4+ methods (render, click, drag, key).

---

### 4.2 [High] Text field editing logic duplicated for 2 field types

**File:** `ModConfigScreen.java:558-604`

The `keyPressed` and `charTyped` handlers contain near-identical code blocks for `focusedFieldIndex == 0` (display format) and `focusedFieldIndex == 1` (name). The only difference is which backing list (`cardFieldTexts` vs `nameFieldTexts`) and which `CounterEntry` field is updated. Any new text field would require duplicating this code a third time.

**Impact:** 6x code repetition (2 fields × 3 handlers). High risk of divergence bugs.

---

### 4.3 [High Δ] `drawBorder` still duplicated between `ColorPickerScreen` and `DrawUtils`

**File:** `ColorPickerScreen.java:172-177`, `DrawUtils.java:31-36`

Despite `DrawUtils.drawBorder` existing as a shared utility, `ColorPickerScreen` has its own private `drawBorder` method with identical logic. The `drawBorder` call inside `ColorPickerScreen` (line 95, 128, 141, 157, 168) uses the local version, not `DrawUtils.drawBorder`.

**Impact:** Any border rendering change must be applied in two places. The `DrawUtils` utility was apparently added after the color picker was written, and the color picker was never updated to use it.

---

### 4.4 [High Δ] Theme constants partially extracted to `DrawUtils` but not consistently used

**File:** `DrawUtils.java:7-27`, `ModConfigScreen.java`, `ColorPickerScreen.java`, `HudPositionScreen.java`

`DrawUtils` defines 18 color constants (ACCENT, PANEL_BG, CARD_BG, etc.), but they are only partially referenced. Audit of actual usage:

| Constant | Defined in DrawUtils | Used in ModConfigScreen | Used in ColorPickerScreen | Used in HudPositionScreen |
|----------|---------------------|------------------------|--------------------------|--------------------------|
| ACCENT | ✅ | ❌ (uses `0xFF4FC3F7` inline) | ❌ (uses `0xFF4FC3F7` inline) | ✅ |
| CARD_BG | ✅ | ❌ (uses `0xFF1E1E1E` inline) | N/A | N/A |
| BORDER | ✅ | ❌ (uses `0xFF444444` inline) | ❌ (uses `0xFF444444` inline) | N/A |
| BTN_BG | ✅ | ❌ (uses `0xFF2A2A2A` inline) | ❌ (uses `0xFF2A2A2A` inline) | N/A |

Only `DrawUtils.ACCENT` is used in `HudPositionScreen.java`. ALL other files continue to hardcode color values inline despite the constants existing.

**Impact:** The `DrawUtils` constants are dead code — the extraction was started but never completed. This is worse than not having them at all, because it creates a false sense of centralization.

---

### 4.5 [Medium Δ] `ModConfigScreen.tick()` implements a 5-second save flush — but `requestSave()` is also called alongside direct mutations

**File:** `ModConfigScreen.java:130-139, 379, 395, 411, 464, 474, 480, 492, 504, 542`

The `requestSave()` + `flushIfDirty()` pattern with a 5-second debounce was added to address the original report's "save on every keystroke" issue. However, the debounce only applies to text field changes (via `requestSave()` in `keyPressed`/`charTyped`). Toggle clicks, slider drags, color changes, and add/delete operations also call `requestSave()`, but `HudPositionScreen.close()` and `mouseReleased` call `config.save()` directly, bypassing the debounce entirely.

**Impact:** Mixed save strategy — some changes are debounced, others are immediate. Inconsistent behavior that's confusing to debug.

---

### 4.6 [Medium Δ] `mouseReleased` in `ModConfigScreen` calls `save()` directly instead of `requestSave()`

**File:** `ModConfigScreen.java:549-554`

```java
if (sliderDragging) {
    sliderDragging = false; sliderCardIndex = -1;
    ModConfig.getConfig().save();  // Direct save, bypasses debounce
}
```

The slider drag release triggers an immediate disk write. This is inconsistent with the `requestSave()` pattern used for text field edits. If the user drags a slider and then immediately types in a text field, the slider's direct `save()` call writes, and the text field's debounced save fires 5 seconds later — potentially overwriting the slider value if the user made more changes.

**Impact:** Race condition between immediate saves and debounced saves. In extreme cases, debounced saves can overwrite recent immediate saves.

---

### 4.7 [Medium Δ] `copyFrom` in `ModConfig` clears and re-adds all counters — breaks reference identity

**File:** `ModConfig.java:76-80`

```java
private void copyFrom(ModConfig other) {
    this.enabled = other.enabled;
    this.counters.clear();
    this.counters.addAll(other.counters);
}
```

When `load()` is called, `copyFrom` replaces the list contents. But `ModConfigScreen` and `HudPositionScreen` hold references to individual `CounterEntry` objects from `config.counters`. After `copyFrom`, the list contains different `CounterEntry` instances (from the loaded config), so any cached references become stale. The `HudPositionScreen.counter` field would point to the OLD `CounterEntry` object, not the one in the updated list.

**Impact:** If `load()` is called while `HudPositionScreen` is open, the screen would modify an orphaned `CounterEntry` that's no longer in the config's counter list. Changes would be silently lost.

---

### 4.8 [Medium Δ] `CpsHudOverlay.resolveCached` uses a `LinkedHashMap` with `removeEldestEntry` — not thread-safe

**File:** `CpsHudOverlay.java:31-36`

```java
private static final Map<String, String> resolveCache = new java.util.LinkedHashMap<>() {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
        return size() > 32;
    }
};
```

The cache is accessed from the render thread (HUD render callback) but could theoretically be accessed from other threads if `resolveCached` is called from a non-render context. `LinkedHashMap` is not thread-safe, and concurrent access during eviction could cause infinite loops (known `LinkedHashMap` bug during concurrent resizing).

**Impact:** Potential for render thread hang if concurrent access occurs. In practice, this is unlikely since all access is currently from the render thread, but it's a latent crash risk.

---

### 4.9 [Low Δ] `DrawUtils.drawCheckerboard` uses fixed 4px cells — doesn't scale with GUI scale

**File:** `DrawUtils.java:38-47`

The checkerboard uses `cell = 4` regardless of the user's GUI scale setting. At 4x GUI scale, each cell is 1 physical pixel — barely visible. At 1x GUI scale, each cell is 4 physical pixels — quite large. Standard transparency indicators scale with the UI.

**Impact:** Checkerboard pattern is too small on high GUI scales and too large on low GUI scales.

---

## 5. INCOMPLETE / HALF-BAKED UI FEATURES

### 5.1 [High Δ] No way to duplicate a counter

There is no "Duplicate" button on any counter card. Users who want a second counter with similar settings must create a new one and manually configure every field. The add button always creates a default counter with `%cps_mouse.left%` only.

**Impact:** Common workflow (creating LMB + RMB counters, or per-key counters) requires tedious manual reconfiguration for each counter.

---

### 5.2 [High Δ] No undo/redo for any operation

There is no undo stack for any action — counter deletion, field edits, position changes, color changes, preset changes are all immediate and irreversible. The `requestSave()` debounce means the config file may not be written for 5 seconds, but there is no way to revert the in-memory state.

**Impact:** A single misclick (especially on the poorly-targeted delete button) can permanently destroy a counter configuration with no recovery path.

---

### 5.3 [Medium Δ] No drag-and-drop to reorder counters

Counters are rendered in list order with no way to reorder them. Users must delete and re-create counters to change their visual stacking order. The sidebar has no drag affordance, and the card list has no drag handles.

**Impact:** Counter ordering is fixed at creation time. No way to organize counters visually.

---

### 5.4 [Medium Δ] No import/export of counter configurations

Users cannot share counter configurations with other players. There is no "Copy to Clipboard" or "Export" button. The only way to share is to manually copy the JSON file from the config directory.

**Impact:** No community sharing of HUD layouts. Reduces the mod's social/competitive appeal.

---

### 5.5 [Medium Δ] No multi-select or batch operations

Users cannot select multiple counters to enable/disable, change colors, or delete in batch. Every operation is one-at-a-time.

**Impact:** Managing 5+ counters requires repeating the same action 5 times.

---

### 5.6 [Medium Δ] Color picker has no hex input field

Users cannot type a specific hex color code. They must use the SB field and sliders to approximate the color. For users who know the exact color they want (e.g., from a team color code), this is extremely frustrating.

**Impact:** Precise color entry is impossible. Users with specific color requirements cannot achieve them through the UI.

---

### 5.7 [Medium Δ] Color picker has no eyedropper / pick-from-screen tool

Users cannot pick a color from the game world (e.g., matching a team's banner color). The color picker only supports manual SB/hue/alpha adjustment.

**Impact:** Color matching requires external tools or guessing.

---

### 5.8 [Low Δ] No preview of the HUD counter in the config screen

**File:** `ModConfigScreen.java`

The config screen renders counter settings in a card layout but does NOT render a live preview of what the HUD counter will look like with the current settings. Users must close the config screen to see the effect of their changes.

**Impact:** No WYSIWYG editing. Every change requires a config → game → config round-trip.

---

## 6. RENDERING CORRECTNESS ISSUES

### 6.1 [High Δ] SB field knob cross-hair is offset by 1px — appears asymmetric

**File:** `ColorPickerScreen.java:130-133`

```java
dc.fill(knobX - 4, knobY - 1, knobX + 4, knobY + 2, 0xFFFFFFFF);  // Horizontal: 8×3
dc.fill(knobX - 1, knobY - 4, knobX + 2, knobY + 4, 0xFFFFFFFF);  // Vertical: 3×8
```

The horizontal bar is 8px wide (knobX-4 to knobX+4) and 3px tall (knobY-1 to knobY+2). The vertical bar is 3px wide (knobX-1 to knobX+2) and 8px tall (knobY-4 to knobY+4). The center of the horizontal bar is at `(knobX, knobY+0.5)` while the center of the vertical bar is at `(knobX+0.5, knobY)`. The cross-hair is visually off-center by 0.5px in both axes, making it look slightly tilted.

**Impact:** Minor visual imprecision that contributes to the unpolished feel.

---

### 6.2 [Medium Δ] Hue bar knob renders the wrong color when positioned at the rightmost pixel

**File:** `ColorPickerScreen.java:145`

```java
dc.fill(knobX - 1, y, knobX + 2, y + BAR_HEIGHT, HUE_COLORS[Math.min(knobX - x, BAR_WIDTH - 1)]);
```

`HUE_COLORS` is indexed by `knobX - x`, which is the pixel offset from the bar's left edge. At the rightmost position, `knobX - x = BAR_WIDTH`, which is clamped to `BAR_WIDTH - 1`. This is correct, but the knob's center is at `knobX`, which should correspond to `hue = 1.0` (pure red, wrapping from violet). `HUE_COLORS[BAR_WIDTH - 1]` is the last pixel, which is slightly before the wrap-around. The knob appears slightly off-color at the right edge.

**Impact:** Minor color inaccuracy at the extreme right of the hue bar. Not noticeable for most users.

---

### 6.3 [Medium Δ] `hsbToRgb` can produce values slightly outside 0-255 range due to floating-point imprecision

**File:** `ColorPickerScreen.java:271-289`

```java
return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (bl * 255);
```

Due to floating-point arithmetic, `r * 255` can produce 255.999... which truncates to 255 (correct), or `bl * 255` can produce 256.0001 (from `1.0000001 * 255`), which truncates to 256. This would set bits in the next channel's position, corrupting the color. The standard fix is `Math.min(255, (int)(channel * 255))` or `(int)(channel * 255 + 0.5)` with clamping.

**Impact:** Rare but possible color corruption when dragging to extreme brightness/saturation values. Would manifest as a slight color shift in the preview.

---

## 7. BUILD & CONFIGURATION (Unchanged from Original Report)

*(See original report.md sections 7.1–7.5 — no new findings.)*

---

## SUMMARY

| Severity | Count | Key Themes |
|----------|-------|------------|
| **Critical** | 2 | Keyboard CPS broken; color picker ~966 GPU draw calls/frame |
| **High** | 14 | Dead code in click handler; stale animation state; scissor bleed; slider knob inaccuracy; no accessibility; button indistinguishability; accordion asymmetry; cursor blink; no cursor-from-click; text overflow; cache key flaw; god class; duplicate drawBorder; inconsistent theme usage |
| **Medium** | 24 | Delete confirm UX; slider drag math; sidebar waste; scroll indicators; no auto-scroll; animation easing; resize handle; no tooltips; preset auto-detect; checkerboard scaling; missing features (duplicate, undo, reorder, import/export, hex input, eyedropper); hsbToRgb precision; rendering order |
| **Low** | 9 | Border derivation; knob sizes; no cursor change; docs underline; Unicode glyphs; HUD dimming; thread safety; checkerboard scale; elideText complexity |
| **Info** | 0 | *(No new Info findings)* |

### Delta from Previous Report

| Category | Previous | New | Change |
|----------|----------|-----|--------|
| Critical | 2 | 2 | Revised color picker draw call count from ~730 to ~966 |
| High | 9 | 14 | +6 new high-severity UI findings |
| Medium | 20 | 24 | +8 new medium-severity UI findings (some previous issues downgraded/upgraded) |
| Low | 5 | 9 | +4 new low-severity findings |

### Top 10 Priority Fixes (UI-Focused)

1. **Fix `KeyboardHandlerMixin` parameter order** — keyboard CPS is completely non-functional; the `onKey` callback maps parameters incorrectly
2. **Fix `resolveCached` cache key** — include keyboard CPS in the cache key, not just mouse; otherwise keyboard counters show stale values
3. **Batch color picker rendering** — pre-render the SB field, hue bar, and alpha bar to `NativeImage`-backed textures on value change, then blit the texture in one draw call; eliminates ~966 draw calls/frame → ~5
4. **Add horizontal text scrolling to custom text fields** — implement a `scrollOffset` that keeps the cursor visible; without this, format strings longer than ~25 chars are uneditable
5. **Set cursor position from click location** — implement `setCursorFromMouseX()` in text field click handler; currently clicking a field leaves the cursor at a random previous position
6. **Add visual distinction between Cancel/Done buttons** — accent the Done button, add click sounds, add press animation
7. **Fix accordion collapse behavior** — make the entire card header clickable for both expand AND collapse; currently only the tiny arrow collapses
8. **Fix card header → expanded body click fallthrough** — clicking the header on an expanded card should collapse it, not fall through to body click handling
9. **Complete the `DrawUtils` theme extraction** — replace ALL hardcoded color literals in `ModConfigScreen`, `ColorPickerScreen`, and `HudPositionScreen` with `DrawUtils` constants; remove the duplicate `drawBorder` from `ColorPickerScreen`
10. **Fix `handleCpsTabClick` dead code** — move the card iteration loop and second add button into an `else` block; the current structure has a loop inside an `if (isEmpty())` check

### Architectural Recommendations

1. **Replace custom text fields with Minecraft's `TextFieldWidget`** — the custom implementation lacks: horizontal scrolling, cursor-from-click, clipboard support, selection, Home/End, Ctrl+A, and accessibility. `TextFieldWidget` provides all of these for free.
2. **Replace manual hit-testing with `ClickableWidget` subclasses** — the entire `mouseClicked`/`handleClick`/`handleExpandedCardClick` chain is fragile, hard to extend, and impossible to make accessible. Minecraft's widget system handles focus, hover, click, and narration automatically.
3. **Implement a proper `HudElement` interface** — extract `CpsHudOverlay` into a generic HUD renderer that accepts `HudElement` implementations. This would enable adding FPS counters, ping displays, kill trackers, etc. without modifying the overlay class.
4. **Separate config screen into composable components** — extract `CounterCardWidget`, `ColorSwatchWidget`, `PresetButton`, `ScaleSlider` as independent widget classes. The 611-line god class is the #1 maintainability blocker.
5. **Implement a proper undo system** — maintain a snapshot stack of `ModConfig` states; support Ctrl+Z / Ctrl+Y; flush only on explicit "Done" action.
