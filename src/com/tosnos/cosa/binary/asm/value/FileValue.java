package com.tosnos.cosa.binary.asm.value;

import com.microsoft.z3.Context;
import com.microsoft.z3.Z3Exception;
import com.tosnos.cosa.binary.asm.ArithmeticInstruction;
import com.tosnos.cosa.binary.asm.value.memory.IMemoryValue;

import java.io.File;

/**
 * Created by kevin on 8/18/14.
 */
public class FileValue extends AbstractValue {
    private static final int SEEK_SET = 0;
    private static final int SEEK_CUR = 1;
    private static final int SEEK_END = 2;
    private static final int MAX_LENGTH = 0x16;
    private static int total_id = 1;
    private final int id;

    private int pos = 0;

    private final File file;

    public FileValue(File file) {
        this.file = file;
        id = total_id++;
    }

    public FileValue(File file, int id) {
        this.file = file;
        this.id = id;
    }

    public Immediate fseek(AbstractValue pos, AbstractValue whence) {
        if(file!=null) { // send dummy data
            throw new RuntimeException("not");
        }
        switch(whence.intValue()) {
            case SEEK_CUR:
                this.pos += pos.intValue();
                break;
            case SEEK_SET:
                this.pos = pos.intValue();
                break;
            case SEEK_END:
                this.pos = MAX_LENGTH - pos.intValue();
                break;
        }

        if(this.pos>MAX_LENGTH) {
            this.pos = MAX_LENGTH;
            return Immediate.MINUS_ONE;
        }

        return Immediate.ZERO;
    }

    public Immediate ftell() {
        if(file!=null) { // send dummy data
            throw new RuntimeException("not");
        }
        return new Immediate(pos);
    }

    public AbstractValue fread(Context ctx, AbstractValue ptr, AbstractValue size, AbstractValue nitems) {
        if(file!=null) { // send dummy data
            throw new RuntimeException("not");
        } else if(!size.isUnknown() && !nitems.isUnknown() && pos<MAX_LENGTH){
            int count = size.intValue()*nitems.intValue();
            pos+=count;
            if(pos>MAX_LENGTH) {
                count -= (pos-MAX_LENGTH);
                pos = MAX_LENGTH;
            }

            AbstractValue addr = ptr;
            for(int i=0;i<count;i++) {
                ((IMemoryValue) addr).write(new Immediate(0, TYPE_BYTE), TYPE_BYTE);
                try {
                    addr = ArithmeticInstruction.add(ctx, ptr, new Immediate(i+1), null, 0);
                } catch (Z3Exception e) {
                    e.printStackTrace();
                }
            }
            return new Immediate(count);
        }
        return Immediate.MINUS_ONE;
    }

    public AbstractValue getReplacement(Numeral numeral) {
        return this;
    }

    @Override
    public boolean isFileValue() {
        return true;
    }

    public File getFile() {
        return file;
    }


    public Integer intValue() {
        return id;
    }

    @Override
    public Numeral getNumeral() {
        return new Numeral(id);
    }
}