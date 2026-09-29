# Upstream references

Pinned during bootstrap research. These projects are references, not vendored dependencies.

| Project | Purpose | License | Pinned commit |
|---|---|---|---|
| `Weverses/OPCameraPro` | OPlus Camera LSPosed feature hooks/capability experiments | GPL-3.0 | `1fe83082dfbe176f723437018ee9f89f3478c2b3` |
| `suqi8/OShin` | ColorOS/OxygenOS LSPosed module; historical camera hooks are research references | AGPL-3.0 | `e870b90dc01bc021e7bc749a69ddfd59eb484c78` |
| `Kill3rEz/oplus-camera-config-dump` | Frida APS decryption/dump research | PolyForm Noncommercial 1.0.0 | `11f2c8581d45cf47da2c4a7dbbf7ef81b31e47d0` |

## License boundary

- Do not copy PolyForm Noncommercial implementation into the MIT core.
- Do not paste GPL/AGPL code into the MIT core.
- If a future Android/LSPosed component is actually derived from GPL/AGPL code, keep it in a separately licensed component and comply with upstream.
- APS JSON produced from a user's own device should remain local by default; avoid committing decrypted vendor configuration to a public repository.

## Known observations

- OPCameraPro explicitly documents that exposing a feature does not mean the underlying pipeline can run it; OnePlus 13 + 4K120 is a concrete failure example.
- OShin history is worth mining for removed OPlus Camera hook names and feature gates.
- The APS dump project exposes runtime-decrypted OPlus APS configuration and is useful as an external producer for this repository's clean-room JSON diff tooling.
