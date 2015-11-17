package com.tosnos.cosa.binary.function;

import com.microsoft.z3.Z3Exception;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.NativeLibraryHandler;
import com.tosnos.cosa.binary.asm.value.*;
import com.tosnos.cosa.binary.asm.value.JNI.JNIValue;
import com.tosnos.cosa.binary.asm.value.memory.Address;
import com.tosnos.cosa.util.Misc;
import soot.*;

import java.io.IOException;
import java.util.Stack;

//import java.io.IOException;

/**
 * Created by kevin on 12/23/14.
 */
public abstract class Function extends SubroutineOrFunction {
    public static final Type VOID = VoidType.v();
    public static final Type BOOL = BooleanType.v();
    public static final Type BYTE = ByteType.v();
    public static final Type CHAR = CharType.v();
    public static final Type INT = IntType.v();
    public static final Type SHORT = ShortType.v();
    public static final Type LONG = LongType.v();
    public static final Type B_64 = RefType.v("bottom");
    public static final Type H_64 = RefType.v("high");
    public static final Type DOUBLE = DoubleType.v();
    public static final Type FLOAT = FloatType.v();
    public static final Type ADDRESS = RefType.v("address");
    public static final Type STRING = RefType.v("java.lang.String");
    public static final Type FORMAT = RefType.v("format");
    public static final Type NEW = RefType.v("NEW");
    public static final Type VA_LIST = RefType.v("va_list");
    public static final Type FILE = RefType.v("java.io.File");


    public static final Type INTFIELD = RefType.v("intfield");
    public static final Type JNIENV = RefType.v("env");
    public static final Type CLASSTYPE = RefType.v("classType");
    public static final Type RESERVED = RefType.v("reserved");
    public static final Type JAVAVM = RefType.v("vm");
    public static final Type JMETHOD_ID = RefType.v("methodid");
    public static final Type JNINATIVE_METHOD = RefType.v("jninativemethod");
    public static final Type JFIELD_ID = RefType.v("fieldid");
    public static final Type JSIZE = INT;
    public static final Type CHAR_PTR = ADDRESS;
    public static final Type JVALUE = RefType.v("value");
    public static final Type JWEAK = RefType.v("jweak");
    public static final Type JCLASS = RefType.v("java.lang.Class");
    public static final Type JOBJECT = RefType.v("java.lang.Object");
    public static final Type JARRAY = ArrayType.v(JOBJECT, 1);
    public static final Type JOBJECT_ARRAY = ArrayType.v(JOBJECT, 1);
    public static final Type JTHROWABLE = RefType.v("java.lang.Throwable");
    public static final Type JSTRING = STRING;
    public static final Type JOBJECT_REF_TYPE = RefType.v("object_ref_type");
    public static final Type JINT = INT;
    public static final Type JINT_ARRAY = ArrayType.v(JINT, 1);
    public static final Type JBYTE = BYTE;
    public static final Type JBYTE_ARRAY = ArrayType.v(JBYTE, 1);
    public static final Type JSHORT = SHORT;
    public static final Type JSHORT_ARRAY = ArrayType.v(JSHORT, 1);
    public static final Type JLONG = LONG;
    public static final Type JB_64 = B_64;
    public static final Type JH_64 = H_64;
    public static final Type JLONG_ARRAY = ArrayType.v(JLONG, 1);
    public static final Type JCHAR = CHAR;
    public static final Type JCHAR_ARRAY = ArrayType.v(JCHAR, 1);
    public static final Type JFLOAT = FLOAT;
    public static final Type JFLOAT_ARRAY = ArrayType.v(JFLOAT, 1);
    public static final Type JDOUBLE = DOUBLE;
    public static final Type JDOUBLE_ARRAY = ArrayType.v(JDOUBLE, 1);
    public static final Type JBOOLEAN = BOOL;
    public static final Type JBOOLEAN_ARRAY = ArrayType.v(JBOOLEAN, 1);

    protected Type returnType;
    protected Type[] parameterTypes;
    protected boolean ignore;

    protected Function(String name, Type[] parameterTypes, Type returnType) {
        this(name, parameterTypes, returnType, false);
    }

    protected Function(String name, Type[] parameterTypes, Type returnType, boolean ignore) {
        super(name);
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.ignore = ignore;
    }

    public final boolean isIgnore() {
        return ignore;
    }

    public final int getParameterCount() {
        return parameterTypes.length;
    }

    public final Type[] getParamTypes() {
        return parameterTypes;
    }

    public final Type getParamType(int index) {
        return parameterTypes[index];
    }

    public final Type getReturnType() {
        return returnType;
    }

    public String toString() {
        return Misc.ansi().bg(Misc.BLUE).a(name).reset().toString();
    }

    public boolean isFunction() {
        return true;
    }


    public boolean exec(NativeLibraryHandler handler, LibraryModule module, Subroutine subroutine, Stack<Subroutine> depth) throws IOException, Z3Exception {
        throw new RuntimeException("exec is not supported " + getClass().getName());
    }

    public static StringValue getStringValue(LibraryModule module, AbstractValue v) throws IOException, Z3Exception {
        if(v.isUnknown()) {
            return new StringValue("");
        }

        if(v.isVariable()) {
            return new StringValue(((LocalVariable)v).getAddress());
        }

        if(v.isStringValue()) {
            return (StringValue)v;
        }
        if(v.isAssociatedValue()) {
            module = ((AssociatedValue)v).getModule();
            v = v.getValue();
        }

        if(v.isAllocatedMemory()) {
            StringValue str = new StringValue((AllocatedMemory)v);
            ((AllocatedMemory)v).setDataStructureMap(((AllocatedMemory) v).intValue(), str);
        }

        if(v.isJNIValue()) {
            return new StringValue(((JNIValue)v).getAddress());
        }

        if(v.isAddress()) {
            return new StringValue((Address)v);
        }
//        else if(v.isVariable()) {
//            return new StringValue(((Variable)v).getAddress());
//        }
        return new StringValue(module.getInternalMemory(), v.getNumeral());
    }
}
