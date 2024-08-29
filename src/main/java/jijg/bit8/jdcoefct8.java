package jijg.bit8;

import jijg.bit8.error8.ErrorStrings8;
import jijg.bit8.structs8.*;

import java.util.Arrays;

/*
 * jdcoefct.c
 *
 * Copyright (C) 1994-1998, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains the coefficient buffer controller for decompression.
 * This controller is the top level of the lossy JPEG decompressor proper.
 * The coefficient buffer lies between entropy decoding and inverse-DCT steps.
 *
 * In buffered-image mode, this controller is the interface between
 * input-oriented processing and output-oriented processing.
 * Also, the input side (only) is used when reading a file for transcoding.
 */

public class jdcoefct8 extends d_coef_controller8
{
    /* Natural-order array positions of the first 5 zigzag-order coefficients */
    public static final int Q01_POS = 1;
    public static final int Q10_POS = 8;
    public static final int Q20_POS = 16;
    public static final int Q11_POS = 9;
    public static final int Q02_POS = 2;

    public void start_iMCU_row(jpeg_decompress_struct8 cinfo)
        /* Reset within-iMCU-row counters for a new row (input side) */
    {
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

    public void start_input_pass(jpeg_decompress_struct8 cinfo)
    {
        cinfo.input_iMCU_row = 0;
        start_iMCU_row(cinfo);
    }

    public void start_output_pass(jpeg_decompress_struct8 cinfo)
    {
        jdlossy8 lossyd = (jdlossy8) cinfo.codec;
        if (jmorecfg8.BLOCK_SMOOTHING_SUPPORTED)
        {
            /* If multipass, check to see whether to use block smoothing on this pass */
            if (lossyd.coef_arrays != null)
            {
                if (cinfo.do_block_smoothing && smoothing_ok(cinfo))
                    lossyd.decompress_data = jpeg_d_codec8.DECOMPRESS.decompress_smooth_data;
                else
                    lossyd.decompress_data = jpeg_d_codec8.DECOMPRESS.decompress_data;
            }
        }
        cinfo.output_iMCU_row = 0;
    }

    /*
     * Consume input data and store it in the full-image coefficient buffer.
     * We read as much as one fully interleaved MCU row ("iMCU" row) per call,
     * ie, v_samp_factor block rows for each component in the scan.
     * Return value is JPEG_ROW_COMPLETED, JPEG_SCAN_COMPLETED, or jpeglib.JPEG_SUSPENDED.
     */

    public int consume_data(jpeg_decompress_struct8 cinfo)
    {
        jdlossy8 lossyd = (jdlossy8) cinfo.codec;
        int MCU_col_num; /* index of current MCU within row */
        int blkn, ci, xindex, yindex, yoffset;
        int start_col;
        short[][][][] buffer = new short[jpeglib8.MAX_COMPS_IN_SCAN][][][];
        short[][] buffer_ptr;
        int buffer_ptr_offset;
        jpeg_component_info8 compptr;

        /* Align the virtual buffers for the components used in this scan. */
        for (ci = 0; ci < cinfo.comps_in_scan; ci++)
        {
            compptr = cinfo.cur_comp_info[ci];
            // whole_image[index] = jvirt_barray_control, which wraps a short[][][] (among other things)
            buffer[ci] = cinfo.mem.access_virt_barray(cinfo, whole_image[compptr.component_index],
                    cinfo.input_iMCU_row * compptr.v_samp_factor, compptr.v_samp_factor, true);
            // NOTE: the 3rd value in the access_virt_barray is "start_row", which is an offset into the array of length cinfo.input_iMCU_row * compptr.v_samp_factor
            // hence the additional offset in buffer[i][yindex + yoffset + (cinfo.input_iMCU_row * compptr.v_samp_factor)] below

            /* Note: entropy decoder expects buffer to be zeroed,
             * but this is handled automatically by the memory manager
             * because we requested a pre-zeroed array.
             */
        }

        /* Loop to process one whole iMCU row */

        for (yoffset = MCU_vert_offset; yoffset < MCU_rows_per_iMCU_row; yoffset++)
        {
            for (MCU_col_num = MCU_ctr; MCU_col_num < cinfo.MCUs_per_row; MCU_col_num++)
            {
                /* Construct list of pointers to DCT blocks belonging to this MCU */
                blkn = 0; /* index of current DCT block within MCU */
                for (ci = 0; ci < cinfo.comps_in_scan; ci++)
                {
                    compptr = cinfo.cur_comp_info[ci];
                    start_col = MCU_col_num * compptr.MCU_width;
                    for (yindex = 0; yindex < compptr.MCU_height; yindex++)
                    {
                        buffer_ptr = buffer[ci][yindex + yoffset + (cinfo.input_iMCU_row * compptr.v_samp_factor)];
                        buffer_ptr_offset = start_col;
                        for (xindex = 0; xindex < compptr.MCU_width; xindex++)
                        {
                            MCU_buffer[blkn++] = buffer_ptr[buffer_ptr_offset++];
                        }
                    }
                }

                /* Try to fetch the MCU. */
                // MCU_BUFFER is not filled properly until entropy_decode_mcu is implemented
                if (!lossyd.entropy_decode_mcu(cinfo, MCU_buffer))
                {
                    /* Suspension forced; update state counters and exit */
                    MCU_vert_offset = yoffset;
                    MCU_ctr = MCU_col_num;
                    return jpeglib8.JPEG_SUSPENDED;
                }
            }
            /* Completed an MCU row, but perhaps not an iMCU row */
            MCU_ctr = 0;
        }

        /* Completed the iMCU row, advance counters for next one */
        if (++(cinfo.input_iMCU_row) < cinfo.total_iMCU_rows)
        {
            start_iMCU_row(cinfo);
            return jpeglib8.JPEG_ROW_COMPLETED;
        }
        /* Completed the scan */
        cinfo.inputctl.finish_input_pass(cinfo);
        return jpeglib8.JPEG_SCAN_COMPLETED;
    }

    /*
     * Decompress and return some data in the single-pass case.
     * Always attempts to emit one fully interleaved MCU row ("iMCU" row).
     * Input and output must run in lockstep since we have only a one-MCU buffer.
     * Return value is JPEG_ROW_COMPLETED, JPEG_SCAN_COMPLETED, or jpeglib.JPEG_SUSPENDED.
     *
     * NB: output_buf contains a plane for each component in image,
     * which we index according to the component's SOF position.
     */

    public int decompress_onepass(jpeg_decompress_struct8 cinfo, byte[][][] output_buf, int[] buffer_offset)
    {
        jdlossy8 lossyd = (jdlossy8) cinfo.codec;
        int MCU_col_num; /* index of current MCU within row */
        int last_MCU_col = cinfo.MCUs_per_row - 1;
        int last_iMCU_row = cinfo.total_iMCU_rows - 1;
        int blkn, ci, xindex, yindex, yoffset, useful_width;
        byte[][] output_ptr;
        int start_col, output_col;
        jpeg_component_info8 compptr;

        /* Loop to process as much as one whole iMCU row */
        for (yoffset = MCU_vert_offset; yoffset < MCU_rows_per_iMCU_row; yoffset++)
        {
            for (MCU_col_num = MCU_ctr; MCU_col_num <= last_MCU_col; MCU_col_num++)
            {
                /* Try to fetch an MCU.  Entropy decoder expects buffer to be zeroed. */
                //jzero_far(MCU_buffer[0], cinfo.data_units_in_MCU * SIZEOF(JBLOCK));
                for (int i = 0; i < cinfo.data_units_in_MCU; i++)
                {
                    short[] block = MCU_buffer[i];
                    Arrays.fill(block, (short) 0);
                }

                if (!lossyd.entropy_decode_mcu(cinfo, MCU_buffer))
                {
                    /* Suspension forced; update state counters and exit */
                    MCU_vert_offset = yoffset;
                    MCU_ctr = MCU_col_num;
                    return jpeglib8.JPEG_SUSPENDED;
                }
                /* Determine where data should go in output_buf and do the IDCT thing.
                 * We skip dummy blocks at the right and bottom edges (but blkn gets
                 * incremented past them!).  Note the inner loop relies on having
                 * allocated the MCU_buffer[] blocks sequentially.
                 */
                blkn = 0; /* index of current DCT block within MCU */
                for (ci = 0; ci < cinfo.comps_in_scan; ci++)
                {
                    compptr = cinfo.cur_comp_info[ci];
                    /* Don't bother to IDCT an uninteresting component. */
                    if (!compptr.component_needed)
                    {
                        blkn += compptr.MCU_data_units;
                        continue;
                    }

                    useful_width = (MCU_col_num < last_MCU_col) ? compptr.MCU_width : compptr.last_col_width;

                    output_ptr = output_buf[compptr.component_index];
                    int output_ptr_offset = buffer_offset[compptr.component_index] + yoffset * compptr.codec_data_unit;

                    start_col = MCU_col_num * compptr.MCU_sample_width;

                    for (yindex = 0; yindex < compptr.MCU_height; yindex++)
                    {
                        if (cinfo.input_iMCU_row < last_iMCU_row || yoffset + yindex < compptr.last_row_height)
                        {
                            output_col = start_col;
                            for (xindex = 0; xindex < useful_width; xindex++)
                            {
                                lossyd.dctmgr.inverse_DCT(cinfo, compptr, MCU_buffer[blkn + xindex], output_ptr, output_ptr_offset,
                                        output_col, lossyd.inverse_DCT[ci]);
                                output_col += compptr.codec_data_unit;
                            }
                        }
                        blkn += compptr.MCU_width;
                        output_ptr_offset += compptr.codec_data_unit;
                    }
                }
            }
            /* Completed an MCU row, but perhaps not an iMCU row */
            MCU_ctr = 0;
        }

        /* Completed the iMCU row, advance counters for next one */
        cinfo.output_iMCU_row++;
        if (++(cinfo.input_iMCU_row) < cinfo.total_iMCU_rows)
        {
            start_iMCU_row(cinfo);
            return jpeglib8.JPEG_ROW_COMPLETED;
        }

        /* Completed the scan */
        cinfo.inputctl.finish_input_pass(cinfo);
        return jpeglib8.JPEG_SCAN_COMPLETED;
    }

    /*
     * Decompress and return some data in the multi-pass case.
     * Always attempts to emit one fully interleaved MCU row ("iMCU" row).
     * Return value is JPEG_ROW_COMPLETED, JPEG_SCAN_COMPLETED, or jpeglib.JPEG_SUSPENDED.
     *
     * NB: output_buf contains a plane for each component in image.
     */

    public int decompress_data(jpeg_decompress_struct8 cinfo, byte[][][] output_buf, int[] output_buffer_offset)
    {
        jdlossy8 lossyd = (jdlossy8) cinfo.codec;
        int last_iMCU_row = cinfo.total_iMCU_rows - 1;
        int block_num;
        int ci, block_row, block_rows;
        //	  JBLOCKARRAY buffer;
        short[][][] buffer;
        //	  short[][] buffer_ptr;
        short[][] buffer_ptr;
        //	  JSAMPARRAY output_ptr;
        byte[][] output_ptr;

        int output_col;
        jpeg_component_info8 compptr;
        //idct_controller.inverse_DCT_method_ptr inverse_DCT;

        /* Force some input to be done if we are getting ahead of the input. */
        while (cinfo.input_scan_number < cinfo.output_scan_number
                || (cinfo.input_scan_number == cinfo.output_scan_number && cinfo.input_iMCU_row <= cinfo.output_iMCU_row))
        {
            if (cinfo.inputctl.consume_input(cinfo) == jpeglib8.JPEG_SUSPENDED)
                return jpeglib8.JPEG_SUSPENDED;
        }

        /* OK, output from the virtual arrays. */
        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            compptr = cinfo.comp_info[ci];
            /* Don't bother to IDCT an uninteresting component. */
            if (!compptr.component_needed)
            {
                continue;
            }

            /* Align the virtual buffer for this component. */

            // In my implementation access_virt_barray returns whole_image[ci]
            buffer = cinfo.mem.access_virt_barray(cinfo, whole_image[ci], cinfo.output_iMCU_row * compptr.v_samp_factor,
                    compptr.v_samp_factor, false);
            // the offset is the start_row (3rd parameter)
            int buffer_offset = cinfo.output_iMCU_row * compptr.v_samp_factor;

            /* Count non-dummy DCT block rows in this iMCU row. */
            if (cinfo.output_iMCU_row < last_iMCU_row)
            {
                block_rows = compptr.v_samp_factor;
            }
            else
            {
                /* NB: can't use last_row_height here; it is input-side-dependent! */
                block_rows = compptr.height_in_data_units % compptr.v_samp_factor;
                if (block_rows == 0)
                {
                    block_rows = compptr.v_samp_factor;
                }
            }
            // method pointer to the inverse_DCT method, e.g. jpeg_idct_slow, etc. = lossyd.inverse_DCT[ci];
            output_ptr = output_buf[ci];
            int output_ptr_offset = output_buffer_offset[ci];

            /* Loop over all DCT blocks to be processed. */
            for (block_row = 0; block_row < block_rows; block_row++)
            {
                buffer_ptr = buffer[block_row + buffer_offset];
                int buffer_ptr_offset = 0;
                output_col = 0;
                for (block_num = 0; block_num < compptr.width_in_data_units; block_num++)
                {
                    lossyd.dctmgr.inverse_DCT(cinfo, compptr, buffer_ptr[buffer_ptr_offset], output_ptr, output_ptr_offset,
                            output_col, lossyd.inverse_DCT[ci]);
                    buffer_ptr_offset++;
                    output_col += compptr.codec_data_unit;
                }
                output_ptr_offset += compptr.codec_data_unit;
            }
        }

        if (++(cinfo.output_iMCU_row) < cinfo.total_iMCU_rows)
        {
            return jpeglib8.JPEG_ROW_COMPLETED;
        }

        return jpeglib8.JPEG_SCAN_COMPLETED;
    }

    /*
     * Determine whether block smoothing is applicable and safe.
     * We also latch the current states of the coef_bits[] entries for the
     * AC coefficients; otherwise, if the input side of the decompressor
     * advances into a new scan, we might think the coefficients are known
     * more accurately than they really are.
     */

    public boolean smoothing_ok(jpeg_decompress_struct8 cinfo)
    {
        boolean smoothing_useful = false;
        int ci, coefi;
        jpeg_component_info8 compptr;
        JQUANT_TBL8 qtable;
        int[] coef_bits;

        if (!(cinfo.process == jpeglib8.J_CODEC_PROCESS.JPROC_PROGRESSIVE || cinfo.coef_bits == null))
        {
            return false;
        }

        /* Allocate latch area if not already done */
        if (coef_bits_latch == null)
        {
            coef_bits_latch = new int[cinfo.num_components * SAVED_COEFS];//cinfo.mem.alloc_small(cinfo, cinfo.num_components * (SAVED_COEFS * SIZEOF(int)));
        }
        //coef_bits_latch = this.coef_bits_latch;
        int coef_bits_latch_offset = 0;

        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            compptr = cinfo.comp_info[ci];
            /* All components' quantization values must already be latched. */
            if ((qtable = compptr.quant_table) == null)
            {
                return false;
            }

            /* Verify DC & first 5 AC quantizers are nonzero to avoid zero-divide. */
            if (qtable.quantval[0] == 0 || qtable.quantval[Q01_POS] == 0 || qtable.quantval[Q10_POS] == 0
                    || qtable.quantval[Q20_POS] == 0 || qtable.quantval[Q11_POS] == 0 || qtable.quantval[Q02_POS] == 0)
            {
                return false;
            }

            /* DC values must be at least partly known for all components. */
            coef_bits = cinfo.coef_bits[ci];
            if (coef_bits[0] < 0)
            {
                return false;
            }

            /* Block smoothing is helpful if some AC coefficients remain inaccurate. */
            for (coefi = 1; coefi <= 5; coefi++)
            {
                coef_bits_latch[coefi + coef_bits_latch_offset] = coef_bits[coefi];
                if (coef_bits[coefi] != 0)
                {
                    smoothing_useful = true;
                }
            }
            coef_bits_latch_offset += SAVED_COEFS;
        }

        return smoothing_useful;
    }

    /*
     * Variant of decompress_data for use when doing block smoothing.
     */

    public int decompress_smooth_data(jpeg_decompress_struct8 cinfo, byte[][][] output_buf, int[] output_buffer_offset)
    {
        jdlossy8 lossyd = (jdlossy8) cinfo.codec;
        int last_iMCU_row = cinfo.total_iMCU_rows - 1;
        int block_num, last_block_column;
        int ci, block_row, block_rows, access_rows;
        //	  JBLOCKARRAY buffer;
        short[][][] buffer;
        //	  short[][] buffer_ptr;
        short[][] buffer_ptr, prev_block_row, next_block_row;
        //	  JSAMPARRAY output_ptr;
        byte[][] output_ptr;

        int output_col;
        jpeg_component_info8 compptr;
        boolean first_row, last_row;
        short[] workspace = new short[jpeglib8.DCTSIZE2];
        int[] coef_bits;
        JQUANT_TBL8 quanttbl;
        long Q00, Q01, Q02, Q10, Q11, Q20, num;
        int DC1, DC2, DC3, DC4, DC5, DC6, DC7, DC8, DC9;
        int Al, pred;

        /* Force some input to be done if we are getting ahead of the input. */
        while (cinfo.input_scan_number <= cinfo.output_scan_number && !cinfo.inputctl.eoi_reached)
        {
            if (cinfo.input_scan_number == cinfo.output_scan_number)
            {
                /* If input is working on current scan, we ordinarily want it to
                 * have completed the current row.  But if input scan is DC,
                 * we want it to keep one row ahead so that next block row's DC
                 * values are up to date.
                 */
                int delta = (cinfo.Ss == 0) ? 1 : 0;
                if (cinfo.input_iMCU_row > cinfo.output_iMCU_row + delta)
                {
                    break;
                }
            }
            if (cinfo.inputctl.consume_input(cinfo) == jpeglib8.JPEG_SUSPENDED)
            {
                return jpeglib8.JPEG_SUSPENDED;
            }
        }

        /* OK, output from the virtual arrays. */
        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            compptr = cinfo.comp_info[ci];
            /* Don't bother to IDCT an uninteresting component. */
            if (!compptr.component_needed)
            {
                continue;
            }

            /* Count non-dummy DCT block rows in this iMCU row. */
            if (cinfo.output_iMCU_row < last_iMCU_row)
            {
                block_rows = compptr.v_samp_factor;
                access_rows = block_rows * 2; /* this and next iMCU row */
                last_row = false;
            }
            else
            {
                /* NB: can't use last_row_height here; it is input-side-dependent! */
                block_rows = compptr.height_in_data_units % compptr.v_samp_factor;
                if (block_rows == 0)
                {
                    block_rows = compptr.v_samp_factor;
                }
                access_rows = block_rows; /* this iMCU row only */
                last_row = true;
            }

            /* Align the virtual buffer for this component. */
            int buffer_offset = 0;
            if (cinfo.output_iMCU_row > 0)
            {
                access_rows += compptr.v_samp_factor; /* prior iMCU row too */
                buffer = cinfo.mem.access_virt_barray(cinfo, whole_image[ci],
                        (cinfo.output_iMCU_row - 1) * compptr.v_samp_factor, access_rows, false);
                buffer_offset = (cinfo.output_iMCU_row - 1) * compptr.v_samp_factor;
                buffer_offset += compptr.v_samp_factor; /* point to current iMCU row */
                first_row = false;
            }
            else
            {
                buffer = cinfo.mem.access_virt_barray(cinfo, whole_image[ci], 0, access_rows, false);
                first_row = true;
            }

            /* Fetch component-dependent info */
            coef_bits = coef_bits_latch;
            int coef_bits_offset = ci * SAVED_COEFS;
            quanttbl = compptr.quant_table;
            Q00 = quanttbl.quantval[0];
            Q01 = quanttbl.quantval[Q01_POS];
            Q10 = quanttbl.quantval[Q10_POS];
            Q20 = quanttbl.quantval[Q20_POS];
            Q11 = quanttbl.quantval[Q11_POS];
            Q02 = quanttbl.quantval[Q02_POS];

            output_ptr = output_buf[ci];
            int output_ptr_offset = output_buffer_offset[ci];
            int buffer_ptr_offset = 0, prev_block_row_offset = 0, next_block_row_offset = 0;

            /* Loop over all DCT blocks to be processed. */
            for (block_row = 0; block_row < block_rows; block_row++)
            {
                buffer_ptr = buffer[block_row + buffer_offset];
                if (first_row && block_row == 0)
                {
                    prev_block_row = buffer_ptr;
                    prev_block_row_offset = buffer_ptr_offset;
                }
                else
                {
                    // it's already -1 in the buffer index
                    prev_block_row = buffer[block_row - 1 + buffer_offset];
                    prev_block_row_offset = 0;
                }

                if (last_row && block_row == block_rows - 1)
                {
                    next_block_row = buffer_ptr;
                    next_block_row_offset = buffer_ptr_offset;
                }
                else
                {
                    next_block_row = buffer[block_row + 1 + buffer_offset];
                    next_block_row_offset = 0;
                }

                /* We fetch the surrounding DC values using a sliding-register approach.
                 * Initialize all nine here so as to do the right thing on narrow pics.
                 */
                DC1 = DC2 = DC3 = prev_block_row[prev_block_row_offset][0];
                DC4 = DC5 = DC6 = buffer_ptr[buffer_ptr_offset][0];
                DC7 = DC8 = DC9 = next_block_row[next_block_row_offset][0];
                output_col = 0;
                last_block_column = compptr.width_in_data_units - 1;
                for (block_num = 0; block_num <= last_block_column; block_num++)
                {
                    /* Fetch current DCT block into workspace so we can modify it. */
                    //jcopy_block_row(buffer_ptr, (short[][]) workspace, 1);
                    System.arraycopy(buffer_ptr[buffer_ptr_offset], 0, workspace, 0, workspace.length);
                    /* Update DC values */
                    if (block_num < last_block_column)
                    {
                        DC3 = prev_block_row[1 + prev_block_row_offset][0];
                        DC6 = buffer_ptr[1 + buffer_ptr_offset][0];
                        DC9 = next_block_row[1 + next_block_row_offset][0];
                    }
                    /* Compute coefficient estimates per K.8.
                     * An estimate is applied only if coefficient is still zero,
                     * and is not known to be fully accurate.
                     */
                    /* AC01 */
                    if ((Al = coef_bits[1 + coef_bits_offset]) != 0 && workspace[1] == 0)
                    {
                        num = 36 * Q00 * (DC4 - DC6);
                        if (num >= 0)
                        {
                            pred = (int) (((Q01 << 7) + num) / (Q01 << 8));
                            if (Al > 0 && pred >= (1 << Al))
                            {
                                pred = (1 << Al) - 1;
                            }
                        }
                        else
                        {
                            pred = (int) (((Q01 << 7) - num) / (Q01 << 8));
                            if (Al > 0 && pred >= (1 << Al))
                            {
                                pred = (1 << Al) - 1;
                            }
                            pred = -pred;
                        }
                        workspace[1] = (short) pred;
                    }
                    /* AC10 */
                    if ((Al = coef_bits[2 + coef_bits_offset]) != 0 && workspace[8] == 0)
                    {
                        num = 36 * Q00 * (DC2 - DC8);
                        if (num >= 0)
                        {
                            pred = (int) (((Q10 << 7) + num) / (Q10 << 8));
                            if (Al > 0 && pred >= (1 << Al))
                            {
                                pred = (1 << Al) - 1;
                            }
                        }
                        else
                        {
                            pred = (int) (((Q10 << 7) - num) / (Q10 << 8));
                            if (Al > 0 && pred >= (1 << Al))
                            {
                                pred = (1 << Al) - 1;
                            }
                            pred = -pred;
                        }
                        workspace[8] = (short) pred;
                    }
                    /* AC20 */
                    if ((Al = coef_bits[3 + coef_bits_offset]) != 0 && workspace[16] == 0)
                    {
                        num = 9 * Q00 * (DC2 + DC8 - 2 * DC5);
                        if (num >= 0)
                        {
                            pred = (int) (((Q20 << 7) + num) / (Q20 << 8));
                            if (Al > 0 && pred >= (1 << Al))
                            {
                                pred = (1 << Al) - 1;
                            }
                        }
                        else
                        {
                            pred = (int) (((Q20 << 7) - num) / (Q20 << 8));
                            if (Al > 0 && pred >= (1 << Al))
                            {
                                pred = (1 << Al) - 1;
                            }
                            pred = -pred;
                        }
                        workspace[16] = (short) pred;
                    }
                    /* AC11 */
                    if ((Al = coef_bits[4 + coef_bits_offset]) != 0 && workspace[9] == 0)
                    {
                        num = 5 * Q00 * (DC1 - DC3 - DC7 + DC9);
                        if (num >= 0)
                        {
                            pred = (int) (((Q11 << 7) + num) / (Q11 << 8));
                            if (Al > 0 && pred >= (1 << Al))
                            {
                                pred = (1 << Al) - 1;
                            }
                        }
                        else
                        {
                            pred = (int) (((Q11 << 7) - num) / (Q11 << 8));
                            if (Al > 0 && pred >= (1 << Al))
                            {
                                pred = (1 << Al) - 1;
                            }
                            pred = -pred;
                        }
                        workspace[9] = (short) pred;
                    }
                    /* AC02 */
                    if ((Al = coef_bits[5 + coef_bits_offset]) != 0 && workspace[2] == 0)
                    {
                        num = 9 * Q00 * (DC4 + DC6 - 2 * DC5);
                        if (num >= 0)
                        {
                            pred = (int) (((Q02 << 7) + num) / (Q02 << 8));
                            if (Al > 0 && pred >= (1 << Al))
                            {
                                pred = (1 << Al) - 1;
                            }
                        }
                        else
                        {
                            pred = (int) (((Q02 << 7) - num) / (Q02 << 8));
                            if (Al > 0 && pred >= (1 << Al))
                            {
                                pred = (1 << Al) - 1;
                            }
                            pred = -pred;
                        }
                        workspace[2] = (short) pred;
                    }
                    /* OK, do the IDCT */
                    lossyd.dctmgr.inverse_DCT(cinfo, compptr, workspace, output_ptr, output_ptr_offset, output_col, lossyd.inverse_DCT[ci]);
                    /* Advance for next column */
                    DC1 = DC2;
                    DC2 = DC3;
                    DC4 = DC5;
                    DC5 = DC6;
                    DC7 = DC8;
                    DC8 = DC9;

                    // Pointer Lesson: anytime there is a pointer arithmatic, must have a pointer offset
                    //buffer_ptr++, prev_block_row++, next_block_row++;
                    buffer_ptr_offset++;
                    prev_block_row_offset++;
                    next_block_row_offset++;
                    output_col += compptr.codec_data_unit;
                }
                output_ptr_offset += compptr.codec_data_unit;
            }
        }

        if (++(cinfo.output_iMCU_row) < cinfo.total_iMCU_rows)
        {
            return jpeglib8.JPEG_ROW_COMPLETED;
        }

        return jpeglib8.JPEG_SCAN_COMPLETED;
    }

    /*
     * Initialize coefficient buffer controller.
     */
    public void jinit_d_coef_controller(jpeg_decompress_struct8 cinfo, boolean need_full_buffer)
    {
        jdlossy8 lossyd = (jdlossy8) cinfo.codec;

        if (jmorecfg8.BLOCK_SMOOTHING_SUPPORTED)
        {
            coef_bits_latch = null;
        }

        /* Create the coefficient buffer. */
        if (need_full_buffer)
        {
            if (jmorecfg8.D_MULTISCAN_FILES_SUPPORTED)
            {
                /* Allocate a full-image virtual array for each component, */
                /* padded to a multiple of samp_factor DCT blocks in each direction. */
                /* Note we ask for a pre-zeroed array. */
                int ci, access_rows;
                jpeg_component_info8 compptr;

                for (ci = 0; ci < cinfo.num_components; ci++)
                {
                    compptr = cinfo.comp_info[ci];
                    access_rows = compptr.v_samp_factor;

                    if (jmorecfg8.BLOCK_SMOOTHING_SUPPORTED)
                    {
                        /* If block smoothing could be used, need a bigger window */
                        if (cinfo.process == jpeglib8.J_CODEC_PROCESS.JPROC_PROGRESSIVE)
                        {
                            access_rows *= 3;
                        }
                    }

                    whole_image[ci] = cinfo.mem.request_virt_barray(cinfo, true,
                            (int) jutils8.jround_up(compptr.width_in_data_units, compptr.h_samp_factor),
                            (int) jutils8.jround_up(compptr.height_in_data_units, compptr.v_samp_factor),
                            access_rows);
                }
                lossyd.consume_data = jpeg_d_codec8.CONSUME_DATA.consume_data;
                lossyd.decompress_data = jpeg_d_codec8.DECOMPRESS.decompress_data;
                lossyd.coef_arrays = whole_image; /* link to virtual arrays */
            }
            else
            {
                cinfo.err.ERREXIT(ErrorStrings8.JERR_NOT_COMPILED);
            }
        }
        else
        {
            /* We only need a single-MCU buffer. */
            // already initialized this

            MCU_buffer = new short[jpeglib8.D_MAX_DATA_UNITS_IN_MCU][jpeglib8.DCTSIZE2];
            lossyd.consume_data = jpeg_d_codec8.CONSUME_DATA.dummy_consume_data;
            lossyd.decompress_data = jpeg_d_codec8.DECOMPRESS.decompress_onepass;
            lossyd.coef_arrays = null; /* flag for no virtual arrays */
        }
    }
}
