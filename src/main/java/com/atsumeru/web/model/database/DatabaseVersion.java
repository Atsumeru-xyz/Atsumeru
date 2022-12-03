package com.atsumeru.web.model.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "DATABASE_VERSION")
public class DatabaseVersion {
    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField(columnName = "VERSION")
    private long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
