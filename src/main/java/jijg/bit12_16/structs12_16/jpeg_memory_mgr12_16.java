package jijg.bit12_16.structs12_16;

import java.util.LinkedList;

public abstract class jpeg_memory_mgr12_16
{
    /* Each pool identifier (lifetime class) names a linked list of pools. */
    //small_pool_ptr small_list[JPOOL_NUMPOOLS];
    //large_pool_ptr large_list[JPOOL_NUMPOOLS];

    /* Since we only have one lifetime class of virtual arrays, only one
     * linked list is necessary (for each datatype).  Note that the virtual
     * array control blocks being linked together are actually stored somewhere
     * in the small-pool list.
     */
    public LinkedList<jvirt_sarray_control12_16> virt_sarray_list = new LinkedList<jvirt_sarray_control12_16>();
    public LinkedList<jvirt_barray_control12_16> virt_barray_list = new LinkedList<jvirt_barray_control12_16>();

    /* This counts total space obtained from jpeg_get_small/large */
    public long total_space_allocated;

    /* alloc_sarray and alloc_barray set this value for use by virtual
     * array routines.
     */
    public int last_rowsperchunk; /* from most recent alloc_sarray/barray */

    /* Limit on memory allocation for this JPEG object.  (Note that this is
     * merely advisory, not a guaranteed maximum; it only affects the space
     * used for virtual-array buffers.)  May be changed by outer application
     * after creating the JPEG object.
     */
    public long max_memory_to_use;

    /* Maximum allocation request accepted by alloc_large. */
    public long max_alloc_chunk;

    /* Method pointers and C-memory management that is not relevant in Java, but leave them and no-op*/
    public abstract void alloc_small(jpeg_decompress_struct12_16 cinfo, int sizeofobject);

    public abstract void alloc_large(jpeg_decompress_struct12_16 cinfo, int sizeofobject);

    public abstract short[][] alloc_sarray(jpeg_decompress_struct12_16 cinfo, int samplesperow, int numrows);

    public abstract short[][] alloc_barray(jpeg_decompress_struct12_16 cinfo, int blocksperrow, int numrows);

    public abstract int[][] alloc_darray(jpeg_decompress_struct12_16 cinfo, int diffsperrow, int numrows);

    public abstract jvirt_sarray_control12_16 request_virt_sarray(jpeg_decompress_struct12_16 cinfo, boolean pre_zero, int samplesperrow,
                                                                  int numrows, int maxaccess);

    public abstract jvirt_barray_control12_16 request_virt_barray(jpeg_decompress_struct12_16 cinfo, boolean pre_zero, int blocksperrow,
                                                                  int numrows, int maxaccess);

    public abstract void realize_virt_arrays(jpeg_decompress_struct12_16 cinfo);

    public abstract short[][] access_virt_sarray(jpeg_decompress_struct12_16 cinfo, jvirt_sarray_control12_16 ptr, int start_row, int num_rows,
                                                 boolean writable);

    public abstract short[][][] access_virt_barray(jpeg_decompress_struct12_16 cinfo, jvirt_barray_control12_16 ptr, int start_row,
                                                   int num_rows, boolean writable);

    public abstract void free_pool(jpeg_decompress_struct12_16 cinfo);

    public abstract void self_destruct(jpeg_decompress_struct12_16 cinfo);
}
