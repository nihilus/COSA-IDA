package com.tosnos.cosa.binary.elf;

import one.elf.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Created by kevin on 9/8/15.
 */
public class ElfHelper {
    public static final int ELF32_OFF_SIZE = 4;
    public static final int PLT0_ENT_SIZE = 5;
    public static final int PLT_ENT_SIZE = 3;
    public static final int ELF32_ADDR_SIZE = 4;
    public static final int R_ARM_GLOB_DAT = 21;
    public static final int R_ARM_JUMP_SLOT = 22;
    public static final int R_ARM_RELATIVE = 23;
    public static final int R_ARM_ABS32 = 2;
    public static final int R_ARM_REL32 = 3;

    public static final String PLT = ".plt";
    public static final String BSS = ".bss";
    public static final String GOT = ".got";
    public static final String DATA = ".data";
    public static final String TEXT = ".text";
    public static final String RODATA = ".rodata";
    public static final String REL_PLT = ".rel.plt";
    private static final String REL_PLT_64 = ".rela.plt";
    public static final String REL_DYN = ".rel.dyn";
    private static final String REL_DYN_64 = ".rela.dyn";
    public static final String INIT_ARRAY = ".init_array";
    public static final String FINI_ARRAY = ".fini_array";
    public static final String DYNSYM= ".dynsym";
    public static final String DYNAMIC= ".dynamic";
    public static final String DATA_REL = ".data.rel";
    public static final String DATA_REL_RO= ".data.rel.ro";
    public static final String DATA_REL_RO_LOCAL=".data.rel.ro.local";
    public static final String JCR=".jcr";
    public static final String EH_FRAME = ".eh_frame";
    public static final String ARM_ATTRIBUTES = ".ARM.attributes";

    private final ElfReader elfReader;
    private final RandomAccessFile raf;
    private final ByteOrder order;
    private final Map<String, ElfSection> sectionMap = new HashMap<String, ElfSection>();
    private NavigableMap<Long, ElfSection> sectionAddressMap =  new TreeMap<Long, ElfSection>();

    public ElfHelper(String fileName) throws IOException {
        elfReader = new ElfReader(fileName);
        order = elfReader.endian();
        for(ElfSection section:elfReader.sections()) {
            if(section!=null) {
                sectionMap.put(section.name(), section);
                sectionAddressMap.put(section.address(), section);
            }
        }
        raf = new RandomAccessFile(fileName, "r");
    }

    public short getMachineCode() {
        return elfReader.machine();
    }

    public ElfRelocationTable getRelocationTable(String name) {
        if(elfReader.elf64()) {
            if(REL_PLT.equals(name)) {
                name = REL_PLT_64;
            } else if(REL_DYN.equals(name)) {
                name = REL_DYN_64;
            }
        }

        ElfSection section = sectionMap.get(name);
        if(section!=null && section instanceof ElfRelocationTable) {
            return (ElfRelocationTable)section;
        }
        return null;
    }

    public Dynamic[] getDynamicSections() throws IOException {
        ArrayList<Dynamic> dynList = new ArrayList<Dynamic>();
        ElfSection section = sectionMap.get(DYNAMIC);
        raf.seek(section.offset());
        int off = 0;
        // We must assume the section is a table ignoring the sh_entsize as it
        // is not
        // set for MIPS.
        while (off < section.size()) {
            Dynamic dynEnt = new Dynamic(section);
            if(elfReader.elf64()) {
                dynEnt.d_tag = readLong();
                dynEnt.d_val = readLong();
                off += Dynamic.DYN_ENT_SIZE_64;
            } else {
                dynEnt.d_tag = readInt();
                dynEnt.d_val = readInt();
                off += Dynamic.DYN_ENT_SIZE_32;
            }
            if (dynEnt.d_tag != Dynamic.DT_NULL)
                dynList.add(dynEnt);
        }
        return dynList.toArray(new Dynamic[0]);
    }

    public ElfSection getSectionByAddress(int address) throws IOException {
        return getSectionByAddress(Integer.toUnsignedLong(address));

    }

    public ElfSection getSection(String name) {
        return sectionMap.get(name);
    }

    public ElfSection getSectionByAddress(long address) {
        Map.Entry<Long, ElfSection> entry = sectionAddressMap.floorEntry(address);
        if(entry!=null) {
            ElfSection section = entry.getValue();
            if((address == section.address()) || (address > section.address() && address < (section.address()+section.size()))) {
                return section;
            }
        }
        return null;
    }

    public ElfSymbol[] getDynamicSymbols() {
        ElfSymbolTable symbolTable = (ElfSymbolTable)elfReader.section(DYNSYM);
        return symbolTable.symbols();
    }

    public final byte[] readBytes(long offset, int length) throws IOException {
        raf.seek(offset);
        byte buffer[] = new byte[length];
        raf.readFully(buffer);
        return buffer;
    }

    public final byte readByte(long offset) throws IOException {
        raf.seek(offset);
        return raf.readByte();
    }

    public final short readShort(long offset) throws IOException {
        raf.seek(offset);
        byte buffer[] = new byte[2];
        raf.readFully(buffer);
        return ByteBuffer.wrap(buffer).order(order).getShort();
    }

    public final int readInt(long offset) throws IOException {
        raf.seek(offset);
        return readInt();
    }

    public final int readInt() throws IOException {
        byte buffer[] = new byte[4];
        raf.readFully(buffer);
        return ByteBuffer.wrap(buffer).order(order).getInt();
    }

    public final float readFloat(long offset) throws IOException {
        raf.seek(offset);
        byte buffer[] = new byte[4];
        raf.readFully(buffer);
        return ByteBuffer.wrap(buffer).order(order).getFloat();
    }

    public final long readLong(long offset) throws IOException {
        raf.seek(offset);
        return readLong();
    }

    public final long readLong() throws IOException {
        byte buffer[] = new byte[8];
        raf.readFully(buffer);
        return ByteBuffer.wrap(buffer).order(order).getLong();
    }

    public final double readDouble(long offset) throws IOException {
        raf.seek(offset);
        byte buffer[] = new byte[8];
        raf.readFully(buffer);
        return ByteBuffer.wrap(buffer).order(order).getDouble();
    }

    public final String readString(long offset) throws IOException {
        StringBuffer stringBuffer = new StringBuffer();
        raf.seek(offset);
        char ch;
        while((ch=(char)raf.readByte())!=0) {
            stringBuffer.append(ch);
        }
        return stringBuffer.toString();
    }

    public class Dynamic {

        public static final int DYN_ENT_SIZE_32 = 8;
        public static final int DYN_ENT_SIZE_64 = 16;

        public static final int DT_NULL = 0;
        public static final int DT_NEEDED = 1;
        public static final int DT_PLTRELSZ = 2;
        public static final int DT_PLTGOT = 3;
        public static final int DT_HASH = 4;
        public static final int DT_STRTAB = 5;
        public static final int DT_SYMTAB = 6;
        public static final int DT_RELA = 7;
        public static final int DT_RELASZ = 8;
        public static final int DT_RELAENT = 9;
        public static final int DT_STRSZ = 10;
        public static final int DT_SYMENT = 11;
        public static final int DT_INIT = 12;
        public static final int DT_FINI = 13;
        public static final int DT_SONAME = 14;
        public static final int DT_RPATH = 15;
        private final ElfSection section;
        public long d_tag;
        public long d_val;
        private String name;

        protected Dynamic(ElfSection section) {
            this.section = section;
        }

        @Override
        public String toString() {
            if (name == null) {
                switch ((int) d_tag) {
                    case DT_NEEDED:
                    case DT_SONAME:
                    case DT_RPATH:
                        try {
                            name = readString(section.link().offset() + d_val);
                        } catch (IOException e) {
                            name = "";
                        }
                        break;
                    default:
                        name = "";
                }
            }
            return name;
        }
    }
}
