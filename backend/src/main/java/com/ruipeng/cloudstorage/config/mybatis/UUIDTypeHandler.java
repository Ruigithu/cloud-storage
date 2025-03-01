package com.ruipeng.cloudstorage.config.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.UUID;

public class UUIDTypeHandler extends BaseTypeHandler<UUID> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, parameter, Types.OTHER); // 使用Types.OTHER处理UUID
    }

    @Override
    public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Object uuid = rs.getObject(columnName);
        return uuid != null ? (UUID) uuid : null;
    }

    @Override
    public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Object uuid = rs.getObject(columnIndex);
        return uuid != null ? (UUID) uuid : null;
    }

    @Override
    public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object uuid = cs.getObject(columnIndex);
        return uuid != null ? (UUID) uuid : null;
    }
}
