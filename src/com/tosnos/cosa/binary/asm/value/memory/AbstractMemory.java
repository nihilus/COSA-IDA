package com.tosnos.cosa.binary.asm.value.memory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Z3Exception;
import com.tosnos.cosa.binary.NativeLibraryHandler;
import com.tosnos.cosa.binary.asm.Instruction;
import com.tosnos.cosa.binary.asm.Modifier;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.ExprValue;
import com.tosnos.cosa.binary.asm.value.Immediate;
import com.tosnos.cosa.binary.asm.value.Numeral;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kevin on 7/22/14.
 */
public class AbstractMemory implements Modifier {
    private Multimap<Integer, MemoryCell> backendMemoryCellMap; // store data of history call data // it is the last value
    protected Multimap<Integer, MemoryCell> temporaryMemoryCellMap;

    protected Map<Expr, AbstractValue> virtualDataMap = new HashMap<Expr, AbstractValue>();

    protected Map<Integer, MemoryCell> memoryCellMap = new HashMap<Integer, MemoryCell>();
    protected final String name;

    public AbstractMemory(String name) {
        this(name, null);
    }

    public AbstractMemory(String name, AbstractMemory memory) {
        if (memory != null) {
            this.name = memory.name;
            this.memoryCellMap.putAll(memory.memoryCellMap);
            this.backendMemoryCellMap = memory.backendMemoryCellMap;
        } else {
            this.backendMemoryCellMap = HashMultimap.create();
            this.name = name;
        }
    }

    public String getName() {
        return name;
    }

    public Map<Integer, MemoryCell> getInternalMemory() {
        return memoryCellMap;
    }

    public void copyInternalMemory(Map<Integer, MemoryCell> memory, int size) {
        this.memoryCellMap.clear();
        for (int i = 0; i < size; i++) {
            MemoryCell cell = memory.get(i);
            if (cell != null) {
                this.memoryCellMap.put(i, cell);
            } else {
                memory.remove(i + i);
            }
        }
    }

    public Collection<MemoryCell> getValues() {
        return memoryCellMap.values();
    }

    public int write(int address, AbstractValue value) {
        return write(address, value, Modifier.TYPE_INT);
    }

    public int write(int address, AbstractValue value, byte type) { // TODO: memoryCellMap set
        int size;

        byte[] data = null;
        if(value==null) {
            value = Immediate.newValue(type);
        }

        switch (type) {
            case TYPE_BYTE:
                size = Instruction.BYTE;
                if (!value.isUnknown()) {
                    data = new byte[]{value.intValue().byteValue()};
                }
                break;
            case TYPE_HALF:  // upper && lower
                size = Instruction.HALFWORD;
                if (!value.isUnknown()) {
                    data = ByteBuffer.allocate(Instruction.HALFWORD).order(ByteOrder.LITTLE_ENDIAN).putShort(value.intValue().shortValue()).array();
                }
                break;
            case TYPE_DOUBLE:
                size = Instruction.DOUBLEWORD;
                if (!value.isUnknown()) {
                    if (value.isImmediate()) {
                        data = ByteBuffer.allocate(Instruction.DOUBLEWORD).order(ByteOrder.LITTLE_ENDIAN).putDouble(((Immediate) value).getNumeral().doubleValue()).array();
                    }
                }

                break;
            case TYPE_LONG:
                size = Instruction.DOUBLEWORD;
                if (!value.isUnknown()) {
                    if (value.isImmediate()) {
                        data = ByteBuffer.allocate(Instruction.DOUBLEWORD).order(ByteOrder.LITTLE_ENDIAN).putLong(((Immediate) value).getNumeral().longValue()).array();
                    }
                }
                break;
            case TYPE_INT:
                size = Instruction.WORD;
                if (!value.isUnknown()) {
                    if (value.isImmediate()) {
                        data = ByteBuffer.allocate(Instruction.WORD).order(ByteOrder.LITTLE_ENDIAN).putInt(value.intValue()).array();
                    }
                }
                break;
            default:
                throw new RuntimeException("error!!! " + value + " " + type);
        }


        if (data != null) {
            if(temporaryMemoryCellMap!=null) {
                for (int i = 0; i < size; i++) {
                    MemoryCell cell = new MemoryCell(address + i, new Immediate(data[i] & 0xFF));
                    memoryCellMap.put(address + i, cell);
                    temporaryMemoryCellMap.put(address, cell);
                }
            } else {
                for (int i = 0; i < size; i++) {
                    MemoryCell cell = new MemoryCell(address + i, new Immediate(data[i] & 0xFF));
                    memoryCellMap.put(address + i, cell);
                }
            }
        } else { // variable
            if(temporaryMemoryCellMap!=null && !value.isPseudoValue()) {
                for (int i = 0; i < size; i++) {
                    MemoryCell cell = new MemoryCell(address, value);
                    memoryCellMap.put(address + i, cell);
                    temporaryMemoryCellMap.put(address, cell);
                }
            } else {
                for (int i = 0; i < size; i++) {
                    MemoryCell cell = new MemoryCell(address, value);
                    memoryCellMap.put(address + i, cell);
                }
            }
        }
        return size;
    }

    public AbstractValue deepRead(int address, byte type) {
        Collection<MemoryCell> cells = backendMemoryCellMap.get(address); // one cell represents one byte
        if (cells==null || cells.isEmpty()) {
            return null;// AbstractValue.newValue(type);
        }
        MemoryCell cell = cells.iterator().next();


        AbstractValue value = cell.getValue();
        if(!value.isImmediate() || value.isUnknown()) {
            if(value.getType()!=type) {
                if (type == TYPE_BYTE) { // for what? String?
                    return cell.getValue().getReplacement(new Numeral(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(cell.getValue().intValue()).array()[address - cell.address] & 0xFF));
                } else if (type == TYPE_HALF) {
                    // return Immediate.newValue(type);
                    throw new RuntimeException("wrong format " + Integer.toHexString(address));
                } else if (type == TYPE_DOUBLE) { // should be bytes
                    if(value.getType()==TYPE_INT) {
                        cells = backendMemoryCellMap.get(address+BYTES[value.getType()]);
                        if (cells==null || cells.isEmpty()) {
                            return null;// AbstractValue.newValue(type);
                        }
                        MemoryCell cell1 = cells.iterator().next();
                        return new Immediate(value, cell1.getValue(), type);
                    }
                    throw new RuntimeException("wrong format " + Integer.toHexString(address));

                }
            }
            return cell.getValue();
        }

        try {
            switch (type) {
                case TYPE_BYTE:
                    return cell.getValue();
                case TYPE_HALF: { // upper && lower
                    cells = backendMemoryCellMap.get(address + 1);
                    if (cells==null || cells.isEmpty()) {
                        return null;// AbstractValue.newValue(type);
                    }
                    byte[] data = new byte[]{cells.iterator().next().getValue().intValue().byteValue(), cell.byteValue()};
                    return new Immediate(ByteBuffer.wrap(data).getShort() & 0xFFFF);
                }
                case TYPE_DOUBLE:
                case TYPE_LONG: {
                    byte[] data = new byte[8];
                    data[7] = cell.byteValue();
                    for (int i = 0; i < 7; i++) {
                        cells = backendMemoryCellMap.get(address + i + 1);
                        if (cells==null || cells.isEmpty()) {
                            return null;// AbstractValue.newValue(type);
                        }
                        data[6 - i] = cells.iterator().next().getValue().intValue().byteValue();
                    }
                    if (type == TYPE_DOUBLE) {
                        return new Immediate(ByteBuffer.wrap(data).getDouble());
                    } else {
                        return new Immediate(ByteBuffer.wrap(data).getLong());
                    }
                }
                default: {
                    byte[] data = new byte[4];
                    data[3] = cell.byteValue();
                    for (int i = 0; i < 3; i++) {
                        cells = backendMemoryCellMap.get(address + i + 1);
                        if (cells==null || cells.isEmpty()) {
                            return null;// AbstractValue.newValue(type);
                        }
                        data[2 - i] = cells.iterator().next().getValue().intValue().byteValue();
                    }
                    return new Immediate(ByteBuffer.wrap(data).getInt());
                }
            }
        } catch (NullPointerException e) {
            throw new RuntimeException("wrong format " + Integer.toHexString(address));
//            return AbstractValue.newValue(type);
        }
    }

    public int writeText(int address, String str) {
        int size = 0;
        if (str != null) {
            for (int i = 0; i < str.length(); i++) {
                size += write(address + i, new Immediate(str.charAt(i)), TYPE_BYTE);
            }
            size += write(address + str.length(), new Immediate((byte) 0), TYPE_BYTE);
        }
        return size;
    }

    public String readText(int address) throws IOException {
        AbstractValue v = memoryCellMap.get(address).getValue();
        if (v.isUnknown()) {
            return null;
        }

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append((char) v.intValue().byteValue());
        address++;
        while (true) {
            char ch = (char) memoryCellMap.get(address++).getValue().intValue().byteValue();
            if (ch == 0) {
                break;
            }
            stringBuffer.append(ch);
        }
        return stringBuffer.toString();
    }

    public MemoryCell getMemoryCell(int address) {
        return memoryCellMap.get(address);
    }

    public MemoryCell putMemoryCell(int address, MemoryCell cell) {
        if(temporaryMemoryCellMap!=null && !cell.isPseudoValue()) {
            temporaryMemoryCellMap.put(address, cell);
        }
        return memoryCellMap.put(address, cell);
    }

    public MemoryCell removeMemoryCell(int address) {
        return memoryCellMap.remove(address);
    }

    public int write(Numeral address, AbstractValue v, byte type) {
        if(!address.isUnknown()) {
            return write(address.intValue(), v, type);
        }
        Context ctx = NativeLibraryHandler.getInstance().getContext();
        try {
            BitVecExpr key = address.getBitVecExpr(ctx);
            virtualDataMap.put(key, v);
        } catch (Z3Exception e) {
            e.printStackTrace();
        }
        return BYTES[type];
    }

    public AbstractValue read(Numeral address, byte type) throws IOException {
        if(!address.isUnknown()) {
            return read(address.intValue(), type);
        }

        try {
            Context ctx = NativeLibraryHandler.getInstance().getContext();
            BitVecExpr key = address.getBitVecExpr(ctx);
            AbstractValue value = virtualDataMap.get(key);
            if(value==null) {
                value = Immediate.newValue(type);
                virtualDataMap.put(key, value);
            }
            return value;
        } catch (Z3Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public AbstractValue read(int address, byte type) throws IOException {
        MemoryCell cell = memoryCellMap.get(address); // one cell represents one byte
        if (cell == null) {
            return null;// AbstractValue.newValue(type);
        }

        AbstractValue value = cell.getValue();
        if(!value.isImmediate() || value.isUnknown()) {
            Context ctx = NativeLibraryHandler.getInstance().getContext();

            if(value.getType()!=type) {
                if (type == TYPE_BYTE) { // for what? String?
                    try {
                        BitVecExpr expr = value.getNumeral().getBitVecExpr(ctx);
                        return cell.getValue().getReplacement(new Numeral(ctx.mkBVAND(expr, ctx.mkBV(0xFF, expr.getSortSize())), type));
                    } catch (Z3Exception e) {
                        e.printStackTrace();
                    }

                } else if (type == TYPE_HALF) {
                    try {
                        BitVecExpr expr = value.getNumeral().getBitVecExpr(ctx);
                        return cell.getValue().getReplacement(new Numeral(ctx.mkBVAND(expr, ctx.mkBV(0xFFFF, expr.getSortSize())), type));
                    } catch (Z3Exception e) {
                        e.printStackTrace();
                    }
                } else if (type == TYPE_DOUBLE) { // should be bytes
                    if(value.getType()==TYPE_INT) {
                        MemoryCell cell1 = memoryCellMap.get(address+BYTES[value.getType()]);
                        return new Immediate(value, cell1.getValue(), type);
                    }
                    throw new RuntimeException("wrong format " + Integer.toHexString(address));

                }
            }
            return cell.getValue();
        }

        try {
            switch (type) {
                case TYPE_BYTE:
                    return cell.getValue();
                case TYPE_HALF: { // upper && lower
                    byte[] data = new byte[]{memoryCellMap.get(address + 1).getValue().intValue().byteValue(), cell.byteValue()};
                    return new Immediate(ByteBuffer.wrap(data).getShort() & 0xFFFF);
                }
                case TYPE_DOUBLE:
                case TYPE_LONG: {
                    byte[] data = new byte[8];
                    data[7] = cell.byteValue();
                    for (int i = 0; i < 7; i++) {
                        data[6 - i] = memoryCellMap.get(address + i + 1).getValue().intValue().byteValue();
                    }
                    if (type == TYPE_DOUBLE) {
                        return new Immediate(ByteBuffer.wrap(data).getDouble());
                    } else {
                        return new Immediate(ByteBuffer.wrap(data).getLong());
                    }
                }
                default: {
                    byte[] data = new byte[4];
                    data[3] = cell.byteValue();
                    for (int i = 0; i < 3; i++) {
                        data[2 - i] = memoryCellMap.get(address + i + 1).getValue().intValue().byteValue();
                    }
                    return new Immediate(ByteBuffer.wrap(data).getInt());
                }
            }
        } catch (NullPointerException e) {
//            throw new RuntimeException("wrong format " + Integer.toHexString(address));
            return AbstractValue.newValue(type);
        }
    }

    public void clear(int address, int size) {
        for (int i = 0; i < size; i++) {
            memoryCellMap.remove(address + i);
        }
    }

    public void clear() {
        memoryCellMap.clear();
    }

    public int size() {
        return memoryCellMap.size();
    }

    public class MemoryCell {
        private final Integer address;
        private final AbstractValue value;

        public MemoryCell(Integer address, AbstractValue value) {
            this.address = address;
            this.value = value;
        }

        public AbstractValue getValue() {
            return value;
        }

        public boolean isImmediate() {
            if (value == null) {
                return false;
            }
            return value.isImmediate();
        }

        public boolean isEmpty() {
            return value == null;
        }

        @Override
        public int hashCode() {
            return address.hashCode() + value.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if(this==o) {
                return true;
            }

            if(!(o instanceof MemoryCell)) {
                return false;
            }

            MemoryCell other = (MemoryCell)o;

            if(address != other.address || !value.equals(other.value)) {
                return false;
            }
            return true;
        }

        public boolean isUnknown() {
            if(value==null) {
                return true;
            }
            return value.isUnknown();
        }

        public boolean isPseudoValue() {
            return value.isPseudoValue();
        }

        public byte byteValue() {
            return value.intValue().byteValue();
        }
    }

    public String toString() {
        return name;
    }

    public void commit() {
        if(temporaryMemoryCellMap!=null) {
            backendMemoryCellMap.putAll(temporaryMemoryCellMap);
        }
    }

    public LocalSnapshot checkPoint() {
        return new LocalSnapshot(this);
    }


    public class LocalSnapshot {
        private final AbstractMemory memory;
        private final Multimap<Integer, MemoryCell> oldTemporaryMemoryCellMap;
        private final Map<Integer, MemoryCell> oldMemoryCellMap;

        public LocalSnapshot(AbstractMemory memory) {
            this.memory = memory;
            this.oldTemporaryMemoryCellMap = temporaryMemoryCellMap;
            this.oldMemoryCellMap = memoryCellMap;
            memoryCellMap = new HashMap<Integer,MemoryCell>(memoryCellMap);
            temporaryMemoryCellMap = HashMultimap.create();
        }

        public void commit() {
            memory.commit();
            rollback();
        }

        public void rollback() {
            memory.temporaryMemoryCellMap = oldTemporaryMemoryCellMap;
            memory.memoryCellMap = oldMemoryCellMap;
        }
    }
}