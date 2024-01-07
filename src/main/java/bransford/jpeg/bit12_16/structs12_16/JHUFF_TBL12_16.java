package bransford.jpeg.bit12_16.structs12_16;

public class JHUFF_TBL12_16
{
    /* These two fields directly represent the contents of a JPEG DHT marker */
    public byte[] bits = new byte[17]; /* bits[k] = # of symbols with codes of */
    /* length k bits; bits[0] is unused */

    public byte[] huffval = new byte[256]; /* The symbols, in order of incr code length */
    /* This field is used only during compression.  It's initialized FALSE when
     * the table is created, and set TRUE when it's been output to the file.
     * You could suppress output of a table by setting this to TRUE.
     * (See jpeg_suppress_tables for an example.)
     */
    public boolean sent_table; /* TRUE when table has been output */
}
