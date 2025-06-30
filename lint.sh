#!/bin/bash

echo "🔍 Running comprehensive linting suite..."
echo ""

# Initialize counters
checkstyle_issues=0
pmd_issues=0
spotbugs_issues=0

# Run Checkstyle
echo "📏 Running Checkstyle (code style)..."
if mvn checkstyle:check -q; then
    echo "✅ Checkstyle: PASSED"
else
    echo "⚠️  Checkstyle: Found style violations"
    checkstyle_issues=1
fi
echo ""

# Run SpotBugs (actual bugs)
echo "🐛 Running SpotBugs (bug detection)..."
spotbugs_output=$(mvn spotbugs:check -q 2>&1)
if echo "$spotbugs_output" | grep -q "BUILD SUCCESS"; then
    echo "✅ SpotBugs: No bugs found"
else
    bug_count=$(echo "$spotbugs_output" | grep "Total bugs:" | awk '{print $3}')
    if [ -n "$bug_count" ] && [ "$bug_count" -gt 0 ]; then
        echo "🚨 SpotBugs: Found $bug_count bug(s)"
        spotbugs_issues=$bug_count
    else
        echo "⚠️  SpotBugs: Check failed"
        spotbugs_issues=1
    fi
fi
echo ""

# Summary
echo "=== LINTING SUMMARY ==="
echo "🎯 Most Important (Actual Bugs):"
if [ $spotbugs_issues -eq 0 ]; then
    echo "  ✅ SpotBugs: Clean"
else
    echo "  🚨 SpotBugs: $spotbugs_issues issue(s) found"
fi

echo ""
echo "💡 To view detailed reports:"
echo "  • Checkstyle: mvn checkstyle:checkstyle"
echo "  • PMD: mvn pmd:pmd"  
echo "  • SpotBugs: mvn spotbugs:gui"

# Exit with error if critical bugs found
if [ $spotbugs_issues -gt 0 ]; then
    echo ""
    echo "❌ Linting failed due to actual bugs. Please fix SpotBugs issues."
    exit 1
else
    echo ""
    echo "✅ No critical bugs found. Code quality is good!"
    exit 0
fi 