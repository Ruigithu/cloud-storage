package com.ruipeng.cloudstorage.config.mybatis;

import com.ruipeng.cloudstorage.entity.PermissionType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;

@MappedTypes(PermissionType.class)
public class PermissionTypeHandler extends BaseTypeHandler<PermissionType> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, PermissionType parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, parameter.name().toLowerCase(), Types.OTHER);
    }

    @Override
    public PermissionType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null :  PermissionType.valueOf(value.toUpperCase());
    }

    @Override
    public PermissionType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : PermissionType.valueOf(value.toUpperCase());
    }

    @Override
    public PermissionType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null :  PermissionType.valueOf(value.toUpperCase());
    }
}