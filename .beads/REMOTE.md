# Beads Dolt remote setup

Session closeout runs `bd dolt push`. This checkout starts with **no Dolt remotes**, so push fails with:

```text
fatal: remote 'origin' not found
```

Git `origin` is unrelated — beads state lives in `.beads/dolt` and needs its own remote.

## One-time setup (maintainer)

1. Create a Dolt remote (DoltHub database, self-hosted Dolt SQL server, or another git-compatible Dolt remote your team uses).
2. From the repo root:

```bash
# Inspect
bd dolt remote list
bd dolt status

# Add (URL is team-specific — do not commit secrets)
bd dolt remote add origin <your-dolt-remote-url>

# Verify
bd dolt remote list
bd dolt push
```

3. Optional helper:

```bash
./buildsystem/setup-beads-remote.sh
# or with URL:
./buildsystem/setup-beads-remote.sh 'https://doltremoteapi.dolthub.com/<org>/<db>'
```

## Agents / CI

- Prefer `bd dolt push` after issue mutations when a remote exists.
- If `bd dolt remote list` is empty, document the gap and continue; **do not** invent a remote URL.
- `git push` still ships code; beads history remains local until a remote is configured.

## Why this is separate from git

| | Git | Beads Dolt |
|---|---|---|
| Data | source tree | issues / deps / memory |
| Remote config | `.git/config` | `bd dolt remote` |
| Push command | `git push` | `bd dolt push` |
