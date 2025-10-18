#!/bin/bash

set -e
trap 'kill 0' EXIT

PROJECT_ROOT=$(cd "$(dirname "$0")/../../../.." && pwd)
CLASS_PATH="$PROJECT_ROOT/target/classes"
CONFIG_FILE="network_config"

NUM_MEMBERS=9

PIDS=()

LaunchMembers() {
    local RELIABLE=${1:-0}
    local LATENT=${2:-0}
    local FAILURE=${3:-0}
    CURRENT=1

    for i in $(seq 1 $RELIABLE); do
        ID="M$CURRENT"
        echo "Class path: $CLASS_PATH"
        java -cp "$CLASS_PATH" com.example.CouncilMember "$ID" RELIABLE "$CONFIG_FILE" >> "logs/$ID.log" 2>&1 &
        PIDS+=($!)
        CURRENT=$((CURRENT + 1))
        sleep 0.1
    done
    for i in $(seq 1 $LATENT); do
        ID="M$CURRENT"
        java -cp "$CLASS_PATH" com.example.CouncilMember "$ID" LATENT "$CONFIG_FILE" >> "logs/$ID.log" 2>&1 &
        PIDS+=($!)
        CURRENT=$((CURRENT + 1))
        sleep 0.1
    done
    for i in $(seq 1 $FAILURE); do
        ID="M$CURRENT"
        java -cp "$CLASS_PATH" com.example.CouncilMember "$ID" FAILURE "$CONFIG_FILE" >> "logs/$ID.log" 2>&1 &
        PIDS+=($!)
        CURRENT=$((CURRENT + 1))
        sleep 0.1
    done
    while [ $CURRENT -le $NUM_MEMBERS ]; do
        ID="M$CURRENT"
        java -cp "$CLASS_PATH" com.example.CouncilMember "$ID" STANDARD "$CONFIG_FILE" >> "logs/$ID.log" 2>&1 &
        PIDS+=($!)
        CURRENT=$((CURRENT + 1))
        sleep 0.1
    done
    sleep 1
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
        if grep -q "Consensus Result" logs/*.log 2>/dev/null; then
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
LaunchMembers 9
sleep 2
echo "Initiating election of M5"
java -cp "$CLASS_PATH" com.example.CouncilMember M5 RELIABLE "$CONFIG_FILE" >> logs/test1.log 2>&1 || true
WaitForConsensus 15 "M5"
ShutdownMembers


echo
echo "===== Test 2: Concurrent Proposals ====="
rm -rf logs && mkdir logs
LaunchMembers 9
sleep 2
echo "Initiating election of M1 and M8"
java -cp "$CLASS_PATH" com.example.CouncilMember M1 RELIABLE "$CONFIG_FILE" >> logs/test2.log 2>&1 || true
java -cp "$CLASS_PATH" com.example.CouncilMember M8 RELIABLE "$CONFIG_FILE" >> logs/test2.log 2>&1 || true
WaitForConsensus 8 "M1 and M8"
ShutdownMembers


echo
echo "===== Test 3a: Fault Tolerance ====="
rm -rf logs && mkdir logs
LaunchMembers 1 1 1
sleep 2
echo "Initiating election of M4 with some failures"
java -cp "$CLASS_PATH" com.example.CouncilMember M4 STANDARD "$CONFIG_FILE" >> logs/test3.log 2>&1 || true
WaitForConsensus 8 "M4"
ShutdownMembers