/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.persistence.liquibase.datasource.ecm.tests;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttributes;
import org.everit.osgi.ecm.extender.ExtendComponent;
import org.junit.Test;

/**
 * Test component that tests functionlity.
 */
@ExtendComponent
@Component(componentId = "LiquibaseTest", configurationPolicy = ConfigurationPolicy.REQUIRE)
@StringAttributes({
    @StringAttribute(attributeId = TestRunnerConstants.SERVICE_PROPERTY_TEST_ID,
        defaultValue = "JettyComponentTest"),
    @StringAttribute(attributeId = TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE,
        defaultValue = "junit4") })
@Service
public class LiquibaseDataSourceComponentTest {

  private final AtomicReference<DataSource> dataSource = new AtomicReference<DataSource>();

  @ServiceRef
  public void setDataSource(final DataSource dataSource) {
    this.dataSource.set(dataSource);
  }

  @Test
  public void testDatabaseExistence() {
    try (Connection connection = dataSource.get().getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("select * from person");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

}
