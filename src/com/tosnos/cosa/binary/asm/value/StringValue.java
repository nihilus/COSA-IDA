package com.tosnos.cosa.binary.asm.value;

import com.tosnos.cosa.binary.asm.Modifier;
import com.tosnos.cosa.binary.asm.value.memory.AbstractMemory;
import com.tosnos.cosa.binary.asm.value.memory.Address;

import java.io.IOException;

/**
 * Created by kevin on 2/7/15.
 */
public class StringValue extends Address {
    public StringValue(AbstractMemory memory, Numeral address) {
        super(memory, address);
    }

    public StringValue(Address address) {
        super(address.getMemory(), address.getNumeral());
    }

    public StringValue(String str) {
        this(new AllocatedMemory(str));
    }

    @Override
    public AbstractValue getReplacement(Numeral address) {
        return new StringValue(memory, address);
    }

    public AbstractValue get(int index) throws IOException {
        return memory.read(address.intValue()+index, Modifier.TYPE_BYTE);
    }

    public boolean isStringValue() {
        return true;
    }

    public int length() {
        String str = toString();
        if(str==null) {
            return -1;
        }
        return str.length();
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        try {
            int i = 0;
            while (true) {
                AbstractValue value = super.read(i++, Modifier.TYPE_BYTE);
                if (value == null) {
                    return null;
                } else if (value.getNumeral().isUnknown()) {
                    return null;
                } else if(value.getNumeral().intValue().byteValue() == 0) {
                    return stringBuffer.toString();
                }
                stringBuffer.append((char)value.getNumeral().intValue().byteValue());
            }
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean isUnknown() {
        return memory==null;
    }


    public byte getType() {
        return TYPE_STRING;
    }

//    public void write(AbstractMemory memory, int address) {
//        for(int i=0;i<length;i++) {
//            memory.write(i + address, new Immediate(str.charAt(i)), Modifier.TYPE_BYTE);
//        }
//        memory.write(address+length, new Immediate((byte)0), Modifier.TYPE_BYTE);
//    }
}
