package jijg.bit8;

import jijg.bit8.structs8.jpeg_decompress_struct8;
import jijg.bit8.structs8.jpeg_memory_mgr8;
import jijg.bit8.structs8.jvirt_barray_control8;
import jijg.bit8.structs8.jvirt_sarray_control8;

public class jmemmgr8 extends jpeg_memory_mgr8
{

    /*
     * Creation of 2-D sample arrays.
     * The pointers are in near heap, the samples themselves in FAR heap.
     *
     * To minimize allocation overhead and to allow I/O of large contiguous
     * blocks, we allocate the sample rows in groups of as many rows as possible
     * without exceeding MAX_ALLOC_CHUNK total bytes per allocation request.
     * NB: the virtual array control routines, later in this file, know about
     * this chunking of rows.  The rowsperchunk value is left in the mem manager
     * object so that it can be saved away if this sarray is the workspace for
     * a virtual array.
     */
    public byte[][] alloc_sarray(jpeg_decompress_struct8 cinfo, int samplesperow, int numrows)
    /* Allocate a 2-D sample array */
    {
        return new byte[numrows][samplesperow];
    }

    /*
     * Creation of 2-D coefficient-block arrays.
     * This is essentially the same as the code for sample arrays, above.
     */
    /* Allocate a 2-D coefficient-block array */

    @Override
    public short[][] alloc_barray(jpeg_decompress_struct8 cinfo, int blocksperrow, int numrows)
    {
        return new short[numrows][blocksperrow];
    }


    /*
     * Creation of 2-D difference arrays.
     * This is essentially the same as the code for sample arrays, above.
     */
     public int[][] alloc_darray(jpeg_decompress_struct8 cinfo, int diffsperrow, int numrows)
    /* Allocate a 2-D difference array */
    {
        return new int[numrows][diffsperrow];
    }

    public void free_pool(jpeg_decompress_struct8 cinfo)
    {
        // no-op
    }

    public void self_destruct(jpeg_decompress_struct8 cinfo)
    {
        // no-op
    }

    public void alloc_small(jpeg_decompress_struct8 cinfo, int sizeofobject)
    {
        // no-op
        // a bit weird in Java.. can be asking for 1,2 or 3-d arrays of multiple types
        // just new up the objects in the code where this is called
    }

    public void alloc_large(jpeg_decompress_struct8 cinfo, int sizeofobject)
    {
        // no-op
        // a bit weird in Java.. can be asking for 1,2 or 3-d arrays of multiple types
        // just new up the objects in the code where this is called
    }


    public jvirt_sarray_control8 request_virt_sarray(jpeg_decompress_struct8 cinfo, boolean pre_zero, int samplesperrow, int numrows,
                                                     int maxaccess)
    {
        jvirt_sarray_control8 sarray = new jvirt_sarray_control8();
        sarray.pre_zero = pre_zero;
        sarray.rows_in_array = numrows;
        sarray.samplesperrow = samplesperrow;
        sarray.maxaccess = maxaccess;
        cinfo.mem.virt_sarray_list.add(sarray);
        return sarray;
    }

    public jvirt_barray_control8 request_virt_barray(jpeg_decompress_struct8 cinfo, boolean pre_zero, int blocksperrow, int numrows,
                                                     int maxaccess)
    {
        jvirt_barray_control8 barray = new jvirt_barray_control8();
        barray.pre_zero = pre_zero;
        barray.rows_in_array = numrows;
        barray.blocksperrow = blocksperrow;
        barray.maxaccess = maxaccess;
        cinfo.mem.virt_barray_list.add(barray);
        return barray;
    }

    public void realize_virt_arrays(jpeg_decompress_struct8 cinfo)
    {
        for (jvirt_barray_control8 barray : cinfo.mem.virt_barray_list)
        {
            barray.mem_buffer = new short[barray.rows_in_array][barray.blocksperrow][jpeglib8.DCTSIZE2];
        }

        // jvirt_sarray_control memory allocation
        for (jvirt_sarray_control8 sarray : cinfo.mem.virt_sarray_list)
        {
            sarray.mem_buffer = new byte[sarray.rows_in_array][sarray.samplesperrow];
        }
    }

    public byte[][] access_virt_sarray(jpeg_decompress_struct8 cinfo, jvirt_sarray_control8 ptr, int start_row, int num_rows,
                                       boolean writable)
    {
        return null;
    }

    public short[][][] access_virt_barray(jpeg_decompress_struct8 cinfo, jvirt_barray_control8 ptr, int start_row, int num_rows,
                                          boolean writable)
    {
        return ptr.mem_buffer;
    }
}
