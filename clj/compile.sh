#!/bin/sh

lein uberjar
mkdir -p ../core/libs/
cp -rf ./target/uberjar/clj-render-standalone.jar ../core/libs/
