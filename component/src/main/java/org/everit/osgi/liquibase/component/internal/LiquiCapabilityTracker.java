package org.everit.osgi.liquibase.component.internal;

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

import java.util.ArrayList;
import java.util.HashMap;
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
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;

public class LiquiCapabilityTracker extends BundleTracker<Bundle> {

    private final Filter filter;

    private final LinkedHashMap<Bundle, BundleCapability> matchingBundles = new LinkedHashMap<>();

    private Bundle selectedBundle;

    private final DataSource wrappedDataSource;

    private final LogService logService;

    private ServiceRegistration<DataSource> dataSourceSR;

    private final LiquibaseService liquibaseService;

    public LiquiCapabilityTracker(final BundleContext context, final String schemaExpression,
            final LiquibaseService liquibaseService, final DataSource wrappedDataSource, final LogService logService) {

        super(context, Bundle.ACTIVE, null);
        this.logService = logService;
        this.liquibaseService = liquibaseService;
        this.filter = LiquibaseOSGiUtil.createFilterForLiquibaseCapabilityAttributes(schemaExpression);
        this.wrappedDataSource = wrappedDataSource;
    }

    @Override
    public Bundle addingBundle(Bundle bundle, BundleEvent event) {
        handleBundleChange(bundle);
        return bundle;
    }

    private void handleBundleChange(Bundle bundle) {
        dropBundle(bundle);
        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        List<BundleCapability> capabilities = bundleWiring.getCapabilities(LiquibaseOSGiUtil.LIQUIBASE_CAPABILITY_NS);
        for (BundleCapability capability : capabilities) {
            Map<String, Object> attributes = capability.getAttributes();
            if (filter.matches(attributes)) {

            } else {

            }
        }
    }

    private void dropBundle(Bundle bundle) {
        matchingBundles.remove(bundle);
        if (bundle.equals(selectedBundle)) {
            selectedBundle = null;
            dataSourceSR.unregister();
            dataSourceSR = null;
            selectBundleFromAvailables();
        }
    }

    private void selectBundleFromAvailables() {
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
                liquibaseService.process(wrappedDataSource, bundle.getBundleContext(), resourceName);
                selectedBundle = bundle;
                selected = true;
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
    public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
        dropBundle(bundle);
    }

}
