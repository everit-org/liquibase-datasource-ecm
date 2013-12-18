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

    public LiquiCapabilityTracker(final BundleContext context, final String[] schemaFilters,
            final LogService logService) {

        super(context, Bundle.ACTIVE, null);
        this.logService = logService;

        matchingBundlesByFilters = new HashMap<Filter, List<LiquibaseCapability>>();

        for (String schemaFilter : schemaFilters) {
            try {
                Filter filter = parseFilter(context, schemaFilter);
                matchingBundlesByFilters.put(filter, new ArrayList<LiquibaseCapability>());
            } catch (InvalidSyntaxException e) {
                throw new ConfigurationException("Invalid syntax in the filter expression of LiquiDataSource component"
                        + schemaFilter);
            }
        }
    }

    private Filter parseFilter(BundleContext context, String filterString) throws InvalidSyntaxException {
        int indexOfSemicolon = filterString.indexOf(';');
        String schemaName = filterString;
        String schemaFilterPart = null;
        if (indexOfSemicolon != -1) {
            schemaName = filterString.substring(0, indexOfSemicolon);
            if (filterString.length() > indexOfSemicolon + 1) {
                String filterPart = filterString.substring(indexOfSemicolon + 1).trim();
                int fpl = filterPart.length();
                if (fpl > 0 && fpl < 4 && !filterPart.startsWith("filter:=\"") && filterPart.charAt(fpl - 1) != '"') {
                    throw new ConfigurationException(
                            "Could not analyze the expression for liquibase capability selection in the configuration: "
                                    + filterString);
                }
                schemaFilterPart = filterPart.substring("filter:=".length(), fpl - 1).trim();
                if (schemaFilterPart.length() == 0) {
                    schemaFilterPart = null;
                }
            }
        }

        String schemaNameFilter = "(" + Constants.CAPABILITY_ATTR_SCHEMA_NAME + "=" + schemaName + ")";

        StringBuilder resultSB = new StringBuilder();
        if (schemaFilterPart != null) {
            resultSB.append("(&");
        }
        resultSB.append(schemaNameFilter);
        if (schemaFilterPart != null) {
            resultSB.append(schemaFilterPart).append(")");
        }
        return context.createFilter(resultSB.toString());
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

    /**
     * @return The matching capability or null if it was not matching.
     */
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
