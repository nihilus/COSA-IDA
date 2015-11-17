package com.tosnos.cosa.binary.function;

import com.microsoft.z3.Z3Exception;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.NativeLibraryHandler;
import com.tosnos.cosa.binary.asm.Instruction;
import com.tosnos.cosa.binary.asm.Register;
import com.tosnos.cosa.binary.asm.value.*;
import com.tosnos.cosa.binary.asm.value.Immediate;
import com.tosnos.cosa.binary.asm.value.JNI.JNIValue;
import com.tosnos.cosa.binary.asm.value.memory.Address;
import com.tosnos.cosa.binary.asm.value.memory.IMemoryValue;
import com.tosnos.cosa.binary.asm.Modifier;
import com.tosnos.cosa.util.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.FieldRef;
import soot.jimple.Jimple;

import java.io.IOException;
import java.util.List;
import java.util.Stack;

/**
 * Created by kevin on 7/23/14.
 */
public class JNIFunction extends Function {
    // for JNIEnv
    public final static JNIFunction GetVersion = new JNIFunction("GetVersion", new Type[]{JNIENV}, JINT);
    public final static JNIFunction DefineClass = new JNIFunction("DefineClass", new Type[]{JNIENV, CHAR, JOBJECT, JBYTE, JSIZE}, JCLASS);
    public final static JNIFunction FindClass = new JNIFunction("FindClass", new Type[]{JNIENV, CHAR}, JCLASS);
    public final static JNIFunction FromReflectedMethod = new JNIFunction("FromReflectedMethod", new Type[]{JNIENV, JOBJECT}, JMETHOD_ID);
    public final static JNIFunction FromReflectedField = new JNIFunction("FromReflectedField", new Type[]{JNIENV, JOBJECT}, JFIELD_ID);
    public final static JNIFunction ToReflectedMethod = new JNIFunction("ToReflectedMethod", new Type[]{JNIENV, JCLASS, JMETHOD_ID, JBOOLEAN}, JOBJECT);
    public final static JNIFunction GetSuperclass = new JNIFunction("GetSuperclass", new Type[]{JNIENV, JCLASS}, JCLASS);
    public final static JNIFunction IsAssignableFrom = new JNIFunction("IsAssignableFrom", new Type[]{JNIENV, JCLASS, JCLASS}, JBOOLEAN);
    public final static JNIFunction ToReflectedField = new JNIFunction("ToReflectedField", new Type[]{JNIENV, JCLASS, JFIELD_ID, JBOOLEAN}, JOBJECT);
    public final static JNIFunction Throw = new JNIFunction("Throw", new Type[]{JNIENV, JTHROWABLE}, JINT);
    public final static JNIFunction ThrowNew = new JNIFunction("ThrowNew", new Type[]{JNIENV, JCLASS, CHAR}, JINT);
    public final static JNIFunction ExceptionOccurred = new JNIFunction("ExceptionOccurred", new Type[]{JNIENV}, JTHROWABLE);
    public final static JNIFunction ExceptionDescribe = new JNIFunction("ExceptionDescribe", new Type[]{JNIENV}, VOID);
    public final static JNIFunction ExceptionClear = new JNIFunction("ExceptionClear", new Type[]{JNIENV}, VOID);
    public final static JNIFunction FatalError = new JNIFunction("FatalError", new Type[]{JNIENV, CHAR}, VOID);
    public final static JNIFunction PushLocalFrame = new JNIFunction("PushLocalFrame", new Type[]{JNIENV, JINT}, JINT);
    public final static JNIFunction PopLocalFrame = new JNIFunction("PopLocalFrame", new Type[]{JNIENV, JOBJECT}, JOBJECT);
    public final static JNIFunction NewGlobalRef = new JNIFunction("NewGlobalRef", new Type[]{JNIENV, JOBJECT}, JOBJECT);
    public final static JNIFunction DeleteGlobalRef = new JNIFunction("DeleteGlobalRef", new Type[]{JNIENV, JOBJECT}, VOID);
    public final static JNIFunction DeleteLocalRef = new JNIFunction("DeleteLocalRef", new Type[]{JNIENV, JOBJECT}, VOID);
    public final static JNIFunction IsSameObject = new JNIFunction("IsSameObject", new Type[]{JNIENV, JOBJECT, JOBJECT}, JBOOLEAN);
    public final static JNIFunction NewLocalRef = new JNIFunction("NewLocalRef", new Type[]{JNIENV, JOBJECT}, JOBJECT);
    public final static JNIFunction EnsureLocalCapacity = new JNIFunction("EnsureLocalCapacity", new Type[]{JNIENV, JINT}, JINT);
    public final static JNIFunction AllocObject = new JNIFunction("AllocObject", new Type[]{JNIENV, JCLASS}, JOBJECT);
    public final static JNIFunction NewObject = new JNIFunction("NewObject", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JOBJECT);
    public final static JNIFunction NewObjectV = new JNIFunction("NewObjectV", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JOBJECT);
    public final static JNIFunction NewObjectA = new JNIFunction("NewObjectA", new Type[]{JNIENV, JCLASS, JMETHOD_ID, JVALUE}, JOBJECT);
    public final static JNIFunction GetObjectClass = new JNIFunction("GetObjectClass", new Type[]{JNIENV, JOBJECT}, JCLASS);
    public final static JNIFunction IsInstanceOf = new JNIFunction("IsInstanceOf", new Type[]{JNIENV, JOBJECT, JCLASS}, JBOOLEAN);
    public final static JNIFunction GetMethodID = new JNIFunction("GetMethodID", new Type[]{JNIENV, JCLASS, CHAR, CHAR}, JMETHOD_ID);
    public final static JNIFunction CallObjectMethod = new JNIFunction("CallObjectMethod", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JOBJECT);
    public final static JNIFunction CallObjectMethodV = new JNIFunction("CallObjectMethodV", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JOBJECT);
    public final static JNIFunction CallObjectMethodA = new JNIFunction("CallObjectMethodA", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, JVALUE}, JOBJECT);
    public final static JNIFunction CallBooleanMethod = new JNIFunction("CallBooleanMethod", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JBOOLEAN);
    public final static JNIFunction CallBooleanMethodV = new JNIFunction("CallBooleanMethodV", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JBOOLEAN);
    public final static JNIFunction CallBooleanMethodA = new JNIFunction("CallBooleanMethodA", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, JVALUE}, JBOOLEAN);
    public final static JNIFunction CallByteMethod = new JNIFunction("CallByteMethod", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JBYTE);
    public final static JNIFunction CallByteMethodV = new JNIFunction("CallByteMethodV", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JBYTE);
    public final static JNIFunction CallByteMethodA = new JNIFunction("CallByteMethodA", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, JVALUE}, JBYTE);
    public final static JNIFunction CallCharMethod = new JNIFunction("CallCharMethod", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JCHAR);
    public final static JNIFunction CallCharMethodV = new JNIFunction("CallCharMethodV", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JCHAR);
    public final static JNIFunction CallCharMethodA = new JNIFunction("CallCharMethodA", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, JVALUE}, JCHAR);
    public final static JNIFunction CallShortMethod = new JNIFunction("CallShortMethod", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JSHORT);
    public final static JNIFunction CallShortMethodV = new JNIFunction("CallShortMethodV", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JSHORT);
    public final static JNIFunction CallShortMethodA = new JNIFunction("CallShortMethodA", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, JVALUE}, JSHORT);
    public final static JNIFunction CallIntMethod = new JNIFunction("CallIntMethod", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JINT);
    public final static JNIFunction CallIntMethodV = new JNIFunction("CallIntMethodV", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JINT);
    public final static JNIFunction CallIntMethodA = new JNIFunction("CallIntMethodA", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, JVALUE}, JINT);
    public final static JNIFunction CallLongMethod = new JNIFunction("CallLongMethod", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JLONG);
    public final static JNIFunction CallLongMethodV = new JNIFunction("CallLongMethodV", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JLONG);
    public final static JNIFunction CallLongMethodA = new JNIFunction("CallLongMethodA", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, JVALUE}, JLONG);
    public final static JNIFunction CallFloatMethod = new JNIFunction("CallFloatMethod", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JFLOAT);
    public final static JNIFunction CallFloatMethodV = new JNIFunction("CallFloatMethodV", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JFLOAT);
    public final static JNIFunction CallFloatMethodA = new JNIFunction("CallFloatMethodA", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, JVALUE}, JFLOAT);
    public final static JNIFunction CallDoubleMethod = new JNIFunction("CallDoubleMethod", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JDOUBLE);
    public final static JNIFunction CallDoubleMethodV = new JNIFunction("CallDoubleMethodV", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, JDOUBLE);
    public final static JNIFunction CallDoubleMethodA = new JNIFunction("CallDoubleMethodA", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, JVALUE}, JDOUBLE);
    public final static JNIFunction CallVoidMethod = new JNIFunction("CallVoidMethod", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, VOID);
    public final static JNIFunction CallVoidMethodV = new JNIFunction("CallVoidMethodV", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, VA_LIST}, VOID);
    public final static JNIFunction CallVoidMethodA = new JNIFunction("CallVoidMethodA", new Type[]{JNIENV, JOBJECT, JMETHOD_ID, JVALUE}, VOID);
    public final static JNIFunction CallNonvirtualObjectMethod = new JNIFunction("CallNonvirtualObjectMethod", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JOBJECT);
    public final static JNIFunction CallNonvirtualObjectMethodV = new JNIFunction("CallNonvirtualObjectMethodV", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JOBJECT);
    public final static JNIFunction CallNonvirtualObjectMethodA = new JNIFunction("CallNonvirtualObjectMethodA", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, JVALUE}, JOBJECT);
    public final static JNIFunction CallNonvirtualBooleanMethod = new JNIFunction("CallNonvirtualBooleanMethod", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JBOOLEAN);
    public final static JNIFunction CallNonvirtualBooleanMethodV = new JNIFunction("CallNonvirtualBooleanMethodV", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JBOOLEAN);
    public final static JNIFunction CallNonvirtualBooleanMethodA = new JNIFunction("CallNonvirtualBooleanMethodA", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, JVALUE}, JBOOLEAN);
    public final static JNIFunction CallNonvirtualByteMethod = new JNIFunction("CallNonvirtualByteMethod", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JBYTE);
    public final static JNIFunction CallNonvirtualByteMethodV = new JNIFunction("CallNonvirtualByteMethodV", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JBYTE);
    public final static JNIFunction CallNonvirtualByteMethodA = new JNIFunction("CallNonvirtualByteMethodA", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, JVALUE}, JBYTE);
    public final static JNIFunction CallNonvirtualCharMethod = new JNIFunction("CallNonvirtualCharMethod", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JCHAR);
    public final static JNIFunction CallNonvirtualCharMethodV = new JNIFunction("CallNonvirtualCharMethodV", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JCHAR);
    public final static JNIFunction CallNonvirtualCharMethodA = new JNIFunction("CallNonvirtualCharMethodA", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, JVALUE}, JCHAR);
    public final static JNIFunction CallNonvirtualShortMethod = new JNIFunction("CallNonvirtualShortMethod", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JSHORT);
    public final static JNIFunction CallNonvirtualShortMethodV = new JNIFunction("CallNonvirtualShortMethodV", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JSHORT);
    public final static JNIFunction CallNonvirtualShortMethodA = new JNIFunction("CallNonvirtualShortMethodA", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, JVALUE}, JSHORT);
    public final static JNIFunction CallNonvirtualIntMethod = new JNIFunction("CallNonvirtualIntMethod", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JINT);
    public final static JNIFunction CallNonvirtualIntMethodV = new JNIFunction("CallNonvirtualIntMethodV", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JINT);
    public final static JNIFunction CallNonvirtualIntMethodA = new JNIFunction("CallNonvirtualIntMethodA", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, JVALUE}, JINT);
    public final static JNIFunction CallNonvirtualLongMethod = new JNIFunction("CallNonvirtualLongMethod", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JLONG);
    public final static JNIFunction CallNonvirtualLongMethodV = new JNIFunction("CallNonvirtualLongMethodV", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JLONG);
    public final static JNIFunction CallNonvirtualLongMethodA = new JNIFunction("CallNonvirtualLongMethodA", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, JVALUE}, JLONG);
    public final static JNIFunction CallNonvirtualFloatMethod = new JNIFunction("CallNonvirtualFloatMethod", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JFLOAT);
    public final static JNIFunction CallNonvirtualFloatMethodV = new JNIFunction("CallNonvirtualFloatMethodV", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JFLOAT);
    public final static JNIFunction CallNonvirtualFloatMethodA = new JNIFunction("CallNonvirtualFloatMethodA", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, JVALUE}, JFLOAT);
    public final static JNIFunction CallNonvirtualDoubleMethod = new JNIFunction("CallNonvirtualDoubleMethod", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JDOUBLE);
    public final static JNIFunction CallNonvirtualDoubleMethodV = new JNIFunction("CallNonvirtualDoubleMethodV", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, JDOUBLE);
    public final static JNIFunction CallNonvirtualDoubleMethodA = new JNIFunction("CallNonvirtualDoubleMethodA", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, JVALUE}, JDOUBLE);
    public final static JNIFunction CallNonvirtualVoidMethod = new JNIFunction("CallNonvirtualVoidMethod", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, VOID);
    public final static JNIFunction CallNonvirtualVoidMethodV = new JNIFunction("CallNonvirtualVoidMethodV", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, VA_LIST}, VOID);
    public final static JNIFunction CallNonvirtualVoidMethodA = new JNIFunction("CallNonvirtualVoidMethodA", new Type[]{JNIENV, JOBJECT, JCLASS, JMETHOD_ID, JVALUE}, VOID);
    public final static JNIFunction GetFieldID = new JNIFunction("GetFieldID", new Type[]{JNIENV, JCLASS, CHAR_PTR, CHAR_PTR}, JFIELD_ID);
    public final static JNIFunction GetObjectField = new JNIFunction("GetObjectField", new Type[]{JNIENV, JOBJECT, JFIELD_ID}, JOBJECT);
    public final static JNIFunction GetBooleanField = new JNIFunction("GetBooleanField", new Type[]{JNIENV, JOBJECT, JFIELD_ID}, JBOOLEAN);
    public final static JNIFunction GetByteField = new JNIFunction("GetByteField", new Type[]{JNIENV, JOBJECT, JFIELD_ID}, JBYTE);
    public final static JNIFunction GetCharField = new JNIFunction("GetCharField", new Type[]{JNIENV, JOBJECT, JFIELD_ID}, JCHAR);
    public final static JNIFunction GetShortField = new JNIFunction("GetShortField", new Type[]{JNIENV, JOBJECT, JFIELD_ID}, JSHORT);
    public final static JNIFunction GetIntField = new JNIFunction("GetIntField", new Type[]{JNIENV, JOBJECT, JFIELD_ID}, JINT);
    public final static JNIFunction GetLongField = new JNIFunction("GetLongField", new Type[]{JNIENV, JOBJECT, JFIELD_ID}, JLONG);
    public final static JNIFunction GetFloatField = new JNIFunction("GetFloatField", new Type[]{JNIENV, JOBJECT, JFIELD_ID}, JFLOAT);
    public final static JNIFunction GetDoubleField = new JNIFunction("GetDoubleField", new Type[]{JNIENV, JOBJECT, JFIELD_ID}, JDOUBLE);
    public final static JNIFunction SetObjectField = new JNIFunction("SetObjectField", new Type[]{JNIENV, JOBJECT, JFIELD_ID, JOBJECT}, VOID);
    public final static JNIFunction SetBooleanField = new JNIFunction("SetBooleanField", new Type[]{JNIENV, JOBJECT, JFIELD_ID, JBOOLEAN}, VOID);
    public final static JNIFunction SetByteField = new JNIFunction("SetByteField", new Type[]{JNIENV, JOBJECT, JFIELD_ID, JBYTE}, VOID);
    public final static JNIFunction SetCharField = new JNIFunction("SetCharField", new Type[]{JNIENV, JOBJECT, JFIELD_ID, JCHAR}, VOID);
    public final static JNIFunction SetShortField = new JNIFunction("SetShortField", new Type[]{JNIENV, JOBJECT, JFIELD_ID, JSHORT}, VOID);
    public final static JNIFunction SetIntField = new JNIFunction("SetIntField", new Type[]{JNIENV, JOBJECT, JFIELD_ID, JINT}, VOID);
    public final static JNIFunction SetLongField = new JNIFunction("SetLongField", new Type[]{JNIENV, JOBJECT, JFIELD_ID, JLONG}, VOID);
    public final static JNIFunction SetFloatField = new JNIFunction("SetFloatField", new Type[]{JNIENV, JOBJECT, JFIELD_ID, JFLOAT}, VOID);
    public final static JNIFunction SetDoubleField = new JNIFunction("SetDoubleField", new Type[]{JNIENV, JOBJECT, JFIELD_ID, JDOUBLE}, VOID);
    public final static JNIFunction GetStaticMethodID = new JNIFunction("GetStaticMethodID", new Type[]{JNIENV, JCLASS, CHAR, CHAR}, JMETHOD_ID);
    public final static JNIFunction CallStaticObjectMethod = new JNIFunction("CallStaticObjectMethod", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JOBJECT);
    public final static JNIFunction CallStaticObjectMethodV = new JNIFunction("CallStaticObjectMethodV", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JOBJECT);
    public final static JNIFunction CallStaticObjectMethodA = new JNIFunction("CallStaticObjectMethodA", new Type[]{JNIENV, JCLASS, JMETHOD_ID, JVALUE}, JOBJECT);
    public final static JNIFunction CallStaticBooleanMethod = new JNIFunction("CallStaticBooleanMethod", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JBOOLEAN);
    public final static JNIFunction CallStaticBooleanMethodV = new JNIFunction("CallStaticBooleanMethodV", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JBOOLEAN);
    public final static JNIFunction CallStaticBooleanMethodA = new JNIFunction("CallStaticBooleanMethodA", new Type[]{JNIENV, JCLASS, JMETHOD_ID, JVALUE}, JBOOLEAN);
    public final static JNIFunction CallStaticByteMethod = new JNIFunction("CallStaticByteMethod", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JBYTE);
    public final static JNIFunction CallStaticByteMethodV = new JNIFunction("CallStaticByteMethodV", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JBYTE);
    public final static JNIFunction CallStaticByteMethodA = new JNIFunction("CallStaticByteMethodA", new Type[]{JNIENV, JCLASS, JMETHOD_ID, JVALUE}, JBYTE);
    public final static JNIFunction CallStaticCharMethod = new JNIFunction("CallStaticCharMethod", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JCHAR);
    public final static JNIFunction CallStaticCharMethodV = new JNIFunction("CallStaticCharMethodV", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JCHAR);
    public final static JNIFunction CallStaticCharMethodA = new JNIFunction("CallStaticCharMethodA", new Type[]{JNIENV, JCLASS, JMETHOD_ID, JVALUE}, JCHAR);
    public final static JNIFunction CallStaticShortMethod = new JNIFunction("CallStaticShortMethod", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JSHORT);
    public final static JNIFunction CallStaticShortMethodV = new JNIFunction("CallStaticShortMethodV", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JSHORT);
    public final static JNIFunction CallStaticShortMethodA = new JNIFunction("CallStaticShortMethodA", new Type[]{JNIENV, JCLASS, JMETHOD_ID, JVALUE}, JSHORT);
    public final static JNIFunction CallStaticIntMethod = new JNIFunction("CallStaticIntMethod", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JINT);
    public final static JNIFunction CallStaticIntMethodV = new JNIFunction("CallStaticIntMethodV", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JINT);
    public final static JNIFunction CallStaticIntMethodA = new JNIFunction("CallStaticIntMethodA", new Type[]{JNIENV, JCLASS, JMETHOD_ID, JVALUE}, JINT);
    public final static JNIFunction CallStaticLongMethod = new JNIFunction("CallStaticLongMethod", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JLONG);
    public final static JNIFunction CallStaticLongMethodV = new JNIFunction("CallStaticLongMethodV", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JLONG);
    public final static JNIFunction CallStaticLongMethodA = new JNIFunction("CallStaticLongMethodA", new Type[]{JNIENV, JCLASS, JMETHOD_ID, JVALUE}, JLONG);
    public final static JNIFunction CallStaticFloatMethod = new JNIFunction("CallStaticFloatMethod", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JFLOAT);
    public final static JNIFunction CallStaticFloatMethodV = new JNIFunction("CallStaticFloatMethodV", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JFLOAT);
    public final static JNIFunction CallStaticFloatMethodA = new JNIFunction("CallStaticFloatMethodA", new Type[]{JNIENV, JCLASS, JMETHOD_ID, JVALUE}, JFLOAT);
    public final static JNIFunction CallStaticDoubleMethod = new JNIFunction("CallStaticDoubleMethod", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JDOUBLE);
    public final static JNIFunction CallStaticDoubleMethodV = new JNIFunction("CallStaticDoubleMethodV", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, JDOUBLE);
    public final static JNIFunction CallStaticDoubleMethodA = new JNIFunction("CallStaticDoubleMethodA", new Type[]{JNIENV, JCLASS, JMETHOD_ID, JVALUE}, JDOUBLE);
    public final static JNIFunction CallStaticVoidMethod = new JNIFunction("CallStaticVoidMethod", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, VOID);
    public final static JNIFunction CallStaticVoidMethodV = new JNIFunction("CallStaticVoidMethodV", new Type[]{JNIENV, JCLASS, JMETHOD_ID, VA_LIST}, VOID);
    public final static JNIFunction CallStaticVoidMethodA = new JNIFunction("CallStaticVoidMethodA", new Type[]{JNIENV, JCLASS, JMETHOD_ID, JVALUE}, VOID);
    public final static JNIFunction GetStaticFieldID = new JNIFunction("GetStaticFieldID", new Type[]{JNIENV, JCLASS, CHAR, CHAR}, JFIELD_ID);
    public final static JNIFunction GetStaticObjectField = new JNIFunction("GetStaticObjectField", new Type[]{JNIENV, JCLASS, JFIELD_ID}, JOBJECT);
    public final static JNIFunction GetStaticBooleanField = new JNIFunction("GetStaticBooleanField", new Type[]{JNIENV, JCLASS, JFIELD_ID}, JBOOLEAN);
    public final static JNIFunction GetStaticByteField = new JNIFunction("GetStaticByteField", new Type[]{JNIENV, JCLASS, JFIELD_ID}, JBYTE);
    public final static JNIFunction GetStaticCharField = new JNIFunction("GetStaticCharField", new Type[]{JNIENV, JCLASS, JFIELD_ID}, JCHAR);
    public final static JNIFunction GetStaticShortField = new JNIFunction("GetStaticShortField", new Type[]{JNIENV, JCLASS, JFIELD_ID}, JSHORT);
    public final static JNIFunction GetStaticIntField = new JNIFunction("GetStaticIntField", new Type[]{JNIENV, JCLASS, JFIELD_ID}, JINT);
    public final static JNIFunction GetStaticLongField = new JNIFunction("GetStaticLongField", new Type[]{JNIENV, JCLASS, JFIELD_ID}, JLONG);
    public final static JNIFunction GetStaticFloatField = new JNIFunction("GetStaticFloatField", new Type[]{JNIENV, JCLASS, JFIELD_ID}, JFLOAT);
    public final static JNIFunction GetStaticDoubleField = new JNIFunction("GetStaticDoubleField", new Type[]{JNIENV, JCLASS, JFIELD_ID}, JDOUBLE);
    public final static JNIFunction SetStaticObjectField = new JNIFunction("SetStaticObjectField", new Type[]{JNIENV, JCLASS, JFIELD_ID, JOBJECT}, VOID);
    public final static JNIFunction SetStaticBooleanField = new JNIFunction("SetStaticBooleanField", new Type[]{JNIENV, JCLASS, JFIELD_ID, JBOOLEAN}, VOID);
    public final static JNIFunction SetStaticByteField = new JNIFunction("SetStaticByteField", new Type[]{JNIENV, JCLASS, JFIELD_ID, JBYTE}, VOID);
    public final static JNIFunction SetStaticCharField = new JNIFunction("SetStaticCharField", new Type[]{JNIENV, JCLASS, JFIELD_ID, JCHAR}, VOID);
    public final static JNIFunction SetStaticShortField = new JNIFunction("SetStaticShortField", new Type[]{JNIENV, JCLASS, JFIELD_ID, JSHORT}, VOID);
    public final static JNIFunction SetStaticIntField = new JNIFunction("SetStaticIntField", new Type[]{JNIENV, JCLASS, JFIELD_ID, JINT}, VOID);
    public final static JNIFunction SetStaticLongField = new JNIFunction("SetStaticLongField", new Type[]{JNIENV, JCLASS, JFIELD_ID, JLONG}, VOID);
    public final static JNIFunction SetStaticFloatField = new JNIFunction("SetStaticFloatField", new Type[]{JNIENV, JCLASS, JFIELD_ID, JFLOAT}, VOID);
    public final static JNIFunction SetStaticDoubleField = new JNIFunction("SetStaticDoubleField", new Type[]{JNIENV, JCLASS, JFIELD_ID, JDOUBLE}, VOID);
    public final static JNIFunction NewString = new JNIFunction("NewString", new Type[]{JNIENV, JCHAR, JSIZE}, JSTRING);
    public final static JNIFunction GetStringLength = new JNIFunction("GetStringLength", new Type[]{JNIENV, JSTRING}, JSIZE);
    public final static JNIFunction GetStringChars = new JNIFunction("GetStringChars", new Type[]{JNIENV, JSTRING, JBOOLEAN}, JCHAR);
    public final static JNIFunction ReleaseStringChars = new JNIFunction("ReleaseStringChars", new Type[]{JNIENV, JSTRING, JCHAR}, VOID);
    public final static JNIFunction NewStringUTF = new JNIFunction("NewStringUTF", new Type[]{JNIENV, CHAR}, JSTRING);
    public final static JNIFunction GetStringUTFLength = new JNIFunction("GetStringUTFLength", new Type[]{JNIENV, JSTRING}, JSIZE);
    public final static JNIFunction GetStringUTFChars = new JNIFunction("GetStringUTFChars", new Type[]{JNIENV, JSTRING, JBOOLEAN}, CHAR);
    public final static JNIFunction ReleaseStringUTFChars = new JNIFunction("ReleaseStringUTFChars", new Type[]{JNIENV, JSTRING, CHAR}, VOID);
    public final static JNIFunction GetArrayLength = new JNIFunction("GetArrayLength", new Type[]{JNIENV, JARRAY}, JSIZE);
    public final static JNIFunction NewObjectArray = new JNIFunction("NewObjectArray", new Type[]{JNIENV, JSIZE, JCLASS, JOBJECT}, JOBJECT_ARRAY);
    public final static JNIFunction GetObjectArrayElement = new JNIFunction("GetObjectArrayElement", new Type[]{JNIENV, JOBJECT_ARRAY, JSIZE}, JOBJECT);
    public final static JNIFunction SetObjectArrayElement = new JNIFunction("SetObjectArrayElement", new Type[]{JNIENV, JOBJECT_ARRAY, JSIZE, JOBJECT}, VOID);
    public final static JNIFunction NewBooleanArray = new JNIFunction("NewBooleanArray", new Type[]{JNIENV, JSIZE}, JBOOLEAN_ARRAY);
    public final static JNIFunction NewByteArray = new JNIFunction("NewByteArray", new Type[]{JNIENV, JSIZE}, JBYTE_ARRAY);
    public final static JNIFunction NewCharArray = new JNIFunction("NewCharArray", new Type[]{JNIENV, JSIZE}, JCHAR_ARRAY);
    public final static JNIFunction NewShortArray = new JNIFunction("NewShortArray", new Type[]{JNIENV, JSIZE}, JSHORT_ARRAY);
    public final static JNIFunction NewIntArray = new JNIFunction("NewIntArray", new Type[]{JNIENV, JSIZE}, JINT_ARRAY);
    public final static JNIFunction NewLongArray = new JNIFunction("NewLongArray", new Type[]{JNIENV, JSIZE}, JLONG_ARRAY);
    public final static JNIFunction NewFloatArray = new JNIFunction("NewFloatArray", new Type[]{JNIENV, JSIZE}, JFLOAT_ARRAY);
    public final static JNIFunction NewDoubleArray = new JNIFunction("NewDoubleArray", new Type[]{JNIENV, JSIZE}, JDOUBLE_ARRAY);
    public final static JNIFunction GetBooleanArrayElements = new JNIFunction("GetBooleanArrayElements", new Type[]{JNIENV, JBOOLEAN_ARRAY, JBOOLEAN}, JBOOLEAN);
    public final static JNIFunction GetByteArrayElements = new JNIFunction("GetByteArrayElements", new Type[]{JNIENV, JBYTE_ARRAY, JBOOLEAN}, JBYTE);
    public final static JNIFunction GetCharArrayElements = new JNIFunction("GetCharArrayElements", new Type[]{JNIENV, JCHAR_ARRAY, JBOOLEAN}, JCHAR);
    public final static JNIFunction GetShortArrayElements = new JNIFunction("GetShortArrayElements", new Type[]{JNIENV, JSHORT_ARRAY, JBOOLEAN}, JSHORT);
    public final static JNIFunction GetIntArrayElements = new JNIFunction("GetIntArrayElements", new Type[]{JNIENV, JINT_ARRAY, JBOOLEAN}, JINT);
    public final static JNIFunction GetLongArrayElements = new JNIFunction("GetLongArrayElements", new Type[]{JNIENV, JLONG_ARRAY, JBOOLEAN}, JLONG);
    public final static JNIFunction GetFloatArrayElements = new JNIFunction("GetFloatArrayElements", new Type[]{JNIENV, JFLOAT_ARRAY, JBOOLEAN}, JFLOAT);
    public final static JNIFunction GetDoubleArrayElements = new JNIFunction("GetDoubleArrayElements", new Type[]{JNIENV, JDOUBLE_ARRAY, JBOOLEAN}, JDOUBLE);
    public final static JNIFunction ReleaseBooleanArrayElements = new JNIFunction("ReleaseBooleanArrayElements", new Type[]{JNIENV, JBOOLEAN_ARRAY, JBOOLEAN, JINT}, VOID);
    public final static JNIFunction ReleaseByteArrayElements = new JNIFunction("ReleaseByteArrayElements", new Type[]{JNIENV, JBYTE_ARRAY, JBYTE, JINT}, VOID);
    public final static JNIFunction ReleaseCharArrayElements = new JNIFunction("ReleaseCharArrayElements", new Type[]{JNIENV, JCHAR_ARRAY, JCHAR, JINT}, VOID);
    public final static JNIFunction ReleaseShortArrayElements = new JNIFunction("ReleaseShortArrayElements", new Type[]{JNIENV, JSHORT_ARRAY, JSHORT, JINT}, VOID);
    public final static JNIFunction ReleaseIntArrayElements = new JNIFunction("ReleaseIntArrayElements", new Type[]{JNIENV, JINT_ARRAY, JINT, JINT}, VOID);
    public final static JNIFunction ReleaseLongArrayElements = new JNIFunction("ReleaseLongArrayElements", new Type[]{JNIENV, JLONG_ARRAY, JLONG, JINT}, VOID);
    public final static JNIFunction ReleaseFloatArrayElements = new JNIFunction("ReleaseFloatArrayElements", new Type[]{JNIENV, JFLOAT_ARRAY, JFLOAT, JINT}, VOID);
    public final static JNIFunction ReleaseDoubleArrayElements = new JNIFunction("ReleaseDoubleArrayElements", new Type[]{JNIENV, JDOUBLE_ARRAY, JDOUBLE, JINT}, VOID);
    public final static JNIFunction GetBooleanArrayRegion = new JNIFunction("GetBooleanArrayRegion", new Type[]{JNIENV, JBOOLEAN_ARRAY, JSIZE, JSIZE, JBOOLEAN}, VOID);
    public final static JNIFunction GetByteArrayRegion = new JNIFunction("GetByteArrayRegion", new Type[]{JNIENV, JBYTE_ARRAY, JSIZE, JSIZE, JBYTE}, VOID);
    public final static JNIFunction GetCharArrayRegion = new JNIFunction("GetCharArrayRegion", new Type[]{JNIENV, JCHAR_ARRAY, JSIZE, JSIZE, JCHAR}, VOID);
    public final static JNIFunction GetShortArrayRegion = new JNIFunction("GetShortArrayRegion", new Type[]{JNIENV, JSHORT_ARRAY, JSIZE, JSIZE, JSHORT}, VOID);
    public final static JNIFunction GetIntArrayRegion = new JNIFunction("GetIntArrayRegion", new Type[]{JNIENV, JINT_ARRAY, JSIZE, JSIZE, JINT}, VOID);
    public final static JNIFunction GetLongArrayRegion = new JNIFunction("GetLongArrayRegion", new Type[]{JNIENV, JLONG_ARRAY, JSIZE, JSIZE, JLONG}, VOID);
    public final static JNIFunction GetFloatArrayRegion = new JNIFunction("GetFloatArrayRegion", new Type[]{JNIENV, JFLOAT_ARRAY, JSIZE, JSIZE, JFLOAT}, VOID);
    public final static JNIFunction GetDoubleArrayRegion = new JNIFunction("GetDoubleArrayRegion", new Type[]{JNIENV, JDOUBLE_ARRAY, JSIZE, JSIZE, JDOUBLE}, VOID);
    public final static JNIFunction SetBooleanArrayRegion = new JNIFunction("SetBooleanArrayRegion", new Type[]{JNIENV, JBOOLEAN_ARRAY, JSIZE, JSIZE, JINT}, VOID);
    public final static JNIFunction SetByteArrayRegion = new JNIFunction("SetByteArrayRegion", new Type[]{JNIENV, JBYTE_ARRAY, JSIZE, JSIZE, JINT}, VOID);
    public final static JNIFunction SetCharArrayRegion = new JNIFunction("SetCharArrayRegion", new Type[]{JNIENV, JCHAR_ARRAY, JSIZE, JSIZE, JINT}, VOID);
    public final static JNIFunction SetShortArrayRegion = new JNIFunction("SetShortArrayRegion", new Type[]{JNIENV, JSHORT_ARRAY, JSIZE, JSIZE, JINT}, VOID);
    public final static JNIFunction SetIntArrayRegion = new JNIFunction("SetIntArrayRegion", new Type[]{JNIENV, JINT_ARRAY, JSIZE, JSIZE, JINT}, VOID);
    public final static JNIFunction SetLongArrayRegion = new JNIFunction("SetLongArrayRegion", new Type[]{JNIENV, JLONG_ARRAY, JSIZE, JSIZE, JINT}, VOID);
    public final static JNIFunction SetFloatArrayRegion = new JNIFunction("SetFloatArrayRegion", new Type[]{JNIENV, JFLOAT_ARRAY, JSIZE, JSIZE, JINT}, VOID);
    public final static JNIFunction SetDoubleArrayRegion = new JNIFunction("SetDoubleArrayRegion", new Type[]{JNIENV, JDOUBLE_ARRAY, JSIZE, JSIZE, JINT}, VOID);
    public final static JNIFunction RegisterNatives = new JNIFunction("RegisterNatives", new Type[]{JNIENV, JCLASS, JNINATIVE_METHOD, JINT}, JINT);
    public final static JNIFunction UnregisterNatives = new JNIFunction("UnregisterNatives", new Type[]{JNIENV, JCLASS}, JINT);
    public final static JNIFunction MonitorEnter = new JNIFunction("MonitorEnter", new Type[]{JNIENV, JOBJECT}, JINT);
    public final static JNIFunction MonitorExit = new JNIFunction("MonitorExit", new Type[]{JNIENV, JOBJECT}, JINT);
    public final static JNIFunction GetJavaVM = new JNIFunction("GetJavaVM", new Type[]{JNIENV, JAVAVM}, JINT);
    public final static JNIFunction GetStringRegion = new JNIFunction("GetStringRegion", new Type[]{JNIENV, JSTRING, JSIZE, JSIZE, JCHAR}, VOID);
    public final static JNIFunction GetStringUTFRegion = new JNIFunction("GetStringUTFRegion", new Type[]{JNIENV, JSTRING, JSIZE, JSIZE, CHAR}, VOID);
    public final static JNIFunction GetPrimitiveArrayCritical = new JNIFunction("GetPrimitiveArrayCritical", new Type[]{JNIENV, JARRAY, JBOOLEAN}, VOID);
    public final static JNIFunction ReleasePrimitiveArrayCritical = new JNIFunction("ReleasePrimitiveArrayCritical", new Type[]{JNIENV, JARRAY, VOID, JINT}, VOID);
    public final static JNIFunction GetStringCritical = new JNIFunction("GetStringCritical", new Type[]{JNIENV, JSTRING, JBOOLEAN}, JCHAR);
    public final static JNIFunction ReleaseStringCritical = new JNIFunction("ReleaseStringCritical", new Type[]{JNIENV, JSTRING, JCHAR}, VOID);
    public final static JNIFunction NewWeakGlobalRef = new JNIFunction("NewWeakGlobalRef", new Type[]{JNIENV, JOBJECT}, JWEAK);
    public final static JNIFunction DeleteWeakGlobalRef = new JNIFunction("DeleteWeakGlobalRef", new Type[]{JNIENV, JWEAK}, VOID);
    public final static JNIFunction ExceptionCheck = new JNIFunction("ExceptionCheck", new Type[]{JNIENV}, JBOOLEAN);
    public final static JNIFunction NewDirectByteBuffer = new JNIFunction("NewDirectByteBuffer", new Type[]{JNIENV, VOID, JLONG}, JOBJECT);
    public final static JNIFunction GetDirectBufferAddress = new JNIFunction("GetDirectBufferAddress", new Type[]{JNIENV, JOBJECT}, VOID);
    public final static JNIFunction GetDirectBufferCapacity = new JNIFunction("GetDirectBufferCapacity", new Type[]{JNIENV, JOBJECT}, JLONG);
    public final static JNIFunction GetObjectRefType = new JNIFunction("GetObjectRefType", new Type[]{JNIENV, JOBJECT}, JOBJECT_REF_TYPE);

    // for JavaVM
    public final static JNIFunction DestroyJavaVM = new JNIFunction("DestroyJavaVM", new Type[]{JAVAVM}, JINT);
    public final static JNIFunction AttachCurrentThread = new JNIFunction("AttachCurrentThread", new Type[]{JAVAVM, JNIENV, VOID}, JINT);
    public final static JNIFunction DetachCurrentThread = new JNIFunction("DetachCurrentThread", new Type[]{JAVAVM}, JINT);
    public final static JNIFunction GetEnv = new JNIFunction("GetEnv", new Type[]{JAVAVM, JNIENV, JINT}, JINT);
    public final static JNIFunction AttachCurrentThreadAsDaemon = new JNIFunction("AttachCurrentThreadAsDaemon", new Type[]{JAVAVM, JNIENV, VOID}, JINT);

    private final static Logger logger = LoggerFactory.getLogger(JNIFunction.class);

    public JNIFunction(String name, Type[] parameterType, Type returnType) {
        super(name, parameterType, returnType);
    }

    public boolean exec(NativeLibraryHandler handler, LibraryModule module, Subroutine subroutine, Stack<Subroutine> depth) throws IOException, Z3Exception {
        logger.info("JNI Function Call : {}", name);
        JNIValue[] params = new JNIValue[parameterTypes.length];
        JNIValue result = null;
        if(returnType!=VOID) {
            result = new JNIValue(JNIValue.RESULT, returnType);
        }

        int index = 1;
        for (int i = 1; i < parameterTypes.length; i++) { // prepare parameters; index 0 is JNIEvn all the time
            AbstractValue v = handler.getValueOfParameter(index); // get actual param pattern from regs and stack
            if (v.isJNIValue()) {
                params[i] = (JNIValue)v;
            } else if (parameterTypes[i] == JLONG) {
                Long value = Misc.combineToLong(v.intValue(), (handler.getValueOfParameter(++index).intValue()));
                if(value==null) {
                    params[i] = new JNIValue(parameterTypes[i], Immediate.newValue(JLONG));
                } else {
                    params[i] = new JNIValue(parameterTypes[i], new Immediate(value));
                }
            } else if(parameterTypes[i] == DOUBLE && (((Immediate)v).getNumeral().getType()!=Modifier.TYPE_DOUBLE)) {
                Double value = Misc.combineToDouble(v.intValue(), (handler.getValueOfParameter(++index).intValue()));
                if (value == null) {
                    params[i] = new JNIValue(parameterTypes[i], Immediate.newValue(DOUBLE));
                } else {
                    params[i] = new JNIValue(parameterTypes[i], new Immediate(value));
                }
            } else {
                params[i] = new JNIValue(parameterTypes[i], v);
            }

//            if (params[i].getValue() == AbstractValue.empty && parameterTypes[i] != NEW) {
//                if (returnType != VOID) {
//                    Register.setValue(Register.R0, AbstractValue.empty);
//                }
//                return false;
//            }
            index++;
        }


        if (this == GetJavaVM) {
            params[1].getAddress().write(handler.pVM);
        } else if (this == GetEnv) {
            params[1].getAddress().write(handler.pEnv);
            result.setValue(JNIValue.JNI_OK);
        } else if (this == NewGlobalRef) {
            result.setValue(new AllocatedMemory(params[1].getAddress().read()));
        } else if (this == GetObjectClass) {
            if(params[1].isJNIValue()) {
                result.setValue(params[1].getValue());
            } else {
                result.setValue(new AllocatedMemory(new SootValue(((RefType) (((SootValue) params[1].getValue()).getSootValue())).getSootClass())));
            }
                // jclass GetObjectClass(JNIEnv *env, jobject obj);
//            System.out.println(params[1]);

//            if (result != null && params[1].isStaticValue()) {
            // the second parameter should be class type
//                result.setValue(new TypeValue(((SootClass) params[1].getValue()).getType()));
//                functionCall.setIgnorable();
//            }
        } else if (this == FindClass) {
            StringValue className = getStringValue(module, params[1]);
            logger.info("Class name is {}", className);
            result.setValue(new AllocatedMemory(new SootValue(Misc.getSootClass(className))));
        } else if (this == RegisterNatives) {
            SootClass sc = (SootClass)((SootValue) params[1].read()).getSootValue();
            if (sc != null && !sc.isPhantom()) {
                int methodSize = params[3].intValue();
                Address address = params[2].getAddress();
                for(int i=0;i<methodSize;i++) {
                    String methodName = getStringValue(module, address.read(i * 12, Modifier.TYPE_INT)).toString();
                    String token[] =   getStringValue(module, address.read(i * 12 + 4, Modifier.TYPE_INT)).toString().split("\\)");; //((Address)address.read(i*12+4, 0)).toString().split("\\)");
                    token[0] = token[0].substring(1);
                    Integer addr = address.read(i * 12 + 8, Modifier.TYPE_INT).intValue()&~1;

                    if(!sc.declaresMethod(methodName, Misc.getTypeList(token[0]))) {
                        throw new RuntimeException("there is no native method named "+ methodName +" at " + sc.getName());
                    }
                    SootMethod m = sc.getMethod(methodName, Misc.getTypeList(token[0]), Misc.getType(token[1]));
                    String subroutineName = Misc.javaMingling(sc.getName(), methodName, token[0]);
                    NativeMethod nm = new NativeMethod(module, subroutineName, addr, m);
                    logger.info("Register native "+m.getSignature());
                    module.addSubroutine(subroutineName, nm);
                    handler.addNativeMethodMap(nm);
                }
            }
            result.setValue(JNIValue.JNI_OK);
        } else if (this == GetFieldID) {
            SootClass sc = (SootClass)((SootValue) params[1].read()).getSootValue();
            if (sc != null && !sc.isPhantom()) {
                String fieldName = getStringValue(module, params[2]).toString();
                Type fieldType = Misc.getType(getStringValue(module, params[3]).toString());
                SootField sootField = Misc.getField(sc, fieldName, fieldType);
                if(sootField!=null) {
                    result.setValue(new AllocatedMemory(new SootValue(sootField)));
                } else {
                    logger.error("field id is not found");
                    handler.setJNIException();
                    result.setValue(Immediate.ZERO);
                }
//                if (sc.declaresField(fieldName, fieldType)) {
//                    SootField sootField = sc.getField(fieldName, fieldType);
//                    //   functionCall.addField(sootField);
//
//                    result.setValue(new AllocatedMemory(new SootValue(sootField)));
//                }
            }
        } else if (this == GetStaticFieldID) {
            SootClass sc = (SootClass)((SootValue) params[1].read()).getSootValue();
            if (sc != null && !sc.isPhantom()) {
                String fieldName = getStringValue(module, params[2]).toString();
                Type fieldType = Misc.getType(getStringValue(module, params[3]).toString());
                if (sc.declaresField(fieldName, fieldType)) {
                    SootField sootField = sc.getField(fieldName, fieldType);
                    result.setValue(new AllocatedMemory(new SootValue(sootField)));
                } else {
                    logger.error("field id is not found");
                    handler.setJNIException();
                    result.setValue(Immediate.ZERO);
                }
            }
        } else if (this == GetMethodID || this == GetStaticMethodID) {
            SootClass sc = (SootClass) ((SootValue) params[1].read()).getSootValue();
            if (sc != null && !sc.isPhantom()) {
                StringValue methodName = getStringValue(module, params[2]);
                StringValue strParams = getStringValue(module,params[3]);
                logger.info("Class name:{} Method name:{} params{}", sc.getName(), methodName, strParams);
                List<Type> paramTypes = Misc.getTypeList(strParams.toString().substring(1, strParams.toString().indexOf(")")));
                SootMethod method = Misc.getMethod(sc, methodName.toString(), paramTypes);
                if(method!=null) {
                    result.setValue(new AllocatedMemory(new SootValue(method)));
                } else {
                    logger.error("method id is not found");
                    handler.setJNIException();
                    result.setValue(Immediate.ZERO);
                }
            }
        } else if(this == GetStaticIntField ) {
            if(subroutine.isNativeMethod()) {
                NativeMethod method = (NativeMethod)subroutine;
                Value[] values = method.getLocal(handler, params, this);
                SootFieldRef sootFieldRef = null;
                if(values[1]==null) {
                    sootFieldRef = ((SootField)((SootValue)params[2].read()).getSootValue()).makeRef();
                }
                FieldRef fieldRef = soot.jimple.Jimple.v().newStaticFieldRef(sootFieldRef);
                Local value = method.getLocal(fieldRef);
                method.addUnit(Jimple.v().newAssignStmt(value, fieldRef));
                result.setValue(new AllocatedMemory(new SootValue(fieldRef)));
            }
        } else if (this == GetObjectField || this == GetBooleanField || this == GetByteField
                || this == GetCharField || this == GetShortField || this == GetIntField
                || this == GetLongField || this == GetFloatField || this == GetDoubleField) {
            if (subroutine.isNativeMethod()) {
                NativeMethod method = (NativeMethod) subroutine;
                Value[] values = method.getLocal(handler, params, this);
                SootFieldRef sootFieldRef = null;
                if (values[1] == null) {
                    sootFieldRef = ((SootField) ((SootValue) params[2].read()).getSootValue()).makeRef();
                }
                FieldRef fieldRef = soot.jimple.Jimple.v().newInstanceFieldRef(values[0], sootFieldRef);
                Local value = method.getLocal(fieldRef);
                method.addUnit(Jimple.v().newAssignStmt(value, fieldRef));
                result.setValue(new AllocatedMemory(new SootValue(fieldRef)));
            }
        } else if( this == SetObjectField || this == SetBooleanField || this == SetByteField 
                || this == SetCharField || this == SetShortField || this == SetIntField 
                || this == SetLongField || this == SetFloatField || this == SetDoubleField){  
         // nothing
        } else if (this == GetStringUTFChars || this == NewStringUTF) {
            result.setValue(new AllocatedMemory(getStringValue(module, params[1])));
        } else if (this == GetArrayLength ) {
            result.setValue(Immediate.newValue(TYPE_INT));
        } else if (this == GetIntArrayElements) {
            result.setValue(Immediate.newValue(TYPE_INT));
        } else if (this == GetByteArrayElements) {
            result.setValue(Immediate.newValue(TYPE_BYTE)); // TODO
        } else if (this == GetFloatArrayElements) {
            result.setValue(Immediate.newValue(TYPE_FLOAT)); // TODO
        } else if (this == GetObjectArrayElement) {
            result.setValue(new SootValue(((ArrayType)params[1].getValueType()).baseType));
        } else if(this == CallVoidMethodV || this == CallObjectMethodV || this==CallStaticIntMethodV || this==CallStaticObjectMethodV) {
            // CallObjectMethodV(JNIEnvironment env, int objJREF, int methodID, Address argAddress)
            // CallVoidMethodV(JNIEnvironment env, int objJREF, int methodID, Address argAddress)
            if (subroutine.isNativeMethod()) {
                NativeMethod method = (NativeMethod) subroutine;
                Value[] values = method.getLocal(handler, params, this); // search data from results of other subroutines.
                try {
                    SootMethodRef methodRef = ((SootMethod) ((SootValue) params[2].read()).getSootValue()).makeRef();
//                    if (values[1] == null) {
//                        methodRef = ((SootMethod) ((SootValue) params[2].read()).getSootValue()).makeRef();
//                    }
                    // parameter.....handling
                    if (this == CallObjectMethodV || this == CallStaticObjectMethodV) {
                        result.setValue(new SootValue(methodRef.returnType()));
                    } else if(this == CallStaticIntMethodV) {
                        result.setValue(new Immediate());
                    }
                    method.addUnit(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr((Local) values[0], methodRef)));
                    System.out.println("method call :" + methodRef.getSignature());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }



        } else if (this == NewByteArray) {
            result.setValue(new AllocatedMemory(params[1]));
        } else if (this == NewIntArray) {
            result.setValue(new AllocatedMemory(new Immediate(params[1].getValue().intValue() * Instruction.WORD)));
        } else if (this == ReleaseByteArrayElements) {
            Register.setValue(Register.R2, Immediate.ZERO);
        } else if (this == ReleaseStringUTFChars) {
            ((IMemoryValue)params[2].getAddress()).free();
//            heapMemory.free(params[2].getAddress());
        } else if (this == DeleteLocalRef) {
            if(params[1].isIMemoryValue()) {
                ((IMemoryValue) params[1].getAddress()).free();
            } else {
                Register.setValue(Register.R1, Immediate.ZERO);
            }
//            heapMemory.free(params[1].getAddress());
        } else if (this== ExceptionCheck) { // we assume there is no exception for all jni functions
            if (handler.isJNIException()) {
                Register.setValue(Register.R0, Immediate.ONE);
                handler.clearJNIException();
            } else {
                Register.setValue(Register.R0, Immediate.ZERO);
            }
            return false;
        } else if (this==GetByteArrayRegion) { // return void
            params[3].getAddress().write(new JNIValue(JNIValue.RESULT, JBYTE_ARRAY));
        } else if (this==GetShortArrayRegion) { // return void
            params[3].getAddress().write(new JNIValue(JNIValue.RESULT, JSHORT_ARRAY));
        } else if (this== ExceptionClear) {
            handler.clearJNIException();
        } else if (this == ExceptionOccurred) {
            return true; // abort - something wrong
        } else if (this == AttachCurrentThreadAsDaemon) { // restricted methods
            params[1].getAddress().write(handler.pEnv);
            result.setValue(JNIValue.JNI_OK);
        } else if (this== SetIntArrayRegion || this == SetByteArrayRegion || this == SetShortArrayRegion ) {
            if(params[2].isPseudoValue()) {
                // int

            }
            if(params[3].isPseudoValue()) {
                // len
            }
            if(params[4].isPseudoValue()) {
                // point...
            }
            // TODO
        } else {

            //            case GetByteArrayRegion:
            //                System.out.println(params.get(1) + " " + params.get(1).getParamIndex());
            //                System.out.println(params.get(2)+ " " + params.get(2).getParamIndex());
            //                System.out.println(params.get(3)+ " " + params.get(3).getParamIndex());
            //
            throw new RuntimeException("undefined JNI " + name);
        }

        if(returnType!=VOID) {
            if (result != null) {
                Register.setValue(Register.R0, result);
            } else {
                Register.clear(Register.R0);
            }
        }
        return false; // true for exit?
    }
}
