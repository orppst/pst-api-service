# Released database upgrade

Back up the database, stop application writes and rehearse the upgrade on a
restored copy before deployment. `changelog.xml` is the production Liquibase
entry point; it includes the immutable `initial.sql` baseline followed by the
upgrade. The database must have the PostgreSQL `pg_sphere` extension installed
and the migration user needs permission to modify objects in `public`, `pdm`
and `coords`. Do not enable Hibernate schema generation in production.

An empty database can run the complete changelog. For an existing release-1.0
database, the baseline changeSets must already be recorded using the same
paths (`db/initial-changelog.xml` and `db/initial.sql`). If the database has
no Liquibase history, inspect its schema and drift first, then use Liquibase
`changelog-sync` **only with `db/initial-changelog.xml`** to record the
existing baseline. Never sync the root changelog, which would mark the upgrade
as executed without running it. Run `update-sql` and then `update` against
`db/changelog.xml`. Preserve the configured changelog paths across deployments.

The upgrade is transactional and intentionally fails on unsupported field or
observing-constraint subtypes, unrecognized coordinate units, reference frames,
epochs or invalid latitudes. It converts Julian epochs to MJD, interprets
timezone-less timestamps as UTC, and preserves original coordinate columns and
polarization values. Target ID 5 is swapped only when its legacy coordinates
match the reviewed values (-13.792, 274.688). Legacy polarization values are
mapped to `CIRCULAR`; telescope positions are marked `GEOCENTER`. Justification
text is retained at 20,000 characters, despite the dependency's 10,000-character
JPA column declaration. The wider review-comment column is also retained.
The custom pgSphere dialect translates application coordinates in degrees to
and from pgSphere's radians. Legacy columns and tables must not be dropped
until the migrated data has been independently verified.

For a disposable copy of the supplied release database with the baseline
recorded, the optional `MigratedDatabaseTest` runs Liquibase on application
startup and exercises Hibernate reads and writes without recreating the schema.
Set `PST_MIGRATION_TEST_JDBC_URL`, `PST_MIGRATION_TEST_USER` and
`PST_MIGRATION_TEST_PASSWORD`, then run Gradle with
`--tests org.orph2020.pst.apiimpl.entities.MigratedDatabaseTest`. The test
expects reviewed target ID 5 from that backup and rolls back its writes.

Rollback of this data conversion is by restoring the pre-upgrade backup. Do not
run old and new application versions concurrently against the migrated schema.
Check the migration, row counts, source values, coordinates, polarization,
timestamps, and observation relationships before allowing writes.


## Audit report on current database

The audit report provides a overview of the current database state, the migration
scripts have been created based on the audit.

The read-only audit can be run on the legacy database before migration:

```sh
psql -X -v ON_ERROR_STOP=1 "$DATABASE_URL" \
  -f src/main/resources/db/release-1.0-audit.sql
```

The connection must have read access to the `public`, `pdm`, and `coords`
schemas. The script runs in a read-only transaction and expects the released
schema; a missing table or column indicates schema drift and must be investigated.
The output contains database IDs and astronomical coordinates, so handle it
according to the database's data-sharing rules.

The audit reports legacy field and observing-constraint types, coordinate
systems and epochs, telescope reference systems, polarization values, long review
comments, and association-table counts. It does not modify or validate the
database, determine conversion rules, or migrate any data. In particular,
`initial.sql` has no latitude or longitude columns for `pdm.Point`, so point
positions may need to be recovered from another source.
