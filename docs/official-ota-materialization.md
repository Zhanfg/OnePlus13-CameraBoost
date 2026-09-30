# Official OTA materialization planner

The OTA indexer can identify that an official package contains `payload.bin`,
`super.img` or partition images. The planner converts that fact into a **read-only,
manual materialization plan**.

It does not execute extraction tools, download dependencies, flash a phone or mount a
partition writable.

## Why only these partitions

The OnePlus 13 10-bit still/userspace investigation primarily needs:

- `vendor` — camera HAL, provider, codecs, vendor camera config and libraries;
- `odm` — device-specific camera config and hardware customization;
- `system_ext` — OPlus framework/system extensions;
- `product` — OPlus apps/config/product features;
- `system` — Android framework/media plumbing.

Other partitions can be added later if a specific evidence gap requires them.

## Workflow

First create an inventory:

```bash
cameraboost ota-index official.zip --out ota-index.json
```

Then generate the plan:

```bash
cameraboost ota-plan ota-index.json --out ota-plan.json
```

Typical plan:

```text
payload.bin
  -> payload extractor
  -> vendor.img / odm.img / system_ext.img / product.img / system.img
  -> detect sparse/raw + EROFS/ext4
  -> simg2img only if sparse
  -> read-only filesystem extraction
  -> cameraboost ota-index extracted/
```

For `super.img`, the first step becomes AOSP `lpunpack`.

## Tooling boundary

The plan may name external extraction utilities, including third-party payload extractors,
but those tools are **not evidence**. Only the materialized bytes from the official
OnePlus/OPPO package are evidence.

The repository does not vendor or auto-download those utilities.

## Repository hygiene

Do not commit:

- OTA ZIPs;
- `payload.bin`;
- partition images;
- proprietary camera libraries;
- camera calibration blobs;
- proprietary ML models;
- complete decrypted vendor camera configs.

Commit only derived inventories, hashes, small manually reviewed findings and clean-room
code.
