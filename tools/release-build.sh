#!/usr/bin/env bash
# Bumps the patch version, builds the signed release APK and AAB, and copies them with the R8 mapping
# to dist/<version>/. Then commits the bump with its F-Droid changelog and tags it v<version>.
#
# Usage:
#   tools/release-build.sh "• Fixed X" "• Added Y"   bump, build, commit and tag; each argument is a changelog line
#   tools/release-build.sh --no-bump                 rebuild the current version without committing
#
# Signing reads H3LWP_STORE_FILE, H3LWP_STORE_PASSWORD, H3LWP_KEY_ALIAS and H3LWP_KEY_PASSWORD
# from ~/.gradle/gradle.properties or ORG_GRADLE_PROJECT_* environment variables.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK_DIR="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
SIGNING_PROPERTIES=(H3LWP_STORE_FILE H3LWP_STORE_PASSWORD H3LWP_KEY_ALIAS H3LWP_KEY_PASSWORD)
BUILD_GRADLE="$REPO_ROOT/android/build.gradle"
CHANGELOG_DIR="$REPO_ROOT/fastlane/metadata/android/en-US/changelogs"
APK="$REPO_ROOT/android/build/outputs/apk/release/android-release.apk"
AAB="$REPO_ROOT/android/build/outputs/bundle/release/android-release.aab"
MAPPING="$REPO_ROOT/android/build/outputs/mapping/release/mapping.txt"

fail() {
  echo "error: $*" >&2
  exit 1
}

has_signing_property() {
  local name="$1"
  local env_name="ORG_GRADLE_PROJECT_$name"
  [[ -n "${!env_name:-}" ]] && return 0
  grep -qs "^$name=" "$HOME/.gradle/gradle.properties" "$REPO_ROOT/gradle.properties"
}

read_version_code() {
  sed -n 's/^ *versionCode \([0-9][0-9]*\)$/\1/p' "$BUILD_GRADLE"
}

read_version_name() {
  sed -n 's/^ *versionName "\([^"]*\)"$/\1/p' "$BUILD_GRADLE"
}

restore_version_files() {
  git -C "$REPO_ROOT" checkout --quiet -- "$BUILD_GRADLE"
  rm -f "$CHANGELOG_DIR/$NEW_CODE.txt"
  echo "Build failed; version bump reverted" >&2
}

IS_BUMP=true
if [[ "${1:-}" == "--no-bump" ]]; then
  IS_BUMP=false
  shift
fi
CHANGELOG_LINES=("$@")

for property in "${SIGNING_PROPERTIES[@]}"; do
  has_signing_property "$property" || fail "signing property $property is not set"
done

BUILD_TOOLS="$(ls -d "$SDK_DIR"/build-tools/* 2>/dev/null | sort -V | tail -1)"
[[ -n "$BUILD_TOOLS" ]] || fail "no Android build-tools in $SDK_DIR"

cd "$REPO_ROOT"
HAS_UNCOMMITTED_CHANGES=false
[[ -n "$(git status --porcelain --untracked-files=no)" ]] && HAS_UNCOMMITTED_CHANGES=true

if [[ "$IS_BUMP" == true ]]; then
  [[ "$HAS_UNCOMMITTED_CHANGES" == false ]] || fail "commit or stash changes first; the bump is committed"
  [[ ${#CHANGELOG_LINES[@]} -gt 0 ]] || fail "pass changelog lines, e.g. tools/release-build.sh \"• Fixed X\""

  OLD_CODE="$(read_version_code)"
  OLD_NAME="$(read_version_name)"
  [[ "$OLD_NAME" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]] || fail "versionName '$OLD_NAME' is not MAJOR.MINOR.PATCH"
  NEW_NAME="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.$((BASH_REMATCH[3] + 1))"
  NEW_CODE="$((OLD_CODE + 1))"
  git rev-parse --quiet --verify "refs/tags/v$NEW_NAME" >/dev/null && fail "tag v$NEW_NAME already exists"

  sed -i '' "s/^\( *versionCode \)$OLD_CODE$/\1$NEW_CODE/; s/^\( *versionName \)\"$OLD_NAME\"$/\1\"$NEW_NAME\"/" "$BUILD_GRADLE"
  printf '%s\n' "${CHANGELOG_LINES[@]}" > "$CHANGELOG_DIR/$NEW_CODE.txt"
  trap restore_version_files EXIT
  echo "Bumped $OLD_NAME ($OLD_CODE) -> $NEW_NAME ($NEW_CODE)"
elif [[ "$HAS_UNCOMMITTED_CHANGES" == true ]]; then
  echo "warning: uncommitted changes are included in this build" >&2
fi

./gradlew --quiet clean :android:assembleRelease :android:bundleRelease

[[ -f "$APK" ]] || fail "signed APK not found at $APK (an unsigned build means signing is misconfigured)"
"$BUILD_TOOLS/apksigner" verify "$APK" 2>/dev/null || fail "APK signature verification failed"

BADGING="$("$BUILD_TOOLS/aapt2" dump badging "$APK")"
VERSION_NAME="$(sed -n "s/.*versionName='\([^']*\)'.*/\1/p" <<< "$BADGING")"
VERSION_CODE="$(sed -n "s/.*versionCode='\([^']*\)'.*/\1/p" <<< "$BADGING")"
OUT_DIR="$REPO_ROOT/dist/$VERSION_NAME"
mkdir -p "$OUT_DIR"
cp "$APK" "$OUT_DIR/h3lwp-$VERSION_NAME.apk"
cp "$AAB" "$OUT_DIR/h3lwp-$VERSION_NAME.aab"
cp "$MAPPING" "$OUT_DIR/mapping-$VERSION_NAME.txt"

if [[ "$IS_BUMP" == true ]]; then
  trap - EXIT
  git add "$BUILD_GRADLE" "$CHANGELOG_DIR/$NEW_CODE.txt"
  git commit --quiet -m "chore(android): bump version to $NEW_NAME ($NEW_CODE)"
  git tag "v$NEW_NAME"
fi

echo "Built $VERSION_NAME ($VERSION_CODE), signed and verified:"
echo "  $OUT_DIR/h3lwp-$VERSION_NAME.aab"
echo "  $OUT_DIR/h3lwp-$VERSION_NAME.apk"
echo "  $OUT_DIR/mapping-$VERSION_NAME.txt"
if [[ "$IS_BUMP" == true ]]; then
  echo "Committed and tagged v$VERSION_NAME. Publish with: git push origin main v$VERSION_NAME"
fi
