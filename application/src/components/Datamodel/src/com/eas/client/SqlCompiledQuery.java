/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.client;

import com.bearsoft.rowset.Rowset;
import com.bearsoft.rowset.changes.ChangeValue;
import com.bearsoft.rowset.changes.Command;
import com.bearsoft.rowset.dataflow.FlowProvider;
import com.bearsoft.rowset.exceptions.RowsetException;
import com.bearsoft.rowset.metadata.Fields;
import com.bearsoft.rowset.metadata.Parameter;
import com.bearsoft.rowset.metadata.Parameters;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A compiled SQL query.
 *
 * <p>An instance of this class contains JDBC-compliant SQL query text with "?"
 * placeholders for parameters and all parameters values.</p>
 *
 * @author pk, mg
 */
public class SqlCompiledQuery {

    protected DatabasesClient client;
    protected String datasourceName;
    protected String entityId;
    protected String sqlClause;
    protected Parameters parameters;// 1 - Based    
    protected Fields expectedFields;// 1 - Based
    protected FlowProvider flow;
    protected boolean procedure;
    protected Set<String> readRoles = new HashSet<>();
    protected Set<String> writeRoles = new HashSet<>();
    private int pageSize = FlowProvider.NO_PAGING_PAGE_SIZE;

    /**
     * Creates an instance of compiled query.
     *
     * @param aClient
     * @param aSqlClause the SQL query text
     * @throws java.lang.Exception
     */
    public SqlCompiledQuery(DatabasesClient aClient, String aSqlClause) throws Exception {
        super();
        sqlClause = aSqlClause;
        parameters = new Parameters();
        client = aClient;
        createFlow();
    }

    /**
     * Creates an instance of compiled SQL query.
     *
     * <p>This constructor is used when creating a compiled query out of
     * SqlQuery, providing parameters values.</p>
     *
     * @param sql the SQL query text.
     * @param params parameters' values vector.
     */
    SqlCompiledQuery(DatabasesClient aClient, String aSqlClause, Parameters aParams) throws Exception {
        super();
        sqlClause = aSqlClause;
        parameters = aParams;
        client = aClient;
        createFlow();
    }

    SqlCompiledQuery(DatabasesClient aClient, String aDbId, String aSqlClause, Parameters aParams) throws Exception {
        super();
        sqlClause = aSqlClause;
        parameters = aParams;
        datasourceName = aDbId;
        client = aClient;
        createFlow();
    }

    public SqlCompiledQuery(DatabasesClient aClient, String aDbId, String aSqlClause, Parameters aParams, Fields aExpectedFields, Set<String> aReadRoles, Set<String> aWriteRoles) throws Exception {
        super();
        sqlClause = aSqlClause;
        parameters = aParams;
        datasourceName = aDbId;
        expectedFields = aExpectedFields;
        client = aClient;
        readRoles = aReadRoles;
        writeRoles = aWriteRoles;
        createFlow();
    }

    public SqlCompiledQuery(DatabasesClient aClient, String aDbId, String aEntityId, String aSqlClause, Parameters aParams, Fields aExpectedFields, Set<String> aReadRoles, Set<String> aWriteRoles) throws Exception {
        super();
        sqlClause = aSqlClause;
        parameters = aParams;
        datasourceName = aDbId;
        entityId = aEntityId;
        expectedFields = aExpectedFields;
        client = aClient;
        readRoles = aReadRoles;
        writeRoles = aWriteRoles;
        createFlow();
    }
    /**
     * Creates an instance of compiled query.
     *
     * @param aClient
     * @param aDatasourceName Database identifier.
     * @param aSqlClause the SQL query text
     * @throws java.lang.Exception
     */
    public SqlCompiledQuery(DatabasesClient aClient, String aDatasourceName, String aSqlClause) throws Exception {
        super();
        datasourceName = aDatasourceName;
        sqlClause = aSqlClause;
        parameters = new Parameters();
        client = aClient;
        createFlow();
    }

    public boolean isProcedure() {
        return procedure;
    }

    public void setProcedure(boolean aValue) {
        procedure = aValue;
        if (flow != null) {
            flow.setProcedure(procedure);
        }
    }

    public Set<String> getReadRoles() {
        return readRoles;
    }

    public Set<String> getWriteRoles() {
        return writeRoles;
    }

    public void setParameters(Parameters aValue) {
        parameters = aValue;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int aPageSize) throws Exception {
        pageSize = aPageSize;
        createFlow();
    }

    private void createFlow() throws Exception {
        if (client != null) {
            flow = client.createFlowProvider(datasourceName, entityId, sqlClause, expectedFields, readRoles, writeRoles);
            flow.setPageSize(pageSize);
            flow.setProcedure(procedure);
        }
    }

    /**
     * Executes query and returns results whatever setted in procedure flag.
     *
     * @param onSuccess
     * @param onFailure
     * @return Rowset insatance, representing query results.
     * @throws RowsetException
     * @see Rowset
     */
    public Rowset executeQuery(Consumer<Rowset> onSuccess, Consumer<Exception> onFailure) throws Exception {
        Rowset rs = new Rowset(flow);
        rs.refresh(parameters, onSuccess, onFailure);
        /*
        if(expectedFields != rs.getFields())
            refineFields(rs);
            */ 
        return rs;
    }
    
    public Rowset prepareRowset() throws Exception {
        Rowset rowset = new Rowset(flow);
        rowset.setFields(expectedFields);
        return rowset;
    }

    /**
     * Enqueues an update to database or platypus server. Enqueueing
     * occurs when procedure flag is false and when it is true direct executing
     * is performed and commit is called. If procedure flag is not setted and so
     * enqueueing is performed, affected rows count is returned in subsequent
     * call to commit().
     *
     * @throws Exception
     */
    public void enqueueUpdate() throws Exception {
        Command command = new Command(entityId);
        command.command = sqlClause;
        command.parameters = new ChangeValue[parameters.getParametersCount()];
        for (int i = 0; i < command.parameters.length; i++) {
            Parameter param = parameters.get(i + 1);
            command.parameters[i] = new ChangeValue(param.getName(), param.getValue(), param.getTypeInfo());
        }
        flow.getChangeLog().add(command);
    }

    public FlowProvider getFlow() {
        return flow;
    }

    /**
     * Returns the SQL query text.
     *
     * @return the SQL query text.
     */
    public String getSqlClause() {
        return sqlClause;
    }

    public void setSqlClause(String aValue) throws Exception {
        sqlClause = aValue;
        createFlow();
    }

    /**
     * Returns the vector of parameters' values.
     *
     * @return the vector of parameters' values.
     */
    public Parameters getParameters() {
        return parameters;
    }

    /**
     * @return the databaseId
     */
    public String getDatabaseId() {
        return datasourceName;
    }

    public void setDatabaseId(String aValue) throws Exception {
        datasourceName = aValue;
        createFlow();
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String aEntityId) throws Exception {
        entityId = aEntityId;
        createFlow();
    }

}