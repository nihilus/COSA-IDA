package com.tosnos.cosa.binary.asm.value;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.NativeLibraryHandler;
import com.tosnos.cosa.binary.asm.Modifier;
import com.tosnos.cosa.binary.asm.value.memory.*;
import java.io.IOException;
import java.lang.annotation.Native;
import java.util.*;

/**
 * Created by kevin on 8/27/15.
 */

// TODO: change size, offset
public class PseudoValue extends AbstractValue implements IMemoryValue {


    public enum PossibleType { UNKNOWN, STRING, INT, ADDRESS, FLOAT, DOUBLE, LONG }
    private PossibleType possibleType = PossibleType.UNKNOWN;
    private AbstractMemory memory;
    private Multimap<Integer, Map.Entry<AbstractValue, Byte>> dataStructureMap; // Address, value, type
    private AbstractValue reference;
    private AbstractValue replacedValue = null;
    public enum ReferenceType { DIRECT, INDIRECT, TEMPORARY }
    private ReferenceType refType;
    private Numeral offset;
    private byte type;


    public PseudoValue(AbstractValue reference, byte type) { // indirect: this value is come out from refrence
        this(reference, type, ReferenceType.INDIRECT, Numeral.newValue(type), null, null);
        if (reference.isIMemoryValue()) { // put address
            ((IMemoryValue) reference).write(this, type);
        }
    }

    public PseudoValue(AbstractValue reference, byte type, AbstractValue replacedValue) {
        this.reference = reference;
        this.type = type;
        this.refType = ReferenceType.TEMPORARY;
        this.replacedValue = replacedValue;
    }

    public void setPossibleType(PossibleType refType) {
        if(possibleType!=PossibleType.UNKNOWN && possibleType!=refType) {
            throw new RuntimeException("possible type is already defined");
        }
        possibleType = refType;
    }

    public void setAsAllocatedMemory() {
        replacedValue = new AllocatedMemory();
        ((Address)reference).write(replacedValue);
    }


    public PseudoValue(AbstractValue reference, byte type, ReferenceType refType) {
        this(reference, type, refType, Numeral.newValue(type), null, null);
    }

    private PseudoValue(AbstractValue reference, byte type, ReferenceType refType, Numeral numeral, Multimap<Integer, Map.Entry<AbstractValue, Byte>> dataStructureMap, AbstractMemory memory) { // direct
        this.reference = reference;
        this.refType = refType;
        this.type = type;
        this.offset = numeral;

        this.dataStructureMap = ArrayListMultimap.create();
        if(dataStructureMap!=null) {
            this.dataStructureMap.putAll(dataStructureMap);
        }
        this.memory = new AbstractMemory("P", memory); // pseudoValue
    }


    @Override
    public AbstractValue getReplacement(Numeral v) {
        if(replacedValue!=null) {
            return replacedValue.getReplacement(v);
        }

        if(refType == ReferenceType.TEMPORARY) {
            return new PseudoValue(reference, type, replacedValue.getReplacement(v));
        }

        if(!v.isUnknown() && v.intValue()==0) {
            return this;
        }
        return new PseudoValue(this, type, ReferenceType.DIRECT, v, dataStructureMap, memory);
    }

    @Override
    public LibraryModule getModule() {
        if(replacedValue!=null && replacedValue.isIMemoryValue()) {
            return ((IMemoryValue)replacedValue).getModule();
        }

//        if(refType!=ReferenceType.PTHREAD_KEY && reference.isIMemoryValue()) {

        if(reference.isIMemoryValue()) {
            return ((IMemoryValue)reference).getModule();
        }

        return null;
    }

    public String toString() {
        if(replacedValue!=null) {
            return "PR:("+replacedValue+")";
        }
        if(refType==ReferenceType.DIRECT) {
            return "PD:0x"+offset+"+("+reference+")";
        } else if(refType==ReferenceType.INDIRECT){
            return "PI:("+reference+")";
        } else if(refType==ReferenceType.TEMPORARY) {
            return "PT:"+replacedValue+"("+reference+")";
        }
        return "P:"+refType.toString()+"("+reference+")";
    }

    public ReferenceType getReferenceType() {
        return refType;
    }

    public AbstractValue getReference() {
        return reference;
    }

    @Override
    public Integer intValue() {
        if(replacedValue!=null) {
            return replacedValue.intValue();
        }
        return offset.intValue();
    }

    @Override
    public boolean isPseudoValue() {
        return true;
    }

    @Override
    public AbstractValue getValue() {
        if(replacedValue!=null) {
            return replacedValue;
        }
        return this;
    }

    @Override
    public Numeral getNumeral() {

        return offset;
    }

    @Override
    public boolean isIMemoryValue() {
        if(refType==ReferenceType.TEMPORARY ) { //|| refType==ReferenceType.PTHREAD_KEY) {
            return false;
        }
        return true;
    }

    public AbstractValue deepRead() {
        if(replacedValue!=null) {
            return replacedValue;
        }
        if(reference.isAddress()) {
            AbstractValue v = ((Address)reference).deepRead(0, type);
            if(v!=null) {
                replacedValue = v;
                return v;
            }
        }
        return null;
    }

    public AbstractValue read(byte type) throws IOException {
        if(replacedValue!=null) {
//            return ((IMemoryValue)replacedValue).read(offset, type);
            return ((IMemoryValue)replacedValue).read(type);
        }

        AbstractValue value = null;
        for(Map.Entry<AbstractValue, Byte> e:dataStructureMap.get(offset.intValue())) {
            if(e.getValue()==type) {
                value = e.getKey();
                break;
            }
        }

        if(value==null) {
            value = new PseudoValue(this, type);
        }
        return value;
    }

    @Override
    public void memmove(IMemoryValue v, AbstractValue len) {
        if(replacedValue!=null) {
            ((IMemoryValue)replacedValue).memmove(v, len);
            return;
        }

        Integer length = len.intValue();
        if(length!=null) {
//            if(size==HeapMemory.UNKNOWN_ALLOCATED_SIZE || size<length) {
//                size = length;
//            }
            for (int i = 0; i < length; i++) {
                AbstractMemory.MemoryCell cell = v.removeMemoryCell(i);
                if(cell==null) {
                    removeMemoryCell(i);
                } else {
                    putMemoryCell(i, cell);
                }
            }
        }
        return;
    }

    @Override
    public AbstractMemory.MemoryCell getMemoryCell(int address) {
        if(replacedValue!=null) {
            return ((IMemoryValue)replacedValue).getMemoryCell(address);
        }
        return memory.getMemoryCell(offset.intValue() + address);
    }

    @Override
    public AbstractMemory.MemoryCell putMemoryCell(int address, AbstractMemory.MemoryCell cell) {
        if(replacedValue!=null) {
            return ((IMemoryValue)replacedValue).putMemoryCell(address, cell);
        }
        return memory.putMemoryCell(offset.intValue() + address, cell);
    }

    @Override
    public AbstractMemory.MemoryCell removeMemoryCell(int address) {
        if(replacedValue!=null) {
            return ((IMemoryValue)replacedValue).removeMemoryCell(address);
        }
        return memory.removeMemoryCell(offset.intValue() + address);
    }

    @Override
    public void memset(AbstractValue v, AbstractValue len) {
        if(replacedValue!=null) {
            ((IMemoryValue)replacedValue).memset(v, len);
            return;
        }

        if(len.isImmediate()) {
            Integer length = len.intValue();
            if(length!=null) {
//                if (size == HeapMemory.UNKNOWN_ALLOCATED_SIZE || size < length) {
//                    size = length;
//                }
                for (int i = 0; i < length; i++) {
                    memory.write(i, v, Modifier.TYPE_BYTE);
                }
            }
        }
    }


    @Override
    public void memcpy(IMemoryValue v, AbstractValue len) {
        if(replacedValue!=null) {
            ((IMemoryValue)replacedValue).memcpy(v, len);
            return;
        }

        Integer length = len.intValue();
        if(length!=null) {
//            if(size==HeapMemory.UNKNOWN_ALLOCATED_SIZE || size<length) {
//                size = length;
//            }
            for (int i = 0; i < length; i++) {
                AbstractMemory.MemoryCell cell = v.getMemoryCell(i);
                if(cell==null) {
                    removeMemoryCell(i);
                } else {
                    putMemoryCell(i, cell);
                }
            }
        }
    }


    public int write(AbstractValue value, byte type) {
        if(replacedValue!=null) {
//            ((IMemoryValue)replacedValue).write(offset.intValue(), value, type);
            return ((IMemoryValue)replacedValue).write(value, type);
        } else {
            dataStructureMap.put(this.offset.intValue(), new AbstractMap.SimpleEntry(value, type));
            memory.write(this.offset.intValue(), value, type);
            return -1;
        }
    }

    public int writeText(String str) {
        throw new RuntimeException("not yet");
        /*
        if(replacedValue!=null) {
            ((IMemoryValue)replacedValue).write(offset, str);
        }
        dataStructureMap.put(offset, new AbstractMap.SimpleEntry(str));
        memory.write(offset, str);
        return -1;*/
    }

    public void setReplacedValue(AllocatedMemory allocatedMemory) {
        if(refType==ReferenceType.INDIRECT) {
            ((Address)reference).write(allocatedMemory);
            for(Map.Entry<Integer, Map.Entry<AbstractValue, Byte>> entry:dataStructureMap.entries()) {
                Integer offset = entry.getKey();
                Map.Entry<AbstractValue, Byte> e = entry.getValue();
                allocatedMemory.write(offset, e.getKey(), e.getValue());
            }
            replacedValue = allocatedMemory;
            return;
        }
        throw new RuntimeException("not implemented");
    }


    @Override
    public void free() {
        if(refType==ReferenceType.INDIRECT) {
            AllocatedMemory memory = new AllocatedMemory();
            ((IMemoryValue)reference).write(memory, Modifier.TYPE_INT);
            for(Map.Entry<Integer, Map.Entry<AbstractValue, Byte>> entry:dataStructureMap.entries()) {
                Integer offset = entry.getKey();
                Map.Entry<AbstractValue, Byte> e = entry.getValue();
                memory.write(offset, e.getKey(), e.getValue());
            }
           // replacedValue = memory;
           // return replacedValue;
        } else {

            throw new RuntimeException("not implemented");
        }
    }


    public AbstractValue realloc(Immediate value) {
        if(replacedValue!=null) {
            if(replacedValue.isAllocatedMemory()) {
                return ((AllocatedMemory)replacedValue).realloc(value);
            }  else {
                throw new RuntimeException("error");
            }
        }

        if(refType==ReferenceType.INDIRECT) {
            AllocatedMemory memory = new AllocatedMemory(value);
            setReplacedValue(memory);
            return memory;
        }
        throw new RuntimeException("not implemented");
    }


    public AbstractValue getReplacedValue() {
        return replacedValue;
    }
}
