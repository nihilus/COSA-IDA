package com.tosnos.cosa.binary.asm.value;

import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.Modifier;
import com.tosnos.cosa.binary.asm.Register;
import com.tosnos.cosa.binary.asm.value.memory.*;

import java.io.IOException;

/**
 * Created by kevin on 8/27/15.
 */
public class AssociatedValue extends AbstractValue implements IMemoryValue { // memory allocated value
    private final Address address;
    private AbstractValue value;

    public AssociatedValue(Address address, AbstractValue value) {
        this.address = address;
        if(value.isAssociatedValue() && ((AssociatedValue)value).address == address) {
            value = ((AssociatedValue)value).value;
        }
        this.value = value;
    }

    @Override
    public LibraryModule getModule() {
        if(value.isIMemoryValue()) {
            return ((IMemoryValue)value).getModule();
        }
        return address.getModule();
    }

    public AbstractValue getReplacement(Numeral value) {
        return new AssociatedValue(address, this.value.getReplacement(value));
    }

    public void replaceValue(AbstractValue value) {
        this.value = value;
        address.write(value);
    }

    @Override
    public int write(AbstractValue v, byte type) {
        if(value.isIMemoryValue()) {
            return ((IMemoryValue)value).write(v, type);
        }

//        else if(value.isImmediate()) {
//            if(value.intValue()==0) {
//                value = new PseudoValue(address, Modifier.TYPE_INT); // it is address
//                return ((IMemoryValue)value).write(offset, v, type);
//            }
//            return address.getMemory().write(value.intValue()+offset,v, type);
//        }
        throw new RuntimeException("not ");
    }

    public int write(String str) {
        if(value.isAddress()) {
            return ((Address)value).writeText(str);
        }
//        else if(value.isImmediate()) {
//            if(value.intValue()==0) {
//                value = new PseudoValue(address, Modifier.TYPE_INT); // it is address
//                return ((IMemoryValue)value).write(str);
//            }
//            return address.getMemory().write(value.intValue(),str);
//        }
        throw new RuntimeException("not ");
    }

    public AbstractValue read(byte type) throws IOException {
        if(value.isImmediate() && !value.isUnknown()) {
            if (value.intValue() > 0) {
                value = new Address(address.getMemory(), value.intValue());
            }
        }

        if(value.isAddress()) {
            return ((Address)value).read(type);
        }

//        else if(value.isImmediate()) {
//            if(value.intValue()==0) {
//                value = new PseudoValue(address, Modifier.TYPE_INT); // it is address
//                return ((IMemoryValue)value).read(offset, type);
//            }
//            return address.getMemory().read(value.intValue() + offset, type);
//        }
        throw new RuntimeException("not umplement");
    }

    @Override
    public void memset(AbstractValue v, AbstractValue len) {
        if(value.isIMemoryValue()) {
            if(value.isPseudoValue()) {
                AllocatedMemory allocatedMemory = new AllocatedMemory(len);
                ((PseudoValue) value).setReplacedValue(allocatedMemory);
                value = allocatedMemory;
            }
            ((IMemoryValue) value).memset(v, len);
            return;
        } else if(value.isImmediate()) {
            if(value.intValue()==0) {
                AllocatedMemory allocatedMemory = new AllocatedMemory(len);
                address.write(allocatedMemory);
                value = allocatedMemory;
            } else {
                value = new Address(address.getMemory(), value.intValue());
            }
            ((IMemoryValue) value).memset(v, len);
            return;
        }
        throw new RuntimeException("not implemented");
    }


    @Override
    public void memcpy(IMemoryValue v, AbstractValue len) {
        // it may be for the stack
        if(value.isIMemoryValue()) {
            if(value.isPseudoValue()) {
                AllocatedMemory allocatedMemory = new AllocatedMemory(len);
                ((PseudoValue) value).setReplacedValue(allocatedMemory);
                value = allocatedMemory;
            }
            ((IMemoryValue) value).memcpy(v, len);
            return;
        } else if(value.isImmediate()) {
            if(value.intValue()==0) {
                AllocatedMemory allocatedMemory = new AllocatedMemory(len);
                address.write(allocatedMemory);
                value = allocatedMemory;
            } else {
                value = new Address(address.getMemory(), value.intValue()); // need to check boundary
            }
            ((IMemoryValue) value).memcpy(v, len);
            return;
        }
        throw new RuntimeException("not ");
    }

    @Override
    public void memmove(IMemoryValue v, AbstractValue len) {
        if (value.isIMemoryValue()) {
            if (value.isPseudoValue()) {
                AllocatedMemory allocatedMemory = new AllocatedMemory(len);
                ((PseudoValue) value).setReplacedValue(allocatedMemory);
                value = allocatedMemory;
            }
            ((IMemoryValue) value).memmove(v, len);
            return;
        } else if (value.isImmediate()) {
            if (value.intValue() == 0) {
                AllocatedMemory allocatedMemory = new AllocatedMemory(len);
                address.write(allocatedMemory);
                value = allocatedMemory;
            } else {
                value = new Address(address.getMemory(), value.intValue());
            }
            ((IMemoryValue) value).memmove(v, len);
            return;
        }

        throw new RuntimeException("not ");
    }

    @Override
    public AbstractMemory.MemoryCell getMemoryCell(int offset) {
         if (value.isIMemoryValue()) {
            if (value.isPseudoValue()) {
                AllocatedMemory allocatedMemory = new AllocatedMemory();
                ((PseudoValue) value).setReplacedValue(allocatedMemory);
                value = allocatedMemory;
            }
            return ((IMemoryValue) value).getMemoryCell(offset);
        } else if (value.isImmediate()) {
            if (value.intValue() == 0) {
                AllocatedMemory allocatedMemory = new AllocatedMemory();
                address.write(allocatedMemory);
                value = allocatedMemory;
            } else {
                value = new Address(address.getMemory(), value.intValue());
            }
            return ((IMemoryValue) value).getMemoryCell(offset);
        }
        throw new RuntimeException("not ");
    }

    @Override
    public AbstractMemory.MemoryCell putMemoryCell(int offset, AbstractMemory.MemoryCell cell) {
        if (value.isIMemoryValue()) {
            if (value.isPseudoValue()) {
                AllocatedMemory allocatedMemory = new AllocatedMemory();
                ((PseudoValue) value).setReplacedValue(allocatedMemory);
                value = allocatedMemory;
            }
            return ((IMemoryValue) value).putMemoryCell(offset, cell);
        } else if (value.isImmediate()) {
            if (value.intValue() == 0) {
                AllocatedMemory allocatedMemory = new AllocatedMemory();
                address.write(allocatedMemory);
                value = allocatedMemory;
            } else {
                value = new Address(address.getMemory(), value.intValue());
            }
            return ((IMemoryValue) value).putMemoryCell(offset, cell);
        }
        throw new RuntimeException("not ");
    }

    @Override
    public AbstractMemory.MemoryCell removeMemoryCell(int offset) {
        if (value.isIMemoryValue()) {
            if (value.isPseudoValue()) {
                AllocatedMemory allocatedMemory = new AllocatedMemory();
                ((PseudoValue) value).setReplacedValue(allocatedMemory);
                value = allocatedMemory;
            }
            return ((IMemoryValue) value).removeMemoryCell(offset);
        } else if (value.isImmediate()) {
            if (value.intValue() == 0) {
                AllocatedMemory allocatedMemory = new AllocatedMemory();
                address.write(allocatedMemory);
                value = allocatedMemory;
            } else {
                value = new Address(address.getMemory(), value.intValue());
            }
            return ((IMemoryValue) value).removeMemoryCell(offset);
        }
        throw new RuntimeException("not ");
    }

    public Address getAddress() {
        return address;
    }

    public boolean isAssociatedValue() {
        return true;
    }

    @Override
    public Numeral getNumeral() {
        return value.getNumeral();
    }

    @Override
    public AbstractValue getValue() {
        if(value.isAssociatedValue()) {
            return value.getValue();
        }
        return value;
    }

    @Override
    public Integer intValue() {
        return value.intValue();
    }

    public String toString() {
        return "A:("+value+")@"+address;
    }

    @Override
    public boolean isIMemoryValue() {
        return true;
    }

    @Override
    public void free() {
        if(value.isPseudoValue()) {
            throw new RuntimeException("not implemented");
        } else if(value.isAllocatedMemory()) {
            Register.setValue(Register.R0, Immediate.ZERO);
        } else if(value.isIMemoryValue()) {
            ((IMemoryValue)value).free();
            Register.setValue(Register.R0, Immediate.ZERO);
        }
    }

    public Address getValueAddress() {
        if(value.isAddress()) {
            return (Address)value;
        }

        if(value.isImmediate()) {
            return new Address(address.getMemory(), value.intValue());
        }
        return null;
    }

    @Override
    public boolean isUnknown() {
        return value.isUnknown();
    }
}
