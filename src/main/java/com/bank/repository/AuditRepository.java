package com.bank.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditRepository {
    public void save(Connection connection, String action, String entityType, long entityId, String description)
            throws SQLException {
        String sql = """
                INSERT INTO audit_logs (action, entity_type, entity_id, description)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, action);
            statement.setString(2, entityType);
            statement.setLong(3, entityId);
            statement.setString(4, description);
            statement.executeUpdate();
        }
    }
}
