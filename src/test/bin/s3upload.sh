# Supporting script for TestFull
# Single graph operation upload
# Assumes running at root of git project

[[ $# = 6 ]] || { echo "usage: s3upload s3base timecount op base extension graph" 1>&2 ; exit 1 ; }

readonly DATA=src/test/data

S3BASE="$1"
t="12-00-$2-0000"
op="$3"
base="$4"
ext="$5"
graph="http:%2F%2Flocalhost%2Fgraph%2F$6"
file="$DATA/${base}${ext}"
target="${base}_${op}_${graph}${ext}"
aws s3 cp "$file" "$S3BASE/updates/$(date +%F)/$t/$target"
