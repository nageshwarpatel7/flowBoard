#!/bin/bash
TOKEN="squ_adab6eaf83f6a716d04eb80739f41a6e7f2ae8da"
SONAR_URL="http://localhost:9000"
SERVICES=(auth-service workspace-service board-service list-service card-service notification-service payment-service)

# ── Wait for SonarQube to be truly ready (not just port-open) ────────────────
wait_for_sonar() {
    echo "Waiting for SonarQube to be ready..."
    local max_attempts=40   # 40 x 15s = 10 minutes max
    local attempt=0
    while [ $attempt -lt $max_attempts ]; do
        local status
        status=$(curl -s --connect-timeout 5 --max-time 10 \
            -u admin:admin \
            "${SONAR_URL}/api/system/status" 2>/dev/null | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$status" = "UP" ]; then
            echo "SonarQube is UP and ready!"
            return 0
        fi
        attempt=$((attempt + 1))
        echo "  SonarQube status: '${status:-unreachable}' — retrying in 15s (attempt $attempt/$max_attempts)..."
        sleep 15
    done
    echo "WARNING: SonarQube did not become ready in time. Sonar uploads will be skipped."
    return 1
}

# Check once — if it's already up, great. If not, wait.
SONAR_READY=false
raw_status=$(curl -s --connect-timeout 3 --max-time 6 \
    -u admin:admin \
    "${SONAR_URL}/api/system/status" 2>/dev/null | grep -o '"status":"[^"]*"' | cut -d'"' -f4)

if [ "$raw_status" = "UP" ]; then
    echo "SonarQube is already UP."
    SONAR_READY=true
else
    if wait_for_sonar; then
        SONAR_READY=true
    else
        echo "SonarQube is not running or failed to start — Sonar uploads will be skipped."
    fi
fi

# ── Run each service ──────────────────────────────────────────────────────────
FAILED_SERVICES=()
PASSED_SERVICES=()

for SVC in "${SERVICES[@]}"; do
    echo ""
    echo "════════════════════════════════════════"
    echo "=== Testing $SVC ==="
    echo "════════════════════════════════════════"
    cd "$SVC" || { echo "FAILED: $SVC (directory not found)"; FAILED_SERVICES+=("$SVC"); continue; }

    # Step 1: tests + coverage check (ignoring coverage failures)
    mvn clean verify -q -Djacoco.haltOnFailure=false 2>&1
    STATUS=$?

    if [ $STATUS -ne 0 ]; then
        echo "FAILED: $SVC (tests failed)"
        FAILED_SERVICES+=("$SVC")
        cd ..
        continue
    fi

    echo "PASSED: $SVC"
    PASSED_SERVICES+=("$SVC")

    # Step 2: Sonar upload (only if Sonar is ready)
    if [ "$SONAR_READY" = true ]; then
        # Small delay to let the server breathe
        sleep 3
        mvn sonar:sonar \
            -Dsonar.token="$TOKEN" \
            -Dsonar.host.url="$SONAR_URL" \
            -Dsonar.ws.timeout=300 \
            -q 2>&1
        if [ $? -eq 0 ]; then
            echo "  → Sonar upload OK: $SVC"
        else
            echo "  → Sonar upload FAILED (non-fatal): $SVC"
        fi
    else
        echo "  → Sonar upload SKIPPED (server not ready): $SVC"
    fi

    cd ..
done

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════"
echo "SUMMARY"
echo "════════════════════════════════════════"
echo "PASSED (${#PASSED_SERVICES[@]}): ${PASSED_SERVICES[*]}"
echo "FAILED (${#FAILED_SERVICES[@]}): ${FAILED_SERVICES[*]:-none}"
if [ "$SONAR_READY" = true ]; then
    echo "SonarQube dashboard: $SONAR_URL"
fi