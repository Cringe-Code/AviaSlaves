#!/bin/bash
set -e

# Define key file names
PRIVATE_KEY1="jwt_key.pem"
PUBLIC_KEY1="jwt_public_key.pem"
PRIVATE_KEY2="jwt_key2.pem"
PUBLIC_KEY2="jwt_public_key2.pem"

# Remove existing keys if they exist
rm -f $PRIVATE_KEY1 $PUBLIC_KEY1 $PRIVATE_KEY2 $PUBLIC_KEY2

# Generate first RSA private key
openssl genpkey -algorithm RSA -out $PRIVATE_KEY1 -pkeyopt rsa_keygen_bits:2048 2>/dev/null

# Generate the corresponding public key
openssl rsa -pubout -in $PRIVATE_KEY1 -out $PUBLIC_KEY1 2>/dev/null

# Generate second RSA private key
openssl genpkey -algorithm RSA -out $PRIVATE_KEY2 -pkeyopt rsa_keygen_bits:2048 2>/dev/null

# Generate the corresponding public key
openssl rsa -pubout -in $PRIVATE_KEY2 -out $PUBLIC_KEY2 2>/dev/null

echo "Key rotation complete. New keys have been generated."