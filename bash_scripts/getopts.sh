#!/bin/bash
while getopts u:d:p:f: option
do
case "${option}"
in
u) USER=${OPTARG};;
d) DATE=${OPTARG};;
p) PRODUCT=${OPTARG};;
f) FORMAT=${OPTARG};;
esac
done

echo "user: $USER";
echo "date: $DATE";
echo "product: $PRODUCT";
echo "format: $FORMAT";
