package jijg.bit8;

import jijg.bit8.error8.ErrorStrings8;
import jijg.bit8.structs8.jpeg_decompress_struct8;

public class jdapistd8
{
    /*
     * Decompression initialization.
     * jpeg_read_header must be completed before calling this.
     *
     * If a multipass operating mode was selected, this will do all but the
     * last pass, and thus may take a great deal of time.
     *
     * Returns FALSE if suspended.  The return value need be inspected only if
     * a suspending data source is used.
     */
    public boolean jpeg_start_decompress(jpeg_decompress_struct8 cinfo)
    {
        if (cinfo.global_state == jpegint8.DSTATE_READY)
        {
            jdmaster8 master = new jdmaster8();
            cinfo.master = master;
            master.is_dummy_pass = false;
            master.jinit_master_decompress(cinfo);

            if (cinfo.buffered_image)
            {
                cinfo.global_state = jpegint8.DSTATE_BUFIMAGE;
                return true;
            }
            cinfo.global_state = jpegint8.DSTATE_PRELOAD;
        }

        if (cinfo.global_state == jpegint8.DSTATE_PRELOAD)
        {
            if (cinfo.inputctl.has_multiple_scans)
            {
                if (jmorecfg8.D_MULTISCAN_FILES_SUPPORTED)
                {
                    for (; ; )
                    {
                        int retcode;

                        if (cinfo.progress != null)
                        {
                            cinfo.progress.progress_monitor(cinfo);
                        }

                        retcode = cinfo.inputctl.consume_input(cinfo);

                        if (retcode == jpeglib8.JPEG_SUSPENDED)
                        {
                            return false;
                        }

                        if (retcode == jpeglib8.JPEG_REACHED_EOI)
                        {
                            break;
                        }

                        if (cinfo.progress != null)
                        {
                            if (++cinfo.progress.pass_counter >= cinfo.progress.pass_limit)
                            {
                                cinfo.progress.pass_limit += cinfo.total_iMCU_rows;
                            }
                        }
                    }
                }
                else
                {
                    cinfo.err.ERREXIT(ErrorStrings8.JERR_NOT_COMPILED);
                }
            }

            cinfo.output_scan_number = cinfo.input_scan_number;
        }
        else if (cinfo.global_state != jpegint8.DSTATE_PRESCAN)
        {
            // Error out
            cinfo.err.ERREXIT1(ErrorStrings8.JERR_BAD_STATE, cinfo.global_state);
        }

        return output_pass_setup(cinfo);
    }

    /*
     * Set up for an output pass, and perform any dummy pass(es) needed.
     * Common subroutine for jpeg_start_decompress and jpeg_start_output.
     * Entry: global_state = DSTATE_PRESCAN only if previously suspended.
     * Exit: If done, returns TRUE and sets global_state for proper output mode.
     *       If suspended, returns FALSE and sets global_state = DSTATE_PRESCAN.
     */

    // NOTE that jpeg_start_output is only for buffered images (e.g. reading from a file, and so this
    // will never be called from that method
    public boolean output_pass_setup(jpeg_decompress_struct8 cinfo)
    {
        if (cinfo.global_state != jpegint8.DSTATE_PRESCAN)
        {
            /* First call: do pass setup */
            cinfo.master.prepare_for_output_pass(cinfo);
            cinfo.output_scanline = new int[]{0};
            cinfo.global_state = jpegint8.DSTATE_PRESCAN;
        }
        /* Loop over any required dummy passes */
        while (cinfo.master.is_dummy_pass)
        {
            if (jmorecfg8.QUANT_2PASS_SUPPORTED)
            {
                /* Crank through the dummy pass */
                while (cinfo.output_scanline[0] < cinfo.output_height)
                {
                    int last_scanline;
                    /* Call progress monitor hook if present */
                    if (cinfo.progress != null)
                    {
                        cinfo.progress.pass_counter = cinfo.output_scanline[0];
                        cinfo.progress.pass_limit = cinfo.output_height;
                        cinfo.progress.progress_monitor(cinfo);
                    }
                    /* Process some data */
                    last_scanline = cinfo.output_scanline[0];
                    cinfo.main.process_data(cinfo, null, cinfo.output_scanline, 0);
                    if (cinfo.output_scanline[0] == last_scanline)
                    {
                        return false; /* No progress made, must suspend */
                    }
                }
                /* Finish up dummy pass, and set up for another one */
                cinfo.master.finish_output_pass(cinfo);
                cinfo.master.prepare_for_output_pass(cinfo);
                cinfo.output_scanline = new int[]{0};
            }
            else
            {
                cinfo.err.ERREXIT(ErrorStrings8.JERR_NOT_COMPILED);
            }
        }
        /* Ready for application to drive output pass through
         * jpeg_read_scanlines or jpeg_read_raw_data.
         */
        cinfo.global_state = cinfo.raw_data_out ? jpegint8.DSTATE_RAW_OK : jpegint8.DSTATE_SCANNING;
        return true;
    }

    /*
     * Read some scanlines of data from the JPEG decompressor.
     *
     * The return value will be the number of lines actually read.
     * This may be less than the number requested in several cases,
     * including bottom of image, data source suspension, and operating
     * modes that emit multiple scanlines at a time.
     *
     * Note: we warn about excess calls to jpeg_read_scanlines() since
     * this likely signals an application programmer error.  However,
     * an oversize buffer (max_lines > scanlines remaining) is not an error.
     */
     public int jpeg_read_scanlines(jpeg_decompress_struct8 cinfo, byte[][] scanlines, int max_lines)
    {
        int[] row_ctr = new int[1];

        if (cinfo.global_state != jpegint8.DSTATE_SCANNING)
        {
            cinfo.err.ERREXIT1(ErrorStrings8.JERR_BAD_STATE, cinfo.global_state);
        }

        if (cinfo.output_scanline[0] >= cinfo.output_height)
        {
            cinfo.err.WARNMS(ErrorStrings8.JWRN_TOO_MUCH_DATA);
            return 0;
        }

        /* Call progress monitor hook if present */
        if (cinfo.progress != null)
        {
            cinfo.progress.pass_counter = cinfo.output_scanline[0];
            cinfo.progress.pass_limit = cinfo.output_height;
            cinfo.progress.progress_monitor(cinfo);
        }

        /* Process some data */
        cinfo.main.process_data(cinfo, scanlines, row_ctr, max_lines);
        cinfo.output_scanline[0] += row_ctr[0];
        return row_ctr[0];
    }

    /*
     * Alternate entry point to read raw data.
     * Processes exactly one iMCU row per call, unless suspended.
     */
    public int jpeg_read_raw_data(jpeg_decompress_struct8 cinfo, byte[][][] data,
                                  int max_lines)
    {
        int lines_per_iMCU_row;

        if (cinfo.global_state != jpegint8.DSTATE_RAW_OK)
            cinfo.err.ERREXIT1(ErrorStrings8.JERR_BAD_STATE, cinfo.global_state);
        if (cinfo.output_scanline[0] >= cinfo.output_height)
        {
            cinfo.err.WARNMS(ErrorStrings8.JWRN_TOO_MUCH_DATA);
            return 0;
        }

        /* Call progress monitor hook if present */
        if (cinfo.progress != null)
        {
            cinfo.progress.pass_counter = cinfo.output_scanline[0];
            cinfo.progress.pass_limit = cinfo.output_height;
            cinfo.progress.progress_monitor(cinfo);
        }

        /* Verify that at least one iMCU row can be returned. */
        lines_per_iMCU_row = (cinfo.max_v_samp_factor * cinfo.min_codec_data_unit);
        if (max_lines < lines_per_iMCU_row)
            cinfo.err.ERREXIT(ErrorStrings8.JERR_BUFFER_SIZE);

        /* Decompress directly into user's buffer. */
        if (cinfo.codec.decompress_data(cinfo, data, cinfo.main.buffer_offset) != 0)
            /* suspension forced, can do nothing more */
            return 0;

        /* OK, we processed one iMCU row. */
        cinfo.output_scanline[0] += lines_per_iMCU_row;
        return lines_per_iMCU_row;
    }

    /*
     * Initialize for an output pass in buffered-image mode.
     */
    public boolean jpeg_start_output(jpeg_decompress_struct8 cinfo, int scan_number)
    {
        if (cinfo.global_state != jpegint8.DSTATE_BUFIMAGE &&
                cinfo.global_state != jpegint8.DSTATE_PRESCAN)
            cinfo.err.ERREXIT1(ErrorStrings8.JERR_BAD_STATE, cinfo.global_state);
        /* Limit scan number to valid range */
        if (scan_number <= 0)
            scan_number = 1;
        if (cinfo.inputctl.eoi_reached &&
                scan_number > cinfo.input_scan_number)
            scan_number = cinfo.input_scan_number;
        cinfo.output_scan_number = scan_number;
        /* Perform any dummy output passes, and set up for the real pass */
        return output_pass_setup(cinfo);
    }

    /*
     * Finish up after an output pass in buffered-image mode.
     *
     * Returns FALSE if suspended.  The return value need be inspected only if
     * a suspending data source is used.
     */
    public boolean jpeg_finish_output(jpeg_decompress_struct8 cinfo)
    {
        if ((cinfo.global_state == jpegint8.DSTATE_SCANNING ||
                cinfo.global_state == jpegint8.DSTATE_RAW_OK) && cinfo.buffered_image)
        {
            /* Terminate this pass. */
            /* We do not require the whole pass to have been completed. */
            cinfo.master.finish_output_pass(cinfo);
            cinfo.global_state = jpegint8.DSTATE_BUFPOST;
        }
        else if (cinfo.global_state != jpegint8.DSTATE_BUFPOST)
        {
            /* BUFPOST = repeat call after a suspension, anything else is error */
            cinfo.err.ERREXIT1(ErrorStrings8.JERR_BAD_STATE, cinfo.global_state);
        }
        /* Read markers looking for SOS or EOI */
        while (cinfo.input_scan_number <= cinfo.output_scan_number &&
                !cinfo.inputctl.eoi_reached)
        {
            if (cinfo.inputctl.consume_input(cinfo) == jpeglib8.JPEG_SUSPENDED)
                return false;     /* Suspend, come back later */
        }
        cinfo.global_state = jpegint8.DSTATE_BUFIMAGE;
        return true;
    }
}
