#!/bin/bash
# MCP Inspector E2E Test Script
# Tests all 3 MCP tools: get_current_weather, get_forecast, search_city

set -e

MCP_SERVER_URL="http://localhost:8095/mcp"
EVIDENCE_DIR=".omo/evidence"
SCRIPT_NAME="mcp-inspector-e2e"

mkdir -p "$EVIDENCE_DIR"

echo "=== MCP Inspector E2E Test ==="
echo "Server URL: $MCP_SERVER_URL"
echo "Evidence Dir: $EVIDENCE_DIR"
echo ""

# Check server
echo "Checking server..."
HEALTH=$(curl -s "$MCP_SERVER_URL/../actuator/health" 2>/dev/null || echo "")
if [ -z "$HEALTH" ]; then
    echo "Server not reachable"
    exit 1
fi
echo "Server is running: $HEALTH"
echo ""

# Test 1: get_current_weather
echo "Test 1: get_current_weather..."
curl -s -X POST "$MCP_SERVER_URL" \
    -H "Content-Type: application/json" \
    -H "MCP-Protocol-Version: 2024-11-05" \
    -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"get_current_weather","arguments":{"city":"Moscow"}}}' \
    | tee "$EVIDENCE_DIR/${SCRIPT_NAME}-get_current_weather.json"
echo ""

# Test 2: get_forecast
echo "Test 2: get_forecast..."
curl -s -X POST "$MCP_SERVER_URL" \
    -H "Content-Type: application/json" \
    -H "MCP-Protocol-Version: 2024-11-05" \
    -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"get_forecast","arguments":{"city":"Moscow","days":7}}}' \
    | tee "$EVIDENCE_DIR/${SCRIPT_NAME}-get_forecast.json"
echo ""

# Test 3: search_city
echo "Test 3: search_city..."
curl -s -X POST "$MCP_SERVER_URL" \
    -H "Content-Type: application/json" \
    -H "MCP-Protocol-Version: 2024-11-05" \
    -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"search_city","arguments":{"query":"Moscow"}}}' \
    | tee "$EVIDENCE_DIR/${SCRIPT_NAME}-search_city.json"
echo ""

echo "=== All Tests Completed ==="
echo "Evidence: $EVIDENCE_DIR/${SCRIPT_NAME}-*.json"
exit 0
