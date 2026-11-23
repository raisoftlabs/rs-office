# TASK — RS Office (Android office studio)

GOAL: Build and ship "RS Office" — a professional offline Android office suite
for Word, Excel, PowerPoint and PDF, with scanning, OCR, creation tools and a
document library. Polished, smooth, zero ads, zero login.

Plan / PRD: `PLAN.md` (to be written after design gate)
Repo: github.com/raisoftlabs/rs-office (public, Actions → signed release APK)
Local dir: ~/rsoffice

## Study of existing office apps (done 2026-11-21)
- WPS Office: most complete, but ads + PDF tools paywalled + heavy.
- Microsoft 365: best Office fidelity, needs account/plan for editing.
- Google Docs/Sheets/Slides: 3 separate apps, cloud-first, weak PDF tools.
- ONLYOFFICE: good OOXML editing, scanner is basic, no deck generation.
- OfficeSuite / MobiOffice / Polaris: freemium, ads, PDF export/convert gated.
- Adobe Scan / CamScanner / Microsoft Lens: great scanning, but conversion
  to editable Office files is locked/cloud and no real slide builder.
GAP → nobody ships a free, offline, ad-free pipeline that turns a scanned
document or any PDF into a real editable .pptx. That is RS Office's core.

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
- [x] Original Material icons + light glossy theme + empty/loading/error states
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

## NOW (2026-11-23)
v2.1: repository and package renamed to RS Office. Word/Excel/PowerPoint
read-modify-write editors added in v2; the first v2 CI failed on two
OfficeScreen expression errors and a missing `DocxEditor.newParagraph`, all
fixed. CI green again. White/light glossy fluid reskin, route back-stack,
credits in About, and post-scan page review (rotate / reorder / remove) are in.

DONE: v1.0.6 (`Vellum-1.2.apk`) shipped before the rename; v2.1 source pushed
to `main`, CI green on `a88abaf`.

NEXT (user direction): no automated deck generation. Study LibreOffice
architecture (clean room, no copied source) and build our own OOXML engine:
1. `PartStore` + `XmlTree` + `UndoStack` foundation — DONE, CI green.
2. Migrate DocxEditor, then XlsxEditor, then PptxEditor to that foundation.
3. Continue light glossy UI polish and tool expansion.
