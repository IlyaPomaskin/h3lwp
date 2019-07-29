#!/bin/sh

# lein uberjar
# mkdir -p ../core/libs/
# cp -rf ./target/uberjar/clj-render-standalone.jar ../core/libs/

# lein compile
# mkdir -p ../core/classes/
# cp -rf ./target/uberjar/classes/ ../core/classes/

lein jar
mkdir -p ../core/libs/
cp -rf ./target/clj-0.1.0-SNAPSHOT.jar ../core/libs/clj.jar
