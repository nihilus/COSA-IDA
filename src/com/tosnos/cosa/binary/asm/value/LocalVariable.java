package com.tosnos.cosa.binary.asm.value;

import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.value.memory.AbstractMemory;
import com.tosnos.cosa.binary.asm.value.memory.Address;
import com.tosnos.cosa.binary.asm.value.memory.IMemoryValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by kevin on 10/19/15.
 */
public class LocalVariable extends AbstractValue implements IMemoryValue{
    private final static Logger logger = LoggerFactory.getLogger(LocalVariable.class);
    protected final String name;
    protected final LibraryModule module;
    private int value;
    private Address address;

    public LocalVariable(LibraryModule module, String name) {
        this(module, name, parseAddress(name));
    }

    public LocalVariable(LibraryModule module, String name, int value) {
        this.value = value;
        this.module = module;
        this.name = name;
        this.address = new Address(module.getInternalMemory(), value);
    }

    public Address getAddress() {
        return address;
    }

    private static int parseAddress(String name) {
        int address = 0;
        String[] tokens = name.split("_");
        // loc, off, dword, sub, byte,
        if (tokens.length == 2) {
            try {
                address = Integer.parseUnsignedInt(tokens[1], 16);
            }
            catch(NumberFormatException e)
            {
            }
        }
        return address;
    }

    @Override
    public boolean isIMemoryValue(){
        return true;
    }

    public Integer intValue() {
        return value;
    }

    public Numeral getNumeral() {
        return new Numeral(value);
    }

    @Override
    public AbstractValue getReplacement(Numeral value) {
        if(value.isUnknown()) {
            return this;
        }
        return new LocalVariable(module, name, value.intValue());
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean isVariable() {
        return true;
    }

    @Override
    public String toString() {
        return "Var:" + name + ": #0x" + Integer.toHexString(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if(this==obj) {
            return true;
        }

        if (!getClass().equals(obj.getClass())) {
            return false;
        }

        LocalVariable other = (LocalVariable) obj;
        return (other.name == this.name && other.value==this.value);
    }


    @Override
    public void free() {

    }

    public int write(AbstractValue v, byte type) {
        return module.getInternalMemory().write(value, v, type);
    }

    @Override
    public AbstractValue read(byte type) throws IOException {
        return address.read(type);
    }

    @Override
    public void memset(AbstractValue v, AbstractValue len) {
        address.memset(v,len);
    }

    @Override
    public void memcpy(IMemoryValue v, AbstractValue len) {
        address.memcpy(v,len);
    }

    @Override
    public void memmove(IMemoryValue v, AbstractValue len) {
        address.memmove(v,len);
    }

    @Override
    public AbstractMemory.MemoryCell getMemoryCell(int address) {
        return this.address.getMemoryCell(address);
    }

    @Override
    public AbstractMemory.MemoryCell putMemoryCell(int address, AbstractMemory.MemoryCell cell) {
        return this.address.putMemoryCell(address, cell);
    }

    @Override
    public AbstractMemory.MemoryCell removeMemoryCell(int address) {
        return this.address.removeMemoryCell(address);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public void setValue(int value) {
        this.value = value;
    }

    public LibraryModule getModule() {
        return module;
    }
}
