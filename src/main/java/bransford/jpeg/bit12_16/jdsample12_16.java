package bransford.jpeg.bit12_16;

import bransford.jpeg.bit12_16.error12_16.ErrorStrings12_16;
import bransford.jpeg.bit12_16.structs12_16.jpeg_component_info12_16;
import bransford.jpeg.bit12_16.structs12_16.jpeg_decompress_struct12_16;
import bransford.jpeg.bit12_16.structs12_16.jpeg_upsampler12_16;

public class jdsample12_16 extends jpeg_upsampler12_16
{
    @Override
    public void start_pass(jpeg_decompress_struct12_16 cinfo)
    {
        start_pass_upsample(cinfo);
    }

    public void start_pass_upsample(jpeg_decompress_struct12_16 cinfo)
    {
        /* Mark the conversion buffer empty */
        next_row_out = cinfo.max_v_samp_factor;
        /* Initialize total-height counter for detecting bottom of image */
        rows_to_go = cinfo.output_height;
    }

    @Override
    public void jinit_upsampler(jpeg_decompress_struct12_16 cinfo)
    {
        int ci;
        jpeg_component_info12_16 compptr;
        boolean need_buffer, do_fancy;
        int h_in_group, v_in_group, h_out_group, v_out_group;

        need_context_rows = false; /* until we find out differently */

        if (cinfo.CCIR601_sampling) /* this isn't supported */
        {
           cinfo.err.ERREXIT(ErrorStrings12_16.JERR_CCIR601_NOTIMPL);
        }

        /* jdmainct.c doesn't support context rows when min_codec_data_unit = 1,
         * so don't ask for it.
         */
        do_fancy = cinfo.do_fancy_upsampling && cinfo.min_codec_data_unit > 1;

        /* Verify we can handle the sampling factors, select per-component methods,
         * and create storage as needed.
         */
        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            compptr = cinfo.comp_info[ci];
            /* Compute size of an "input group" after IDCT scaling.  This many samples
             * are to be converted to max_h_samp_factor * max_v_samp_factor pixels.
             */
            h_in_group = (compptr.h_samp_factor * compptr.codec_data_unit) / cinfo.min_codec_data_unit;
            v_in_group = (compptr.v_samp_factor * compptr.codec_data_unit) / cinfo.min_codec_data_unit;
            h_out_group = cinfo.max_h_samp_factor;
            v_out_group = cinfo.max_v_samp_factor;
            rowgroup_height[ci] = v_in_group; /* save for use later */
            need_buffer = true;

            if (!compptr.component_needed)
            {
                /* Don't bother to upsample an uninteresting component. */
                methods[ci] = UPSAMPLE_METHODS.noop_upsample;
                need_buffer = false;
            }
            else if (h_in_group == h_out_group && v_in_group == v_out_group)
            {
                /* Fullsize components can be processed without any work. */
                methods[ci] = UPSAMPLE_METHODS.fullsize_upsample;
                need_buffer = false;
            }
            else if (h_in_group * 2 == h_out_group && v_in_group == v_out_group)
            {
                /* Special cases for 2h1v upsampling */
                if (do_fancy && compptr.downsampled_width > 2)
                {
                    methods[ci] = UPSAMPLE_METHODS.h2v1_fancy_upsample;
                }
                else
                {
                    methods[ci] = UPSAMPLE_METHODS.h2v1_upsample;
                }
            }
            else if (h_in_group * 2 == h_out_group && v_in_group * 2 == v_out_group)
            {
                /* Special cases for 2h2v upsampling */
                if (do_fancy && compptr.downsampled_width > 2)
                {
                    methods[ci] = UPSAMPLE_METHODS.h2v2_fancy_upsample;
                    need_context_rows = true;
                }
                else
                {
                    methods[ci] = UPSAMPLE_METHODS.h2v2_upsample;
                }
            }
            else if ((h_out_group % h_in_group) == 0 && (v_out_group % v_in_group) == 0)
            {
                /* Generic integral-factors upsampling method */
                methods[ci] = UPSAMPLE_METHODS.int_upsample;
                h_expand[ci] = (byte) (h_out_group / h_in_group);
                v_expand[ci] = (byte) (v_out_group / v_in_group);
            }
            else
            {
                cinfo.err.ERREXIT(ErrorStrings12_16.JERR_FRACT_SAMPLE_NOTIMPL);
            }

            if (need_buffer)
            {
                color_buf[ci] = cinfo.mem.alloc_sarray(cinfo, (int) jutils12_16.jround_up(cinfo.output_width, cinfo.max_h_samp_factor),
                        cinfo.max_v_samp_factor);
            }
        }
    }

    @Override
    public void upsample(jpeg_decompress_struct12_16 cinfo, short[][][] input_buf, int[] input_buf_offset,
                         int[] in_row_group_ctr, int in_row_groups_avail,
                         short[][] outputbuf, int[] out_row_counter, int out_rows_avail)
    {
        int ci;
        jpeg_component_info12_16 compptr;
        int num_rows;

        /* Fill the conversion buffer, if it's empty */
        if (next_row_out >= cinfo.max_v_samp_factor)
        {
            for (ci = 0; ci < cinfo.num_components; ci++)
            {
                compptr = cinfo.comp_info[ci];
                /* Invoke per-component upsample method.  Notice we pass a POINTER
                 * to color_buf[ci], so that fullsize_upsample can change it.
                 */
                int offset = input_buf_offset[ci] + (in_row_group_ctr[0] * rowgroup_height[ci]);
                switch (methods[ci])
                {
                    case fullsize_upsample ->
                        fullsize_upsample(cinfo, compptr, input_buf[ci], offset, color_buf, color_buf_offset, ci);
                    case h2v1_fancy_upsample ->
                        h2v1_fancy_upsample(cinfo, compptr, input_buf[ci], offset, color_buf, color_buf_offset, ci);
                    case h2v1_upsample ->
                        h2v1_upsample(cinfo, compptr, input_buf[ci], offset, color_buf, color_buf_offset, ci);
                    case h2v2_fancy_upsample ->
                        h2v2_fancy_upsample(cinfo, compptr, input_buf[ci], offset, color_buf, color_buf_offset, ci);
                    case h2v2_upsample ->
                        h2v2_upsample(cinfo, compptr, input_buf[ci], offset, color_buf, color_buf_offset, ci);
                    case int_upsample ->
                        int_upsample(cinfo, compptr, input_buf[ci], offset, color_buf, color_buf_offset, ci);
                    case noop_upsample ->
                        noop_upsample(cinfo, compptr, input_buf[ci], offset, color_buf, color_buf_offset, ci);
                 }
            }
            next_row_out = 0;
        }

        /* Color-convert and emit rows */

        /* How many we have in the buffer: */
        num_rows = cinfo.max_v_samp_factor - next_row_out;
        /* Not more than the distance to the end of the image.  Need this test
         * in case the image height is not a multiple of max_v_samp_factor:
         */
        if (num_rows > rows_to_go)
        {
            num_rows = rows_to_go;
        }
        /* And not more than what the client can accept: */
        out_rows_avail -= out_row_counter[0];
        if (num_rows > out_rows_avail)
        {
            num_rows = out_rows_avail;
        }

        cinfo.cconvert.color_convert(cinfo, color_buf, color_buf_offset, next_row_out, outputbuf,
                out_row_counter[0], num_rows);

        /* Adjust counts */
        out_row_counter[0] += num_rows;
        rows_to_go -= num_rows;
        next_row_out += num_rows;
        /* When the buffer is emptied, declare this input row group consumed */
        if (next_row_out >= cinfo.max_v_samp_factor)
        {
            in_row_group_ctr[0]++;
        }
    }

    /*
     * These are the routines invoked by sep_upsample to upsample pixel values
     * of a single component.  One row group is processed per call.
     */

    /*
     * For full-size components, we just make color_buf[ci] point at the
     * input buffer, and thus avoid copying any data.  Note that this is
     * safe only because sep_upsample doesn't declare the input row group
     * "consumed" until we are done color converting and emitting it.
     */

     public void fullsize_upsample(jpeg_decompress_struct12_16 cinfo, jpeg_component_info12_16 compptr,
                                   short[][] input_data, int input_data_offset, short[][][] output_data_ptr, int[]
                                          output_data_offset, int output_data_index)
    {
        output_data_ptr[output_data_index] = input_data;
        output_data_offset[output_data_index] = input_data_offset;
    }

    public void noop_upsample(jpeg_decompress_struct12_16 cinfo, jpeg_component_info12_16 compptr,
                              short[][] input_data, int input_data_offset, short[][][] output_data_ptr, int[]
                                      output_data_offset, int output_data_index)
    {
        output_data_ptr[output_data_index] = null;
    }

    public void int_upsample(jpeg_decompress_struct12_16 cinfo, jpeg_component_info12_16 compptr,
                             short[][] input_data, int input_data_offset, short[][][] output_data_ptr, int[]
                                     output_data_offset, int output_data_index)
    {
        short[][] output_data = output_data_ptr[output_data_index];
        short[] inptr;
        short[] outptr;
        short invalue;
        int h;
        int outend;
        int inrow, outrow, inrow_offset, outrow_offset;

        int h_expand = cinfo.upsample.h_expand[compptr.component_index];
        int v_expand = cinfo.upsample.v_expand[compptr.component_index];

        inrow = outrow = 0;
        while (outrow < cinfo.max_v_samp_factor) {
            /* Generate one output row with proper horizontal expansion */
            inptr = input_data[inrow + input_data_offset];
            int inptr_offset = 0;

            outptr = output_data[outrow];
            int outptr_offset = 0;
            outend = outptr_offset + cinfo.output_width;
            while (outptr_offset < outend)
            {
                invalue = inptr[inptr_offset++];   /* don't need GETJSAMPLE() here */
                for (h = h_expand; h > 0; h--)
                {
                    outptr[outptr_offset++] = invalue;
                }
            }
            /* Generate any additional output rows by duplicating the first one */
            if (v_expand > 1) {
                jutils12_16.jcopy_sample_rows(output_data, outrow, output_data, outrow+1,
                        v_expand-1, cinfo.output_width);
            }
            inrow++;
            outrow += v_expand;
        }
    }

    public void h2v1_upsample(jpeg_decompress_struct12_16 cinfo, jpeg_component_info12_16 compptr,
                              short[][] input_data, int input_data_offset, short[][][] output_data_ptr, int[]
                                      output_data_offset, int output_data_index)
    {
        short[][] output_data = output_data_ptr[output_data_index];
        short[] inptr, outptr;
        short invalue;
        int outend;
        int inrow;
        output_data_offset[output_data_index] = 0;

        for (inrow = 0; inrow < cinfo.max_v_samp_factor; inrow++) {
            inptr = input_data[inrow + input_data_offset];
            int inptr_offset = 0;

            outptr = output_data[inrow ];
            int outptr_offset = 0;
            outend = outptr_offset + cinfo.output_width;
            while (outptr[outptr_offset] < outend) {
                invalue = inptr[inptr_offset++];   /* don't need GETJSAMPLE() here */
                outptr[outptr_offset++] = invalue;
                outptr[outptr_offset++] = invalue;
            }
        }
    }

    public void h2v2_upsample(jpeg_decompress_struct12_16 cinfo, jpeg_component_info12_16 compptr,
                              short[][] input_data, int input_data_offset, short[][][] output_data_ptr, int[]
                                      output_data_offset, int output_data_index)
    {
        short[][] output_data = output_data_ptr[output_data_index];
        short[] inptr, outptr;
        short invalue;
        int outend;
        int inrow, outrow;

        inrow = outrow = 0;
        while (outrow < cinfo.max_v_samp_factor)
        {
            inptr = input_data[inrow];
            outptr = output_data[outrow];
            int inptr_offset = 0;
            int outptr_offset = 0;
            outend = outptr_offset + cinfo.output_width;
            while (outptr_offset < outend)
            {
                invalue = inptr[inptr_offset++];   /* don't need GETJSAMPLE() here */
                outptr[outptr_offset++] = invalue;
                outptr[outptr_offset++] = invalue;
            }
            jutils12_16.jcopy_sample_rows(output_data, outrow, output_data, outrow+1,
                    1, cinfo.output_width);
            inrow++;
            outrow += 2;
        }

    }

    public void h2v1_fancy_upsample(jpeg_decompress_struct12_16 cinfo, jpeg_component_info12_16 compptr,
                                    short[][] input_data, int input_data_offset, short[][][] output_data_ptr, int[]
                                            output_data_offset, int output_data_index)
    {
        short[][] output_data = output_data_ptr[output_data_index];
        short[] inptr, outptr;
        short invalue;
        int colctr;
        int inrow;

        for (inrow = 0; inrow < cinfo.max_v_samp_factor; inrow++)
        {
            inptr = input_data[inrow];
            outptr = output_data[inrow];
            int inptr_offset = 0;
            int outptr_offset = 0;
            /* Special case for first column */
            invalue = (short) (inptr[inptr_offset++] & 0xFFFF);
            outptr[outptr_offset++] = invalue;
            outptr[outptr_offset++] = (short) ((invalue * 3 + (inptr[inptr_offset] & 0xFFFF) + 2) >> 2);

            for (colctr = compptr.downsampled_width - 2; colctr > 0; colctr--)
            {
                /* General case: 3/4 * nearer pixel + 1/4 * further pixel */
                invalue = (short) ((inptr[inptr_offset++] & 0xFFFF) * 3);
                outptr[outptr_offset++] = (short) ((invalue + (inptr[inptr_offset -2] & 0xFFFF) + 1) >> 2);
                outptr[outptr_offset++] = (short) ((invalue + (inptr[inptr_offset] & 0xFFFF) + 2) >> 2);
            }

            /* Special case for last column */
            invalue = (short) (inptr[inptr_offset] & 0xFFFF);
            outptr[outptr_offset++] = (short) ((invalue * 3 + (inptr[inptr_offset -1] & 0xFFFF) + 1) >> 2);
            outptr[outptr_offset] = invalue;
        }

    }

    public void h2v2_fancy_upsample(jpeg_decompress_struct12_16 cinfo, jpeg_component_info12_16 compptr,
                                    short[][] input_data, int input_data_offset, short[][][] output_data_ptr, int[]
                                            output_data_offset, int output_data_index)
    {
        // JSAMPARRAY output_data = *output_data_ptr;
        short[] inptr0, inptr1, outptr;
        int thiscolsum, lastcolsum, nextcolsum;
        int colctr;
        int inrow, outrow, v;

        inrow = outrow = 0;
        while (outrow < cinfo.max_v_samp_factor)
        {
            for (v = 0; v < 2; v++) {
                /* inptr0 points to nearest input row, inptr1 points to next nearest */
                inptr0 = input_data[inrow + input_data_offset];
                if (v == 0)       /* next nearest is row above */
                    inptr1 = input_data[inrow - 1 + input_data_offset];
                else          /* next nearest is row below */
                    inptr1 = input_data[inrow + 1 + input_data_offset];
                outptr = output_data_ptr[output_data_index][outrow++];

                int inptr0_offset = 0, inptr1_offset = 0, outptr_offset = 0;
                /* Special case for first column */
                thiscolsum = (inptr0[inptr0_offset++] & 0xFFFF) * 3 + (inptr1[inptr1_offset++] & 0xFFFF);
                nextcolsum = (inptr0[inptr0_offset++] & 0xFFFF) * 3 + (inptr1[inptr1_offset++] & 0xFFFF);

                outptr[outptr_offset++] = (short) ((thiscolsum * 4 + 8) >> 4);
                outptr[outptr_offset++] = (short) ((thiscolsum * 3 + nextcolsum + 7) >> 4);
                lastcolsum = thiscolsum; thiscolsum = nextcolsum;

                for (colctr = compptr.downsampled_width - 2; colctr > 0; colctr--) {
                    /* General case: 3/4 * nearer pixel + 1/4 * further pixel in each */
                    /* dimension, thus 9/16, 3/16, 3/16, 1/16 overall */
                    nextcolsum = (inptr0[inptr0_offset++] & 0xFFFF) * 3 + (inptr1[inptr1_offset++] & 0xFFFF);
                    outptr[outptr_offset++] = (short) ((thiscolsum * 3 + lastcolsum + 8) >> 4);
                    outptr[outptr_offset++] = (short) ((thiscolsum * 3 + nextcolsum + 7) >> 4);
                    lastcolsum = thiscolsum; thiscolsum = nextcolsum;
                }

                /* Special case for last column */
                outptr[outptr_offset++] = (short) ((thiscolsum * 3 + lastcolsum + 8) >> 4);
                outptr[outptr_offset] = (short) ((thiscolsum * 4 + 7) >> 4);
            }
            inrow++;
        }
    }

    @Override
    public void upmethod(jpeg_decompress_struct12_16 cinfo, short[][][] input_buf, int[] in_row_group_ctr,
                         int in_row_groups_avail, short[][] outputbuf, int[] out_row_counter,
                         int out_rows_avail)
    {
        // no-op
    }

}
