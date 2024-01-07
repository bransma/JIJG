package bransford.jpeg.bit8;

import bransford.jpeg.bit8.error8.ErrorStrings8;
import bransford.jpeg.bit8.error8.TraceStrings8;
import bransford.jpeg.bit8.structs8.jpeg_input_controller8;
import bransford.jpeg.bit8.structs8.jpeg_component_info8;
import bransford.jpeg.bit8.structs8.jpeg_decompress_struct8;

import static bransford.jpeg.bit8.jpeglib8.JPEG_MAX_DIMENSION;
import static bransford.jpeg.bit8.jpeglib8.J_CODEC_PROCESS.JPROC_LOSSLESS;
import static bransford.jpeg.bit8.jpeglib8.MAX_COMPS_IN_SCAN;

public class jdinput8 extends jpeg_input_controller8
{
    public enum CONSUME_INPUT
    {
        consume_data, consume_markers
    }

    public CONSUME_INPUT consume_input = CONSUME_INPUT.consume_markers;

    public jdinput8()
    {
        has_multiple_scans = false;
        eoi_reached = false;
        inheaders = true;
    }

    public void reset_input_controller(jpeg_decompress_struct8 cinfo)
    {
        has_multiple_scans = false;
        eoi_reached = false;
        inheaders = true;
        cinfo.err.reset_error_mgr(cinfo);
        cinfo.marker.reset_marker_reader(cinfo);
        consume_input = CONSUME_INPUT.consume_markers;
        cinfo.coef_bits = null;
    }

    /*
     * Read JPEG markers before, between, or after compressed-data scans.
     * Change state as necessary when a new scan is reached.
     * Return value is JPEG_SUSPENDED, JPEG_REACHED_SOS, or JPEG_REACHED_EOI.
     *
     * The consume_input method pointer points either here or to the
     * coefficient controller's consume_data routine, depending on whether
     * we are reading a compressed data segment or inter-segment markers.
     */
    public int consume_markers(jpeg_decompress_struct8 cinfo)
    {
        int val;

        if (cinfo.inputctl.eoi_reached) /* After hitting EOI, read no further */
        {
            return jpeglib8.JPEG_REACHED_EOI;
        }

        val = cinfo.marker.read_markers(cinfo);

        switch (val)
        {
            case jpeglib8.JPEG_REACHED_SOS: /* Found SOS */
                if (inheaders)
                {
                    /* 1st SOS */
                    initial_setup(cinfo);

                    /*
                     * Initialize the decompression codec.  We need to do this here so that
                     * any codec-specific fields and function pointers are available to
                     * the rest of the library.
                     */
                    if (cinfo.process == JPROC_LOSSLESS)
                    {
                        jdlossls8 lossls = new jdlossls8();
                        cinfo.codec = lossls;
                        lossls.jinit_lossless_d_codec(cinfo);
                    }
                    else
                    {
                        jdlossy8 lossy = new jdlossy8();
                        cinfo.codec = lossy;
                        lossy.jinit_lossy_d_codec(cinfo);
                    }

                    inheaders = false;

                    /* Note: start_input_pass must be called by jdmaster.c
                     * before any more input can be consumed.  jdapimin.c is
                     * responsible for enforcing this sequencing.
                     */
                }
                else
                {
                    /* 2nd or later SOS marker */
                    if (!has_multiple_scans)
                    {
                        cinfo.err.ERREXIT(TraceStrings8.EOI_EXPECTED); /* Oops, I wasn't expecting this! */
                    }

                    /*
                     * Initialize the input modules to read a scan of compressed data.
                     * The first call to this is done by jdmaster.c after initializing
                     * the entire decompressor (during jpeg_start_decompress).
                     * Subsequent calls come from consume_markers, below.
                     */
                    // defines a method pointer to
                    start_input_pass(cinfo);
                }
                break;
            case jpeglib8.JPEG_REACHED_EOI: /* Found EOI */
                eoi_reached = true;
                if (inheaders)
                { /* Tables-only datastream, apparently */
                    if (cinfo.marker.saw_SOF)
                    {
                        cinfo.err.ERREXIT(TraceStrings8.SOF_NO_SOS);
                    }
                }
                else
                {
                    /* Prevent infinite loop in coef ctlr's decompress_data routine
                     * if user set output_scan_number larger than number of scans.
                     */
                    if (cinfo.output_scan_number > cinfo.input_scan_number)
                    {
                        cinfo.output_scan_number = cinfo.input_scan_number;
                    }
                }
                break;
            case jpeglib8.JPEG_SUSPENDED:
                break;
        }

        return val;
    }

    public void initial_setup(jpeg_decompress_struct8 cinfo)
        /* Called once, when first SOS marker is reached */
    {
        int ci;

        /* Make sure image isn't bigger than I can handle */
        if (cinfo.image_height > JPEG_MAX_DIMENSION ||
                cinfo.image_width > JPEG_MAX_DIMENSION)
            cinfo.err.ERREXIT1(ErrorStrings8.JERR_IMAGE_TOO_BIG, JPEG_MAX_DIMENSION);

        if (cinfo.process == JPROC_LOSSLESS)
        {
            /* If precision > compiled-in value, we must downscale */
            if (cinfo.data_precision > jmorecfg8.BITS_IN_JSAMPLE)
                cinfo.err.WARNMS2(ErrorStrings8.JWRN_MUST_DOWNSCALE,
                        cinfo.data_precision, jmorecfg8.BITS_IN_JSAMPLE);
        }
        else
        {  /* Lossy processes */
            /* For now, precision must match compiled-in value... */
            if (cinfo.data_precision != jmorecfg8.BITS_IN_JSAMPLE)
                cinfo.err.ERREXIT1(ErrorStrings8.JERR_BAD_PRECISION, cinfo.data_precision);
        }

        /* Make sure image isn't bigger than I can handle */
        if (cinfo.image_height > JPEG_MAX_DIMENSION || cinfo.image_width > JPEG_MAX_DIMENSION)
        {
            cinfo.err.ERREXIT1(ErrorStrings8.JERR_IMAGE_TOO_BIG, JPEG_MAX_DIMENSION);
        }

        /* Check that number of components won't exceed internal array sizes */
        if (cinfo.num_components > jpeglib8.MAX_COMPONENTS)
        {
            // throw exception
            cinfo.err.ERREXIT2(ErrorStrings8.JERR_COMPONENT_COUNT, cinfo.num_components, jmorecfg8.MAX_COMPONENTS);
        }

        /* Compute maximum sampling factors; check factor validity */
        cinfo.max_h_samp_factor = 1;
        cinfo.max_v_samp_factor = 1;
        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            if (cinfo.comp_info[ci].h_samp_factor <= 0 || cinfo.comp_info[ci].h_samp_factor > jpeglib8.MAX_SAMP_FACTOR
                    || cinfo.comp_info[ci].v_samp_factor <= 0 || cinfo.comp_info[ci].v_samp_factor > jpeglib8.MAX_SAMP_FACTOR)
            {
                cinfo.err.ERREXIT(ErrorStrings8.JERR_BAD_SAMPLING);
            }
            cinfo.max_h_samp_factor = Math.max(cinfo.max_h_samp_factor, cinfo.comp_info[ci].h_samp_factor);
            cinfo.max_v_samp_factor = Math.max(cinfo.max_v_samp_factor, cinfo.comp_info[ci].v_samp_factor);
        }

        /* We initialize codec_data_unit and min_codec_data_unit to data_unit.
         * In the full decompressor, this will be overridden by jdmaster.c;
         * but in the transcoder, jdmaster.c is not used, so we must do it here.
         */
        cinfo.min_codec_data_unit = cinfo.data_unit;

        /* Compute dimensions of components */
        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            cinfo.comp_info[ci].codec_data_unit = cinfo.data_unit;
            /* Size in data units */
            cinfo.comp_info[ci].width_in_data_units = (int) jutils8.jdiv_round_up(
                    (long) cinfo.image_width * cinfo.comp_info[ci].h_samp_factor,
                    ((long) cinfo.max_h_samp_factor * cinfo.data_unit));
            cinfo.comp_info[ci].height_in_data_units = (int) jutils8.jdiv_round_up(
                    (long) cinfo.image_height * cinfo.comp_info[ci].v_samp_factor,
                    ((long) cinfo.max_v_samp_factor * cinfo.data_unit));
            /* downsampled_width and downsampled_height will also be overridden by
             * jdmaster.c if we are doing full decompression.  The transcoder library
             * doesn't use these values, but the calling application might.
             */

            /* Size in samples */
            cinfo.comp_info[ci].downsampled_width = (int) jutils8
                    .jdiv_round_up((long) cinfo.image_width * cinfo.comp_info[ci].h_samp_factor,
                            cinfo.max_h_samp_factor);
            cinfo.comp_info[ci].downsampled_height = (int) jutils8
                    .jdiv_round_up((long) cinfo.image_height * cinfo.comp_info[ci].v_samp_factor,
                            cinfo.max_v_samp_factor);
            /* Mark component needed, until color conversion says otherwise */
            cinfo.comp_info[ci].component_needed = true;
            /* Mark no quantization table yet saved for component */
            cinfo.comp_info[ci].quant_table = null;
        }

        /* Compute number of fully interleaved MCU rows. */
        cinfo.total_iMCU_rows = (int) jutils8.jdiv_round_up(cinfo.image_height,
                (long) cinfo.max_v_samp_factor * cinfo.data_unit);

        /* Decide whether file contains multiple scans */
        has_multiple_scans = cinfo.comps_in_scan < cinfo.num_components || cinfo.process == jpeglib8.J_CODEC_PROCESS.JPROC_PROGRESSIVE;
    }

    public void start_input_pass(jpeg_decompress_struct8 cinfo)
    {
        per_scan_setup(cinfo);
        cinfo.codec.start_input_pass(cinfo);
        // this is a method pointer telling (this) class to redirect consume input to consume data on lossy/lossless codec (and not markers anymore)
        //cinfo.inputctl.consume_input = cinfo.codec.consume_data;
        consume_input = CONSUME_INPUT.consume_data;
    }

    public void finish_input_pass(jpeg_decompress_struct8 cinfo)
    {
        consume_input = CONSUME_INPUT.consume_markers;
    }

    public int consume_input(jpeg_decompress_struct8 cinfo)
    {
        switch (consume_input)
        {
            case consume_data ->
            {
                return cinfo.codec.consume_data(cinfo);
            }
            case consume_markers ->
            {
                return consume_markers(cinfo);
            }
            default ->
            {
                return 0;
            }
        }
    }

    private void per_scan_setup(jpeg_decompress_struct8 cinfo)
        /* Do computations that are needed before processing a JPEG scan */
        /* cinfo.comps_in_scan and cinfo.cur_comp_info[] were set from SOS marker */
    {
        int ci, mcublks, tmp;
        jpeg_component_info8 compptr;

        if (cinfo.comps_in_scan == 1)
        {

            /* Noninterleaved (single-component) scan */
            compptr = cinfo.cur_comp_info[0];

            /* Overall image size in MCUs */
            cinfo.MCUs_per_row = compptr.width_in_data_units;
            cinfo.MCU_rows_in_scan = compptr.height_in_data_units;

            /* For noninterleaved scan, always one data unit per MCU */
            compptr.MCU_width = 1;
            compptr.MCU_height = 1;
            compptr.MCU_data_units = 1;
            compptr.MCU_sample_width = compptr.codec_data_unit;
            compptr.last_col_width = 1;
            /* For noninterleaved scans, it is convenient to define last_row_height
             * as the number of data unit rows present in the last iMCU row.
             */
            tmp = compptr.height_in_data_units % compptr.v_samp_factor;
            if (tmp == 0)
                tmp = compptr.v_samp_factor;
            compptr.last_row_height = tmp;

            /* Prepare array describing MCU composition */
            cinfo.data_units_in_MCU = 1;
            cinfo.MCU_membership[0] = 0;

        }
        else
        {
            /* Interleaved (multi-component) scan */
            if (cinfo.comps_in_scan <= 0 || cinfo.comps_in_scan > MAX_COMPS_IN_SCAN)
                cinfo.err.ERREXIT2(ErrorStrings8.JERR_COMPONENT_COUNT, cinfo.comps_in_scan,
                        MAX_COMPS_IN_SCAN);

            /* Overall image size in MCUs */
            cinfo.MCUs_per_row = (int) jutils8.jdiv_round_up(cinfo.image_width,
                    (long) cinfo.max_h_samp_factor * cinfo.data_unit);
            cinfo.MCU_rows_in_scan = (int) jutils8.jdiv_round_up(cinfo.image_height,
                    (long) cinfo.max_v_samp_factor * cinfo.data_unit);

            cinfo.data_units_in_MCU = 0;

            for (ci = 0; ci < cinfo.comps_in_scan; ci++)
            {
                compptr = cinfo.cur_comp_info[ci];
                /* Sampling factors give # of data units of component in each MCU */
                compptr.MCU_width = compptr.h_samp_factor;
                compptr.MCU_height = compptr.v_samp_factor;
                compptr.MCU_data_units = compptr.MCU_width * compptr.MCU_height;
                compptr.MCU_sample_width = compptr.MCU_width * compptr.codec_data_unit;
                /* Figure number of non-dummy data units in last MCU column & row */
                tmp = compptr.width_in_data_units % compptr.MCU_width;
                if (tmp == 0)
                    tmp = compptr.MCU_width;
                compptr.last_col_width = tmp;
                tmp = compptr.height_in_data_units % compptr.MCU_height;
                if (tmp == 0)
                    tmp = compptr.MCU_height;
                compptr.last_row_height = tmp;
                /* Prepare array describing MCU composition */
                mcublks = compptr.MCU_data_units;
                if (cinfo.data_units_in_MCU + mcublks > jpeglib8.D_MAX_DATA_UNITS_IN_MCU)
                    cinfo.err.ERREXIT(ErrorStrings8.JERR_BAD_MCU_SIZE);
                while (mcublks-- > 0)
                {
                    cinfo.MCU_membership[cinfo.data_units_in_MCU++] = ci;
                }
            }
        }
    }
}
