# UI Wireframes — FastIPTV

Visual layout specifications for all screens. TV-first design (landscape, d-pad navigation).

---

## Color Palette

```
Background:       #0A0A0F  (Deep black — OLED/LCD optimized)
Surface:          #14141F  (Card backgrounds)
Surface Elevated: #1E1E2E  (Focused/elevated cards)
Primary:          #4A9EFF  (Electric blue — focus rings, accents)
Primary Variant:  #2D7DD2  (Pressed states)
Secondary:        #FF6B6B  (Live indicator, errors)
Text Primary:     #FFFFFF  (High contrast)
Text Secondary:   #8B8BA3  (Muted labels)
Text Tertiary:    #5C5C73  (Disabled)
Success:          #4ADE80  (Online/playing indicators)
Warning:          #FBBF24  (Buffering)
```

---

## 1. Home Screen

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│  FASTIPTV     📺 Live    🎬 Movies    ⭐ Fav    🔍    ⚙️       │
│  ─────────────────────────────────────────────────────────────── │
│                                                                  │
│  ▶ Continue Watching                                             │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐  │
│  │  ┌───┐  │ │  ┌───┐  │ │  ┌───┐  │ │  ┌───┐  │ │  ┌───┐  │  │
│  │  │ ▶ │  │ │  │ ▶ │  │ │  │ ▶ │  │ │  │ ▶ │  │ │  │   │  │  │
│  │  └───┘  │ │  └───┘  │ │  └───┘  │ │  └───┘  │ │  └───┘  │  │
│  │ CNN HD  │ │ ESPN    │ │ BBC One │ │ Fox News│ │ Sky Spt │  │
│  │ 🔴 LIVE │ │ 🔴 LIVE │ │ 🔴 LIVE │ │ 🔴 LIVE │ │ 🔴 LIVE │  │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘  │
│                                                                  │
│  📺 USA | General                                      See All > │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐  │
│  │ [logo]  │ │ [logo]  │ │ [logo]  │ │ [logo]  │ │ [logo]  │  │
│  │ CNN HD  │ │ MSNBC   │ │ ABC News│ │ CBS News│ │ NBC     │  │
│  │ Newsrm..│ │ Morning │ │ World.. │ │ Evening │ │ Today.. │  │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘  │
│                                                                  │
│  🏈 Sports                                             See All > │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐  │
│  │ [logo]  │ │ [logo]  │ │ [logo]  │ │ [logo]  │ │ [logo]  │  │
│  │ ESPN HD │ │ ESPN 2  │ │ Fox Spt │ │ NFL Net │ │ NBA TV  │  │
│  │ SportC..│ │ First T │ │ FS1 Li..│ │ NFL To..│ │ NBA Co..│  │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Card States

```
┌─────────────┐    ┌═════════════════┐    ┌─────────────┐
│  [logo]     │    ║  [logo]  ✦glow  ║    │  [logo]     │
│  CNN HD     │    ║  CNN HD         ║    │  CNN HD     │
│  Newsroom   │    ║  Newsroom       ║    │  ▶ Playing  │
└─────────────┘    └═════════════════┘    └─────────────┘
   Normal             Focused (d-pad)        Playing
   border: none       border: #4A9EFF 2px    accent bar bottom
   scale: 1.0         scale: 1.05            green dot
```

---

## 2. Channel List Screen

```
┌──────────────────────────────────────────────────────────────────┐
│  ← Back    USA | General (47 channels)              🔍 Search   │
│  ─────────────────────────────────────────────────────────────── │
│                                                                  │
│  ┌──────┐  CNN HD                           Now: CNN Newsroom   │
│  │[logo]│  Channel 1                        8:00 PM - 9:00 PM  │
│  └──────┘  ⭐ Favorite                                    HD   │
│  ─────────────────────────────────────────────────────────────── │
│  ┌──────┐  MSNBC HD                         Now: Morning Joe   │
│  │[logo]│  Channel 2                        6:00 AM - 9:00 AM  │
│  └──────┘                                                  HD   │
│  ─────────────────────────────────────────────────────────────── │
│  ┌──────┐  ▎ ABC News Live                  Now: World News     │ ← Focused
│  │[logo]│  ▎ Channel 3                      6:30 PM - 7:00 PM  │    (left accent)
│  └──────┘  ▎                                               HD   │
│  ─────────────────────────────────────────────────────────────── │
│  ┌──────┐  Fox News HD                      Now: Hannity       │
│  │[logo]│  Channel 4                        9:00 PM - 10:00 PM │
│  └──────┘                                                  HD   │
│  ─────────────────────────────────────────────────────────────── │
│                                                                  │
│  ← ↑↓ Navigate   Enter: Play   ⭐ Toggle Favorite              │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. Player Screen

### 3a. Player — Overlay Visible (auto-hides after 3s)

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│                                                                  │
│                     ╔══════════════════╗                         │
│                     ║                  ║                         │
│                     ║   VIDEO SURFACE  ║                         │
│                     ║    (FULLSCREEN)  ║                         │
│                     ║                  ║                         │
│                     ╚══════════════════╝                         │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  ┌──┐                                                    │   │
│  │  │🔴│  CNN HD                          1920x1080  HD     │   │
│  │  └──┘  CNN Newsroom with Jim Acosta                      │   │
│  │        8:00 PM - 9:00 PM                                 │   │
│  │  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░  75% through        │   │
│  │                                                          │   │
│  │  ◀ CH 41/350 ▶                      ⭐  │  ℹ️  │  ⚙️    │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### 3b. Player — Clean (overlay hidden)

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│                                                                  │
│                                                                  │
│                     ╔══════════════════╗                         │
│                     ║                  ║                         │
│                     ║   VIDEO SURFACE  ║                         │
│                     ║    (FULLSCREEN)  ║                         │
│                     ║                  ║                         │
│                     ╚══════════════════╝                         │
│                                                                  │
│                                                                  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### 3c. Player — Channel Switching Toast

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│   ┌───────────────────┐                                         │
│   │  ▲ CH 42          │   ← Appears briefly on ↑ press          │
│   │  ┌──┐ ESPN HD     │                                         │
│   │  │🔴│ SportsCenter│                                         │
│   │  └──┘             │                                         │
│   │  ▼ CH 43          │                                         │
│   └───────────────────┘                                         │
│                     ╔══════════════════╗                         │
│                     ║   VIDEO SURFACE  ║                         │
│                     ╚══════════════════╝                         │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### 3d. Player — Error Recovery State

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│                                                                  │
│                     ╔══════════════════╗                         │
│                     ║                  ║                         │
│                     ║   ⚠️ Stream      ║                         │
│                     ║   Unavailable    ║                         │
│                     ║                  ║                         │
│                     ║  [🔄 Retry]     ║                         │
│                     ║  [📺 Channels]  ║                         │
│                     ║                  ║                         │
│                     ║  Auto-retry: 28s ║                         │
│                     ╚══════════════════╝                         │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. Search Screen

```
┌──────────────────────────────────────────────────────────────────┐
│  ← Back                                                         │
│  ┌────────────────────────────────────────────────────┐          │
│  │  🔍  cnn                                     ✕    │          │
│  └────────────────────────────────────────────────────┘          │
│  3 results                                                       │
│  ─────────────────────────────────────────────────────────────── │
│                                                                  │
│  📺 LIVE                                                        │
│  ┌──────┐  CNN HD                              USA | General    │
│  │[logo]│  Now: CNN Newsroom                                    │
│  └──────┘                                                       │
│  ┌──────┐  CNN International                   INTERNATIONAL    │
│  │[logo]│  Now: CNN World Sport                                 │
│  └──────┘                                                       │
│                                                                  │
│  🎬 MOVIES                                                      │
│  ┌──────┐  CNN Films: RBG (2018)               Documentary     │
│  │[post]│  ★ 7.6  │  1h 38m                                    │
│  └──────┘                                                       │
│                                                                  │
│  ─────────────────────────────────────────────────────────────── │
│  On-screen keyboard (for TV remote)                              │
│  ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐                     │
│  │ Q │ W │ E │ R │ T │ Y │ U │ I │ O │ P │                     │
│  ├───┼───┼───┼───┼───┼───┼───┼───┼───┼───┤                     │
│  │ A │ S │ D │ F │ G │ H │ J │ K │ L │ ⌫ │                     │
│  └───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘                     │
└──────────────────────────────────────────────────────────────────┘
```

---

## 5. VOD / Movies Screen

```
┌──────────────────────────────────────────────────────────────────┐
│  ← Back    Movies                                    🔍 Search  │
│                                                                  │
│  [All] [Action] [Comedy] [Drama] [Horror] [Sci-Fi] [Thriller]  │
│  ─────────────────────────────────────────────────────────────── │
│                                                                  │
│  ┌─────────┐ ┌─────────┐ ┌═════════┐ ┌─────────┐ ┌─────────┐  │
│  │         │ │         │ ║         ║ │         │ │         │  │
│  │ [poster]│ │ [poster]│ ║ [poster]║ │ [poster]│ │ [poster]│  │
│  │         │ │         │ ║         ║ │         │ │         │  │
│  │─────────│ │─────────│ ║─────────║ │─────────│ │─────────│  │
│  │Inception│ │The Dark │ ║Interst- ║ │Tenet    │ │Dunkirk  │  │
│  │★ 8.8    │ │★ 9.0    │ ║★ 8.7   ║ │★ 7.3    │ │★ 7.8    │  │
│  │2010     │ │2008     │ ║2014    ║ │2020     │ │2017     │  │
│  └─────────┘ └─────────┘ └═════════┘ └─────────┘ └─────────┘  │
│                                                                  │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐  │
│  │ [poster]│ │ [poster]│ │ [poster]│ │ [poster]│ │ [poster]│  │
│  │─────────│ │─────────│ │─────────│ │─────────│ │─────────│  │
│  │Oppenh.. │ │The Bat..│ │Joker    │ │Avatar   │ │Top Gun │  │
│  │★ 8.3    │ │★ 7.8    │ │★ 8.4    │ │★ 7.9    │ │★ 8.3    │  │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 6. Favorites Screen

```
┌──────────────────────────────────────────────────────────────────┐
│  ← Back    ⭐ Favorites (12)                                    │
│  ─────────────────────────────────────────────────────────────── │
│                                                                  │
│  📺 Live Channels (8)                                           │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐  │
│  │ [logo]  │ │ [logo]  │ │ [logo]  │ │ [logo]  │ │ [logo]  │  │
│  │ CNN HD  │ │ ESPN    │ │ BBC One │ │ HBO     │ │ Sky Spt │  │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘  │
│                                                                  │
│  🎬 Movies (4)                                                  │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐              │
│  │ [poster]│ │ [poster]│ │ [poster]│ │ [poster]│              │
│  │Inception│ │The Dark │ │Interst..│ │Tenet    │              │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘              │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## Remote Control Mapping

```
┌─────────────────────────────┐
│         [POWER]             │
│                             │
│  [BACK]    [HOME]   [MENU]  │
│                             │
│           [▲]               │
│     [◀]  [OK]  [▶]         │
│           [▼]               │
│                             │
│  [REW]  [PLAY]  [FFW]      │
│                             │
└─────────────────────────────┘
```

| Button | Global Action | In Player |
|---|---|---|
| ▲ / ▼ | Navigate items | Switch channel (prev/next) |
| ◀ / ▶ | Navigate items | Seek -10s / +10s (VOD only) |
| OK / Enter | Select / Open | Show/hide overlay |
| Back | Go back / Exit | Exit player → channel list |
| Menu | Open side menu | Open settings overlay |
| Play/Pause | — | Toggle playback |
| 0-9 (number) | — | Channel number input |
