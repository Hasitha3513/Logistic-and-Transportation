#!/usr/bin/env bash

set -e

echo "=== Transport & Logistics Static Analysis ==="

echo "1. Compile + Tests"
mvn clean verify

echo "2. SpotBugs"
mvn spotbugs:check

echo "3. PMD"
mvn pmd:check

echo "4. Checkstyle"
mvn checkstyle:check

echo "5. OWASP Dependency Check"
mvn org.owasp:dependency-check-maven:check

echo "=== STATIC ANALYSIS PASSED ==="