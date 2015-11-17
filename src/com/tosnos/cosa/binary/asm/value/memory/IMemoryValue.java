package com.tosnos.cosa.binary.asm.value.memory;

import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.Immediate;

import java.io.IOException;

/**
 * Created by kevin on 8/30/15.
 */
public interface IMemoryValue {

//    int write(int offset, AbstractValue value, byte type);

//    AbstractValue read(int offset, byte type) throws IOException;
    AbstractValue read(byte type) throws IOException;
    int write(AbstractValue value, byte type);
//    int write(String str);
    void free();
    void memset(AbstractValue v, AbstractValue len);
    void memcpy(IMemoryValue v, AbstractValue len);
    void memmove(IMemoryValue v, AbstractValue len);
    AbstractMemory.MemoryCell getMemoryCell(int address);
    AbstractMemory.MemoryCell putMemoryCell(int address, AbstractMemory.MemoryCell cell);
    AbstractMemory.MemoryCell removeMemoryCell(int address);
    LibraryModule getModule();
}
