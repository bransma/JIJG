package jijg.bit12_16;

import jijg.bit12_16.error12_16.ErrorStrings12_16;
import jijg.bit12_16.structs12_16.*;

/*
 * Copyright (C) 1991-1998, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains master control logic for the JPEG decompressor.
 * These routines are concerned with selecting the modules to be executed
 * and with determining the number of passes and the work to be done in each
 * pass.
 */

public class jdmaster12_16 extends jpeg_decomp_master12_16
{
    public boolean use_merged_upsample(jpeg_decompress_struct12_16 cinfo)
    {
        if (jmorecfg12_16.UPSAMPLE_SCALING_SUPPORTED)
        {
            /* Merging is the equivalent of plain box-filter upsampling */
            if (cinfo.do_fancy_upsampling || cinfo.CCIR601_sampling)
            {
                return false;
            }

            /* jdmerge.c only supports YCC=>RGB color conversion */
            if (cinfo.jpeg_color_space != jpeglib12_16.J_COLOR_SPACE.JCS_YCbCr || cinfo.num_components != 3
                    || cinfo.out_color_space != jpeglib12_16.J_COLOR_SPACE.JCS_RGB
                    || cinfo.out_color_components != jmorecfg12_16.RGB_PIXELSIZE)
            {
                return false;
            }

            /* and it only handles 2h1v or 2h2v sampling ratios */
            if (cinfo.comp_info[0].h_samp_factor != 2 || cinfo.comp_info[1].h_samp_factor != 1
                    || cinfo.comp_info[2].h_samp_factor != 1 || cinfo.comp_info[0].v_samp_factor > 2
                    || cinfo.comp_info[1].v_samp_factor != 1 || cinfo.comp_info[2].v_samp_factor != 1)
            {
                return false;
            }

	    /* furthermore, it doesn't work if each component has been
	       processed differently */
            return cinfo.comp_info[0].codec_data_unit == cinfo.min_codec_data_unit
                    && cinfo.comp_info[1].codec_data_unit == cinfo.min_codec_data_unit
                    && cinfo.comp_info[2].codec_data_unit == cinfo.min_codec_data_unit;

            /* ??? also need to test for upsample-time rescaling, when & if supported *//* by golly, it'll work... */
        }
        else
        {
            return false;
        }
    }

    /*
     * Compute output image dimensions and related values.
     * NOTE: this is exported for possible use by application.
     * Hence it mustn't do anything that can't be done twice.
     * Also note that it may be called before the master module is initialized!
     */

    public void jpeg_calc_output_dimensions(jpeg_decompress_struct12_16 cinfo)
        /* Do computations that are needed before master selection phase */
    {
        if (cinfo.global_state != jpegint12_16.DSTATE_READY)
        {
            cinfo.err.ERREXIT1(ErrorStrings12_16.JERR_BAD_STATE, cinfo.global_state);
        }

        cinfo.codec.calc_output_dimensions(cinfo);

        /* Report number of components in selected colorspace. */
        /* Probably this should be in the color conversion module... */

        switch (cinfo.out_color_space)
        {
            case JCS_GRAYSCALE:
                cinfo.out_color_components = 1;
                break;
            case JCS_RGB:
            case JCS_YCbCr:
                cinfo.out_color_components = 3;
                break;
            case JCS_CMYK:
            case JCS_YCCK:
                cinfo.out_color_components = 4;
                break;
            default:
                /* else must be same colorspace as in file */
                cinfo.out_color_components = cinfo.num_components;
        }

        cinfo.output_components = cinfo.quantize_colors ? 1 : cinfo.out_color_components;

        /* See if upsampler will want to emit more than one row at a time */
        if (use_merged_upsample(cinfo))
            cinfo.rec_outbuf_height = cinfo.max_v_samp_factor;
        else
            cinfo.rec_outbuf_height = 1;
    }

    /*
     * Several decompression processes need to range-limit values to the range
     * 0..MAXJSAMPLE; the input value may fall somewhat outside this range
     * due to noise introduced by quantization, roundoff error, etc.  These
     * processes are inner loops and need to be as fast as possible.  On most
     * machines, particularly CPUs with pipelines or instruction prefetch,
     * a (subscript-check-less) C table lookup
     *		x = sample_range_limit[x];
     * is faster than explicit tests
     *		if (x < 0)  x = 0;
     *		else if (x > MAXJSAMPLE)  x = MAXJSAMPLE;
     * These processes all use a common table prepared by the routine below.
     *
     * For most steps we can mathematically guarantee that the initial value
     * of x is within MAXJSAMPLE+1 of the legal range, so a table running from
     * -(MAXJSAMPLE+1) to 2*MAXJSAMPLE+1 is sufficient.  But for the initial
     * limiting step (just after the IDCT), a wildly out-of-range value is
     * possible if the input data is corrupt.  To avoid any chance of indexing
     * off the end of memory and getting a bad-pointer trap, we perform the
     * post-IDCT limiting thus:
     *		x = range_limit[x & MASK];
     * where MASK is 2 bits wider than legal sample data, ie 10 bits for 8-bit
     * samples.  Under normal circumstances this is more than enough range and
     * a correct output will be generated; with bogus input data the mask will
     * cause wraparound, and we will safely generate a bogus-but-in-range output.
     * For the post-IDCT step, we want to convert the data from signed to unsigned
     * representation by adding CENTERJSAMPLE at the same time that we limit it.
     * So the post-IDCT limiting table ends up looking like this:
     *   CENTERJSAMPLE,CENTERJSAMPLE+1,...,MAXJSAMPLE,
     *   MAXJSAMPLE (repeat 2*(MAXJSAMPLE+1)-CENTERJSAMPLE times),
     *   0          (repeat 2*(MAXJSAMPLE+1)-CENTERJSAMPLE times),
     *   0,1,...,CENTERJSAMPLE-1
     * Negative inputs select values from the upper half of the table after
     * masking.
     *
     * We can save some space by overlapping the start of the post-IDCT table
     * with the simpler range limiting table.  The post-IDCT table begins at
     * sample_range_limit + CENTERJSAMPLE.
     *
     * Note that the table is allocated in near data space on PCs; it's small
     * enough and used often enough to justify this.
     */
    public void prepare_range_limit_table(jpeg_decompress_struct12_16 cinfo)
    /* Allocate and fill in the sample_range_limit table */
    {
        short[] table = new short[(5 * (jmorecfg12_16.MAXJSAMPLE + 1) + jmorecfg12_16.CENTERJSAMPLE)];// * SIZEOF(JSAMPLE)]//JSAMPLE * table;
        int i;

        //	  table = (JSAMPLE *)
        //	    (cinfo.mem.alloc_small) ((j_common_ptr) cinfo, JPOOL_IMAGE,
        //			(5 * (MAXJSAMPLE+1) + CENTERJSAMPLE) * SIZEOF(JSAMPLE));
        //	  table += (jmorecfg.MAXJSAMPLE+1);	/* allow negative subscripts of simple table */
        // in the C-code 'table' is a pointer (address) so table += jmorecfg.MAXJSAMPLE + 1; is in effect an offset into the array
        int offset = jmorecfg12_16.MAXJSAMPLE + 1;
        cinfo.sample_range_limit_offset = offset;
        cinfo.sample_range_limit = table;

        /* Main part of "simple" table: limit[x] = x */
        for (i = 0; i <= jmorecfg12_16.MAXJSAMPLE; i++)
        {
            table[i + offset] = (short) i;
        }

        offset += jmorecfg12_16.CENTERJSAMPLE; /* Point to where post-IDCT table starts */

        /* End of simple table, rest of first half of post-IDCT table */
        for (i = jmorecfg12_16.CENTERJSAMPLE; i < 2 * (jmorecfg12_16.MAXJSAMPLE + 1); i++)
            table[i + offset] = (short) jmorecfg12_16.MAXJSAMPLE;

        /* Second half of post-IDCT table */

        System.arraycopy(cinfo.sample_range_limit, cinfo.sample_range_limit_offset, table,
                (4 * (jmorecfg12_16.MAXJSAMPLE + 1) - jmorecfg12_16.CENTERJSAMPLE), jmorecfg12_16.CENTERJSAMPLE);

    }

    /*
     * Master selection of decompression modules.
     * This is done once at jpeg_start_decompress time.  We determine
     * which modules will be used and give them appropriate initialization calls.
     * We also initialize the decompressor input side to begin consuming data.
     *
     * Since jpeg_read_header has finished, we know what is in the SOF
     * and (first) SOS markers.  We also have all the application parameter
     * settings.
     */
    public void master_selection(jpeg_decompress_struct12_16 cinfo)
    {
        /* Initialize dimensions and other stuff */
        jpeg_calc_output_dimensions(cinfo);
        prepare_range_limit_table(cinfo);

        /* Initialize my private state */
        pass_number = 0;
        using_merged_upsample = use_merged_upsample(cinfo);

        /* Color quantizer selection */
        quantizer_1pass = null;
        quantizer_2pass = null;
        /* No mode changes if not using buffered-image mode. */
        if (!cinfo.quantize_colors || !cinfo.buffered_image)
        {
            cinfo.enable_1pass_quant = false;
            cinfo.enable_external_quant = false;
            cinfo.enable_2pass_quant = false;
        }

        if (cinfo.quantize_colors)
        {
            if (cinfo.raw_data_out)
            {
               cinfo.err.ERREXIT(ErrorStrings12_16.JERR_NOTIMPL);
            }
            /* 2-pass quantizer only works in 3-component color space. */
            if (cinfo.out_color_components != 3)
            {
                cinfo.enable_1pass_quant = true;
                cinfo.enable_external_quant = false;
                cinfo.enable_2pass_quant = false;
                cinfo.colormap = null;
            }
            else if (cinfo.colormap != null)
            {
                cinfo.enable_external_quant = true;
            }
            else if (cinfo.two_pass_quantize)
            {
                cinfo.enable_2pass_quant = true;
            }
            else
            {
                cinfo.enable_1pass_quant = true;
            }

            if (cinfo.enable_1pass_quant)
            {
                if (jmorecfg12_16.QUANT_1PASS_SUPPORTED)
                {
                    quantizer_1pass = new jquant1_12_16();
                    quantizer_1pass.jinit_pass_quantizer(cinfo);
                }
            }
            /* We use the 2-pass code to map to external colormaps. */
            if (cinfo.enable_2pass_quant || cinfo.enable_external_quant)
            {
                if (jmorecfg12_16.QUANT_2PASS_SUPPORTED)
                {
                    quantizer_2pass = new jquant2_12_16();
                    quantizer_2pass.jinit_pass_quantizer(cinfo);
                }
                /* If both quantizers are initialized, the 2-pass one is left active;
                 * this is necessary for starting with quantization to an external map.
                 */
            }
        }

        /* Post-processing: in particular, color conversion first */
        if (!cinfo.raw_data_out)
        {
            if (using_merged_upsample)
            {
                if (jmorecfg12_16.UPSAMPLE_MERGING_SUPPORTED)
                {
                    jdmerge12_16 merged_upsample = new jdmerge12_16();
                    cinfo.upsample = merged_upsample;
                    merged_upsample.jinit_upsampler(cinfo); /* does color conversion too */
                }
                else
                {
                    cinfo.err.ERREXIT(ErrorStrings12_16.JERR_NOTIMPL);
                }
            }
            else
            {
                jpeg_color_deconverter12_16 color_deconverter = new jdcolor12_16();
                color_deconverter.jinit_color_deconverter(cinfo);
                cinfo.cconvert = color_deconverter;

                jdsample12_16 sample_upsample = new jdsample12_16();
                cinfo.upsample = sample_upsample;
                sample_upsample.jinit_upsampler(cinfo);
            }

            jpeg_d_post_controller12_16 post = new jdpostct12_16();
            cinfo.post = post;
            post.jinit_d_post_controller(cinfo, cinfo.enable_2pass_quant);
        }

        /* Initialize principal buffer controllers. */
        if (!cinfo.raw_data_out)

        {
            jpeg_d_main_controller12_16 main = new jdmainct12_16();
            cinfo.main = main;
            main.jinit_d_main_controller(cinfo, false /* never need full buffer here */);
        }
        /* We can now tell the memory manager to allocate virtual arrays. */
        cinfo.mem.realize_virt_arrays(cinfo);

        /* Initialize input side of de-compressor to consume first scan. */
        cinfo.inputctl.start_input_pass(cinfo);

        if (jmorecfg12_16.D_MULTISCAN_FILES_SUPPORTED)
        {
            /* If jpeg_start_decompress will read the whole file, initialize
             * progress monitoring appropriately.  The input step is counted
             * as one pass.
             */
            if (cinfo.progress != null && !cinfo.buffered_image && cinfo.inputctl.has_multiple_scans)
            {
                int nscans;
                /* Estimate number of scans to set pass_limit. */
                if (cinfo.process == jpeglib12_16.J_CODEC_PROCESS.JPROC_PROGRESSIVE)
                {
                    /* Arbitrarily estimate 2 interleaved DC scans + 3 AC scans/component. */
                    nscans = 2 + 3 * cinfo.num_components;
                }
                else
                {
                    /* For a nonprogressive multiscan file, estimate 1 scan per component. */
                    nscans = cinfo.num_components;
                }
                cinfo.progress.pass_counter = 0L;
                cinfo.progress.pass_limit = (long) cinfo.total_iMCU_rows * nscans;
                cinfo.progress.completed_passes = 0;
                cinfo.progress.total_passes = (cinfo.enable_2pass_quant ? 3 : 2);
                /* Count the input pass as done */
                pass_number++;
            }
        }
    }

    public void prepare_for_output_pass(jpeg_decompress_struct12_16 cinfo)
    {
        if (is_dummy_pass)
        {
            if (jmorecfg12_16.QUANT_2PASS_SUPPORTED)
            {
                /* Final pass of 2-pass quantization */
                is_dummy_pass = false;
                cinfo.cquantize.start_pass(cinfo, false);
                cinfo.post.start_pass(cinfo, jpegint12_16.J_BUF_MODE.JBUF_CRANK_DEST);
                cinfo.main.start_pass(cinfo, jpegint12_16.J_BUF_MODE.JBUF_CRANK_DEST);
            }
            else
            {
                cinfo.err.ERREXIT(ErrorStrings12_16.JERR_NOT_COMPILED);
            }
        }
        else
        {
            if (cinfo.quantize_colors && cinfo.colormap == null)
            {
                /* Select new quantization method */
                if (cinfo.two_pass_quantize && cinfo.enable_2pass_quant)
                {
                    cinfo.cquantize = quantizer_2pass;
                    is_dummy_pass = true;
                }
                else if (cinfo.enable_1pass_quant)
                {
                    cinfo.cquantize = quantizer_1pass;
                }
                else
                {
                    cinfo.err.ERREXIT(ErrorStrings12_16.JERR_MODE_CHANGE);
                }
            }
            cinfo.codec.start_output_pass(cinfo);
            if (!cinfo.raw_data_out)
            {
                if (!using_merged_upsample)
                {
                    cinfo.cconvert.start_pass(cinfo);
                }

                cinfo.upsample.start_pass(cinfo);

                if (cinfo.quantize_colors)
                {
                    cinfo.cquantize.start_pass(cinfo, is_dummy_pass);
                }

                cinfo.post.start_pass(cinfo,
                        (is_dummy_pass ? jpegint12_16.J_BUF_MODE.JBUF_SAVE_AND_PASS : jpegint12_16.J_BUF_MODE.JBUF_PASS_THRU));
                cinfo.main.start_pass(cinfo, jpegint12_16.J_BUF_MODE.JBUF_PASS_THRU);
            }
        }

        /* Set up progress monitor's pass info if present */
        if (cinfo.progress != null)
        {
            cinfo.progress.completed_passes = pass_number;
            cinfo.progress.total_passes = pass_number + (is_dummy_pass ? 2 : 1);
            /* In buffered-image mode, we assume one more output pass if EOI not
             * yet reached, but no more passes if EOI has been reached.
             */
            if (cinfo.buffered_image && !cinfo.inputctl.eoi_reached)
            {
                cinfo.progress.total_passes += (cinfo.enable_2pass_quant ? 2 : 1);
            }
        }
    }

    public void finish_output_pass(jpeg_decompress_struct12_16 cinfo)
    {
        if (cinfo.quantize_colors)
        {
            cinfo.cquantize.finish_pass(cinfo);
        }
        pass_number++;
    }

    public void jpeg_new_colormap(jpeg_decompress_struct12_16 cinfo)
    {
        /* Prevent application from calling me at wrong times */
        if (cinfo.global_state != jpegint12_16.DSTATE_BUFIMAGE)
        {
            cinfo.err.ERREXIT1(ErrorStrings12_16.JERR_BAD_STATE, cinfo.global_state);
        }

        if (cinfo.quantize_colors && cinfo.enable_external_quant && cinfo.colormap != null)
        {
            /* Select 2-pass quantizer for external colormap use */
            cinfo.cquantize = quantizer_2pass;
            /* Notify quantizer of colormap change */
            cinfo.cquantize.new_color_map(cinfo);
            is_dummy_pass = false; /* just in case */
        }
        else
        {
            cinfo.err.ERREXIT(ErrorStrings12_16.JERR_MODE_CHANGE);
        }
    }

    /*
     * Initialize master decompression control and select active modules.
     * This is performed at the start of jpeg_start_decompress.
     */
    public void jinit_master_decompress(jpeg_decompress_struct12_16 cinfo)
    {
        master_selection(cinfo);
    }
}
