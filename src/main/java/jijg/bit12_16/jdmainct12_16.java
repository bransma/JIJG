package jijg.bit12_16;
/*
 * jdmainct.c
 *
 * Copyright (C) 1994-1998, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains the main buffer controller for decompression.
 * The main buffer lies between the JPEG decompressor proper and the
 * post-processor; it holds downsampled data in the JPEG colorspace.
 *
 * Note that this code is bypassed in raw-data mode, since the application
 * supplies the equivalent of the main buffer in that case.
 */

import jijg.bit12_16.error12_16.ErrorStrings12_16;
import jijg.bit12_16.structs12_16.jpeg_component_info12_16;
import jijg.bit12_16.structs12_16.jpeg_d_main_controller12_16;
import jijg.bit12_16.structs12_16.jpeg_decompress_struct12_16;

/*
 * In the current system design, the main buffer need never be a full-image
 * buffer; any full-height buffers will be found inside the coefficient or
 * postprocessing controllers.  Nonetheless, the main controller is not
 * trivial.  Its responsibility is to provide context rows for upsampling/
 * rescaling, and doing this in an efficient fashion is a bit tricky.
 *
 * Postprocessor input data is counted in "row groups".  A row group
 * is defined to be (v_samp_factor * codec_data_unit / min_codec_data_unit)
 * sample rows of each component.  (We require codec_data_unit values to be
 * chosen such that these numbers are integers.  In practice codec_data_unit
 * values will likely be powers of two, so we actually have the stronger
 * condition that codec_data_unit / min_codec_data_unit is an integer.)
 * Upsampling will typically produce max_v_samp_factor pixel rows from each
 * row group (times any additional scale factor that the upsampler is
 * applying).
 *
 * The decompression codec will deliver data to us one iMCU row at a time;
 * each iMCU row contains v_samp_factor * codec_data_unit sample rows, or
 * exactly min_codec_data_unit row groups.  (This amount of data corresponds
 * to one row of MCUs when the image is fully interleaved.)  Note that the
 * number of sample rows varies across components, but the number of row
 * groups does not.  Some garbage sample rows may be included in the last iMCU
 * row at the bottom of the image.
 *
 * Depending on the vertical scaling algorithm used, the upsampler may need
 * access to the sample row(s) above and below its current input row group.
 * The upsampler is required to set need_context_rows true at global selection
 * time if so.  When need_context_rows is false, this controller can simply
 * obtain one iMCU row at a time from the coefficient controller and dole it
 * out as row groups to the postprocessor.
 *
 * When need_context_rows is true, this controller guarantees that the buffer
 * passed to postprocessing contains at least one row group's worth of samples
 * above and below the row group(s) being processed.  Note that the context
 * rows "above" the first passed row group appear at negative row offsets in
 * the passed buffer.  At the top and bottom of the image, the required
 * context rows are manufactured by duplicating the first or last real sample
 * row; this avoids having special cases in the upsampling inner loops.
 *
 * The amount of context is fixed at one row group just because that's a
 * convenient number for this controller to work with.  The existing
 * upsamplers really only need one sample row of context.  An upsampler
 * supporting arbitrary output rescaling might wish for more than one row
 * group of context when shrinking the image; tough, we don't handle that.
 * (This is justified by the assumption that downsizing will be handled mostly
 * by adjusting the codec_data_unit values, so that the actual scale factor at
 * the upsample step needn't be much less than one.)
 *
 * To provide the desired context, we have to retain the last two row groups
 * of one iMCU row while reading in the next iMCU row.  (The last row group
 * can't be processed until we have another row group for its below-context,
 * and so we have to save the next-to-last group too for its above-context.)
 * We could do this most simply by copying data around in our buffer, but
 * that'd be very slow.  We can avoid copying any data by creating a rather
 * strange pointer structure.  Here's how it works.  We allocate a workspace
 * consisting of M+2 row groups (where M = min_codec_data_unit is the number
 * of row groups per iMCU row).  We create two sets of redundant pointers to
 * the workspace.  Labeling the physical row groups 0 to M+1, the synthesized
 * pointer lists look like this:
 *                   M+1                          M-1
 * master pointer -. 0         master pointer -. 0
 *                    1                            1
 *                   ...                          ...
 *                   M-3                          M-3
 *                   M-2                           M
 *                   M-1                          M+1
 *                    M                           M-2
 *                   M+1                          M-1
 *                    0                            0
 * We read alternate iMCU rows using each master pointer; thus the last two
 * row groups of the previous iMCU row remain un-overwritten in the workspace.
 * The pointer lists are set up so that the required context rows appear to
 * be adjacent to the proper places when we pass the pointer lists to the
 * upsampler.
 *
 * The above pictures describe the normal state of the pointer lists.
 * At top and bottom of the image, we diddle the pointer lists to duplicate
 * the first or last sample row as necessary (this is cheaper than copying
 * sample rows around).
 *
 * This scheme breaks down if M < 2, ie, min_codec_data_unit is 1.  In that
 * situation each iMCU row provides only one row group so the buffering logic
 * must be different (eg, we must read two iMCU rows before we can emit the
 * first row group).  For now, we simply do not support providing context
 * rows when min_codec_data_unit is 1.  That combination seems unlikely to
 * be worth providing --- if someone wants a 1/8th-size preview, they probably
 * want it quick and dirty, so a context-free upsampler is sufficient.
 */

public class jdmainct12_16 extends jpeg_d_main_controller12_16
{
    public enum PROCESS_DATA
    {
        process_data_simple_main, process_data_context_main, process_data_crank_post
    }

    public PROCESS_DATA process_data = PROCESS_DATA.process_data_context_main;

    public void alloc_funny_pointers(jpeg_decompress_struct12_16 cinfo)
        /* Allocate space for the funny pointer lists.
         * This is done only once, not once per pass.
         */
    {
        //my_main_ptr mymain = (my_main_ptr) cinfo.main;
        int ci, rgroup;
        int M = cinfo.min_codec_data_unit;
        jpeg_component_info12_16 compptr;
        short[][] xbuf;
        // byte[][] xbuf;

        /* Get top-level space for component array pointers.
         * We alloc both arrays with one call to save a few cycles (hence *2, drop it in Java and do seperate new's)
         */
        xbuffer[0] = new short[cinfo.num_components][][];//(byte[][][]) cinfo.mem.alloc_small(cinfo, cinfo.num_components * 2);// * SIZEOF(byte[][]));
        xbuffer[1] = new short[cinfo.num_components][][];
        xbuffer_offset[0] = new int[cinfo.num_components];
        xbuffer_offset[1] = new int[cinfo.num_components];

        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            compptr = cinfo.comp_info[ci];

            /* height of a row group of component */
            /* Get space for pointer lists --- M+4 row groups in each list.
             * We alloc both pointer lists with one call to save a few cycles.
             */
            rgroup = (compptr.v_samp_factor * compptr.codec_data_unit)
                    / cinfo.min_codec_data_unit;
            xbuf = new short[2 * (rgroup * (M + 4))][];
            int offset = rgroup;

            // In the C-world xbuf, being a pointer, means that xbuffer[0][ci] points at xbuf + rgroup memory location
            // In java keep track of these locations via an offset array

            xbuffer_offset[0][ci] = offset;
            xbuffer[0][ci] = xbuf;

            offset += rgroup * (M + 4);
            xbuffer_offset[1][ci] = offset;
            xbuffer[1][ci] = xbuf;
        }
    }

    public void make_funny_pointers(jpeg_decompress_struct12_16 cinfo)
        /* Create the funny pointer lists discussed in the comments above.
         * The actual workspace is already allocated (in main.buffer),
         * and the space for the pointer lists is allocated too.
         * This routine just fills in the curiously ordered lists.
         * This will be repeated at the beginning of each pass.
         */
    {
        int ci, i, rgroup;
        int M = cinfo.min_codec_data_unit;
        jpeg_component_info12_16 compptr;
        short[][] buf, xbuf0, xbuf1;
        // byte[][] buf, xbuf0, xbuf1;

        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            compptr = cinfo.comp_info[ci];
            rgroup = (compptr.v_samp_factor * compptr.codec_data_unit)
                    / cinfo.min_codec_data_unit; /* height of a row group of component */
            xbuf0 = xbuffer[0][ci];
            int xbuf0_offset = xbuffer_offset[0][ci];
            xbuf1 = xbuffer[1][ci];
            int xbuf1_offset = xbuffer_offset[1][ci];

            /* First copy the workspace pointers as-is */
            buf = buffer[ci];
            for (i = 0; i < rgroup * (M + 2); i++)
            {
                xbuf0[i + xbuf0_offset] = xbuf1[i + xbuf1_offset] = buf[i];
            }

            /* In the second list, put the last four row groups in swapped order */
            for (i = 0; i < rgroup * 2; i++)
            {
                xbuf1[rgroup * (M - 2) + i + xbuf1_offset] = buf[rgroup * M + i];
                xbuf1[rgroup * M + i + xbuf1_offset] = buf[rgroup * (M - 2) + i];
            }

            /* The wraparound pointers at top and bottom will be filled later
             * (see set_wraparound_pointers, below).  Initially we want the "above"
             * pointers to duplicate the first actual data line.  This only needs
             * to happen in xbuffer[0].
             */
            for (i = 0; i < rgroup; i++)
            {
                xbuf0[i - rgroup + xbuf0_offset] = xbuf0[xbuf0_offset];
            }
        }
    }

    public void set_wraparound_pointers(jpeg_decompress_struct12_16 cinfo)
        /* Set up the "wraparound" pointers at top and bottom of the pointer lists.
         * This changes the pointer list state from top-of-image to the normal state.
         */
    {
        //my_main_ptr mymain = (my_main_ptr) cinfo.main;
        int ci, i, rgroup;
        int M = cinfo.min_codec_data_unit;
        jpeg_component_info12_16 compptr;
        short[][] xbuf0, xbuf1;
        // byte[][] xbuf0, xbuf1;

        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            compptr = cinfo.comp_info[ci];
            rgroup = (compptr.v_samp_factor * compptr.codec_data_unit)
                    / cinfo.min_codec_data_unit; /* height of a row group of component */
            xbuf0 = xbuffer[0][ci];
            int xbuf0_offset = xbuffer_offset[0][ci];
            xbuf1 = xbuffer[1][ci];
            int xbuf1_offset = xbuffer_offset[1][ci];
            for (i = 0; i < rgroup; i++)
            {
                xbuf0[i - rgroup + xbuf0_offset] = xbuf0[rgroup * (M + 1) + i + xbuf0_offset];
                xbuf1[i - rgroup + xbuf1_offset] = xbuf1[rgroup * (M + 1) + i + xbuf1_offset];
                xbuf0[rgroup * (M + 2) + i + xbuf0_offset] = xbuf0[i + xbuf0_offset];
                xbuf1[rgroup * (M + 2) + i + xbuf1_offset] = xbuf1[i + xbuf1_offset];
            }
        }
    }

    public void set_bottom_pointers(jpeg_decompress_struct12_16 cinfo)
        /* Change the pointer lists to duplicate the last sample row at the bottom
         * of the image.  whichptr indicates which xbuffer holds the final iMCU row.
         * Also sets rowgroups_avail to indicate number of nondummy row groups in row.
         */
    {
        int ci, i, rgroup, iMCUheight, rows_left;
        jpeg_component_info12_16 compptr;
        short[][] xbuf;
        // byte[][] xbuf;

        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            compptr = cinfo.comp_info[ci];

            /* Count sample rows in one iMCU row and in one row group */
            iMCUheight = compptr.v_samp_factor * compptr.codec_data_unit;
            rgroup = iMCUheight / cinfo.min_codec_data_unit;

            /* Count nondummy sample rows remaining for this component */
            rows_left = compptr.downsampled_height % iMCUheight;
            if (rows_left == 0)
            {
                rows_left = iMCUheight;
            }

            /* Count nondummy row groups.  Should get same answer for each component,
             * so we need only do it once.
             */
            if (ci == 0)
            {
                rowgroups_avail = (rows_left - 1) / rgroup + 1;
            }
            /* Duplicate the last real sample row rgroup*2 times; this pads out the
             * last partial rowgroup and ensures at least one full rowgroup of context.
             */
            xbuf = xbuffer[whichptr][ci];
            int xbuf_offset = xbuffer_offset[whichptr][ci];
            for (i = 0; i < rgroup * 2; i++)
            {
                xbuf[rows_left + i + xbuf_offset] = xbuf[rows_left - 1 + xbuf_offset];
            }
        }
    }

    public void start_pass(jpeg_decompress_struct12_16 cinfo, jpegint12_16.J_BUF_MODE pass_mode)
    {
        start_pass_main(cinfo, pass_mode);
    }

    public void start_pass_main(jpeg_decompress_struct12_16 cinfo, jpegint12_16.J_BUF_MODE pass_mode)
    {
        switch (pass_mode)
        {
            case JBUF_PASS_THRU:
            {
                if (cinfo.upsample.need_context_rows)
                {
                    process_data = PROCESS_DATA.process_data_context_main;
                    make_funny_pointers(cinfo); /* Create the xbuffer[] lists */
                    whichptr = 0; /* Read first iMCU row into xbuffer[0] */
                    context_state = CTX_PREPARE_FOR_IMCU;
                    iMCU_row_ctr = 0;
                }
                else
                {
                    /* Simple case with no context needed */
                    process_data = PROCESS_DATA.process_data_simple_main;
                }
                buffer_full = false; /* Mark buffer empty */
                rowgroup_ctr[0] = 0;
                break;
            }
            case JBUF_CRANK_DEST:
            {
                if (jmorecfg12_16.QUANT_2PASS_SUPPORTED)
                {
                    /* For last pass of 2-pass quantization, just crank the postprocessor */
                    process_data = PROCESS_DATA.process_data_crank_post;
                }
                else
                {
                    cinfo.err.ERREXIT(ErrorStrings12_16.JERR_BAD_BUFFER_MODE);
                }
                break;
            }
        }
    }

    // public void process_data(jpeg_decompress_struct cinfo, byte[][] output_buf, int[] out_row_ctr, int out_rows_avail)
    public void process_data(jpeg_decompress_struct12_16 cinfo, short[][] output_buf, int[] out_row_ctr, int out_rows_avail)
    {
        switch (process_data)
        {
            case process_data_context_main:
                process_data_context_main(cinfo, output_buf, out_row_ctr, out_rows_avail);
                break;
            case process_data_crank_post:
                process_data_crank_post(cinfo, output_buf, out_row_ctr, out_rows_avail);
                break;
            case process_data_simple_main:
                process_data_simple_main(cinfo, output_buf, out_row_ctr, out_rows_avail);
                break;
        }
    }

     // public void process_data_simple_main(jpeg_decompress_struct cinfo, byte[][] output_buf, int[] out_row_ctr, int out_rows_avail)
     public void process_data_simple_main(jpeg_decompress_struct12_16 cinfo, short[][] output_buf, int[] out_row_ctr, int out_rows_avail)
    {
        int rowgroups_avail;

        /* Read input data if we haven't filled the main buffer yet */
        if (!buffer_full)
        {
            if (cinfo.codec.decompress_data(cinfo, buffer, buffer_offset) == 0)
            {
                return; /* suspension forced, can do nothing more */
            }
            buffer_full = true; /* OK, we have an iMCU row to work with */
        }

        /* There are always min_codec_data_unit row groups in an iMCU row. */
        rowgroups_avail = cinfo.min_codec_data_unit;
        /* Note: at the bottom of the image, we may pass extra garbage row groups
         * to the postprocessor.  The postprocessor has to check for bottom
         * of image anyway (at row resolution), so no point in us doing it too.
         */

        /* Feed the postprocessor */
        cinfo.post.post_process_data(cinfo, buffer, buffer_offset, rowgroup_ctr,
                rowgroups_avail, output_buf, out_row_ctr, out_rows_avail);

        /* Has postprocessor consumed all the data yet? If so, mark buffer empty */
        if (rowgroup_ctr[0] >= rowgroups_avail)
        {
            buffer_full = false;
            rowgroup_ctr[0] = 0;
        }
    }

    /*
     * Process some data.
     * This handles the case where context rows must be provided.
     */

    //    process_data_context_main (jpeg_decompress_struct cinfo,
    //                   JSAMPARRAY output_buf, int *out_row_ctr,
    //                   int out_rows_avail)
    // public void process_data_context_main(jpeg_decompress_struct cinfo, byte[][] output_buf, int[] out_row_ctr, int out_rows_avail)
    public void process_data_context_main(jpeg_decompress_struct12_16 cinfo, short[][] output_buf, int[] out_row_ctr, int out_rows_avail)
    {
        /* Read input data if we haven't filled the main buffer yet */
        if (!buffer_full)
        {
            if (cinfo.codec.decompress_data(cinfo, xbuffer[whichptr], xbuffer_offset[whichptr]) == 0)
            {
                return; /* suspension forced, can do nothing more */
            }
            buffer_full = true; /* OK, we have an iMCU row to work with */
            iMCU_row_ctr++; /* count rows received */
        }

        /* Postprocessor typically will not swallow all the input data it is handed
         * in one call (due to filling the output buffer first).  Must be prepared
         * to exit and restart.  This switch lets us keep track of how far we got.
         * Note that each case falls through to the next on successful completion.
         */
        switch (context_state)
        {
            case CTX_POSTPONED_ROW:
            {
                /* Call postprocessor using previously set pointers for postponed row */
                cinfo.post.post_process_data(cinfo, xbuffer[whichptr], xbuffer_offset[whichptr],
                        rowgroup_ctr, rowgroups_avail, output_buf,
                        out_row_ctr, out_rows_avail);
                if (rowgroup_ctr[0] < rowgroups_avail)
                {
                    return; /* Need to suspend */
                }

                context_state = CTX_PREPARE_FOR_IMCU;
                if (out_row_ctr[0] >= out_rows_avail)
                {
                    return; /* Postprocessor exactly filled output buf */
                }
                /*FALLTHROUGH*/
            }
            case CTX_PREPARE_FOR_IMCU:
            {
                /* Prepare to process first M-1 row groups of this iMCU row */
                rowgroup_ctr[0] = 0;
                rowgroups_avail = cinfo.min_codec_data_unit - 1;
                /* Check for bottom of image: if so, tweak pointers to "duplicate"
                 * the last sample row, and adjust rowgroups_avail to ignore padding rows.
                 */
                if (iMCU_row_ctr == cinfo.total_iMCU_rows)
                {
                    set_bottom_pointers(cinfo);
                }
                context_state = CTX_PROCESS_IMCU;
            }
            /*FALLTHROUGH*/
            case CTX_PROCESS_IMCU:
            {
                /* Call postprocessor using previously set pointers */
                cinfo.post.post_process_data(cinfo, xbuffer[whichptr],  xbuffer_offset[whichptr],
                        rowgroup_ctr, rowgroups_avail, output_buf, out_row_ctr, out_rows_avail);
                if (rowgroup_ctr[0] < rowgroups_avail)
                {
                    return; /* Need to suspend */
                }
                /* After the first iMCU, change wraparound pointers to normal state */
                if (iMCU_row_ctr == 1)
                {
                    set_wraparound_pointers(cinfo);
                }
                /* Prepare to load new iMCU row using other xbuffer list */
                whichptr ^= 1; /* 0=>1 or 1=>0 */
                buffer_full = false;
                /* Still need to process last row group of this iMCU row, */
                /* which is saved at index M+1 of the other xbuffer */
                rowgroup_ctr[0] = cinfo.min_codec_data_unit + 1;
                rowgroups_avail = cinfo.min_codec_data_unit + 2;
                context_state = CTX_POSTPONED_ROW;
            }
        }
    }

    /*
     * Process some data.
     * Final pass of two-pass quantization: just call the postprocessor.
     * Source data will be the postprocessor controller's internal buffer.
     */
     // public void process_data_crank_post(jpeg_decompress_struct cinfo, byte[][] output_buf, int[] out_row_ctr, int out_rows_avail)
    public void process_data_crank_post(jpeg_decompress_struct12_16 cinfo, short[][] output_buf, int[] out_row_ctr, int out_rows_avail)
    {
        cinfo.post.post_process_data(cinfo, null, null,
                null, 0, output_buf, out_row_ctr, out_rows_avail);
    }

    /*
     * Initialize main buffer controller.
     */
    public void jinit_d_main_controller(jpeg_decompress_struct12_16 cinfo, boolean need_full_buffer)
    {
        int ci, rgroup, ngroups;
        jpeg_component_info12_16 compptr;

        // as best I can tell this is the only possibility
        // start_pass = start_pass_main;

        /* Allocate the workspace.
         * ngroups is the number of row groups we need.
         */
        if (cinfo.upsample.need_context_rows)
        {
            if (cinfo.min_codec_data_unit < 2) /* unsupported, see comments above */
              cinfo.err.ERREXIT(ErrorStrings12_16.JERR_NOTIMPL);

            alloc_funny_pointers(cinfo); /* Alloc space for xbuffer[] lists */
            ngroups = cinfo.min_codec_data_unit + 2;
        }
        else
        {
            ngroups = cinfo.min_codec_data_unit;
        }

        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            compptr = cinfo.comp_info[ci];
            rgroup = (compptr.v_samp_factor * compptr.codec_data_unit)
                    / cinfo.min_codec_data_unit; /* height of a row group of component */
            buffer[ci] = cinfo.mem.alloc_sarray(cinfo, compptr.width_in_data_units * compptr.codec_data_unit, (rgroup * ngroups));
        }
    }
}
