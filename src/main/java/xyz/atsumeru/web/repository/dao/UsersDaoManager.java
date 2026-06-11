package xyz.atsumeru.web.repository.dao;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.table.TableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import xyz.atsumeru.web.manager.Workspace;
import xyz.atsumeru.web.model.database.AtsumeruUser;
import xyz.atsumeru.web.model.database.ShareToken;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

@Component
@DependsOn("workspace")
public class UsersDaoManager extends BaseDaoManager {
    public static final long DB_VERSION = 3;
    private static final Logger logger = LoggerFactory.getLogger(UsersDaoManager.class.getSimpleName());
    private static final String dbPath = new File(Workspace.DATABASES_DIR, "users.db").getAbsolutePath();

    private Dao<AtsumeruUser, String> usersDao;
    private Dao<ShareToken, String> shareTokenDao;

    public UsersDaoManager() throws SQLException {
        super(dbPath);
        try {
            usersDao = DaoManager.createDao(connectionSource, AtsumeruUser.class);
            TableUtils.createTableIfNotExists(connectionSource, AtsumeruUser.class);

            shareTokenDao = DaoManager.createDao(connectionSource, ShareToken.class);
            TableUtils.createTableIfNotExists(connectionSource, ShareToken.class);

            upgradeSchema(DB_VERSION, usersDao, AtsumeruUser.class);
            upgradeSchema(DB_VERSION, shareTokenDao, ShareToken.class);
            logger.info("Connected to users database!");
        } catch (Exception e) {
            logger.error("Failed to connect to users database!", e);
        }
    }

    public boolean save(AtsumeruUser item) {
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

    public AtsumeruUser query(long id) {
        try {
            return usersDao.queryForEq("id", id).get(0);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException ignored) {
        }
        return null;
    }

    public AtsumeruUser query(String userName) {
        try {
            return usersDao.queryForEq("USERNAME", userName).get(0);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException ignored) {
        }
        return null;
    }

    public List<AtsumeruUser> queryAll() {
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
            DeleteBuilder<AtsumeruUser, String> deleteBuilder = usersDao.deleteBuilder();
            deleteBuilder.where().eq(columnName, columnValue);
            return usersDao.delete(deleteBuilder.prepare()) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public ShareToken queryShareToken(String token) {
        try {
            return shareTokenDao.queryForEq("TOKEN", token).get(0);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException ignored) {
        }
        return null;
    }

    public boolean saveShareToken(ShareToken token) {
        try {
            return (token.getId() == null || token.getId() < 0
                    ? shareTokenDao.create(token)
                    : shareTokenDao.update(token)
            ) == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void deleteShareToken(ShareToken token) {
        try {
            shareTokenDao.delete(token);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<ShareToken> queryAllShareTokens() {
        try {
            return shareTokenDao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}