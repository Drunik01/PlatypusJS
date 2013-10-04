package com.eas.client.gxtcontrols.wrappers.handled;

import com.eas.client.gxtcontrols.ControlsUtils;
import com.eas.client.gxtcontrols.model.ModelElementRef;
import com.eas.client.gxtcontrols.published.PublishedCell;
import com.eas.client.gxtcontrols.wrappers.component.PlatypusAdapterField;
import com.eas.client.gxtcontrols.wrappers.component.PlatypusAdapterStandaloneField;
import com.eas.client.gxtcontrols.wrappers.component.PlatypusTextArea;
import com.google.gwt.core.client.JavaScriptObject;

public class PlatypusTextHandledArea extends PlatypusTextArea {

	protected PlatypusTextHandledAreaInputCell textCell;

	public PlatypusTextHandledArea(PlatypusTextHandledAreaInputCell aCell) {
		super(aCell);
		textCell = aCell;
		textCell.setContainer(this);
	}

	public JavaScriptObject getCellFunction() {
		return textCell.getCellFunction();
	}

	public void setCellFunction(JavaScriptObject aValue) {
		if (aValue != textCell.getCellFunction()) {
			textCell.setCellFunction(aValue);
			redraw();
		}
	}

	public ModelElementRef getModelElement() {
		return textCell.getModelElement();
	}

	public void setModelElement(ModelElementRef aValue) {
		textCell.setModelElement(aValue);
	}

	@Override
	public void setValue(String value, boolean fireEvents, boolean redraw) {
		super.setValue(value, fireEvents, redraw);
		if (!redraw && textCell != null && textCell.getCellFunction() != null)
			redraw();
	}

	@Override
	protected void onRedraw() {
		super.onRedraw();
		PublishedCell lastPublishedCell = textCell.getPublishedCell();
		if (lastPublishedCell != null) {
			lastPublishedCell.styleToElement(getInputEl());
		} else {
			// TODO: refactor to onTargetRedraw event
			if (getParent() != null && getParent().getParent() instanceof PlatypusAdapterStandaloneField<?>) {
				PlatypusAdapterField<?> adapter = (PlatypusAdapterStandaloneField<?>) getParent().getParent();
				ControlsUtils.reapplyStyle(adapter);
			}
		}
	}
}
