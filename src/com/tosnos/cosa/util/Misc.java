package com.tosnos.cosa.util;

import com.tosnos.cosa.android.AndroidPackage;
import com.tosnos.cosa.binary.asm.value.StringValue;
import soot.*;
import soot.util.NumberedString;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class Misc {
    public static final int BLACK = 0;
    public static final int RED = 1;
    public static final int GREEN = 2;
    public static final int YELLOW = 3;
    public static final int BLUE = 4;
    public static final int PURPLE = 5;
    public static final int CYAN = 6;
    public static final int WHITE = 7;
    
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_BG_BLACK = "\u001B[40m";
    public static final String ANSI_BG_RED = "\u001B[41m";
    public static final String ANSI_BG_GREEN = "\u001B[42m";
    public static final String ANSI_BG_YELLOW = "\u001B[43m";
    public static final String ANSI_BG_BLUE = "\u001B[44m";
    public static final String ANSI_BG_PURPLE = "\u001B[45m";
    public static final String ANSI_BG_CYAN = "\u001B[46m";
    public static final String ANSI_BG_WHITE = "\u001B[47m";
    
    private static final int CONSTRUCT = 4096;
    private static final String CMD_IDA = "TVHEADLESS=1 tools/" + ((System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0) ? "darwin" : "linux") + "/idal -B -c ";
    private static String CLASSPATH = "core.odex:services.odex:framework.odex:ext.odex:android.policy.odex:android.test.runner.odex:bouncycastle.odex:twframework.odex:core-junit.odex";
    
    public static String javaMingling(String className, String methodName, String sig) {
        StringBuffer name = new StringBuffer("Java_");
        name.append(className.replace(".","_"));
        name.append("_");
        for(int i=0;i<methodName.length();i++) {
            char c = methodName.charAt(i);
            switch(c) {
                case '_':
                    name.append("_1");
                    break;
                case ';':
                    name.append("_2");
                    break;
                case '[':
                    name.append("_3");
                    break;
                case '.':
                    name.append("_");
                    break;
                default:
                    name.append(c);
            }
        }
        name.append("__");
        if(sig!=null && !sig.isEmpty()) {
            for(int i=0;i<sig.length();i++) {
                char c = sig.charAt(i);
                switch(c) {
                    case '_':
                        name.append("_1");
                        break;
                    case ';':
                        name.append("_2");
                        break;
                    case '[':
                        name.append("_3");
                        break;
                    case '/':
                        name.append("_");
                        break;
                    default:
                        name.append(c);
                }
            }
        }
        return name.toString();
    }
    
    public static String[] javaDemingling(String str) {
        String[] names;
        int pos = -1;
        
        Matcher m = Pattern.compile("([a-zA-Z0-9]__)").matcher(str);
        while (m.find()) { // find the last "__"
            pos = m.end();
        }
        
        if (pos > 0) {
            names = new String[3];
            names[2] = demingling(str.substring(pos));
            str = str.substring(0, pos - 2);
        } else {
            names = new String[2];
        }
        str = demingling(str);
        str = str.substring(str.indexOf(".") + 1);
        
        pos = str.lastIndexOf(".");
        names[0] = str.substring(0, pos);
        names[1] = str.substring(pos + 1);
        return names;
    }
    
    public static String demingling(String str) {
        StringBuffer strBuffer = new StringBuffer();
        boolean underscore = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                underscore = true;
            } else {
                if (underscore) {
                    underscore = false;
                    switch (c) {
                        case '1':
                            strBuffer.append('_');
                            continue;
                        case '2':
                            strBuffer.append(';');
                            continue;
                        case '3':
                            strBuffer.append('[');
                            continue;
                        default:
                            strBuffer.append('.');
                    }
                }
                strBuffer.append(c);
            }
            
        }
        return strBuffer.toString();
    }
    
    public static boolean moveFile(String src, String dest) {
        File srcFile = new File(src);
        File destFile = new File(dest);
        if (destFile.isDirectory()) {
            destFile = new File(destFile.getAbsolutePath() + File.separator + srcFile.getName());
        }
        return srcFile.renameTo(destFile);
    }
    
    public static void deleteFile(String fileName) {
        File f = new File(fileName);
        if (f.exists()) {
            f.delete();
        }
    }
    
    // remove a file extension from a file path
    public static String getPrefix(String fileName) {
        int pos = fileName.lastIndexOf(".");
        if (pos > 0 && pos < fileName.length()) {
            return fileName.substring(0, pos);
        }
        return null;
    }
    
    public static String getFileName(String fileName) {
        int pos = fileName.lastIndexOf(File.separator);
        if (pos > 0 && pos < fileName.length()) {
            return fileName.substring(pos + 1);
        }
        return null;
    }
    
    public static String getParentDir(String dirName) {
        int pos = dirName.lastIndexOf(File.separator);
        if (pos > 0 && pos < dirName.length()) {
            return dirName.substring(0, pos);
        }
        return null;
    }
    
    public static String getExtention(String fileName) {
        int pos = fileName.lastIndexOf(".");
        if (pos > 0 && pos < fileName.length()) {
            return fileName.substring(pos + 1);
        }
        return null;
    }
    
    public static int getHashCode(SootMethod method) {
        String[] methodName = method.getBytecodeSignature().split(": ");
        return getHashCode(classNameJ2A(methodName[0].substring(1)) + methodName[1].substring(0, methodName[1].length() - 1));
    }
    
    public static int getHashCode(String str) {
        int hash = 1;
        for (int i = 0; i < str.length(); i++) {
            hash = hash * 31 + str.charAt(i);
        }
        return hash;
    }
    
    public static boolean deleteDir(String dir) {
        return deleteDir(new File(dir));
    }
    
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
    
    public static String getSpaceString(int numOfSpace) {
        if (numOfSpace > 0) {
            return String.format("%" + Integer.toString(numOfSpace) + "s", " ");
        }
        return "";
    }
    
    @SuppressWarnings("deprecation")
    public static String getSpaceString(int numOfSpace, String c) {
        StringBuffer strBuf = new StringBuffer();
        for (int i = 0; i < numOfSpace; i++) {
            strBuf.append(c);
        }
        return strBuf.toString();
    }
    
    @SuppressWarnings("deprecation")
    public static String textProgBar(int x) {
        int lineLength = 50;
        String anim = "|/-\\";
        String[] color = new String[]{ANSI_BLUE, ANSI_YELLOW, ANSI_RED, ANSI_WHITE};
        StringBuffer str = new StringBuffer("\r");
        int start = x / (100 / lineLength);
        str.append(ANSI_GREEN);
        for (int i = 0; i < start; i++) {
            str.append("#");
        }
        str.append(ANSI_RESET);
        if (x < 100) {
            int index = x % anim.length();
            str.append(color[index] + anim.charAt(index) + ANSI_RESET);
        }
        lineLength--;
        for (int i = start; i < lineLength; i++) {
            str.append(" ");
        }
        str.append(String.format("| %s%3d%%%s", ANSI_CYAN, x, ANSI_RESET));
        return str.toString();
    }
    
    public static String classNameJ2A(String className) {
        if (className.charAt(0) == '[') {
            className = className.replace('.', '/');
        } else if ("boolean".equals(className)) {
            className = "Z";
        } else if ("byte".equals(className)) {
            className = "B";
        } else if ("char".equals(className)) {
            className = "C";
        } else if ("double".equals(className)) {
            className = "D";
        } else if ("float".equals(className)) {
            className = "F";
        } else if ("int".equals(className)) {
            className = "I";
        } else if ("long".equals(className)) {
            className = "J";
        } else if ("short".equals(className)) {
            className = "S";
        } else if ("void".equals(className)) {
            className = "V";
        } else {
            className = "L" + className.replace('.', '/') + ";";
        }
        return className;
    }
    
    public static String classNameA2J(String className) {
        int len = className.length();
        if (className.charAt(len - 1) == ';') {
            className = className.replace('/', '.');
            if (className.charAt(0) == 'L') {
                className = className.substring(1, len - 1);
            }
        }
        return className;
    }
    
    public static List<Type> getTypeList(String parameters) {
        List<Type> pList = new ArrayList<Type>();
        int idx = 0;
        StringBuffer curr = new StringBuffer();
        while (idx < parameters.length()) {
            char ch = parameters.charAt(idx++);
            switch (ch) {
                case '[':
                    int arraySize = 1;
                    while ((ch = parameters.charAt(idx++)) == '[') {
                        arraySize++;
                    }
                    
                    if (ch == 'L') {
                        ch = parameters.charAt(idx++);
                        while (ch != ';') {
                            if(ch=='/') {
                                ch = '.';
                            }
                            curr.append(ch);
                            ch = parameters.charAt(idx++);
                        }
                        pList.add(ArrayType.v(RefType.v(adjustClassName(curr.toString())), arraySize));
                        curr.setLength(0);
                    } else {
                        pList.add(ArrayType.v(getType(ch), arraySize));
                    }
                    break;
                case 'L':
                    ch = parameters.charAt(idx++);
                    while (ch != ';') {
                        if(ch=='/') {
                            ch = '.';
                        }
                        curr.append(ch);
                        ch = parameters.charAt(idx++);
                    }
                    pList.add(RefType.v(adjustClassName(curr.toString())));
                    curr.setLength(0);
                    break;
                default:
                    pList.add(getType(ch));
                    break;
            }
        }
        return pList;
    }
    
    public static Type getType(char c) {
        Type type = null;
        switch (c) {
            case 'J':
                type = LongType.v();
                break;
                
            case 'S':
                type = ShortType.v();
                break;
                
            case 'D':
                type = DoubleType.v();
                break;
                
            case 'I':
                type = IntType.v();
                break;
                
            case 'F':
                type = FloatType.v();
                break;
                
            case 'B':
                type = ByteType.v();
                break;
                
            case 'C':
                type = CharType.v();
                break;
                
            case 'V':
                type = VoidType.v();
                break;
                
            case 'Z':
                type = BooleanType.v();
                break;
        }
        return type;
    }
    
    public static Type getType(String str) {
        Type type;
        int length = str.length();
        str = str.replace("[", "");
        int arraySize = length - str.length();
        
        if (str.contains("/")) { // class
            length = str.length();
            if (str.charAt(0) == 'L' && str.charAt(length - 1) == ';') {
                str = str.substring(1, length - 1);
            }
            type = RefType.v(str.replace("/", "."));
        } else {
            type = getType(str.charAt(0));
        }
        
        if (arraySize > 0) {
            type = ArrayType.v(type, arraySize);
        }
        return type;
    }
    
    public static String adjustClassName(String className) {
        String[] names = className.split("\\.");
        StringBuffer name = new StringBuffer();
        for (int i = 0; i < names.length; i++) {
            name.append(names[i]);
            if (i < (names.length - 1)) {
                if (Scene.v().containsClass(name.toString())) {
                    name.append("$");
                } else {
                    name.append(".");
                }
            }
        }
        return name.toString();
    }
    
    public static Ansi ansi() {
        return new Ansi();
    }
    
    public static SootClass getSootClass(Type type) {
        SootClass sc = null;
        if (type instanceof RefType) {
            return ((RefType) type).getSootClass();
        }
        
        if (type instanceof ArrayType) {
            
        }
        
        return sc;
    }

    public static SootClass getSootClass(StringValue typeName) {
        return getSootClass(getType(typeName.toString()));
    }

    public static SootClass getSootClass(String typeName) {
        return getSootClass(getType(typeName));
    }

    
    public static void diasm(String fileName) {
        try {
            String[] cmd = {
                "/bin/sh",
                "-c",
                CMD_IDA + fileName
            };
            Runtime.getRuntime().exec(cmd).waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static long parseLong(String str) {
        int radix = 10;
        boolean negative = false;
        str = str.trim();
        if (str.charAt(0) == '#') {
            str = str.substring(1);
        }
        
        if (str.charAt(0) == '-') {
            negative = true;
            str = str.substring(1);
        }
        
        if (str.startsWith("0x")) {
            radix = 16;
            str = str.substring(2);
        }
        
        return negative ? Long.parseLong(str, radix) * -1 : Long.parseLong(str, radix);
    }
    
    public static int parseInt(String str) {
        return (int) parseLong(str);
    }
    
    public static String[] parseMethodInfo(String methodSig) {
        String[] methodInfo = new String[3];
        int start = 0;
        int end = methodSig.indexOf(' ');
        methodInfo[0] = methodSig.substring(0, end);
        start = end + 2;
        end = methodSig.indexOf(')', start);
        methodInfo[1] = methodSig.substring(start, end);
        methodInfo[2] = methodSig.substring(end+1);
        return methodInfo;
    }
    
    public static class Ansi {
        private static final String ANSI = "\u001B[";
        private static final String ANSI_RESET = "\u001B[0m";
        private static boolean enabled = true;
        private StringBuilder sb;
        
        public Ansi() {
            sb = new StringBuilder();
        }
        
        public Ansi(StringBuilder sb) {
            this.sb = sb;
        }
        
        public Ansi fg(int COLOR) {
            return new Ansi(enabled ? sb.append(ANSI).append(30 + COLOR).append("m") : sb);
        }
        
        public Ansi bg(int COLOR) {
            return new Ansi(enabled ? sb.append(ANSI).append(40 + COLOR).append("m") : sb);
        }
        
        public Ansi a(String text) {
            return new Ansi(sb.append(text));
        }
        
        public Ansi reset() {
            return new Ansi(enabled ? sb.append(ANSI_RESET) : sb);
        }
        
        public void disable() {
            enabled = false;
        }
        
        public void enable() {
            enabled = true;
        }
        
        public String toString() {
            return sb.toString();
        }
    }
    

    public static Long combineToLong(Integer l, Integer u) { // done!
        if(l==null && u == null) {
            return null;
        }
        byte[] upper = new byte[4];
        byte[] lower = new byte[4];
        byte[] doubleValue = new byte[8];
        ByteBuffer.wrap(upper).putInt(u);
        ByteBuffer.wrap(lower).putInt(l);
        System.arraycopy(upper, 0, doubleValue, 0, 4);
        System.arraycopy(lower, 0, doubleValue, 4, 4);
        return ByteBuffer.wrap(doubleValue).getLong();
    }

    public static Double combineToDouble(Integer l, Integer u) { // done!
        if(l==null && u == null) {
            return null;
        }
        byte[] upper = new byte[4];
        byte[] lower = new byte[4];
        byte[] doubleValue = new byte[8];
        ByteBuffer.wrap(upper).putInt(u);
        ByteBuffer.wrap(lower).putInt(l);
        System.arraycopy(upper, 0, doubleValue, 0, 4);
        System.arraycopy(lower, 0, doubleValue, 4, 4);
        return ByteBuffer.wrap(doubleValue).getDouble();
    }

    public static int[] separateToInt(Number value) {
        if(value instanceof Double) {
            return separateToInt(value.doubleValue());
        } else {
            return separateToInt(value.longValue());
        }
    }

    public static int[] separateToInt(Double value) { // done!
        if(value==null) {
            return null;
        }
        byte[] bytes = new byte[8];
        byte[] upper = new byte[4];
        byte[] lower = new byte[4];
        ByteBuffer.wrap(bytes).putDouble(value);
        System.arraycopy(bytes, 0, upper, 0, 4);
        System.arraycopy(bytes, 4, lower, 0, 4);
        return new int[] {ByteBuffer.wrap(lower).getInt(), ByteBuffer.wrap(upper).getInt()};
    }

    public static int[] separateToInt(Long value) {
        if(value==null) {
            return null;
        }
        byte[] bytes = new byte[8];
        byte[] upper = new byte[4];
        byte[] lower = new byte[4];
        ByteBuffer.wrap(bytes).putLong(value);
        System.arraycopy(bytes, 0, upper, 0, 4);
        System.arraycopy(bytes, 4, lower, 0, 4);
        return new int[] {ByteBuffer.wrap(lower).getInt(), ByteBuffer.wrap(upper).getInt()};
    }

    public static SootMethod getMethod(SootClass sc, String methodName, List<Type> paramTypes) {
        SootMethod method = null;
        if (sc.declaresMethod(methodName, paramTypes)) {
            method = sc.getMethod(methodName, paramTypes);
            if(method.isAbstract()) {
                return null;
            }
        } else {
            if (sc.hasSuperclass()) {
                return getMethod(sc.getSuperclass(), methodName, paramTypes);
            }
        }
        return method;
    }

    public static SootField getField(SootClass sc, String fieldName, Type fieldType) {
        SootField field = null;
        if (sc.declaresField(fieldName, fieldType)) {
            field  = sc.getField(fieldName, fieldType);
        } else {
            if (sc.hasSuperclass()) {
                return getField(sc.getSuperclass(), fieldName, fieldType);
            }
        }
        return field;
    }


}
