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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Objects;

import javax.sql.DataSource;

import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.BundleCapabilityRef;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Deactivate;
import org.everit.osgi.ecm.annotation.ManualService;
import org.everit.osgi.ecm.annotation.ManualServices;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttributes;
import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.component.ConfigurationException;
import org.everit.osgi.ecm.extender.ExtendComponent;
import org.everit.persistence.liquibase.datasource.ecm.LiquibaseDataSourceConstants;
import org.everit.persistence.liquibase.ext.osgi.EOSGiResourceAccessor;
import org.everit.persistence.liquibase.ext.osgi.LiquibaseEOSGiConstants;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleCapability;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ResourceAccessor;

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
        defaultValue = "Everit Liquibase DataSource",
        priority = LiquibaseDataSourceComponent.P_SERVICE_DESCRIPTION,
        label = "Service Description",
        description = "The description of this component configuration. It is used to easily "
            + "identify the service registered by this component.") })
@ManualServices(@ManualService(DataSource.class))
public class LiquibaseDataSourceComponent {

  public static final int P_CONTEXTS = 5;

  public static final int P_EMBEDDED_DATA_SOURCE = 2;

  public static final int P_LABEL_EXPRESSION = 6;

  public static final int P_LIQUIBASE_SERVICE = 3;

  public static final int P_LOG_SERVICE = 4;

  public static final int P_REF_LIQUIBASE_CHANGELOGS = 1;

  public static final int P_SERVICE_DESCRIPTION = 0;

  public static final int P_TAG = 7;

  private String[] contexts;

  private DataSource dataSource;

  private String labelExpression;

  private BundleCapability[] liquibaseChangeLogs;

  private ServiceRegistration<DataSource> serviceRegistration;

  private String tag;

  /**
   * Component activator method.
   */
  @Activate
  public void activate(final ComponentContext<LiquibaseDataSourceComponent> context) {
    if (liquibaseChangeLogs == null || liquibaseChangeLogs.length == 0) {
      throw new ConfigurationException("schemaExpression must be defined");
    }

    try (Connection connection = dataSource.getConnection()) {
      DatabaseConnection databaseConnection = new JdbcConnection(connection);
      Contexts contextsObj = (contexts == null) ? new Contexts() : new Contexts(contexts);
      LabelExpression labelExpressionObj = new LabelExpression(labelExpression);

      for (BundleCapability changeLogCapability : liquibaseChangeLogs) {
        ResourceAccessor resourceAccessor = new EOSGiResourceAccessor(
            changeLogCapability.getRevision().getBundle(), changeLogCapability.getAttributes());

        Object resourceName = changeLogCapability.getAttributes()
            .get(LiquibaseEOSGiConstants.CAPABILITY_ATTR_RESOURCE);

        Objects.requireNonNull(resourceName,
            "'" + LiquibaseEOSGiConstants.CAPABILITY_ATTR_RESOURCE
                + "' attribute must be specified in '"
                + LiquibaseEOSGiConstants.CAPABILITY_NS_LIQUIBASE_CHANGELOG + "' capability: "
                + changeLogCapability.toString());

        Liquibase liquibase =
            new Liquibase(String.valueOf(resourceName), resourceAccessor, databaseConnection);

        liquibase.update(tag, contextsObj, labelExpressionObj);

        serviceRegistration = context.registerService(DataSource.class, dataSource,
            new Hashtable<>(context.getProperties()));
      }
    } catch (SQLException | LiquibaseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Unregisters the datasource service.
   */
  @Deactivate
  public void deactivate() {
    if (serviceRegistration != null) {
      serviceRegistration.unregister();
    }
  }

  @StringAttribute(attributeId = LiquibaseDataSourceConstants.ATTR_CONTEXTS, optional = true,
      priority = P_CONTEXTS, label = "Contexts",
      description = "When you run the migrator though any of the available methods, you can pass"
          + " in a set of contexts to run. Only changeSets marked with the passed contexts will"
          + " be run.")
  public void setContexts(final String[] contexts) {
    this.contexts = contexts;
  }

  @ServiceRef(attributeId = LiquibaseDataSourceConstants.ATTR_DATASOURCE_TARGET,
      defaultValue = "", attributePriority = P_EMBEDDED_DATA_SOURCE,
      label = "Embedd DataSource filter",
      description = "OSGi filter expression to reference the DataSource service that will be re-"
          + "registered after the database schema is processed by Liquibase.")
  public void setDataSource(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @StringAttribute(attributeId = LiquibaseDataSourceConstants.ATTR_LABEL_EXPRESSION,
      optional = true, priority = P_LABEL_EXPRESSION, label = "Label expression",
      description = ". In your changeSet you can only specify a simple list of \"labels\" that"
          + " apply to the changeSet but at runtime you can write a complex expression to chose"
          + " the labels you want to execute. This allows you to specify a changeSet with"
          + " labels=\"qa, acme_inc\" and then at runtime use expressions such as"
          + " labelExpression=\"!acme_inc\" or labelExpression=\"pro or (free and beta)\".")
  public void setLabelExpression(final String labelExpression) {
    this.labelExpression = labelExpression;
  }

  @BundleCapabilityRef(namespace = LiquibaseEOSGiConstants.CAPABILITY_NS_LIQUIBASE_CHANGELOG,
      referenceId = LiquibaseDataSourceConstants.ReferenceConstants.LIQUIBASE_CHANGELOG,
      attributeId = LiquibaseDataSourceConstants.ATTR_LIQUIBASE_CHANGELOG,
      optional = false, attributePriority = P_REF_LIQUIBASE_CHANGELOGS,
      label = "Liquibase Changelog Capability",
      description = "Filter expression that points to a liquibase.changelog Bundle Capability."
          + " E.g.: (" + LiquibaseEOSGiConstants.CAPABILITY_NS_LIQUIBASE_CHANGELOG + "=mySchema)")
  public void setLiquibaseChangeLogCapabilities(final BundleCapability[] liquibaseSchemas) {
    this.liquibaseChangeLogs = liquibaseSchemas;
  }

  @StringAttribute(attributeId = LiquibaseDataSourceConstants.ATTR_TAG, optional = true,
      priority = P_TAG, label = "Tag", description = "Tag of the database for future rollback.")
  public void setTag(final String tag) {
    this.tag = tag;
  }

}
