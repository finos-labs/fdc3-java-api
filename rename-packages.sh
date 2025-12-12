#!/bin/bash
# Script to rename com.finos to org.finos in Java files

echo "=== Renaming com.finos to org.finos in Java files ==="

# Find all Java files and update package declarations and imports
find . -name "*.java" -type f | while read file; do
    if grep -q "com\.finos" "$file"; then
        echo "Updating: $file"
        sed -i '' 's/com\.finos/org.finos/g' "$file"
    fi
done

# Update pom.xml files
find . -name "pom.xml" -type f | while read file; do
    if grep -q "com\.finos" "$file"; then
        echo "Updating POM: $file"
        sed -i '' 's/com\.finos/org.finos/g' "$file"
    fi
done

echo "=== Done! ==="
