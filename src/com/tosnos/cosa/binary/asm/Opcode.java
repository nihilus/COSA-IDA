package com.tosnos.cosa.binary.asm;

/**
 * Created by kevin on 12/7/14.
 */
public enum Opcode {
    DATA,
    IGNORABLE,
    B, BL, BLX, BX, POP, PUSH, STM, LDM, STR, LDR, MOVT,
    CBZ, CBNZ,
    //    LDREX, STREX,

    CLZ,

    BFC, BFI,
    PKH, // PLD - CACHE PRELOAD
    SBFX, UBFX,
    SMLA, SMLS,
    SMUL, SMUAD, SMUSD,
    UMAAL, UQADD,
    SWP, UXT, SXT, RBIT, REV, UXTA, SXTA,
    TXTA, SSAT, USAT,
    UDIV, SDIV,
    ADR, // ?? assign address directly
    LDA, STL,
    SWI, // SOFTWARE INTERRUPT FOR SYSTEM CALL USING R7 REGISTER.
    IT, // IF THEN
    TB, // TABLE BRANCH
    NOP,
    XT,
    QADD(true), QSUB(true), QDADD(true), QDSUB(true),
    VLDR(true), VSTR(true), VPOP(true), VPUSH(true), VMOV(true), VCVT(true), VDIV(true), VSUB(true), VADD(true),
    VLD1(true), VST1(true),

    UMLAL(Modifier.UPDATE_FLAG_NZ),
    UMULL(Modifier.UPDATE_FLAG_NZ),
    SMULL(Modifier.UPDATE_FLAG_NZ),
    SMLAL(Modifier.UPDATE_FLAG_NZ),

    ADD(Modifier.UPDATE_FLAG_NZCV),
    SUB(Modifier.UPDATE_FLAG_NZCV),
    RSB(Modifier.UPDATE_FLAG_NZCV),
    ADC(Modifier.UPDATE_FLAG_NZCV),
    SBC(Modifier.UPDATE_FLAG_NZCV),
    RSC(Modifier.UPDATE_FLAG_NZCV),
    NEG(Modifier.UPDATE_FLAG_NZCV),
    CMP(Modifier.UPDATE_FLAG_NZCV),
    CMN(Modifier.UPDATE_FLAG_NZCV),

    EOR(Modifier.UPDATE_FLAG_NZ),

    AND(Modifier.UPDATE_FLAG_NZ),
    ORR(Modifier.UPDATE_FLAG_NZ),
    BIC(Modifier.UPDATE_FLAG_NZ),
    ORN(Modifier.UPDATE_FLAG_NZ),

    MOV(Modifier.UPDATE_FLAG_NZ),
    MVN(Modifier.UPDATE_FLAG_NZ),

    TST(Modifier.UPDATE_FLAG_NZ),
    TEQ(Modifier.UPDATE_FLAG_NZ),

    ASR(Modifier.UPDATE_FLAG_NZ),
    LSL(Modifier.UPDATE_FLAG_NZ),
    LSR(Modifier.UPDATE_FLAG_NZ),
    ROR(Modifier.UPDATE_FLAG_NZ),
    RRX(Modifier.UPDATE_FLAG_NZ),

    MUL(Modifier.UPDATE_FLAG_NZ),
    MLA(Modifier.UPDATE_FLAG_NZ),
    MLS;

    private final byte flag;
    private final byte thumb_flag;
    private final boolean vfp;

    Opcode(int flag) {
        this(flag, 0, false);
    }

    Opcode(int flag, int thumb_flag) {
        this(flag, thumb_flag, false);
    }

    Opcode(int flag, int thumb_flag, boolean vfp) {
        this.flag = (byte)flag;
        this.thumb_flag = (byte)flag;
        this.vfp = vfp;
    }

    Opcode(boolean vfp) {
        this(0, 0, vfp);
    }

    Opcode() {
        this(0, 0,false);
    }

    public byte getFlag() {
        return flag;
    }

    public boolean isVFP() {
        return vfp;
    }

    public boolean isCovered(boolean hasShift, short conditional) {
        switch(conditional) {
            case Modifier.CONDI_EQ:
            case Modifier.CONDI_NE:
                if((flag&Modifier.UPDATE_FLAG_Z)>0) {
                    return true;
                } else {
                    return false;
                }
            case Modifier.CONDI_PL:
            case Modifier.CONDI_MI:
                if((flag&Modifier.UPDATE_FLAG_N)>0) {
                    return true;
                } else {
                    return false;
                }
            case Modifier.CONDI_CS:
            case Modifier.CONDI_CC:
                if(hasShift || (flag&Modifier.UPDATE_FLAG_C)>0) {
                    return true;
                } else {
                    return false;
                }
            case Modifier.CONDI_HI:
            case Modifier.CONDI_LS:
                if((hasShift || (flag&Modifier.UPDATE_FLAG_C)>0) && (flag&Modifier.UPDATE_FLAG_Z)>0) {
                    return true;
                } else {
                    return false;
                }
            case Modifier.CONDI_GE:
            case Modifier.CONDI_LT:
                if((flag&Modifier.UPDATE_FLAG_N)>0 && (flag&Modifier.UPDATE_FLAG_V)>0) {
                    return true;
                } else {
                    return false;
                }
            case Modifier.CONDI_GT:
            case Modifier.CONDI_LE:
                if((flag&Modifier.UPDATE_FLAG_N)>0 && (flag&Modifier.UPDATE_FLAG_N)>0 && (flag&Modifier.UPDATE_FLAG_V)>0) {
                    return true;
                } else {
                    return false;
                }
            default:
                throw new RuntimeException("not supported sign "+conditional);
        }
    }



// unused...
//    LDC, STC, DMB, VMUL, VMOV, VCVT, VSUB, VCMP, VLDM, VSTM, VMRS, VCVTR, FLDM, FSTM, VNEG, VDIV, VPOP, VPUSH, VADD,
//    VAND, VABS, VCMPE, VORR, VEOR, VMLA, VSQRT, VNMLS, VMLS, VEXT, VBSL, VST1, VABD, VCGE, VHADD, VNMUL, VLD1, VST4,
//    PLD, VDUP, VST2, VDL2, VLD3, VST3, VSHRN, MSR, SVC, VPADD, VLD4, VBIT, VCNT, CDP, VSHLL, BKPT, VNMLA, VMAX, VMIN,
//    VZIP, VPAD, VRECPE, VSHL, VC, VRSHRN, VLD2, VUZP, VSWP, VQADD, VBIC, VTBL, VTRN, VQSUB, VSHR, VQRSHRUN, VMOVN,
//    VQDMUL, VQMOVN, SMMLS, VMVN, VQSHL, VABAL, VBIF, VPMAX, VQMOVUN, MRC2, MRC, VQRSHL, VSUBHN, VQRDMULH, VSRI,
//    VRECPS, VQNEG, VQRSHRN, LDC2, STC2,

                // nz
}

/*
ADC, ADCNVS, ADCS, ADD, ADDCC, ADDCS, ADDEQ, ADDGE, ADDGT, ADDHI,
ADDLE, ADDLS, ADDLT, ADDMI, ADDNE, ADDPL, ADDS, ADR, AND, ANDCC,
ANDEQ, ANDGE, ANDGT, ANDHI, ANDLE, ANDLS, ANDLT, ANDMI, ANDNE, ANDNES,
ANDNVS, ANDPL, ANDS, ASR, ASRCS, ASREQ, ASRGE, ASRGT, ASRLE, ASRLT,
ASRS, B, BCC, BCS, BEQ, BFI, BGE, BGT, BHI, BIC,
BICCC, BICEQ, BICGE, BICGT, BICHI, BICLE, BICLS, BICLT, BICMI, BICNE,
BICNES, BICPL, BICS, BKPT, BL, BLE, BLEQ, BLS, BLT, BLX,
BMI, BNE, BPL, BX, BXCC, BXEQ, BXGT, BXHI, BXLE, BXNE,
CBNZ, CBZ, CDP, CLZ, CMN, CMNMI, CMNP, CMP, CMPCC, CMPEQ,
CMPHI, CMPLE, CMPLS, CMPNE, CMPP, CMPPL, DMB, EOR, EORCC, EORCS,
EOREQ, EORGE, EORGT, EORLE, EORLT, EORMI, EORNE, EORPL, EORS, FLDMIAX,
FSTMIAX, IT, ITE, ITEE, ITEEE, ITEET, ITET, ITETE, ITETT, ITT,
ITTE, ITTEE, ITTET, ITTT, ITTTE, ITTTT, LDC, LDCL, LDMCSFD, LDMDB,
LDMEQFD, LDMEQIA, LDMFD, LDMGEIA, LDMGTIA, LDMHIFD, LDMHIIA, LDMIA, LDMIB, LDMLEDB,
LDMLTIA, LDMMIIA, LDMNEFD, LDMNEIA, LDMNVFD, LDMNVIA, LDMPLIA, LDR, LDRB, LDRBT,
LDRCC, LDRCCB, LDRCCH, LDRCS, LDRD, LDREQ, LDREQB, LDREQH, LDREX, LDREXB,
LDRGE, LDRGEB, LDRGESH, LDRGT, LDRGTB, LDRGTH, LDRH, LDRHI, LDRHIB, LDRHIH,
LDRHT, LDRLE, LDRLEB, LDRLS, LDRLSSH, LDRLT, LDRLTB, LDRLTH, LDRMI, LDRMIB,
LDRMIBT, LDRMIH, LDRNE, LDRNEB, LDRNEH, LDRNV, LDRNVB, LDRNVBT, LDRNVD, LDRPL,
LDRPLB, LDRPLH, LDRSB, LDRSBT, LDRSH, LDRT, LSL, LSLCC, LSLCS, LSLEQ,
LSLGE, LSLLE, LSLLS, LSLLT, LSLMI, LSLNE, LSLS, LSR, LSRGT, LSRHI,
LSRLT, LSRNE, LSRS, MLA, MLACC, MLAEQ, MLAGE, MLAGT, MLAHI, MLALE,
MLALT, MLAMI, MLANE, MLS, MOV, MOVCC, MOVCS, MOVEQ, MOVEQS, MOVEQW,
MOVGE, MOVGEW, MOVGT, MOVGTW, MOVHI, MOVHIW, MOVLE, MOVLEW, MOVLS, MOVLSW,
MOVLT, MOVLTW, MOVMI, MOVNE, MOVNES, MOVPL, MOVPLW, MOVS, MOVT, MOVTEQ,
MOVTGE, MOVTHI, MOVTLE, MOVTMI, MOVTNE, MOVW, MRC, MRC2, MSR, MUL,
MULCC, MULCS, MULEQ, MULGE, MULGT, MULHI, MULLE, MULLS, MULLT, MULMI,
MULNE, MULS, MVN, MVNCC, MVNEQS, MVNLT, MVNNES, MVNS, NEGEQ, NEGGT,
NEGLE, NEGLT, NEGMI, NEGNE, NEGS, NOP, NOPLE, ORN, ORR, ORRCC,
ORRCS, ORREQ, ORREQS, ORRGE, ORRGT, ORRHI, ORRLE, ORRLS, ORRLT, ORRMI,
ORRNE, ORRNES, ORRPL, ORRS, PLD, POP, POPCC, POPGE, POPGT, POPHI,
POPLE, POPNE, PUSH, PUSHGE, RORS, RSB, RSBCC, RSBCS, RSBEQ, RSBGE,
RSBGT, RSBGTS, RSBHI, RSBLE, RSBLS, RSBLT, RSBNE, RSBS, RSC, RSCS,
SBC, SBCS, SBFX, SMLABB, SMLABT, SMLAL, SMLALBT, SMLATB, SMLAWT, SMMLS,
SMULBB, SMULBT, SMULL, SMULLGT, SMULWB, STC, STCL, STMDB, STMEA, STMEQIA,
STMFA, STMFD, STMGEDA, STMGEIA, STMGTIA, STMHIIA, STMIA, STMIB, STMLEIA, STMLTIA,
STMMIIA, STMNEIA, STMNEIB, STR, STRB, STRBT, STRCC, STRCCB, STRCCH, STRCS,
STRCSH, STRD, STREQ, STREQB, STREQD, STREQH, STREX, STREXB, STRGE, STRGEB,
STRGEH, STRGT, STRGTB, STRGTH, STRH, STRHI, STRHIB, STRHIH, STRHT, STRLE,
STRLEB, STRLS, STRLSB, STRLT, STRLTB, STRLTH, STRMI, STRMIB, STRMIH, STRNE,
STRNEB, STRNED, STRNEH, STRNV, STRNVB, STRPL, STRPLB, STRPLH, STRT, SUB,
SUBCC, SUBCS, SUBEQ, SUBEQS, SUBGE, SUBGT, SUBHI, SUBLE, SUBLS, SUBLT,
SUBMI, SUBNE, SUBPL, SUBS, SVC, SVCLT, SXTAH, SXTB, SXTBGT, SXTBHI,
SXTH, SXTHGT, SXTHLS, TBB, TBH, TEQ, TEQEQ, TEQNE, TEQP, TST,
TSTEQ, TSTP, UBFX, UBFXEQ, UBFXNE, UBFXPL, UMLAL, UMULL, UXTAB, UXTB,
UXTBEQ, UXTBGE, UXTBGT, UXTBHI, UXTBLE, UXTBLS, UXTBLT, UXTBMI, UXTBNE, UXTH,
UXTHCC, UXTHCS, UXTHEQ, UXTHGT, UXTHHI, UXTHLE, UXTHLS, UXTHLT, UXTHMI, UXTHNE,
UXTHPL, VABAL, VABD, VABDL, VABS, VABSEQ, VABSGE, VABSLE, VABSLS, VABSMI,
VABSNE, VABSPL, VADD, VADDEQ, VADDGE, VADDGT, VADDHI, VADDL, VADDLE, VADDLS,
VADDLT, VADDMI, VADDNE, VADDPL, VADDW, VAND, VBIC, VBIF, VBIT, VBSL,
VCEQ, VCGE, VCGT, VCMP, VCMPE, VCNT, VCVT, VCVTCC, VCVTCS, VCVTEQ,
VCVTGE, VCVTGT, VCVTHI, VCVTLE, VCVTLS, VCVTLT, VCVTMI, VCVTNE, VCVTPL, VCVTR,
VCVTRGT, VCVTRLE, VCVTRMI, VCVTRNE, VCVTRPL, VDIV, VDIVEQ, VDIVGT, VDIVLE, VDIVLS,
VDIVMI, VDIVNE, VDIVPL, VDUP, VEOR, VEXT, VHADD, VLD1, VLD2, VLD3,
VLD4, VLDMDB, VLDMEA, VLDMIA, VLDR, VLDRCC, VLDRCS, VLDREQ, VLDRGE, VLDRGT,
VLDRHI, VLDRLE, VLDRLS, VLDRLT, VLDRMI, VLDRNE, VLDRPL, VMAX, VMIN, VMLA,
VMLAEQ, VMLAGE, VMLAL, VMLALE, VMLAMI, VMLANE, VMLS, VMLSGT, VMLSHI, VMLSL,
VMOV, VMOVCC, VMOVCS, VMOVEQ, VMOVGE, VMOVGT, VMOVHI, VMOVL, VMOVLE, VMOVLS,
VMOVLT, VMOVMI, VMOVN, VMOVNE, VMOVPL, VMRS, VMUL, VMULCC, VMULEQ, VMULGE,
VMULGT, VMULHI, VMULL, VMULLE, VMULLS, VMULLT, VMULMI, VMULNE, VMULPL, VMVN,
VNEG, VNEGEQ, VNEGGE, VNEGGT, VNEGLE, VNEGMI, VNEGNE, VNMLA, VNMLS, VNMLSEQ,
VNMLSPL, VNMUL, VNMULGE, VORR, VPADAL, VPADD, VPADDL, VPMAX, VPOP, VPUSH,
VPUSHLT, VQADD, VQDMULH, VQMOVN, VQMOVUN, VQNEG, VQRDMULH, VQRSHL, VQRSHRN, VQRSHRUN,
VQSHL, VQSUB, VRECPE, VRECPS, VRSHRN, VSHL, VSHLL, VSHR, VSHRN, VSQRT,
VSRI, VST1, VST2, VST3, VST4, VSTMDB, VSTMEA, VSTMIA, VSTMLSDB, VSTR,
VSTRCC, VSTREQ, VSTRGE, VSTRGT, VSTRHI, VSTRLE, VSTRLS, VSTRLT, VSTRMI, VSTRNE,
VSTRPL, VSUB, VSUBEQ, VSUBGE, VSUBGT, VSUBHN, VSUBL, VSUBLE, VSUBLS, VSUBLT,
VSUBMI, VSUBNE, VSUBPL, VSWP, VTBL, VTRN, VUZP, VZIP
 */

/*

    DATA,
    IGNORABLE,
    add, b, bl, and, sub, mov, mvn, neg, blx, bx, pop, push, stm, ldm, str, ldr, movt, adc,
    cbz, cbnz,
    //    ldrex, strex,
    qadd, qsub, qdadd, qdsub,
    asr, lsl, lsr, ror, rrx, eor, orr, bic, orn, clz,
    rsb, sbc, rsc,
    bfc, bfi,
    mul, mla, mls, pkh, // pld - cache preload
    sbfx, ubfx,
    smla, smls,
    umlal, umull,
    smul, smull, smuad, smusd,
    umaal, uqadd,
    swp, uxtb, sxth, sxtb, rbit, rev, uxth, uxtah, uxtab, sxtah, sxtab,
    txta, ssat, usat,
    udiv, sdiv,
    adr, // ??
    lda, stl,
    swi, // software interrupt for system call using r7 register.
    cmp, cmn, teq, tst,
    it,
    vldr, vstr,
    tb, // table branch
    nop,
//    --- example
//    mov     r7, #1			@ set r7 to 1 - the syscall for exit
//    swi     0			@ then invoke the syscall from linux

//    bkpt, // unused
//    nop, cbz, cmp,

//      adc, // unused
//
//      aes, sha1, sha2, sha256, smc,cps, hvc,
//      adf, muf, suf, rsf, dvf, rdf, pow, rpw, rmf, fml, fdv, frd, pol, mvf, abs, rnd, sqt, log, lgn,
//      exp, sin, cos, tan, asn, acs, atn, urd,    // Floating point data operations
//      ldf, stf, // Floating point data transfer
//      cnf, cmf, // Floating-point comparisons
//      wfs, rfs, // Floating-point
//      isb, dmb, dsb, //Data Memory Barrier, Data Synchronization Barrier, and Instruction Synchronization Barrier.
//      teq, tst, //Test and Test Equivalence.
//      swi, //software interrupt
//      cmp, cmn, // unused
//      nop, sev, wfe, wfi, yield, // No Operation, Set Event, Wait For Event, Wait for Interrupt, and Yield
//      miaph, mia, // Multiply with internal accumulate
//      mrs, msr,  mar, mra, mrc, mrc2, mrrc, mrrc2, mcr2, mcr, mcrr, ldc, stc2, stc, cdp, cdp2, ldc2, // Coprocessor instructions unused
//      cbz, cbnz, it, // IT instructions
//      svc, // SuperVisor
//      dbg,  // Call Breakpoint
//      smls,
//      v // vector operation
 */