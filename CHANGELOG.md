<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# additional-custom-syntax-higlight Changelog

## [Unreleased]

## [0.0.1]
### Added
- Configurable regex-based word search highlighting across all open editors.
- Layered on top of any language syntax highlighting (`HighlighterLayer.ADDITIONAL_SYNTAX + 100`).
- Text color (foreground) configuration with full support for background color, bold, italic, and text decorations (boxed border, underline, strikeout).
- File types / language targeting (`*` for all files by default, or comma-separated extensions like `java, kt, py, md`).
- Settings page under **Settings > Editor > Custom Regex Highlighting** with rule management and real-time live preview.
- Context menu action **"Add Regex Highlight..."** to quickly create a rule from any selected word.
