#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SRC_DIR="$ROOT_DIR/deploy/crd"
DEST_DIR="$ROOT_DIR/charts/microcks-operator/crds"

usage() {
  echo "Usage: $0 [--check]" >&2
}

CHECK=0
if [[ "${1:-}" == "--check" ]]; then
  CHECK=1
elif [[ "${1:-}" != "" ]]; then
  usage; exit 2
fi

mkdir -p "$DEST_DIR"
status=0
for f in "$SRC_DIR"/*.yml; do
  base="$(basename "$f")"
  if [[ $CHECK -eq 1 ]]; then
    if ! cmp -s "$f" "$DEST_DIR/$base"; then
      echo "CRD drift detected: $base differs between deploy/crd and chart/crds" >&2
      status=1
    fi
  else
    cp "$f" "$DEST_DIR/$base"
    echo "Synced $base"
  fi
done

if [[ $CHECK -eq 1 ]]; then
  if [[ $status -ne 0 ]]; then
    echo "Drift found. Run: hack/sync-crds.sh to update chart CRDs" >&2
  else
    echo "CRDs are in sync."
  fi
  exit $status
fi
