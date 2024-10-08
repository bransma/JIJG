package jijg.bit12_16.structs12_16;

import jijg.bit12_16.jpeglib12_16;

public class JQUANT_TBL12_16
{
    /* This array gives the coefficient quantizers in natural array order
     * (not the zigzag order in which they are stored in a JPEG DQT marker).
     * CAUTION: IJG versions prior to v6a kept this array in zigzag order.
     */
    public short[] quantval = new short[jpeglib12_16.DCTSIZE2]; /* quantization step for each coefficient */
    /* This field is used only during compression.  It's initialized FALSE when
     * the table is created, and set TRUE when it's been output to the file.
     * You could suppress output of a table by setting this to TRUE.
     * (See jpeg_suppress_tables for an example.)
     */
    public boolean sent_table; /* TRUE when table has been output */
}
