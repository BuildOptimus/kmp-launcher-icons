# kmp-launcher-icons

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/com.buildoptimus.kmp-launcher-icons?label=Gradle%20Plugin%20Portal)](https://plugins.gradle.org/plugin/com.buildoptimus.kmp-launcher-icons)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

A Gradle plugin that generates native launcher icon resources for **Android**, **iOS**, and **Desktop** from a single source image — using a unified Kotlin DSL.

No more manually resizing icons or juggling platform-specific tooling. Define your icon once, run a task, and get production-ready assets for every platform.

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Project Structure Variants](#project-structure-variants)
    - [Variant A — Per-Platform Entry Points](#variant-a--per-platform-entry-points-androidapp--iosapp--desktopapp)
    - [Variant B — Shared composeApp Entry Point](#variant-b--shared-composeapp-entry-point-androidios-desktop--iosapp-separate)
- [Configuration](#configuration)
    - [Source Image](#source-image)
    - [Android](#android)
    - [iOS](#ios)
    - [Desktop](#desktop)
- [Gradle Tasks](#gradle-tasks)
- [Platform Details](#platform-details)
    - [Android Icons](#android-icons)
    - [iOS Icons](#ios-icons)
    - [Desktop Icons](#desktop-icons)
- [Full Configuration Example](#full-configuration-example)
- [❤️ Support](#support)
- [Author](#author)
- [License](#license)

---

## Features

- ✅ **Android** — raster icons, round icons, and adaptive icons (foreground/background layers)
- ✅ **iOS** — Xcode 15+ modern single-icon format and legacy multi-size format (pre-Xcode 15)
- ✅ **Windows** — multi-resolution `.ico` file (16px–256px)
- ✅ **macOS** — `.iconset` directory with 1x and 2x Retina variants ready for `iconutil`
- ✅ **Linux** — PNGs at configurable sizes
- ✅ Gradle build cache support (`@CacheableTask`)
- ✅ Per-platform source image overrides
- ✅ Validates source images and output directories with clear error messages
- ✅ sRGB color profile enforcement for iOS compatibility

---

## Requirements

- Java 17+
- Gradle 8.x or 9.x
- Source image: a **square PNG** of at least **1024×1024px**

---

## Installation

> **Apply the plugin in your root `build.gradle.kts`** (project level). This is the recommended placement because the plugin generates resources for multiple platform modules and is not tied to any single module's build configuration.

```kotlin
// build.gradle.kts (root / project level)
plugins {
    id("com.buildoptimus.kmp-launcher-icons") version "<latest-version>"
}
```

Or using Groovy `build.gradle` at the project level:

```groovy
// build.gradle (root / project level)
plugins {
    id 'com.buildoptimus.kmp-launcher-icons' version '<latest-version>'
}
```

Check the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.buildoptimus.kmp-launcher-icons) for the latest version.

---

## Project Structure Variants

Before configuring the plugin, identify which project layout your KMP project uses — this determines the `outputDirectory` paths for each platform.

---

### Variant A — Per-Platform Entry Points (`androidApp` / `iosApp` / `desktopApp`)

This layout uses separate Gradle modules for each platform. It is common in projects that follow the KMP wizard template with AGP 9.0+ or that have significant per-platform customisation.

```
my-project/
├── build.gradle.kts              ← apply plugin here
├── composeApp/                   ← shared KMP module
│   └── src/commonMain/composeResources/drawable/icon.png
├── androidApp/
│   └── src/main/res/             ← Android resources
├── iosApp/
│   └── iosApp/Assets.xcassets/AppIcon.appiconset/
└── desktopApp/
    └── src/main/resources/
        ├── windows/
        ├── macos/
        └── linux/
```

```kotlin
// build.gradle.kts (root)
launcherIcons {
    sourceImage = file("composeApp/src/commonMain/composeResources/drawable/icon.png")

    android {
        generateRaster = true
        generateRound = true
        adaptive { generate = true }
        outputDirectory = layout.projectDirectory.dir("androidApp/src/main/res")
    }

    ios {
        generate = true
        outputDirectory = layout.projectDirectory.dir("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset")
    }

    desktop {
        windows {
            generate = true
            outputDirectory = layout.projectDirectory.dir("desktopApp/src/main/resources/windows")
        }
        macOS {
            generate = true
            outputDirectory = layout.projectDirectory.dir("desktopApp/src/main/resources/macos")
        }
        linux {
            generate = true
            outputDirectory = layout.projectDirectory.dir("desktopApp/src/main/resources/linux")
        }
    }
}
```

---

### Variant B — Shared `composeApp` Entry Point (Android/iOS/Desktop + `iosApp` separate)

This layout is common when Android, Desktop, and iOS targets all share a single `composeApp` module (the default from the Kotlin Multiplatform wizard), with `iosApp` as a separate Xcode project entry point.

```
my-project/
├── build.gradle.kts              ← apply plugin here
├── composeApp/
│   └── src/
│       ├── commonMain/composeResources/drawable/icon.png
│       ├── androidMain/res/      ← Android resources live here
│       └── desktopMain/resources/
│           ├── windows/
│           ├── macos/
│           └── linux/
└── iosApp/
    └── iosApp/Assets.xcassets/AppIcon.appiconset/
```

```kotlin
// build.gradle.kts (root)
launcherIcons {
    sourceImage = file("composeApp/src/commonMain/composeResources/drawable/icon.png")

    android {
        generateRaster = true
        generateRound = true
        adaptive { generate = true }
        outputDirectory = layout.projectDirectory.dir("composeApp/src/androidMain/res")
    }

    ios {
        generate = true
        outputDirectory = layout.projectDirectory.dir("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset")
    }

    desktop {
        windows {
            generate = true
            outputDirectory = layout.projectDirectory.dir("composeApp/src/desktopMain/resources/windows")
        }
        macOS {
            generate = true
            outputDirectory = layout.projectDirectory.dir("composeApp/src/desktopMain/resources/macos")
        }
        linux {
            generate = true
            outputDirectory = layout.projectDirectory.dir("composeApp/src/desktopMain/resources/linux")
        }
    }
}
```

> **Tip:** If you're unsure which variant applies to your project, check whether Android and Desktop sources live under `composeApp/src/androidMain` and `composeApp/src/desktopMain` (Variant B) or under their own top-level modules (Variant A).

---

## Configuration

All configuration lives inside the `launcherIcons` block in your root `build.gradle.kts`. You can set a top-level `sourceImage` that all platforms fall back to, or override it per platform.

### Source Image

Your source image must be:
- A valid **PNG** file
- **Square** (width == height)
- At least **1024×1024px** (recommended)

A conventional location for shared assets in KMP projects is inside `composeApp/src/commonMain/composeResources/`:

```kotlin
launcherIcons {
    sourceImage = file("composeApp/src/commonMain/composeResources/drawable/icon.png")
}
```

---

### Android

```kotlin
launcherIcons {
    android {
        // Optional: override the top-level sourceImage for Android only.
        // Remove this line to fall back to launcherIcons.sourceImage.
        // sourceImage = file("assets/icon_android.png")

        // Generate standard density-bucketed launcher icons (mdpi → xxxhdpi + Play Store icon).
        // Produces ic_launcher.png at 48, 72, 96, 144, 192, and 512px.
        // Default: false
        generateRaster = true

        // Generate circular launcher icons (ic_launcher_round) at each density.
        // Android launchers that support round icons will use these instead of the raster icons.
        // Default: false
        generateRound = true

        // Configure adaptive icons — the layered foreground/background format used on Android 8.0+.
        // Adaptive icons let the launcher apply its own shape mask (circle, squircle, etc.).
        adaptive {
            // Must be explicitly set to true to generate adaptive icon assets.
            // Default: false
            generate = true

            // Optional: a separate image used as the foreground layer of the adaptive icon.
            // If not set, the top-level sourceImage is used as the foreground.
            foregroundImage = file("assets/icon_foreground.png")

            // Optional: an image used as the background layer of the adaptive icon.
            // When set, this takes precedence over backgroundColor.
            backgroundImage = file("assets/icon_background.png")

            // Solid color used as the background layer when backgroundImage is not set.
            // Accepts #RGB, #RRGGBB, or #AARRGGBB.
            // Default: "#FFFFFF"
            backgroundColor = "#FFFFFF"

            // Scales the foreground image down to fit within Android's adaptive icon safe zone
            // (66% of the 432px canvas). Content outside this zone may be clipped by the launcher.
            // Set to false only if your source image is already sized for the adaptive canvas.
            // Default: true
            applyPadding = true
        }

        // Output directory where all Android icon resources will be written.
        // Use the path that matches your project layout — see "Project Structure Variants" above.
        // Default: "androidApp/src/main/res"
        outputDirectory = layout.projectDirectory.dir("androidApp/src/main/res")
    }
}
```

#### Android Output Structure

```
<outputDirectory>/
├── mipmap-mdpi/
│   ├── ic_launcher.png           (48×48)
│   └── ic_launcher_round.png     (48×48, circular)
├── mipmap-hdpi/
│   ├── ic_launcher.png           (72×72)
│   └── ic_launcher_round.png
├── mipmap-xhdpi/
│   ├── ic_launcher.png           (96×96)
│   └── ic_launcher_round.png
├── mipmap-xxhdpi/
│   ├── ic_launcher.png           (144×144)
│   └── ic_launcher_round.png
├── mipmap-xxxhdpi/
│   ├── ic_launcher.png           (192×192)
│   ├── ic_launcher_round.png
│   ├── ic_launcher_foreground.png  (432×432, adaptive)
│   └── ic_launcher_background.png  (432×432, adaptive — if image background)
├── mipmap-anydpi-v26/
│   ├── ic_launcher.xml           (adaptive icon descriptor)
│   └── ic_launcher_round.xml
├── values/
│   └── ic_launcher_background.xml  (color resource — if color background)
└── ic_launcher-playstore.png     (512×512, Google Play Store)
```

---

### iOS

```kotlin
launcherIcons {
    ios {
        // Must be explicitly set to true to generate iOS icon assets.
        // Default: false
        generate = true

        // Optional: override the top-level sourceImage for iOS only.
        // Remove this line to fall back to launcherIcons.sourceImage.
        // sourceImage = file("assets/icon_ios.png")

        // Controls which Xcode asset catalog format is generated:
        //   MODERN → Xcode 15+ single 1024×1024 universal icon (recommended for new projects)
        //   LEGACY → pre-Xcode 15 full size matrix (iPhone, iPad, App Store — 13 sizes)
        // Default: XcodeVersion.MODERN
        xcodeVersion = XcodeVersion.MODERN

        // Background color used to flatten any transparency in the App Store icon (1024×1024).
        // Apple requires the App Store icon to be fully opaque. Device icons are not affected.
        // Accepts #RGB, #RRGGBB, or #AARRGGBB.
        // Default: "#FFFFFF"
        appStoreBackgroundColor = "#FFFFFF"

        // Output directory for the AppIcon.appiconset contents (PNG files + Contents.json).
        // Use the path that matches your project layout — see "Project Structure Variants" above.
        // Default: "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
        outputDirectory = layout.projectDirectory.dir("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset")
    }
}
```

#### iOS Output Structure (Modern)

```
<outputDirectory>/
├── AppIcon.png        (1024×1024, sRGB, opaque)
└── Contents.json
```

#### iOS Output Structure (Legacy)

```
<outputDirectory>/
├── AppIcon-20x20.png
├── AppIcon-29x29.png
├── AppIcon-40x40.png
├── AppIcon-58x58.png
├── AppIcon-60x60.png
├── AppIcon-76x76.png
├── AppIcon-80x80.png
├── AppIcon-87x87.png
├── AppIcon-120x120.png
├── AppIcon-152x152.png
├── AppIcon-167x167.png
├── AppIcon-180x180.png
├── AppIcon-1024x1024.png
└── Contents.json
```

> **Note:** All iOS icons are converted to the sRGB color space. The App Store icon (1024×1024) has transparency flattened to the configured `appStoreBackgroundColor`. Device icons retain transparency.

---

### Desktop

```kotlin
launcherIcons {
    desktop {
        // Optional: override the top-level sourceImage for all desktop platforms.
        // Remove this line to fall back to launcherIcons.sourceImage.
        // sourceImage = file("assets/icon_desktop.png")

        windows {
            // Must be explicitly set to true to generate the Windows icon.
            // Default: false
            generate = true

            // Output directory for icon.ico (a multi-resolution file containing
            // 16, 32, 48, 64, 128, and 256px variants in a single file).
            // Use the path that matches your project layout — see "Project Structure Variants" above.
            // Default: "desktopApp/src/main/resources/windows"
            outputDirectory = layout.projectDirectory.dir("desktopApp/src/main/resources/windows")
        }

        macOS {
            // Must be explicitly set to true to generate the macOS icon assets.
            // Default: false
            generate = true

            // Output directory for the icon.iconset/ directory, which contains
            // 1x and @2x Retina PNG variants at 16, 32, 128, 256, and 512px.
            // To compile into a standalone .icns, run on macOS: iconutil -c icns icon.iconset
            // Use the path that matches your project layout — see "Project Structure Variants" above.
            // Default: "desktopApp/src/main/resources/macos"
            outputDirectory = layout.projectDirectory.dir("desktopApp/src/main/resources/macos")
        }

        linux {
            // Must be explicitly set to true to generate Linux icon PNGs.
            // Default: false
            generate = true

            // Pixel sizes to generate. Each size produces a separate icon_NxN.png file.
            // Default: [16, 32, 48, 64, 128, 256]
            sizes = listOf(16, 32, 48, 64, 128, 256)

            // Output directory for the generated PNG files.
            // Use the path that matches your project layout — see "Project Structure Variants" above.
            // Default: "desktopApp/src/main/resources/linux"
            outputDirectory = layout.projectDirectory.dir("desktopApp/src/main/resources/linux")
        }
    }
}
```

#### Desktop Output Structure

```
<outputDirectory>/
├── windows/
│   └── icon.ico                  (multi-resolution: 16, 32, 48, 64, 128, 256px)
├── macos/
│   └── icon.iconset/
│       ├── icon_16x16.png
│       ├── icon_16x16@2x.png
│       ├── icon_32x32.png
│       ├── icon_32x32@2x.png
│       ├── icon_128x128.png
│       ├── icon_128x128@2x.png
│       ├── icon_256x256.png
│       ├── icon_256x256@2x.png
│       ├── icon_512x512.png
│       └── icon_512x512@2x.png
└── linux/
    ├── icon_16x16.png
    ├── icon_32x32.png
    ├── icon_48x48.png
    ├── icon_64x64.png
    ├── icon_128x128.png
    └── icon_256x256.png
```

> **macOS note:** The plugin generates an `icon.iconset/` directory. To compile it into a standalone `.icns` binary, run this on a macOS machine:
> ```sh
> iconutil -c icns icon.iconset
> ```
> `iconutil` is only available on macOS, so the compilation step is left to you.

---

## Gradle Tasks

| Task | Description |
|------|-------------|
| `generateAndroidIcons` | Generates Android launcher icons (raster, round, adaptive) |
| `generateIosIcons` | Generates iOS launcher icons and `Contents.json` |
| `generateDesktopIcons` | Generates Windows ICO, macOS iconset, and Linux PNGs |
| `generateAllIcons` | Runs all three tasks above |

Run a task from the command line:

```sh
./gradlew generateAllIcons
./gradlew generateAndroidIcons
./gradlew generateIosIcons
./gradlew generateDesktopIcons
```

All tasks are `@CacheableTask`-annotated, so Gradle's build cache will skip them when inputs haven't changed.

---

## Platform Details

### Android Icons

| Type | Files Generated | Notes |
|------|----------------|-------|
| Raster | `ic_launcher.png` in each mipmap density | mdpi (48px) → xxxhdpi (192px) + Play Store (512px) |
| Round | `ic_launcher_round.png` in each mipmap density | Circular crop applied |
| Adaptive | `ic_launcher_foreground.png`, background layer, XML descriptors | Canvas 432×432px (xxxhdpi), safe zone 66% |

### iOS Icons

| Mode | Xcode Compatibility | Images Generated |
|------|--------------------|--------------------|
| `MODERN` | Xcode 15+ | Single 1024×1024 universal PNG |
| `LEGACY` | Pre-Xcode 15 | 13 sizes covering iPhone, iPad, and App Store |

### Desktop Icons

| Platform | Format | Sizes |
|----------|--------|-------|
| Windows | `.ico` (multi-resolution) | 16, 32, 48, 64, 128, 256px |
| macOS | `.iconset` directory (1x + @2x Retina) | 16, 32, 128, 256, 512px |
| Linux | Individual PNGs | Configurable (default: 16, 32, 48, 64, 128, 256px) |

---

## Full Configuration Example

```kotlin
// build.gradle.kts (root / project level)
launcherIcons {
    // Shared source image — all platforms fall back to this unless overridden
    sourceImage = file("composeApp/src/commonMain/composeResources/drawable/icon.png")

    android {
        generateRaster = true    // default: false
        generateRound = true     // default: false

        adaptive {
            generate = true                   // default: false
            foregroundImage = file("composeApp/src/commonMain/composeResources/drawable/icon_foreground.png")
            backgroundColor = "#1A1A2E"       // default: "#FFFFFF"
            applyPadding = true               // default: true
        }

        // Adjust to match your project layout (see "Project Structure Variants")
        outputDirectory = layout.projectDirectory.dir("androidApp/src/main/res")
    }

    ios {
        generate = true                          // default: false
        xcodeVersion = XcodeVersion.MODERN       // default: XcodeVersion.MODERN
        appStoreBackgroundColor = "#1A1A2E"      // default: "#FFFFFF"
        outputDirectory = layout.projectDirectory.dir("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset")
    }

    desktop {
        windows {
            generate = true    // default: false
            outputDirectory = layout.projectDirectory.dir("desktopApp/src/main/resources/windows")
        }

        macOS {
            generate = true    // default: false
            outputDirectory = layout.projectDirectory.dir("desktopApp/src/main/resources/macos")
        }

        linux {
            generate = true                                   // default: false
            sizes = listOf(16, 32, 48, 64, 128, 256, 512)    // default: [16, 32, 48, 64, 128, 256]
            outputDirectory = layout.projectDirectory.dir("desktopApp/src/main/resources/linux")
        }
    }
}
```

---

## Support

If this plugin saves you time, consider supporting my work ❤️

👉 [Support](https://selar.com/showlove/fegzdev?currency=USD)

For bugs and feature requests, please [open an issue](https://github.com/fegzdev/kmp-launcher-icons/issues).

---

## Author

**Oghenefega Oghenovo**

- 🌐 Portfolio: [portfolly.io/fegzdev](https://portfolly.io/fegzdev)
- 💼 LinkedIn: [linkedin.com/in/oghenefega-oghenovo-b51bbb349](https://www.linkedin.com/in/oghenefega-oghenovo-b51bbb349)
- 📧 Email: [fegzdev@gmail.com](mailto:fegzdev@gmail.com)

---

## License

```
Copyright 2025 BuildOptimus Labs Limited RC 8752095

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```