/*
 * IronJacamar, a Java EE Connector Architecture implementation
 * Copyright 2008, Red Hat Inc, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.jca.common.api.metadata.ds;

import org.jboss.jca.common.api.metadata.common.CommonPool;

import java.util.Map;

/**
 * A DataSource.
 *
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public interface DataSource extends CommonDataSource
{
   /**
    * Get the JTA setting.
    * @return The value
    */
   public Boolean isJTA();

   /**
    * Get the connectionUrl.
    *
    * @return the connectionUrl.
    */
   public String getConnectionUrl();

   /**
    * Get the driverClass.
    *
    * @return the driverClass.
    */
   public String getDriverClass();

   /**
    * Get the dataSourceClass.
    *
    * @return the value.
    */
   public String getDataSourceClass();

   /**
    * Get the connectionProperties.
    *
    * @return the connectionProperties.
    */
   public Map<String, String> getConnectionProperties();

   /**
    * Get the statement.
    *
    * @return the statement.
    */
   public Statement getStatement();

   /**
    * Get the urlDelimiter.
    *
    * @return the urlDelimiter.
    */
   public String getUrlDelimiter();

   /**
    * Get the urlSelectorStrategyClassName.
    *
    * @return the urlSelectorStrategyClassName.
    */
   public String getUrlSelectorStrategyClassName();

   /**
    * Get the newConnectionSql.
    *
    * @return the newConnectionSql.
    */
   public String getNewConnectionSql();

   /**
    * Get the pool.
    *
    * @return the pool.
    */
   public CommonPool getPool();
}
