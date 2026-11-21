# Vellum — offline office studio (Android)

Android app that turns scans and PDFs into real, editable PowerPoint decks,
plus PDF tools and OCR. No account, no cloud, no ads.

Repo: github.com/raisoftlabs/vellum (public) · Package: com.raisoftlabs.vellum
Local: ~/vellum · Branch: main (push `master:main`)

## Build & verify
No local JDK/Gradle — the phone never builds. Verification is CI:
- Workflow: `.github/workflows/android.yml` (Gradle 8.9 supplied by
  gradle/actions/setup-gradle, JDK 17, AGP 8.5.2, Kotlin 2.0.20)
- Runs `gradle :app:testReleaseUnitTest` then `:app:assembleRelease`
- Publishes a GitHub Release per build with `Vellum-1.0.apk` (direct link)
- Push branch `master` to remote `main`; watch runs via the API
  (`/repos/raisoftlabs/vellum/actions/runs`) — CI is the compile step.
- No gradle wrapper in the repo on purpose (setup-gradle installs Gradle).

## Architecture
- `core/PptxBuilder.kt` — hand-written OOXML writer. `build(template, slides,
  palette, wEmu, hEmu, out)`. Seeded from `assets/deck_template.pptx`
  (python-pptx made, no slides: master/layouts/theme only). Slides:
  `Cover`, `Text(bullets)`, `Page(image bytes, ext, caption)`.
  Pure JVM → unit tested in `app/src/test/.../PptxBuilderTest.kt`.
- `core/PdfEngine.kt` — pdfbox-android. NOTE: package is
  `com.tom_roush.pdfbox.*` (not org.apache.pdfbox). Text extraction keeps
  per-line max font size for heading detection.
- `core/PdfRender.kt` — android.graphics.pdf.PdfRenderer wrapper (Closeable).
- `core/Ocr.kt` — ML Kit text-recognition: `recognize(bitmap)`,
  `fileText(file)` (PDF ≤30 pages or one image).
- `core/DeckMaker.kt` — EXACT (page images) / SMART (retypeset text, font-size
  headings, OCR fallback, `Result?` null when no text). Max 80 slides.
- `core/Store.kt` — SAF import into `getExternalFilesDir/Vellum`, list,
  share, openWith (FileProvider `${applicationId}.fileprovider`).
- `ui/App.kt` — all navigation: `Route` sealed interface (Result/Deck/Tool/
  Viewer/Text), bottom tabs Home/Studio/Files/About, ML Kit document-scanner
  launcher (IntentSender), busy overlay.
- `ui/` screens: Home, DeckScreen (studio), ToolScreens (one screen for all
  ToolId tools), ViewerScreen (paged render + zoom), ResultScreen, Library,
  TextScreen (extracted text export), About. Theme = "Slate Pro" (indigo).

## Decisions (dated)
- 2026-11-21: Compose M3 over XML views; minSdk 26, target 34.
- 2026-11-21: PPTX by template-seeded hand-written OOXML, not a library —
  guarantees editable output and no huge dependency.
- 2026-11-21: Keystore committed (`keystore/vellum.p12`, PKCS12,
  alias `vellum`, pass `vellum-mobile-2026`) so every build updates in place.
- 2026-11-21: Scanner = Play Services ML Kit document scanner
  (`play-services-mlkit-document-scanner:16.0.0`), PDF result format.

## State
v1.0.3 released green (tests + APK). Awaiting first on-device install test:
scan → deck → open in PowerPoint; verify SMART mode on a text PDF.
