#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

APP_NAME="${APP_NAME:-Sales Mgr}"
APP_VERSION="${APP_VERSION:-1.0.0}"
VENDOR="${VENDOR:-PBL3}"
PACKAGE_IDENTIFIER="${PACKAGE_IDENTIFIER:-com.pbl3.salesmgr}"
PROVISIONING_API_BASE_URL="${PROVISIONING_API_BASE_URL:-}"
PROVISIONING_API_KEY="${PROVISIONING_API_KEY:-}"
PROVISIONING_API_ALLOW_LOCAL="${PROVISIONING_API_ALLOW_LOCAL:-false}"
OUTPUT_DIR="${OUTPUT_DIR:-dist/installers/macos}"
JPACKAGE_TEMP_DIR="${JPACKAGE_TEMP_DIR:-${TMPDIR:-/tmp}/pbl3-sales-mgr-jpackage-macos}"
JPACKAGE_INPUT_DIR="${JPACKAGE_INPUT_DIR:-target/jpackage/input-macos}"
MAIN_JAR="${MAIN_JAR:-pbl3-project-0.0.1-SNAPSHOT.jar}"
ICON_PATH="${ICON_PATH:-src/main/resources/AppIcon/AppIcon.icns}"

if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "This script must be run on macOS because jpackage can only build DMG on macOS." >&2
    exit 1
fi

if ! command -v jpackage >/dev/null 2>&1; then
    echo "jpackage was not found. Install/use a full JDK 21+ and make sure jpackage is on PATH." >&2
    exit 1
fi

if [[ -z "$PROVISIONING_API_BASE_URL" ]]; then
    echo "PROVISIONING_API_BASE_URL is required for release packaging." >&2
    echo "Example: PROVISIONING_API_BASE_URL=https://provisioning.example.com $0" >&2
    exit 1
fi

if [[ "$PROVISIONING_API_BASE_URL" != https://* ]]; then
    if [[ "$PROVISIONING_API_ALLOW_LOCAL" != "true" ]]; then
        echo "Release desktop packages must use an HTTPS provisioning API URL." >&2
        echo "For local/demo packaging, set PROVISIONING_API_ALLOW_LOCAL=true." >&2
        exit 1
    fi
    if [[ ! "$PROVISIONING_API_BASE_URL" =~ ^http://(localhost|127\.|0\.0\.0\.0|10\.|192\.168\.|172\.(1[6-9]|2[0-9]|3[0-1])\.) ]]; then
        echo "Local/demo packaging only allows localhost or private LAN HTTP URLs." >&2
        exit 1
    fi
fi

if [[ ! -f "$ICON_PATH" ]]; then
    echo "Icon file not found: $ICON_PATH" >&2
    exit 1
fi

echo "Building desktop release jar..."
./mvnw -q -Pdesktop-release -DskipTests package

if [[ ! -f "target/$MAIN_JAR" ]]; then
    echo "Main jar not found: target/$MAIN_JAR" >&2
    exit 1
fi

rm -rf "$JPACKAGE_TEMP_DIR" "$JPACKAGE_INPUT_DIR" "$OUTPUT_DIR"
mkdir -p "$JPACKAGE_TEMP_DIR" "$JPACKAGE_INPUT_DIR" "$OUTPUT_DIR"
cp "target/$MAIN_JAR" "$JPACKAGE_INPUT_DIR/$MAIN_JAR"
xattr -cr "$JPACKAGE_INPUT_DIR" "$ICON_PATH" "$JPACKAGE_TEMP_DIR" "$OUTPUT_DIR" 2>/dev/null || true

JAVA_OPTIONS=(
    "-DAPP_DESKTOP_RELEASE=true"
    "-DPROVISIONING_API_BASE_URL=$PROVISIONING_API_BASE_URL"
    "-Dapp.client.version=$APP_VERSION"
    "-Dspring.profiles.active=tenant-client"
    "-Dspring.main.web-application-type=none"
)

if [[ -n "$PROVISIONING_API_KEY" ]]; then
    JAVA_OPTIONS+=("-DPROVISIONING_API_KEY=$PROVISIONING_API_KEY")
fi

if [[ "$PROVISIONING_API_ALLOW_LOCAL" == "true" ]]; then
    JAVA_OPTIONS+=("-DPROVISIONING_API_ALLOW_LOCAL=true")
fi

JPACKAGE_ARGS=(
    --type dmg
    --name "$APP_NAME"
    --app-version "$APP_VERSION"
    --vendor "$VENDOR"
    --description "Sales Management System desktop client"
    --dest "$OUTPUT_DIR"
    --temp "$JPACKAGE_TEMP_DIR"
    --input "$JPACKAGE_INPUT_DIR"
    --main-jar "$MAIN_JAR"
    --icon "$ICON_PATH"
    --mac-package-name "$APP_NAME"
    --mac-package-identifier "$PACKAGE_IDENTIFIER"
)

for option in "${JAVA_OPTIONS[@]}"; do
    JPACKAGE_ARGS+=(--java-options "$option")
done

echo "Packaging DMG..."
jpackage "${JPACKAGE_ARGS[@]}"

echo "DMG output:"
find "$OUTPUT_DIR" -maxdepth 1 -type f -name "*.dmg" -print
