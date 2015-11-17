package com.tosnos.freeme;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;


public class FreemeDBClient {

    public static final byte OPCODE_END = 4;
    public static final byte OPCODE_INSERT_PACKAGE_TB = 6;
    public static final byte OPCODE_INSERT_METHOD_TB = 7;
    public static final byte OPCODE_INSERT_PARAMETER_TB = 8;
    public static final byte OPCODE_INSERT_FIELD_TB = 9;
    public static final byte OPCODE_INSERT_STATIC_TB = 10;
    public static final byte OPCODE_INSERT_MEMORY_TB = 11;
    public static final byte OPCODE_INSERT_OFFSET_TB = 12;
    private final byte PREAMBLE_RUNTIME = 0x5D;
    //	static private LRUCache<Long, MapMethod> methodCache = new LRUCache<Long, MapMethod>(1000);
    private Socket conn = null;
    private String localhost = "127.0.0.1";
    private int port = 1806;
    private DataInputStream dis = null;
    private DataOutputStream dos = null;

    //	private SQLiteDatabase db = null;
    public FreemeDBClient() {

    }

    public void disconnectDB() {
        if (conn != null) {
            try {
                dos.writeByte(OPCODE_END);
                dos.flush();
                conn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized boolean connectDB() throws SQLException {
        if (conn == null) {
            try {
                conn = new Socket(localhost, port);
                dis = new DataInputStream(conn.getInputStream());
                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeByte(PREAMBLE_RUNTIME);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public int insertPackageTb(String packageName, String apkFile, String mainActivity) throws SQLException {
        int packageId = -1;
        if (conn != null) {
            try {
                dos.writeByte(OPCODE_INSERT_PACKAGE_TB);
                dos.writeUTF(packageName);
                dos.writeUTF(apkFile);
                dos.writeUTF(mainActivity);
                dos.flush();
                packageId = dis.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return packageId;
    }
//
//    public void insertMethodTb(int packageId, Node node) throws SQLException {
//        SootMethod method = node.getMethod();
//        String className = method.getDeclaringClass().getName();
//        String methodName = method.getName();
//        List<Type> paramTypes = method.getParameterTypes();
//
//        StringBuffer param = new StringBuffer();
//        for (Iterator<Type> it = paramTypes.iterator(); it.hasNext(); ) {
//            param.append(it.next() + " ");
//        }
//
//        int methodId = Misc.getHashCode(method);
//        if (conn != null) {
//            try {
//                dos.writeByte(OPCODE_INSERT_METHOD_TB);
//                dos.writeInt(packageId);
//                dos.writeInt(methodId);
//                dos.writeUTF(className);
//                dos.writeUTF(methodName);
//                dos.writeUTF(param.toString().trim());
//                dos.flush();
//
//                int start = method.isStatic() ? 1 : 0;
//
//                for (int i = start; i < method.getParameterCount() + 1; i++) {
//                    if (i > 0 && (Analyzer.isPrimitive(method.getParameterType(i - 1)) || (method.getParameterType(i - 1) instanceof ArrayType))) {
//                        dos.writeByte(OPCODE_INSERT_PARAMETER_TB);
//                        dos.writeInt(packageId);
//                        dos.writeInt(methodId);
//                        dos.writeInt(i - start);
//                        dos.writeInt(-1);
//                        dos.flush();
//
//                    } else {
//                        Set<Node> visited = new HashSet<Node>();
//                        Set<SootField> fields = node.getAllRequiredField(i, visited);
//                        int fieldSize = fields.size();
//
//                        if (fields.contains(Node.EntirelyUsedField)) {
//                            dos.writeByte(OPCODE_INSERT_PARAMETER_TB);
//                            dos.writeInt(packageId);
//                            dos.writeInt(methodId);
//                            dos.writeInt(i - start);
//                            dos.writeInt(-1);
//                            dos.flush();
//                        } else {
//                            dos.writeByte(OPCODE_INSERT_PARAMETER_TB);
//                            dos.writeInt(packageId);
//                            dos.writeInt(methodId);
//                            dos.writeInt(i - start);
//                            dos.writeInt(fieldSize);
//                            dos.flush();
//
//                            if (fieldSize > 0) {
//
//                                for (Iterator<SootField> it = fields.iterator(); it.hasNext(); ) {
//                                    SootField field = it.next();
//                                    dos.writeByte(OPCODE_INSERT_FIELD_TB);
//                                    dos.writeInt(packageId);
//                                    dos.writeInt(methodId);
//                                    dos.writeInt(i - start);
//                                    dos.writeUTF(field.getName());
//                                    dos.writeUTF(field.getType().toString());
//                                    dos.flush();
//                                }
//                            }
//                        }
//                    }
//                }
//
//                Set<SootField> staticFields = node.getAllRequiredStaticField();
//
//                for (Iterator<SootField> it = staticFields.iterator(); it.hasNext(); ) {
//                    SootField field = it.next();
//                    dos.writeByte(OPCODE_INSERT_STATIC_TB);
//                    dos.writeInt(packageId);
//                    dos.writeInt(methodId);
//                    dos.writeUTF(field.getDeclaringClass().getName());
//                    dos.writeUTF(field.getName());
//                    dos.writeUTF(field.getType().toString());
//                    dos.flush();
//                }
//
//                Memory[] memory = MemoryInfo.getMemory(node.getAllSharedMemorySet());
//                for (Memory m : memory) {
//                    if (m.getBase() == null) {
//                        Memory[] offsets = m.getOffsets();
//                        dos.writeByte(OPCODE_INSERT_MEMORY_TB);
//                        dos.writeInt(packageId);
//                        dos.writeInt(methodId);
//                        dos.writeInt((int) m.getAddr());
//                        dos.writeInt(m.getSize());
//                        dos.writeInt(offsets.length);
//                        dos.writeUTF(m.getLibName());
//                        dos.flush();
//                        int memoryId = dis.readInt();
//                        for (Memory offset : offsets) {
//                            insertOffsetTb(packageId, memoryId, 0, offset);
//                        }
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public void insertOffsetTb(int packageId, int memoryId, int baseId, Memory m) throws IOException {
//        Memory[] offsets = m.getOffsets();
//        dos.writeByte(OPCODE_INSERT_OFFSET_TB);
//        dos.writeInt(packageId);
//        dos.writeInt(memoryId);
//        dos.writeInt(baseId);
//        dos.writeInt((int) m.getAddr());
//        dos.writeInt(m.getSize());
//        dos.writeInt(offsets.length);
//        dos.flush();
//        baseId = dis.readInt();
//        for (Memory offset : offsets) {
//            insertOffsetTb(packageId, memoryId, baseId, offset);
//        }
//    }
}

