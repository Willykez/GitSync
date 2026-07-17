package com.willykez.repomaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Repo Master's Material 3 Expressive palette.
 *
 * This replaces the old flat "cockpit" blue/gold scheme with M3 Expressive's
 * richer three-accent model (primary / secondary / tertiary), tuned for
 * higher chroma and more contrast between tonal steps — the look Expressive
 * is built around. Dynamic color (Material You, wallpaper-derived) is the
 * default on Android 12+; these are the static fallback + the seed colors
 * used to *build* the expressive scheme on older devices.
 */

// ---------------------------------------------------------------------------
// Surfaces — dark is still the default identity, but warmer/more neutral
// than the old blue-black "Void", closer to M3 Expressive's neutral-variant
// surfaces which lean on tonal elevation instead of hairline borders.
// ---------------------------------------------------------------------------
val Void = Color(0xFF14120F)          // app background — warm near-black
val Hull = Color(0xFF1C1A17)          // base surface (scaffold, bars)
val Deck = Color(0xFF262320)          // card surface
val DeckRaised = Color(0xFF322E2A)    // pressed / raised card, sheets, menus
val HullBorder = Color(0x1FFFFFFF)    // hairline — used sparingly now; expressive cards lean on tonal fill, not borders

// Primary accent — "Signal Violet": navigation, links, primary actions
val CommandBlue = Color(0xFFC6A6FF)       // (legacy name) → vivid orchid/violet
val CommandBlueDeep = Color(0xFF7A4FE0)
val CommandBlueDim = Color(0x33C6A6FF)

// Secondary accent — "Flare": still rationed to commit/push (anything that
// touches the remote), just re-hued from gold to a warmer flame-coral.
val SignalGold = Color(0xFFFF8A5B)        // (legacy name) → flame coral
val SignalGoldDeep = Color(0xFFD65A2E)

// Tertiary accent — new in the Expressive palette: a bright emerald used for
// the "everything's clean / synced" state and general success accents.
val Emerald = Color(0xFF4CDA9B)
val EmeraldDeep = Color(0xFF1F9E6C)

// Status
val StatusAdded = Emerald                 // staged / new file / success
val StatusModified = SignalGold           // modified file (shares flare hue family)
val StatusDeleted = Color(0xFFFF5C72)     // deleted / destructive / errors
val StatusMuted = Color(0xFF9C948A)       // neutral / clean / secondary text

// Light theme (secondary — dark is still the default identity for a dev tool)
val Paper = Color(0xFFFFF8F2)
val PaperDim = Color(0xFFF0E7DD)
val Graphite = Color(0xFF1F1B16)

// ---------------------------------------------------------------------------
// Legacy token aliases — every screen inherited from the three source apps
// was written against one of these names. Keeping them as aliases (instead
// of touching 50+ screen files individually) repaints the whole app with
// the new Expressive palette immediately and consistently. New/rewritten
// screens should prefer the tokens above.
// ---------------------------------------------------------------------------
val PlumDeep = Hull
val PlumSoft = Deck
val Amber = SignalGold
val AmberDeep = SignalGoldDeep
val Cream = Color(0xFFFBF3EA)
val CreamDim = DeckRaised
val Ink = Void
val InkSurface = Deck
val StatusClean = StatusMuted
