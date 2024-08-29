package jijg.bit12_16;

import jijg.bit12_16.structs12_16.jpeg_d_codec12_16;
import jijg.bit12_16.structs12_16.jpeg_decompress_struct12_16;

/*
 * jdlossls.c
 *
 * Copyright (C) 1998, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains the control logic for the lossless JPEG decompressor.
 */

public class jdlossls12_16 extends jpeg_d_codec12_16
{
    public jddiffct12_16 diff = new jddiffct12_16();

    /* Huffman entropy module */
    jdlhuff12_16 entropy_private = new jdlhuff12_16();

    jdscaler12_16 scaler_private = new jdscaler12_16();

    jdpred12_16 pred = new jdpred12_16();

    /* It is useful to allow each component to have a separate undiff method. */
    public jdpred12_16.predict_undifference[] predict_undifference =
            new jdpred12_16.predict_undifference[jmorecfg12_16.MAX_COMPONENTS];

    public void scaler_scale(jpeg_decompress_struct12_16 cinfo, int[] diff_buf, short[] output_buf, int width)
    {
        switch(scaler_private.scale_method)
        {
            case noscale:
                scaler_private.noscale(cinfo, diff_buf, output_buf, width);
                break;
            case simple_downscale:
                scaler_private.simple_downscale(cinfo, diff_buf, output_buf, width);
                break;
            case simple_upscale:
                scaler_private.simple_upscale(cinfo, diff_buf, output_buf, width);
                break;
        }
    }

    /*
     * Compute output image dimensions and related values.
     */
    public void diff_start_input_pass(jpeg_decompress_struct12_16 cinfo)
    {

    }

    /* Entropy decoding */
    public void entropy_start_pass(jpeg_decompress_struct12_16 cinfo)
    {
        entropy_private.entropy_start_pass(cinfo);
    }

    public boolean entropy_process_restart(jpeg_decompress_struct12_16 cinfo)
    {
        return true;
    }

    public int entropy_decode_mcus(jpeg_decompress_struct12_16 cinfo,
                                   int[][][] diff_buf,
                                   int MCU_row_num,
                                   int MCU_col_num,
                                   int nMCU)
    {
        return entropy_private.decode_mcus(cinfo, diff_buf, MCU_row_num, MCU_col_num, nMCU);
    }

    @Override
    public void predict_start_pass(jpeg_decompress_struct12_16 cinfo)
    {

    }

    @Override
    public void predict_process_restart(jpeg_decompress_struct12_16 cinfo)
    {

    }

    public void predict_undifference(jpeg_decompress_struct12_16 cinfo, int comp_index,
                                     int[] diff_buf, int[] prev_row,
                                     int[] undiff_buf, int width)
    {
        jdpred12_16.predict_undifference predict_method = predict_undifference[comp_index];
        switch (predict_method)
        {
            case jpeg_undifference_first_row:
                pred.jpeg_undifference_first_row(cinfo, comp_index, diff_buf, prev_row, undiff_buf, width);
                break;
            case jpeg_undifference1:
                pred.jpeg_undifference1(cinfo, comp_index, diff_buf, prev_row, undiff_buf, width);
                break;
            case jpeg_undifference2:
                pred.jpeg_undifference2(cinfo, comp_index, diff_buf, prev_row, undiff_buf, width);
                break;
            case jpeg_undifference3:
                pred.jpeg_undifference3(cinfo, comp_index, diff_buf, prev_row, undiff_buf, width);
                break;
            case jpeg_undifference4:
                pred.jpeg_undifference4(cinfo, comp_index, diff_buf, prev_row, undiff_buf, width);
                break;
            case jpeg_undifference5:
                pred.jpeg_undifference5(cinfo, comp_index, diff_buf, prev_row, undiff_buf, width);
                break;
            case jpeg_undifference6:
                pred.jpeg_undifference6(cinfo, comp_index, diff_buf, prev_row, undiff_buf, width);
                break;
            case jpeg_undifference7:
                pred.jpeg_undifference7(cinfo, comp_index, diff_buf, prev_row, undiff_buf, width);
                break;

        }
    }

    @Override
    public void calc_output_dimensions(jpeg_decompress_struct12_16 cinfo)
    {
        /* Hardwire it to "no scaling" */
        cinfo.output_width = cinfo.image_width;
        cinfo.output_height = cinfo.image_height;
        /* jdinput.c has already initialized codec_data_unit to 1,
         * and has computed unscaled downsampled_width and downsampled_height.
         */
    }

    /*
     * Initialize for an input processing pass.
     */

    @Override
    public void start_input_pass(jpeg_decompress_struct12_16 cinfo)
    {
        // passthrough to diffct
        entropy_start_pass(cinfo);
        pred.predict_start_pass(cinfo);
        scaler_private.scaler_start_pass(cinfo);
        diff.start_input_pass(cinfo);
    }

    /*
     * Initialize for an output processing pass.
     */

    @Override
    public void start_output_pass(jpeg_decompress_struct12_16 cinfo)
    {
        // passthrough to diffct
        diff.start_output_pass(cinfo);
    }

    /*
     * Initialize the lossless decompression codec.
     * This is called only once, during master selection.
     */

    public void jinit_lossless_d_codec(jpeg_decompress_struct12_16 cinfo)
    {
        boolean use_c_buffer = cinfo.inputctl.has_multiple_scans || cinfo.buffered_image;
        diff.jinit_d_diff_controller(cinfo, use_c_buffer);
    }

    @Override
    public int consume_data(jpeg_decompress_struct12_16 cinfo)
    {
        // passthrough to diffct (not in the C-code jlossls.c)
        return diff.consume_data(cinfo);
    }

    @Override
    public int decompress_data(jpeg_decompress_struct12_16 cinfo, short[][][] output_buf, int[] buffer_offset)
    {
        return diff.decompress_data(cinfo, output_buf, buffer_offset);
    }
}
