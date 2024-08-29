package jijg.bit12_16;

import jijg.bit12_16.structs12_16.jpeg_decompress_struct12_16;
import jijg.bit12_16.structs12_16.jpeg_memory_mgr12_16;
import jijg.bit12_16.structs12_16.jvirt_barray_control12_16;
import jijg.bit12_16.structs12_16.jvirt_sarray_control12_16;

public class jmemmgr12_16 extends jpeg_memory_mgr12_16
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
    public short[][] alloc_sarray(jpeg_decompress_struct12_16 cinfo, int samplesperow, int numrows)
    /* Allocate a 2-D sample array */
    {
        return new short[numrows][samplesperow];
    }

    /*
     * Creation of 2-D coefficient-block arrays.
     * This is essentially the same as the code for sample arrays, above.
     */
    /* Allocate a 2-D coefficient-block array */

    @Override
    public short[][] alloc_barray(jpeg_decompress_struct12_16 cinfo, int blocksperrow, int numrows)
    {
        return new short[numrows][blocksperrow];
    }


    /*
     * Creation of 2-D difference arrays.
     * This is essentially the same as the code for sample arrays, above.
     */
     public int[][] alloc_darray(jpeg_decompress_struct12_16 cinfo, int diffsperrow, int numrows)
    /* Allocate a 2-D difference array */
    {
        return new int[numrows][diffsperrow];
    }

    public void free_pool(jpeg_decompress_struct12_16 cinfo)
    {
        // no-op
    }

    public void self_destruct(jpeg_decompress_struct12_16 cinfo)
    {
        // no-op
    }

    public void alloc_small(jpeg_decompress_struct12_16 cinfo, int sizeofobject)
    {
        // no-op
        // a bit weird in Java.. can be asking for 1,2 or 3-d arrays of multiple types
        // just new up the objects in the code where this is called
    }

    public void alloc_large(jpeg_decompress_struct12_16 cinfo, int sizeofobject)
    {
        // no-op
        // a bit weird in Java.. can be asking for 1,2 or 3-d arrays of multiple types
        // just new up the objects in the code where this is called
    }


    public jvirt_sarray_control12_16 request_virt_sarray(jpeg_decompress_struct12_16 cinfo, boolean pre_zero, int samplesperrow, int numrows,
                                                         int maxaccess)
    {
        jvirt_sarray_control12_16 sarray = new jvirt_sarray_control12_16();
        sarray.pre_zero = pre_zero;
        sarray.rows_in_array = numrows;
        sarray.samplesperrow = samplesperrow;
        sarray.maxaccess = maxaccess;
        cinfo.mem.virt_sarray_list.add(sarray);
        return sarray;
    }

    public jvirt_barray_control12_16 request_virt_barray(jpeg_decompress_struct12_16 cinfo, boolean pre_zero, int blocksperrow, int numrows,
                                                         int maxaccess)
    {
        jvirt_barray_control12_16 barray = new jvirt_barray_control12_16();
        barray.pre_zero = pre_zero;
        barray.rows_in_array = numrows;
        barray.blocksperrow = blocksperrow;
        barray.maxaccess = maxaccess;
        cinfo.mem.virt_barray_list.add(barray);
        return barray;
    }

    public void realize_virt_arrays(jpeg_decompress_struct12_16 cinfo)
    {
        for (jvirt_barray_control12_16 barray : cinfo.mem.virt_barray_list)
        {
            barray.mem_buffer = new short[barray.rows_in_array][barray.blocksperrow][jpeglib12_16.DCTSIZE2];
        }

        // jvirt_sarray_control memory allocation
        for (jvirt_sarray_control12_16 sarray : cinfo.mem.virt_sarray_list)
        {
            sarray.mem_buffer = new short[sarray.rows_in_array][sarray.samplesperrow];
        }
    }

    public short[][] access_virt_sarray(jpeg_decompress_struct12_16 cinfo, jvirt_sarray_control12_16 ptr, int start_row, int num_rows,
                                        boolean writable)
    {
        return null;
    }

    public short[][][] access_virt_barray(jpeg_decompress_struct12_16 cinfo, jvirt_barray_control12_16 ptr, int start_row, int num_rows,
                                          boolean writable)
    {
        return ptr.mem_buffer;
    }
}
