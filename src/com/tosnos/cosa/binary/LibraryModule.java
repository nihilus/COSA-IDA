package com.tosnos.cosa.binary;

import com.tosnos.cosa.binary.asm.Instruction;
import com.tosnos.cosa.binary.asm.InstructionDecoder;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.asm.value.LocalVariable;
import com.tosnos.cosa.binary.asm.value.StringValue;
import com.tosnos.cosa.binary.asm.value.memory.Address;
import com.tosnos.cosa.binary.asm.value.memory.InternalMemory;
import com.tosnos.cosa.binary.callgraph.CallGraph;
import com.tosnos.cosa.binary.callgraph.Edge;
import com.tosnos.cosa.binary.cfg.CFG;
import com.tosnos.cosa.binary.elf.ElfHelper;
import com.tosnos.cosa.binary.function.*;
import com.tosnos.cosa.util.Misc;
import one.elf.*;
import org.slf4j.LoggerFactory;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * Created by kevin on 12/7/14.
 */
public class LibraryModule {
    public enum STATUS { READY, START, DONE }
    private final static int SKIP_LINES = 8;
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(LibraryModule.class);
    private final String packageName;
    private final String fileName;
    private boolean loaded = false;
    private boolean external = false;
    private Map<Integer, Instruction> instructionMap = new HashMap<Integer, Instruction>();

    private Map<String, Address> exportedObject = new HashMap<String, Address>();
    private Map<String, Address> importedObject = new HashMap<String, Address>();


    private Set<Subroutine> exportedSubroutines = new HashSet<Subroutine>();
    private Set<ImportedSubroutine> importedSubroutines = new HashSet<ImportedSubroutine>();
    private Set<NativeMethod> nativeMethods = new HashSet<NativeMethod>();

    private Map<String, LocalVariable> variableMap = new HashMap<String, LocalVariable>();
    private InternalMemory internalMemory = new InternalMemory(this);

    private Map<String, Subroutine> subroutineMap = new HashMap<String, Subroutine>();
    private Map<Integer, Subroutine> subroutineAddressMap = new HashMap<Integer, Subroutine>();
    private String moduleName;
    private Subroutine jniOnLoad;
    private List<String> requiredLibraries = new ArrayList<String>();
    private List<Subroutine> initArrays = new ArrayList<Subroutine>();
    private List<Subroutine> finiArrays = new ArrayList<Subroutine>();
    private CallGraph cg;

    private STATUS initialized = STATUS.READY; // 0 : not yet 1: initializing 2: initialized
    private ElfHelper elfHelper;
    private int cpuArch = -1;

    public LibraryModule(String pacakgeName, String fileName) throws IOException {
        this(pacakgeName, fileName, true);
    }

    public LibraryModule(String pacakgeName, String fileName, boolean isInit) throws IOException {
        this.elfHelper = new ElfHelper(fileName);
        this.packageName = pacakgeName;
        this.fileName = fileName;
        if (packageName == null) {
            external = true;
        }

        // extract module name
        ElfHelper.Dynamic[] dynamics = elfHelper.getDynamicSections();
        for (ElfHelper.Dynamic dyn : dynamics) {
            if (dyn.d_tag == ElfHelper.Dynamic.DT_NEEDED) {
                String moduleName = dyn.toString();
                requiredLibraries.add(moduleName.substring(3, moduleName.length() - 3));
            } else if (dyn.d_tag == ElfHelper.Dynamic.DT_SONAME) {
                String moduleName = dyn.toString();
                this.moduleName = moduleName.substring(3, moduleName.length() - 3);
                logger.info("{} is being preprocessed", moduleName);
            }
        }

        if(isInit) {
            init();
        }
    }

    public short getMachine() {
        return elfHelper.getMachineCode();
    }

    public int getARMCPUArch() {
        if(cpuArch<0) {
            ElfArmAttributesSection section = (ElfArmAttributesSection)elfHelper.getSection(ElfHelper.ARM_ATTRIBUTES);
            List<ElfArmAttribute> attributes = section.attributes();
            for(ElfArmAttribute attribute:attributes) {
                for(ElfArmAttribute.Attribute subAttribute:attribute.getAttributes()) {
                    if(subAttribute.tag == ElfArmAttribute.Tag.CPU_arch) {
                        cpuArch = subAttribute.getULEB128();
                        return cpuArch;
                    }
                }
            }
        }
        return cpuArch;
    }

    public void clear() {
        internalMemory.clear();
    }

    public Map<String, Address> getExportedObject() {
        return exportedObject;
    }

    public Map<String, Address> getImportedObject() {
        return importedObject;
    }

    public boolean isInitializing() {
        return initialized == STATUS.START;
    }

    public boolean isInitialized() {
        return initialized != STATUS.READY;
    }

    public void setInitialized(STATUS initialized) {
        this.initialized = initialized;
        if (initialized == STATUS.DONE) {
            this.internalMemory.setInit();
        }
    }

    public Instruction getInstruction(int address) {
        return instructionMap.get(address);
    }

    public AbstractValue read(int address, byte type) throws IOException {
        return internalMemory.read(address, type);
    }

    public int write(int address, AbstractValue value, byte type) throws IOException {
        return internalMemory.write(address, value, type);
    }

    public LocalVariable findLocalVariable(String name) {
        return findLocalVariable(name, 0);
    }

    public LocalVariable findLocalVariable(String name, int address) {
        LocalVariable var = variableMap.get(name);
        if (var == null) {
            if (address == 0) {
                var = new LocalVariable(this, name);
            } else {
                var = new LocalVariable(this, name, address);
            }
            variableMap.put(name, var);
        } else {
            if (var.intValue() == 0) {
                var.setValue(address);
            }
        }
        return var;
    }

    public void init() throws IOException {
        ElfSymbol[] dynamicSymbols = elfHelper.getDynamicSymbols();
        for (ElfSymbol s : dynamicSymbols) {
            if (!s.name().isEmpty() && s.value() > 0) { // skip imported functions
                int address = (int) (s.value() & ~1);
                Subroutine procedure = null;
                String name = s.name();
                switch (s.type()) {
                    case ElfSymbol.STT_FUNC:
                        if (!external) { // except external, because function need to be simulated
                            if (name.startsWith("Java_")) {
                                SootMethod sootMethod = null;
                                String names[] = Misc.javaDemingling(name);
                                SootClass sootClass = Scene.v().loadClassAndSupport(Misc.adjustClassName(names[0]));
                                if (!sootClass.isPhantom()) {
                                    if (names.length == 3) {
                                        sootMethod = sootClass.getMethod(names[1], Misc.getTypeList(names[2]));
                                    } else {
                                        if (sootClass.declaresMethod(names[1], Arrays.asList(new Type[]{}))) {
                                            sootMethod = sootClass.getMethod(names[1], Arrays.asList(new Type[]{}));
                                        } else {
                                            for (SootMethod m : sootClass.getMethods()) {
                                                if (m.isNative() && m.getName().equals(names[1])) {
                                                    sootMethod = m;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (sootMethod != null) {
                                        procedure = new NativeMethod(this, name, address, sootMethod);
                                        nativeMethods.add((NativeMethod) procedure);
                                    }
                                }
                            } else {
                                procedure = new Subroutine(this, name, address);
                                if ("JNI_OnLoad".equals(name)) {
                                    jniOnLoad = procedure;
                                } else if (s.isSystemFunction()) {
                                    procedure.setSystemSubroutine();
                                } else {
                                    exportedSubroutines.add(procedure);
                                }
                            }
                            if (procedure != null) {
                                subroutineAddressMap.put(address, procedure);
                                subroutineMap.put(name, procedure);
                                variableMap.put(name, new LocalVariable(this, name, address));
                            }
                        }
                        break;
                    case ElfSymbol.STT_OBJECT: { // it is not a subroutine, but an exported variable.
                        exportedObject.put(name, new Address(internalMemory, address));
                        break;
                    }
                    case ElfSymbol.STT_NOTYPE:
                        break;
                    default:
                        throw new RuntimeException("Unknown symbol type " + s.name() + " " + s.type());
                }
            }
        }

        if (!external) {
            //////////////////////////////////
            // Parse the PLT and generate importSubroutines
            ElfRelocationTable relPlt = elfHelper.getRelocationTable(ElfHelper.REL_PLT);
            if (relPlt != null) {
                ElfSection pltSection = elfHelper.getSection(ElfHelper.PLT);
                long address = pltSection.address() + ElfHelper.ELF32_ADDR_SIZE * ElfHelper.PLT0_ENT_SIZE;
                for (ElfRelocation relocation : relPlt.relocations()) {
                    address &= ~1;
                    if(relocation.type() == ElfHelper.R_ARM_JUMP_SLOT) {
                        String functionName = relocation.symbol().name();
                        ImportedSubroutine isof = new ImportedSubroutine(this, functionName, (int) address);
                        subroutineMap.put(functionName, isof);
                        subroutineAddressMap.put((int) address, isof);
                        variableMap.put(functionName, new LocalVariable(this, functionName, (int) address)); // add plt name to variables
                        importedSubroutines.add(isof);
                    }
                    address += ElfHelper.ELF32_ADDR_SIZE * ElfHelper.PLT_ENT_SIZE;
                }
            }
//   /*     // Parse the Dynamic relocation

            ElfRelocationTable relDyn = elfHelper.getRelocationTable(ElfHelper.REL_DYN);
            if (relDyn != null) {
                for (ElfRelocation relocation : relDyn.relocations()) {
                    if(relocation.type() == ElfHelper.R_ARM_GLOB_DAT) { // need to get an address for data from other libraries
                        ElfSymbol symbol = relocation.symbol();
                        if (!symbol.isSystemFunction()) {
                            String name = symbol.name();
                            if (symbol.type() == ElfSymbol.STT_OBJECT) {
                                importedObject.put(name, new Address(internalMemory, (int) relocation.offset()));
                            } else if (symbol.type() == ElfSymbol.STT_FUNC) {
                                Address addr = InternalFunction.getFunctionAddress(name);
                                if (addr != null) {
                                    internalMemory.write((int) relocation.offset(), addr);
                                    if (!variableMap.containsKey(name)) {
                                        variableMap.put(name, new LocalVariable(this, name, (int) relocation.offset()));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ElfSection section = elfHelper.getSection(ElfHelper.INIT_ARRAY);
            if (section != null) {
                int off = 0;
                while (off < section.size()) {
                    int address = elfHelper.readInt(section.offset() + off);
                    if (address > 0) {
                        address &= ~1;
                        String functionName = String.format("loc_%X", address);
                        Subroutine subroutine = new Subroutine(this, functionName, address);
                        subroutineMap.put(functionName, subroutine);
                        subroutineAddressMap.put(address, subroutine);
                        initArrays.add(subroutine);
                    }
                    off += ElfHelper.ELF32_OFF_SIZE;
                }
            }


            section = elfHelper.getSection(ElfHelper.FINI_ARRAY);
            if (section != null) {
                int off = 0;
                while (off < section.size()) {
                    int address = elfHelper.readInt(section.offset() + off);
                    if (address > 0) {
                        address &= ~1;
                        String functionName = String.format("sub_%X", address);
                        Subroutine subroutine = new Subroutine(this, functionName, address);
                        subroutineMap.put(functionName, subroutine);
                        subroutineAddressMap.put(address, subroutine);
                        finiArrays.add(subroutine);
                    }
                    off += ElfHelper.ELF32_OFF_SIZE;
                }
            }
        }


//        // for .ARM.exidx - for unwind functions
//        section = sectionMap.get(SectionType.ARM_EXIDX);
//        if (section != null) {
//            int off = 0;
//            while (off < section.sh_size) {
//                efile.seek(section.sh_offset + off);
//                int offset = (int) efile.readIntE();
//                offset = (offset << 1) >> 1; // change sign bit, since it is a 31bits value.
//                long address = offset + off + section.sh_addr;
//                if (address > 0) {
//                    if ((address & 1) > 0) {
//                        address &= ~1;
//                    }
//                }
//                off += Elf.ELF32_OFF_SIZE * 2;
//            }
//        }

    }

    public boolean isExternal() {
        return external;
    }

    public void setExternal() {
        this.external = true;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public CallGraph getCG() {
        return cg;
    }

    public synchronized void load() throws IOException, ParseException {
        if (loaded) {
            return;
        }
        loaded = true;
        logger.warn("lib{}.so is loading....", moduleName);
        // disassemble file
        boolean doNotDelete = false;
        String filePath = fileName.substring(0, fileName.length() - 3);
        File file = new File(filePath + ".asm");

        if (!file.exists()) {
            file = new File(file.getParent() + "/lib" + moduleName + ".asm");
            doNotDelete = true;
        }

        if (!file.exists()) {
            Misc.diasm(fileName);
            file = new File(filePath + ".asm");
        }

        logger.warn("file size is " + file.length());

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        int address;
        boolean thumb = false;
        boolean startSub = false;
        Instruction prev = null;
        String sectionName = null;
        for (int i = 0; i < SKIP_LINES; br.readLine(), i++) ; // skip header

        while ((line = br.readLine()) != null) {
            String[] strs = line.split("\\s+", 2);
            line = strs[1];
            if (line.isEmpty()) {
                continue;
            }
            // set area
            if (line.startsWith("AREA")) {
                sectionName = line.substring(5, line.indexOf(",", 5));
                continue;
            } else if (line.endsWith("ends")) {
                sectionName = "";
                continue;
            }

            if (ElfHelper.TEXT.equals(sectionName)) {
                if ("; =============== S U B R O U T I N E =======================================".equals(line)) {
                    startSub = true;
                    prev = null;
                    continue;
                } else if (line.startsWith("EXPORT") || line.startsWith("WEAK")) { // TODO: what is WEAK?
                    startSub = true;
                    prev = null;
                    continue;
                } else if (line.startsWith("; End")) {
                    prev = null;
                    continue;
                } else if (line.startsWith("ALIGN")) {


                    continue;
                } else if (line.startsWith("CODE")) {
                    thumb = line.endsWith("16") ? true : false;
                    continue;
                } else if (line.startsWith("loc")) {
                    continue;
                } else if (line.startsWith(";")) {
                    continue;
                }

                if (line.startsWith("DC") || line.contains("\tDC")) { // Data doesn't need to be stored
                    prev = null;
                    continue;
                }

                line = line.split(";", 2)[0].trim();

                address = Integer.parseUnsignedInt(strs[0], 16);

                if (startSub) {
                    String[] tokens = line.split("\\s");
                    Subroutine subroutine = subroutineMap.get(tokens[0]);
                    if (subroutine == null) {
                        subroutine = new Subroutine(this, tokens[0], address);
                        subroutineMap.put(tokens[0], subroutine);
                        subroutineAddressMap.put(address, subroutine);
                    }
                    logger.debug("Subroutine \"{}\" has been created", tokens[0]);
                    startSub = false;
                    continue;
                }

                Instruction inst = InstructionDecoder.decode(this, address, thumb, line);
                if (inst != null) {
                    instructionMap.put(inst.getAddress(), inst);
                    if (prev != null) {
                        inst.setPrev(prev);
                    }
                    prev = inst;
                }
            } else {//if(ElfHelper.DATA.equals(sectionName) || ElfHelper.GOT.equals(sectionName) || ElfHelper.BSS.equals(sectionName)) {
                if (line.startsWith("EXPORT") || line.startsWith("WEAK")) {
                    continue;
                } else if (line.startsWith(";")) {
                    continue;
                }
                //          address = Integer.parseUnsignedInt(strs[0], 16);

                String tokens[] = line.split("\\s+", 2);

                if (!tokens[0].startsWith("DC")) {
                    address = Integer.parseUnsignedInt(strs[0], 16);
                    String name = tokens[0];
                    //           tokens = tokens[1].split("\\s+", 2); // TODO:delete comment
                    variableMap.put(name, new LocalVariable(this, name, address));
                }
            }
        }


        br.close();
        if (!external && !doNotDelete) {
            file.delete();
            new File(filePath + ".idb").delete();
        }

        // build call graph
        cg = new CallGraph();
        if (!doNotDelete) {
            file = new File(file.getParent() + "/lib" + moduleName + ".gdl");
        } else {
            file = new File(filePath + ".gdl");
        }

        if (file.exists()) {
            br = new BufferedReader(new FileReader(file));
            Map<String, Subroutine> nodeMap = new HashMap<String, Subroutine>();
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("node:")) {
                    String[] tokens = line.split("\"");
                    nodeMap.put(tokens[1], subroutineMap.get(tokens[3])); // node: title, function's name
                } else if (line.startsWith("edge:")) {
                    String[] tokens = line.split("\"");
                    cg.addEdge(new Edge(nodeMap.get(tokens[1]), nodeMap.get(tokens[3])));
                }

            }
            br.close();
            if (!external && !doNotDelete) {
                file.delete();
            }
        }
        logger.warn("lib{}.so is loaded....", moduleName);
    }

    public ElfHelper getElfHelper() {
        return elfHelper;
    }

    public InternalMemory getInternalMemory() {
        return internalMemory;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public Subroutine getSubroutine(String name) {
        return subroutineMap.get(name);
    }

    public Subroutine getSubroutine(Integer address) {
        return subroutineAddressMap.get(address);
    }

    public void addSubroutine(String name, Subroutine sub) {
        subroutineMap.put(name, sub);
    }

    public Subroutine getJniOnLoad() {
        return jniOnLoad;
    }

    public List<String> getRequiredLibraries() {
        return requiredLibraries;
    }

    public List<Subroutine> getInitArrays() {
        return initArrays;
    }

    public Map<String, Subroutine> getSubroutineMap() {
        return subroutineMap;
    }

    public Set<Subroutine> getExportedSubroutines() {
        return exportedSubroutines;
    }

    public Set<NativeMethod> getNativeMethods() {
        return nativeMethods;
    }

    public Set<ImportedSubroutine> getImportedSubroutines() {
        return importedSubroutines;
    }

}
