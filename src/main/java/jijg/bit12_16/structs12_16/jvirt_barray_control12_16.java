package jijg.bit12_16.structs12_16;

public class jvirt_barray_control12_16
{
    public short[][][] mem_buffer;// //JBLOCKARRAY mem_buffer = new short[rows_in_array][blocksperrow][jpeglib.DCTSIZE2];   /* => the in-memory buffer */
    public int rows_in_array; /* total virtual array height */
    public int blocksperrow;  /* width of array (and of memory buffer) */
    public int maxaccess;     /* max rows accessed by access_virt_barray */
    public int rows_in_mem;   /* height of memory buffer */
    public int rowsperchunk;  /* allocation chunk size in mem_buffer */
    public int cur_start_row; /* first logical row # in the buffer */
    public int first_undef_row;   /* row # of first uninitialized row */
    public boolean pre_zero;     /* pre-zero mode requested? */
    public boolean dirty;        /* do current buffer contents need written? */
    public boolean b_s_open;     /* is backing-store data valid? */
    //public jvirt_barray_control next;    /* link to next virtual barray control block  (handled as an external array of these objects) */
    //backing_store_info b_s_info;  /* System-dependent control info */
}
