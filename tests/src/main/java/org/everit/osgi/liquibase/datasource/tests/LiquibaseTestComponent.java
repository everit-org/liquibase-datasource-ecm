/**
 * This file is part of Everit - Liquibase DataSource Tests.
 *
 * Everit - Liquibase DataSource Tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - Liquibase DataSource Tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - Liquibase DataSource Tests.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.liquibase.datasource.tests;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.junit.Test;

@Component(name = "LiquibaseTest", immediate = true, metatype = true)
@Service(value = LiquibaseTestComponent.class)
@Properties({ @Property(name = "eosgi.testEngine", value = "junit4"),
        @Property(name = "eosgi.testId", value = "liquibaseTest"),
        @Property(name = "dataSource.target") })
public class LiquibaseTestComponent {

    @Reference(referenceInterface = DataSource.class, target = "(liquibase.schema.name=myApp)",
            policy = ReferencePolicy.DYNAMIC)
    private AtomicReference<DataSource> dataSource = new AtomicReference<DataSource>();

    @Activate
    public void activate() {
    }

    public void bindDataSource(final DataSource dataSource) {
        this.dataSource.set(dataSource);
        ;
    }

    public void unbindDataSource(final DataSource dataSource) {
        this.dataSource = null;
    }

    @Test
    @TestDuringDevelopment
    public void testDatabaseExistence() {
        try {
            Connection connection = dataSource.get().getConnection();
            Statement statement = connection.createStatement();
            statement.execute("select * from person");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
