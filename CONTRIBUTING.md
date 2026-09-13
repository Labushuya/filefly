# Contributing to FileFly

Thanks for your interest! FileFly is MIT-licensed and meant to be usable and
hackable by anyone running their own home server.

## Commit messages

We use [Conventional Commits](https://www.conventionalcommits.org/). Release Please
reads them to bump the version and generate the changelog:

- `fix: …` → patch release
- `feat: …` → minor release
- `feat!: …` or a `BREAKING CHANGE:` footer → major release
- `chore:`, `docs:`, `ci:`, `refactor:`, `test:` → no release on their own

Example: `feat(upload): resume interrupted chunked uploads`

## Development

Requires **JDK 21**. Locally, create `local.properties` with your SDK path:

```properties
sdk.dir=/path/to/Android/Sdk
```

Then:

```bash
./gradlew ktlintCheck detekt          # lint + static analysis (must pass)
./gradlew ktlintFormat                # auto-fix formatting
./gradlew testDebugUnitTest test      # unit tests
./gradlew :app:assembleDebug          # debug APK
```

The JVM core (`core-common`, `core-network` tests) runs without an emulator.

## Architecture rules

- `core-*` modules stay DI-annotation-free; all Hilt wiring lives in `:app`.
- `feature-*` modules depend only on `core-*`, never on each other.
- ViewModels are built via `simpleFactory` in `:app` (no Hilt in ViewModels).
- SDK levels and toolchain versions live in `build-logic` and `gradle/libs.versions.toml` — change them there, not per-module.

## Releases

Do **not** tag manually. Merge the Release Please PR on `main`; that creates the
tag, which triggers the signed APK build and publishes `latest.json` to `gh-pages`.
The signing key is held only as a GitHub secret and must remain stable across all
releases (otherwise the in-app updater breaks with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`).

## Reporting issues

Open a GitHub issue with your Android version, device, FileFly version, and steps
to reproduce. For server-side problems, check [filefly-server](https://github.com/Labushuya/filefly-server) first.
