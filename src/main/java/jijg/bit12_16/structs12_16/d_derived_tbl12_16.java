package jijg.bit12_16.structs12_16;

import jijg.bit12_16.jdhuff12_16;

public class d_derived_tbl12_16
{
    public long[] maxcode = new long[18];
    public long[] valoffset = new long[17];

    public JHUFF_TBL12_16 pub;

    public int[] look_nbits = new int[1 << jdhuff12_16.HUFF_LOOKAHEAD];
    public short[] look_sym = new short[1 << jdhuff12_16.HUFF_LOOKAHEAD];
}
