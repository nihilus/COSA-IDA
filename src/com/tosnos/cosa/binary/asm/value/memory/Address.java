package com.tosnos.cosa.binary.asm.value.memory;

import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.value.*;
import com.tosnos.cosa.util.Pair;

import java.io.IOException;
import java.util.NavigableMap;
import java.util.TreeMap;

public class Address extends AbstractValue implements IMemoryValue {
    protected NavigableMap<Integer, Pair<Integer, AbstractValue>> setValueMap = new TreeMap<Integer, Pair<Integer, AbstractValue>>();
    protected final AbstractMemory memory;
    protected Numeral address;

    public Address(AbstractMemory memory, int address) {
        this.address = new Numeral(address);
        this.memory = memory;
    }

    public Address(AbstractMemory memory, Numeral address) {
        this.address = address;
        this.memory = memory;
    }

    @Override
    public boolean isUnknown() {
        return address.isUnknown();
    }

    @Override
    public boolean isImmediate() {
        return false;
    }

    public AbstractMemory getMemory() {
        return this.memory;
    }

    public AbstractValue read() throws IOException {
        return read(TYPE_INT);
    }

    public AbstractValue deepRead(int offset, byte type) { // read from backend memory
        return memory.deepRead(address.intValue() + offset, type);
    }

    public AbstractValue read(byte type) throws IOException {
        return read(0, type);
    }

    public AbstractValue read(int offset, byte type) throws IOException {
        if(address.isUnknown()) {
            return memory.read(address.add(offset), type);
        }
        return memory.read(address.intValue()+offset, type);
    }

//    @Override
    public void memset(AbstractValue v, AbstractValue len) {
        // it may be for the stack
        if(!len.isUnknown()) {
            for (int i = 0; i < len.intValue(); i++) {
                memory.write(address.intValue() + i, v, TYPE_BYTE);
            }
        }
    }

//    @Override
    public void memcpy(IMemoryValue v, AbstractValue len) {
        // it may be for the stack
        if(!len.isUnknown()) {
//            if(size==HeapMemory.UNKNOWN_ALLOCATED_SIZE || size<length) {
//                size = length;
//            }
            for (int i = 0; i < len.intValue(); i++) {
                AbstractMemory.MemoryCell cell = v.getMemoryCell(i);
                if(cell==null) {
                    removeMemoryCell(i);
                } else {
                    putMemoryCell(i, cell);
                }
            }
        }
    }

//    @Override
    public void memmove(IMemoryValue v, AbstractValue len) {
        // it may be for the stack
        if(!len.isUnknown()) {
//            if(size==HeapMemory.UNKNOWN_ALLOCATED_SIZE || size<length) {
//                size = length;
//            }
            for (int i = 0; i < len.intValue(); i++) {
                AbstractMemory.MemoryCell cell = v.removeMemoryCell(i);
                if(cell==null) {
                    removeMemoryCell(i);
                } else {
                    putMemoryCell(i, cell);
                }
            }
        }
    }

//    @Override
    public LibraryModule getModule() {
        if(memory instanceof InternalMemory) {
            return ((InternalMemory)memory).getModule();
        }
        return null;
    }

    public int writeText(String str) {
        return memory.writeText(address.intValue(), str);
    }

    @Override
    public void free() {
        throw new RuntimeException("N/A");
    }

    public int write(AbstractValue v) {
        return memory.write(address, v, TYPE_INT);
    }

    public int write(AbstractValue v, byte type) {
        return memory.write(address, v, type);
    }


//    public int write(int offset, AbstractValue v) {
//        return memory.write(address+offset, v, v.getValueType());
//    }

    public int write(int offset, AbstractValue v, byte type) {
        if(address.isUnknown()) {
            return memory.write(address.add(offset), v, type);
        }
        return memory.write(address.intValue()+offset, v, type);
    }

    @Override
    public AbstractValue getReplacement(Numeral v) {
        return new Address(memory, v);
    }

    @Override
    public boolean isAddress() {
        return true;
    }

    public String toString() {
        return memory.toString()+":"+address;
    }

//
//    @Override
//    public int compareTo(Address o) {
//        final int BEFORE = -1;
//        final int EQUAL = 0;
//        final int AFTER = 1;
//
//        if(address < o.address) {
//            return BEFORE;
//        }
//
//        if(address == o.address & memory==o.memory) {
//            return EQUAL;
//        }
//
//        return AFTER;
//    }

    @Override
    public Integer intValue() {
        return address.intValue();
    }

    @Override
    public Numeral getNumeral() {
        return address;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) return true;
        if ( !(obj instanceof Address) ) return false;

        Address other = (Address)obj;

        if(address == other.address && memory == other.memory) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if(memory!=null && address!=null) {
            return memory.hashCode() + address.hashCode();
        }
        return super.hashCode();
    }

    @Override
    public boolean isIMemoryValue() {
        return true;
    }

//    @Override
    public AbstractMemory.MemoryCell getMemoryCell(int offset) {
        return memory.getMemoryCell(address.intValue()+offset);
    }

//    @Override
    public AbstractMemory.MemoryCell putMemoryCell(int offset, AbstractMemory.MemoryCell cell) {
        return memory.putMemoryCell(address.intValue() + offset, cell);
    }

//    @Override
    public AbstractMemory.MemoryCell removeMemoryCell(int offset) {
        return memory.removeMemoryCell(address.intValue() + offset);
    }
}
