/* Datamodel license.
 * Exclusive rights on this code in any form
 * are belong to it's author. This code was
 * developed for commercial purposes only. 
 * For any questions and any actions with this
 * code in any form you have to contact to it's
 * author.
 * All rights reserved.
 */

package com.eas.client.model;

import com.bearsoft.rowset.metadata.Field;
import com.bearsoft.rowset.metadata.Parameter;

/**
 * 
 * @author mg
 */
public class Relation {

	// runtime properties
	protected Entity leftEntity;
	protected Field leftField;
	protected Entity rightEntity;
	protected Field rightField;

	public Relation() {
		super();
	}

	public Relation(Entity aLeftEntity, Field aLeftField, Entity aRightEntity, Field aRightField) {
		this();
		leftEntity = aLeftEntity;
		leftField = aLeftField;
		rightEntity = aRightEntity;
		rightField = aRightField;
	}

	public void accept(ModelVisitor visitor) {
		if (visitor != null)
			visitor.visit(this);
	}

	public Entity getLeftEntity() {
		return leftEntity;
	}

	public Entity getRightEntity() {
		return rightEntity;
	}

	public Field getLeftField() {
		return leftField;
	}

	public void setLeftField(Field aValue) {
		leftField = aValue;
	}

	public Field getRightField() {
		return rightField;
	}

	public void setRightField(Field aValue) {
		rightField = aValue;
	}

	public boolean isLeftParameter() {
		return leftField instanceof Parameter;
	}

	public boolean isRightParameter() {
		return rightField instanceof Parameter;
	}

	public boolean isLeftField() {
		return !isLeftParameter();
	}

	public boolean isRightField() {
		return !isRightParameter();
	}

	public void setLeftEntity(Entity aValue) {
		leftEntity = aValue;
	}

	public void setRightEntity(Entity aValue) {
		rightEntity = aValue;
	}

	public Relation copy() {
		Relation copied = new Relation();
		assign(copied);
		return copied;
	}

	protected void assign(Relation copied) {
	    copied.setLeftEntity(leftEntity);
		copied.setLeftField(leftField);
		copied.setRightEntity(rightEntity);
		copied.setRightField(rightField);
    }

	public Parameter getLeftParameter() {
		if (isLeftParameter()) {
			return (Parameter) leftField;
		} else {
			return null;
		}
	}

	public Parameter getRightParameter() {
		if (isRightParameter()) {
			return (Parameter) rightField;
		} else {
			return null;
		}
	}
}
