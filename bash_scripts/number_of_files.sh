#!/bin/bash

#let's define two variables
NUMBER=$(ls -l | wc -l)
MAX=100

echo "${NUMBER} files in this directory!"

if [[ $NUMBER<$MAX ]]
then
  echo "$NUMBER is less than $MAX";
  exit 0;
else
  echo "$NUMBER is more than $MAX";
  exit 1;
fi

