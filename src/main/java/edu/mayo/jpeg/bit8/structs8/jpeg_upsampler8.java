package edu.mayo.jpeg.bit8.structs8;

import edu.mayo.jpeg.bit8.jpeglib8;

public abstract class jpeg_upsampler8
{
    /* Pointer to routine to do actual upsampling/conversion of one row group */
    public abstract void start_pass(jpeg_decompress_struct8 cinfo);

    public abstract void upsample(jpeg_decompress_struct8 cinfo, byte[][][] input_buf, int[] input_buf_offset,
                                  int[] in_row_group_ctr, int in_row_groups_avail,
                                  byte[][] outputbuf, int[] out_row_counter, int out_rows_avail);

    //    JMETHOD(void, upsample, (j_decompress_ptr cinfo,
    //  			   JSAMPIMAGE input_buf,
    //  			   JDIMENSION *in_row_group_ctr,
    //  			   JDIMENSION in_row_groups_avail,
    //  			   JSAMPARRAY output_buf,
    //  			   JDIMENSION *out_row_ctr,
    //  			   JDIMENSION out_rows_avail));

    /* Pointer to routine to do actual upsampling/conversion of one row group */
    //    JMETHOD(void, upmethod, (j_decompress_ptr cinfo,
    //                 JSAMPIMAGE input_buf, JDIMENSION in_row_group_ctr,
    //                 JSAMPARRAY output_buf));
    public abstract void upmethod(jpeg_decompress_struct8 cinfo, byte[][][] input_buf, int[] in_row_group_ctr,
                                  int in_row_groups_avail, byte[][] outputbuf, int[] out_row_counter,
                                  int out_rows_avail); // no-op in jdsample

    public abstract void jinit_upsampler(jpeg_decompress_struct8 cinfo);

    /* Per-component upsampling method pointers */
    public enum UPSAMPLE_METHODS
    {
        fullsize_upsample, noop_upsample, int_upsample, h2v1_upsample, h2v2_upsample, h2v1_fancy_upsample,
        h2v2_fancy_upsample, merged_1v_upsample, merged_2v_upsample
    }

    public enum UPMETHODS
    {
        h2v1_merged_upsample, h2v2_merged_upsample
    }

    public UPSAMPLE_METHODS[] methods = new UPSAMPLE_METHODS[jpeglib8.MAX_COMPONENTS];
    public UPSAMPLE_METHODS upsample;
    public UPMETHODS upmethod;

    // jdsample
    // start_pass
    // start_pass_upsample

    // sep_usample, which I'll simply call upsample, will iterate over the methods:
    // fullsize_upsample
    // noop_upsample
    // int_upsample
    // h2v1_upsample
    // h2v2_upsample
    // h2v1_fancy_upsample
    // h2v2_fancy_upsample

    //jdmerge
    // convienence method
    // build_ycc_rgb_table
    //
    // start_pass
    // start_pass_merged_upsample
    //
    // upsample
    // merged_2v_upsample
    // merged_1v_upsample

    // upmethod
    // h2v1_merged_upsample
    // h2v2_merged_upsample

    // In order for object oriented love, combining the fields from jdmerge and jdsample

    //*************************
    // fields from jdsample.c
    //*************************

    /* Color conversion buffer.  When using separate upsampling and color
     * conversion steps, this buffer holds one upsampled row group until it
     * has been color converted and output.
     * Note: we do not allocate any storage for component(s) which are full-size,
     * ie do not need rescaling.  The corresponding entry of color_buf[] is
     * simply set to point to the input data array, thereby avoiding copying.
     */
    public byte[][][] color_buf = new byte[jpeglib8.MAX_COMPONENTS][][];// JSAMPARRAY color_buf[MAX_COMPONENTS];
    public int[] color_buf_offset = new int[jpeglib8.MAX_COMPONENTS];

    public int next_row_out; /* counts rows emitted from color_buf */
    public int rows_to_go; /* counts rows remaining in image */

    /* Height of an input row group for each component. */
    public int[] rowgroup_height = new int[jpeglib8.MAX_COMPONENTS];

    /* These arrays save pixel expansion factors so that int_expand need not
     * recompute them each time.  They are unused for other upsampling methods.
     */
    public short[] h_expand = new short[jpeglib8.MAX_COMPONENTS];
    public short[] v_expand = new short[jpeglib8.MAX_COMPONENTS];

    //*************************
    // fields from jdmerge.c
    //*************************
    /* Private state for YCC->RGB conversion */
    public int[] Cr_r_tab; /* => table for Cr to R conversion */
    public int[] Cb_b_tab; /* => table for Cb to B conversion */
    public long[] Cr_g_tab; /* => table for Cr to G conversion */
    public long[] Cb_g_tab; /* => table for Cb to G conversion */

    /* For 2:1 vertical sampling, we produce two output rows at a time.
     * We need a "spare" row buffer to hold the second output row if the
     * application provides just a one-row buffer; we also use the spare
     * to discard the dummy last row if the image height is odd.
     */
    public byte[] spare_row;
    public boolean spare_full; /* T if spare buffer is occupied */
    public boolean need_context_rows; /* TRUE if need rows above & below */
    public int out_row_width; /* samples per output row */
}
