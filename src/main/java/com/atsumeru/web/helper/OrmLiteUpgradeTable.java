package com.atsumeru.web.helper;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableInfo;
import com.j256.ormlite.table.TableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

public class OrmLiteUpgradeTable {
    private static final Logger logger = LoggerFactory.getLogger(OrmLiteUpgradeTable.class.getSimpleName());
    private static final boolean verbose = false;

    /**
     * Handles adding/removing columns when a DAO Class changes.
     * Columns that are staying must keep the same name.
     */
    public static <T> void migrateTable(final ConnectionSource connectionSource, final Class<T> clazz) throws SQLException, IOException {
        Dao<T, Integer> dao = DaoManager.createDao(connectionSource, clazz);
        boolean alreadyExists = dao.isTableExists();

        log("migrateTable() dao<" + clazz.getSimpleName() + ">.isTableExists(): " + alreadyExists);

        String tableName = DatabaseTableConfig.extractTableName(connectionSource.getDatabaseType(), clazz);
        if (tableName != null && tableName.length() > 0 && !alreadyExists) {
            alreadyExists = isTableExists(connectionSource, tableName);
            log("migrateTable() table '" + tableName + "' already exists: " + alreadyExists);
        }

        if (alreadyExists) {
            //get columns from the old table - we don't have the Class that backs this any more so read from the table
            //probably a more efficient way than to execute a 'select *' and get the column names from the results
            QueryBuilder<?, Integer> qb = dao.queryBuilder();
            GenericRawResults<String[]> oldTableQueryResults = dao.queryRaw(qb.prepareStatementString());

            String[] oldColumns = oldTableQueryResults.getColumnNames();
            oldTableQueryResults.close();

            //define a temporary table name
            String tempTableName = (tableName + "_OLD").toUpperCase();

            try {
                if (isTableExists(connectionSource, tempTableName)) {
                    //remove old table as it already exists
                    dao.executeRaw("DROP TABLE " + tempTableName);
                }
            } catch (Throwable t) {
                logError("migrateTable() could not find or drop old table '" + tempTableName + "'", t);
            }

            //rename the table to the temporary table name
            log("migrateTable() renaming existing table '" + tableName + "' to old table '" + tempTableName + "'");
            dao.executeRaw("ALTER TABLE '" + tableName + "' RENAME TO '" + tempTableName + "';");

            log("migrateTable() renamed existing table to '" + tempTableName + "', creating new '" + tableName + "'...");
            //create the new table based on the Class
            TableUtils.createTable(connectionSource, clazz);

            //get TableInfo form the new class which gives us a convenient method .hasColumnName()
            TableInfo tableInfo = new TableInfo(connectionSource, (BaseDaoImpl) dao, clazz);

            log("migrateTable() created new table '" + tableName + "', building column lists to copy values from old (renamed) table...");

            //loop around both sets of columns and build up a String listing the common column names
            StringBuilder columnNamesString = new StringBuilder();

            boolean first = true;
            for (String col : oldColumns) {
                if (tableInfo.hasColumnName(col)) {
                    if (!first) {
                        columnNamesString.append(", ");
                    }
                    first = false;
                    columnNamesString.append(col);
                }
            }

            //build an SQL statement to copy data from matching columns from the old to new tables
            String statement = "INSERT INTO " + tableName + " (" + columnNamesString + ") SELECT " + columnNamesString + " FROM " + tempTableName;
            log("migrateTable() built table-copy statement: " + statement);

            dao.executeRaw(statement);
            log("migrateTable() table-copy statement executed, dropping old table '" + tempTableName + "'.");

            //now clean up by dropping the old table
            dao.executeRaw("DROP TABLE " + tempTableName);
        } else {
            TableUtils.createTable(connectionSource, clazz);
        }
    }

    private static boolean isTableExists(final ConnectionSource connectionSource, final String tableName) throws SQLException {
        boolean alreadyExists = connectionSource.getReadOnlyConnection(tableName).isTableExists(tableName);

        if (!alreadyExists) {
            alreadyExists = connectionSource.getReadOnlyConnection(tableName).isTableExists(tableName.toUpperCase());
        }
        if (!alreadyExists) {
            alreadyExists = connectionSource.getReadOnlyConnection(tableName).isTableExists(tableName.toLowerCase());
        }

        return (alreadyExists);

    }

    private static void log(String message) {
        if (verbose) {
            logger.info(message);
        }
    }

    private static void logError(String message, Throwable throwable) {
        logger.error(message, throwable);
    }
}