#!/bin/bash
# Library of utilities used in DMS update processing

readonly CONFIG=/opt/dms-update/config.json
readonly STATUS=/var/opt/dms-update/status.json
readonly LAST_EFFECTIVE=/var/opt/dms-update/lastEffectiveDate
readonly LOCK_FILE=/var/opt/dms-update/lock
readonly MAX_LOCK_WAIT=200

# Initial environment variables for the configuration data
S3ROOT=$( jq -r ".s3root" $CONFIG)
DSNAME=$( jq -r ".dsname"  $CONFIG)

# Report an error
#     report_error {error message}
report_error() {
    [[ $# = 1 ]] || { echo "Internal error calling report_error" 1>&2 ; exit 99 ; }
    echo '{ "success" : false, "message" : "'$1'" }' > $STATUS
    echo "Failed: $1"
    exit 1
}

# Report sucess
#    report_success
report_success() {
    echo '{ "success" : true, "date" : "'$(date +%FT%T)'", "effective_date" : "'$(cat $LAST_EFFECTIVE)'" }' > $STATUS
}

# Establish a lock so only one update runs at a time, maximum wait is ~3 minute
lock() {
    exec 200>$LOCK_FILE

    flock -n 200 -w $MAX_LOCK_WAIT && return 0 || report_error "Failed to lock, other update in progress?"
}
