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
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;

public class LiquiCapabilityTracker extends BundleTracker<Bundle> {

    private final Map<Filter, List<LiquibaseCapability>> matchingBundlesByFilters;

    private final LogService logService;

    public LiquiCapabilityTracker(final BundleContext context, final String[] capabilityFilters,
            final LogService logService) {

        super(context, Bundle.ACTIVE, null);
        this.logService = logService;

        matchingBundlesByFilters = new HashMap<Filter, List<LiquibaseCapability>>();

        for (String capabilityFilter : capabilityFilters) {
            try {
                Filter filter = context.createFilter(capabilityFilter);
                matchingBundlesByFilters.put(filter, new ArrayList<LiquibaseCapability>());
            } catch (InvalidSyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public Bundle addingBundle(Bundle bundle, BundleEvent event) {
        
        return null;
    }

    private LiquibaseCapability findMatchingCapability(String schemaName, Filter filter, Bundle bundle) {
        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        List<BundleCapability> schemaCapabilities = bundleWiring
                .getCapabilities(Constants.CAPABILITY_NAMESPACE);
        LiquibaseCapability matchingCapability = null;
        Iterator<BundleCapability> capabilityIterator = schemaCapabilities.iterator();

        while (matchingCapability == null && capabilityIterator.hasNext()) {
            BundleCapability bundleCapability = capabilityIterator.next();
            matchingCapability = createCapabilityIfMatches(schemaName, filter, bundleCapability);
        }
        return matchingCapability;
    }

    private LiquibaseCapability createCapabilityIfMatches(String schemaName, Filter filter,
            BundleCapability bundleCapability) {

        Map<String, Object> capabilityAttributes = bundleCapability.getAttributes();

        Object schemaNameAttr = capabilityAttributes.get(Constants.CAPABILITY_ATTR_SCHEMA_NAME);
        if (schemaName.equals(schemaNameAttr)) {
            if (filter.matches(capabilityAttributes)) {
                Object schemaResourceAttr = capabilityAttributes.get(Constants.CAPABILITY_ATTR_SCHEMA_RESOURCE);
                Bundle bundle = bundleCapability.getRevision().getBundle();
                if (schemaResourceAttr != null) {
                    return new LiquibaseCapability(bundle, schemaName, schemaResourceAttr.toString());
                } else {
                    logService.log(LogService.LOG_ERROR, "Liquibase schema '" + schemaName + "' matches with filter '"
                            + filter.toString() + "' but no schemaResource attribute found. Capability is provided"
                            + " by the following bundle: " + bundle);
                }
            }
        }
        return null;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
        // TODO Auto-generated method stub

    }

}
