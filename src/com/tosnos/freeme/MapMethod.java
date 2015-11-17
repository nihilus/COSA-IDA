package com.tosnos.freeme;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MapMethod {
    public Field[] staticFields;
    public boolean isStatic = false;
    //	private int [] contains;
    Class<?>[] paramTypes;
    private Method method;
    private int methodId;

//	private SparseArray<FreemeObjectStreamClass> freemeClassDesc = null;

    public MapMethod(int methodId, Method method) {
        this.methodId = methodId;
        this.isStatic = Modifier.isStatic(method.getModifiers());
        this.method = method;
        method.setAccessible(true);

        Class<?>[] types = method.getParameterTypes();
        int length = types.length;

        if (!isStatic) {
            length++;
        }

        if (length > 0) {
            int index = 0;
            paramTypes = new Class<?>[length];
            if (!isStatic) {
                paramTypes[index++] = method.getDeclaringClass();
            }
            if (types.length > 0) {
                System.arraycopy(types, 0, paramTypes, index, types.length);
            }
        }
    }

//	public void setContains(int[] contains) {
//		this.contains = contains;
//	}

    public void setStaticFields(Field[] fields) {
        staticFields = fields;
    }

    public int getMethodId() {
        return methodId;
    }

//	public SparseArray<FreemeObjectStreamClass> getFreemeClassDesc() {
//		return freemeClassDesc;
//	}
//
//	public synchronized void setClassDesc(int paramId, FreemeObjectStreamClass classDesc) {
//		if(freemeClassDesc==null) {
//			freemeClassDesc = new SparseArray<FreemeObjectStreamClass>();
//		}
//		freemeClassDesc.put(paramId, classDesc);
//	}

    public Class<?>[] getParamTypes() {
        return paramTypes;
    }

    public Class<?> getParamType(int paramId) {
        return paramTypes[paramId];
    }

    public Method getMethod() {
        return method;
    }

    public Field[] getStaticField() {
        return staticFields;
    }

//	public int[] getContains() {
//		return contains;
//	}
}
