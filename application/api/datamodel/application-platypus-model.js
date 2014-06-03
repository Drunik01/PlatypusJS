(function() {
    var javaClass = Java.type("com.eas.client.model.application.ApplicationPlatypusModel");
    javaClass.setPublisher(function(aDelegate) {
        return new P.ApplicationPlatypusModel(aDelegate);
    });
    
    /**
     * Generated constructor.
     * @namespace ApplicationPlatypusModel
     */
    P.ApplicationPlatypusModel = function () {

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
         * Saves model data changes. Calls aCallback when done.
         * If model can't apply the changed, than exception is thrown.
         * In this case, application can call model.save() another time to save the changes.
         * @method save
         * @memberOf ApplicationPlatypusModel
         * If an application need to abort futher attempts and discard model data changes, than it can call model.revert().
        Object.defineProperty(this, "save", {
            get: function() {
                return function(arg0) {
                    var value = delegate.save(P.boxAsJava(arg0));
                    return P.boxAsJs(value);
                };
            }
        });

        /**
         * Requeries model data with callback.
         * @method requery
         * @memberOf ApplicationPlatypusModel
         */
        Object.defineProperty(this, "requery", {
            get: function() {
                return function(arg0) {
                    var value = delegate.requery(P.boxAsJava(arg0));
                    return P.boxAsJs(value);
                };
            }
        });

        /**
         * Requeries model data with callback.
         * @method requery
         * @memberOf ApplicationPlatypusModel
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
        * Refreshes the model, only if any of its parameters has changed.
        * @param onSuccessCallback the handler function for refresh data on success event (optional).
        * @param onFailureCallback the handler function for refresh data on failure event (optional).
         * @method execute
         * @memberOf ApplicationPlatypusModel
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
        * Creates new entity of model, based on application query.
        * @param queryId the query application element ID.
        * @return a new entity.
         * @method loadEntity
         * @memberOf ApplicationPlatypusModel
        */
        Object.defineProperty(this, "loadEntity", {
            get: function() {
                return function(arg0) {
                    var value = delegate.loadEntity(P.boxAsJava(arg0));
                    return P.boxAsJs(value);
                };
            }
        });


        delegate.setPublished(this);
    };
})();