#!/usr/bin/env bash
set -euo pipefail
echo "Downloading NVD database (first run ~20 min, subsequent ~30s)..."
mvn dependency-check:update-only --no-transfer-progress
echo "Done. OWASP DC database is ready."
