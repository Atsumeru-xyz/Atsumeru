package com.atsumeru.web.repository.dao;

import com.atsumeru.web.model.database.User;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.List;

public class UsersDaoManager extends BaseDaoManager {
    public static final long DB_VERSION = 1;
    private final Dao<User, String> usersDao;

    public UsersDaoManager(String dbname) throws SQLException {
        super(dbname);
        usersDao = DaoManager.createDao(connectionSource, User.class);
        TableUtils.createTableIfNotExists(connectionSource, User.class);
        upgradeSchema(DB_VERSION, usersDao, User.class);
    }

    public boolean save(User item) {
        try {
            return (
                    item.getId() == null || item.getId() < 0
                            ? usersDao.create(item)
                            : usersDao.update(item)
            ) == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public User query(long id) {
        try {
            return usersDao.queryForEq("id", id).get(0);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException ignored) {
        }
        return null;
    }

    public User query(String userName) {
        try {
            return usersDao.queryForEq("USERNAME", userName).get(0);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException ignored) {
        }
        return null;
    }

    public List<User> queryAll() {
        try {
            return usersDao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteById(long id) {
        return deleteByColumnEq("id", String.valueOf(id));
    }

    public boolean deleteByColumnEq(String columnName, String columnValue) {
        try {
            DeleteBuilder<User, String> deleteBuilder = usersDao.deleteBuilder();
            deleteBuilder.where().eq(columnName, columnValue);
            return usersDao.delete(deleteBuilder.prepare()) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}