# RS Office — offline office studio (Android)

Android app that turns scans and PDFs into real, editable PowerPoint decks,
plus PDF tools and OCR. No account, no cloud, no ads.

Repo: github.com/raisoftlabs/rs-office (public) · Package: com.raisoftlabs.rsoffice
Local: ~/rsoffice · Branch: master (push `master:main`)

## Build & verify
No local JDK/Gradle — the phone never builds. Verification is CI:
- Workflow: `.github/workflows/android.yml` (Gradle 8.9 supplied by
  gradle/actions/setup-gradle, JDK 17, AGP 8.5.2, Kotlin 2.0.20)
- Runs `gradle :app:testReleaseUnitTest` then `:app:assembleRelease`
- Publishes a GitHub Release per build with `RSOffice-2.1.apk` (direct link)
- Push branch `master` to remote `main`; watch runs via the API
  (`/repos/raisoftlabs/rs-office/actions/runs`) — CI is the compile step.
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
- `core/Store.kt` — SAF import into `getExternalFilesDir/RS Office`, list,
  share, openWith (FileProvider `${applicationId}.fileprovider`).
- `ui/App.kt` — all navigation: `Route` sealed interface (Result/ScanReview/
  Deck/Tool/Viewer/Office/Text), route back-stack, bottom tabs Home/Tools/
  Files/About, ML Kit document-scanner launcher (IntentSender), busy overlay.
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

## Office readers (v1.2)
- `core/DocxReader.kt` — paragraphs + heading styles from word/document.xml
- `core/XlsxEngine.kt` — sharedStrings + cell refs → rows; `toPdf()` draws a
  paginated A4-landscape table with wrapped cells
- `core/PptxReader.kt` — slide text in order from ppt/slides/slideN.xml
- `ui/OfficeScreen.kt` — one viewer for all three (paper list / table with
  horizontal scroll + PDF export / slide cards)

## Design system (v2.1 — light glossy / fluid)
- `ui/Theme.kt` — white-light palette (#FAFBFF bg), original electric blue /
  violet / mint / amber hues; document-type colors are our own, not MS brand
  colors; light theme is the default
- `ui/Design.kt` — AppBackground (2 drifting radial washes), GlassCard
  (vertical gloss + hairline), GradientIcon, SectionHeader, GlossTag,
  `Modifier.bounceClick` (spring squish on press)
- `ui/App.kt` — AnimatedContent route slide+fade, Crossfade tabs, animated
  bottom pill; tabs Home / Tools / Files / About
- `ui/HomeScreen.kt` — hero, quick actions, Office apps 2x2, PDF toolkit
  grid, recent files with page thumbnails
- `ui/ToolsScreen.kt` — every tool grouped Create / Organize / Convert / Read
- `ui/ScanReviewScreen.kt` — post-scan page review: rotate / reorder / remove
  pages, rebuilds the PDF at high resolution via `PdfRender` + `imagesToPdf`

## Decisions
- 2026-11-23: user rejected the custom look — UI now follows mainstream
  office-app conventions (WPS/Office style grids, brand-colored app tiles).
- 2026-11-23: "all office tools" means real Word/Excel/PowerPoint readers in
  the app, not only a PDF→PPT converter. Viewers are read-only; Excel exports PDF.

## Roadmap: READ → WRITE (v2 direction)
- 2026-11-23 directive: readers are not enough — RS Office must be an MS Office
  competitor, i.e. real editing of .docx / .xlsx / .pptx offline.
- No usable free offline OOXML edit engine exists for Android (POI/docx4j are
  broken or huge; ONLYOFFICE/Collabora SDKs are licensed + server-backed).
  Decision: extend our own hand-written OOXML layer — read-modify-write the
  package, preserving every part we do not touch (styles, theme, media,
  relationships) so Office opens the result with zero repair prompts.
- Plan per module: model (parse) → editor UI → writer (round-trip) → unit test
  on the produced zip/XML (existing PptxBuilderTest pattern).
- Editing surface (phase 1): text entry, bold/italic/underline/strike, font
  size, color, highlight, alignment, lists, undo/redo, save-as-copy.
  Phase 2: tables, images, headers/footers, formulas, charts, slide layouts.

## Decisions (2026-11-23, rename + quality directive)
- User rejected a name-only change: RS Office must be a full offline MS Office
  competitor with professional, polished UX — not a rebranded PDF tool.
- Package renamed to `com.raisoftlabs.rsoffice`; GitHub repo renamed to
  `raisoftlabs/rs-office`. Keystore file/alias intentionally unchanged so the
  signing identity stays stable.
- Icons: only Google Material Icons (Apache 2.0) and original vector marks.
  No Microsoft logos or third-party copyrighted artwork.
- Target look: white/light, glossy/fluid, mainstream office-app conventions but
  visually distinct; fluid gradients, soft depth, no skeuomorphic clutter.
- Next priorities: fix v2 CI, light glossy reskin + About, accurate scan page
  review, then broader Office/PDF creation and editing tools.

## State
v2.1 shipped to `main`: package/repo renamed, v2 editors compile, light glossy
fluid reskin done, route back-stack added, credits in About, and post-scan
page review added. CI is the only compile step. Next: verify page review on a
device, finish UI polish on secondary screens, then expand Office/PDF tools.
