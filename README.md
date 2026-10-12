# VX7 Marketing

Offline Android app (Kotlin, Jetpack Compose, Material 3, Room) for recording which products you
collect from shops and in what quantity, then sharing the record as a PDF or PNG image.
No prices, rates, billing, or money totals anywhere.

## Features
- Product library (add, edit, delete with confirmation, search, duplicate/similar-name warning)
- New/Edit record: date and time, optional shop and location, multi-product picker, quantity + unit
  (13 units incl. custom "Other"), per-product notes, general notes
- Dashboard, history (search by shop/product, Today / Last 7 Days / This Month / date / range), reports
  (units are never added together)
- Real PDF (A4, multi-page) and PNG (auto height) export, shared through FileProvider + system chooser
- Versioned JSON backup, and merge-only restore with validation and confirmation
- Settings (theme, default unit), About. No login, ads, analytics, or special permissions

## Build
Push to GitHub: Actions -> "Build VX7 Marketing APK" runs tests, builds the debug APK and uploads the
artifact `VX7-Marketing.apk`. Locally: Android Studio (JDK 17) -> sync -> `:app:assembleDebug`.
The workflow installs Gradle 8.9 itself; no wrapper is committed.

## Status
Not yet compiled locally. On failure the workflow prints the real compiler error and uploads build.log.

## Known limits
- The unsaved record form survives rotation but is lost if Android kills the app process.
- Extremely large records (hundreds of products) may be too big for one PNG; share the PDF instead.
- Restore merges only; it never replaces or deletes existing data.
