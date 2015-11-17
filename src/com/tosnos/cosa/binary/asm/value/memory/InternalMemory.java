package com.tosnos.cosa.binary.asm.value.memory;

import com.google.common.collect.HashMultimap;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.asm.value.*;
import com.tosnos.cosa.binary.elf.ElfHelper;
import one.elf.ElfSection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kevin on 8/23/15.
 */
public class InternalMemory extends AbstractMemory { // for BSS, DATA
    private Map<Integer, MemoryCell> initializedMemory = null; // store data for initialized memory by jniOnLoad() and static initializer
    private final LibraryModule module;

    public InternalMemory(LibraryModule module) {
        super("I");
        this.module = module;
        super.temporaryMemoryCellMap = HashMultimap.create();
    }

    public void setInit() {
        initializedMemory = new HashMap<Integer, MemoryCell>(memoryCellMap);
    }

    public void clear() { // restore to the initedMemory
        memoryCellMap.clear();
        if(initializedMemory!=null) {
            memoryCellMap.putAll(initializedMemory);
        }
    }

//    public InternalMemory dump() {
//        return new InternalMemory(this);
//    }

    public LibraryModule getModule() {
        return module;
    }

    public boolean isInternalMemory() {
        return true;
    }

    @Override
    public MemoryCell getMemoryCell(int address) {
        MemoryCell cell = memoryCellMap.get(address);
        if(cell==null) {
            try {
                this.read(address, TYPE_BYTE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return memoryCellMap.get(address);
    }

    @Override
    public AbstractValue read(int address, byte type) throws IOException {
        if(address==0) {
            throw new RuntimeException("zero address!");
        }
        ElfHelper elfHelper = module.getElfHelper();
        ElfSection section = elfHelper.getSectionByAddress(address);
        if(section==null) {
            throw new IOException("wrong data section ");
        }

        String sectionName = section.name();
        AbstractValue value = super.read(address, type);
//        AbstractValue value = dataMap.get(Integer.toUnsignedLong(address));
// problem......
        if(value==null) {
            if (sectionName.equals(ElfHelper.BSS)) {
                if (module.isInitializing() || module.isExternal()) { // for external... it should read from actual value
                    value = Immediate.ZERO;
                } else {
                    return new PseudoValue(new Address(this, address), type);
                }
            } else {
                switch(type) {
                    case TYPE_BYTE:
                        value = new Immediate(elfHelper.readByte(section.fileOffset(address)));
                        break;
                    case TYPE_HALF:
                        value = new Immediate(elfHelper.readShort(section.fileOffset(address)));
                        break;
                    case TYPE_FLOAT:
                        value = new Immediate(elfHelper.readFloat(section.fileOffset(address)));
                        break;
                    case TYPE_DOUBLE:
                        value = new Immediate(elfHelper.readDouble(section.fileOffset(address)));
                        break;
                    case TYPE_LONG:
                        value = new Immediate(elfHelper.readLong(section.fileOffset(address)));
                        break;
                    default:
                        if(sectionName.equals(ElfHelper.GOT)) {
                            value = new Address(this, elfHelper.readInt(section.fileOffset(address)));
                        } else {
                            value = new Immediate(elfHelper.readInt(section.fileOffset(address)));
                        }
                }
            }
            super.write(address, value, type);
        }
        if(value.isImmediate()) {
            return new AssociatedValue(new Address(this, address), value);
        }
        return value;
    }

    @Override
    public int write(int address, AbstractValue value, byte type)  {
        try {
            ElfHelper elfHelper = module.getElfHelper();
            ElfSection section = elfHelper.getSectionByAddress(address);
            String name = section.name();
            if(!ElfHelper.BSS.equals(name) && !ElfHelper.GOT.equals(name) && !ElfHelper.DATA.equals(name)) {
                throw new RuntimeException("wrong data section "+ name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.write(address, value, type);
    }

    @Override
    public int writeText(int address, String str)  {
        try {
            ElfHelper elfHelper = module.getElfHelper();

            ElfSection section = elfHelper.getSectionByAddress(address);
            String name = section.name();
            if(!ElfHelper.BSS.equals(name) && !ElfHelper.DATA.equals(name)) {
                throw new RuntimeException("wrong data section "+ name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.writeText(address, str);
    }

    @Override
    public String readText(int address) throws IOException {
        ElfHelper elfHelper = module.getElfHelper();
        ElfSection section =  elfHelper.getSectionByAddress(address);

        String name = section.name();

        if(!ElfHelper.DATA.equals(name) && !ElfHelper.BSS.equals(name) && !ElfHelper.RODATA.equals(name) && !ElfHelper.DATA_REL_RO.equals(name) && !ElfHelper.DATA_REL_RO_LOCAL.equals(name)) {
            throw new RuntimeException("wrong data section "+ name);
        }

        String str = super.readText(address);
        if(str==null) {
            str = elfHelper.readString(section.fileOffset(address));
            super.writeText(address, str);
        }
        return str;
    }
}
