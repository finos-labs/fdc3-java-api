#!/bin/bash
# Script to rename directory structure from com/finos to org/finos

echo "=== Renaming directory structure com/finos to org/finos ==="

# Find all com/finos directories and rename them
find . -type d -path "*/com/finos" | while read dir; do
    parent=$(dirname "$dir")
    org_dir="$parent/org"
    
    echo "Processing: $dir"
    
    # Create org directory if it doesn't exist
    mkdir -p "$org_dir"
    
    # Move finos directory from com to org
    if [ -d "$dir" ]; then
        mv "$dir" "$org_dir/"
        echo "  Moved to: $org_dir/finos"
    fi
    
    # Remove empty com directory
    com_dir="$parent/com"
    if [ -d "$com_dir" ] && [ -z "$(ls -A "$com_dir")" ]; then
        rmdir "$com_dir"
        echo "  Removed empty: $com_dir"
    fi
done

echo "=== Done! ==="
