#!/usr/bin/env bash
set -e

echo '1) Direct User Service:'
curl -fsS http://localhost:8081/api/users/10
echo

echo '2) Gateway -> User Service:'
curl -fsS http://localhost:8080/api/users/10
echo

echo '3) Gateway -> Product Service:'
curl -fsS http://localhost:8080/api/products/10
echo

echo '4) Gateway -> Order Service:'
curl -fsS http://localhost:8080/api/orders/health
echo

echo 'Gateway smoke tests passed.'
