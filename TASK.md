# TASK — Vellum (Android office studio)

GOAL: Build and ship "Vellum" — an offline Android office app whose core is
Scan → PDF → real PowerPoint (.pptx), plus PDF tools, OCR and a document
library. Polished, smooth, zero ads, zero login.

Plan / PRD: `PLAN.md` (to be written after design gate)
Repo: github.com/raisoftlabs/vellum (public, Actions → signed release APK)
Local dir: ~/vellum

## Study of existing office apps (done 2026-11-21)
- WPS Office: most complete, but ads + PDF tools paywalled + heavy.
- Microsoft 365: best Office fidelity, needs account/plan for editing.
- Google Docs/Sheets/Slides: 3 separate apps, cloud-first, weak PDF tools.
- ONLYOFFICE: good OOXML editing, scanner is basic, no deck generation.
- OfficeSuite / MobiOffice / Polaris: freemium, ads, PDF export/convert gated.
- Adobe Scan / CamScanner / Microsoft Lens: great scanning, but conversion
  to editable Office files is locked/cloud and no real slide builder.
GAP → nobody ships a free, offline, ad-free pipeline that turns a scanned
document or any PDF into a real editable .pptx. That is Vellum's core.

## Build steps
- [x] Recon: toolchain, GitHub creds, library versions, PPTX format prototype
- [x] PPTX writer prototype validated with python-pptx (4 slides: cover,
      bullets, full-bleed image, captioned image)
- [x] Design gate: 3 UI directions → user picks (ui-preview.html on :8484)
- [x] Scaffold Gradle project (Kotlin + Compose M3, AGP 8.5.2, K 2.0.20)
- [x] Core: PptxBuilder.kt (port of deckgen.py, template-in-assets model)
- [x] Core: PdfEngine.kt (pdfbox-android: merge/split/compress/images→pdf/
      text extract with font-size headings/DOCX text)
- [x] Core: PdfRender.kt (PdfRenderer → bitmaps for viewer + image slides)
- [x] Core: OcrEngine.kt (ML Kit text-recognition 16.0.1)
- [ ] Core: ScanLauncher (ML Kit document-scanner 16.0.0 + fallback picker)
- [x] UI: Home / Scan / Deck studio / Tools / Viewer / Library / About
- [x] Icon + dark mode + empty/loading/error states
- [x] Unit test: PPTX zip + XML validity (runs in CI)
- [x] Signed release build: keystore generated with python cryptography
- [x] Push, watch BOTH green, deliver APK link + install steps

## Decisions
- Kotlin + Jetpack Compose Material3 (not Java/XML): smooth 60fps lists,
  real dark mode, modern feel.
- No cloud, no accounts, no ads: everything runs on-device (ML Kit OCR is
  bundled; document scanner uses Play Services models).
- PPTX generated with a hand-rolled OOXML writer seeded from a python-pptx
  template (master/layout/theme guaranteed valid), slides composed in Kotlin.
- Release keystore committed (PKCS12) so every future build updates in place.

## NOW (2026-11-21)
Sources complete: core (PptxBuilder, PdfEngine, PdfRender, Ocr, DeckMaker,
Store) + UI (Home, Deck studio, Tools, Viewer, Result, Library, Text, About)
+ App.kt navigation + MainActivity + PPTX unit test + GitHub Actions workflow.

DONE: repo pushed, CI green (unit tests + signed APK), release v1.0.3 with
Vellum-1.0.apk (60.5 MB).

NEXT: user installs the APK and tests: scan -> PDF -> deck, SMART mode on a
text PDF, merge/split, OCR. Then fix whatever the device test exposes.
