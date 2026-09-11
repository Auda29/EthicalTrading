# Publishing Ethical Trading

## Current release policy

`gradle.properties` contains the complete binary version (`0.1.0+mc26.2`). The tag is exactly `v` plus that value. `release.json` explicitly selects **beta** for the first public version; do not infer stability from the version string. Release notes, commit messages and workflow output are in English.

Minecraft 26.2 / Fabric only. Fabric API is required. Do not copy OreHeightIndicator's Client marker, NeoForge targets, Mod Menu or Cloth Config dependencies.

## Immutable release flow

1. A new version on `main` or its matching tag starts `Release`. Normal changes with an existing version tag do not release again.
2. Java 25 builds the mod once; Java tests, packaged-JAR GameTests and both restart phases must pass.
3. GitHub Release stores the production JAR, its exact-name SHA-256 file, complete project ZIP and test report. Beta/alpha are GitHub prereleases. Assets and tags are never clobbered or force-moved.
4. The workflow downloads the uploaded JAR/checksum and verifies them again. A small `release-created` Actions artifact distinguishes a real new release from a no-op run.
5. Independent Modrinth and CurseForge workflows download this GitHub JAR and checksum. They check the tag-to-commit relation and checksums before uploads. Neither workflow rebuilds Minecraft code.

The Modrinth publisher is an isolated Gradle project using Minotaur **2.9.0**. Its `modrinth` task has no `assemble` dependency and accepts only an explicit existing file. Modrinth is read back after upload and checked for exact SHA-512, version, game/loader and required Fabric API dependency.

The CurseForge Upload API returns a submitted file ID, **not proof of public availability**. The workflow saves the response as an Actions artifact and explicitly reports moderation/public verification as outstanding. Inspect that exact file in the author dashboard before calling it published. Do not blindly rerun a failed/timeout upload: it may already have been accepted.

## One-time platform setup (not automatic)

Create an **Ethical Trading** project on each platform under your own author account. These require authenticated author access and may require initial moderation; GitHub credentials cannot create them. No platform project IDs or tokens are embedded in this repository.

Modrinth project compatibility: **client optional, server required**. Category suggestions: utility and game mechanics. CurseForge file environment: **Server**, loader **Fabric**, Minecraft **26.2**. Declare Fabric API required on both platforms. Do not select unsupported Minecraft versions. Use the README/RELEASE_NOTES content, preserving the beta/testing limitations.

In GitHub Settings → Secrets and variables → Actions, add:

| Kind | Name | Value |
|---|---|---|
| Variable | `MODRINTH_PROJECT_ID` | Public project ID or slug of Ethical Trading |
| Secret | `MODRINTH_TOKEN` | Token allowed to create versions and read back this project's metadata |
| Variable | `CURSEFORGE_PROJECT_ID` | Numeric project ID of Ethical Trading |
| Secret | `CURSEFORGE_API_TOKEN` | Author upload token from CurseForge |

Enter secrets directly through GitHub's UI or interactive `gh secret set NAME`. Never commit tokens, place them in Gradle properties or paste them into chat. GitHub does not reveal existing secret values, including those stored in OreHeightIndicator.

With no platform project ID configured, that platform's job is skipped. With an ID but a missing token, it fails clearly before upload. Adding an ID after a GitHub release does not retroactively publish it: dispatch the corresponding workflow for the exact existing tag after setup/moderation is ready.

## Recovery and duplicate safety

A manual platform dispatch requires the exact tag and explicit `confirm_not_uploaded=true`. Check the author dashboard first, including files pending review. Automatic triggers require the trusted main-branch Release run, its first attempt and its creation marker. Each platform serializes its own uploads. POST uploads are not retried automatically. An upload receipt is submission evidence, not permission to repeat the request.

If a GitHub release fails after creating a tag but before publishing, do not force-move the tag or overwrite assets. Inspect the exact tagged commit and failed run. Recovery may need publishing the already-verified artifacts manually from that commit; rerunning an ordinary main push is intentionally a no-op once its tag exists.

## Local verification without uploading

```sh
python scripts/test_release_tools.py
./gradlew --no-daemon test assemble
python scripts/package-release.py
python scripts/release_tools.py checksum
```

For Minotaur's no-upload test, supply a synthetic placeholder token only as an environment variable and pass `-Pmodrinth_debug=true` along with explicit project ID, existing JAR/changelog paths, version and release type. Debug output is only a local payload test, not a real API upload. `--dry-run` must list only `:modrinth`, never `assemble` or compilation tasks.

Actual platform publishing is not tested until authenticated platform projects and credentials are configured. See `TESTING.md` for the separate mod-behavior verification.
