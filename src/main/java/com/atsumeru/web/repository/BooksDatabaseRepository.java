package com.atsumeru.web.repository;

import com.atsumeru.web.repository.dao.BooksDaoManager;
import com.atsumeru.web.util.GUFile;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

public class BooksDatabaseRepository {
    private static final Logger logger = LoggerFactory.getLogger(BooksDatabaseRepository.class.getSimpleName());
    private static BooksDatabaseRepository INSTANCE;

    @Getter @Setter private String dbName;
    @Getter private BooksDaoManager daoManager;

    public static void connect(String dbName) {
        INSTANCE = new BooksDatabaseRepository(dbName);
        INSTANCE.connect();
    }

    // TODO: allow api call
    public static void vacuum() {
        logger.info("VACUUMing database...");
        INSTANCE.daoManager.vacuum();
    }

    public static BooksDatabaseRepository getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("You must call Database.connect(String) before calling getInstance()");
        }

        return INSTANCE;
    }

    private BooksDatabaseRepository(String dbName) {
        this.dbName = dbName;
    }

    public void connect() {
        try {
            daoManager = new BooksDaoManager(dbName);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    public BooksDaoManager.HistoryDao getHistoryDao() {
        return daoManager.getHistoryDao();
    }

    public void close() {
        GUFile.closeQuietly(daoManager);
    }
}
