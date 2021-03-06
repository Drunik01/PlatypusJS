/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.client.threetier.requests;

import com.bearsoft.rowset.Rowset;
import com.eas.client.threetier.Request;
import com.eas.client.threetier.Requests;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import jdk.nashorn.api.scripting.JSObject;

/**
 *
 * @author pk, mg refactoring
 */
public class ExecuteServerModuleMethodRequest extends Request {

    private String methodName;
    private Object[] arguments;
    private String moduleName;

    public ExecuteServerModuleMethodRequest() {
        super(Requests.rqExecuteServerModuleMethod);
    }

    public ExecuteServerModuleMethodRequest(String aModuleName, String aMethodName, Object[] aArguments) {
        this();
        moduleName = aModuleName;
        methodName = aMethodName;
        if (aArguments == null) {
            throw new NullPointerException("No arguments.");
        }
        arguments = aArguments;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String aValue) {
        methodName = aValue;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String aValue) {
        moduleName = aValue;
    }

    @Override
    public void accept(PlatypusRequestVisitor aVisitor) throws Exception {
        aVisitor.visit(this);
    }

    public void setArguments(Object[] aValue) {
        arguments = aValue;
    }

    public static enum ArgumentType {

        STRING(1, String.class), BYTE(2, Byte.class), SHORT(3, Short.class),
        INTEGER(4, Integer.class), LONG(5, Long.class), FLOAT(6, Float.class),
        DOUBLE(7, Double.class), BIG_DECIMAL(8, BigDecimal.class),
        BIG_INTEGER(9, BigInteger.class), BOOLEAN(10, Boolean.class),
        CHARACTER(11, Character.class), DATE(12, Date.class),
        OBJECT(13, String.class);
        private int typeID;
        private Class clazz;

        private ArgumentType(int typeID, Class clazz) {
            this.typeID = typeID;
            this.clazz = clazz;
        }

        public int getTypeID() {
            return typeID;
        }

        public Class getClazz() {
            return clazz;
        }

        public static ArgumentType getArgumentType(int typeID) {
            for (ArgumentType at : values()) {
                if (typeID == at.getTypeID()) {
                    return at;
                }
            }
            return null;
        }

        public static ArgumentType getArgumentType(Object value) {
            if (value instanceof JSObject || value instanceof Rowset) {
                return ArgumentType.OBJECT;
            } else {
                for (ArgumentType at : values()) {
                    if (at.getClazz().isInstance(value)) {
                        return at;
                    }
                }
                return null;
            }
        }
    }

    public static class Response extends com.eas.client.threetier.Response {

        private Object result;

        public Response(Object aResult) {
            super();
            result = aResult;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object aValue) {
            result = aValue;
        }

        @Override
        public void accept(PlatypusResponseVisitor aVisitor) throws Exception {
            aVisitor.visit(this);
        }
    }
}
