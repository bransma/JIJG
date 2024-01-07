package bransford.jpeg.bit12_16;

import bransford.jpeg.bit12_16.error12_16.ErrorStrings12_16;
import bransford.jpeg.bit12_16.structs12_16.*;
import bransford.jpeg.bit12_16.structs12_16.*;

/*
 * jdlossy.c
 *
 * Copyright (C) 1998, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains the control logic for the lossy JPEG decompressor.
 */

public class jdlossy12_16 extends jpeg_d_codec12_16
{
    /* Pointer to data which is private to coef module */
    //void *coef_private;
    public jdcoefct12_16 coef_private = new jdcoefct12_16();

    /* This is here to share code between baseline and progressive decoders; */
    /* other modules probably should not use it */
    public boolean entropy_insufficient_data; /* set TRUE after emitting warning */

    /* Pointer to data which is private to idct module */
    // void *idct_private;
    public idct_controller12_16 dctmgr = new jddctmgr12_16();

    /* It is useful to allow each component to have a separate IDCT method. */
    idct_controller12_16.inverse_DCT_method_ptr[] inverse_DCT = new idct_controller12_16.inverse_DCT_method_ptr[jpeglib12_16.MAX_COMPONENTS];

    /* Pointer to array of coefficient virtual arrays, or NULL if none */
    public jvirt_barray_control12_16[] coef_arrays;

    /*
     * Compute output image dimensions and related values.
     */

    @Override
    public void calc_output_dimensions(jpeg_decompress_struct12_16 cinfo)
    {
        if (jmorecfg12_16.IDCT_SCALING_SUPPORTED)
        {
            /* Compute actual output image dimensions and DCT scaling choices. */
            if (cinfo.scale_num * 8 <= cinfo.scale_denom)
            {
                /* Provide 1/8 scaling */
                cinfo.output_width = (int) jutils12_16.jdiv_round_up(cinfo.image_width, 8L);

                cinfo.output_height = (int) jutils12_16.jdiv_round_up(cinfo.image_height, 8L);

                cinfo.min_codec_data_unit = 1;
            }
            else if (cinfo.scale_num * 4 <= cinfo.scale_denom)
            {
                /* Provide 1/4 scaling */
                cinfo.output_width = (int) jutils12_16.jdiv_round_up(cinfo.image_width, 4L);

                cinfo.output_height = (int) jutils12_16.jdiv_round_up(cinfo.image_height, 4L);

                cinfo.min_codec_data_unit = 2;
            }
            else if (cinfo.scale_num * 2 <= cinfo.scale_denom)
            {
                /* Provide 1/2 scaling */
                cinfo.output_width = (int) jutils12_16.jdiv_round_up(cinfo.image_width, 2L);

                cinfo.output_height = (int) jutils12_16.jdiv_round_up(cinfo.image_height, 2L);
                cinfo.min_codec_data_unit = 4;
            }
            else
            {
                /* Provide 1/1 scaling */
                cinfo.output_width = cinfo.image_width;
                cinfo.output_height = cinfo.image_height;
                cinfo.min_codec_data_unit = jpeglib12_16.DCTSIZE;
            }
            /* In selecting the actual DCT scaling for each component, we try to
             * scale up the chroma components via IDCT scaling rather than upsampling.
             * This saves time if the upsampler gets to use 1:1 scaling.
             * Note this code assumes that the supported DCT scalings are powers of 2.
             */

            for (int ci = 0; ci < cinfo.num_components; ci++)
            {
                int ssize = cinfo.min_codec_data_unit;
                while (ssize < jpeglib12_16.DCTSIZE
                        && (cinfo.comp_info[ci].h_samp_factor * ssize * 2 <= cinfo.max_h_samp_factor * cinfo.min_codec_data_unit)
                        && (cinfo.comp_info[ci].v_samp_factor * ssize * 2 <= cinfo.max_v_samp_factor * cinfo.min_codec_data_unit))
                {
                    ssize = ssize * 2;
                }
                cinfo.comp_info[ci].codec_data_unit = ssize;
            }

            /* Recompute downsampled dimensions of components;
             * application needs to know these if using raw downsampled data.
             */
            for (int ci = 0; ci < cinfo.num_components; ci++)
            {
                /* Size in samples, after IDCT scaling */
                cinfo.comp_info[ci].downsampled_width = (int) jutils12_16.jdiv_round_up(
                        (long) cinfo.image_width * (long) ((long) cinfo.comp_info[ci].h_samp_factor * cinfo.comp_info[ci].codec_data_unit),
                        (long) cinfo.max_h_samp_factor * jpeglib12_16.DCTSIZE);

                cinfo.comp_info[ci].downsampled_height = (int) jutils12_16.jdiv_round_up(
                        (long) cinfo.image_height
                                * (long) ((long) cinfo.comp_info[ci].v_samp_factor * cinfo.comp_info[ci].codec_data_unit),
                        (long) cinfo.max_v_samp_factor * jpeglib12_16.DCTSIZE);
            }
        }
        else /* !IDCT_SCALING_SUPPORTED */
        {
            /* Hardwire it to "no scaling" */
            cinfo.output_width = cinfo.image_width;
            cinfo.output_height = cinfo.image_height;
            /* jdinput.c has already initialized codec_data_unit to DCTSIZE,
             * and has computed unscaled downsampled_width and downsampled_height.
             */
        }
    }

    /*
     * Initialize for an input processing pass.
     */

    @Override
    public void start_input_pass(jpeg_decompress_struct12_16 cinfo)
    {
        latch_quant_tables(cinfo);
        // passthrough to dcoef
        entropy_start_pass(cinfo);
        coef_private.start_input_pass(cinfo);
    }

    /*
     * Initialize for an output processing pass.
     */

    @Override
    public void start_output_pass(jpeg_decompress_struct12_16 cinfo)
    {
        // passthrough to to dctmgr & jdcoefct
        dctmgr.start_pass(cinfo);
        coef_private.start_output_pass(cinfo);
    }

    @Override
    public int consume_data(jpeg_decompress_struct12_16 cinfo)
    {
        // passthrough to dcoefct
        switch (consume_data)
        {
            case consume_data:
            {
                return coef_private.consume_data(cinfo);
            }
            case dummy_consume_data:
                return dummy_consume_data(cinfo);
            default:
                return jpeglib12_16.JPEG_SUSPENDED;
        }
    }

    @Override
    public int decompress_data(jpeg_decompress_struct12_16 cinfo, short[][][] output_buf, int[] output_buffer_offset)
    {
        //  passthrough  dcoef
        return switch (decompress_data)
                {
                    case decompress_data -> coef_private.decompress_data(cinfo, output_buf, output_buffer_offset);
                    case decompress_onepass -> coef_private.decompress_onepass(cinfo, output_buf, output_buffer_offset);
                    case decompress_smooth_data ->
                            coef_private.decompress_smooth_data(cinfo, output_buf, output_buffer_offset);
                };
    }

    @Override
    public void scaler_scale(jpeg_decompress_struct12_16 cinfo, int[] diff_buf, short[] output_buf, int width)
    {

    }

    @Override
    public void diff_start_input_pass(jpeg_decompress_struct12_16 cinfo)
    {

    }

    /*
     * Save away a copy of the Q-table referenced by each component present
     * in the current scan, unless already saved during a prior scan.
     *
     * In a multiple-scan JPEG file, the encoder could assign different components
     * the same Q-table slot number, but change table definitions between scans
     * so that each component uses a different Q-table.  (The IJG encoder is not
     * currently capable of doing this, but other encoders might.)  Since we want
     * to be able to dequantize all the components at the end of the file, this
     * means that we have to save away the table actually used for each component.
     * We do this by copying the table at the start of the first scan containing
     * the component.
     * The JPEG spec prohibits the encoder from changing the contents of a Q-table
     * slot between scans of a component using that slot.  If the encoder does so
     * anyway, this decoder will simply use the Q-table values that were current
     * at the start of the first scan for the component.
     *
     * The decompressor output side looks only at the saved quant tables,
     * not at the current Q-table slots.
     */

    private void latch_quant_tables(jpeg_decompress_struct12_16 cinfo)
    {
        int ci, qtblno;
        jpeg_component_info12_16 compptr;
        JQUANT_TBL12_16 qtbl;

        for (ci = 0; ci < cinfo.comps_in_scan; ci++)
        {
            compptr = cinfo.cur_comp_info[ci];
            /* No work if we already saved Q-table for this component */
            if (compptr.quant_table != null)
                continue;
            /* Make sure specified quantization table is present */
            qtblno = compptr.quant_tbl_no;
            if (qtblno < 0 || qtblno >= jpeglib12_16.NUM_QUANT_TBLS || cinfo.quant_tbl_ptrs[qtblno] == null)
                cinfo.err.ERREXIT1(ErrorStrings12_16.JERR_NO_QUANT_TABLE, qtblno);
            /* OK, save away the quantization table */
            qtbl = new JQUANT_TBL12_16();

            System.arraycopy(cinfo.quant_tbl_ptrs[qtblno].quantval, 0, qtbl.quantval, 0, qtbl.quantval.length);
            compptr.quant_table = qtbl;
        }
    }

    /* Coefficient buffer control */
    public void coef_start_input_pass(jpeg_decompress_struct12_16 cinfo)
    {

    }

    public void coef_start_output_pass(jpeg_decompress_struct12_16 cinfo)
    {

    }

    /* Entropy decoding */
    public void entropy_start_pass(jpeg_decompress_struct12_16 cinfo)
    {
        // passthrough to the proper huffman decoder
        entropy_private.entropy_start_pass(cinfo);
    }

    @Override
    public boolean entropy_process_restart(jpeg_decompress_struct12_16 cinfo)
    {
        return false;
    }

    @Override
    public int entropy_decode_mcus(jpeg_decompress_struct12_16 cinfo, int[][][] diff_buf,
                                   int MCU_row_num, int MCU_col_num, int nMCU)
    {
        return 0;
    }

    @Override
    public void predict_start_pass(jpeg_decompress_struct12_16 cinfo)
    {
    }

    @Override
    public void predict_process_restart(jpeg_decompress_struct12_16 cinfo)
    {
    }

    @Override
    public void predict_undifference(jpeg_decompress_struct12_16 cinfo, int comp_index,
                                     int[] diff_buf, int[] prev_row,
                                     int[] undiff_buf, int width)
    {
    }

    public boolean entropy_decode_mcu(jpeg_decompress_struct12_16 cinfo, short[][] MCU_data)//JBLOCKROW *MCU_data)
    {
        return entropy_private.entropy_decode_mcu(cinfo, MCU_data);
    }

    /* Inverse DCT (also performs dequantization) */
    public void idct_start_pass(jpeg_decompress_struct12_16 cinfo)
    {

    }

    /*
     * Initialize the lossy decompression codec.
     * This is called only once, during master selection.
     */
    // initialized from consume_markers in jdinput
    public void jinit_lossy_d_codec(jpeg_decompress_struct12_16 cinfo)
    {
        boolean use_c_buffer;

        /* Initialize sub-modules */

        /* Inverse DCT */
        dctmgr.jinit_inverse_dct(cinfo);
        /* Entropy decoding: either arithmetic coding (true) or Huffman (false) */
        if (cinfo.arith_code)
        {
            if (jmorecfg12_16.WITH_ARITHMETIC_PATCH)
            {
                // unimplemented, do so only if jmorecfg.WITH_ARITHMETIC_PATCH = true (which it isn't)
                jdarith12_16.jinit_arith_decoder(cinfo);
            }
            else
            {
                cinfo.err.ERREXIT(ErrorStrings12_16.JERR_ARITH_NOTIMPL);
            }
        }
        else
        {
            if (cinfo.process == jpeglib12_16.J_CODEC_PROCESS.JPROC_PROGRESSIVE)
            {
                if (jmorecfg12_16.D_PROGRESSIVE_SUPPORTED)
                {
                    jdphuff12_16 phuff = new jdphuff12_16();
                    entropy_private = phuff;
                    phuff.jinit_phuff_decoder(cinfo);

                }
                else
                {
                    cinfo.err.ERREXIT(ErrorStrings12_16.JERR_NOT_COMPILED);
                }
            }
            else
            {
                jdshuff12_16 shuff = new jdshuff12_16();
                entropy_private = shuff;
                shuff.jinit_shuff_decoder(cinfo);
            }
        }

        use_c_buffer = cinfo.inputctl.has_multiple_scans || cinfo.buffered_image;
        coef_private.jinit_d_coef_controller(cinfo, use_c_buffer);
    }
}
