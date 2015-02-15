#!/bin/bash
# Library of utilities used in DMS update processing

. /opt/dms-update/bin/lib.sh

# Locate the updates that should be applied
find_updates() {
    local last_effective_date=$( cat $LAST_EFFECTIVE )

    if [[ $last_effective_date =~ ([0-9]+-[0-9]+-[0-9]+)-(.*) ]]; then
        local date=${BASH_REMATCH[1]}
        local time=${BASH_REMATCH[2]}

#        aws s3 ls $S3ROOT/updates/ \
#        | awk '$2 >= "'$date'" {print $2}' \
#        | xargs -I {} aws s3 ls --recursive $S3ROOT/updates/{} 

         cat temp.txt \
         | awk ' $4 >= "'$date'" {
            tmp=$4; 
            sub(/.*updates\//,"",tmp); 
            split(tmp, a, /\//);
            time=a[1]"-"a[2];
            op=a[3];
            sub(/^[^_]*_/,"",op)
            if (time >= "'$last_effective_date'") print time, op, $4;
            }'

#        | awk '{print $4}' \
#        | sed -e 's!.*/\([0-9-]*\)/\([0-9-]*\)/[^_/]*_\(.*\)!\1-\2 \3 &!' \
#        | awk '$1 >= "'$date'" {print $2, $3}'
    else
        report_error "Illegal format for last effective date: $last_effective_date"
    fi
}