# dms-update-util
Simple utilities to help with data updates within DMS systems

## Functionality

Allows the state of a replicated set of data servers to be held in S3. Existing servers can catch up to get by replaying updates from the S3 state. New servers can initialize their database from the S3 state.

The S3 state is not necessiarly a complete historical record. Old updates may be garbage collected.

## S3 layout

Issue that S3 commands can't easily list more than 1000 objects (and provide only very limited patterns for filtering). We have systems that will exceed that limit in a few days. So we break the "folder" structure down fine grained.

    images/
        {date}-{time}_{label}_{op}.{format}
        {name}.opt                   # optional, for use when rebuilding from dump
        config.ttl                   # future text index support
    updates
        {date}/
            {time}/
                {label}_{op}[_{arg}].{format}

Where

   * `{label}` is an arbitrary identifier for the supplying system, used to avoid clashes in publishing the update files, must not contain "_" or "/".

   * `{date}` is `YYYY-MM-DD`

   * `{time}` is `hh-mm-ss-ns`

   * `{op}` is one of
       * image
       * dump
       * add {graph}
       * replace {graph}
       * drop {graph}
       * update
       * postproc {component-name}

   * `{arg}` is optional graph URI with "/" and "." escaped by %2F and %2E. For `postproc` the arg is a (data component) name used as a key for filtering our redudant postprocs

   * `{format}` is `tgz` (images), `nq.gz` (dumps), `ttl` or `ttl.gz` (add/replace), `ru` or `ru.gz` (updates)

## Operations 

Server

   * create database from current state
   * update database from current state
   * report status

DMS

   * trigger and wait for update on all data servers
   * record external publication event (source, user, time, S3 URL)

Other

   * external publication (upload to S3, trigger servers, notify DMS (via queue))
   * build clean base image from current state (external worker, request via queue)
   * GC old records

## Server structure

/opt/dms-update/config.json

   * S3 location to load from
   * database location
   * endpoint address

/var/opt/dms-update/status.json

   * last update succeeded
   * effective time of last update (most recent timestamp)
   * clock time of last update

/opt/dms-update/bin/

    update  (returns status.json)
    rebuild
