# FileFly Roadmap

This file is the single source of truth for planned features. Versioning follows
[Conventional Commits](https://www.conventionalcommits.org/) via Release Please:
`fix:` → patch, `feat:` → minor, `feat!:`/`BREAKING CHANGE` → major.

## v0.1 — MVP (initial scaffold)

- [x] Multi-module project scaffold (build-logic convention plugins, version catalog)
- [x] CI/CD: lint + test + debug build; Release Please + signed APK release + `latest.json` on gh-pages
- [x] In-app updater (check → download → SHA-256 verify → PackageInstaller)
- [x] Onboarding: server URL + invite redemption (`/auth/invite/validate`)
- [x] Chunked upload client (`init` → `chunk` → `complete`) with progress
- [x] Share Sheet receiver (`SEND` / `SEND_MULTIPLE`, `image/*` + `video/*`)
- [x] Directory picker (role- and base-path-aware) + create folder
- [x] Conflict dialog (overwrite / rename / skip) with "apply to all"
- [x] Upload history (Room)
- [ ] First end-to-end test against a live `filefly-server`
- [ ] Verify signed release build + updater round-trip on a real device

## v0.5 — Polish &amp; robustness

- [ ] Per-conflict prompt driven by the server's `init` response (not just local listing)
- [ ] Retry failed uploads from the history screen
- [ ] Resume interrupted uploads (persist upload sessions across app restarts)
- [ ] Thumbnails in the upload queue (Coil)
- [ ] Foreground service + notification for large/long uploads
- [ ] Settings: theme mode (system/light/dark), connection diagnostics
- [ ] Real brand icon + splash (replace placeholder Firefly mark)
- [ ] Screenshots in README

## v1.0 — Stable

- [ ] Full role matrix exercised end-to-end (admin/user/service/guest)
- [ ] Rename/delete operations in the picker (admin)
- [ ] Accessibility pass (TalkBack, large fonts)
- [ ] Localization (EN + DE)
- [ ] iOS evaluation (feasibility of a companion app; sideload constraints)

## Ideas / backlog

- Background auto-upload of a chosen album (opt-in)
- Multiple server profiles
- Upload queue prioritization / pause-resume UI
- Optional TLS with self-signed cert pinning for the LAN server
