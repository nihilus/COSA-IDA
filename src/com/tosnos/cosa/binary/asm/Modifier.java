package com.tosnos.cosa.binary.asm;


/**
 N(Negative) is set if the result of a data processing instruction was negative.
 Z(Zero) is set if the result was zero.
 C(Carry) is set if an addition, subtraction or compare causes a result bigger than 32 bits, or is set from the output of the shifter for move and logical instructions.
 V(Overflow) is set if an addition, subtraction or compare produces a signed result bigger than 31 bits.

 Code	Meaning (for cmp or subs)	Flags Tested
 eq	Equal.	Z==1
 ne	Not equal.	Z==0
 cs or hs	Unsigned higher or same (or carry set).	C==1
 cc or lo	Unsigned lower (or carry clear).	C==0
 mi	Negative. The mnemonic stands for "minus".	N==1
 pl	Positive or zero. The mnemonic stands for "plus".	N==0
 vs	Signed overflow. The mnemonic stands for "V set".	V==1
 vc	No signed overflow. The mnemonic stands for "V clear".	V==0
 hi	Unsigned higher.	(C==1) && (Z==0)
 ls	Unsigned lower or same.	(C==0) || (Z==1)
 ge	Signed greater than or equal.	N==V
 lt	Signed less than.	N!=V
 gt	Signed greater than.	(Z==0) && (N==V)
 le	Signed less than or equal.	(Z==1) || (N!=V)
 al (or omitted)	Always executed.	None tested.
 **/

public interface Modifier {
    enum SIGN { EQ, NE, CS, CC, MI, PL, VS, VC, HI, LS, GE, LT, GT, LE } //inequality

    String[] conditions = new String[]{"EQ", "NE", "CS", "CC", "MI", "PL", "VS", "VC", "HI", "LS", "GE", "LT", "GT", "LE", "AL", "NV"};
    int N = 3;
    int Z = 2;
    int C = 1;
    int V = 0;
    int WORD = 4;
    int HALFWORD = WORD >> 1;
    int BYTE = 1;
    int DOUBLEWORD = WORD << 1;
    int BITS_WORD = WORD*8;
    int BITS_HALFWORD =  WORD*8;
    int BITS_BYTE =  WORD*8;
    int BITS_DOUBLEWORD = DOUBLEWORD*8;

    byte CONDI_EQ = 0;
    byte CONDI_NE = 1;
    byte CONDI_CS = 2;
    byte CONDI_HS = 2;
    byte CONDI_CC = 3;
    byte CONDI_LO = 3;
    byte CONDI_MI = 4;
    byte CONDI_PL = 5;
    byte CONDI_VS = 6;
    byte CONDI_VC = 7;
    byte CONDI_HI = 8;
    byte CONDI_LS = 9;
    byte CONDI_GE = 10;
    byte CONDI_LT = 11;
    byte CONDI_GT = 12;
    byte CONDI_LE = 13;
    byte CONDI_AL = 14;
    byte CONDI_NV = 15;

    byte[] INVCOND = new byte[] {CONDI_NE, CONDI_EQ, CONDI_CC, CONDI_CS, CONDI_PL, CONDI_MI, CONDI_VC, CONDI_VS, CONDI_LS, CONDI_HI, CONDI_LT, CONDI_GE, CONDI_LE, CONDI_GT, CONDI_AL, CONDI_NV};


    byte UPDATE_FLAG_N = 8; // 0b1000;
    byte UPDATE_FLAG_Z = 4; // 0b0100;
    byte UPDATE_FLAG_C = 2; // 0b0010;
    byte UPDATE_FLAG_V = 1; // 0b0001;
    byte UPDATE_FLAG_NZCV = UPDATE_FLAG_N + UPDATE_FLAG_Z + UPDATE_FLAG_C + UPDATE_FLAG_V;
    byte UPDATE_FLAG_NV = UPDATE_FLAG_N + UPDATE_FLAG_V;
    byte UPDATE_FLAG_NZ = UPDATE_FLAG_N + UPDATE_FLAG_Z; // C is optional

    short FLAG_PN = 0x80; // 0b10000000;
    short FLAG_NN = 0x40; // 0b01000000;
    short FLAG_MN = 0xC0; // 0b11000000; // mask

    short FLAG_PZ = 0x20; // 0b00100000;
    short FLAG_NZ = 0x10; // 0b00010000;
    short FLAG_MZ = 0x30; // 0b00110000; // mask

    short FLAG_PC = 0x08; // 0b00001000;
    short FLAG_NC = 0x04; // 0b00000100;
    short FLAG_MC = 0x0C; // 0b00001100; // mask

    short FLAG_PV = 0x02; // 0b00000010;
    short FLAG_NV = 0x01; // 0b00000001;
    short FLAG_MV = 0x03; // 0b00000011; // mask

    int[] BYTES = new int[]{ BYTE, HALFWORD, WORD, WORD, WORD, DOUBLEWORD, DOUBLEWORD, HALFWORD, WORD, HALFWORD, WORD, HALFWORD, WORD, DOUBLEWORD};
    int[] BITS = new int[]{ BITS_BYTE, BITS_HALFWORD, BITS_WORD, BITS_WORD, BITS_WORD, BITS_DOUBLEWORD, BITS_DOUBLEWORD, BITS_HALFWORD, BITS_WORD, BITS_HALFWORD, BITS_WORD, BITS_HALFWORD, BITS_WORD, BITS_DOUBLEWORD};

    byte TYPE_BYTE =        0x0;
    byte TYPE_HALF =        0x1;
    byte TYPE_INT =         0x2;
    byte TYPE_ADDRESS =     0x2;
    byte TYPE_STRING =      0x3;
    byte TYPE_FLOAT =       0x4;
    byte TYPE_DOUBLE =      0x5;
    byte TYPE_LONG =        0x6;

    byte VLD_DATA_8    = 0x7;
    byte VLD_DATA_16   = 0x8;
    byte VLD_DATA_32   = 0x9;
    byte VLD_DATA_64   = 0xa;

    int MODIFIER_COND_DO_NOT_SKIP_FOR_FALSE  = 0x00010;
    int MODIFIER_UPDATE_FLAG    = 0x00020;
    int MODIFIER_UPDATE_FLAG_NZ = 0x00040;
    int MODIFIER_DECREMENT  = 0x00080;
    int MODIFIER_BEFORE     = 0x00100;
    int MODIFIER_8          = 0x00200;
    int MODIFIER_16         = 0x00400;
    int MODIFIER_EXTENTION  = 0x00800;

    int MODIFIER_USER       = 0x01000;
    int MODIFIER_SIGNED     = 0x02000;
    int MODIFIER_WIDTH      = 0x04000;
    int MODIFIER_26_BIT_PSR = 0x08000;

    int MODIFIER_MB  = 0x10000; // for SMLAL<x><y>{cond} RdLo, RdHi, *Rm*, Rs
    int MODIFIER_MT  = 0x20000; // for SMLAL<x><y>{cond} RdLo, RdHi, Rm, *Rs*
    int MODIFIER_SB  = 0x40000; // for SMLAL<x><y>{cond} RdLo, RdHi, *Rm*, Rs
    int MODIFIER_ST  = 0x80000; // for SMLAL<x><y>{cond} RdLo, RdHi, Rm, *Rs*
    int MODIFIER_WIDE = 0x100000; // for SMLAL<x><y>{cond} RdLo, RdHi, Rm, *Rs*



    int VFP_DATA_8     = 0x0001000;
    int VFP_DATA_16    = 0x0002000;
    int VFP_DATA_32    = 0x0004000;
    int VFP_DATA_64    = 0x0008000;
    int VFP_DATA_U     = 0x0010000;
    int VFP_DATA_S     = 0x0020000; //
    int VFP_DATA_F     = 0x0040000;
    int VFP_DATA_I     = 0x0080000;
    int VFP_DATA_P     = 0x0100000;
    int VFP_DATA_FROM_8  = 0x0200000;
    int VFP_DATA_FROM_16 = 0x0400000;
    int VFP_DATA_FROM_32 = 0x0800000;
    int VFP_DATA_FROM_64 = 0x1000000;
    int VFP_DATA_FROM_U  = 0x2000000;
    int VFP_DATA_FROM_S  = 0x4000000;
    int VFP_DATA_FROM_F  = 0x8000000;
    int VFP_DATA_FROM_I  = 0x10000000;
    int VFP_DATA_FROM_P  = 0x20000000;
}
