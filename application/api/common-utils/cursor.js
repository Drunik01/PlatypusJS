(function() {
    var javaClass = Java.type("com.eas.gui.Cursor");
    javaClass.setPublisher(function(aDelegate) {
        return new P.Cursor(null, aDelegate);
    });
    
    /**
     *
     * @constructor Cursor Cursor
     */
    P.Cursor = function (aType) {
        var maxArgs = 1;
        var delegate = arguments.length > maxArgs ?
              arguments[maxArgs] 
            : arguments.length === 1 ? new javaClass(P.boxAsJava(aType))
            : new javaClass();

        Object.defineProperty(this, "unwrap", {
            value: function() {
                return delegate;
            }
        });
        if(P.Cursor.superclass)
            P.Cursor.superclass.constructor.apply(this, arguments);
        delegate.setPublished(this);
    };
})();