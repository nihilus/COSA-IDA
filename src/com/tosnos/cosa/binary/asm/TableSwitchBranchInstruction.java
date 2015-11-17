package com.tosnos.cosa.binary.asm;

import com.microsoft.z3.Z3Exception;
import com.tosnos.cosa.binary.LibraryModule;

import java.io.IOException;
import java.util.*;


/**
 * Created by kevin on 8/3/14.
 */
public class TableSwitchBranchInstruction extends Instruction {
    private final static int MAX_TRY = 10; // the maximum number of backtracking for finding "cmp"
    Set<Integer> branches = null;

    public TableSwitchBranchInstruction(LibraryModule libraryModule, int address, boolean thumb, Opcode opcode, int modifier, byte condition, Operand operand) {
        super(libraryModule, address, thumb, opcode, modifier, condition, operand);
    }


//    public TableSwitchBranchInstruction(Instruction instruction) {
//        super(instruction);
//    }

    public Operand getRn() {
        return operands[0];
    }

    public Operand getRm() {
        return operands[1];
    }

    @Override
    public boolean isTableSwitchBranch() {
        return true;
    }

    @Override
    public boolean isConditional() {
        return true;
    }

    @Override
    public Operand[] getRequiredOperands() {
        return null;
    }

    @Override
    public Operand[] getAffectedOperands() {
        return null;
    }

    public Set<Integer> getBranches() {
        try {
            if(branches==null) {
                branches = new HashSet<Integer>();
                int numOfCase = 0;
                Instruction cmp = getPrev();
                for(int i=0;i<MAX_TRY;i++) {
                    if(cmp==null) {
                        return branches;
                    }
                    if(cmp.isCompare()) {
                        numOfCase = cmp.getOperand(1).getValue().intValue() + 1;
                        break;
                    }
                    cmp = cmp.getPrev();
                }
                int size = ((modifier & 0xf)== TYPE_BYTE) ? 1 : 2;
                int base = address + (thumb?THUMB_PC_RELATIVE:ARM_PC_RELATIVE); // get pc
                for(int i=0;i<numOfCase;i++)  {
                    int branch = getLibraryModule().read(base + i * size, (byte)(modifier & 0xf)).intValue() * 2 + base;
                    branches.add(branch);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return branches;
    }

}
