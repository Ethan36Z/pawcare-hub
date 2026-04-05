package com.pawcarehub.backend.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PetMedicalNoteSchemaRepairService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(PetMedicalNoteSchemaRepairService.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public PetMedicalNoteSchemaRepairService(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean hasCreatedAt = columnExists("pet_medical_notes", "created_at");
        boolean hasUpdatedAt = columnExists("pet_medical_notes", "updated_at");
        if (hasCreatedAt && hasUpdatedAt) {
            backfillMissingTimestamps();
            return;
        }

        logger.info("Repairing pet_medical_notes timestamp columns for backward-compatible medical note loading");
        String timestampColumnType = resolveTimestampColumnType();

        if (!hasCreatedAt) {
            jdbcTemplate.execute("ALTER TABLE pet_medical_notes ADD COLUMN created_at " + timestampColumnType + " NULL");
        }

        if (!hasUpdatedAt) {
            jdbcTemplate.execute("ALTER TABLE pet_medical_notes ADD COLUMN updated_at " + timestampColumnType + " NULL");
        }

        backfillMissingTimestamps();
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            if (hasColumn(metaData, connection.getCatalog(), null, tableName, columnName)) {
                return true;
            }

            String schema = connection.getSchema();
            if (schema != null && hasColumn(metaData, connection.getCatalog(), schema, tableName, columnName)) {
                return true;
            }

            return hasColumn(metaData, connection.getCatalog(), schema == null ? null : schema.toUpperCase(), tableName.toUpperCase(), columnName.toUpperCase());
        }
    }

    private boolean hasColumn(
        DatabaseMetaData metaData,
        String catalog,
        String schema,
        String tableName,
        String columnName
    ) throws SQLException {
        try (ResultSet columns = metaData.getColumns(catalog, schema, tableName, columnName)) {
            return columns.next();
        }
    }

    private String resolveTimestampColumnType() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String databaseProductName = connection.getMetaData().getDatabaseProductName().toLowerCase();
            return databaseProductName.contains("mysql") ? "DATETIME(6)" : "TIMESTAMP";
        }
    }

    private void backfillMissingTimestamps() {
        jdbcTemplate.execute("""
            UPDATE pet_medical_notes
            SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
                updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP(6))
            WHERE created_at IS NULL OR updated_at IS NULL
            """);
    }
}
