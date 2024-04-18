#!/bin/bash

# Java version: 21

# This script runs the tests for the PINS24 project.
# The script takes a path to a test file or directory as an argument.
# If the path is a directory, the script runs all the test files in the directory.

# The script assumes that the test files are in the following format:
# - The test file has the extension .pins24
# - The output file has the same name as the test file with the extension _out.pins24

# Example of usage:
# ./run_tests.sh LexAn successful.pins24
# ./run_tests.sh LexAn lexan
# Warning: you have to run the script from the tests/ directory and tests/ directory should be in the root of the project
# Warning 2: in output files, make sure to add a newline at the end of the file


function run_test {
    echo "---> Running test: $2"

    # run java project in ../out catch error too
    java -p ../out/production/pins24 -m pins24/pins24.phase.$1 $2 > /tmp/test_output.pins24 2>&1
    diff -q /tmp/test_output.pins24 $3 > /dev/null

    # check if the test passed
    if [ $? -eq 0 ]; then
        echo "   ✓ Test passed"
    else
        echo "   ✕ Test failed"

        echo ""
        echo "   ✕ Actual output (left) and expected output (right):"
        diff -y $3 /tmp/test_output.pins24
        echo ""
    fi

    # remove the output file
    rm /tmp/test_output.pins24
}

function is_output_file {
    # check if file has _out.pins24 extension
    if [[ $1 == *_out.pins24 ]]; then
        return 0
    else
        return 1
    fi
}

function has_extension {
    # check if file has .pins24 extension
    if [[ $1 == *.pins24 ]]; then
        return 0
    else
        return 1
    fi
}

function check_build {
    # check if the project has been built
    if [ ! -d ../out ]; then
        echo "Error: Please build project with IntelliJ before running tests (make sure that out/ directory exists in project root)."
        exit 1
    fi

    # check if phase class is in ../out/production/pins24/pins24/phase
    if [ ! -f ../out/production/pins24/pins24/phase/$1.class ]; then
        echo "Error: $1.class not found in ../out/production/pins24/pins24/phase"
        exit 1
    fi
}

# check if the user has provided a path to the test file or directory and name of the phase to run
if [ "$#" -ne 2 ]; then
    echo "Usage: ./run_tests.sh <phase> <file or directory>"
    echo "Please provide a path to the test file or directory and name of the phase to run."
    exit 1
fi

# check if the path is a directory
if [ -d "$2" ]; then
    for file in "$2"/*; do
        if [ -f "$file" ]; then
            # check if the file is an output file
            if is_output_file "$file"; then
                continue
            fi

            # check if the output file exists
            output_file="${file%.*}_out.pins24"
            if [ ! -f "$output_file" ]; then
                echo "Error: $output_file not found."
                exit 1
            fi


            # run the test
            run_test "$1" "$file" "$output_file"
        fi
    done
elif [ -f "$2" ]; then # check if the path is a file
    # check if the file is an output file
    if is_output_file "$2"; then
        echo "Error: $2 is an output file. Please provide a test file."
        exit 1
    fi

    # check if the file has a .pins24 extension
    if ! has_extension "$2"; then
        echo "Error: $2 does not have a .pins24 extension."
        exit 1
    fi

    # check if the output file exists
    output_file="${2%.*}_out.pins24"
    if [ ! -f "$output_file" ]; then
        echo "Error: $output_file not found."
        exit 1
    fi


    # run the test
    run_test "$1" "$2" "$output_file"
else
    echo "Error: $2 is not a valid file or directory."
    exit 1
fi

