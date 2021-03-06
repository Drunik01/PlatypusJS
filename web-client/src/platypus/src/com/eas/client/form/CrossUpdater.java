package com.eas.client.form;

import java.util.HashSet;
import java.util.Set;

import com.bearsoft.rowset.events.RowsetAdapter;
import com.bearsoft.rowset.events.RowsetDeleteEvent;
import com.bearsoft.rowset.events.RowsetEvent;
import com.bearsoft.rowset.events.RowsetFilterEvent;
import com.bearsoft.rowset.events.RowsetInsertEvent;
import com.bearsoft.rowset.events.RowsetRequeryEvent;
import com.bearsoft.rowset.events.RowsetRollbackEvent;
import com.bearsoft.rowset.events.RowsetSaveEvent;
import com.bearsoft.rowset.events.RowsetScrollEvent;
import com.eas.client.model.Entity;
import com.google.gwt.core.client.Callback;

public class CrossUpdater extends RowsetAdapter {

	protected Set<Entity> toListenTo = new HashSet<Entity>();
	protected Callback<RowsetEvent, RowsetEvent> onChange;

	public CrossUpdater(Callback<RowsetEvent, RowsetEvent> aOnChange) {
		super();
		onChange = aOnChange;
	}

	public void add(Entity aEntity) {
		if (aEntity != null && !toListenTo.contains(aEntity)) {
			toListenTo.add(aEntity);
			aEntity.getRowset().addRowsetListener(this);
			rowsetRequeried(null);
		}
	}

	public void remove(Entity aEntity) {
		if (aEntity != null) {
			toListenTo.remove(aEntity);
			if (aEntity.getRowset() != null) {
				aEntity.getRowset().removeRowsetListener(this);
			}
		}
	}

	public void die() {
		for (Entity e : toListenTo.toArray(new Entity[] {})) {
			remove(e);
		}
	}

	@Override
	public void rowsetFiltered(RowsetFilterEvent event) {
		onChange.onSuccess(event);
	}

	@Override
	public void rowsetRequeried(RowsetRequeryEvent event) {
		onChange.onSuccess(event);
	}

	@Override
	public void rowsetRolledback(RowsetRollbackEvent event) {
		onChange.onSuccess(event);
	}

	@Override
	public void rowsetSaved(RowsetSaveEvent event) {
	}

	@Override
	public void rowsetScrolled(RowsetScrollEvent event) {
	}

	@Override
	public void rowInserted(RowsetInsertEvent event) {
		if (!event.isAjusting())
			onChange.onSuccess(event);
	}

	@Override
	public void rowDeleted(RowsetDeleteEvent event) {
		if (!event.isAjusting())
			onChange.onSuccess(event);
	}
}
