package com.tosnos.freeme;

import com.tosnos.cosa.DB;
import org.jf.dexlib2.AccessFlags;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by kevin on 8/13/15.
 */
public class DynamicAnalyzer {
    private File file;
    private String pkgName;
    private int count;
    private Map<String, DynamicClass> classMap = new HashMap<String, DynamicClass>();

    private class DynamicClass {
        String name;
        Set<String> methods = new HashSet<String>();

        public DynamicClass(String name) {
            this.name = name;
        }

        public void addMethod(String method) {
            methods.add(method);
        }
    }

    public DynamicAnalyzer(File file) {
        this.file = file;
        String fileName = file.getName();
        int pos = fileName.lastIndexOf("_");
        pkgName = fileName.substring(0,pos);
        count = Integer.parseInt(fileName.substring(pos+1, fileName.length()-5));
    }

    public void updateCallgraph() throws SQLException {
        parse();
    }

    private boolean parse() throws SQLException {
        try {
            int pkgNo = 0;
            ResultSet rs = DB.getStmt().executeQuery("select no from app where package='"+pkgName+"'");
            if(rs.next()) {
                pkgNo = rs.getInt(1);
            }
            if(pkgNo<=0) {
                return false;
            }

            int numOfTotalClass = 0;
            int numOfClass = 0;
            int numOfTotalMethod = 0;
            int numOfMethod = 0;
            int numOfTotalNativeMethod = 0;
            int numOfNativeMethod = 0;

            if(file.length()>0) {
              //  Process p = Runtime.getRuntime().exec("dmtracedump -o " + file.getAbsolutePath());
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                int pos;
                // get thread ids
                br.readLine();
                line = br.readLine();
                int threadCount = Integer.parseInt(line.substring(line.lastIndexOf("(") + 1, line.length() - 2));
                String mainThreadId = null;

                for (int i = 0; i < threadCount; i++) {
                    line = br.readLine().trim();
                    pos = line.indexOf(" ");
                    if ("main".equals(line.substring(pos + 1))) {
                        mainThreadId = line.substring(0, pos);
                    }
                }

                br.readLine();
                StringBuffer digitBuffer = new StringBuffer();
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    pos = line.indexOf(" ");
                    String threadId = line.substring(0, pos);
                    if (!threadId.equals(mainThreadId)) {
                        continue;
                    }

                    boolean enter = line.charAt(pos + 1) == 'e';

                    if (!enter) {
                        continue;
                    }

                    long timestamp = 0;
                    int depth = 0;
                    // delete debug and profiling methods

                    for (int i = pos + 4; i < line.length(); i++) {
                        char c = line.charAt(i);
                        if (Character.isDigit(c)) {
                            digitBuffer.append(c);
                            depth = i;
                        } else if (Character.isLetter(line.charAt(i))) {
                            pos = i;
                            timestamp = Long.parseLong(digitBuffer.toString());
                            digitBuffer.setLength(0);
                            depth = i - depth - 2;
                            break;
                        }
                    }

                    String[] methodSig = line.substring(pos).split("\\.", 2);

                    DynamicClass dc = classMap.get(methodSig[0]);
                    if (dc == null) {
                        dc = new DynamicClass(methodSig[0]);
                        classMap.put(methodSig[0], dc);
                    }
                    dc.addMethod(methodSig[1]);
                }


                PreparedStatement pstmtClass = DB.getPStmt("select app_no, id from class where (app_no=? or app_no=0) and name=?");
                PreparedStatement pstmtMethod = DB.getPStmt("select accessflags from method where class_id=? and signature=?");


                for (DynamicClass dc : classMap.values()) {
                    if (dc.name.startsWith("dalvik")) {
                        continue;
                    }

                    numOfTotalClass++;

                    pstmtClass.setInt(1, pkgNo);
                    pstmtClass.setString(2, dc.name.replace("/", "."));
                    rs = pstmtClass.executeQuery();
                    if (rs.next()) {
                        int appNo = rs.getInt(1);
                        int classId = rs.getInt(2);
                        if (appNo > 0) {
                            numOfClass++;
                        }
                        for (String method : dc.methods) {
                            numOfTotalMethod++;
                            if (appNo > 0) {
                                numOfMethod++;
                            }
                            pstmtMethod.setInt(1, classId);
                            pstmtMethod.setString(2, method);
                            rs = pstmtMethod.executeQuery();
                            if (rs.next()) {
                                int accessFlag = rs.getInt(1);
                                if (AccessFlags.NATIVE.isSet(accessFlag)) {
                                    numOfTotalNativeMethod++;
                                    if (appNo > 0) {
                                        numOfNativeMethod++;
                                    }
                                }
                            } else {
                                System.out.println(classId + " " + method);
                            }
                        }
                    }
                }
            }

            PreparedStatement pstmt = DB.getPStmt("insert or replace into callgraph values(?,?,?,?,?,?,?,?)");
            pstmt.setInt(1, pkgNo);
            pstmt.setInt(2, numOfTotalMethod);
            pstmt.setInt(3, numOfTotalNativeMethod);
            pstmt.setInt(4, numOfTotalClass);
            pstmt.setInt(5, numOfMethod);
            pstmt.setInt(6, numOfNativeMethod);
            pstmt.setInt(7, numOfClass);
            pstmt.setInt(8, count);
            pstmt.execute();

            System.out.println(numOfTotalClass+" " + numOfTotalMethod +"("+numOfTotalNativeMethod+")" + numOfClass + " " +numOfMethod+"("+numOfNativeMethod+")");


            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int[] getMethod(int pkgNo, int classId, String signature) throws SQLException {
        ResultSet rs = DB.getStmt().executeQuery("select accessflags from method where class_id="+classId+" and signature='"+signature+"'");
        if(rs.next()) {
            int[] result=new int[3];
            result[0]= pkgNo;
            result[1]= classId;
            result[2] = rs.getInt(1);
            return result;
        } else {
            rs = DB.getStmt().executeQuery("select superclass from class where id="+classId);
            if(rs.next()) {
                classId = rs.getInt(1);
                if(classId>0) { // for java.lang.Object
                    rs = DB.getStmt().executeQuery("select app_no from class where id="+classId);
                    if(rs.next()) {
                        pkgNo=rs.getInt(1);
                        return getMethod(pkgNo, classId, signature);
                    }
                }
            }
        }
        return null;
    }
}
