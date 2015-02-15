#!/bin/bash
# Library of utilities used in DMS update processing

. /opt/dms-update/bin/lib.sh

readonly TDBLOADER_MEM="-Xmx1024M"

# Initial environment variables for the configuration data
DBLOCATION=$( jq -r ".dblocation"  $CONFIG)
DBNAME=$( jq -r ".dbname"  $CONFIG)
FUSEKI_START=$( jq -r ".fuseki_start"  $CONFIG)
FUSEKI_STOP=$( jq -r ".fuseki_stop"  $CONFIG)
FUSEKI_JAR=$( jq -r ".fuseki_jar"  $CONFIG)

# Find the most recent available image or dump
latestImage() {
    aws s3 ls $S3ROOT/images/ | egrep "dump|image" | tail -1 | awk '{print $4}'
}

# Install an image from s3
install_image() {
    [[ $# = 1 ]] || { echo "Internal error called install_image" 1>&2 ; exit 99 ; }
    local image_object=$1
    if [[ $image_object =~ ([^_]*)_([^_]*)_([^_\.]*)(.*) ]]; then
        local date_stamp=${BASH_REMATCH[1]}
        local label=${BASH_REMATCH[2]}
        local op=${BASH_REMATCH[3]}
        local fmt=${BASH_REMATCH[4]}

        if [[ $op$fmt == "image.tgz" || $op$fmt == "dump.nq.gz" ]]; then
            echo "Installing $label $op from $date_stamp"
            aws s3 cp "$S3ROOT/images/$image_object" "$DBLOCATION" || report_error "Failed to load from S3"
            cd $DBLOCATION
            if [[ $op == "image" ]]; then
                tar xzf $image_object || report_error "Failed to unpack image"
            else
                rm -rf $DBNAME
                java $TDBLOADER_MEM -classpath $FUSEKI_JAR tdb.tdbloader --loc=$DBNAME $image_object || report_error "Failed to unpack dump"
            fi
            rm $image_object
            $FUSEKI_START || report_error "Failed to start fuseki"
            sleep 3   
            echo $date_stamp > $LAST_EFFECTIVE
        else
            report_error "Can't handle $op$fmt"
        fi
    else
        report_error "Image file name in wrong format: $image_object"
    fi
}

# Initialize baseline image on a new or reset server
install_latest_image() {
    local image=$(latestImage)
    if [[ -n $image ]]; then
        install_image $image
    else
        report_error "Could not find latest image"
    fi
}