#!/bin/sh
if command -v mvn >/dev/null 2>&1; then exec mvn "$@"; fi
echo "Maven is not installed. Install Maven 3.6.3+ or add the standard Maven Wrapper files." >&2
exit 127
