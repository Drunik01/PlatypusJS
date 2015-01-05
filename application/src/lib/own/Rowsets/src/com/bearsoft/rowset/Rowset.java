/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bearsoft.rowset;

import com.bearsoft.rowset.changes.Change;
import com.bearsoft.rowset.changes.ChangeValue;
import com.bearsoft.rowset.changes.Delete;
import com.bearsoft.rowset.changes.Insert;
import com.bearsoft.rowset.dataflow.DatabaseFlowProvider;
import com.bearsoft.rowset.dataflow.FlowProvider;
import com.bearsoft.rowset.dataflow.JdbcFlowProvider;
import com.bearsoft.rowset.events.RowsetChangeSupport;
import com.bearsoft.rowset.events.RowsetListener;
import com.bearsoft.rowset.exceptions.*;
import com.bearsoft.rowset.ordering.Filter;
import com.bearsoft.rowset.ordering.Locator;
import com.bearsoft.rowset.metadata.Field;
import com.bearsoft.rowset.metadata.Fields;
import com.bearsoft.rowset.metadata.Parameters;
import com.bearsoft.rowset.utils.KeySet;
import com.bearsoft.rowset.utils.RowsetUtils;
import java.beans.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdk.nashorn.api.scripting.JSObject;

/**
 * Rowset serves as original and updated rows vectors holder. There are three
 * developing themes: - rowset's life, with it's data processing (sorting,
 * locating and filtering). - rowset's saving and restoring to and from variety
 * of sources (database, files and others). - applying and rolling back changes,
 * maded to it's data.
 *
 * @author mg
 */
public class Rowset {

    public static final String BAD_FLOW_PROVIDER_RESULT_MSG = "Flow Provider must return at least an empty rowset";

    // rowset's data changes log.
    protected List<Change> log;
    // rowset's data flow.
    protected FlowProvider flow;
    // rowset's metadata
    protected Fields fields;
    // rowset's data
    protected List<Row> original = new ArrayList<>();
    protected List<Row> current = new ArrayList<>();
    // data view capabilities
    protected int currentRowPos; // before first position
    protected boolean showOriginal;
    // data processing
    protected Set<Filter> filters = new HashSet<>(); // filters
    protected Set<Locator> locators = new HashSet<>(); // locators
    protected Filter activeFilter;
    protected Row insertingRow;
    // client code interaction
    protected PropertyChangeSupport propertyChangeSupport;
    protected RowsetChangeSupport rowsetChangeSupport;
    protected Converter converter = new RowsetConverter();
    protected boolean immediateFilter = true;
    protected boolean modified;

    /**
     * Simple constructor.
     */
    public Rowset() {
        super();
        propertyChangeSupport = new PropertyChangeSupport(this);
        rowsetChangeSupport = new RowsetChangeSupport(this);
    }

    /**
     * Rowset's metadata constructor.
     *
     * @param aFields Columns definition new rowset have to work with.
     */
    public Rowset(Fields aFields) {
        this();
        fields = aFields;
    }

    /**
     * Rowset's data flow constructor. Gets a converter from flow provider
     * passed in if flow provider is JdbcFlowProvider.
     *
     * @param aProvider Data flow provider new rowset have to work with.
     * @see DatabaseFlowProvider
     */
    public Rowset(FlowProvider aProvider) {
        this();
        flow = aProvider;
        if (flow instanceof JdbcFlowProvider) {
            converter = ((JdbcFlowProvider) flow).getConverter();
        }
    }

    public List<Change> getLog() {
        return log;
    }

    public void setLog(List<Change> aValue) {
        log = aValue;
    }

    /**
     * Returns the flow provider instance, used by this rowset to support data
     * flow process.
     *
     * @return Current flow provider.
     */
    public FlowProvider getFlowProvider() {
        return flow;
    }

    public void setFlowProvider(FlowProvider aValue) {
        if (flow != aValue) {
            flow = aValue;
            if (flow instanceof JdbcFlowProvider) {
                converter = ((JdbcFlowProvider) flow).getConverter();
            }
        }
    }

    /**
     * Sets the provider to be used in data flow process.
     *
     * @param aFlow Flow provider to set. public void
     * setFlowProvider(FlowProvider aFlow) { flow = aFlow; }
     */
    /**
     * Returns converter, used to convert data from application-specific to
     * source specific and vice versa.
     *
     * @return The converter instance, used by this rowset.
     */
    public Converter getConverter() {
        return converter;
    }

    /**
     * Sets the converter, used to convert data from application-specific to
     * source specific and vice versa.
     *
     * @param aConverter A converter to set.
     * @throws RowsetException
     */
    public void setConverter(Converter aConverter) throws RowsetException {
        converter = aConverter;
        if (converter == null) {
            throw new RowsetException("Converter must present. It can't be null");
        }
    }

    // multi-tier transactions support
    public void commited() throws Exception {
        final Set<RowsetListener> lrowsetListeners = rowsetChangeSupport.getRowsetListeners();
        rowsetChangeSupport.setRowsetListeners(null);
        try {
            currentToOriginal();
        } finally {
            rowsetChangeSupport.setRowsetListeners(lrowsetListeners);
        }
        rowsetChangeSupport.fireSavedEvent();
    }

    public void rolledback() throws Exception {
        final Set<RowsetListener> lrowsetListeners = rowsetChangeSupport.getRowsetListeners();
        rowsetChangeSupport.setRowsetListeners(null);
        try {
            originalToCurrent();
            if (currentRowPos > current.size() + 1) {
                currentRowPos = current.size() + 1;
            } else if (currentRowPos < 0) {
                currentRowPos = 0;
            }
            if (current.isEmpty()) {
                currentRowPos = 0;
            }
        } finally {
            rowsetChangeSupport.setRowsetListeners(lrowsetListeners);
        }
        rowsetChangeSupport.fireRolledbackEvent();
    }

    /**
     * Registers <code>PropertyChangeListener</code> on this rowset.
     *
     * @param aListener <code>PropertyChangeListener</code> to be registered.
     */
    public void addPropertyChangeListener(PropertyChangeListener aListener) {
        propertyChangeSupport.addPropertyChangeListener(aListener);
    }

    public void addPropertyChangeListener(String aPropertyName, PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(aPropertyName, l);
    }

    /**
     * Removes <code>PropertyChangeListener</code> from this rowset.
     *
     * @param aListener <code>PropertyChangeListener</code> to be removed.
     */
    public void removePropertyChangeListener(PropertyChangeListener aListener) {
        propertyChangeSupport.removePropertyChangeListener(aListener);
    }

    public void removePropertyChangeListener(String aPropertyName, PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(aPropertyName, l);
    }

    /**
     * Registers <code>RowsetListener</code> on this rowset.
     *
     * @param aListener <code>RowsetListener</code> to be registered.
     */
    public void addRowsetListener(RowsetListener aListener) {
        rowsetChangeSupport.addRowsetListener(aListener);
    }

    /**
     * Removes <code>RowsetListener</code> from this rowset.
     *
     * @param aListener <code>RowsetListener</code> to be removed.
     */
    public void removeRowsetListener(RowsetListener aListener) {
        rowsetChangeSupport.removeRowsetListener(aListener);
    }

    /**
     * Inner utility method for filters and some others.
     *
     * @return <code>RowsetChangeSupport</code> instance from this rowset.
     */
    public RowsetChangeSupport getRowsetChangeSupport() {
        return rowsetChangeSupport;
    }

    /**
     * Field getter.
     *
     * @return Columns definition of this rowset.
     */
    public Fields getFields() {
        return fields;
    }

    /**
     * Field setter.
     *
     * @param aFields Fields instance.
     * @throws com.bearsoft.rowset.exceptions.InvalidFieldsExceptionException
     */
    public void setFields(Fields aFields) throws InvalidFieldsExceptionException {
        if (!current.isEmpty()) {
            Row row = current.get(0);
            assert row != null;
            if (row.getColumnCount() != aFields.getFieldsCount()) {
                throw new InvalidFieldsExceptionException(String.format("column count is wrong, expected: %d, but %d is got", row.getColumnCount(), aFields.getFieldsCount()));
            }
        } else if (!original.isEmpty()) {
            Row row = original.get(0);
            assert row != null;
            if (row.getColumnCount() != aFields.getFieldsCount()) {
                throw new InvalidFieldsExceptionException(String.format("column count is wrong, expected: %d, but %d is got", row.getColumnCount(), aFields.getFieldsCount()));
            }
        }
        Fields oldValue = fields;
        fields = aFields;
        propertyChangeSupport.firePropertyChange("fields", oldValue, fields);
    }

    /**
     * Returns active (current) filter of this rowset. May be null.
     *
     * @return Active (current) filter of this rowset. May be null.
     */
    public Filter getActiveFilter() {
        return activeFilter;
    }

    /**
     * Sets active (current) filter of this rowset. For internal use only.
     *
     * @param aValue Filter to be setted as active (current) filter of this
     * rowset. May be null.
     */
    public void setActiveFilter(Filter aValue) {
        activeFilter = aValue;
    }

    /**
     * Retruns this rowset's modified status.
     *
     * @return True if this rowses have changes since last currentToOriginal()
     * and originalToCurrent() method calls
     * @see #currentToOriginal()
     * @see #originalToCurrent()
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Sets the modified flag for this rowset. It's not recomended to use this
     * method, but in some cases it may be useful.
     *
     * @param aValue
     */
    public void setModified(boolean aValue) {
        modified = aValue;
    }

    /**
     * Returns weither installed filter have to immediatly refilter this rowset
     * after updating a field which is filtering criteria.
     *
     * @return True is filter have to refilter thia rowset immediatly after
     * updating a field which is filtering criteria.
     */
    public boolean isImmediateFilter() {
        return immediateFilter;
    }

    /**
     * Sets immediateFilter flag for this rowset.
     *
     * @param aValue
     * @see #isImmediateFilter()
     */
    public void setImmediateFilter(boolean aValue) {
        if (immediateFilter != aValue) {
            boolean oldValue = immediateFilter;
            immediateFilter = aValue;
            propertyChangeSupport.firePropertyChange("immediateFilter", oldValue, immediateFilter);
        }
    }

    public void reverse() throws InvalidCursorPositionException {
        if (rowsetChangeSupport.fireWillSortEvent()) {
            Collections.reverse(current);
            rowsetChangeSupport.fireSortedEvent();
        }
    }

    public void refresh(Consumer<Rowset> onSuccess, Consumer<Exception> onFailure) throws Exception {
        refresh(new Parameters(), onSuccess, onFailure);
    }

    /**
     * Queries some source for data, according to supplied parameters values. It
     * queries data using flow provider installed on this rowset instance. It
     * fires RowsetRequeriedEvent event. Call to refresh() will uninstall any
     * installed filter and invalidate other filters, have been created on this
     * rowset.
     *
     * @param aParams Parameters values, ordered with some unknown criteria.
     * @param onSuccess
     * @param onFailure
     * @throws java.lang.Exception
     * @see Parameters
     */
    public void refresh(Parameters aParams, Consumer<Rowset> onSuccess, Consumer<Exception> onFailure) throws Exception {
        if (flow != null) {
            if (rowsetChangeSupport.fireWillRequeryEvent()) {
                if (onSuccess != null) {
                    rowsetChangeSupport.fireBeforeRequeryEvent();
                    flow.refresh(aParams, (Rowset aRowset) -> {
                        if (aRowset != null) {
                            try {
                                if (activeFilter != null && activeFilter.isApplied()) {
                                    activeFilter.deactivate(); // No implicit calls to setCurrent and etc.
                                    activeFilter = null;
                                }
                                if (fields == null) {
                                    setFields(aRowset.getFields());
                                }
                                List<Row> rows = aRowset.getCurrent();
                                aRowset.setCurrent(new ArrayList<>());
                                aRowset.currentToOriginal();
                                setCurrent(rows);
                                rows.stream().forEach((Row aRow) -> {
                                    aRow.setLog(log);
                                });
                                currentToOriginal();
                                // silent first
                                if (!current.isEmpty()) {
                                    currentRowPos = 1;
                                }
                                rowsetChangeSupport.fireRequeriedEvent();
                                try {
                                    onSuccess.accept(this);
                                } catch (Exception ex) {
                                    Logger.getLogger(Rowset.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            } catch (Exception ex) {
                                rowsetChangeSupport.fireNetErrorEvent(ex);
                                if (onFailure != null) {
                                    onFailure.accept(ex);
                                }
                            }
                        } else {
                            if (onFailure != null) {
                                onFailure.accept(new FlowProviderFailedException(BAD_FLOW_PROVIDER_RESULT_MSG));
                            }
                        }
                    }, (Exception ex) -> {
                        rowsetChangeSupport.fireNetErrorEvent(ex);
                        if (onFailure != null) {
                            onFailure.accept(ex);
                        }
                    });
                } else {
                    Rowset rowset = flow.refresh(aParams, null, null);
                    if (rowset != null) {
                        if (activeFilter != null && activeFilter.isApplied()) {
                            activeFilter.deactivate(); // No implicit calls to setCurrent and etc.
                            activeFilter = null;
                        }
                        if (fields == null) {
                            setFields(rowset.getFields());
                        }
                        List<Row> rows = rowset.getCurrent();
                        rowset.setCurrent(new ArrayList<>());
                        rowset.currentToOriginal();
                        setCurrent(rows);
                        rows.stream().forEach((Row aRow) -> {
                            aRow.setLog(log);
                        });
                        currentToOriginal();
                        // silent first
                        if (!current.isEmpty()) {
                            currentRowPos = 1;
                        }
                        rowsetChangeSupport.fireRequeriedEvent();
                    } else {
                        throw new FlowProviderFailedException(BAD_FLOW_PROVIDER_RESULT_MSG);
                    }
                }
            }
        } else {
            throw new MissingFlowProviderException();
        }
    }

    /**
     * Continues scrolling and reading data from underlying result set event
     * jdbc or not It achievies data using flow provider installed on this
     * rowset instance. It fires RowsetNextPageEvent event. Call to nextPage()
     * will uninstall any installed filter and invalidate other filters, have
     * been created on this rowset.
     *
     * @param onSuccess
     * @param onFailure
     * @return Status of the next page fetching. True if some data have been
     * fetched, false otherwise.
     * @throws java.lang.Exception
     * @see RowsetNextPageEvent
     */
    public boolean nextPage(Consumer<Boolean> onSuccess, Consumer<Exception> onFailure) throws Exception {
        if (flow != null) {
            if (flow.getPageSize() <= size()) {
                if (rowsetChangeSupport.fireWillNextPageEvent()) {
                    if (onSuccess != null) {
                        flow.nextPage((Rowset aRowset) -> {
                            if (aRowset != null) {
                                assert fields != null : "Fields is missing. Method nextPage must not be called as the first method while retrieving data, and so, fields must already present.";
                                int fetched = aRowset.getCurrent().size();
                                if (fetched > 0) {
                                    List<Row> rows = aRowset.getCurrent();
                                    aRowset.setCurrent(new ArrayList<>());
                                    aRowset.currentToOriginal();
                                    setCurrent(rows);
                                    rows.stream().forEach((Row aRow) -> {
                                        aRow.setLog(log);
                                    });
                                    currentToOriginal();
                                    rowsetChangeSupport.fireNextPageFetchedEvent();
                                    onSuccess.accept(true);
                                } else {
                                    onSuccess.accept(false);
                                }
                            } else {
                                if (onFailure != null) {
                                    onFailure.accept(new FlowProviderFailedException(BAD_FLOW_PROVIDER_RESULT_MSG));
                                }
                            }
                        }, onFailure);
                        return false;
                    } else {
                        Rowset rowset = flow.nextPage(null, null);
                        if (rowset != null) {
                            assert fields != null : "Fields is missing. Method nextPage must not be called as the first method while retrieving data, and so, fields must already present.";
                            int fetched = rowset.getCurrent().size();
                            if (fetched > 0) {
                                List<Row> rows = rowset.getCurrent();
                                rowset.setCurrent(new ArrayList<>());
                                rowset.currentToOriginal();
                                setCurrent(rows);
                                rows.stream().forEach((Row aRow) -> {
                                    aRow.setLog(log);
                                });
                                currentToOriginal();
                                rowsetChangeSupport.fireNextPageFetchedEvent();
                                return true;
                            } else {
                                return false;
                            }
                        } else {
                            throw new FlowProviderFailedException(BAD_FLOW_PROVIDER_RESULT_MSG);
                        }
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            throw new MissingFlowProviderException();
        }
    }

    /**
     * Returns current rows vector. Used with filtering classes.
     *
     * @return Current rows vector.
     * @see HashOrderer
     */
    public List<Row> getCurrent() {
        return current;
    }

    /**
     * Returns original rows vector.
     *
     * @return Original rows vector.
     */
    public List<Row> getOriginal() {
        return original;
    }

    /**
     * Sets current rows vector. Unsubscribes from old rows events and sub
     * subscribes on new rows events.
     *
     * @param aCurrent Current rows list.
     * @see HashOrderer
     * @see Filter
     * @see Locator
     */
    public void setCurrent(List<Row> aCurrent) {
        assert fields != null;
        current = aCurrent;
        currentRowPos = 0;
    }

    /**
     * Moves cursor on pre first position. Cusor position becomes 0.
     *
     * @return
     * @throws com.bearsoft.rowset.exceptions.InvalidCursorPositionException
     * @see #absolute(int aCursorPos)
     * @see #getCursorPos()
     * @see #size()
     * @see #first()
     * @see #isBeforeFirst()
     * @see #previous()
     * @see #next()
     * @see #last()
     * @see #afterLast()
     * @see #isAfterLast()
     */
    public boolean beforeFirst() throws InvalidCursorPositionException {
        if (!isBeforeFirst()) {
            if (rowsetChangeSupport.fireWillScrollEvent(0)) {
                int oldCurrentRowPos = currentRowPos;
                currentRowPos = 0;
                rowsetChangeSupport.fireScrolledEvent(oldCurrentRowPos);
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * Returns if cursor is before the first row. Takes into account
     * <code>showOriginal</code> flag
     *
     * @return True if the cursor is on before first position.
     * @see #absolute(int aCursorPos)
     * @see #getCursorPos()
     * @see #size()
     * @see #beforeFirst()
     * @see #first()
     * @see #previous()
     * @see #next()
     * @see #last()
     * @see #afterLast()
     * @see #isAfterLast()
     */
    public boolean isBeforeFirst() {
        assert currentRowPos >= 0;
        return isEmpty() || currentRowPos == 0;
    }

    /**
     * Moves cursor to the first position in the rowset. It won't to position
     * the rowset if it is empty. After that, position becomes 1 if this method
     * returns true. If this method returns false, than position remains
     * unchnaged. Takes into account <code>showOriginal</code> flag
     *
     * @return True if rowset is on the first position, and false if it is not.
     * @throws com.bearsoft.rowset.exceptions.InvalidCursorPositionException
     * @see #absolute(int aCursorPos)
     * @see #getCursorPos()
     * @see #size()
     * @see #beforeFirst()
     * @see #isBeforeFirst()
     * @see #previous()
     * @see #next()
     * @see #last()
     * @see #afterLast()
     * @see #isAfterLast()
     */
    public boolean first() throws InvalidCursorPositionException {
        if (!isEmpty()) {
            if (currentRowPos != 1) {
                if (rowsetChangeSupport.fireWillScrollEvent(1)) {
                    int oldCurrentRowPos = currentRowPos;
                    currentRowPos = 1;
                    rowsetChangeSupport.fireScrolledEvent(oldCurrentRowPos);
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Moves cursor to the last position in the rowset. It won't to position the
     * rowset if it is empty. After that, position equals to rows count if this
     * method returns true. If this method returns false, than position remains
     * unchnaged. Takes into account <code>showOriginal</code> flag
     *
     * @return True if rowset is on the last position, and false if it is not.
     * @throws com.bearsoft.rowset.exceptions.InvalidCursorPositionException
     * @see #absolute(int aCursorPos)
     * @see #getCursorPos()
     * @see #size()
     * @see #beforeFirst()
     * @see #first()
     * @see #isBeforeFirst()
     * @see #previous()
     * @see #next()
     * @see #afterLast()
     * @see #isAfterLast()
     */
    public boolean last() throws InvalidCursorPositionException {
        if (!isEmpty()) {
            if (currentRowPos != size()) {
                if (rowsetChangeSupport.fireWillScrollEvent(size())) {
                    int oldCurrentRowPos = currentRowPos;
                    currentRowPos = size();
                    rowsetChangeSupport.fireScrolledEvent(oldCurrentRowPos);
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Moves cursor to the after last position in the rowset. It won't to
     * position the rowset if it is empty. After positioning, position equals to
     * rows count+1 if this method returns true. If this method returns false,
     * than position remains 0. Takes into account <code>showOriginal</code>
     * flag
     *
     * @return True if has been positioned, and false if it hasn't.
     * @throws com.bearsoft.rowset.exceptions.InvalidCursorPositionException
     * @see #absolute(int aCursorPos)
     * @see #getCursorPos()
     * @see #size()
     * @see #beforeFirst()
     * @see #first()
     * @see #isBeforeFirst()
     * @see #previous()
     * @see #next()
     * @see #last()
     * @see #isAfterLast()
     */
    public boolean afterLast() throws InvalidCursorPositionException {
        if (!isAfterLast()) {
            if (rowsetChangeSupport.fireWillScrollEvent(size() + 1)) {
                int oldCurrentRowPos = currentRowPos;
                currentRowPos = size() + 1;
                rowsetChangeSupport.fireScrolledEvent(oldCurrentRowPos);
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * Returns if cursor is after last row. Takes into account
     * <code>showOriginal</code> flag
     *
     * @return True if the cursor is on after last position.
     * @see #absolute(int aCursorPos)
     * @see #getCursorPos()
     * @see #size()
     * @see #beforeFirst()
     * @see #first()
     * @see #isBeforeFirst()
     * @see #previous()
     * @see #next()
     * @see #last()
     * @see #afterLast()
     */
    public boolean isAfterLast() {
        return isEmpty() || currentRowPos > size();
    }

    /**
     * Moves the cursor one position forward. Takes into account
     * <code>showOriginal</code> flag
     *
     * @return True if new position is on the next row. False if the rowset is
     * empty or cursor becomes after last position. In this case cusor is moved,
     * but method returns false.
     * @throws com.bearsoft.rowset.exceptions.InvalidCursorPositionException
     * @see #absolute(int aCursorPos)
     * @see #getCursorPos()
     * @see #size()
     * @see #beforeFirst()
     * @see #first()
     * @see #isBeforeFirst()
     * @see #previous()
     * @see #last()
     * @see #afterLast()
     * @see #isAfterLast()
     */
    public boolean next() throws InvalidCursorPositionException {
        if (!isEmpty()) {
            if (currentRowPos < size() + 1) {
                if (currentRowPos < size()) {
                    if (rowsetChangeSupport.fireWillScrollEvent(currentRowPos + 1)) {
                        int oldCurrentRowPos = currentRowPos;
                        currentRowPos++;
                        rowsetChangeSupport.fireScrolledEvent(oldCurrentRowPos);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    if (rowsetChangeSupport.fireWillScrollEvent(currentRowPos + 1)) {
                        int oldCurrentRowPos = currentRowPos;
                        currentRowPos++;
                        rowsetChangeSupport.fireScrolledEvent(oldCurrentRowPos);
                    }
                    return false;
                }
            }
        } else {
            assert currentRowPos == 0;
        }
        return false;
    }

    /**
     * Moves the cursor one position backward. Takes into account
     * <code>showOriginal</code> flag
     *
     * @return True if new position is on the previous row. False if the rowset
     * is empty or cursor becomes before first position. In this case cusor is
     * moved, but method returns false.
     * @throws com.bearsoft.rowset.exceptions.InvalidCursorPositionException
     * @see #absolute(int aCursorPos)
     * @see #getCursorPos()
     * @see #size()
     * @see #beforeFirst()
     * @see #first()
     * @see #isBeforeFirst()
     * @see #next()
     * @see #last()
     * @see #afterLast()
     * @see #isAfterLast()
     */
    public boolean previous() throws InvalidCursorPositionException {
        if (!isEmpty()) {
            if (currentRowPos > 0) {
                if (currentRowPos > 1) {
                    if (rowsetChangeSupport.fireWillScrollEvent(currentRowPos - 1)) {
                        int oldCurrentRowPos = currentRowPos;
                        currentRowPos--;
                        rowsetChangeSupport.fireScrolledEvent(oldCurrentRowPos);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    if (rowsetChangeSupport.fireWillScrollEvent(currentRowPos - 1)) {
                        int oldCurrentRowPos = currentRowPos;
                        currentRowPos--;
                        rowsetChangeSupport.fireScrolledEvent(oldCurrentRowPos);
                    }
                    return false;
                }
            } else {
                return false;
            }
        } else {
            assert currentRowPos == 0;
        }
        return false;
    }

    /**
     * Returns rows count in this rowset. Takes into account
     * <code>showOriginal</code> flag
     *
     * @return Rows count in this rowset.
     * @see #absolute(int aCursorPos)
     * @see #getCursorPos()
     * @see #beforeFirst()
     * @see #first()
     * @see #isBeforeFirst()
     * @see #previous()
     * @see #next()
     * @see #last()
     * @see #afterLast()
     * @see #isAfterLast()
     */
    public int size() {
        if (showOriginal) {
            return original.size();
        } else {
            return current.size();
        }
    }

    /**
     * Checks whether cusor is in the valid position. If not than the
     * <code>InvalidCursorPositionException</code> is thrown.
     *
     * @throws InvalidCursorPositionException
     */
    protected void checkCursor() throws InvalidCursorPositionException {
        if (!isInserting()) {
            if (currentRowPos < 1) {
                throw new InvalidCursorPositionException("currentRowPos < 1");
            }
            if (currentRowPos > size()) {
                throw new InvalidCursorPositionException("currentRowPos > current.size()");
            }
        }
    }

    /**
     * Checks whether cusor is in the valid position, including before first and
     * after last position. If not than the
     * <code>InvalidCursorPositionException</code> is thrown.
     *
     * @throws InvalidCursorPositionException
     */
    public void wideCheckCursor() throws InvalidCursorPositionException {
        if (currentRowPos < 0) {
            throw new InvalidCursorPositionException("currentRowPos < 0. Before before first posotion is illegal");
        }
        if (currentRowPos > size() + 1) {
            throw new InvalidCursorPositionException("currentRowPos > current.size()+1. After after last position is illegal");
        }
    }

    /**
     * Checks whether index of column is valid for this rowset's
     * <code>Fields</code>
     *
     * @param aColIndex Index of particular column.
     * @throws InvalidColIndexException
     */
    protected void checkColIndex(int aColIndex) throws InvalidColIndexException {
        assert fields != null;
        Field field = fields.get(aColIndex);
        if (field == null) {
            throw new InvalidColIndexException(String.format("%d have been passed as aColIndex parameter. But it had to be >= 1 and <= %d", aColIndex, fields.getFieldsCount()));
        }
    }

    /**
     * Returns current cursor position in this rowset.
     *
     * @return Current cursor position in this rowset.
     * @see #absolute(int aCursorPos)
     * @see #size()
     * @see #beforeFirst()
     * @see #first()
     * @see #isBeforeFirst()
     * @see #previous()
     * @see #next()
     * @see #last()
     * @see #afterLast()
     * @see #isAfterLast()
     */
    public int getCursorPos() {
        return currentRowPos;
    }

    /**
     * Positions the rowset cursor on the specified row number. Row number is
     * 1-based.
     *
     * @param aCursorPos Cursor position you whant to be setted in this rowset.
     * @return True if cursor position in rowset equals to aCursorPos.
     * @throws InvalidCursorPositionException
     * @see #getCursorPos()
     * @see #size()
     * @see #beforeFirst()
     * @see #first()
     * @see #isBeforeFirst()
     * @see #previous()
     * @see #next()
     * @see #last()
     * @see #afterLast()
     * @see #isAfterLast()
     */
    public boolean absolute(int aCursorPos) throws InvalidCursorPositionException {
        if (!isEmpty()) {
            if (aCursorPos >= 1 && aCursorPos <= size()) {
                if (aCursorPos != currentRowPos) {
                    if (rowsetChangeSupport.fireWillScrollEvent(aCursorPos)) {
                        int oldCurrentRowPos = currentRowPos;
                        currentRowPos = aCursorPos;
                        rowsetChangeSupport.fireScrolledEvent(oldCurrentRowPos);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Returns a row by ordinal number. Row number is 1 - based.
     *
     * @param aRowNumber Row number you whant to be used to locate the row.
     * @return Row if speciified row number is valid, null otherwise.
     * @see #getCursorPos()
     * @see #size()
     * @see #beforeFirst()
     * @see #first()
     * @see #isBeforeFirst()
     * @see #previous()
     * @see #next()
     * @see #last()
     * @see #afterLast()
     * @see #isAfterLast()
     */
    public Row getRow(int aRowNumber) {
        if (!isEmpty()) {
            if (aRowNumber >= 1 && aRowNumber <= size()) {
                if (showOriginal) {
                    return original.get(aRowNumber - 1);
                } else {
                    return current.get(aRowNumber - 1);
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Returns whether this rowset is empty.
     *
     * @return Whether this rowset is empty.
     */
    public boolean isEmpty() {
        if (showOriginal) {
            return original.isEmpty();
        } else {
            return current.isEmpty();
        }
    }

    /**
     * Simple insert method. Inserts a new <code>Row</code> in this rowset in
     * both original and current rows vectors. Initialization with current
     * filter values is performed.
     *
     * @throws com.bearsoft.rowset.exceptions.RowsetException
     */
    public void insert() throws RowsetException {
        insert(new Object[]{});
    }

    /**
     * Simple insert method. Inserts a new <code>Row</code> in this rowset in
     * both original and current rows arrays. First, filter's values are used
     * for initialization, than <code>initingValues</code> specified is used.
     * Takes into account <code>showOriginal</code> flag. If
     * <code>showOriginal</code> flag is setted, than no action is performed.
     *
     * @param initingValues Values inserting row to be initialized with.
     * @throws RowsetException
     */
    public void insert(Object... initingValues) throws RowsetException {
        if (!showOriginal) {
            assert fields != null;
            Row row = new Row(flow.getEntityId(), fields);
            insert(row, false, initingValues);
        }
    }

    /**
     * Simple insert method. Inserts a new <code>Row</code> in this rowset in
     * both original and current rows arrays. First, filter's values are used
     * for initialization, than <code>initingValues</code> specified is used.
     * Takes into account <code>showOriginal</code> flag. If
     * <code>showOriginal</code> flag is setted, than no action is performed.
     *
     * @param insertAt
     * @param aAjusting
     * @param initingValues Values inserting row to be initialized with.
     * @return
     * @throws RowsetException
     */
    public Row insertAt(int insertAt, boolean aAjusting, Object... initingValues) throws RowsetException {
        if (!showOriginal) {
            assert fields != null;
            Row row = new Row(flow.getEntityId(), fields);
            insertAt(row, aAjusting, insertAt, initingValues);
            return row;
        }
        return null;
    }

    /**
     * Collections - like insert method. Inserts a passed <code>Row</code> in
     * this rowset in both original and current rows vectors. Initialization
     * with current filter values is performed.
     *
     * @param toInsert A row to insertt in the rowset.
     * @param aAjusting
     * @throws RowsetException
     */
    public void insert(Row toInsert, boolean aAjusting) throws RowsetException {
        insert(toInsert, aAjusting, new Object[]{});
    }

    /**
     * Row insert method. Inserts a passed <code>Row</code> in this rowset in
     * both original and current rows arrays. First, filter's values are used
     * for initialization, than <code>initingValues</code> specified is used.
     * Takes into account <code>showOriginal</code> flag. If
     * <code>showOriginal</code> flag is setted, than no action is performed.
     *
     * @param toInsert A row to insert in the rowset.
     * @param aAjusting Flag, indicating that inserting is within a batch
     * operation
     * @param initingValues Values inserting row to be initialized with.
     * @throws RowsetException
     */
    public void insert(Row toInsert, boolean aAjusting, Object... initingValues)
            throws RowsetException {
        int insertAtPosition;
        if (isBeforeFirst()) {
            insertAtPosition = 1;
        } else if (isAfterLast()) {
            insertAtPosition = currentRowPos;
        } else {
            insertAtPosition = currentRowPos + 1;
        }
        insertAt(toInsert, aAjusting, insertAtPosition, initingValues);
    }

    /**
     * Row insert method. Inserts a passed <code>Row</code> in this rowset in
     * both original and current rows arrays. First, filter's values are used
     * for initialization, than <code>initingValues</code> specified is used.
     * Takes into account <code>showOriginal</code> flag. If
     * <code>showOriginal</code> flag is setted, than no action is performed.
     *
     * @param toInsert A row to insert in the rowset.
     * @param aAjusting Flag, indicating that inserting is within a batch
     * operation
     * @param insertAt Index the new row to be added at. 1-Based.
     * @param initingValues Values inserting row to be initialized with.
     * @throws RowsetException
     */
    public void insertAt(Row toInsert, boolean aAjusting, int insertAt, Object... initingValues) throws RowsetException {
        if (!showOriginal) {
            assert fields != null;
            if (toInsert == null) {
                throw new RowsetException("Bad inserting row. It must be non null value.");
            }
            if (toInsert.getColumnCount() != fields.getFieldsCount()) {
                throw new RowsetException("Bad column count. While inserting, columns count in a row must same with fields count in rowset fields.");
            }
            toInsert.setLog(log);
            insertingRow = toInsert;
            try {
                if (rowsetChangeSupport.fireWillInsertEvent(insertingRow, aAjusting)) {
                    initColumns(insertingRow, initingValues);
                    insertingRow.setInserted();
                    // work on current rows vector, probably filtered
                    current.add(insertAt - 1, insertingRow);
                    currentRowPos = insertAt;
                    original.add(insertingRow);
                    Row insertedRow = insertingRow;
                    modified = true;
                    generateInsert(insertedRow);
                    rowsetChangeSupport.fireRowInsertedEvent(insertedRow, aAjusting);
                }
            } finally {
                insertingRow = null;
            }
        }
    }

    /**
     * Returns whether rowset is in inserting a new row state.
     *
     * @return Whether rowset is inserting a row.
     */
    public boolean isInserting() {
        return insertingRow != null;
    }

    /**
     * Initializes new row with supplied initialization values. Than initializes
     * it with active filter values.
     *
     * @param aRow A <code>Row</code> to initialize.
     * @param values Values the specified <code>Row</code> to initialize with.
     * @throws com.bearsoft.rowset.exceptions.RowsetException
     */
    protected void initColumns(Row aRow, Object... values) throws RowsetException {
        if (aRow != null) {
            // key fields generation
            for (int i = 1; i <= fields.getFieldsCount(); i++) {
                Field field = fields.get(i);
                if (field.isPk() && aRow.getColumnObject(i) == null) {
                    Object pkValue = RowsetUtils.generatePkValueByType(field.getTypeInfo().getSqlType());
                    if (converter != null) {
                        pkValue = converter.convert2RowsetCompatible(pkValue, field.getTypeInfo());
                    }
                    aRow.setColumnObject(i, pkValue);
                }
            }
            // user supplied fields, including values for primary keys
            if (values != null && values.length > 0 && Math.IEEEremainder(values.length, 2.0f) == 0.0f) {
                for (int i = 0; i < values.length - 1; i += 2) {
                    if (values[i] != null && (values[i] instanceof Integer || values[i] instanceof Double || values[i] instanceof Field || values[i] instanceof String)) {
                        int colIndex;
                        if (values[i] instanceof String) {
                            colIndex = fields.find((String) values[i]);
                        } else if (values[i] instanceof Field) {
                            Field field = (Field) values[i];
                            colIndex = fields.find(field.getName());
                        } else {
                            colIndex = values[i] instanceof Integer ? (Integer) values[i] : (int) Math.round((Double) values[i]);
                        }
                        Field field = fields.get(colIndex);
                        if (field != null) {
                            Object fieldValue = values[i + 1];
                            if (converter != null) {
                                fieldValue = converter.convert2RowsetCompatible(fieldValue, field.getTypeInfo());
                            }
                            aRow.setColumnObject(colIndex, fieldValue);
                        }
                    }
                }
            }
            // filtered values to correponding fields, excluding key fields
            if (activeFilter != null) {
                List<Integer> filterCriteriaFields = activeFilter.getFields();
                KeySet ks = activeFilter.getKeysetApplied();
                assert filterCriteriaFields != null;
                assert ks != null;
                assert filterCriteriaFields.size() == ks.size();
                for (int i = 0; i < filterCriteriaFields.size(); i++) {
                    int colIndex = filterCriteriaFields.get(i);
                    Field field = fields.get(colIndex);
                    // do not touch key fields!
                    if (!field.isPk()) {
                        Object fieldValue = ks.get(i);
                        if (fieldValue == RowsetUtils.UNDEFINED_SQL_VALUE) {
                            fieldValue = null;
                        }
                        if (converter != null) {
                            fieldValue = converter.convert2RowsetCompatible(fieldValue, field.getTypeInfo());
                        }
                        aRow.setColumnObject(colIndex, fieldValue);
                    }
                }
            }
        }
    }

    protected boolean isFilteringCriteria(int aFieldIndex) {
        if (activeFilter != null) {
            List<Integer> fIdxes = activeFilter.getFields();
            if (fIdxes != null && fIdxes.contains(aFieldIndex)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns if <code>showOriginal</code> flag is set.
     *
     * @return True if showOriginal flag is set.
     */
    public boolean isShowOriginal() {
        return showOriginal;
    }

    /**
     * Sets <code>showOriginal</code> flag to this rowset.
     *
     * @param aShowOriginal Flag, indicating this rowset show original rows
     * vector.
     */
    public void setShowOriginal(boolean aShowOriginal) {
        showOriginal = aShowOriginal;
        if (currentRowPos < 0) {
            currentRowPos = 0;
        }
        if (currentRowPos > size() + 1) {
            currentRowPos = size() + 1;
        }
    }

    /**
     * Deletes current row. It means current row is marked as deleted and
     * removed from cuurent rows vector. If cursor is not on the valid position
     * no action is performed. Takes into account <code>showOriginal</code>
     * flag. If <code>showOriginal</code> flag is setted, than no action is
     * performed. If <code>showOriginal</code> flag setted, than no action is
     * performed.
     *
     * @see #delete(java.util.Set)
     * @see #deleteAll()
     * @throws RowsetException
     */
    public void delete() throws RowsetException {
        if (!showOriginal) {
            checkCursor();
            Row row = getCurrentRow();
            assert row != null;
            if (rowsetChangeSupport.fireWillDeleteEvent(row)) {
                row.setDeleted();
                generateDelete(row);
                current.remove(currentRowPos - 1);
                if (!isEmpty()) {
                    if (isBeforeFirst()) {
                        currentRowPos = 1;
                    }
                    if (isAfterLast()) {
                        currentRowPos = current.size();
                    }
                } else {
                    currentRowPos = 0;
                }
                modified = true;
                rowsetChangeSupport.fireRowDeletedEvent(row);
            }
        }
    }

    /**
     * Deletes all rows in the rowset. Rows are marked as deleted and removed
     * from cuurent rows vector. After deleting, cursor position becomes invalid
     * and both <code>isBeforeFirst()</code> and <code>isAfterLast()</code> must
     * return true. Subsequent calls to this method perform no action. Takes
     * into account <code>showOriginal</code> flag. If <code>showOriginal</code>
     * flag setted, than no action is performed.
     *
     * @see #isBeforeFirst()
     * @see #isAfterLast()
     * @see #delete()
     * @see #delete(java.util.Set)
     * @throws RowsetException
     */
    public void deleteAll() throws RowsetException {
        if (!showOriginal) {
            /**
             * The following approach is very harmful! If any events listener
             * whould like to veto the deletion, than the cycle never ends.
             * while(!isEmpty()) delete();
             */
            boolean wasBeforeFirst = isBeforeFirst();
            boolean wasAfterLast = isAfterLast();
            for (int i = current.size() - 1; i >= 0; i--) {
                Row row = current.get(i);
                assert row != null;
                if (rowsetChangeSupport.fireWillDeleteEvent(row, i != 0)) { // last iteration will fire non-ajusting event
                    row.setDeleted();
                    generateDelete(row);
                    current.remove(i);
                    modified = true;
                    currentRowPos = i + 1;
                    rowsetChangeSupport.fireRowDeletedEvent(row, i != 0); // last iteration will fire non-ajusting event
                    currentRowPos = Math.min(currentRowPos, current.size());
                }
            }
            if (current.isEmpty()) {
                currentRowPos = 0;
            } else {
                if (wasBeforeFirst) {
                    currentRowPos = 0;
                }
                if (wasAfterLast) {
                    currentRowPos = size() + 1;
                }
                if (!wasBeforeFirst && !wasAfterLast && currentRowPos > size()) {
                    currentRowPos = size();
                }
            }
            wideCheckCursor();
        }
    }

    /**
     * Deletes specified rows from the rowset. Rows are marked as deleted and
     * removed from cuurent rows vector. After deleting, cursor position becomes
     * invalid and rowset may be repositioned.
     *
     * @param aRows2Delete Set of rows to be deleted from the rowset
     * @see #isBeforeFirst()
     * @see #isAfterLast()
     * @see #delete()
     * @see #deleteAll()
     * @throws RowsetException
     */
    public void delete(Set<Row> aRows2Delete) throws RowsetException {
        if (!showOriginal) {
            Set<Row> rows2Delete = new HashSet<>();
            rows2Delete.addAll(aRows2Delete);
            boolean wasBeforeFirst = isBeforeFirst();
            boolean wasAfterLast = isAfterLast();
            for (int i = current.size() - 1; i >= 0; i--) {
                Row row = current.get(i);
                assert row != null;
                if (rows2Delete.contains(row)) {
                    rows2Delete.remove(row);
                    if (rowsetChangeSupport.fireWillDeleteEvent(row, !rows2Delete.isEmpty())) { // last iteration will fire non-ajusting event
                        row.setDeleted();
                        generateDelete(row);
                        current.remove(i);
                        modified = true;
                        currentRowPos = i + 1;
                        rowsetChangeSupport.fireRowDeletedEvent(row, !rows2Delete.isEmpty()); // last iteration will fire non-ajusting event
                        currentRowPos = Math.min(currentRowPos, current.size());
                    }
                }
            }
            if (current.isEmpty()) {
                currentRowPos = 0;
            } else {
                if (wasBeforeFirst) {
                    currentRowPos = 0;
                }
                if (wasAfterLast) {
                    currentRowPos = size() + 1;
                }
                if (!wasBeforeFirst && !wasAfterLast && currentRowPos > size()) {
                    currentRowPos = size();
                }
            }
            wideCheckCursor();
        }
    }

    /**
     * Deletes specified rows from the rowset. Rows are marked as deleted and
     * removed from cuurent rows vector. After deleting, cursor position becomes
     * invalid and rowset may be repositioned.
     *
     * @param aRowIndex Index of row to be deleted from the rowset. aRowIndex is
     * 1-based.
     * @see #isBeforeFirst()
     * @see #isAfterLast()
     * @see #delete()
     * @see #deleteAll()
     * @throws RowsetException
     */
    public void deleteAt(int aRowIndex) throws RowsetException {
        deleteAt(aRowIndex, false);
    }

    public void deleteAt(int aRowIndex, boolean aIsAjusting) throws RowsetException {
        if (!showOriginal) {
            boolean wasBeforeFirst = isBeforeFirst();
            boolean wasAfterLast = isAfterLast();
            Row row = current.get(aRowIndex - 1);
            assert row != null;
            if (rowsetChangeSupport.fireWillDeleteEvent(row, aIsAjusting)) { // the deletion will fire non-ajusting event
                row.setDeleted();
                generateDelete(row);
                current.remove(aRowIndex - 1);
                modified = true;
                currentRowPos = aRowIndex;
                rowsetChangeSupport.fireRowDeletedEvent(row, aIsAjusting); // the deletion will fire non-ajusting event
                currentRowPos = Math.min(currentRowPos, current.size());
            }
            if (current.isEmpty()) {
                currentRowPos = 0;
            } else {
                if (wasBeforeFirst) {
                    currentRowPos = 0;
                }
                if (wasAfterLast) {
                    currentRowPos = size() + 1;
                }
                if (!wasBeforeFirst && !wasAfterLast && currentRowPos > size()) {
                    currentRowPos = size();
                }
            }
            wideCheckCursor();
        }
    }

    /**
     * Returns value of particular field of current row by index of column.
     *
     * @param colIndex Index of particular field.
     * @return Value of perticular field of current row by index of column.
     * @throws InvalidColIndexException
     * @throws InvalidCursorPositionException
     * @see #updateObject(int colIndex, Object aValue)
     */
    public Object getObject(int colIndex) throws InvalidColIndexException, InvalidCursorPositionException {
        checkCursor();
        checkColIndex(colIndex);
        Row row = getCurrentRow();
        assert row != null;
        if (showOriginal) {
            return row.getOriginalColumnObject(colIndex);
        } else {
            return row.getColumnObject(colIndex);
        }
    }

    /**
     * Returns value of particular field of current row by index of column as
     * string.
     *
     * @param colIndex Index of particular field.
     * @return Value of perticular field of current row by index of column as
     * string.
     * @throws InvalidColIndexException
     * @throws InvalidCursorPositionException
     * @see #updateObject(int colIndex, Object aValue)
     * @see #getObject(int colIndex)
     */
    public String getString(int colIndex) throws InvalidColIndexException, InvalidCursorPositionException {
        return (String) getObject(colIndex);
    }

    /**
     * Returns value of particular field of current row by index of column as
     * integer number.
     *
     * @param colIndex Index of particular field.
     * @return Value of perticular field of current row by index of column as
     * integer number.
     * @throws InvalidColIndexException
     * @throws InvalidCursorPositionException
     * @see #updateObject(int colIndex, Object aValue)
     * @see #getObject(int colIndex)
     */
    public Integer getInt(int colIndex) throws InvalidColIndexException, InvalidCursorPositionException {
        Object value = getObject(colIndex);
        if (value instanceof Integer) {
            return (Integer) getObject(colIndex);
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else {
            return null;
        }
    }

    /**
     * Returns value of particular field of current row by index of column as
     * date.
     *
     * @param colIndex Index of particular field.
     * @return Value of perticular field of current row by index of column as
     * date.
     * @throws InvalidColIndexException
     * @throws InvalidCursorPositionException
     * @see #updateObject(int colIndex, Object aValue)
     * @see #getObject(int colIndex)
     */
    public Date getDate(int colIndex) throws InvalidColIndexException, InvalidCursorPositionException {
        return (Date) getObject(colIndex);
    }

    /**
     * Returns value of particular field of current row by index of column as
     * double number.
     *
     * @param colIndex Index of particular field.
     * @return Value of perticular field of current row by index of column as
     * double number.
     * @throws InvalidColIndexException
     * @throws InvalidCursorPositionException
     * @see #updateObject(int colIndex, Object aValue)
     * @see #getObject(int colIndex)
     */
    public Double getDouble(int colIndex) throws InvalidColIndexException, InvalidCursorPositionException {
        Object value = getObject(colIndex);
        if (value instanceof Double) {
            return (Double) getObject(colIndex);
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            return null;
        }
    }

    /**
     * Returns value of particular field of current row by index of column as
     * float number.
     *
     * @param colIndex Index of particular field.
     * @return Value of perticular field of current row by index of column as
     * float number.
     * @throws InvalidColIndexException
     * @throws InvalidCursorPositionException
     * @see #updateObject(int colIndex, Object aValue)
     * @see #getObject(int colIndex)
     */
    public Float getFloat(int colIndex) throws InvalidColIndexException, InvalidCursorPositionException {
        Object value = getObject(colIndex);
        if (value instanceof Float) {
            return (Float) getObject(colIndex);
        } else if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else {
            return null;
        }
    }

    /**
     * Returns value of particular field of current row by index of column as
     * boolean value.
     *
     * @param colIndex Index of particular field.
     * @return Value of perticular field of current row by index of column as
     * boolean value.
     * @throws InvalidColIndexException
     * @throws InvalidCursorPositionException
     * @see #updateObject(int colIndex, Object aValue)
     * @see #getObject(int colIndex)
     */
    public Boolean getBoolean(int colIndex) throws InvalidColIndexException, InvalidCursorPositionException {
        return (Boolean) getObject(colIndex);
    }

    /**
     * Returns value of particular field of current row by index of column as
     * short number.
     *
     * @param colIndex Index of particular field.
     * @return Value of perticular field of current row by index of column as
     * short number.
     * @throws InvalidColIndexException
     * @throws InvalidCursorPositionException
     * @see #updateObject(int colIndex, Object aValue)
     * @see #getObject(int colIndex)
     */
    public Short getShort(int colIndex) throws InvalidColIndexException, InvalidCursorPositionException {
        Object value = getObject(colIndex);
        if (value instanceof Short) {
            return (Short) getObject(colIndex);
        } else if (value instanceof Number) {
            return ((Number) value).shortValue();
        } else {
            return null;
        }
    }

    /**
     * Returns value of particular field of current row by index of column as
     * long number.
     *
     * @param colIndex Index of particular field.
     * @return Value of perticular field of current row by index of column as
     * long number.
     * @throws InvalidColIndexException
     * @throws InvalidCursorPositionException
     * @see #updateObject(int colIndex, Object aValue)
     * @see #getObject(int colIndex)
     */
    public Long getLong(int colIndex) throws InvalidColIndexException, InvalidCursorPositionException {
        Object value = getObject(colIndex);
        if (value instanceof Long) {
            return (Long) getObject(colIndex);
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else {
            return null;
        }
    }

    /**
     * Returns <code>Row</code> at current cursor position. Doesn't perform
     * current position check, so it has to be called internally.
     *
     * @return <code>Row</code> at current cursor position.
     */
    public Row getCurrentRow() {
        if (insertingRow != null) {
            return insertingRow;
        } else if (!isEmpty() && !isBeforeFirst() && !isAfterLast()) {
            if (showOriginal) {
                return original.get(currentRowPos - 1);
            } else {
                return current.get(currentRowPos - 1);
            }
        } else {
            return null;
        }
    }

    /**
     * Applies modifications maded to this rowset. After that no difference
     * between original and current rows vectors and row's data have place.
     */
    public void currentToOriginal() {
        original.clear();
        List<Row> lcurrent;
        if (activeFilter != null && activeFilter.isApplied()) {
            lcurrent = activeFilter.getOriginalRows();
        } else {
            lcurrent = current;
        }
        original.addAll(lcurrent);
        for (int i = original.size() - 1; i >= 0; i--) {
            Row row = original.get(i);
            assert row != null;
            row.currentToOriginal();
            row.clearInserted();
            if (row.isDeleted()) {// Should never happen. Added for code strength in case of mark and sweep row deletion.
                original.remove(i);
                lcurrent.remove(i);
            }
        }
        modified = false;
    }

    /**
     * Cancels modifications maded to this rowset. After that no difference
     * between original and current rows vectors and row's data have place.
     *
     * @throws com.bearsoft.rowset.exceptions.RowsetException
     */
    public void originalToCurrent() throws RowsetException {
        Filter wasFilter = activeFilter;
        boolean wasApplied = false;
        if (wasFilter != null) {
            wasApplied = wasFilter.isApplied();
            wasFilter.cancelFilter();
        }
        try {
            current.clear();
            current.addAll(original);
            for (int i = current.size() - 1; i >= 0; i--) {
                Row row = current.get(i);
                assert row != null;
                row.originalToCurrent();
                if (row.isInserted()) {
                    current.remove(i);
                    original.remove(i);
                }
                row.clearDeleted();
            }
            modified = false;
        } finally {
            if (wasFilter != null && wasApplied) {
                wasFilter.refilterRowset();// implicit setCurrent() call.
            }
        }
    }

    protected void generateInsert(Row aRow) {
        if (flow != null && log != null) {
            Insert insert = new Insert(flow.getEntityId());
            List<ChangeValue> data = new ArrayList<>();
            for (int i = 0; i < aRow.getCurrentValues().length; i++) {
                Field field = aRow.getFields().get(i + 1);
                Object value = aRow.getCurrentValues()[i];
                if (value != null) {
                    data.add(new ChangeValue(field.getName(), value, field.getTypeInfo()));
                }
            }
            insert.data = data.toArray(new ChangeValue[]{});
            log.add(insert);
            aRow.setInserted(insert);
        }
    }

    protected void generateDelete(Row aRow) {
        if (flow != null && log != null) {
            Delete delete = new Delete(flow.getEntityId());
            delete.keys = Row.generateChangeLogKeys(-1, aRow, null);
            log.add(delete);
        }
    }

    /**
     * Creates and returns new filter based on this rowset.
     *
     * @return New filter based on this rowset.
     */
    public Filter createFilter() {
        Filter hf = new Filter(this);
        filters.add(hf);
        return hf;
    }

    /**
     * Creates and returns new locator based on this rowset.
     *
     * @return New locator based on this rowset.
     */
    public Locator createLocator() {
        Locator hl = new Locator(this);
        locators.add(hl);
        return hl;
    }

    /**
     * Removes specified filter from this rowset.
     *
     * @param aFilter Locator object to be removed.
     * @throws RowsetException
     */
    public void removeFilter(Filter aFilter) throws RowsetException {
        if (aFilter == activeFilter) {
            activeFilter.cancelFilter();
        }
        aFilter.die();
        filters.remove(aFilter);
    }

    /**
     * Removes specified locator from this rowset.
     *
     * @param aLocator Locator object to be removed.
     */
    public void removeLocator(Locator aLocator) {
        aLocator.die();
        locators.remove(aLocator);
    }

    /**
     * Service method to sort current vector of rows.
     *
     * @param aComparator Comparator to use while sorting rows.
     * @throws InvalidCursorPositionException
     */
    public void sort(Comparator<Row> aComparator) throws InvalidCursorPositionException {
        if (aComparator != null) {
            if (rowsetChangeSupport.fireWillSortEvent()) {
                Collections.sort(current, aComparator);
                rowsetChangeSupport.fireSortedEvent();
            }
        }
    }

    /**
     * Returns array of locators installed on this rowset.
     *
     * @return Array of locators installed on this rowset.
     */
    public Locator[] getLocators() {
        Locator[] res = new Locator[locators.size()];
        return locators.toArray(res);
    }

    /**
     * Returns array of filters installed on this rowset.
     *
     * @return Array of filters installed on this rowset.
     */
    public Filter[] getFilters() {
        Filter[] res = new Filter[filters.size()];
        return filters.toArray(res);
    }

    public static List<JSObject> toJs(List<Row> aRows) {
        List<JSObject> jses = new ArrayList<>();
        aRows.forEach((Row aRow) -> {
            jses.add(aRow.getPublished());
        });
        return jses;
    }
}
