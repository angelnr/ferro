#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"
require_docker
run_backend mvn -Ppit -DskipITs test-compile org.pitest:pitest-maven:mutationCoverage
