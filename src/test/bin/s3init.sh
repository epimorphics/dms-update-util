# Supporting script for TestFull
# Initializes s3 area
# Assumes running at root of git project

[[ $# = 1 ]] || { echo "usage: s3init s3base" 1>&2 ; exit 1 ; }

readonly S3BASE="$1"
readonly DATA=src/test/data

# upload root time file target
upload() {
    local root="$1"
    local t="12-00-$2-0000"
    local file="$3"
    local target="$4"
    aws s3 cp "$file" "$S3BASE/$root/$(date +%F)/$t/$target"
}

# op time op base extension
op() {
    local op="$2"
    local base="$3"
    local ext="$4"
    upload updates "$1" "$DATA/${base}${ext}" "${base}_${op}${ext}"
}

# opGraph time op base extension graph
opGraph() {
    local op="$2"
    local base="$3"
    local ext="$4"
    local graph="http:%2F%2Flocalhost%2Fgraph%2F$5"
    upload updates "$1" "$DATA/${base}${ext}" "${base}_${op}_${graph}${ext}"
}

upload images 01 $DATA/baseline_image.tgz baseline_image.tgz 
opGraph 02 replace r2    .ttl r2
opGraph 03 replace r2b   .ttl r2
opGraph 04 replace r3    .ttl r3
opGraph 05 drop    empty ""  r3  
opGraph 06 add     r4    .ttl.gz r4
