#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup script for Unix
# ----------------------------------------------------------------------------

# Resolve the location of this script.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"

# Maven wrapper properties
MVNW_REPOURL="${MAVEN_WRAPPER_REPO_URL:-https://repo.maven.apache.org/maven2}"
MVNW_VERSION="3.9.9"
MVNW_WRAPPER_DISTRIBUTOR="maven"

# Find Java
if [ -n "$JAVA_HOME" ] ; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA="java"
fi

# Read wrapper properties
if [ -r "$SCRIPT_DIR/.mvn/wrapper/maven-wrapper.properties" ]; then
    while IFS='=' read -r key value; do
        case "$key" in
            distributionUrl) DISTRIBUTION_URL="$value" ;;
            wrapperUrl) WRAPPER_URL="$value" ;;
            wrapperVersion) MVNW_VERSION="$value" ;;
        esac
    done < "$SCRIPT_DIR/.mvn/wrapper/maven-wrapper.properties"
fi

# Fallback distribution URL
DISTRIBUTION_URL="${DISTRIBUTION_URL:-${MVNW_REPOURL}/org/apache/maven/apache-maven/${MVNW_VERSION}/apache-maven-${MVNW_VERSION}-bin.zip}"

# Maven home
MAVEN_HOME="$SCRIPT_DIR/.mvn/wrapper/dists/apache-maven-${MVNW_VERSION}"

# Download Maven if not already present
if [ ! -d "$MAVEN_HOME" ]; then
    echo "Downloading Maven ${MVNW_VERSION}..."
    mkdir -p "$MAVEN_HOME"
    ZIP_FILE="$SCRIPT_DIR/.mvn/wrapper/dists/apache-maven-${MVNW_VERSION}.zip"
    mkdir -p "$(dirname "$ZIP_FILE")"

    if command -v curl > /dev/null 2>&1; then
        curl -fSL -o "$ZIP_FILE" "$DISTRIBUTION_URL"
    elif command -v wget > /dev/null 2>&1; then
        wget -O "$ZIP_FILE" "$DISTRIBUTION_URL"
    else
        echo "Error: curl or wget is required for downloading Maven."
        exit 1
    fi

    # Extract
    if command -v unzip > /dev/null 2>&1; then
        unzip -qo "$ZIP_FILE" -d "$MAVEN_HOME/.."
    elif command -v tar > /dev/null 2>&1; then
        tar xzf "$ZIP_FILE" -C "$MAVEN_HOME/.."
    fi

    rm -f "$ZIP_FILE"
fi

# Find mvn executable
MVN_CMD="$MAVEN_HOME/bin/mvn"

# Execute Maven
exec "$MVN_CMD" "$@"
