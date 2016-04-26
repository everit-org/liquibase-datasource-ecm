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
package org.everit.persistence.liquibase.datasource.ecm.internal;

import java.util.Map;

import javax.sql.DataSource;

import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.BundleCapabilityRef;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Deactivate;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttributes;
import org.everit.osgi.ecm.component.ConfigurationException;
import org.everit.osgi.ecm.extender.ExtendComponent;
import org.everit.persistence.liquibase.datasource.ecm.LiquibaseDataSourceConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.service.log.LogService;

/**
 * ECM based configurable component that process liquibase schemas.
 */
@ExtendComponent
@Component(componentId = LiquibaseDataSourceConstants.SERVICE_PID,
    configurationPolicy = ConfigurationPolicy.FACTORY, label = "Everit Liquibase DataSource",
    description = "Applies Liquibase changeLog files on the configured DataSource, and registers a"
        + "new DataSource OSGi service after the database upgrade is successful. The changelog"
        + " files must be referenced via liquibase.schema bundle capability.")
@StringAttributes({
    @StringAttribute(attributeId = Constants.SERVICE_DESCRIPTION,
        defaultValue = "Default Liquibase DataSource",
        priority = LiquibaseDataSourceComponent.P_SERVICE_DESCRIPTION,
        label = "Service Description",
        description = "The description of this component configuration. It is used to easily "
            + "identify the service registered by this component.") })
public class LiquibaseDataSourceComponent {

  public static final int P_EMBEDDED_DATA_SOURCE = 2;

  public static final int P_LIQUIBASE_SERVICE = 3;

  public static final int P_LOG_SERVICE = 4;

  public static final int P_REF_LIQUIBASE_CHANGELOGS = 1;

  public static final int P_SERVICE_DESCRIPTION = 0;

  private DataSource dataSource;

  private BundleCapability[] liquibaseChangeLogs;

  private LogService logService;

  /**
   * Component activator method.
   */
  @Activate
  public void activate(final BundleContext context, final Map<String, Object> componentProperties) {
    if (liquibaseChangeLogs == null || liquibaseChangeLogs.length == 0) {
      throw new ConfigurationException("schemaExpression must be defined");
    }
  }

  @Deactivate
  public void deactivate() {
  }

  @ServiceRef(attributeId = LiquibaseDataSourceConstants.ATTR_DATASOURCE_TARGET,
      defaultValue = "", attributePriority = P_EMBEDDED_DATA_SOURCE,
      label = "Embedd DataSource filter",
      description = "OSGi filter expression to reference the DataSource service that will be re-"
          + "registered after the database schema is processed by Liquibase.")
  public void setDataSource(final DataSource dataSource) {
    dataSource = dataSource;
  }

  @BundleCapabilityRef(namespace = "liquibase.schema",
      referenceId = LiquibaseDataSourceConstants.ReferenceConstants.LIQUIBASE_CHANGELOG_CAPABILITIES, // CS_DISABLE_LINE_LENGTH
      attributeId = LiquibaseDataSourceConstants.ATTR_LIQUIBASE_CHANGELOG_CAPABILITIES,
      optional = false)

  @StringAttribute(attributeId = LiquibaseDataSourceConstants.ATTR_LIQUIBASE_CHANGELOG_CAPABILITIES,
      priority = LiquibaseDataSourceComponent.P_REF_LIQUIBASE_CHANGELOGS,
      label = "Schema expression",
      description = "An expression that references the schema to reference a capability of a "
          + "provider bundle. The syntax of the expression is schemaName[;filter:=(expression)] "
          + "where the name of the schema is required. A filter can be defined as well in case "
          + "the same schema is provided by multiple bundles or if the same bundle provides "
          + "the same schema name from different resources. E.g. If we have the "
          + "\"Provide-Capability: liquibase.schema;name=myApp;resource=/META-INF/changelog.xml;"
          + "version=2.0.0\", the value of this property can be "
          + "\"myApp;filter:=(version>=2)\". ")
  public void setLiquibaseChangeLogCapabilities(final BundleCapability[] liquibaseSchemas) {
    this.liquibaseChangeLogs = liquibaseSchemas;
  }

  @ServiceRef(attributeId = LiquibaseDataSourceConstants.ATTR_LOG_SERVICE_TARGET, defaultValue = "",
      attributePriority = P_LOG_SERVICE, label = "LogService filter",
      description = "OSGi filter expression of LogService")
  public void setLogService(final LogService logService) {
    this.logService = logService;
  }

}
