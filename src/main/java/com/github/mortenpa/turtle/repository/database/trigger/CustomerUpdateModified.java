package com.github.mortenpa.turtle.repository.database.trigger;

import org.h2.tools.TriggerAdapter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerUpdateModified extends TriggerAdapter {

    @Override
    public void fire(Connection conn, ResultSet oldRow, ResultSet newRow) throws SQLException {
        newRow.updateTimestamp("modifiedDTime", new java.sql.Timestamp(System.currentTimeMillis()));
    }
}
