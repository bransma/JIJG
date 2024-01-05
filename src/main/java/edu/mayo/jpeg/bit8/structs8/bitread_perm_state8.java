package edu.mayo.jpeg.bit8.structs8;

public class bitread_perm_state8
{
    /* Bitreading state saved across MCUs */
    public int get_buffer;    /* current bit-extraction buffer */
    public int bits_left;        /* # of unused bits in it */
}
