package jijg.bit12_16;

import jijg.bit12_16.error12_16.ErrorStrings12_16;
import jijg.bit12_16.structs12_16.jpeg_d_post_controller12_16;
import jijg.bit12_16.structs12_16.jpeg_decompress_struct12_16;

public class jdpostct12_16 extends jpeg_d_post_controller12_16
{

    public void jinit_d_post_controller(jpeg_decompress_struct12_16 cinfo, boolean need_full_buffer)
    {
        whole_image = null; /* flag for no virtual arrays */
        buffer = null; /* flag for no strip buffer */

        start_pass_method = START_PASS_METHODS.START_PASSDPOST;
        /* Create the quantization buffer, if needed */
        if (cinfo.quantize_colors)
        {
            /* The buffer strip height is max_v_samp_factor, which is typically
             * an efficient number of rows for upsampling to return.
             * (In the presence of output rescaling, we might want to be smarter?)
             */
            strip_height = cinfo.max_v_samp_factor;
            if (need_full_buffer)
            {
                /* Two-pass color quantization: need full-image storage. */
                /* We round up the number of rows to a multiple of the strip height. */
                if (jmorecfg12_16.QUANT_2PASS_SUPPORTED)
                {
                    whole_image = cinfo.mem.request_virt_sarray(cinfo, false,
                            cinfo.output_width * cinfo.out_color_components,
                            (int) jutils12_16.jround_up(cinfo.output_height, strip_height), strip_height);
                }
                else
                {
                    cinfo.err.ERREXIT(ErrorStrings12_16.JERR_BAD_BUFFER_MODE);
                } /* QUANT_2PASS_SUPPORTED */
            }
            else
            {
                /* One-pass color quantization: just make a strip buffer. */
                buffer = cinfo.mem.alloc_sarray(cinfo, cinfo.output_width * cinfo.out_color_components, strip_height);
            }
        }
    }

    public void start_pass(jpeg_decompress_struct12_16 cinfo, jpegint12_16.J_BUF_MODE pass_mode)
    {
        if (start_pass_method == START_PASS_METHODS.START_PASSDPOST)
        {
            start_pass_dpost(cinfo, pass_mode);
        }
    }

    public void post_process_data(jpeg_decompress_struct12_16 cinfo, short[][][] input_buf, int[] buffer_offset,
                                  int[] in_row_group_ctr, int in_row_groups_avail,
                                  short[][] output_buf, int[] out_row_ctr, int out_rows_avail)
    {
        switch (post_pass_methods)
        {
            case upsample: cinfo.upsample.upsample(cinfo, input_buf, buffer_offset, in_row_group_ctr,
                    in_row_groups_avail, output_buf, out_row_ctr, out_rows_avail);
                break;
            case post_process_1pass: post_process_1pass(cinfo, input_buf, buffer_offset, in_row_group_ctr,
                    in_row_groups_avail, output_buf, out_row_ctr, out_rows_avail);
                break;
            case post_process_prepass: post_process_prepass(cinfo, input_buf, in_row_group_ctr,
                    in_row_groups_avail, output_buf, out_row_ctr, out_rows_avail);
                break;
            case post_process_2pass: post_process_2pass(cinfo, input_buf, in_row_group_ctr,
                    in_row_groups_avail, output_buf, out_row_ctr, out_rows_avail);
                break;
        }
    }

    public void start_pass_dpost(jpeg_decompress_struct12_16 cinfo, jpegint12_16.J_BUF_MODE pass_mode)
    {
        switch (pass_mode)
        {
            case JBUF_PASS_THRU:
                if (cinfo.quantize_colors)
                {
                    /* Single-pass processing with color quantization. */
                    post_pass_methods = POST_PASS_METHODS.post_process_1pass;
                    /* We could be doing buffered-image output before starting a 2-pass
                     * color quantization; in that case, jinit_d_post_controller did not
                     * allocate a strip buffer.  Use the virtual-array buffer as workspace.
                     */
                }
                else
                {
                    /* For single-pass processing without color quantization,
                     * I have no work to do; just call the upsampler directly.
                     */
                    post_pass_methods = POST_PASS_METHODS.upsample;
                }
                break;

            case JBUF_SAVE_AND_PASS:
                if (jmorecfg12_16.QUANT_2PASS_SUPPORTED)
                {
                    if (whole_image == null)
                    {
                        cinfo.err.ERREXIT(ErrorStrings12_16.JERR_BAD_BUFFER_MODE);
                    }
                     post_pass_methods = POST_PASS_METHODS.post_process_prepass;
                }
                break;
            case JBUF_CRANK_DEST:
                if (jmorecfg12_16.QUANT_2PASS_SUPPORTED)
                {
                    if (whole_image == null)
                    {
                        cinfo.err.ERREXIT(ErrorStrings12_16.JERR_BAD_BUFFER_MODE);
                    }
                     post_pass_methods = POST_PASS_METHODS.post_process_2pass;
                }
                break;

            default:
                cinfo.err.ERREXIT(ErrorStrings12_16.JERR_BAD_BUFFER_MODE);
                break;
        }

        starting_row = next_row = 0;
    }

    public void post_process_1pass(jpeg_decompress_struct12_16 cinfo, short[][][] input_buf, int[] buffer_offset,
                                   int[] in_row_group_ctr,
                                   int in_row_groups_avail, short[][] outputbuf, int[] out_row_counter,
                                   int out_rows_avail)
    {
        int num_rows, max_rows;

        /* Fill the buffer, but not more than what we can dump out in one go. */
        /* Note we rely on the upsampler to detect bottom of image. */
        max_rows = out_rows_avail - out_row_counter[0];
        if (max_rows > strip_height)
            max_rows = strip_height;
        num_rows = 0;
        cinfo.upsample.upsample(cinfo,
                                      input_buf, buffer_offset, in_row_group_ctr, in_row_groups_avail,
                                      buffer, out_row_counter, max_rows);
        /* Quantize and emit data. */
        cinfo.cquantize.color_quantize(cinfo, buffer, outputbuf, num_rows);
        out_row_counter[0] += out_row_counter[0] + num_rows;
    }

    public void post_process_prepass(jpeg_decompress_struct12_16 cinfo, short[][][] input_buf, int[] in_row_group_ctr,
                                     int in_row_groups_avail, short[][] outputbuf, int[] out_row_counter,
                                     int out_rows_avail)
    {

    }

    public void post_process_2pass(jpeg_decompress_struct12_16 cinfo, short[][][] input_buf, int[] in_row_group_ctr,
                                   int in_row_groups_avail, short[][] outputbuf, int[] out_row_counter,
                                   int out_rows_avail)
    {

    }

}
