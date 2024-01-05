package edu.mayo.jpeg.bit8.structs8;
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
 * The upsampler is required to set need_context_rows TRUE at global selection
 * time if so.  When need_context_rows is FALSE, this controller can simply
 * obtain one iMCU row at a time from the coefficient controller and dole it
 * out as row groups to the postprocessor.
 *
 * When need_context_rows is TRUE, this controller guarantees that the buffer
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
 * master pointer --> 0         master pointer --> 0
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

import edu.mayo.jpeg.bit8.jpegint8;
import edu.mayo.jpeg.bit8.jpeglib8;

public abstract class jpeg_d_main_controller8
{
    /* Pointer to allocated workspace (M or M+2 row groups). */
    public byte[][][] buffer = new byte[jpeglib8.MAX_COMPONENTS][][];//JSAMPARRAY buffer[jpeglib.MAX_COMPONENTS];
    public int[] buffer_offset = new int[jpeglib8.MAX_COMPONENTS]; // in leui of pointer arthimatic

    public boolean buffer_full; /* Have we gotten an iMCU row from decoder? */
    public int[] rowgroup_ctr = new int[1]; /* counts row groups output to postprocessor */

    /* Remaining fields are only used in the context case. */

    /* These are the master pointers to the funny-order pointer lists. */
    public byte[][][][] xbuffer = new byte[2][][][];//JSAMPIMAGE xbuffer[2];    /* pointers to weird pointer lists */
    public int[][] xbuffer_offset = new int[2][]; // in leui of pointer arthimatic 

    public int whichptr; /* indicates which pointer set is now in use */
    public int context_state; /* process_data state machine status */
    public int rowgroups_avail; /* row groups available to postprocessor */
    public int iMCU_row_ctr; /* counts iMCU rows to detect image top/bot */

    /* context_state values: */
    public final int CTX_PREPARE_FOR_IMCU = 0;   /* need to prepare for MCU row */
    public final int CTX_PROCESS_IMCU = 1;   /* feeding iMCU to postprocessor */
    public final int CTX_POSTPONED_ROW = 2;   /* feeding postponed row group */


    public abstract void start_pass(jpeg_decompress_struct8 cinfo, jpegint8.J_BUF_MODE pass_mode);

    //    JMETHOD(void, process_data, (j_decompress_ptr cinfo,
//	       JSAMPARRAY output_buf, JDIMENSION *out_row_ctr,
//	       JDIMENSION out_rows_avail));
    public abstract void process_data(jpeg_decompress_struct8 cinfo, byte[][] output_buf, int[] out_row_ctr, int out_rows_avail);

    public abstract void process_data_simple_main(jpeg_decompress_struct8 cinfo, byte[][] output_buf, int[] out_row_ctr,
                                                  int out_rows_avail);

    public abstract void process_data_context_main(jpeg_decompress_struct8 cinfo, byte[][] output_buf, int[] out_row_ctr,
                                                   int out_rows_avail);

    public abstract void process_data_crank_post(jpeg_decompress_struct8 cinfo, byte[][] output_buf, int[] out_row_ctr,
                                                 int out_rows_avail);

    /*
     * Initialize main buffer controller.
     */
    public abstract void jinit_d_main_controller(jpeg_decompress_struct8 cinfo, boolean need_full_buffer);
}
