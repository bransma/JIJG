package jijg.bit12_16;
/*
 *
 * This file contains application interface code for the decompression half
 * of the JPEG library.  These are the "minimum" API routines that may be
 * needed in either the normal full-decompression case or the
 * transcoding-only case.
 *
 * Most of the routines intended to be called directly by an application
 * are in this file or in jdapistd.c.  But also see jcomapi.c for routines
 * shared by compression and decompression, and jdtrans.c for the transcoding
 * case.
 */

import jijg.bit12_16.error12_16.ErrorStrings12_16;
import jijg.bit12_16.structs12_16.jpeg_decompress_struct12_16;

public class jdapimin12_16
{
    /*
     * Initialization of a JPEG decompression object.
     * The error manager must already be set up (in case memory manager fails).
     */

    public void jpeg_CreateDecompress(jpeg_decompress_struct12_16 cinfo)
    {
        cinfo.is_decompressor = true;
        cinfo.marker = new jdmarker12_16();

        /* Initialize marker processor so application can override methods
         * for COM, APPn markers before calling jpeg_read_header.
         */
        cinfo.marker.jinit_marker_reader(cinfo);

        /* And initialize the overall input controller. */
        cinfo.inputctl = new jdinput12_16();

        cinfo.mem = new jmemmgr12_16();

        /* OK, I'm ready */
        cinfo.global_state = jpegint12_16.DSTATE_START;
    }

    /*
     * Set default decompression parameters.
     */
    public void default_decompress_parms(jpeg_decompress_struct12_16 cinfo)
    {
        /* Guess the input colorspace, and set output colorspace accordingly. */
        /* (Wish JPEG committee had provided a real way to specify this...) */
        /* Note application may override our guesses. */
        switch (cinfo.num_components)
        {
            case 1:
            {
                cinfo.jpeg_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_GRAYSCALE;
                cinfo.out_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_GRAYSCALE;
                break;
            }
            case 3:
            {
                if (cinfo.saw_JFIF_marker)
                {
                    cinfo.jpeg_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_YCbCr; /* JFIF implies YCbCr */
                }
                else if (cinfo.saw_Adobe_marker)
                {
                    switch (cinfo.Adobe_transform)
                    {
                        case 0:
                            cinfo.jpeg_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_RGB;
                            break;
                        case 1:
                            cinfo.jpeg_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_YCbCr;
                            break;
                        default:
                        {
                            cinfo.err.WARNMS1(ErrorStrings12_16.JWRN_ADOBE_XFORM, cinfo.Adobe_transform);
                            cinfo.jpeg_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_YCbCr; /* assume it's YCbCr */
                        }
                    }
                }
                else
                {
                    /* Saw no special markers, try to guess from the component IDs */
                    int cid0 = cinfo.comp_info[0].component_id;
                    int cid1 = cinfo.comp_info[1].component_id;
                    int cid2 = cinfo.comp_info[2].component_id;

                    if (cid0 == 1 && cid1 == 2 && cid2 == 3)
                        cinfo.jpeg_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_YCbCr; /* assume JFIF w/out marker */
                    else if (cid0 == 82 && cid1 == 71 && cid2 == 66)
                        cinfo.jpeg_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_RGB; /* ASCII 'R', 'G', 'B' */
                    else
                    {
                        if (cinfo.process == jpeglib12_16.J_CODEC_PROCESS.JPROC_LOSSLESS)
                        {
                            cinfo.jpeg_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_RGB; /* assume it's RGB */
                        }
                        else
                        { /* Lossy processes */
                            cinfo.jpeg_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_YCbCr; /* assume it's YCbCr */
                        }
                    }
                }
                /* Always guess RGB is proper output colorspace. */
                cinfo.out_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_RGB;
                break;
            }
            case 4:
            {
                if (cinfo.saw_Adobe_marker)
                {
                    switch (cinfo.Adobe_transform)
                    {
                        case 0:
                            cinfo.jpeg_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_CMYK;
                            break;
                        case 2:
                            cinfo.jpeg_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_YCCK;
                            break;
                        default:
                        {
                            cinfo.err.WARNMS1(ErrorStrings12_16.JWRN_ADOBE_XFORM, cinfo.Adobe_transform);
                            cinfo.jpeg_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_YCbCr; /* assume it's YCCK */
                        }
                    }
                }
                else
                {
                    /* No special markers, assume straight CMYK. */
                    cinfo.jpeg_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_CMYK;
                }
                cinfo.out_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_CMYK;
                break;
            }
            default:
            {
                cinfo.jpeg_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_UNKNOWN;
                cinfo.out_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_UNKNOWN;
            }
        }

        /* Set defaults for other decompression parameters. */
        cinfo.scale_num = 1; /* 1:1 scaling */
        cinfo.scale_denom = 1;
        cinfo.output_gamma = 1.0;
        cinfo.buffered_image = false;
        cinfo.raw_data_out = false;
        cinfo.dct_method = jpeglib12_16.JDCT_DEFAULT;
        cinfo.do_fancy_upsampling = true;
        cinfo.do_block_smoothing = true;
        cinfo.quantize_colors = false;
        /* We set these in case application only sets quantize_colors. */
        cinfo.dither_mode = jpeglib12_16.J_DITHER_MODE.JDITHER_FS;
        cinfo.two_pass_quantize = jmorecfg12_16.QUANT_2PASS_SUPPORTED;
        cinfo.desired_number_of_colors = 256;
        cinfo.colormap = null;
        /* Initialize for no mode change in buffered-image mode. */
        cinfo.enable_1pass_quant = false;
        cinfo.enable_external_quant = false;
        cinfo.enable_2pass_quant = false;
    }

    /*
     * Decompression startup: read start of JPEG datastream to see what's there.
     * Need only initialize JPEG object and supply a data source before calling.
     *
     * This routine will read as far as the first SOS marker (ie, actual start of
     * compressed data), and will save all tables and parameters in the JPEG
     * object.  It will also initialize the decompression parameters to default
     * values, and finally return JPEG_HEADER_OK.  On return, the application may
     * adjust the decompression parameters and then call jpeg_start_decompress.
     * (Or, if the application only wanted to determine the image parameters,
     * the data need not be decompressed.  In that case, call jpeg_abort or
     * jpeg_destroy to release any temporary space.)
     * If an abbreviated (tables only) datastream is presented, the routine will
     * return JPEG_HEADER_TABLES_ONLY upon reaching EOI.  The application may then
     * re-use the JPEG object to read the abbreviated image datastream(s).
     * It is unnecessary (but OK) to call jpeg_abort in this case.
     * The JPEG_SUSPENDED return code only occurs if the data source module
     * requests suspension of the decompressor.  In this case the application
     * should load more source data and then re-call jpeg_read_header to resume
     * processing.
     * If a non-suspending data source is used and require_image is true, then the
     * return code need not be inspected since only JPEG_HEADER_OK is possible.
     *
     * This routine is now just a front end to jpeg_consume_input, with some
     * extra error checking.
     */

    public int jpeg_read_header(jpeg_decompress_struct12_16 cinfo, boolean require_image)
    {
        if (cinfo.global_state != jpegint12_16.DSTATE_START && cinfo.global_state != jpegint12_16.DSTATE_INHEADER)
        {
            cinfo.err.ERREXIT1(ErrorStrings12_16.JERR_BAD_STATE, cinfo.global_state);
        }

        int retcode = jpeg_consume_input(cinfo);

        switch (retcode)
        {
            case jpeglib12_16.JPEG_REACHED_SOS:
            {
                return jpeglib12_16.JPEG_HEADER_OK;
            }
            case jpeglib12_16.JPEG_REACHED_EOI:
            {
                if (require_image)
                {
                    /* Complain if application wanted an image */
                    cinfo.err.ERREXIT(ErrorStrings12_16.JERR_NO_IMAGE);
                    /* Reset to start state; it would be safer to require the application to
                     * call jpeg_abort, but we can't change it now for compatibility reasons.
                     * A side effect is to free any temporary memory (there shouldn't be any).
                     */
                    jpeg_abort(cinfo); /* sets state = DSTATE_START */
                }
                return jpeglib12_16.JPEG_HEADER_TABLES_ONLY;
            }
            case jpeglib12_16.JPEG_SUSPENDED:
            {
                /* no work */
            }
        }

        return retcode;
    }

    /*
     * Consume data in advance of what the decompressor requires.
     * This can be called at any time once the decompressor object has
     * been created and a data source has been set up.
     *
     * This routine is essentially a state machine that handles a couple
     * of critical state-transition actions, namely initial setup and
     * transition from header scanning to ready-for-start_decompress.
     * All the actual input is done via the input controller's consume_input
     * method.
     */

    public int jpeg_consume_input(jpeg_decompress_struct12_16 cinfo)
    {
        int retcode = jpeglib12_16.JPEG_SUSPENDED;
        /* NB: every possible DSTATE value should be listed in this switch */
        switch (cinfo.global_state)
        {
            case jpegint12_16.DSTATE_START:
                /* Initialize application's data source module */
                cinfo.inputctl.reset_input_controller(cinfo);
                cinfo.global_state = jpegint12_16.DSTATE_INHEADER;
                /*FALLTHROUGH*/
            case jpegint12_16.DSTATE_INHEADER:

                retcode = cinfo.inputctl.consume_input(cinfo);

                if (retcode == jpeglib12_16.JPEG_REACHED_SOS)
                {
                    /* Found SOS, prepare to decompress */
                    /* Set up default parameters based on header data */
                    default_decompress_parms(cinfo);
                    /* Set global state: ready for start_decompress */
                    cinfo.global_state = jpegint12_16.DSTATE_READY;
                }
                break;
            case jpegint12_16.DSTATE_READY:
                /* Can't advance past first SOS until start_decompress is called */
                retcode = jpeglib12_16.JPEG_REACHED_SOS;
                break;
            case jpegint12_16.DSTATE_PRELOAD:
            case jpegint12_16.DSTATE_PRESCAN:
            case jpegint12_16.DSTATE_SCANNING:
            case jpegint12_16.DSTATE_RAW_OK:
            case jpegint12_16.DSTATE_BUFIMAGE:
            case jpegint12_16.DSTATE_BUFPOST:
            case jpegint12_16.DSTATE_STOPPING:
                break;
            default:
                cinfo.err.ERREXIT1(ErrorStrings12_16.JERR_BAD_STATE, cinfo.global_state);
        }

        return retcode;
    }

    /*
     * Have we finished reading the input file?
     */

    public boolean jpeg_input_complete(jpeg_decompress_struct12_16 cinfo)
    {
        /* Check for valid jpeg object */
        if (cinfo.global_state < jpegint12_16.DSTATE_START || cinfo.global_state > jpegint12_16.DSTATE_STOPPING)
        {
            cinfo.err.ERREXIT1(ErrorStrings12_16.JERR_BAD_STATE, cinfo.global_state);
        }

        return cinfo.inputctl.eoi_reached;
    }

    /*
     * Is there more than one scan?
     */

    public boolean jpeg_has_multiple_scans(jpeg_decompress_struct12_16 cinfo)
    {
        /* Only valid after jpeg_read_header completes */
        if (cinfo.global_state < jpegint12_16.DSTATE_READY || cinfo.global_state > jpegint12_16.DSTATE_STOPPING)
        {
            cinfo.err.ERREXIT1(ErrorStrings12_16.JERR_BAD_STATE, cinfo.global_state);
        }

        return cinfo.inputctl.has_multiple_scans;
    }

    /*
     * Finish JPEG decompression.
     *
     * This will normally just verify the file trailer and release temp storage.
     *
     * Returns false if suspended.  The return value need be inspected only if
     * a suspending data source is used.
     */
    public boolean jpeg_finish_decompress(jpeg_decompress_struct12_16 cinfo)
    {
        if ((cinfo.global_state == jpegint12_16.DSTATE_SCANNING || cinfo.global_state == jpegint12_16.DSTATE_RAW_OK) && !cinfo.buffered_image)
        {
            /* Terminate final pass of non-buffered mode */
            if (cinfo.output_scanline[0] < cinfo.output_height)
            {
                cinfo.err.ERREXIT(ErrorStrings12_16.JERR_TOO_LITTLE_DATA);
            }
            cinfo.master.finish_output_pass(cinfo);
            cinfo.global_state = jpegint12_16.DSTATE_STOPPING;
        }
        else if (cinfo.global_state == jpegint12_16.DSTATE_BUFIMAGE)
        {
            /* Finishing after a buffered-image operation */
            cinfo.global_state = jpegint12_16.DSTATE_STOPPING;
        }
        else if (cinfo.global_state != jpegint12_16.DSTATE_STOPPING)
        {
            /* STOPPING = repeat call after a suspension, anything else is error */
            cinfo.err.ERREXIT1(ErrorStrings12_16.JERR_BAD_STATE, cinfo.global_state);
        }
        /* Read until EOI */
        while (!cinfo.inputctl.eoi_reached)
        {
            if (cinfo.inputctl.consume_input(cinfo) == jpeglib12_16.JPEG_SUSPENDED)
            {
                return false; /* Suspend, come back later */
            }
        }

        /* Do final cleanup */
        cinfo.src.term_source(cinfo);
        /* We can use jpeg_abort to release memory and reset global_state */
        jpeg_abort(cinfo);
        return true;
    }

    // lifted from jcomapi.c -- seemed a strange and arbitrary place for these routines

    public static void jpeg_abort(jpeg_decompress_struct12_16 cinfo)
    {
        int pool;

        /* Do nothing if called on a not-initialized or destroyed JPEG object. */
        if (cinfo.mem == null)
        {
            return;
        }

        /* Releasing pools in reverse order might help avoid fragmentation
         * with some (brain-damaged) malloc libraries.
         */
        for (pool = jpeglib12_16.JPOOL_NUMPOOLS - 1; pool > jpeglib12_16.JPOOL_PERMANENT; pool--)
        {
            cinfo.mem.free_pool(cinfo);
        }

        /* Reset overall state for possible reuse of object */
        if (cinfo.is_decompressor)
        {
            cinfo.global_state = jpegint12_16.DSTATE_START;
            /* Try to keep application from accessing now-deleted marker list.
             * A bit kludgy to do it here, but this is the most central place.
             */
            cinfo.marker_list = null;
        }
        else
        {
            cinfo.global_state = jpegint12_16.CSTATE_START;
        }
    }
 }
