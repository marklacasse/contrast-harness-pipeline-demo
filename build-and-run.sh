#!/bin/bash

# Build and Run Script for Vulnerable Demo Application

set -e

echo "🔧 Building Vulnerable Demo Application..."

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven first."
    exit 1
fi

# Clean and build
echo "📦 Running Maven build..."
mvn clean package -DskipTests

# Check if build was successful
if [ ! -f target/vulnerable-app-1.0.0.jar ]; then
    echo "❌ Build failed - JAR file not found"
    exit 1
fi

echo "✅ Build successful!"

# Ask user if they want to run the application
read -p "🚀 Do you want to run the application? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🏃 Starting application on http://localhost:8080"
    echo "⚠️  WARNING: This is an intentionally vulnerable application!"
    echo "    Use only in isolated testing environments."
    echo ""
    java -jar target/vulnerable-app-1.0.0.jar
fi
