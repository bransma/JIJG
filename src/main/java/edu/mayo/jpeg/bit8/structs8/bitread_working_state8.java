package edu.mayo.jpeg.bit8.structs8;

public class bitread_working_state8
{
    /* Bitreading working state within an MCU */
    /* Current data source location */
    /* We need a copy, rather than munging the original, in case of suspension */
    public int next_input_byte; // index into ByteBuffer for next byte
    public byte next_byte; // good for debug
    public int bytes_in_buffer; /* # of bytes remaining in source buffer */
    /* Bit input buffer --- note these values are kept in register variables,
     * not in this struct, inside the inner loops.
     */
    public int get_buffer; /* current bit-extraction buffer */
    public int bits_left; /* # of unused bits in it */
    /* Pointer needed by jpeg_fill_bit_buffer. */
    public jpeg_decompress_struct8 cinfo; /* back link to decompress master record */
}
