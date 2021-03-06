/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.client.application;

import java.util.ArrayList;
import java.util.List;

import com.bearsoft.rowset.Cancellable;
import com.bearsoft.rowset.Rowset;
import com.bearsoft.rowset.changes.Change;
import com.bearsoft.rowset.dataflow.FlowProvider;
import com.bearsoft.rowset.metadata.Fields;
import com.bearsoft.rowset.metadata.Parameters;
import com.google.gwt.core.client.Callback;

/**
 * 
 * @author mg
 */
public class WebFlowProvider implements FlowProvider {

	protected String entityId;
	protected Fields expectedFields;
	protected AppClient client;
	protected boolean procedure;
	protected List<Change> changeLog = new ArrayList<Change>();

	public WebFlowProvider(AppClient aClient, String aEntityId, Fields aExpectedFields) {
		client = aClient;
		entityId = aEntityId;
		expectedFields = aExpectedFields;
	}

	@Override
	public String getEntityId() {
		return entityId;
	}

	@Override
	public Fields getExpectedFields() {
		return expectedFields;
	}

	@Override
	public Cancellable refresh(Parameters aParams, Callback<Rowset, String> aCallback) throws Exception {
		return client.requestData(entityId, aParams, expectedFields, aCallback);
	}

	@Override
	public List<Change> getChangeLog() {
		return changeLog;
	}
}
