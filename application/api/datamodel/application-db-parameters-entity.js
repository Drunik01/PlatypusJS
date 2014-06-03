(function() {
    var javaClass = Java.type("com.eas.client.model.application.ApplicationDbParametersEntity");
    javaClass.setPublisher(function(aDelegate) {
        return new P.ApplicationDbParametersEntity(aDelegate);
    });
    
    /**
     * Generated constructor.
     * @namespace ApplicationDbParametersEntity
     */
    P.ApplicationDbParametersEntity = function () {

        var maxArgs = 0;
        var delegate = arguments.length > maxArgs ?
            arguments[maxArgs] : new javaClass();

        Object.defineProperty(this, "unwrap", {
            get: function() {
                return function() {
                    return delegate;
                };
            }
        });
        /**
        * Gets the row at cursor position.
        * @return the row object or <code>null</code> if cursor is before first or after last position.
         * @property cursor
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "cursor", {
            get: function() {
                var value = delegate.cursor;
                return P.boxAsJs(value);
            }
        });

        /**
        * The handler function for the event occured before the cursor position changed.
         * @property willScroll
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "willScroll", {
            get: function() {
                var value = delegate.willScroll;
                return P.boxAsJs(value);
            },
            set: function(aValue) {
                delegate.willScroll = P.boxAsJava(aValue);
            }
        });

        /**
        * The handler function for the event occured before an entity row has been inserted.
         * @property willInsert
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "willInsert", {
            get: function() {
                var value = delegate.willInsert;
                return P.boxAsJs(value);
            },
            set: function(aValue) {
                delegate.willInsert = P.boxAsJava(aValue);
            }
        });

        /**
        * The handler function for the event occured after the entity data have been required.
         * @property onRequeried
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "onRequeried", {
            get: function() {
                var value = delegate.onRequeried;
                return P.boxAsJs(value);
            }
        });

        /**
        * The handler function for the event occured after the entity data change.
         * @property onChanged
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "onChanged", {
            get: function() {
                var value = delegate.onChanged;
                return P.boxAsJs(value);
            },
            set: function(aValue) {
                delegate.onChanged = P.boxAsJava(aValue);
            }
        });

        /**
        * The handler function for the event occured after an entity row has been deleted.
         * @property onDeleted
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "onDeleted", {
            get: function() {
                var value = delegate.onDeleted;
                return P.boxAsJs(value);
            },
            set: function(aValue) {
                delegate.onDeleted = P.boxAsJava(aValue);
            }
        });

        /**
        * The handler function for the event occured after the cursor position changed.
         * @property onScrolled
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "onScrolled", {
            get: function() {
                var value = delegate.onScrolled;
                return P.boxAsJs(value);
            },
            set: function(aValue) {
                delegate.onScrolled = P.boxAsJava(aValue);
            }
        });

        /**
        * The handler function for the event occured after the filter have been applied to the entity.
         * @property onFiltered
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "onFiltered", {
            get: function() {
                var value = delegate.onFiltered;
                return P.boxAsJs(value);
            },
            set: function(aValue) {
                delegate.onFiltered = P.boxAsJava(aValue);
            }
        });

        /** * Returns cursor-substitute entity.
         * @property substitute
         * @memberOf ApplicationDbParametersEntity
         * Sunstitute's cursor is used when in original entity's cursor some field's value is null./*
        Object.defineProperty(this, "substitute", {
            get: function() {
                var value = delegate.substitute;
                return P.boxAsJs(value);
            }
        });

        /**
        * Checks if the rowset is empty.
        * @return <code>true</code> if the rowset is empty and <code>false</code> otherwise.
         * @property empty
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "empty", {
            get: function() {
                var value = delegate.empty;
                return P.boxAsJs(value);
            }
        });

        /**
        * The handler function for the event occured after an entity row has been inserted.
         * @property onInserted
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "onInserted", {
            get: function() {
                var value = delegate.onInserted;
                return P.boxAsJs(value);
            },
            set: function(aValue) {
                delegate.onInserted = P.boxAsJava(aValue);
            }
        });

        /**
        * The handler function for the event occured before the entity data change.
         * @property willChange
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "willChange", {
            get: function() {
                var value = delegate.willChange;
                return P.boxAsJs(value);
            },
            set: function(aValue) {
                delegate.willChange = P.boxAsJava(aValue);
            }
        });

        /**
        * The rowset size.
         * @property size
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "size", {
            get: function() {
                var value = delegate.size;
                return P.boxAsJs(value);
            }
        });

        /**
        * Entity's active <code>Filter</code> object.
         * @property activeFilter
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "activeFilter", {
            get: function() {
                var value = delegate.activeFilter;
                return P.boxAsJs(value);
            }
        });

        /**
        * Experimental. The constructor funciton for the entity's data array elements.
         * @property elementClass
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "elementClass", {
            get: function() {
                var value = delegate.elementClass;
                return P.boxAsJs(value);
            },
            set: function(aValue) {
                delegate.elementClass = P.boxAsJava(aValue);
            }
        });

        /**
        * The handler function for the event occured before an entity row has been deleted.
         * @property willDelete
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "willDelete", {
            get: function() {
                var value = delegate.willDelete;
                return P.boxAsJs(value);
            },
            set: function(aValue) {
                delegate.willDelete = P.boxAsJava(aValue);
            }
        });

        /**
        * Moves the rowset cursor to the next row.
        * @return <code>true</code> if cursor moved successfully and <code>false</code> otherwise.
         * @method next
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "next", {
            get: function() {
                return function() {
                    var value = delegate.next();
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Finds rows using field -- field value pairs.
        * @param pairs the search conditions pairs, if a form of key-values pairs, where the key is the property object (e.g. entity.md.propName) and the value for this property.
        * @return the rows object's array accordind to the search condition or empty array if nothing is found.
         * @method find
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "find", {
            get: function() {
                return function(arg0) {
                    var value = delegate.find(P.boxAsJava(arg0));
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Refreshes rowset, only if any of its parameters has changed.
        * @param onSuccessCallback the handler function for refresh data on success event (optional).
        * @param onFailureCallback the handler function for refresh data on failure event (optional).
         * @method execute
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "execute", {
            get: function() {
                return function(arg0arg1) {
                    var value = delegate.execute(P.boxAsJava(arg0)P.boxAsJava(arg1));
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Moves the rowset cursor to the privious row.
        * @return <code>true</code> if cursor moved successfully and <code>false</code> otherwise.
         * @method prev
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "prev", {
            get: function() {
                return function() {
                    var value = delegate.prev();
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Inserts new row in the rowset and sets cursor on this row. @see push.
        * @param pairs the fields value pairs, in a form of key-values pairs, where the key is the property object (e.g. entity.md.propName) and the value for this property (optional).
         * @method insert
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "insert", {
            get: function() {
                return function(arg0) {
                    var value = delegate.insert(P.boxAsJava(arg0));
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Checks if cursor in the position before the first row.
        * @return <code>true</code> if cursor moved successfully and <code>false</code> otherwise.
         * @method eof
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "eof", {
            get: function() {
                return function() {
                    var value = delegate.eof();
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Moves the rowset cursor to the first row.
        * @return <code>true</code> if cursor moved successfully and <code>false</code> otherwise.
         * @method first
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "first", {
            get: function() {
                return function() {
                    var value = delegate.first();
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Moves the rowset cursor to the last row.
        * @return <code>true</code> if cursor moved successfully and <code>false</code> otherwise.
         * @method last
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "last", {
            get: function() {
                return function() {
                    var value = delegate.last();
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Positions the rowset cursor on the specified row number. Row number is 1-based.
        * @param index the row index to check, starting form <code>1</code>.
        * @return <code>true</code> if the cursor is on the row with specified index and <code>false</code> otherwise.
         * @method pos
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "pos", {
            get: function() {
                return function(arg0) {
                    var value = delegate.pos(P.boxAsJava(arg0));
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Gets the row at specified index.
        * @param index the row index, starting form <code>1</code>.
        * @return the row object or <code>null</code> if no row object have found at the specified index.
         * @method getRow
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "getRow", {
            get: function() {
                return function(arg0) {
                    var value = delegate.getRow(P.boxAsJava(arg0));
                    return P.boxAsJs(value);
                };
            }
        });

        /**
         * Deletes the row at the cursor position.
         * @method deleteRow
         * @memberOf ApplicationDbParametersEntity
         */
        Object.defineProperty(this, "deleteRow", {
            get: function() {
                return function() {
                    var value = delegate.deleteRow();
                    return P.boxAsJs(value);
                };
            }
        });

        /**
         * Deletes the row by cursor position.
         * @param aCusorPos row position in terms of cursor API. 1-based.
         * @method deleteRow
         * @memberOf ApplicationDbParametersEntity
         */
        Object.defineProperty(this, "deleteRow", {
            get: function() {
                return function(arg0) {
                    var value = delegate.deleteRow(P.boxAsJava(arg0));
                    return P.boxAsJs(value);
                };
            }
        });

        /**
         * Deletes the row passed in.
         * @param aRow A row to be deleted.
         * @method deleteRow
         * @memberOf ApplicationDbParametersEntity
         */
        Object.defineProperty(this, "deleteRow", {
            get: function() {
                return function(arg0) {
                    var value = delegate.deleteRow(P.boxAsJava(arg0));
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Moves the rowset cursor to the position before the first row.
         * @method beforeFirst
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "beforeFirst", {
            get: function() {
                return function() {
                    var value = delegate.beforeFirst();
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Moves the rowset cursor to the position after the last row.
         * @method afterLast
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "afterLast", {
            get: function() {
                return function() {
                    var value = delegate.afterLast();
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Deletes all rows in the rowset.
         * @method deleteAll
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "deleteAll", {
            get: function() {
                return function() {
                    var value = delegate.deleteAll();
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Creates an instace of filter object to filter rowset data in-place using specified constraints objects.
        * @param pairs the search conditions pairs, if a form of key-values pairs, where the key is the property object (e.g. entity.md.propName) and the value for this property.
        * @return a comparator object.
         * @method createFilter
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "createFilter", {
            get: function() {
                return function(arg0) {
                    var value = delegate.createFilter(P.boxAsJava(arg0));
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Creates an instance of comparator object using specified constraints objects.
        * @param pairs the search conditions pairs, in a form of key-values pairs, where the key is the property object (e.g. entity.md.propName) and the value for this property.
        * @return a comparator object to be passed as a parameter to entity's <code>sort</code> method.
         * @method createSorting
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "createSorting", {
            get: function() {
                return function(arg0) {
                    var value = delegate.createSorting(P.boxAsJava(arg0));
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Finds row by its key. Key must a single property.
        * @param key the unique identifier of the row.
        * @return a row object or <code>null</code> if nothing is found.
         * @method findById
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "findById", {
            get: function() {
                return function(arg0) {
                    var value = delegate.findById(P.boxAsJava(arg0));
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Sets the rowset cursor to the specified row.
        * @param row the row to position the entity cursor.
        * @return <code>true</code> if the rowset scrolled successfully and <code>false</code> otherwise.
         * @method scrollTo
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "scrollTo", {
            get: function() {
                return function(arg0) {
                    var value = delegate.scrollTo(P.boxAsJava(arg0));
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Checks if cursor in the position before the first row.
        * @return <code>true</code> if cursor in the position before the first row and <code>false</code> otherwise.
         * @method bof
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "bof", {
            get: function() {
                return function() {
                    var value = delegate.bof();
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Requeries the rowset's data. Forses the rowset to refresh its data, no matter if its parameters has changed or not.
        * @param onSuccessCallback the handler function for refresh data on success event (optional).
        * @param onFailureCallback the handler function for refresh data on failure event (optional).
         * @method requery
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "requery", {
            get: function() {
                return function(arg0arg1) {
                    var value = delegate.requery(P.boxAsJava(arg0)P.boxAsJava(arg1));
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Disables automatic model update on parameters change, @see endUpdate method.
         * @method beginUpdate
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "beginUpdate", {
            get: function() {
                return function() {
                    var value = delegate.beginUpdate();
                    return P.boxAsJs(value);
                };
            }
        });

        /**
        * Enables automatic model update on parameters change, @see beginUpdate method.
         * @method endUpdate
         * @memberOf ApplicationDbParametersEntity
        */
        Object.defineProperty(this, "endUpdate", {
            get: function() {
                return function() {
                    var value = delegate.endUpdate();
                    return P.boxAsJs(value);
                };
            }
        });


        delegate.setPublished(this);
    };
})();