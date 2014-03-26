package com.eas.client.form.grid;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.bearsoft.rowset.Row;
import com.eas.client.form.factories.GridFactory;
import com.eas.client.model.Entity;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

public class RowsetPositionSelectionHandler implements SelectionHandler<Row> {

	protected Entity rowsSource;

	public RowsetPositionSelectionHandler(Entity aRowsSource) {
		super();
		rowsSource = aRowsSource;
	}

	@Override
	public void onSelection(SelectionEvent<Row> event) {
		if (rowsSource != null && event.getSelectedItem() != null) {
			try {
				rowsSource.scrollTo(event.getSelectedItem());
			} catch (Exception e) {
				Logger.getLogger(GridFactory.class.getName()).log(Level.SEVERE, e.getMessage());
			}
		}
	}

}
