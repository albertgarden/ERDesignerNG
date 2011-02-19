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
package de.erdesignerng.dialect.postgres;

import de.erdesignerng.ERDesignerBundle;
import de.erdesignerng.dialect.*;
import de.erdesignerng.exception.ReverseEngineeringException;
import de.erdesignerng.model.*;
import de.erdesignerng.modificationtracker.VetoException;
import org.apache.commons.lang.StringUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author $Author: mirkosertic $
 * @version $Date: 2009-03-09 19:07:30 $
 */
public class PostgresReverseEngineeringStrategy extends
        JDBCReverseEngineeringStrategy<PostgresDialect> {

    public PostgresReverseEngineeringStrategy(PostgresDialect aDialect) {
        super(aDialect);
    }

    @Override
    public List<SchemaEntry> getSchemaEntries(Connection aConnection)
            throws SQLException {

        List<SchemaEntry> theList = new ArrayList<SchemaEntry>();

        DatabaseMetaData theMetadata = aConnection.getMetaData();
        ResultSet theResult = theMetadata.getSchemas();

        while (theResult.next()) {
            String theSchemaName = theResult.getString("TABLE_SCHEM");
            String theCatalogName = null;

            theList.add(new SchemaEntry(theCatalogName, theSchemaName));
        }

        return theList;
    }

    // Bug Fixing 2876916 [ERDesignerNG] Reverse-Eng. PgSQL VARCHAR max-length
    // wrong
    @Override
    protected void reverseEngineerAttribute(Attribute aAttribute,
                                            TableEntry aTableEntry,
                                            Connection aConnection) throws SQLException {

        if ((aAttribute.getDatatype().getName().equalsIgnoreCase("varchar"))
                || (aAttribute.getDatatype().getName()
                .equalsIgnoreCase("character varying"))) {
            // PostgreSQL liefert Integer.MAX_VALUE (2147483647), wenn VARCHAR
            // ohne Parameter definiert wurde, obwohl 1073741823 korrekt
            // wäre
            if (new Integer(Integer.MAX_VALUE).equals(aAttribute.getSize())) {
                aAttribute.setSize(null);
            }
        }
    }

    // Bug Fixing 2895202 [ERDesignerNG] RevEng PostgreSQL domains shows
    // VARCHAR(0)
    @Override
    protected void reverseEngineerDomain(Model aModel, Domain aDomain,
                                         ReverseEngineeringOptions aOptions,
                                         ReverseEngineeringNotifier aNotifier, Connection aConnection) {

        if ((aDomain.getConcreteType().getName().equalsIgnoreCase("varchar"))
                || (aDomain.getConcreteType().getName()
                .equalsIgnoreCase("character varying"))) {
            // PostgreSQL liefert 0, wenn VARCHAR ohne Parameter definiert wurde
            if (((Integer) 0).equals(aDomain.getSize())) {
                aDomain.setSize(null);
            }
        }
    }

    // Bug Fixing 2949508 [ERDesignerNG] Rev Eng not handling UDTs in PostgreSQL
    // Bug Fixing 2952877 [ERDesignerNG] Custom Types
    // Bug Fixing 3056071 [ERDesignerNG] postgres unkown datatype prevent reverse eng.
    // TODO: [dr-death] reverse engineere details of custom types and create DDL
    @Override
    protected void reverseEngineerCustomTypes(Model aModel,
                                              ReverseEngineeringOptions aOptions,
                                              ReverseEngineeringNotifier aNotifier, Connection aConnection)
            throws SQLException, ReverseEngineeringException {

        // This query is reverse engineered from the original
        // aConnection.getMetaData().getUDTs() method which unfortunately
        // supports only the typtypes 'c' and 'd'
        // It is altered to support the typtypes 'c' and 'e'
        // The support of typtype 'd' (extended basic types) is removed because
        // it is just another representation of domains.
        // 'c' -> complex datatypes (java.sql.Types.STRUCT)
        // 'd' -> domains (java.sql.Types.DISTINCT)
        // 'e' -> enumerations (java.sql.Types.ARRAY)

        // TODO: [mirkosertic] implement valid way to retrieve type ddl from db
        String theQuery = "SELECT NULL AS type_cat, n.nspname AS type_schem, t.typname AS type_name, NULL AS class_name, CASE WHEN t.typtype = 'c' THEN 2002 WHEN t.typtype = 'e' THEN 2003 ELSE 2001 END AS data_type, pg_catalog.Obj_description(t.oid, 'pg_type') AS remarks, NULL AS base_type " +
                "FROM pg_catalog.pg_type t inner join pg_catalog.pg_namespace n on t.typnamespace = n.oid inner join pg_catalog.pg_authid a on t.typowner = a.oid " +
                "WHERE t.typtype IN ( 'c', 'e' ) AND n.nspname = ? AND a.rolname = 'postgres' " +
                "ORDER BY data_type, type_schem, type_name ";
        PreparedStatement theStatement;

        for (SchemaEntry theEntry : aOptions.getSchemaEntries()) {
            theStatement = aConnection.prepareStatement(theQuery);
            theStatement.setString(1, theEntry.getSchemaName());
            ResultSet theResult = null;

            try {
                theResult = theStatement.executeQuery();
                while (theResult.next()) {
                    String theSchemaName = theResult.getString("TYPE_SCHEM");
                    String theCustomTypeName = theResult.getString("TYPE_NAME");
                    String theRemarks = theResult.getString("REMARKS");

                    aNotifier.notifyMessage(ERDesignerBundle.ENGINEERINGCUSTOMTYPE, theCustomTypeName);

                    CustomType theCustomType = aModel.getCustomTypes().findByNameAndSchema(theCustomTypeName, theSchemaName);
                    if (theCustomType != null) {
                        throw new ReverseEngineeringException("Duplicate custom datatype found : " + theCustomTypeName);
                    }

                    theCustomType = new CustomType();
                    theCustomType.setName(theCustomTypeName);
                    theCustomType.setSchema(theSchemaName);

                    if (!StringUtils.isEmpty(theRemarks)) {
                        theCustomType.setComment(theRemarks);
                    }

                    try {
                        aModel.addCustomType(theCustomType);
                    } catch (VetoException e) {
                        throw new ReverseEngineeringException(e.getMessage(), e);
                    }

                    reverseEngineerCustomType(aModel, theCustomType, aOptions, aNotifier, aConnection);
                }
            } finally {
                if (theResult != null) {
                    theResult.close();
                }

                theStatement.close();
            }
        }
    }

    // Bug Fixing 2949508 [ERDesignerNG] Rev Eng not handling UDTs in PostgreSQL
    // Bug Fixing 2952877 [ERDesignerNG] Custom Types
    @Override
    protected void reverseEngineerCustomType(Model aModel,
                                             CustomType aCustomType, ReverseEngineeringOptions aOptions,
                                             ReverseEngineeringNotifier aNotifier, Connection aConnection) {
        // TODO [mirko sertic]: Grab custom type ddl from information_schema
    }

    @Override
    protected String reverseEngineerViewSQL(TableEntry aViewEntry,
                                            Connection aConnection, View aView) throws SQLException {
        PreparedStatement theStatement = aConnection
                .prepareStatement("SELECT * FROM information_schema.views WHERE table_name = ?");
        theStatement.setString(1, aViewEntry.getTableName());
        ResultSet theResult = null;
        try {
            theResult = theStatement.executeQuery();
            if (theResult.next()) {
                String theViewDefinition = theResult
                        .getString("view_definition");
                theViewDefinition = extractSelectDDLFromViewDefinition(theViewDefinition);
                return theViewDefinition;
            }
            return null;
        } finally {
            if (theResult != null) {
                theResult.close();
            }
            theStatement.close();
        }
    }
}