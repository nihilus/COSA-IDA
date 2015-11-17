package com.tosnos.cosa.binary.cfg;

import com.tosnos.cosa.binary.asm.Instruction;

/**
 * Created by kevin on 9/22/15.
 */
public class Edge {
    public static int RETURN      = 0x20;
    public static int ABORT       = 0x40;
    public static int INDIRECT    = 0x80;
    private Instruction instruction;
    private int attribute = 0;

    // conditional ... condition is the same....skip compare, condition, nzcv compare destination values...
    public Edge(Instruction instruction) {
        this.instruction = instruction;
    }

    public void setAttribute(int attribute) {
        this.attribute |= attribute;
    }

    public boolean isJump() {
        return instruction.isBranch() && !instruction.isCall();
    }

    public boolean isCall() {
        return instruction.isCall();
    }

    public boolean isConditional() {
        return instruction.isConditional();
    }

}
