# FileFly — End-to-End Test Guide

So testest du FileFly komplett: Server auf dem Pi, App aufs Handy, Admin-Setup, Invites (Code/QR/Deep-Link), Upload. Beispiel-IP `192.168.178.123` — anpassen an deinen Pi.

---

## Teil 1 — Server auf dem Pi starten

### 1.1 Voraussetzungen
- Pi mit Docker + Docker Compose (`docker compose version` funktioniert)
- Gemountete HDD, z.B. unter `/mnt/data` (Upload-Ziel)

### 1.2 Repo holen & konfigurieren (per SSH auf dem Pi)
```bash
ssh pi@192.168.178.123
git clone https://github.com/Labushuya/filefly-server.git
cd filefly-server

# SECRET_KEY generieren (PFLICHT — Server startet sonst NICHT)
openssl rand -hex 32
# Ausgabe kopieren und in docker-compose.yml bei SECRET_KEY einsetzen
nano docker-compose.yml    # SECRET_KEY=<dein generierter Wert>
```
> ⚠️ Lässt du `SECRET_KEY=change-me-in-production`, bricht der Server bewusst mit einer Fehlermeldung ab (Fail-Fast). Das ist Absicht.

Passe ggf. den Volume-Pfad an deine HDD an (`/mnt/data:/data`).

### 1.3 Starten & Bootstrap-Admin-Code holen
```bash
docker compose up -d --build
docker compose logs filefly-server
```
Im Log erscheint EINMALIG (Beispiel):
```
====================================================================
 FileFly bootstrap admin invite (shown ONCE, valid 24h, single use):

     xB3kf9_QzAbCdEf12

 Redeem it in the FileFly app now. It is not recoverable.
====================================================================
```
> 📋 **Diesen Code notieren.** Er ist nur 24 h und genau 1× gültig, danach verbrannt. Nicht wiederherstellbar — bei Verlust: DB löschen (`rm filefly.db`) + Server neu starten erzeugt einen neuen.

### 1.4 Server erreichbar? (vom PC oder Pi)
```bash
curl http://192.168.178.123:8000/health      # {"status":"ok"} o.ä.
curl http://192.168.178.123:8000/version
```

---

## Teil 2 — App aufs Handy

### 2.1 APK installieren (v0.3.0 — mit Logo + Invite-Feature)
- **Neuinstallation:** https://github.com/Labushuya/filefly/releases/latest → `filefly-v0.3.0.apk` herunterladen, auf dem Handy öffnen, „Installieren aus unbekannter Quelle" erlauben.
- **Wenn v0.2.0 schon installiert:** App öffnen → Einstellungen → „Nach Updates suchen" → der In-App-Updater zieht v0.3.0.

### 2.2 Als Admin einrichten (Bootstrap)
1. App öffnen → Onboarding.
2. **Server-URL** eingeben: `http://192.168.178.123:8000`
3. „Verbindung testen" → muss OK melden.
4. **Invite-Code**: den Bootstrap-Code aus 1.3 eingeben (oder QR/Deep-Link, falls du ihn so bekommst — beim Bootstrap ist es nur der Code).
5. „Einladung einlösen" → du bist jetzt **Admin** (volle Rechte).

---

## Teil 3 — Als Admin Invites erstellen & teilen

1. In der App: **Einstellungen → „Einladungen verwalten"** (nur für Admins sichtbar).
2. Neuen Invite anlegen:
   - **Rolle**: z.B. `GUEST` (temporär, nur Upload) oder `USER` (fester Ordner, upload+mkdir+rename)
   - **Verzeichnis** (base_path): z.B. `photos` — hierhin darf der Eingeladene hochladen
   - **Ablauf** (Stunden) + **max. Nutzungen** setzen
3. „Invite erstellen" → du bekommst **drei Weitergabe-Formen**:
   - **Code** (Copy-Button) — zum Abtippen
   - **QR-Code** — anderes Gerät scannt ihn
   - **Deep-Link teilen** (`filefly://invite?...`) — per WhatsApp/Mail schicken

---

## Teil 4 — Als Gast einlösen (zweites Gerät)

**Variante A — QR scannen:**
1. FileFly auf Gerät 2 installieren, Onboarding öffnen.
2. „QR scannen" (Kamera-Erlaubnis geben) → Admin-QR aus Teil 3 scannen.
3. Server-URL + Code werden automatisch übernommen → einlösen.

**Variante B — Deep-Link:**
- Admin schickt den `filefly://…`-Link → Gast tippt ihn an → App öffnet sich vorbefüllt.

**Variante C — Code tippen:**
- Server-URL + Code manuell eingeben (wie beim Admin-Setup).

---

## Teil 5 — Upload testen (der Kern von FileFly)

1. In der Galerie/Dateien mehrere **Fotos/Videos** markieren → **Teilen** → **FileFly**.
2. In FileFly: Zielverzeichnis wählen (respektiert deine Rechte/base_path).
3. Upload startet — Fortschritt pro Datei + gesamt.
4. **Konflikt** (Datei existiert schon): Dialog fragt overwrite / rename / skip, mit „für alle übernehmen".
5. Prüfen auf dem Pi:
   ```bash
   ssh pi@192.168.178.123 "ls -la /mnt/data/photos/"
   ```
   → deine Dateien liegen da.
6. In der App: **Verlauf** zeigt alle Uploads mit Status.

---

## Teil 6 — Rechte-Enforcement gegenprüfen (Sicherheit)

- Als **GUEST** versuchen, in einen anderen Ordner als `base_path` zu laden → Server lehnt mit 403 ab.
- Nach Ablauf/max_uses eines Invites: erneutes Einlösen → „Invite expired" / „exhausted".
- Detaillierte manuelle Test-Matrix (G01–G12, mit curl-Beispielen): siehe **filefly-server → docs/test-manifest.html** (im Browser öffnen).

---

## Troubleshooting

| Problem | Ursache / Fix |
|---|---|
| Server startet nicht, Log sagt „SECRET_KEY" | Default nicht ersetzt → echten Key setzen (1.2) |
| „Verbindung testen" schlägt fehl | Falsche IP/Port, Pi-Firewall, Server nicht up (`docker compose ps`) |
| Bootstrap-Code weg | `rm filefly.db` auf dem Pi + `docker compose up -d --force-recreate` → neuer Code im Log |
| Upload landet nicht auf HDD | Volume-Pfad in compose falsch, oder Schreibrechte auf `/mnt/data` fehlen |
| QR-Scan tut nichts | Kamera-Erlaubnis verweigert → Android-App-Einstellungen |
| Config-Änderung wirkt nicht | `docker compose up -d --force-recreate` (nicht nur `restart`) |
