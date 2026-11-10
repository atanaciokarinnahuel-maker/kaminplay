# Bundled fonts

All three families are distributed under the SIL Open Font License 1.1 (texts in this folder).

| Files in `app/src/main/res/font` | Family | Source |
|---|---|---|
| `big_shoulders_display_*.ttf` | Big Shoulders Display | github.com/google/fonts, `ofl/bigshouldersdisplay` |
| `instrument_sans_*.ttf` | Instrument Sans | github.com/google/fonts, `ofl/instrumentsans` |
| `dm_mono_*.ttf` | DM Mono | github.com/google/fonts, `ofl/dmmono` |

The Big Shoulders Display and Instrument Sans files are static instances cut from the upstream
variable fonts with `fonttools varLib.instancer` (weights 600/800 and 400/500/600 at normal
width). The DM Mono files are the upstream static files, unmodified.
