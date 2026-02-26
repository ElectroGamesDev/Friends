package com.electro.friends.database;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.SQLException;


public interface Database {

    @Nonnull
    Connection getConnection() throws SQLException;

    void releaseConnection(@Nonnull Connection connection);

    void close();

    boolean isClosed();

    boolean isMySQL();
}
