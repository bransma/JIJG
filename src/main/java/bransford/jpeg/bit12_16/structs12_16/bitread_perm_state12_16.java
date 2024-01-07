package bransford.jpeg.bit12_16.structs12_16;

public class bitread_perm_state12_16
{
    /* Bitreading state saved across MCUs */
    public int get_buffer;    /* current bit-extraction buffer */
    public int bits_left;        /* # of unused bits in it */
}
