package org.everit.osgi.liquibase.datasource;

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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.sql.DataSource;

import org.everit.osgi.liquibase.bundle.LiquibaseOSGiUtil;
import org.everit.osgi.liquibase.component.LiquibaseService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;

public class LiquibaseCapabilityTracker extends BundleTracker<Bundle> {

    private final Filter filter;

    private final LinkedHashMap<Bundle, BundleCapability> matchingBundles = new LinkedHashMap<>();

    private Bundle selectedBundle;

    private final DataSource wrappedDataSource;

    private final Map<String, Object> wrappedDataSourceServiceProperties;

    private final LogService logService;

    private final String componentPid;

    private ServiceRegistration<DataSource> dataSourceSR;

    private final String schemaExpression;

    private final LiquibaseService liquibaseService;

    public LiquibaseCapabilityTracker(final BundleContext context, final String schemaExpression,
            final LiquibaseService liquibaseService,
            final DataSource wrappedDataSource, final Map<String, Object> wrappedDataSourceServiceProperties,
            String componentPid,
            final LogService logService) {

        super(context, Bundle.ACTIVE, null);
        this.logService = logService;
        this.liquibaseService = liquibaseService;
        this.filter = LiquibaseOSGiUtil.createFilterForLiquibaseCapabilityAttributes(schemaExpression);
        this.wrappedDataSource = wrappedDataSource;
        this.wrappedDataSourceServiceProperties = wrappedDataSourceServiceProperties;
        this.componentPid = componentPid;
        this.schemaExpression = schemaExpression;
    }

    @Override
    public Bundle addingBundle(Bundle bundle, BundleEvent event) {
        handleBundleChange(bundle);
        return bundle;
    }

    private BundleCapability findMatchingCapabilityInBundle(final Bundle bundle) {
        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        List<BundleCapability> capabilities = bundleWiring.getCapabilities(LiquibaseOSGiUtil.LIQUIBASE_CAPABILITY_NS);
        Iterator<BundleCapability> iterator = capabilities.iterator();
        BundleCapability matchingCapability = null;
        while (matchingCapability == null && iterator.hasNext()) {
            BundleCapability capability = iterator.next();
            Map<String, Object> attributes = capability.getAttributes();
            if (filter.matches(attributes)) {
                matchingCapability = capability;
            }
        }
        return matchingCapability;
    }

    private synchronized void handleBundleChange(Bundle bundle) {
        dropBundle(bundle);

        BundleCapability matchingCapability = findMatchingCapabilityInBundle(bundle);
        if (matchingCapability != null) {
            matchingBundles.put(bundle, matchingCapability);
        }
        if (selectedBundle == null) {
            selectBundleIfNecessary();
        }
    }

    private void dropBundle(Bundle bundle) {
        matchingBundles.remove(bundle);
        if (bundle.equals(selectedBundle)) {
            selectedBundle = null;
            dataSourceSR.unregister();
            dataSourceSR = null;
        }
    }

    private void selectBundleIfNecessary() {
        if (selectedBundle != null) {
            return;
        }
        Set<Entry<Bundle, BundleCapability>> entries = matchingBundles.entrySet();
        Iterator<Entry<Bundle, BundleCapability>> iterator = entries.iterator();
        boolean selected = false;
        while (iterator.hasNext() && !selected) {
            Entry<Bundle, BundleCapability> entry = iterator.next();
            BundleCapability bundleCapability = entry.getValue();
            Map<String, Object> attributes = bundleCapability.getAttributes();
            String resourceName = (String) attributes.get(LiquibaseOSGiUtil.ATTR_SCHEMA_RESOURCE);
            Bundle bundle = entry.getKey();
            try {
                liquibaseService.process(wrappedDataSource, bundle, resourceName);
                selectedBundle = bundle;
                selected = true;
                logService.log(LogService.LOG_INFO, "Successfully migrated database from schema [" + bundle.toString()
                        + " - " + resourceName + "], registering DataSource");

                Hashtable<String, Object> serviceProps = new Hashtable<>(wrappedDataSourceServiceProperties);
                Object wrappedDSServiceId = wrappedDataSourceServiceProperties.get(Constants.SERVICE_ID);
                if (wrappedDSServiceId != null) {
                    serviceProps.put("wrappedDataSource." + Constants.SERVICE_ID, wrappedDSServiceId);
                }

                Object wrappedDSServicePid = wrappedDataSourceServiceProperties.get(Constants.SERVICE_PID);
                if (wrappedDSServicePid != null) {
                    serviceProps.put("wrappedDataSource." + Constants.SERVICE_PID, wrappedDSServicePid);
                }
                serviceProps.put(Constants.SERVICE_PID, componentPid);
                serviceProps.put("liquibase.schema.bundle.id", bundle.getBundleId());
                serviceProps.put("liquibase.schema.bundle.symbolicName", bundle.getSymbolicName());
                serviceProps.put("liquibase.schema.bundle.version", bundle.getVersion().toString());
                Object schemaName = attributes.get(LiquibaseOSGiUtil.ATTR_SCHEMA_NAME);
                serviceProps.put("liquibase.schema.name", schemaName);
                serviceProps.put("liquibase.schema.expression", schemaExpression);
                serviceProps.put("liquibase.schema.resource", resourceName);

                dataSourceSR = super.context.registerService(DataSource.class, wrappedDataSource, serviceProps);
            } catch (RuntimeException e) {
                logService.log(LogService.LOG_ERROR, "Could not update database with schema file " + resourceName
                        + " of bundle " + bundle.toString());
            }
        }
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {
        handleBundleChange(bundle);
    }

    @Override
    public synchronized void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
        dropBundle(bundle);
        selectBundleIfNecessary();
    }

}
