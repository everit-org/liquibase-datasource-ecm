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

import java.util.Map;

import javax.sql.DataSource;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.everit.osgi.liquibase.component.LiquibaseService;
import org.osgi.service.log.LogService;

@Component(metatype = true, immediate = true)
@Properties({
        @Property(name = "capabilityFilters", unbounded = PropertyUnbounded.ARRAY),
        @Property(name = "embeddedDataSource.target"), @Property(name = "liquibaseService.target"),
        @Property(name = "logService.target")
})
public class LiquibaseDataSourceComponent {

    @Reference
    private DataSource embeddedDataSource;

    @Reference
    private LiquibaseService liquibaseService;

    @Reference
    private LogService logService;

    @Activate
    public void activate(final Map<String, Object> componentProperties) {
    }
}
