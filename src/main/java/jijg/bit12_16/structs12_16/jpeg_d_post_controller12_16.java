package jijg.bit12_16.structs12_16;

import jijg.bit12_16.jpegint12_16;

public abstract class jpeg_d_post_controller12_16
{
    /* Color quantization source buffer: this holds output data from
     * the upsample/color conversion step to be passed to the quantizer.
     * For two-pass color quantization, we need a full-image buffer;
     * for one-pass operation, a strip buffer is sufficient.
     */

    // typedef struct jvirt_sarray_control * jvirt_sarray_ptr;
    public jvirt_sarray_control12_16 whole_image; /* virtual array, or NULL if one-pass */

    public short[][] buffer;//JSAMPARRAY buffer;        /* strip buffer, or current strip of virtual */
    public int strip_height;  /* buffer size in rows */
    /* for two-pass mode only: */
    public int starting_row;  /* row # of first row in current strip */
    public int next_row;      /* index of next row to fill/empty in strip */
    public enum START_PASS_METHODS {START_PASSDPOST};
    public START_PASS_METHODS start_pass_method = null;
    public enum POST_PASS_METHODS {post_process_1pass, post_process_prepass, post_process_2pass, upsample};
    public POST_PASS_METHODS post_pass_methods = null;

    public abstract void jinit_d_post_controller(jpeg_decompress_struct12_16 cinfo, boolean need_full_buffer);

    public abstract void start_pass(jpeg_decompress_struct12_16 cinfo, jpegint12_16.J_BUF_MODE pass_mode);

    //  				    JSAMPIMAGE input_buf,
    //  				    JDIMENSION *in_row_group_ctr,
    //  				    JDIMENSION in_row_groups_avail,
    //  				    JSAMPARRAY output_buf,
    //  				    JDIMENSION *out_row_ctr,
    //  				    JDIMENSION out_rows_avail));
    public abstract void post_process_data(jpeg_decompress_struct12_16 cinfo, short[][][] input_buf, int[] input_buffer_offset,
                                           int[] in_row_group_ctr, int in_row_groups_avail,
                                           short[][] output_buf, int[] out_row_ctr, int out_rows_avail);
}
