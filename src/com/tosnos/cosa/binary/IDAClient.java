package com.tosnos.cosa.binary;

import java.io.*;
import java.net.Socket;

/**
 * Created by kevin on 3/23/15.
 */
public class IDAClient {
    public static final byte SYMTABLE     = (byte) 0x1;
    public static final byte ASMFILE      = (byte) 0x2;
    public static final byte CALLGRAPH    = (byte) 0x4;
    public static final byte COMPRESS     = (byte) 0x8;

    private static final byte PREAMBLE         = (byte) 0xF1;
    private static final byte RES_OK           = (byte) 0x20;
    private static final byte RES_FAIL         = (byte) 0x21;
    private static final byte RES_BUSY         = (byte) 0x22; // file is analyzing
    private static final byte REQ_DONE         = (byte) 0x1F;
    private static final byte RES_SEND_FILE    = (byte) 0x21;
    private static final byte REQ_SYMTABLE     = (byte) 0x22;
    private static final byte REQ_ASMFILE      = (byte) 0x23;
    private static final byte REQ_CALLGRAPH    = (byte) 0x24;
    private static final byte[] FILE_TYPE = { SYMTABLE, ASMFILE, CALLGRAPH };
    private static final String[] FILE_TYPE_EXT = { ".sym",".asm",".gdl"};
    private static final byte[] REQ_FILE_TYPE = { REQ_SYMTABLE, REQ_ASMFILE, REQ_CALLGRAPH };
    private static final int port = 3125;

    public static long getUnsignedInt(int x) {
        return x & 0x00000000ffffffffL;
    }

    public static String removeExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int index = filename.lastIndexOf('.');
        if (index == -1) {
            return filename;
        } else {
            return filename.substring(0, index);
        }
    }

    public static boolean getFiles(String server, String directory, File file, int request) {
        if(!file.exists()) {
            System.err.println("File is not exist");
            return false;
        }
        String fileName = file.getName();
        String fileNameWithoutExt = removeExtension(fileName);

        byte[] bFileName = fileName.getBytes();
        long size = file.length();
        Socket socket = null;
        byte[] buffer = new byte[4096];

        int res;
        int len;
        try {
            socket = new Socket(server, port);
            DataOutputStream dos  = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            dos.writeByte(PREAMBLE);
            dos.writeByte(bFileName.length);
            dos.writeInt((int)size); // filesize
            dos.write(bFileName);
            dos.flush();

            res = dis.readByte();

            while(res==RES_BUSY) {
                res = dis.readByte();
            }

            if(res == RES_SEND_FILE) {
                FileInputStream fi = new FileInputStream(file);
                long index = 0;
                while ((len = fi.read(buffer)) > 0) {
                    index += len;
                    dos.write(buffer, 0, len);
                    if (index == size) {
                        System.out.println(" size = "+index);
                        dos.flush();
                        break;
                    }
                }
                dis.readByte(); // wait for ACK
                System.err.println("sending file done");
            }

            System.err.println("check done");

            for(int i=0;i<3;i++) {
                if ((request & FILE_TYPE[i]) > 0) {
                    dos.writeByte(REQ_FILE_TYPE[i]);
                    System.err.println("send file request");
                    dos.flush();
                    File tempFile = new File(directory, fileNameWithoutExt + FILE_TYPE_EXT[i]);
                    FileOutputStream out = new FileOutputStream(tempFile);
                    size = getUnsignedInt(dis.readInt());
                    long index = 0;
                    while ((len = dis.read(buffer)) > 0) {
                        index += len;
                        out.write(buffer, 0, len);
                        if (index == size) {
                            break;
                        }
                    }
                    System.err.println("received");
                    out.close();
                }
            }
            dos.writeByte(REQ_DONE);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(socket!=null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public static void main(String[] args) {
        File file = new File("libhello.so");

        if (getFiles("neptune.usc.edu", ".", file, IDAClient.ASMFILE | IDAClient.SYMTABLE | IDAClient.CALLGRAPH | IDAClient.COMPRESS)) {
            System.out.println("success");
        } else {
            System.out.println("failed");
        }
    }
}
