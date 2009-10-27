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
package de.erdesignerng.visual.common;

import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang.ArrayUtils;

import de.erdesignerng.ERDesignerBundle;
import de.erdesignerng.dialect.Dialect;
import de.erdesignerng.dialect.SQLGenerator;
import de.erdesignerng.dialect.Statement;
import de.erdesignerng.dialect.StatementList;
import de.erdesignerng.model.Attribute;
import de.erdesignerng.model.Index;
import de.erdesignerng.model.ModelItem;
import de.erdesignerng.model.Relation;
import de.erdesignerng.model.Table;
import de.erdesignerng.model.View;
import de.erdesignerng.modificationtracker.VetoException;
import de.mogwai.common.client.looks.UIInitializer;
import de.mogwai.common.client.looks.components.DefaultPanel;
import de.mogwai.common.client.looks.components.DefaultTextArea;
import de.mogwai.common.i18n.ResourceHelper;
import de.mogwai.common.i18n.ResourceHelperProvider;

public class SQLComponent extends DefaultPanel implements ResourceHelperProvider {

    private DefaultTextArea sql;

    private static SQLComponent DEFAULT;

    public static SQLComponent initializeComponent() {
        DEFAULT = new SQLComponent();
        return DEFAULT;
    }

    public static SQLComponent getDefault() {
        if (DEFAULT == null) {
            throw new RuntimeException("Component is not initialized");
        }
        return DEFAULT;
    }

    private SQLComponent() {
        initialize();
    }

    private void initialize() {

        setLayout(new BorderLayout());
        sql = new DefaultTextArea();
        sql.setEditable(false);
        add(sql.getScrollPane(), BorderLayout.CENTER);

        UIInitializer.getInstance().initialize(this);
    }

    /**
     * Reset the SQL display.
     */
    public void resetDisplay() {
        sql.setText("");
    }

    /**
     * Display the CREATE SQL Statements for a given set of model items.
     * 
     * @param aModelItems
     *            a set of model items
     */
    public void displaySQLFor(ModelItem[] aModelItems) {
        try {
            resetDisplay();
            
            Dialect theDialect = ERDesignerComponent.getDefault().getModel().getDialect();
            if (theDialect != null && !ArrayUtils.isEmpty(aModelItems)) {
                StatementList theStatementList = new StatementList();
                SQLGenerator theGenerator = theDialect.createSQLGenerator();
                for (ModelItem aItem : aModelItems) {
                    if (aItem instanceof Table) {
                        theStatementList.addAll(theGenerator.createAddTableStatement((Table) aItem));
                    }
                    if (aItem instanceof View) {
                        theStatementList.addAll(theGenerator.createAddViewStatement((View) aItem));
                    }
                    if (aItem instanceof Relation) {
                        theStatementList.addAll(theGenerator.createAddRelationStatement((Relation) aItem));
                    }
                    if (aItem instanceof Attribute) {
                        Attribute theAttribute = (Attribute) aItem;
                        theStatementList.addAll(theGenerator.createAddAttributeToTableStatement(
                                theAttribute.getOwner(), theAttribute));
                    }
                    if (aItem instanceof Index) {
                        Index theIndex = (Index) aItem;
                        theStatementList.addAll(theGenerator.createAddIndexToTableStatement(theIndex.getOwner(),
                                theIndex));
                    }
                }

                if (theStatementList.size() > 0) {
                    StringWriter theWriter = new StringWriter();
                    PrintWriter thePW = new PrintWriter(theWriter);
                    for (Statement theStatement : theStatementList) {
                        thePW.print(theStatement.getSql());
                        thePW.println(theGenerator.createScriptStatementSeparator());
                    }
                    thePW.flush();
                    thePW.close();
                    sql.setText(theWriter.toString());
                }
            } else {
                if (theDialect == null) {
                    sql.setText(getResourceHelper().getText(ERDesignerBundle.PLEASEDEFINEADATABASECONNECTIONFIRST));
                }
            }
        } catch (VetoException e) {
            throw new RuntimeException("Unexpected error", e);
        }
    }

    @Override
    public ResourceHelper getResourceHelper() {
        return ResourceHelper.getResourceHelper(ERDesignerBundle.BUNDLE_NAME);
    }
}