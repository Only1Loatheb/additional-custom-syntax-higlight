# Additional Custom Syntax Highlight

![Build](https://github.com/Only1Loatheb/additional-custom-syntax-higlight/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

An IntelliJ Platform plugin that allows you to configure custom regular expression rules to highlight words and patterns on top of any language's syntax highlighting.

## Features

- **Regex-based Word Search**: Define any regular expression to match identifiers, keywords, numbers, or custom tokens.
- **Works On Top of Any Language**: Highlight layers directly on top of base language syntax highlighting across all file types (`.java`, `.kt`, `.py`, `.md`, `.txt`, `.xml`, etc.).
- **Rich Text Properties**:
  - Text color (foreground color) by default.
  - Optional background color highlight.
  - Font styling: **Bold**, *Italic*.
  - Text effects: Boxed border, Rounded box, Underline, Wave underline, and Strikeout with custom effect colors.
  - Error stripe gutter indicator.
- **File Types Targeting**: Default is all file types (`*`), or restrict rules to specific extensions (e.g. `java, kt, py, md` or `*.log`).
- **Interactive Settings Page**: Configure rules in **Settings > Editor > Custom Regex Highlighting** with a real-time live preview panel.
- **Quick Context Menu Action**: Select any word in an editor, right-click, and select **Add Regex Highlight...** to create a rule instantly.

## Installation

### From Marketplace
Once published, install via:
**Settings / Preferences** > **Plugins** > **Marketplace** > Search for **"Additional Custom Syntax Highlight"** > **Install**.

### From Disk (Local Build)
1. Build the distribution zip:
   ```bash
   ./gradlew buildPlugin
   ```
2. In IntelliJ IDEA, go to **Settings / Preferences** > **Plugins** > **⚙️** > **Install Plugin from Disk...**.
3. Select `build/distributions/additional-custom-syntax-higlight-0.0.1.zip`.
4. Restart your IDE.

---
Plugin based on the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).
