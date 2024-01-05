package edu.mayo.jpeg.bit12_16;

import edu.mayo.jpeg.bit12_16.error12_16.ErrorStrings12_16;
import edu.mayo.jpeg.bit12_16.structs12_16.*;

/*
 * jddiffct.c
 *
 * Copyright (C) 1994-1998, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains the [un]difference buffer controller for decompression.
 * This controller is the top level of the lossless JPEG decompressor proper.
 * The difference buffer lies between the entropy decoding and
 * prediction/undifferencing steps.  The undifference buffer lies between the
 * prediction/undifferencing and scaling steps.
 *
 * In buffered-image mode, this controller is the interface between
 * input-oriented processing and output-oriented processing.
 */

public class jddiffct12_16 extends d_coef_controller12_16
{
    int MCU_ctr;       /* counts MCUs processed in current row */
    int restart_rows_to_go;  /* MCU-rows left in this restart interval */
    int MCU_vert_offset;     /* counts MCU rows within iMCU row */
    int MCU_rows_per_iMCU_row;   /* number of such rows needed */

    /* The output side's location is represented by cinfo.output_iMCU_row. */

    int[][][] diff_buf = new int[jmorecfg12_16.MAX_COMPONENTS][][];  /* iMCU row of differences */
    int[][][] undiff_buf = new int[jmorecfg12_16.MAX_COMPONENTS][][]; /* iMCU row of undiff'd samples */

    /* In multi-pass modes, we need a virtual sample array for each component. */
    jvirt_sarray_control12_16[] whole_image = new jvirt_sarray_control12_16[jmorecfg12_16.MAX_COMPONENTS];

    public void start_iMCU_row(jpeg_decompress_struct12_16 cinfo)
        /* Reset within-iMCU-row counters for a new row (input side) */
    {
        jpeg_d_codec12_16 losslsd = cinfo.codec;

        /* In an interleaved scan, an MCU row is the same as an iMCU row.
         * In a noninterleaved scan, an iMCU row has v_samp_factor MCU rows.
         * But at the bottom of the image, process only what's left.
         */
        if (cinfo.comps_in_scan > 1)
        {
            MCU_rows_per_iMCU_row = 1;
        }
        else
        {
            if (cinfo.input_iMCU_row < (cinfo.total_iMCU_rows - 1))
                MCU_rows_per_iMCU_row = cinfo.cur_comp_info[0].v_samp_factor;
            else
                MCU_rows_per_iMCU_row = cinfo.cur_comp_info[0].last_row_height;
        }

        MCU_ctr = 0;
        MCU_vert_offset = 0;
    }


    /*
     * Initialize for an input processing pass.
     */
    public void start_input_pass(jpeg_decompress_struct12_16 cinfo)
    {
        /* Check that the restart interval is an integer multiple of the number
         * of MCU in an MCU-row.
         */
        if (cinfo.restart_interval % cinfo.MCUs_per_row != 0)
            cinfo.err.ERREXIT2(ErrorStrings12_16.JERR_BAD_RESTART, cinfo.restart_interval, cinfo.MCUs_per_row);

        /* Initialize restart counter */
        restart_rows_to_go = cinfo.restart_interval / cinfo.MCUs_per_row;

        cinfo.input_iMCU_row = 0;
        start_iMCU_row(cinfo);
    }

    /*
     * Check for a restart marker & resynchronize decoder, undifferencer.
     * Returns FALSE if must suspend.
     */

    public boolean process_restart(jpeg_decompress_struct12_16 cinfo)
    {
        jpeg_d_codec12_16 losslsd = cinfo.codec;

        if (!(losslsd.entropy_process_restart(cinfo)))
            return false;

        losslsd.predict_process_restart(cinfo);

        /* Reset restart counter */
        restart_rows_to_go = cinfo.restart_interval / cinfo.MCUs_per_row;

        return true;
    }


    /*
     * Initialize for an output processing pass.
     */
    public void start_output_pass(jpeg_decompress_struct12_16 cinfo)
    {
        cinfo.output_iMCU_row = 0;
    }


    /*
     * Decompress and return some data in the supplied buffer.
     * Always attempts to emit one fully interleaved MCU row ("iMCU" row).
     * Input and output must run in lockstep since we have only a one-MCU buffer.
     * Return value is JPEG_ROW_COMPLETED, JPEG_SCAN_COMPLETED, or JPEG_SUSPENDED.
     *
     * NB: output_buf contains a plane for each component in image,
     * which we index according to the component's SOF position.
     */
    // public int decompress_data(jpeg_decompress_struct cinfo, byte[][][] output_buf, int[] buffer_offset)
    public int decompress_data(jpeg_decompress_struct12_16 cinfo, short[][][] output_buf, int[] buffer_offset)
    {
        jpeg_d_codec12_16 losslsd = cinfo.codec;
        int MCU_col_num;   /* index of current MCU within row */
        int MCU_count;     /* number of MCUs decoded */
        int last_iMCU_row = cinfo.total_iMCU_rows - 1;
        int comp, ci, row, prev_row;
        int yoffset;
        jpeg_component_info12_16 compptr;

        /* Loop to process as much as one whole iMCU row */
        for (yoffset = MCU_vert_offset; yoffset < MCU_rows_per_iMCU_row;
             yoffset++)
        {

            /* Process restart marker if needed; may have to suspend */
            if (cinfo.restart_interval == 1)
            {
                if (restart_rows_to_go == 0)
                    if (!process_restart(cinfo))
                        return jpeglib12_16.JPEG_SUSPENDED;
            }

            MCU_col_num = MCU_ctr;
            /* Try to fetch an MCU-row (or remaining portion of suspended MCU-row). */
            MCU_count =
                    losslsd.entropy_decode_mcus(cinfo,
                            diff_buf, yoffset, MCU_col_num,
                            cinfo.MCUs_per_row - MCU_col_num);
            if (MCU_count != cinfo.MCUs_per_row - MCU_col_num)
            {
                /* Suspension forced; update state counters and exit */
                MCU_vert_offset = yoffset;
                MCU_ctr += MCU_count;
                return jpeglib12_16.JPEG_SUSPENDED;
            }

            /* Account for restart interval (no-op if not using restarts) */
            restart_rows_to_go--;

            /* Completed an MCU row, but perhaps not an iMCU row */
            MCU_ctr = 0;
        }

        /*
         * Undifference and scale each scanline of the disassembled MCU-row
         * separately.  We do not process dummy samples at the end of a scanline
         * or dummy rows at the end of the image.
         */
        for (comp = 0; comp < cinfo.comps_in_scan; comp++)
        {
            compptr = cinfo.cur_comp_info[comp];
            ci = compptr.component_index;
            for (row = 0, prev_row = compptr.v_samp_factor - 1;
                 row < (cinfo.input_iMCU_row == last_iMCU_row ?
                         compptr.last_row_height : compptr.v_samp_factor);
                 prev_row = row, row++)
            {
                losslsd.predict_undifference (cinfo, ci,
                    diff_buf[ci][row],
                    undiff_buf[ci][prev_row],
                    undiff_buf[ci][row],
                    compptr.width_in_data_units);
                losslsd.scaler_scale(cinfo, undiff_buf[ci][row],
                        output_buf[ci][row],
                        compptr.width_in_data_units);
            }
        }

        /* Completed the iMCU row, advance counters for next one.
         *
         * NB: output_data will increment output_iMCU_row.
         * This counter is not needed for the single-pass case
         * or the input side of the multi-pass case.
         */
        if (++(cinfo.input_iMCU_row) < cinfo.total_iMCU_rows)
        {
            start_iMCU_row(cinfo);
            return jpeglib12_16.JPEG_ROW_COMPLETED;
        }
        /* Completed the scan */
        cinfo.inputctl.finish_input_pass(cinfo);
        return jpeglib12_16.JPEG_SCAN_COMPLETED;
    }

    /*
     * Consume input data and store it in the full-image sample buffer.
     * We read as much as one fully interleaved MCU row ("iMCU" row) per call,
     * ie, v_samp_factor rows for each component in the scan.
     * Return value is JPEG_ROW_COMPLETED, JPEG_SCAN_COMPLETED, or JPEG_SUSPENDED.
     */

    public int consume_data(jpeg_decompress_struct12_16 cinfo)
    {
        /* JDIMENSION MCU_col_num; */ /* index of current MCU within row */
        /* JDIMENSION MCU_count; */   /* number of MCUs decoded */
        /* JDIMENSION last_iMCU_row = cinfo.total_iMCU_rows - 1; */
        int comp, ci /* , yoffset, row, prev_row */;
        // byte[][][] buffer = new byte[jpeglib.MAX_COMPS_IN_SCAN][][];
        short[][][] buffer = new short[jpeglib12_16.MAX_COMPS_IN_SCAN][][];
        int[] buffer_offset = new int[buffer.length];
        jpeg_component_info12_16 compptr;

        /* Align the virtual buffers for the components used in this scan. */
        for (comp = 0; comp < cinfo.comps_in_scan; comp++)
        {
            compptr = cinfo.cur_comp_info[comp];
            ci = compptr.component_index;
            buffer[ci] = cinfo.mem.access_virt_sarray
            (cinfo, whole_image[ci],
                    cinfo.input_iMCU_row * compptr.v_samp_factor,
                    compptr.v_samp_factor, true);
        }

        return decompress_data(cinfo, buffer, buffer_offset);
    }


    /*
     * Output some data from the full-image buffer sample in the multi-pass case.
     * Always attempts to emit one fully interleaved MCU row ("iMCU" row).
     * Return value is JPEG_ROW_COMPLETED, JPEG_SCAN_COMPLETED, or JPEG_SUSPENDED.
     *
     * NB: output_buf contains a plane for each component in image.
     */
    public int output_data(jpeg_decompress_struct12_16 cinfo, short[][][] output_buf, int[] buffer_offset)
    // public int output_data(jpeg_decompress_struct cinfo, byte[][][] output_buf, int[] buffer_offset)
    {
        int last_iMCU_row = cinfo.total_iMCU_rows - 1;
        int ci, samp_rows, row;
        short[][] buffer;
        jpeg_component_info12_16 compptr = cinfo.comp_info[0];
        int compptr_offset = 0;

        /* Force some input to be done if we are getting ahead of the input. */
        while (cinfo.input_scan_number < cinfo.output_scan_number ||
                (cinfo.input_scan_number == cinfo.output_scan_number &&
                        cinfo.input_iMCU_row <= cinfo.output_iMCU_row))
        {
            if (cinfo.inputctl.consume_input(cinfo) == jpeglib12_16.JPEG_SUSPENDED)
                return jpeglib12_16.JPEG_SUSPENDED;
        }

        /* OK, output from the virtual arrays. */
        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            /* Align the virtual buffer for this component. */
            buffer = cinfo.mem.access_virt_sarray
            (cinfo, whole_image[ci],
                    cinfo.output_iMCU_row * compptr.v_samp_factor,
                    compptr.v_samp_factor, false);

            if (cinfo.output_iMCU_row < last_iMCU_row)
                samp_rows = compptr.v_samp_factor;
            else
            {
                /* NB: can't use last_row_height here; it is input-side-dependent! */
                samp_rows = (int) compptr.height_in_data_units % compptr.v_samp_factor;
                if (samp_rows == 0) samp_rows = compptr.v_samp_factor;
            }
        }

        if (++(cinfo.output_iMCU_row) < cinfo.total_iMCU_rows)
            return jpeglib12_16.JPEG_ROW_COMPLETED;
        return jpeglib12_16.JPEG_SCAN_COMPLETED;
    }

    /*
     * Initialize difference buffer controller.
     */
    public void jinit_d_diff_controller(jpeg_decompress_struct12_16 cinfo, boolean need_full_buffer)
    {
        int ci;
        jpeg_component_info12_16 compptr = cinfo.comp_info[0];
        int compptr_offset = 0;
        int width, height;

        /* Create the [un]difference buffers. */
        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            width = (int) jutils12_16.jround_up(compptr.width_in_data_units, compptr.h_samp_factor);
            height = compptr.v_samp_factor;
            diff_buf[ci] = new int[height][width];
            undiff_buf[ci] = new int[height][width];
            compptr = cinfo.comp_info[compptr_offset++];
        }

        if (need_full_buffer)
        {
            /* Allocate a full-image virtual array for each component. */
            int access_rows;
            compptr_offset = 0;
            compptr = cinfo.comp_info[compptr_offset];

            for (ci = 0; ci < cinfo.num_components; ci++)
            {
                width = (int) jutils12_16.jround_up(compptr.width_in_data_units, compptr.h_samp_factor);
                height = (int) jutils12_16.jround_up(compptr.height_in_data_units, compptr.v_samp_factor);
                access_rows = compptr.v_samp_factor;
                whole_image[ci] = cinfo.mem.request_virt_sarray(cinfo, false, width, height, access_rows);
            }
        }
        else
        {
            whole_image[0] = null; /* flag for no virtual arrays */
        }
    }
}
