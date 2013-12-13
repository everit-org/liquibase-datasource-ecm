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

import org.osgi.framework.Bundle;

public class LiquibaseCapability {

    private final Bundle bundle;

    private final String schemaName;

    private final String schemaResource;

    public LiquibaseCapability(final Bundle bundle, String schemaName, String schemaFile) {
        this.schemaName = schemaName;
        this.schemaResource = schemaFile;
        this.bundle = bundle;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getSchemaResource() {
        return schemaResource;
    }
}
