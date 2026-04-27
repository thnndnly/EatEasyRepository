#!/usr/bin/env bash
# Generiert das RSA-Keypaar fuer die JWT-Signierung im Backend.
# Schluessel werden NICHT eingecheckt (.gitignore *.pem) und muessen lokal
# pro Entwickler erzeugt werden.

set -euo pipefail

DEST="$(cd "$(dirname "$0")/.." && pwd)/backend/src/main/resources"

mkdir -p "$DEST"

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
    -out "$DEST/privateKey.pem"
openssl rsa -in "$DEST/privateKey.pem" -pubout -out "$DEST/publicKey.pem"

echo "JWT keys written to: $DEST"
