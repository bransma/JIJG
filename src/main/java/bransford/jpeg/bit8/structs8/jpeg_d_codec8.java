package bransford.jpeg.bit8.structs8;

import bransford.jpeg.bit8.jdhuff8;
import bransford.jpeg.bit8.jpeglib8;

public abstract class jpeg_d_codec8
{
    public enum DECOMPRESS
    {
        decompress_data, decompress_onepass, decompress_smooth_data
    }

    public enum CONSUME_DATA
    {
        consume_data, dummy_consume_data
    }

    /* Pointer to data which is private to entropy module */
    //void *entropy_private;
    public jdhuff8 entropy_private; // either jdshuff, jdlhuff, jdphuff

    // decompress and consume data methods
    public DECOMPRESS decompress_data = DECOMPRESS.decompress_data;

    public CONSUME_DATA consume_data = CONSUME_DATA.consume_data;

    // jlossy.h and jlossls.h: jdcoefct & jddiffct 
    public abstract void calc_output_dimensions(jpeg_decompress_struct8 cinfo);

    public abstract void start_input_pass(jpeg_decompress_struct8 cinfo);

    public abstract int consume_data(jpeg_decompress_struct8 cinfo);

    public abstract void start_output_pass(jpeg_decompress_struct8 cinfo);

    public abstract int decompress_data(jpeg_decompress_struct8 cinfo, byte[][][] output_buf, int[] buffer_offset);

    // Lossless

    public abstract void scaler_scale (jpeg_decompress_struct8 cinfo, int[] diff_buf, byte[] output_buf, int width);

    public abstract void diff_start_input_pass (jpeg_decompress_struct8 cinfo);

    public abstract void entropy_start_pass (jpeg_decompress_struct8 cinfo);

    public abstract boolean entropy_process_restart (jpeg_decompress_struct8 cinfo);

    public abstract int entropy_decode_mcus (jpeg_decompress_struct8 cinfo,
                                             int[][][] diff_buf,
                                             int MCU_row_num,
                                             int MCU_col_num,
                                             int nMCU);

    public abstract void  predict_start_pass (jpeg_decompress_struct8 cinfo);

    public abstract void predict_process_restart (jpeg_decompress_struct8 cinfo);

    public abstract void predict_undifference(jpeg_decompress_struct8 cinfo, int comp_index,
                                              int[] diff_buf, int[] prev_row,
                                              int[] undiff_buf, int width);

    /*
     * Dummy consume-input routine for single-pass operation.
     */
    public int dummy_consume_data(jpeg_decompress_struct8 cinfo)
    {
        return jpeglib8.JPEG_SUSPENDED; /* Always indicate nothing was done */
    }
}
