package com.tosnos.cosa.binary.asm.value;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.tosnos.cosa.binary.asm.Modifier;
import com.tosnos.cosa.binary.asm.value.memory.AbstractMemory;
import com.tosnos.cosa.binary.asm.value.memory.Address;
import com.tosnos.cosa.binary.asm.value.memory.IMemoryValue;

import java.io.IOException;
import java.util.*;

/**
 * Created by kevin on 8/22/15.
 */
public class AllocatedMemory extends Address implements IMemoryValue {
    private Immediate size = null;
    private final static Numeral start = new Numeral(8);; // start = 8;
//    private NavigableMap<Integer, AbstractValue> memsetValues;
    private static int count = 0;
    private Multimap<Integer, AbstractValue> dataStructureMap;
    private AbstractValue memsetValue = null;

    public AllocatedMemory(Immediate size) {
        this(new AbstractMemory("H"+count++), start, size,  null, ArrayListMultimap.create());
        memory.write(0, this.size); // 8 byte for size infomation
    }

    public AllocatedMemory() {
        this(Immediate.newValue(TYPE_INT));
    }

    public AllocatedMemory(String str) {
        this(new Immediate(str.length()+1));
        super.writeText(str);
    }

    private AllocatedMemory(AbstractMemory memory, Numeral address, Immediate size, AbstractValue memsetValue, Multimap<Integer, AbstractValue> dataStructureMap) {
        super(memory, address);
        this.size = size;
        this.dataStructureMap = dataStructureMap;
        this.memsetValue = memsetValue;
    }

    public AllocatedMemory(AbstractValue value) { // for address
        this(Immediate.WORD);
        write(0, value, Modifier.TYPE_INT);
    }

    @Override
    public AbstractValue getReplacement(Numeral value) {

        return new AllocatedMemory(memory, value, size, memsetValue, dataStructureMap);
    }

    public void setDataStructureMap(Multimap<Integer, AbstractValue> map) {
        dataStructureMap.putAll(map);
    }

    public AbstractValue read(byte type) throws IOException {
        AbstractValue v = super.read(type); // TODO: memset data...
        if(v==null) {
            if(memsetValue!=null) {
                return memsetValue;
            }
            v = Immediate.newValue(type);
            write(0, v, type);
//            v = new PseudoValue(this, type);
        }
        return v;
    }

    public void setDataStructureMap(Integer address, AbstractValue value) {
        dataStructureMap.put(address, value);
    }

    public Collection<AbstractValue> getDataStructure(Integer address) {
        return dataStructureMap.get(address);
    }

    public int write(int offset, AbstractValue value, byte type) {
        dataStructureMap.put(offset, value);
        return super.write(offset, value, type);
    }

    public byte getValueType() {
        return Modifier.TYPE_INT;
    }

    @Override
    public boolean isAllocatedMemory() {
        return true;
    }

    public AbstractValue size() {
        return size;
    }

    public void setSize(int size) {
        if(!this.size.isUnknown()) {
            this.size = new Immediate(size);
            memory.write(0, this.size); // 8 byte for size infomation
        } else {
            throw new RuntimeException("N/A");
        }
//        else if(((UnknownImmediate)this.size).getLowerBound().intValue()<size) {
//            ((UnknownImmediate)this.size).setLowerBound(size);
//        }
    }

    public void free() { // ??
        // release
    }

    @Override
    public void memset(AbstractValue v, AbstractValue len) {
        memsetValue = v;
    }

    public AllocatedMemory realloc(Immediate size) {
        if(size.isUnknown()) {
            throw new RuntimeException("N/A");
        }
        this.size = size;
        memory.write(0, this.size);
        return this;
    }

    /*

      public AllocatedMemory alloc(int size) {
        int address = bottom + 4;
        if (size <= 0) {
            size = UNKNOWN_ALLOCATED_SIZE;
        }
        AllocatedMemory data = new AllocatedMemory(this, address, size);
        super.write(bottom, new Immediate(size), 0);
        bottom += size + (size & 3) + 4; // 8 byte for size infomation
        dataMap.put(Integer.toUnsignedLong(address), data);
        multiDataMap.put(Integer.toUnsignedLong(address), data);
        return data;
    }
     */
}
