# Store graphics and screenshots

The listing images live under `fastlane/metadata/android/de-DE/images/`, the
layout both F-Droid and Fastlane's Play upload understand:

| File | Size | Content |
| --- | --- | --- |
| `icon.png` | 512 × 512 | The launcher mark on its `#F4F5F7` background |
| `featureGraphic.png` | 1024 × 500 | Mark, wordmark and the short description on the „Demnächst“ sky |
| `phoneScreenshots/1-8.png` | 1080 × 2160 | Anne, Demnächst, Zusammen, Bernd, Caro, Mia, Einstellungen, Zusammen (dark) |
| `sevenInchScreenshots/1-3.png` | 1200 × 1920 | Anne, Demnächst, Zusammen |
| `tenInchScreenshots/1-3.png` | 2560 × 1600 | Anne, Demnächst, Zusammen |

Google Play accepts screenshots whose longer side is at most twice the
shorter one, which is why the phone set is 2:1 rather than the emulator's
native 1080 × 2400.

## Provenance

- `icon.png` and the mark in the feature graphic are rendered from
  `app/src/main/res/drawable/ic_launcher_foreground.xml` (the same paths,
  scaled to 72 % as in the adaptive icon) with ImageMagick; provenance of
  those paths is in `docs/assets.md`.
- The feature graphic's text is set in Inter (SIL Open Font License), from
  the system font directory; the font is not redistributed.
- Screenshots come from the `Erdentage_API_36` emulator (Android 16) with the
  debug build, the seeded family below, system-UI demo mode (clock 9:41,
  full battery, Wi-Fi only) and `wm size`/`wm density` set per form factor.

## Reproducing the screenshots

`docs/screenshot-family.json` is the stored state of the four-person family
Anne, Bernd, Caro (with a birth time) and Mia. Their birth dates are `heute −
N Tage` with N chosen so each person's own page shows a „Heute:“ line (Anne
16.796 Catalan-Zahl, Bernd 11.111 Palindrom · Ziffernfolge, Caro 10.301
Primzahl · Palindrom, Mia 2.048 Zweierpotenz); recompute them for the day of a
new shoot, and Mia's next prime then lands on Anne's birthday, which shows
the Geburtstags-Zufall in the „Demnächst“ clouds.

```sh
adb shell am force-stop ch.firsching.erdentage
adb push docs/screenshot-family.json /data/local/tmp/seed.json
adb shell run-as ch.firsching.erdentage mkdir -p files/datastore
adb shell run-as ch.firsching.erdentage sh -c \
  'cat /data/local/tmp/seed.json > files/datastore/erdentage-state.json'
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile hide
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e fully true
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
adb shell wm size 1080x2160            # 2560x1600 + density 320 for 10", 1200x1920 for 7"
adb shell am start -n ch.firsching.erdentage/de.eigenraum.erdentage.MainActivity
adb exec-out screencap -p -d <display id> > 1.png   # id from: dumpsys SurfaceFlinger --display-id
adb shell wm size reset; adb shell wm density reset
adb shell am broadcast -a com.android.systemui.demo -e command exit
```

`run-as` works only with the debuggable build. The app restores the last
page, so reseed (which resets the page index) before each form factor.
