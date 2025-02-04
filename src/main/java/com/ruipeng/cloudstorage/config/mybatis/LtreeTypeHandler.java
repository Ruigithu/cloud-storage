package com.ruipeng.cloudstorage.config.mybatis;


import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(PGobject.class)
public class LtreeTypeHandler extends BaseTypeHandler<PGobject> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, PGobject parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, parameter);
    }

    @Override
    public PGobject getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return (PGobject) rs.getObject(columnName);
    }

    @Override
    public PGobject getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return (PGobject) rs.getObject(columnIndex);
    }

    @Override
    public PGobject getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return (PGobject) cs.getObject(columnIndex);
    }
}