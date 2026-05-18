# 🚴 Pedal Rush

A smooth, minimalist endless cycling game for Android. Dodge obstacles, collect coins, and beat your high score!

---

## Game Features

- **Endless side-scrolling** gameplay with procedurally spawning obstacles and coins
- **4 obstacle types**: Road cones, rocks, wooden boxes, and potholes
- **Gold spinning coins** at varying heights — collect them for bonus score
- **Progressive difficulty** — speed gradually increases as you survive longer
- **Parallax background** — sky, clouds, hills, trees, and road scroll at different speeds
- **Animated cyclist** — rotating wheels, pedalling legs, and lean-on-jump animation
- **3-count countdown** before each run starts
- **Score + Best Score** saved locally with SharedPreferences
- **Sound effects** — jump, coin, crash (via ToneGenerator, no audio files required)
- **Sound on/off toggle** persisted across sessions
- **Game Over screen** with NEW BEST! celebration

---

## Controls

| Action | Input |
|--------|-------|
| **Jump** | Tap the screen |
| **Floaty / Higher jump** | Hold the screen while jumping |

That's it — one-touch arcade simplicity!

---

## Screens

1. **Splash** — Logo with fade-in animation (2 seconds)
2. **Main Menu** — Play, Sound toggle, Exit buttons + Best Score display
3. **Game** — Full gameplay, HUD with score/coins
4. **Game Over** — Final score, best score, coins, restart/menu buttons

---

## Building in Android Studio

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 8 or higher
- Android SDK with API 34 installed
- Gradle 8.2

### Steps

1. **Clone / Extract** this project folder
2. **Open** Android Studio → `File > Open` → select the `PedalRush` folder
3. **Wait** for Gradle sync to complete (first sync may download dependencies)
4. **Run** on a device or emulator:
   - Click the ▶ Run button, or
   - `Run > Run 'app'`

### Build Debug APK

```bash
# From project root
./gradlew assembleDebug
```

The APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK

```bash
./gradlew assembleRelease
```

> Note: For a signed release APK, configure a keystore in `app/build.gradle` under `signingConfigs`.

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java |
| Rendering | Android Canvas + SurfaceView |
| Game Loop | Custom Thread (60 FPS target) |
| Audio | ToneGenerator (no asset files) |
| Storage | SharedPreferences |
| Min SDK | 21 (Android 5.0) |
| Target SDK | 34 (Android 14) |

---

## Project Structure

```
PedalRush/
├── app/
│   ├── src/main/
│   │   ├── java/com/pedalrush/game/
│   │   │   ├── SplashActivity.java       # Entry screen
│   │   │   ├── MainMenuActivity.java     # Main menu
│   │   │   ├── GameActivity.java         # Hosts GameView
│   │   │   ├── GameOverActivity.java     # Score screen
│   │   │   ├── GameView.java             # Core game surface + state
│   │   │   ├── GameLoop.java             # 60fps game thread
│   │   │   ├── Player.java               # Cyclist with physics
│   │   │   ├── Background.java           # Parallax layers
│   │   │   ├── Obstacle.java             # Cone/Rock/Box/Pothole
│   │   │   ├── Coin.java                 # Spinning collectible
│   │   │   ├── SoundManager.java         # Audio (singleton)
│   │   │   └── Utils.java                # SharedPrefs + helpers
│   │   ├── res/
│   │   │   ├── drawable/                 # Buttons, icons, gradients
│   │   │   ├── layout/                   # XML screen layouts
│   │   │   ├── mipmap-*/                 # Launcher icons
│   │   │   └── values/                   # strings, colors, themes
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
├── gradle.properties
└── README.md
```

---

## Performance Notes

- All `Paint` objects are allocated **once** (not inside the draw/update loop)
- `ArrayList` entity lists use `Iterator` for safe in-loop removal
- No bitmap loading — everything is drawn with Canvas primitives
- Game thread properly joined on `pause()` to avoid memory leaks
- `FLAG_KEEP_SCREEN_ON` keeps display active during gameplay

---

## License

Free to use, modify, and distribute. Have fun! 🚴💨
