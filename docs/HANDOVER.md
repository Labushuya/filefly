# FileFly — Handover für neue Claude-Session

**Erstellt:** 2026-10-14  
**Ursprungs-Session:** f8b67b49-0170-4674-b12b-3134371cc72a  
**Ziel:** Opus 5.5 oder gleichwertiges Modell

---

## Projektstatus in einem Satz

FileFly ist ein funktionierendes, veröffentlichtes Android-App + FastAPI-Server-Duo (beide CI-grün, beide auf GitHub). Der erste echte **End-to-End-Test gegen den Pi steht noch aus** — das ist der primäre nächste Schritt.

---

## Repos

| Repo | Branch | Letzter Commit | Release |
|---|---|---|---|
| `github.com/Labushuya/filefly` | **main** | `9a2ba48` | v0.3.0 |
| `github.com/Labushuya/filefly-server` | **master** (!) | `8296402` | v0.1.4 |

⚠️ Der Server-Repo lebt auf `master`, nicht `main`. Das hat in der Vergangenheit dazu geführt, dass die CI nie lief — ist inzwischen gefixt, aber immer daran denken.

---

## Was fertig ist

### App (v0.3.0, CI grün, APK auf GitHub Releases)

- Multi-Modul Kotlin/Compose: `core-common/network/data/ui`, `feature-onboarding/upload/invite/history/settings/updater`
- **Share-Sheet-Upload** (SEND/SEND_MULTIPLE, image/video, chunked mit Fortschritt)
- **Konflikt-Dialog** (overwrite/rename/skip, „für alle übernehmen")
- **Admin-Invite-Screen** (role-gated): Code + QR + Deep-Link `filefly://invite?server=…&code=…`
- **QR-Scan** im Onboarding (ZXing, kein Play-Services — Sideload/degoogled-tauglich)
- **In-App-Updater** (1:1 OverlAI-Pattern, gh-pages latest.json, SHA-256-Verify)
- **Branding**: TLOU-Fireflies-Emblem, Medien-Flügel (Foto + Video), Orange/Gelb auf Schwarz
- Release-Pipeline: Release-Please → `workflow_run` → signiertes Universal-APK → GHCR pages

### Server (v0.1.4, CI grün, multi-arch GHCR-Image)

- FastAPI, 4 Rollen (ADMIN/USER/SERVICE/GUEST), JWT
- One-Time-Bootstrap-Admin (ersten Start → Code ins Log → einmalig, verbrennt)
- Invite-Codes als SHA-256 in DB (kein Klartext), Fail-Fast bei Default-SECRET_KEY
- GHCR-Image: `ghcr.io/labushuya/filefly-server:latest` (amd64 + arm64)
- HomeNexus/hubctl-Integration: `.hubctl.json` + `deploy/docker-compose.fam.ily.yml`

---

## Was SOFORT als nächstes zu tun ist

### 0. Zwei manuelle Schritte (nur der Nutzer kann das)

**a) GHCR-Package public stellen:**  
GitHub → Profil → Packages → `filefly-server` → Package settings → Change visibility → **Public**  
Ohne das schlägt `docker pull` auf dem Pi fehl.

**b) Lokale Secret-Dateien sichern** (siehe Abschnitt unten).

### 1. Ersten Pi-Deploy via hubctl durchführen

Voraussetzung: GHCR public + `hubctl.ps1` auf DEV-PC (H:\DEV\_tools\hubctl\).

```powershell
cd H:\DEV\_tools\hubctl
.\hubctl.bat   # oder: powershell -File "H:\DEV\_tools\hubctl\hubctl.ps1"
# → FileFly in Liste → Bootstrap
# → Admin-Code aus Docker-Log lesen:
#   docker compose -f /srv/hubdata/stacks/filefly/docker-compose.yml logs filefly
```

Vollständige Anleitung: `filefly-server/docs/deploy-hubctl.md`

### 2. End-to-End-Test (aus `filefly/docs/TESTING.md`)

- App installieren (v0.3.0 APK von GitHub Releases oder via In-App-Updater von v0.2.0)
- Server-URL: `http://192.168.178.123:8000`
- Bootstrap-Code einlösen → Admin
- Invite erstellen → zweites Gerät → Upload → Dateien auf Pi verifizieren

---

## Dependabot-PRs — was tun?

Aktuell offen in `filefly` (Stand 2026-10-14):

| PR | Typ | Was | Empfehlung |
|---|---|---|---|
| #1 | CI-Action | gradle-actions 4→6 | ⛔ CI bricht, erst prüfen |
| #3 | CI-Action | actions/checkout 4→7 | 🟡 vorsichtig testen |
| #4 | CI-Action | release-please 4→5 | ⛔ Breaking-Change, erst prüfen |
| #5 | CI-Action | action-gh-release 2→3 | 🟡 testen |
| #6 | CI-Action | setup-java 4→6 | 🟡 testen |
| #7 | androidx | 9 Updates | 🟡 als Gruppe testen |
| #9 | kotlin | 10 Updates | ⛔ Major, eigene PR-Runde |
| #10 | AGP | 8.7.3 → 9.4.0 | ⛔ Breaking Major, NICHT blind mergen |
| #11 | ktlint | 12 → 14 | ⛔ Breaking Major |
| #12 | okhttp | 4 → 5 | ⛔ Breaking Major |
| #13 | detekt | 1.23.7 → 1.23.8 | ⛔ CI bricht aktuell |
| #14 | hilt | 2.53 → 2.60 | 🟡 Minor, testen |
| #15 | mockwebserver | 4 → 5 | ⛔ Major (okhttp5) |

PRs #18 (robolectric) und #19 (truth) wurden bereits gemergt (CI war grün).

**Sichere nächste Merges:** erst CI-Runs der Actions-PRs (#3, #5, #6) abwarten und prüfen.

---

## Architektur-Schnellreferenz

### Server-API-Endpunkte
```
POST /auth/invite/validate  → TokenResponse{access_token, role, base_path, permissions}
POST /auth/invite (Admin)   → InviteCreateResponse{code, url, role, ...}
GET  /auth/invites (Admin)  → [InviteSummary{id, code_hint, ...}]  (kein Klartext-Code)
DELETE /auth/invite/{id}
GET  /files/list?path=
POST /files/upload/init → /files/upload/chunk → /files/upload/complete
POST /files/mkdir | rename | DELETE /files/delete
GET  /health | /version
```

### Invite-URI-Format (QR + Deep-Link)
```
filefly://invite?server=<url-encoded>&code=<code>
```
Client baut diese URI selbst via `InviteUri.build(serverUrl, code)` — das server-seitige `url`-Feld enthält nur `?code=`.

### App-Namespace / Modul-Regel
- Namespace: `de.filefly`
- `feature-*` → `core-*` → (nie rückwärts)
- ViewModels: `simpleFactory` (kein `@HiltViewModel`), Hilt nur für Singletons in `AppModule`

### Signing / Release-Pipeline (App)
- Release-Please → merged PR erzeugt Tag → `release.yml` triggert via `workflow_run`
- **NICHT** manuell taggen (GITHUB_TOKEN-Tags triggern keinen Folge-Workflow)
- PKCS12-Falle: `ANDROID_KEY_PASSWORD` == `ANDROID_KEYSTORE_PASSWORD` (kein separates Key-PW)

---

## Bekannte Fallstricke

| Fallstrick | Detail |
|---|---|
| Server auf `master` | filefly-server Branch ist `master`, nicht `main` |
| PKCS12 Key-PW | Java-21 `.jks` = PKCS12, Key-PW muss == Store-PW sein |
| `?attr/colorControlNormal` | Bricht VectorDrawable Resource-Linking → weglassen |
| Docker Datei-Mount | Fehlende Datei → Docker macht Verzeichnis → SQLite-Fehler; immer Verzeichnis-Mount |
| hubctl `.bat` auf `H:` | `%~dp0` funktioniert nicht wenn H: gemappt/elevated; vollen Pfad nutzen |
| GHCR lowercase | `github.repository_owner` = Labushuya (Groß) → via `tr` kleinschreiben |
| stoqr in hubctl | 17 Namens-Hardcodes (keine .hubctl.json) — nicht anfassen, produktiv |

---

## Roadmap (Single Source of Truth: `ROADMAP.md` im App-Repo)

**v0.5 — Nächste Iteration:**
- Retry fehlgeschlagener Uploads aus der History
- Resume unterbrochener Uploads (persist sessions)
- Thumbnails in Upload-Queue (Coil)
- Foreground Service + Notification für große Uploads
- Settings: Theme-Mode, Connection-Diagnostics

**v1.0:**
- Volle Rollen-Matrix end-to-end
- Rename/Delete im Picker (Admin)
- Accessibility, Localization (EN + DE)
- iOS-Evaluation

---

## Lokale Dateien — was NICHT in Git ist

Diese Dateien existieren nur lokal und müssen gesichert werden:

```
C:\Code\claude\apps\filefly\filefly-release.jks          (338 Bytes → RSA-4096 Keystore)
C:\Code\claude\apps\filefly\.signing-secrets.local.txt   (4394 Bytes → Passwörter)
C:\Code\claude\misc\hubctl.ps1                           (133 KB → bearbeitete Version)
```

Backup-Kopie liegt in: `C:\Code\claude\_backups\filefly-snapshot-2026-09-18\secrets-and-untracked\`

Die 4 GitHub-Secrets (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`) sind **bereits gesetzt** in `Labushuya/filefly`. Falls neu gesetzt werden muss:
```bash
cd C:\Code\claude\apps\filefly
gh secret set ANDROID_KEYSTORE_BASE64  --repo Labushuya/filefly --body "$(base64 -w0 filefly-release.jks)"
gh secret set ANDROID_KEYSTORE_PASSWORD --repo Labushuya/filefly --body "$(grep ANDROID_KEYSTORE_PASSWORD .signing-secrets.local.txt | cut -d= -f2-)"
gh secret set ANDROID_KEY_ALIAS         --repo Labushuya/filefly --body "$(grep ANDROID_KEY_ALIAS .signing-secrets.local.txt | cut -d= -f2-)"
gh secret set ANDROID_KEY_PASSWORD      --repo Labushuya/filefly --body "$(grep ANDROID_KEY_PASSWORD .signing-secrets.local.txt | cut -d= -f2-)"
```
