package jijg.bit8.structs8;

import jijg.bit8.jdhuff8;

public class d_derived_tbl8
{
    public long[] maxcode = new long[18];
    public long[] valoffset = new long[17];

    public JHUFF_TBL8 pub;

    public int[] look_nbits = new int[1 << jdhuff8.HUFF_LOOKAHEAD];
    public short[] look_sym = new short[1 << jdhuff8.HUFF_LOOKAHEAD];
}
