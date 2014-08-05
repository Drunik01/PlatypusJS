package com.eas.client.form.grid;

import com.bearsoft.gwt.ui.widgets.grid.builders.ThemedCellTableBuilder;
import com.eas.client.form.published.PublishedStyle;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.user.cellview.client.AbstractCellTable;

public class RenderedTableCellBuilder<T> extends ThemedCellTableBuilder<T> {

	public RenderedTableCellBuilder(AbstractCellTable<T> cellTable, String aDynamicTDClassName, String aDynamicCellClassName, String aDynamicOddRowsClassName, String aDynamicEvenRowsClassName) {
		super(cellTable, aDynamicTDClassName, aDynamicCellClassName, aDynamicOddRowsClassName, aDynamicEvenRowsClassName);
	}

	@Override
	protected Cell.Context createCellContext(int aIndex, int aColumn, Object aKey) {
		return new RenderedCellContext(aIndex, aColumn, aKey);
	}

	@Override
	protected void tdGenerated(TableCellBuilder aTd, Cell.Context aContext) {
		if (aContext instanceof RenderedCellContext) {
			PublishedStyle pStyle = ((RenderedCellContext) aContext).getStyle();
			if (pStyle != null && pStyle.getBackground() != null) {
				aTd.style().trustedBackgroundColor(pStyle.getBackground().toStyled());
			} else {
				super.tdGenerated(aTd, aContext);
			}
		} else {
			super.tdGenerated(aTd, aContext);
		}
	}
}