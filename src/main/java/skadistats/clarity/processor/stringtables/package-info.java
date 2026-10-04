/**
 * String tables and player info.
 *
 * <p>{@link skadistats.clarity.processor.stringtables.OnStringTableCreated},
 * {@link skadistats.clarity.processor.stringtables.OnStringTableEntry} (per table name) and
 * {@link skadistats.clarity.processor.stringtables.OnStringTableClear} report string table changes. Declare
 * {@link skadistats.clarity.processor.stringtables.UsesStringTable} with a table name to make that table available
 * through {@link skadistats.clarity.processor.stringtables.StringTables#forName(String)}; tables nobody requested
 * are not tracked. For CS:GO and CS2, {@link skadistats.clarity.processor.stringtables.OnPlayerInfo} (with
 * {@link skadistats.clarity.processor.stringtables.UsesPlayerInfo}) reports the contents of the {@code "userinfo"}
 * table. The emitter classes are internals.
 */
package skadistats.clarity.processor.stringtables;
