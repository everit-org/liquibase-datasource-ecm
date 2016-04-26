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
package org.everit.persistence.liquibase.datasource.ecm;

/**
 * Constants of LiquibaseDataSource component.
 */
public final class LiquibaseDataSourceConstants {

  /**
   * Name of references.
   */
  public static final class ReferenceConstants {
    public static final String DATASOURCE = "dataSource";

    public static final String LIQUIBASE_CHANGELOG = "liquibaseChangeLog";

    private ReferenceConstants() {
    }
  }

  public static final String ATTR_CONTEXTS = "contexts";

  public static final String ATTR_DATASOURCE_TARGET = ReferenceConstants.DATASOURCE + ".target";

  public static final String ATTR_LABEL_EXPRESSION = "labelExpression";

  public static final String ATTR_LIQUIBASE_CHANGELOG =
      ReferenceConstants.LIQUIBASE_CHANGELOG + ".target";

  public static final String ATTR_TAG = "tag";

  public static final String SERVICE_PID =
      "org.everit.persistence.liquibase.datasource.ecm.LiquibaseDataSource";

  private LiquibaseDataSourceConstants() {
  }
}
