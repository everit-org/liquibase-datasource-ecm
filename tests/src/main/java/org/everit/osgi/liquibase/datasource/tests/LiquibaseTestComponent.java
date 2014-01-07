package org.everit.osgi.liquibase.datasource.tests;

/*
 * Copyright (c) 2011, Everit Kft.
 *
 * All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.junit.Test;

@Component(name = "LiquibaseTest", immediate = true)
@Service(value = LiquibaseTestComponent.class)
@Properties({ @Property(name = "eosgi.testEngine", value = "junit4"),
        @Property(name = "eosgi.testId", value = "liquibaseTest"),
        @Property(name = "dataSource.target")})
public class LiquibaseTestComponent {

    @Reference(target="(liquibase.schema.name=myApp)")
    private DataSource dataSource;


    @Activate
    public void activate() {
    }

    public void bindDataSource(final DataSource dataSource) {
        this.dataSource = dataSource;
    }


    @Test
    @TestDuringDevelopment
    public void testDatabaseExistence() {
        try {
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            statement.execute("select * from person");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
