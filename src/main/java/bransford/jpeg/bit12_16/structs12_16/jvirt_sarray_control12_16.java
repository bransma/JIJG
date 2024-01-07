package bransford.jpeg.bit12_16.structs12_16;


public class jvirt_sarray_control12_16
{
    public short[][] mem_buffer;//JSAMPARRAY mem_buffer = new byte[rows_in_array][samplesperrow];  /* => the in-memory buffer */
    public int rows_in_array; /* total virtual array height */
    public int samplesperrow; /* width of array (and of memory buffer) */
    public int maxaccess;     /* max rows accessed by access_virt_sarray */
    public int rows_in_mem;   /* height of memory buffer */
    public int rowsperchunk;  /* allocation chunk size in mem_buffer */
    public int cur_start_row; /* first logical row # in the buffer */
    public int first_undef_row;   /* row # of first uninitialized row */
    public boolean pre_zero;     /* pre-zero mode requested? */
    public boolean dirty;        /* do current buffer contents need written? */
    public boolean b_s_open;     /* is backing-store data valid? */
    //public jvirt_sarray_control next;    /* link to next virtual sarray control block (handled as an external array of these objects) */
    //backing_store_info b_s_info;  /* System-dependent control info */
}
