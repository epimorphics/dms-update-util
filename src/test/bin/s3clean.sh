# Supporting script for TestFull
# Clears out an S3 test area

[[ $# = 1 ]] || { echo "usage: s3clean s3base" 1>&2 ; exit 1 ; }

readonly S3BASE="$1"

aws s3 rm $S3BASE --recursive
