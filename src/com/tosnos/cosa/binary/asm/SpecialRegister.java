package com.tosnos.cosa.binary.asm;

/**
 * Created by kevin on 7/9/14.
 */
public class SpecialRegister extends Register {
    public static final int APSR = 0;
    public static final int CR = 1;
    public static final int CPS = 2;
    public static final int SPSR = 3;
    public static final int FPSID = 4;
    public static final int FPSCR = 5;
    public static final int FPEXC = 6;

    private final int type;
    private final String name;

    //The Application Program Status Register (APSR)
    //Saved Program Status Registers (SPSRs).
    //The Current Program Status Register (CPSR)

    SpecialRegister(String name, int number, int type, boolean negative) {
        super(number, negative);
        this.number = number; // make a unique number for each Type
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public int getNumberOfRegisters() {
        return 0;
    }

    @Override
    public boolean isStackPointer() {
        return false;
    }

    @Override
    public boolean isLinkRegister() {
        return false;
    }

    @Override
    public boolean isProgramCounter() {
        return false;
    }

    @Override
    public boolean isGeneralRegister() {
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

}
