package xyz.atsumeru.web.model.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@DatabaseTable(tableName = "DATABASE_VERSION")
public class DatabaseVersion {
    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField(columnName = "VERSION")
    private long version;

}
