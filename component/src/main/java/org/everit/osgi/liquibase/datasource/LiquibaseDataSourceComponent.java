/**
 * This file is part of Everit - Liquibase DataSource Component.
 *
 * Everit - Liquibase DataSource Component is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - Liquibase DataSource Component is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - Liquibase DataSource Component.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.liquibase.datasource;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.everit.osgi.liquibase.component.LiquibaseService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;

@Component(metatype = true, configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Properties({
        @Property(name = "schemaExpression"),
        @Property(name = "embeddedDataSource.target"), @Property(name = "liquibaseService.target"),
        @Property(name = "logService.target")
})
public class LiquibaseDataSourceComponent {

    @Reference(bind = "bindEmbeddedDataSource", unbind = "unbindEmbeddedDataSource")
    private DataSource embeddedDataSource;

    private Map<String, Object> embeddedDataSourceProperties;

    private LiquibaseCapabilityTracker tracker;

    @Reference(bind = "bindLiquibaseService", unbind = "unbindLiquibaseService")
    private LiquibaseService liquibaseService;

    @Reference(bind = "bindLogService", unbind = "unbindLogService")
    private LogService logService;

    @Activate
    public void activate(final BundleContext context, final Map<String, Object> componentProperties) {
        Object schemaExpressionValue = componentProperties.get("schemaExpression");
        if (schemaExpressionValue == null) {
            throw new ConfigurationException("schemaExpression must be defined");
        }
        String schemaExpression = String.valueOf(schemaExpressionValue);
        Object servicePidValue = componentProperties.get(Constants.SERVICE_PID);
        String servicePid = String.valueOf(servicePidValue);

        tracker = new LiquibaseCapabilityTracker(context, schemaExpression, liquibaseService, embeddedDataSource,
                embeddedDataSourceProperties, servicePid, logService);
        tracker.open();
    }

    @Deactivate
    public void deActivate() {
        tracker.close();
        tracker = null;
    }

    public void bindEmbeddedDataSource(DataSource dataSource, Map<String, Object> serviceProperties) {
        this.embeddedDataSource = dataSource;
        this.embeddedDataSourceProperties = serviceProperties;
    }

    public void unbindEmbeddedDataSource(DataSource dataSource) {
        this.embeddedDataSource = null;
        this.embeddedDataSourceProperties = null;
    }

    public void bindLiquibaseService(LiquibaseService liquibaseService) {
        this.liquibaseService = liquibaseService;
    }

    public void unbindLiquibaseService(LiquibaseService liquibaseService) {
        this.liquibaseService = null;
    }

    public void bindLogService(LogService logService) {
        this.logService = logService;
    }

    public void unbindLogService(LogService logService) {
        this.logService = null;
    }
}
