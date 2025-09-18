# dms-update-util

Utility to help with data updates within DMS systems.

## Changes

| Version | Comment                                   |
|---------|-------------------------------------------|
| 1.0.4   | Stable version for legacy DMS deployments |
| 1.0.6   | Update AWS SDK for IMDSv2 compatibility   |
| 1.0.8   | Update AWS SDK for EPIA compatibility     |
| 1.0.10  | Suppress stats.opt warnings               |
| 1.1.0   | AWS SDK2, logback instead of log4j        |

## Functionality

Allows the state of a replicated set of data servers to be held in S3. Existing servers can catch up to date by replaying updates from the S3 state. New servers can initialize their database from the S3 state.

The S3 state is not necessarily a complete historical record, old updates may be garbage collected, it is simply enough to rebuild the current state.

## S3 layout


    images/
        {date}/{time}/{label}_{op}.{format}
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
       * drop {graph} (no format)
       * update
       * postproc {component-name}

   * `{arg}` is optional graph URI with "/" and "." escaped by %2F and %2E. For `postproc` the arg is a (data component) name used as a key for filtering out redundant postprocs

   * `{format}` is `tgz` (images), `nq.gz` (dumps), `ttl` or `ttl.gz` (add/replace), `ru` or `ru.gz` (updates), missing for drop.

The extreme folder structure with the date/time split is to enable scripts to recursively list the directories without falling foul of the S3 limit on object listing (1000 elements).

## Operations

Updating or reloading a server is accomplished using the dms-update java utility.

    dms-update --plan|--perform

The `--plan` option consults the current server status and the S3 state and generates a plan for what updates to perform, one operation per line. The plan will be pruned to eliminate redundant graph replace or postprocessing operations.

The `--perform` option generates and then executes the plan. For normal update operations this assumes that the fuseki service is running and the java utility streams the update commands direct from S3 to this service. If there is no database present then it will be initialized from the most recent image or dump file. These operations are implemented using the `install_image` and `install_dump` shell scripts in `/opt/dms-update/bin`. These in turn start the fuseki server once the bootstrap database is in place using the `start_fuseki` script in the same place. These scripts can be customized for particular installations. In particular the `start_fuseki` script will normally need replacing and the `install_dump` script makes the assumption that the fuseki jar can be found in `/usr/share/fuski` (which it uses to run tdbloader).

## Configuration and status files

`/opt/dms-update/config.json` defines the server configuration, for example:

    {
      "s3root" : "s3://dms-deploy/images/test",
      "dblocation" : "/tmp",
      "dbname" : "DS-DB",
      "service" : "http://localhost:3030/ds/"
    }

`/var/opt/dms-update/status.json` contains the date-time stamp for the most recent successful update plus a list of recent updates.
