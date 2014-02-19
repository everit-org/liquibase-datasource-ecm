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
public class ConfigurationException extends RuntimeException {

    /**
     * .
     */
    private static final long serialVersionUID = 3088041736762438601L;

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }
}
