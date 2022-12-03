package com.atsumeru.web.repository.dao;

import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.BookSerie;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.util.GUString;
import com.atsumeru.web.enums.ContentType;
import com.atsumeru.web.enums.LibraryPresentation;
import com.atsumeru.web.helper.OrmLiteUpgradeTable;
import com.atsumeru.web.model.book.chapter.BookChapter;
import com.atsumeru.web.model.database.Category;
import com.atsumeru.web.model.database.History;
import com.atsumeru.web.util.GUArray;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.TableUtils;
import kotlin.NotImplementedError;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class BooksDaoManager extends BaseDaoManager {
    public static final long DB_VERSION = 1;
    private static final Logger logger = LoggerFactory.getLogger(BooksDaoManager.class.getSimpleName());

    private static final String REMOVED_FIELD_NAME = "REMOVED";
    private static final String IS_SINGLE_FIELD_NAME = "IS_SINGLE";
    private static final String VACUUM_STATEMENT = "VACUUM";

    @Getter
    private final Dao<BookArchive, String> archivesDao;
    private final Dao<BookSerie, String> seriesDao;
    @Getter
    private final Dao<BookChapter, String> chaptersDao;
    private final Dao<Category, String> categoryDao;
    private final Dao<History, String> historyDao;

    @SuppressWarnings("rawtypes")
    private final Map<Dao<?, String>, Class> daoMap = new HashMap<>();

    @Getter
    private final HistoryDao HistoryDao;

    public BooksDaoManager(String dbname) throws IOException, SQLException {
        super(dbname);

        daoMap.put(archivesDao = createDao(BookArchive.class), BookArchive.class);
        daoMap.put(seriesDao = createDao(BookSerie.class), BookSerie.class);
        daoMap.put(chaptersDao = createDao(BookChapter.class), BookChapter.class);
        daoMap.put(categoryDao = createDao(Category.class), Category.class);
        daoMap.put(historyDao = createDao(History.class), History.class);

        HistoryDao = new HistoryDao();

        migrateSchema();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void migrateSchema() throws IOException, SQLException {
        if (isDatabaseObsolete(DB_VERSION)) {
            for (Map.Entry<Dao<?, String>, Class> entry : daoMap.entrySet()) {
                migrateTable(entry.getKey(), entry.getValue());
            }

            fixIsSerieList();
        }
    }

    @SuppressWarnings("rawtypes")
    public void commit() {
        for (Map.Entry<Dao<?, String>, Class> entry : daoMap.entrySet()) {
            commit(entry.getKey());
        }
    }

    @SuppressWarnings("rawtypes")
    public void setAutoCommit(boolean autoCommit) {
        for (Map.Entry<Dao<?, String>, Class> entry : daoMap.entrySet()) {
            setAutoCommit(entry.getKey(), autoCommit);
        }
    }

    @SuppressWarnings("rawtypes")
    public void vacuum() {
        for (Map.Entry<Dao<?, String>, Class> entry : daoMap.entrySet()) {
            vacuum(entry.getKey());
        }
    }

    private <T> void commit(Dao<T, String> dao) {
        try {
            dao.commit(connectionSource.getReadWriteConnection(dao.getTableName()));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private <T> void setAutoCommit(Dao<T, String> dao, boolean autoCommit) {
        try {
            dao.setAutoCommit(connectionSource.getReadWriteConnection(dao.getTableName()), autoCommit);
        } catch (SQLException ignored) {
        }
    }

    private <T> void vacuum(Dao<T, String> dao) {
        try {
            dao.executeRawNoArgs(VACUUM_STATEMENT);
        } catch (SQLException ex) {
            logger.warn("Unable to execute VACUUM on database");
        }
    }

    private synchronized static <T> int createOrUpdate(Dao<T, String> dao, T item, boolean isUpdate) {
        try {
            return isUpdate
                    ? dao.update(item)
                    : dao.create(item);
        } catch (SQLException ex) {
            return -1;
        }
    }

    private <T> Dao<T, String> createDao(Class<T> clazz) throws SQLException {
        Dao<T, String> dao = DaoManager.createDao(connectionSource, clazz);
        TableUtils.createTableIfNotExists(connectionSource, clazz);
        dao.queryRaw("PRAGMA journal_mode=WAL;").getResults();
        return dao;
    }

    private <T> void migrateTable(Dao<T, String> dao, Class<T> clazz) throws IOException, SQLException {
        OrmLiteUpgradeTable.migrateTable(connectionSource, clazz);
        vacuum(dao);
    }

    @SuppressWarnings("unchecked")
    private <T> Dao<T, String> getDao(Class<T> clazz) {
        if (clazz.isAssignableFrom(BookSerie.class)) {
            return (Dao<T, String>) seriesDao;
        } else if (clazz.isAssignableFrom(BookArchive.class)) {
            return (Dao<T, String>) archivesDao;
        } else if (clazz.isAssignableFrom(BookChapter.class)) {
            return (Dao<T, String>) chaptersDao;
        } else if (clazz.isAssignableFrom(Category.class)) {
            return (Dao<T, String>) categoryDao;
        } else if (clazz.isAssignableFrom(History.class)) {
            return (Dao<T, String>) historyDao;
        }
        throw new NotImplementedError();
    }

    private void fixIsSerieList() throws SQLException {
        seriesDao.queryBuilder()
                .where()
                .isNull(IS_SINGLE_FIELD_NAME)
                .query().forEach(it -> {
            it.setIsSingle(false);
            save(it);
        });
    }

    public <T> boolean refresh(T item, Class<T> clazz) {
        try {
            return getDao(clazz).refresh(item) == 1;
        } catch (SQLException ex) {
            return false;
        }
    }

    public synchronized boolean save(BookSerie item) {
        return createOrUpdate(seriesDao, item, item.getDbId() != null) > 0;
    }

    public synchronized <T> boolean save(T item) {
        if (item instanceof BookSerie) {
            return createOrUpdate(seriesDao, (BookSerie) item, ((BookSerie) item).getDbId() != null) > 0;
        } else if (item instanceof BookArchive) {
            return createOrUpdate(archivesDao, (BookArchive) item, ((BookArchive) item).getDbId() != null) > 0;
        } else if (item instanceof BookChapter) {
            return createOrUpdate(chaptersDao, (BookChapter) item, ((BookChapter) item).getId() != null) > 0;
        } else if (item instanceof Category) {
            return createOrUpdate(categoryDao, (Category) item, ((Category) item).getId() != null) > 0;
        } else {
            throw new IllegalArgumentException("Instance type in save() method not supported");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> QueryBuilder<T, String> getQueryBuilder(Class<T> clazz) {
        if (clazz.isAssignableFrom(BookArchive.class)) {
            return (QueryBuilder<T, String>) archivesDao.queryBuilder();
        } else if (clazz.isAssignableFrom(BookSerie.class)) {
            return (QueryBuilder<T, String>) seriesDao.queryBuilder();
        } else if (clazz.isAssignableFrom(BookChapter.class)) {
            return (QueryBuilder<T, String>) chaptersDao.queryBuilder();
        } else if (clazz.isAssignableFrom(Category.class)) {
            return (QueryBuilder<T, String>) categoryDao.queryBuilder();
        }
        return null;
    }

    //**********************//
    //        Count         //
    //**********************//
    public <T> Long count(Class<T> clazz) {
        try {
            QueryBuilder<T, String> queryBuilder = getQueryBuilder(clazz);
            queryBuilder.setCountOf(true);
            queryBuilder.setWhere(queryBuilder.where().eq(REMOVED_FIELD_NAME, false));
            return countOf(queryBuilder, clazz);
        } catch (Exception ignored) {
            QueryBuilder<T, String> queryBuilder = getQueryBuilder(clazz);
            queryBuilder.setCountOf(true);
            return countOf(queryBuilder, clazz);
        }
    }

    public <T> Long countForCategory(Class<T> clazz, String dbCategoryId, ContentType contentType) {
        QueryBuilder<?, String> queryBuilder;
        if (clazz.isAssignableFrom(BookArchive.class)) {
            queryBuilder = archivesDao.queryBuilder();
        } else if (clazz.isAssignableFrom(BookSerie.class)) {
            queryBuilder = seriesDao.queryBuilder();
        } else {
            throw new NotImplementedError();
        }

        try {
            Where<?, String> where = queryBuilder.where();
            if (GUString.isNotEmpty(dbCategoryId)) {
                where.like("CATEGORIES", "%" + dbCategoryId + "%");
            } else if (contentType != null) {
                where.isNull("CATEGORIES").and().eq("CONTENT_TYPE", contentType.name());
            }

            return where.and().ne("REMOVED", true).countOf();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return -1L;
        }
    }

    public <T> Long countForContentType(ContentType contentType, Class<T> clazz) {
        return count("CONTENT_TYPE", contentType.name(), clazz);
    }

    public <T> Long count(String columnName, String value, Class<T> clazz) {
        try {
            QueryBuilder<T, String> queryBuilder = getQueryBuilder(clazz);
            queryBuilder.setCountOf(true);
            Where<T, String> where = queryBuilder.where().eq(columnName, value);
            if (!clazz.isAssignableFrom(BookChapter.class)) {
                where = where.and().eq(REMOVED_FIELD_NAME, false);
            }
            queryBuilder.setWhere(where);
            return countOf(queryBuilder, clazz);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> Long countLike(String columnName, String columnValue, Class<T> clazz, LibraryPresentation libraryPresentation) {
        try {
            QueryBuilder<T, String> queryBuilder = getQueryBuilder(clazz);
            queryBuilder.setCountOf(true);

            if (clazz.isAssignableFrom(BookArchive.class)) {
                queryBuilder.setWhere(queryBuilder.where().like(columnName, "%" + columnValue + "%"));
            } else if (clazz.isAssignableFrom(BookSerie.class)) {
                queryBuilder.setWhere(queryBuilder.where()
                        .like(columnName, "%" + columnValue + "%")
                        .and()
                        .eq("IS_SINGLE", libraryPresentation.isSinglesPresentation()));
            }

            return countOf(queryBuilder, clazz);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> Long countNotEmpty(String columnName, Class<T> clazz) {
        try {
            if (clazz.isAssignableFrom(BookSerie.class)) {
                QueryBuilder<BookSerie, String> queryBuilder = seriesDao.queryBuilder();
                queryBuilder.setCountOf(true);
                queryBuilder.setWhere(queryBuilder.where()
                        .isNotNull(columnName)
                        .and()
                        .ne(columnName, "")
                        .and()
                        .eq(REMOVED_FIELD_NAME, false));
                return seriesDao.countOf(queryBuilder.prepare());
            } else if (clazz.isAssignableFrom(BookArchive.class)) {
                return 0L;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Long countOf(QueryBuilder queryBuilder, Class<T> clazz) {
        try {
            return getDao(clazz).countOf(queryBuilder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //**********************//
    //        Query         //
    //**********************//
    @SuppressWarnings("unchecked")
    public <T, C extends IBaseBookItem> List<C> queryBooks(List<String> ids, Class<T> clazz) {
        if (clazz.isAssignableFrom(BookArchive.class)) {
            return (List<C>) queryArchives(ids);
        } else if (clazz.isAssignableFrom(BookSerie.class)) {
            return (List<C>) querySeries(ids);
        }
        return new ArrayList<>();
    }

    public List<BookArchive> queryArchives(List<String> archiveIds) {
        try {
            return archivesDao.queryBuilder().where().in("MANGA_ID", archiveIds).query();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<BookSerie> querySeries(List<String> serieIds) {
        try {
            return seriesDao.queryBuilder().where().in("SERIE_ID", serieIds).query();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public <T, C> C queryById(Long id, Class<T> clazz) {
        try {
            if (clazz.isAssignableFrom(BookArchive.class)) {
                return (C) archivesDao.queryForId(String.valueOf(id));
            } else if (clazz.isAssignableFrom(BookSerie.class)) {
                return (C) seriesDao.queryForId(String.valueOf(id));
            } else if (clazz.isAssignableFrom(BookChapter.class)) {
                return (C) chaptersDao.queryForId(String.valueOf(id));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T, C> C queryItem(String itemHash, Class<T> clazz) {
        try {
            if (clazz.isAssignableFrom(BookArchive.class)) {
                return (C) archivesDao.queryForEq("MANGA_ID", itemHash).get(0);
            } else if (clazz.isAssignableFrom(BookSerie.class)) {
                return (C) seriesDao.queryForEq("SERIE_ID", itemHash).get(0);
            } else if (clazz.isAssignableFrom(BookChapter.class)) {
                return (C) chaptersDao.queryForEq("CHAPTER_ID", itemHash).get(0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException ignored) {
        }

        try {
            return (C) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T, C> List<C> query(String itemHash, Class<T> clazz) {
        try {
            if (clazz.isAssignableFrom(BookArchive.class)) {
                return (List<C>) archivesDao.queryForEq("MANGA_ID", itemHash);
            } else if (clazz.isAssignableFrom(BookSerie.class)) {
                return (List<C>) seriesDao.queryForEq("SERIE_ID", itemHash);
            } else if (clazz.isAssignableFrom(BookChapter.class)) {
                return (List<C>) chaptersDao.queryForEq("CHAPTER_ID", itemHash);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T, C> List<C> query(String columnName, String fieldValue, Class<T> clazz) {
        try {
            if (clazz.isAssignableFrom(BookArchive.class)) {
                return (List<C>) archivesDao.queryForEq(columnName, fieldValue);
            } else if (clazz.isAssignableFrom(BookSerie.class)) {
                return (List<C>) seriesDao.queryForEq(columnName, fieldValue);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Long countArchivesForSerie(BookSerie serie) {
        return count("SERIE", String.valueOf(serie.getDbId()), BookArchive.class);
    }

    public List<BookArchive> queryArchivesForSerie(BookSerie serie) {
        return query("SERIE", String.valueOf(serie.getDbId()), BookArchive.class);
    }

    public Long countChaptersForSerie(BookSerie serie) {
        return count("SERIE", String.valueOf(serie.getDbId()), BookChapter.class);
    }

    public Long countChaptersForSeries(List<Long> serieIds) {
        try {
            return chaptersDao.queryBuilder()
                    .setCountOf(true)
                    .where()
                    .in("SERIE", serieIds)
                    .countOf();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> queryArchivesForSeries(List<Long> serieIds) {
        if (GUArray.isNotEmpty(serieIds)) {
            try {
                return (List<T>) archivesDao.queryBuilder()
                        .where()
                        .in("SERIE", serieIds)
                        .query();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>();
    }

    public List<BookChapter> queryChaptersForArchives(Collection<Long> archiveIds) {
        try {
            if (GUArray.isEmpty(archiveIds)) {
                return new ArrayList<>();
            }
            return chaptersDao.queryBuilder()
                    .where()
                    .in("ARCHIVE", archiveIds)
                    .query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<BookChapter> queryChapters(List<String> archiveHashs) {
        try {
            if (GUArray.isEmpty(archiveHashs)) {
                return new ArrayList<>();
            }
            return chaptersDao.queryBuilder()
                    .where()
                    .in("ARCHIVE_ID", archiveHashs)
                    .query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> boolean isExist(String itemHash, Class<T> clazz) {
        try {
            QueryBuilder<T, String> queryBuilder = getQueryBuilder(clazz);
            if (clazz.isAssignableFrom(BookArchive.class)) {
                queryBuilder.where().eq("MANGA_ID", itemHash);
            } else if (clazz.isAssignableFrom(BookSerie.class)) {
                queryBuilder.where().eq("SERIE_ID", itemHash);
            }

            return queryBuilder.countOf() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public <T, C> List<T> queryAll(Class<C> clazz) {
        try {
            if (clazz.isAssignableFrom(BookChapter.class)) {
                return (List<T>) chaptersDao.queryForAll();
            } else if (clazz.isAssignableFrom(Category.class)) {
                return (List<T>) categoryDao.queryForAll();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T, C> List<T> queryAll(Class<C> clazz, LibraryPresentation libraryPresentation) {
        try {
            if (clazz.isAssignableFrom(BookArchive.class)) {
                return (List<T>) archivesDao.queryForAll();
            } else if (clazz.isAssignableFrom(BookSerie.class)) {
                if (libraryPresentation.isSinglesPresentation()) {
                    return (List<T>) seriesDao.queryBuilder().where().eq(IS_SINGLE_FIELD_NAME, libraryPresentation.isSinglesPresentation()).query();
                }
                return (List<T>) seriesDao.queryForAll();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T, C> List<C> queryLike(String likeColumn, String like, Class<T> clazz) {
        try {
            return (List<C>) getDao(clazz).queryBuilder()
                    .where()
                    .like(likeColumn, "%" + like.replace("'", "''") + "%")
                    .query();
        } catch (SQLException e) {
            logger.error("Unable to query data by LIKE: likeColumn = " + likeColumn + ", like = " + like);
            logger.error(e.getMessage());
        }
        return null;
    }

    public <T> List<T> queryLike(String orderByColumnName, boolean ascendingOrder, long offset, long limit, List<String> allowedContentTypes,
                                 List<String> allowedCategories, LibraryPresentation libraryPresentation, Class<? extends IBaseBookItem> clazz) {
        return query(orderByColumnName, ascendingOrder, offset, limit, null, null, null, allowedContentTypes, allowedCategories, libraryPresentation, clazz);
    }

    public <T> List<T> query(String orderByColumnName, boolean ascendingOrder, ContentType contentType, String category, List<String> allowedContentTypes,
                             List<String> allowedCategories, LibraryPresentation libraryPresentation, Class<? extends IBaseBookItem> clazz) {
        return query(orderByColumnName, ascendingOrder, 0, Integer.MAX_VALUE, null, contentType, category, allowedContentTypes, allowedCategories, libraryPresentation, clazz);
    }

    public <T> List<T> query(String orderByColumnName, boolean ascendingOrder, long offset, long limit, ContentType contentType, String category,
                             List<String> allowedContentTypes, List<String> allowedCategories, LibraryPresentation libraryPresentation, Class<? extends IBaseBookItem> clazz) {
        return query(orderByColumnName, ascendingOrder, offset, limit, null, contentType, category, allowedContentTypes, allowedCategories, libraryPresentation, clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> query(String orderByColumnName, boolean ascendingOrder, long offset, long limit, String groupBy, ContentType contentType,
                             String category, List<String> allowedContentTypes, List<String> allowedCategories, LibraryPresentation libraryPresentation, Class<? extends IBaseBookItem> clazz) {
        try {
            if (clazz.isAssignableFrom(BookArchive.class)) {
                QueryBuilder<BookArchive, String> queryBuilder = archivesDao.queryBuilder();
                buildQuery(queryBuilder, orderByColumnName, ascendingOrder, offset, limit, groupBy, contentType, category, allowedContentTypes, allowedCategories, null);
                return (List<T>) archivesDao.query(queryBuilder.prepare());
            } else if (clazz.isAssignableFrom(BookSerie.class)) {
                QueryBuilder<BookSerie, String> queryBuilder = seriesDao.queryBuilder();
                buildQuery(queryBuilder, orderByColumnName, ascendingOrder, offset, limit, groupBy, contentType, category, allowedContentTypes, allowedCategories, libraryPresentation);
                return (List<T>) seriesDao.query(queryBuilder.prepare());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private <T> void buildQuery(QueryBuilder<T, String> queryBuilder, String orderByColumnName, boolean ascendingOrder, long offset, long limit,
                                String groupBy, ContentType contentType, String category, List<String> allowedContentTypes, List<String> allowedCategories,
                                LibraryPresentation libraryPresentation) throws SQLException {
        queryBuilder.orderBy(orderByColumnName, ascendingOrder).offset(offset).limit(limit);

        if (GUString.isNotEmpty(groupBy)) {
            queryBuilder.groupBy(groupBy);
        }

        boolean hasAtLeastOneWhere = libraryPresentation != null;
        Where<T, String> where = libraryPresentation != null
                ? queryBuilder.where().eq(REMOVED_FIELD_NAME, false)
                : null;
        if (where == null && (contentType != null || libraryPresentation != null && !libraryPresentation.isSeriesAndSinglesPresentation())) {
            where = queryBuilder.where();
        }

        if (contentType != null) {
            if (hasAtLeastOneWhere) {
                where.and();
            }
            where.eq("CONTENT_TYPE", contentType.name());
            hasAtLeastOneWhere = true;
        }

        if (GUString.isNotEmpty(category)) {
            if (hasAtLeastOneWhere) {
                where.and();
            }
            where.like("CATEGORIES", "%" + category + "%");
            hasAtLeastOneWhere = true;
        } else if (contentType != null) {
            if (hasAtLeastOneWhere) {
                where.and();
            }
            where.isNull("CATEGORIES");
            hasAtLeastOneWhere = true;
        }

        if (GUArray.isNotEmpty(allowedContentTypes) && where != null) {
            if (hasAtLeastOneWhere) {
                where.and();
            }
            where.in("CONTENT_TYPE", allowedContentTypes);
            hasAtLeastOneWhere = true;
        }

        if (libraryPresentation != null && !libraryPresentation.isSeriesAndSinglesPresentation()) {
            if (hasAtLeastOneWhere) {
                where.and();
            }
            where.eq(IS_SINGLE_FIELD_NAME, libraryPresentation.isSinglesPresentation());
        }
    }

    //**********************//
    //        Remove         //
    //**********************//
    public <T> int removeByColumnLike(String columnName, String columnValue, Class<T> clazz) {
        try {
            if (clazz.isAssignableFrom(BookArchive.class)) {
                DeleteBuilder<BookArchive, String> deleteBuilder = archivesDao.deleteBuilder();
                deleteBuilder.where().like(columnName,  "%" + columnValue + "%");
                return archivesDao.delete(deleteBuilder.prepare());
            } else if (clazz.isAssignableFrom(BookSerie.class)) {
                DeleteBuilder<BookSerie, String> deleteBuilder = seriesDao.deleteBuilder();
                deleteBuilder.where().like(columnName,  "%" + columnValue + "%");
                return seriesDao.delete(deleteBuilder.prepare());
            } else if (clazz.isAssignableFrom(BookChapter.class)) {
                DeleteBuilder<BookChapter, String> deleteBuilder = chaptersDao.deleteBuilder();
                deleteBuilder.where().like(columnName,  "%" + columnValue + "%");
                return chaptersDao.delete(deleteBuilder.prepare());
            }
            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public <T> int removeByColumnIn(String columnName, List<String> inValues, Class<T> clazz) {
        try {
            inValues = inValues.stream()
                    .map(folder -> folder.replace("'", "''"))
                    .collect(Collectors.toList());

            if (clazz.isAssignableFrom(BookArchive.class)) {
                DeleteBuilder<BookArchive, String> deleteBuilder = archivesDao.deleteBuilder();
                deleteBuilder.where().in(columnName, inValues);
                return archivesDao.delete(deleteBuilder.prepare());
            } else if (clazz.isAssignableFrom(BookSerie.class)) {
                DeleteBuilder<BookSerie, String> deleteBuilder = seriesDao.deleteBuilder();
                deleteBuilder.where().in(columnName, inValues);
                return seriesDao.delete(deleteBuilder.prepare());
            } else if (clazz.isAssignableFrom(BookChapter.class)) {
                DeleteBuilder<BookChapter, String> deleteBuilder = chaptersDao.deleteBuilder();
                deleteBuilder.where().in(columnName, inValues);
                return chaptersDao.delete(deleteBuilder.prepare());
            } else if (clazz.isAssignableFrom(Category.class)) {
                DeleteBuilder<Category, String> deleteBuilder = categoryDao.deleteBuilder();
                deleteBuilder.where().in(columnName, inValues);
                return categoryDao.delete(deleteBuilder.prepare());
            }
            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public class HistoryDao {

        public boolean save(History item) {
            return createOrUpdate(historyDao, item, item.getDbId() != null) > 0;
        }

        public List<History> queryByHash(String hash, Class<? extends IBaseBookItem> clazz) {
            try {
                String fieldName = clazz.isAssignableFrom(BookSerie.class) ? "SERIE_HASH" : "ARCHIVE_HASH";
                return historyDao.queryForEq(fieldName, hash);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        public List<History> queryByHashForUser(String hash, String userId, Class<?> clazz) {
            try {
                String fieldName = null;
                boolean isChapterFieldNeedsToBeNull = true;
                if (clazz.isAssignableFrom(BookSerie.class)) {
                    fieldName = "SERIE_HASH";
                } else if (clazz.isAssignableFrom(BookArchive.class)) {
                    fieldName = "ARCHIVE_HASH";
                } else if (clazz.isAssignableFrom(BookChapter.class)) {
                    fieldName = "CHAPTER_HASH";
                    isChapterFieldNeedsToBeNull = false;
                }

                 Where<History, String> whereQuery = historyDao.queryBuilder()
                        .where()
                        .eq(fieldName, hash)
                        .and()
                        .eq("USER_ID", userId);

                if (isChapterFieldNeedsToBeNull) {
                    whereQuery = whereQuery.and().isNull("CHAPTER_HASH");
                } else {
                    whereQuery = whereQuery.and().isNotNull("CHAPTER_HASH");
                }

                return whereQuery.query();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        public List<History> queryByHashesForUser(List<String> hashes, String userId, Class<? extends IBaseBookItem> clazz) {
            try {
                String fieldName = clazz.isAssignableFrom(BookSerie.class) ? "SERIE_HASH" : "ARCHIVE_HASH";
                return historyDao.queryBuilder()
                        .where()
                        .in(fieldName, hashes)
                        .and()
                        .eq("USER_ID", userId)
                        .query();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        public List<History> query(String whereField, String whereValue, String orderByColumnName, List<String> allowedContentTypes, List<String> allowedCategories, boolean ascendingOrder, long offset, long limit) {
            return query(whereField, whereValue, orderByColumnName, allowedContentTypes, allowedCategories, ascendingOrder, offset, limit, null);
        }

        public List<History> query(String whereField, String whereValue, String orderByColumnName, List<String> allowedContentTypes, List<String> allowedCategories, boolean ascendingOrder, long offset, long limit, String groupBy) {
            try {
                QueryBuilder<History, String> queryBuilder = historyDao.queryBuilder();
                queryBuilder.where().eq(whereField, whereValue);
                buildQuery(queryBuilder, orderByColumnName, ascendingOrder, offset, limit, groupBy, null, null, allowedContentTypes, allowedCategories, null);
                return historyDao.query(queryBuilder.prepare());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }

        public boolean removeById(long id) {
            try {
                DeleteBuilder<History, String> deleteBuilder = historyDao.deleteBuilder();
                deleteBuilder.where().eq("id", id);
                return historyDao.delete(deleteBuilder.prepare()) > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}
