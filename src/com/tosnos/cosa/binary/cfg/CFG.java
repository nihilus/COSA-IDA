package com.tosnos.cosa.binary.cfg;

import com.tosnos.cosa.binary.asm.*;
import com.tosnos.cosa.binary.asm.value.AbstractValue;
import com.tosnos.cosa.binary.function.Subroutine;
import soot.util.HashMultiMap;

import java.util.*;

/**
 * Created by kevin on 9/21/15.
 */
public class CFG {
    private HashMultiMap<Integer, Instruction> localDestinations = new HashMultiMap<Integer, Instruction>(); // address and instruction
    private Map<Integer, Instruction> returns = new HashMap<Integer, Instruction>();
    private Set<Instruction> indirects = new HashSet<Instruction>();
    // build cfg...

//    private CFG() {
//    }

    public static CFG parse(Subroutine method) {
        CFG cfg = new CFG();
        Instruction inst = method.getFirstInstruction();
        int itCount = 0;
        while(inst!=null) {
            // check IT....
            if(itCount>0) {
                itCount--;
            }

            if(inst.isIfThen()) {
                itCount = ((IfThenInstruction) inst).getConditionSwitch().length + 2;
            } else if(inst.isBranch()) {
                System.out.println(inst);
                Operand rd = ((BranchInstruction)inst).getBranchDestination();
                if(rd.isRegister()) {
                    if(((Register)rd).isLinkRegister()) {
                        cfg.returns.put(inst.getAddress(), inst);
                    } else {
                        cfg.indirects.add(inst);
                        // indirect
                        throw new RuntimeException("indirect");
                    }
                } else {
                    AbstractValue destination = rd.getValue();
                    if (!inst.isCall()) {
                        if (inst.isConditional()) {
                            Instruction next = inst.getNext(); // the next instruction of conditional should be a destination
                            if (next != null) {
                                cfg.localDestinations.put(next.getAddress(), inst); //
                            }
                        }
                        // get destination

                        cfg.localDestinations.put(destination.intValue(), inst);
                        // if jump.......printf... or system call, return.... abort...
                        System.out.println("destination.intValue()" + destination.intValue());

                    } else { // call, it can be abort
                        if (inst.isConditional()) {

                        }
                        System.out.println(destination.intValue());
                    }
                }
            } else if(inst.isTableSwitchBranch()) {
                for (int branch : ((TableSwitchBranchInstruction) inst).getBranches()) {
                    cfg.localDestinations.put(branch, inst);
                }
                // skip couple of instruction set...
            } else if(inst.isLoad() && ((LoadInstruction)inst).isPCRelative()) {
                if(inst.isConditional()) {
                    Instruction next = inst.getNext(); // the next instruction of conditional should be a destination
                    if(next!=null) {
                        cfg.localDestinations.put(next.getAddress(), inst);
                    }

                }
                cfg.returns.put(inst.getAddress(), inst);
            }
            inst = inst.getNext();
        }
        return cfg;
    }

//    public Edge getEdge(Integer address) {
//        return edgeMap.get(address);
//    }
}
