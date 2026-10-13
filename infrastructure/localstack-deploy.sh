#!/usr/bin/env bash
# Deploys the synthesized CDK template to LocalStack.
set -euo pipefail

usage() {
  cat <<EOF
Usage: $(basename "$0") [-h|--help]

Deletes any existing 'patient-management' stack in LocalStack, deploys the
template at cdk.out/localstack.template.json (relative to this script) and
prints the load balancer DNS name.

Prerequisites:
  - LocalStack running on \${LOCALSTACK_ENDPOINT:-http://localhost:4566}
  - Template synthesized first: ./mvnw compile exec:java

Environment:
  LOCALSTACK_ENDPOINT   LocalStack edge URL (default: http://localhost:4566)
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATE_FILE="${SCRIPT_DIR}/cdk.out/localstack.template.json"
ENDPOINT="${LOCALSTACK_ENDPOINT:-http://localhost:4566}"
STACK_NAME="patient-management"

if [[ ! -f "${TEMPLATE_FILE}" ]]; then
  echo "Template not found: ${TEMPLATE_FILE}" >&2
  echo "Run './mvnw compile exec:java' in ${SCRIPT_DIR} first." >&2
  usage >&2
  exit 1
fi

aws --endpoint-url="${ENDPOINT}" cloudformation delete-stack \
    --stack-name "${STACK_NAME}" \
  || echo "No existing '${STACK_NAME}' stack to delete; continuing."

aws --endpoint-url="${ENDPOINT}" cloudformation deploy \
    --stack-name "${STACK_NAME}" \
    --template-file "${TEMPLATE_FILE}"

aws --endpoint-url="${ENDPOINT}" elbv2 describe-load-balancers \
    --query "LoadBalancers[0].DNSName" --output text
