#!/bin/bash

set -e
trap 'kill 0' EXIT

PROJECT_ROOT=$(cd "$(dirname "$0")/../../../.." && pwd)
CLASS_PATH="$PROJECT_ROOT/target/classes"
CONFIG_FILE="network_config"
export PATH="$PATH:/c/Program Files (x86)/Nmap"

NUM_MEMBERS=9

PIDS=()

LaunchMembers() {
    local RELIABLE=${1:-0}
    local LATENT=${2:-0}
    local FAILURE=${3:-0}
    local EXCLUDE=("${!4}")
    local CURRENT=1

    for i in $(seq 1 $NUM_MEMBERS); do
        ID="M$CURRENT"
        if [[ " ${EXCLUDE[*]} " == *" $ID "* ]]; then
            CURRENT=$((CURRENT + 1))
            continue
        fi

        local PROFILE="STANDARD"
        if (( CURRENT <= RELIABLE )); then
            PROFILE="RELIABLE"
        elif (( CURRENT <= RELIABLE + LATENT )); then
            PROFILE="LATENT"
        elif (( CURRENT <= RELIABLE + LATENT + FAILURE )); then
            PROFILE="FAILURE"
        fi

        LaunchMember "$ID" "$PROFILE"
        CURRENT=$((CURRENT + 1))
    done
}

LaunchMember() {
    local ID=$1
    local PROFILE=$2
    java -cp "$CLASS_PATH" com.example.CouncilMember "$ID" "$PROFILE" "$CONFIG_FILE" >> "logs/$ID.log" 2>&1 &
    PIDS+=($!)
    sleep 0.1
}

LaunchProposer() {
    local ID=$1
    local PROFILE=$2
    local PROPOSE=$3
    echo "$PROPOSE" | java -cp "$CLASS_PATH" com.example.CouncilMember "$ID" "$PROFILE" "$CONFIG_FILE" >> "logs/$ID.log" 2>&1 &
    PIDS+=($!)
    sleep 0.1
}

ShutdownMembers() {
    for pid in "${PIDS[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null || true
        fi
    done
    PIDS=()
    sleep 1
}

WaitForConsensus(){
    TIMEOUT=$1
    VALUE=$2
    SECS=0
    echo "Waiting up to ${TIMEOUT}s for consensus on $VALUE..."
    while [ $SECS -lt $TIMEOUT ]; do
        if grep -q "elected Council President" logs/*.log 2>/dev/null; then
            echo "Consensus reached."
            return 0
        fi
        sleep 1
        SECS=$((SECS + 1))
    done
    echo "Timeout reached without consensus."
    return 1
}

echo
echo "===== Test 1: Ideal Network ====="
rm -rf logs && mkdir logs
PROPOSERS=("M5")
LaunchMembers 9 0 0 PROPOSERS[@]
sleep 2

for proposer in "${PROPOSERS[@]}"; do
    LaunchProposer "$proposer" "RELIABLE" "M4"
done

echo "Initiating election of M5"
sleep 2
WaitForConsensus 15 "M5"
ShutdownMembers


echo
echo "===== Test 2: Concurrent Proposals ====="
rm -rf logs && mkdir logs
PROPOSERS=("M1" "M8")
LaunchMembers 9 0 0 PROPOSERS[@]
sleep 2

for proposer in "${PROPOSERS[@]}"; do
    LaunchProposer "$proposer" "RELIABLE" "$proposer"
done
echo "Initiating election of M1 and M8"
WaitForConsensus 8 "M1 and M8"
ShutdownMembers


echo
echo "===== Test 3a: Fault Tolerance ====="
rm -rf logs && mkdir logs
PROPOSERS=("M4")
LaunchMembers 1 1 1 PROPOSERS[@]
sleep 2

for proposer in "${PROPOSERS[@]}"; do
    LaunchProposer "$proposer" "STANDARD" "$proposer"
done
echo "Initiating election of M4 with some failures"
WaitForConsensus 8 "M4"
ShutdownMembers


echo
echo "===== Test 3b: Fault Tolerance ====="
rm -rf logs && mkdir logs
PROPOSERS=("M2")
LaunchMembers 1 0 1 PROPOSERS[@]
sleep 2

for proposer in "${PROPOSERS[@]}"; do
    LaunchProposer "$proposer" "LATENT" "$proposer"
done
echo "Initiating election of M4 with some failures"
WaitForConsensus 25 "M4"
ShutdownMembers


echo
echo "===== Test 3c: Fault Tolerance ====="
rm -rf logs && mkdir logs
PROPOSERS=("M3")
LaunchMembers 1 1 0 PROPOSERS[@]
sleep 2

for proposer in "${PROPOSERS[@]}"; do
    LaunchProposer "$proposer" "FAILURE" "$proposer" &
    PROPOSER_PID=$!
done

sleep 2
kill "$PROPOSER_PID" 2>/dev/null || true
echo "Initiating election of M4 with some failures"
WaitForConsensus 8 "M4"
ShutdownMembers