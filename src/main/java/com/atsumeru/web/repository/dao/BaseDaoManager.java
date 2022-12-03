package com.atsumeru.web.repository.dao;

import com.atsumeru.web.helper.OrmLiteUpgradeTable;
import com.atsumeru.web.model.database.DatabaseVersion;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.sqlite.SQLiteException;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BaseDaoManager implements Closeable {
    protected String databaseUrl;
    protected ConnectionSource connectionSource;

    public BaseDaoManager(String dbName) throws SQLException {
        databaseUrl = "jdbc:sqlite:" + dbName;
        connectionSource = new JdbcConnectionSource(databaseUrl);
    }

    public <T> void clearTable(Class<T> clazz) {
        try {
            TableUtils.clearTable(this.connectionSource, clazz);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            this.connectionSource.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected <T> void upgradeSchema(long schemaVersion, Dao<T, String> dao, Class<T> clazz) throws SQLException {
        if (isDatabaseObsolete(schemaVersion)) {
            try {
                OrmLiteUpgradeTable.migrateTable(connectionSource, clazz);
                dao.executeRawNoArgs("VACUUM");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    protected boolean isDatabaseObsolete(long schemaVersion) throws SQLException {
        TableUtils.createTableIfNotExists(connectionSource, DatabaseVersion.class);
        Dao<DatabaseVersion, Object> dbVersionDao = DaoManager.createDao(connectionSource, DatabaseVersion.class);

        List<DatabaseVersion> dbVersions = new ArrayList<>();
        QueryBuilder<DatabaseVersion, Object> queryBuilder = dbVersionDao.queryBuilder();
        try {
            queryBuilder.where().eq("VERSION", new SelectArg(schemaVersion));
            dbVersions = dbVersionDao.query(queryBuilder.prepare());
        } catch (SQLiteException ex) {
            System.err.println("DB column VERSION not found. Creating...");
        }

        if (dbVersions.isEmpty()) {
            DatabaseVersion version = new DatabaseVersion();
            version.setVersion(schemaVersion);
            dbVersionDao.create(version);
        }
        return dbVersions.isEmpty();
    }

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}