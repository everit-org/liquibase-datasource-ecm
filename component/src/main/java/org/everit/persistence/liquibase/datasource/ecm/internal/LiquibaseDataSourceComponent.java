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
import org.everit.osgi.ecm.annotation.attribute.BooleanAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttributeOption;
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
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.ObjectQuotingStrategy;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
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

  private static final float P_AUTO_COMMIT = 8;

  private static final float P_CAN_CACHE_LIQUIBASE_TABLE_INFO = 20;

  public static final int P_CONTEXTS = 5;

  private static final float P_CURRENT_DATETIME_FUNCTION = 19;

  private static final float P_DATABASE_CHANGE_LOG_LOCK_TABLE_NAME = 17;

  private static final float P_DATABASE_CHANGE_LOG_TABLE_NAME = 18;

  private static final float P_DEFAULT_CATALOG_NAME = 8;

  private static final float P_DEFAULT_SCHEMA_NAME = 9;

  public static final int P_EMBEDDED_DATA_SOURCE = 2;

  public static final int P_LABEL_EXPRESSION = 6;

  private static final float P_LIQUIBASE_CATALOG_NAME = 10;

  private static final float P_LIQUIBASE_SCHEMA_NAME = 11;

  public static final int P_LIQUIBASE_SERVICE = 3;

  private static final float P_LIQUIBASE_TABLE_SPACE_NAME = 12;

  public static final int P_LOG_SERVICE = 4;

  private static final float P_OBJECT_QUOTING_STRATEGY = 13;

  private static final float P_OUTPUT_DEFAULT_CATALOG = 14;

  private static final float P_OUTPUT_DEFAULT_SCHEMA = 15;

  public static final int P_REF_LIQUIBASE_CHANGELOGS = 1;

  public static final int P_SERVICE_DESCRIPTION = 0;

  public static final int P_TAG = 7;

  private Boolean autoCommit;

  private Boolean canCacheLiquibaseTableInfo;

  private String[] contexts;

  private String currentDateTimeFunction;

  private String databaseChangeLogLockTableName;

  private String databaseChangeLogTableName;

  private DataSource dataSource;

  private String defaultCatalogName;

  private String defaultSchemaName;

  private String labelExpression;

  private String liquibaseCatalogName;

  private BundleCapability[] liquibaseChangeLogs;

  private String liquibaseSchemaName;

  private String liquibaseTableSpaceName;

  private String objectQuotingStrategy;

  private Boolean outputDefaultCatalog;

  private Boolean outputDefaultSchema;

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
      Database database =
          DatabaseFactory.getInstance().findCorrectDatabaseImplementation(databaseConnection);

      applyDatabaseSettings(database);

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
            new Liquibase(String.valueOf(resourceName), resourceAccessor, database);

        liquibase.update(tag, contextsObj, labelExpressionObj);
      }

      serviceRegistration = context.registerService(DataSource.class, dataSource,
          new Hashtable<>(context.getProperties()));

    } catch (SQLException | LiquibaseException e) {
      throw new RuntimeException(e);
    }
  }

  private void applyAutoCommitIfSet(final Database database) throws DatabaseException {
    if (autoCommit != null) {
      database.setAutoCommit(autoCommit);
    }
  }

  private void applyCanCacheLiquibaseTableInfoIfSet(final Database database) {
    if (canCacheLiquibaseTableInfo != null) {
      database.setCanCacheLiquibaseTableInfo(canCacheLiquibaseTableInfo);
    }
  }

  private void applyCurrentDateTimeFunctionIfSet(final Database database) {
    if (currentDateTimeFunction != null) {
      database.setCurrentDateTimeFunction(currentDateTimeFunction);
    }
  }

  private void applyDatabaseChangeLogLockTableNameIfSet(final Database database) {
    if (databaseChangeLogLockTableName != null) {
      database.setDatabaseChangeLogLockTableName(databaseChangeLogLockTableName);
    }
  }

  private void applyDatabaseChangeLogTableNameIfSet(final Database database) {
    if (databaseChangeLogTableName != null) {
      database.setDatabaseChangeLogTableName(databaseChangeLogTableName);
    }
  }

  private void applyDatabaseSettings(final Database database) throws DatabaseException {
    applyAutoCommitIfSet(database);
    applyCanCacheLiquibaseTableInfoIfSet(database);
    applyCurrentDateTimeFunctionIfSet(database);
    applyDatabaseChangeLogLockTableNameIfSet(database);
    applyDatabaseChangeLogTableNameIfSet(database);
    applyDefaultCatalogNameIfSet(database);
    applyDefaultSchemaNameIfSet(database);
    applyLiquibaseCatalogNameIfSet(database);
    applyLiquibaseSchemaNameIfSet(database);
    applyLiquibaseTableSpaceNameIfSet(database);
    applyObjectQuotingStrategyIfSet(database);
    applyOutputDefaultCatalogIfSet(database);
    applyOutputDefaultSchemaIfSet(database);
  }

  private void applyDefaultCatalogNameIfSet(final Database database) throws DatabaseException {
    if (defaultCatalogName != null) {
      database.setDefaultCatalogName(defaultCatalogName);
    }
  }

  private void applyDefaultSchemaNameIfSet(final Database database) throws DatabaseException {
    if (defaultSchemaName != null) {
      database.setDefaultSchemaName(defaultSchemaName);
    }
  }

  private void applyLiquibaseCatalogNameIfSet(final Database database) {
    if (liquibaseCatalogName != null) {
      database.setLiquibaseCatalogName(liquibaseCatalogName);
    }
  }

  private void applyLiquibaseSchemaNameIfSet(final Database database) {
    if (liquibaseSchemaName != null) {
      database.setLiquibaseSchemaName(liquibaseSchemaName);
    }
  }

  private void applyLiquibaseTableSpaceNameIfSet(final Database database) {
    if (liquibaseTableSpaceName != null) {
      database.setLiquibaseTablespaceName(liquibaseTableSpaceName);
    }
  }

  private void applyObjectQuotingStrategyIfSet(final Database database) {
    if (objectQuotingStrategy != null) {
      database.setObjectQuotingStrategy(ObjectQuotingStrategy.valueOf(objectQuotingStrategy));
    }
  }

  private void applyOutputDefaultCatalogIfSet(final Database database) {
    if (outputDefaultCatalog != null) {
      database.setOutputDefaultCatalog(outputDefaultCatalog);
    }
  }

  private void applyOutputDefaultSchemaIfSet(final Database database) {
    if (outputDefaultSchema != null) {
      database.setOutputDefaultSchema(outputDefaultSchema);
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

  @BooleanAttribute(attributeId = LiquibaseDataSourceConstants.ATTR_AUTO_COMMIT, optional = true,
      priority = P_AUTO_COMMIT, label = "Auto commit",
      description = "If set, Liquibase will call setAutoCommit on the database connection.")
  public void setAutoCommit(final Boolean autoCommit) {
    this.autoCommit = autoCommit;
  }

  @BooleanAttribute(attributeId = LiquibaseDataSourceConstants.ATTR_CAN_CACHE_LIQUIBASE_TABLE_INFO,
      optional = true, priority = P_CAN_CACHE_LIQUIBASE_TABLE_INFO,
      label = "Can cache liquibase table info",
      description = "Whether Liquibase can cache the liquibase table info or not.")
  public void setCanCacheLiquibaseTableInfo(final Boolean canCacheLiquibaseTableInfo) {
    this.canCacheLiquibaseTableInfo = canCacheLiquibaseTableInfo;
  }

  @StringAttribute(attributeId = LiquibaseDataSourceConstants.ATTR_CONTEXTS, optional = true,
      priority = P_CONTEXTS, label = "Contexts",
      description = "When you run the migrator though any of the available methods, you can pass"
          + " in a set of contexts to run. Only changeSets marked with the passed contexts will"
          + " be run.")
  public void setContexts(final String[] contexts) {
    this.contexts = contexts;
  }

  @StringAttribute(attributeId = LiquibaseDataSourceConstants.ATTR_CURRENT_DATETIME_FUNCTION,
      optional = true,
      priority = P_CURRENT_DATETIME_FUNCTION, label = "Current datetime function",
      description = "The SQL function that Liquibase will use to get the current time.")
  public void setCurrentDateTimeFunction(final String currentDateTimeFunction) {
    this.currentDateTimeFunction = currentDateTimeFunction;
  }

  @StringAttribute(
      attributeId = LiquibaseDataSourceConstants.ATTR_DATABASE_CHANGE_LOG_LOCK_TABLE_NAME,
      optional = true,
      priority = P_DATABASE_CHANGE_LOG_LOCK_TABLE_NAME,
      label = "Database Changelog Lock table name",
      description = "The name of the database changelog lock table that Liquibase should check or"
          + " create.")
  public void setDatabaseChangeLogLockTableName(final String databaseChangeLogLockTableName) {
    this.databaseChangeLogLockTableName = databaseChangeLogLockTableName;
  }

  @StringAttribute(
      attributeId = LiquibaseDataSourceConstants.ATTR_DATABASE_CHANGE_LOG_TABLE_NAME,
      optional = true,
      priority = P_DATABASE_CHANGE_LOG_TABLE_NAME,
      label = "Database Changelog table name",
      description = "The name of the database changelog table that Liquibase should check or"
          + " create.")
  public void setDatabaseChangeLogTableName(final String databaseChangeLogTableName) {
    this.databaseChangeLogTableName = databaseChangeLogTableName;
  }

  @ServiceRef(attributeId = LiquibaseDataSourceConstants.ATTR_DATASOURCE_TARGET,
      defaultValue = "", attributePriority = P_EMBEDDED_DATA_SOURCE,
      label = "Embedd DataSource filter",
      description = "OSGi filter expression to reference the DataSource service that will be re-"
          + "registered after the database schema is processed by Liquibase.")
  public void setDataSource(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @StringAttribute(
      attributeId = LiquibaseDataSourceConstants.ATTR_DEFAULT_CATALOG_NAME,
      optional = true,
      priority = P_DEFAULT_CATALOG_NAME,
      label = "Default catalog",
      description = "The name of the catalog Liquibase should use by default.")
  public void setDefaultCatalogName(final String defaultCatalogName) {
    this.defaultCatalogName = defaultCatalogName;
  }

  @StringAttribute(
      attributeId = LiquibaseDataSourceConstants.ATTR_DEFAULT_SCHEMA_NAME,
      optional = true,
      priority = P_DEFAULT_SCHEMA_NAME,
      label = "Default schema",
      description = "The name of the schema Liquibase should use by default.")
  public void setDefaultSchemaName(final String defaultSchemaName) {
    this.defaultSchemaName = defaultSchemaName;
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

  @StringAttribute(
      attributeId = LiquibaseDataSourceConstants.ATTR_LIQUIBASE_CATALOG_NAME,
      optional = true,
      priority = P_LIQUIBASE_CATALOG_NAME,
      label = "Liquibase catalog",
      description = "The name of the catalog Liquibase should use to generate its own tables.")
  public void setLiquibaseCatalogName(final String liquibaseCatalogName) {
    this.liquibaseCatalogName = liquibaseCatalogName;
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

  @StringAttribute(
      attributeId = LiquibaseDataSourceConstants.ATTR_LIQUIBASE_SCHEMA_NAME,
      optional = true,
      priority = P_LIQUIBASE_SCHEMA_NAME,
      label = "Liquibase schema",
      description = "The name of the schema Liquibase should use to generate its own tables.")
  public void setLiquibaseSchemaName(final String liquibaseSchemaName) {
    this.liquibaseSchemaName = liquibaseSchemaName;
  }

  @StringAttribute(
      attributeId = LiquibaseDataSourceConstants.ATTR_LIQUIBASE_TABLE_SPACE_NAME,
      optional = true,
      priority = P_LIQUIBASE_TABLE_SPACE_NAME,
      label = "Liquibase table space",
      description = "The name of the table space Liquibase should use to generate its own tables.")
  public void setLiquibaseTableSpaceName(final String liquibaseTableSpaceName) {
    this.liquibaseTableSpaceName = liquibaseTableSpaceName;
  }

  @StringAttribute(
      attributeId = LiquibaseDataSourceConstants.ATTR_OBJECT_QUOTING_STRATEGY,
      optional = true,
      options = { @StringAttributeOption(label = "Legacy", value = "LEGACY"),
          @StringAttributeOption(label = "Quote all objects", value = "QUOTE_ALL_OBJECTS"),
          @StringAttributeOption(label = "Quote only reserved words",
              value = "QUOTE_ONLY_RESERVED_WORDS") },
      priority = P_OBJECT_QUOTING_STRATEGY,
      label = "Object quoting strategy",
      description = "The object quoting strategy that Liquibase will use to create its own tables.")
  public void setObjectQuotingStrategy(final String objectQuotingStrategy) {
    this.objectQuotingStrategy = objectQuotingStrategy;
  }

  @BooleanAttribute(attributeId = LiquibaseDataSourceConstants.ATTR_OUTPUT_DEFAULT_CATALOG,
      optional = true, priority = P_OUTPUT_DEFAULT_CATALOG,
      label = "Output default catalog",
      description = "Whether to ignore the catalog/database name.")
  public void setOutputDefaultCatalog(final Boolean outputDefaultCatalog) {
    this.outputDefaultCatalog = outputDefaultCatalog;
  }

  @BooleanAttribute(attributeId = LiquibaseDataSourceConstants.ATTR_OUTPUT_DEFAULT_SCHEMA,
      optional = true, priority = P_OUTPUT_DEFAULT_SCHEMA,
      label = "Output default schema",
      description = "Whether to ignore the schema name.")
  public void setOutputDefaultSchema(final Boolean outputDefaultSchema) {
    this.outputDefaultSchema = outputDefaultSchema;
  }

  @StringAttribute(attributeId = LiquibaseDataSourceConstants.ATTR_TAG, optional = true,
      priority = P_TAG, label = "Tag", description = "Tag of the database for future rollback.")
  public void setTag(final String tag) {
    this.tag = tag;
  }

}
