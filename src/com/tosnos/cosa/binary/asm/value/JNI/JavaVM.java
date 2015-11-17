package com.tosnos.cosa.binary.asm.value.JNI;

import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.memory.Address;
import com.tosnos.cosa.binary.asm.value.memory.AbstractMemory;
import com.tosnos.cosa.binary.function.JNIFunction;

/**
 * Created by kevin on 12/28/14.
 */
public class JavaVM extends Address {
    private final static int start = 0x8;
    private static AbstractMemory functions =  new AbstractMemory("VM");
    public static JavaVM vm = new JavaVM(start);

    static {
        functions.write(start + 0xc, JNIFunction.DestroyJavaVM);
        functions.write(start + 0x10, JNIFunction.AttachCurrentThread);
        functions.write(start + 0x14, JNIFunction.DetachCurrentThread);
        functions.write(start + 0x18, JNIFunction.GetEnv);
        functions.write(start + 0x1c, JNIFunction.FromReflectedMethod);
        functions.write(start + 0x20, JNIFunction.AttachCurrentThreadAsDaemon);
    }

    private JavaVM(int offset) {
        super(functions, offset);
    }

    public AbstractValue getReplacement(Number value) {
        return new JavaVM(value.intValue());
    }
}