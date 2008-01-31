/**
 * Mogwai ERDesigner. Copyright (C) 2002 The Mogwai Project.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package de.erdesignerng.test;

import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Types;

import de.erdesignerng.dialect.mssql.MSSQLDialect;
import de.erdesignerng.model.Domain;
import de.erdesignerng.model.Model;

/**
 * Test for MySQL dialect.
 * 
 * @author $Author: mirkosertic $
 * @version $Date: 2008-01-31 16:14:37 $
 */
public class MSSQLTest extends BaseUseCases {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        model = new Model();
        model.setDialect(new MSSQLDialect());

        model.setModificationTracker(new MyTracker(model));

        Class.forName("net.sourceforge.jtds.jdbc.Driver");
        connection = DriverManager.getConnection("jdbc:jtds:sqlserver://192.168.0.140:1433/ERDesignerTest",
                "erdesigner", "erdesigner");
    }

    @Override
    public Domain createCharDomain(String aName, int aLength) {
        Domain theDomain = new Domain();
        theDomain.setName(aName);
        // theDomain.setDatatype("varchar");
        theDomain.setDomainSize(aLength);
        return theDomain;
    }

    /**
     * Test extraction of datatypes.
     * @throws Exception is thrown in case of an error
     */
    public void testExtractDataTypes() throws Exception {
        DatabaseMetaData theMeta = connection.getMetaData();
        ResultSet theTypes = theMeta.getTypeInfo();
        while (theTypes.next()) {
            String theTypeName = theTypes.getString("TYPE_NAME");
            int theType = theTypes.getInt("DATA_TYPE");
            String theParams = theTypes.getString("CREATE_PARAMS");
            if (theParams == null) {
                theParams = "";
            }
            if (!(Types.OTHER == theType) && !(Types.ARRAY == theType)) {
                System.out.println("registerType(new MSSQLDataType(\"" + theTypeName + "\",\"" + theParams + "\","
                        + getTypeName(theType) + "));");
            }
        }
    }
}