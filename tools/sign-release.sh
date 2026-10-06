#!/usr/bin/env bash
# Signs the release build with the release key (ADR-0034), on the machine that
# holds the key; the build itself makes an unsigned package.
#
#   GEOSHARE_RELEASE_KEYSTORE=<file> GEOSHARE_RELEASE_ALIAS=<alias> \
#   GEOSHARE_RELEASE_PASSWORD=<password> tools/sign-release.sh [<output.apk>]
#
# The default output is build/geoshare-<version>.apk. The script refuses a key
# whose fingerprints are not the ones in android/release-fingerprints.txt, so a
# package signed with the wrong key never reaches the releases page.
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

: "${GEOSHARE_RELEASE_KEYSTORE:?the keystore file}"
: "${GEOSHARE_RELEASE_ALIAS:?the key alias}"
: "${GEOSHARE_RELEASE_PASSWORD:?the key password}"

readonly UNSIGNED="${REPO_ROOT}/android/app/build/outputs/apk/release/app-release-unsigned.apk"
version="$(sed -n 's/^appVersionName=//p' "${REPO_ROOT}/android/gradle.properties")"
out="${1:-${REPO_ROOT}/build/geoshare-${version}.apk}"

if [[ ! -f "${UNSIGNED}" ]]; then
    echo "error: no ${UNSIGNED#"${REPO_ROOT}"/}; build it with VARIANT=release tools/build.sh" >&2
    exit 1
fi

key="$(keytool -list -v -keystore "${GEOSHARE_RELEASE_KEYSTORE}" -alias "${GEOSHARE_RELEASE_ALIAS}" \
    -storepass:env GEOSHARE_RELEASE_PASSWORD | awk '/SHA1:|SHA256:/ { print $2 }' | sort)"
listed="$(grep -v '^#' "${REPO_ROOT}/android/release-fingerprints.txt" | sort)"
if [[ "${key}" != "${listed}" ]]; then
    echo "error: the key's fingerprints differ from android/release-fingerprints.txt" >&2
    exit 1
fi

echo "==> signing into ${out#"${REPO_ROOT}"/}" >&2
mkdir -p "$(dirname "${out}")"
apksigner sign --ks "${GEOSHARE_RELEASE_KEYSTORE}" --ks-key-alias "${GEOSHARE_RELEASE_ALIAS}" \
    --ks-pass env:GEOSHARE_RELEASE_PASSWORD --key-pass env:GEOSHARE_RELEASE_PASSWORD \
    --out "${out}" "${UNSIGNED}"
apksigner verify --print-certs "${out}" | grep -m1 'SHA-256'
