package bransford.jpeg.bit8;

import bransford.jpeg.bit8.structs8.jpeg_upsampler8;
import bransford.jpeg.bit8.structs8.jpeg_decompress_struct8;

import static bransford.jpeg.bit8.jdcolor8.*;

/*
 * jdmerge.c
 *
 * Copyright (C) 1994-1996, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains code for merged upsampling/color conversion.
 *
 * This file combines functions from jdsample.c and jdcolor.c;
 * read those files first to understand what's going on.
 *
 * When the chroma components are to be upsampled by simple replication
 * (ie, box filtering), we can save some work in color conversion by
 * calculating all the output pixels corresponding to a pair of chroma
 * samples at one time.  In the conversion equations
 *  R = Y           + K1 * Cr
 *  G = Y + K2 * Cb + K3 * Cr
 *  B = Y + K4 * Cb
 * only the Y term varies among the group of pixels corresponding to a pair
 * of chroma samples, so the rest of the terms can be calculated just once.
 * At typical sampling ratios, this eliminates half or three-quarters of the
 * multiplications needed for color conversion.
 *
 * This file currently provides implementations for the following cases:
 *  YCbCr => RGB color conversion only.
 *  Sampling ratios of 2h1v or 2h2v.
 *  No scaling needed at upsample time.
 *  Corner-aligned (non-CCIR601) sampling alignment.
 * Other special cases could be added, but in most applications these are
 * the only common cases.  (For uncommon cases we fall back on the more
 * general code in jdsample.c and jdcolor.c.)
 */
public class jdmerge8 extends jpeg_upsampler8
{
    @Override
    public void start_pass(jpeg_decompress_struct8 cinfo)
    {
        start_pass_merged_upsample(cinfo);
    }

    private void start_pass_merged_upsample(jpeg_decompress_struct8 cinfo)
    {
        spare_full = false;
        rows_to_go = cinfo.output_height;
    }

    @Override
    public void jinit_upsampler(jpeg_decompress_struct8 cinfo)
    {
        need_context_rows = false;
        out_row_width = cinfo.output_width * cinfo.out_color_components;

        if (cinfo.max_v_samp_factor == 2)
        {
            upsample = UPSAMPLE_METHODS.merged_2v_upsample;
            upmethod = UPMETHODS.h2v2_merged_upsample;
            spare_row = new byte[out_row_width];
        }
        else
        {
            upsample = UPSAMPLE_METHODS.merged_1v_upsample;
            upmethod = UPMETHODS.h2v1_merged_upsample;
            spare_row = null;
        }

        build_ycc_rgb_table(cinfo);
    }

    @Override
    public void upsample(jpeg_decompress_struct8 cinfo, byte[][][] input_buf, int[] input_buf_offset,
                         int[] in_row_group_ctr, int in_row_groups_avail,
                         byte[][] outputbuf, int[] out_row_counter, int out_rows_avail)
    {
        switch (upsample)
        {
            case merged_1v_upsample ->
                    merged_1v_upsample(cinfo, input_buf, in_row_group_ctr, in_row_groups_avail, outputbuf, out_row_counter, out_rows_avail);
            case merged_2v_upsample ->
                    merged_2v_upsample(cinfo, input_buf, in_row_group_ctr, in_row_groups_avail, outputbuf, out_row_counter, out_rows_avail);
        }
    }

    @Override
    public void upmethod(jpeg_decompress_struct8 cinfo, byte[][][] input_buf, int[] in_row_group_ctr,
                         int in_row_groups_avail, byte[][] outputbuf, int[] out_row_counter,
                         int out_rows_avail)
    {
        switch (upmethod)
        {
            case h2v1_merged_upsample -> h2v1_merged_upsample(cinfo, input_buf, in_row_group_ctr, in_row_groups_avail,
                    outputbuf, out_row_counter, out_rows_avail);
            case h2v2_merged_upsample -> h2v2_merged_upsample(cinfo, input_buf, in_row_group_ctr, in_row_groups_avail,
                    outputbuf, out_row_counter, out_rows_avail);
        }
    }

    /*
     * Control routine to do upsampling (and color conversion).
     *
     * The control routine just handles the row buffering considerations.
     */
    public void merged_1v_upsample(jpeg_decompress_struct8 cinfo, byte[][][] input_buf, int[] in_row_group_ctr,
                                   int in_row_groups_avail, byte[][] outputbuf, int[] out_row_counter,
                                   int out_rows_avail)
    /* 1:1 vertical sampling case: much easier, never need a spare row. */
    {
        /* Just do the upsampling. */
        upmethod(cinfo, input_buf, in_row_group_ctr, in_row_groups_avail, outputbuf, out_row_counter, out_rows_avail);
        /* Adjust counts */
        out_row_counter[0]++;
        in_row_group_ctr[0]++;
    }

    public void merged_2v_upsample(jpeg_decompress_struct8 cinfo, byte[][][] input_buf, int[] in_row_group_ctr,
                                   int in_row_groups_avail, byte[][] outputbuf, int[] out_row_counter,
                                   int out_rows_avail)
        /* 2:1 vertical sampling case: may need a spare row. */
    {
        byte[][] work_ptrs = new byte[2][];
        int num_rows;      /* number of rows returned to caller */

        if (spare_full)
        {
            /* If we have a spare row saved from a previous cycle, just return it. */
            System.arraycopy(spare_row, 0, outputbuf[out_row_counter[0]], 0, out_row_width);
            num_rows = 1;
            spare_full = false;
        }
        else
        {
            /* Figure number of rows to return to caller. */
            num_rows = 2;
            /* Not more than the distance to the end of the image. */
            if (num_rows > rows_to_go)
                num_rows = rows_to_go;
            /* And not more than what the client can accept: */
            out_rows_avail -= out_row_counter[0];
            if (num_rows > out_rows_avail)
                num_rows = out_rows_avail;
            /* Create output pointer array for upsampler. */
            work_ptrs[0] = outputbuf[out_row_counter[0]];
            if (num_rows > 1)
            {
                work_ptrs[1] = outputbuf[out_row_counter[0] + 1];
            }
            else
            {
                work_ptrs[1] = spare_row;
                spare_full = true;
            }
            /* Now do the upsampling. */
            upmethod(cinfo, input_buf, in_row_group_ctr, in_row_groups_avail, work_ptrs,
                    out_row_counter, out_rows_avail);
        }

        /* Adjust counts */
        out_row_counter[0] += num_rows;
        rows_to_go -= num_rows;
        /* When the buffer is emptied, declare this input row group consumed */
        if (!spare_full)
            in_row_group_ctr[0]++;
    }

    /*
     * These are the routines invoked by the control routines to do
     * the actual upsampling/conversion.  One row group is processed per call.
     *
     * Note: since we may be writing directly into application-supplied buffers,
     * we have to be honest about the output width; we can't assume the buffer
     * has been rounded up to an even width.
     */

    /*
     * Upsample and color convert for the case of 2:1 horizontal and 1:1 vertical.
     */

    public void h2v1_merged_upsample(jpeg_decompress_struct8 cinfo, byte[][][] input_buf, int[] in_row_group_ctr,
                                     int in_row_groups_avail, byte[][] outputbuf, int[] out_row_counter,
                                     int out_rows_avail)
    {
        int y, cred, cgreen, cblue;
        int cb, cr;
        byte[] outptr;
        byte[] inptr0, inptr1, inptr2;
        int col;
        /* copy these pointers into registers if possible */
        byte[] range_limit = cinfo.sample_range_limit;
        int[] Crrtab = Cr_r_tab;
        int[] Cbbtab = Cb_b_tab;
        long[] Crgtab = Cr_g_tab;
        long[] Cbgtab = Cb_g_tab;

        inptr0 = input_buf[0][in_row_group_ctr[0]];
        inptr1 = input_buf[1][in_row_group_ctr[0]];
        inptr2 = input_buf[2][in_row_group_ctr[0]];
        outptr = outputbuf[0];

        int inptr0_offset = 0;
        int inptr1_offset = 0;
        int inptr2_offset = 0;
        int outptr_offset = 0;
        /* Loop for each pair of output pixels */

        for (col = cinfo.output_width >> 1; col > 0; col--)
        {
            /* Do the chroma part of the calculation */
            cb = (inptr1[inptr1_offset++] & 0xFF);
            cr = (inptr2[inptr2_offset++] & 0xFF);
            cred = Crrtab[cr];
            cgreen = jutils8.RIGHT_SHIFT((int) (Cbgtab[cb] + Crgtab[cr]), SCALEBITS);
            cblue = Cbbtab[cb];
            /* Fetch 2 Y values and emit 2 pixels */
            y = (inptr0[inptr0_offset++] & 0xFF);
            outptr[jmorecfg8.RGB_RED + outptr_offset] = range_limit[y + cred];
            outptr[jmorecfg8.RGB_GREEN + outptr_offset] = range_limit[y + cgreen];
            outptr[jmorecfg8.RGB_BLUE + outptr_offset] = range_limit[y + cblue];
            outptr_offset += jmorecfg8.RGB_PIXELSIZE; // advance 3 pixels
            y = (inptr0[inptr0_offset++] & 0xFF);
            outptr[jmorecfg8.RGB_RED + outptr_offset] = range_limit[y + cred];
            outptr[jmorecfg8.RGB_GREEN + outptr_offset] = range_limit[y + cgreen];
            outptr[jmorecfg8.RGB_BLUE + outptr_offset] = range_limit[y + cblue];
            outptr_offset += jmorecfg8.RGB_PIXELSIZE;
        }
        /* If image width is odd, do the last output column separately */
        if ((cinfo.output_width & 1) != 0)
        {
            cb = (inptr1[inptr1_offset] & 0xFF);
            cr = (inptr2[inptr2_offset] & 0xFF);
            cred = Crrtab[cr];
            cgreen = jutils8.RIGHT_SHIFT((int) (Cbgtab[cb] + Crgtab[cr]), SCALEBITS);
            cblue = Cbbtab[cb];
            y = (inptr0[inptr0_offset] & 0xFF);
            outptr[jmorecfg8.RGB_RED + outptr_offset] = range_limit[y + cred];
            outptr[jmorecfg8.RGB_GREEN + outptr_offset] = range_limit[y + cgreen];
            outptr[jmorecfg8.RGB_BLUE + outptr_offset] = range_limit[y + cblue];
        }
    }

    /*
     * Upsample and color convert for the case of 2:1 horizontal and 2:1 vertical.
     */
    public void h2v2_merged_upsample(jpeg_decompress_struct8 cinfo, byte[][][] input_buf, int[] in_row_group_ctr,
                                     int in_row_groups_avail, byte[][] outputbuf, int[] out_row_counter,
                                     int out_rows_avail)
    {
        int y, cred, cgreen, cblue;
        int cb, cr;
        byte[] outptr0, outptr1;
        byte[] inptr00, inptr01, inptr1, inptr2;
        int col;
        /* copy these pointers into registers if possible */
        byte[] range_limit = cinfo.sample_range_limit;
        int[] Crrtab = Cr_r_tab;
        int[] Cbbtab = Cb_b_tab;
        long[] Crgtab = Cr_g_tab;
        long[] Cbgtab = Cb_g_tab;

        inptr00 = input_buf[0][in_row_group_ctr[0] * 2];
        inptr01 = input_buf[0][in_row_group_ctr[0] * 2 + 1];
        inptr1 = input_buf[1][in_row_group_ctr[0]];
        inptr2 = input_buf[2][in_row_group_ctr[0]];
        outptr0 = outputbuf[0];
        outptr1 = outputbuf[1];

        int inptr00_offset = 0;
        int inptr01_offset = 0;
        int inptr1_offset = 0;
        int inptr2_offset = 0;
        int outptr0_offset = 0;
        int outptr1_offset = 0;

        /* Loop for each group of output pixels */
        for (col = cinfo.output_width >> 1; col > 0; col--)
        {
            /* Do the chroma part of the calculation */
            cb = (inptr1[inptr1_offset++] & 0xFF);
            cr = (inptr2[inptr2_offset++]);
            cred = Crrtab[cr];
            cgreen = jutils8.RIGHT_SHIFT((int) (Cbgtab[cb] + Crgtab[cr]), SCALEBITS);
            cblue = Cbbtab[cb];
            /* Fetch 4 Y values and emit 4 pixels */
            y = (inptr00[inptr00_offset++] & 0xFF);
            outptr0[jmorecfg8.RGB_RED + outptr0_offset] = range_limit[y + cred];
            outptr0[jmorecfg8.RGB_GREEN + outptr0_offset] = range_limit[y + cgreen];
            outptr0[jmorecfg8.RGB_BLUE + outptr0_offset] = range_limit[y + cblue];
            outptr0_offset += jmorecfg8.RGB_PIXELSIZE;
            y = (inptr00[inptr00_offset++] & 0xFF);
            outptr0[jmorecfg8.RGB_RED + outptr0_offset] = range_limit[y + cred];
            outptr0[jmorecfg8.RGB_GREEN + outptr0_offset] = range_limit[y + cgreen];
            outptr0[jmorecfg8.RGB_BLUE + outptr0_offset] = range_limit[y + cblue];
            outptr0_offset += jmorecfg8.RGB_PIXELSIZE;
            y = (inptr01[inptr01_offset++] & 0xFF);
            outptr1[jmorecfg8.RGB_RED + outptr1_offset] = range_limit[y + cred];
            outptr1[jmorecfg8.RGB_GREEN + outptr1_offset] = range_limit[y + cgreen];
            outptr1[jmorecfg8.RGB_BLUE + outptr1_offset] = range_limit[y + cblue];
            outptr1_offset += jmorecfg8.RGB_PIXELSIZE;
            y = (inptr01[inptr01_offset++] & 0xFF);
            outptr1[jmorecfg8.RGB_RED + outptr1_offset] = range_limit[y + cred];
            outptr1[jmorecfg8.RGB_GREEN + outptr1_offset] = range_limit[y + cgreen];
            outptr1[jmorecfg8.RGB_BLUE + outptr1_offset] = range_limit[y + cblue];
            outptr1_offset += jmorecfg8.RGB_PIXELSIZE;
        }
        /* If image width is odd, do the last output column separately */
        if ((cinfo.output_width & 1) != 0)
        {
            cb = (inptr1[inptr1_offset] & 0xFF);
            cr = (inptr2[inptr2_offset] & 0xFF);
            cred = Crrtab[cr];
            cgreen = jutils8.RIGHT_SHIFT((int)(Cbgtab[cb] + Crgtab[cr]), SCALEBITS);
            cblue = Cbbtab[cb];
            y = (inptr00[inptr00_offset] & 0xFF);
            outptr0[jmorecfg8.RGB_RED] = range_limit[y + cred];
            outptr0[jmorecfg8.RGB_GREEN] = range_limit[y + cgreen];
            outptr0[jmorecfg8.RGB_BLUE] = range_limit[y + cblue];
            y = (inptr01[inptr01_offset] & 0xFF);
            outptr1[jmorecfg8.RGB_RED] = range_limit[y + cred];
            outptr1[jmorecfg8.RGB_GREEN] = range_limit[y + cgreen];
            outptr1[jmorecfg8.RGB_BLUE] = range_limit[y + cblue];
        }
    }

    public void build_ycc_rgb_table(jpeg_decompress_struct8 cinfo)
    {
        int i;
        long x;

        Cr_r_tab = new int[jmorecfg8.MAXJSAMPLE + 1];
        Cb_b_tab = new int[jmorecfg8.MAXJSAMPLE + 1];
        Cr_g_tab = new long[jmorecfg8.MAXJSAMPLE + 1];
        Cb_g_tab = new long[jmorecfg8.MAXJSAMPLE + 1];

        for (i = 0, x = -jmorecfg8.CENTERJSAMPLE; i <= jmorecfg8.MAXJSAMPLE; i++, x++)
        {
            /* i is the actual input pixel value, in the range 0..MAXJSAMPLE */
            /* The Cb or Cr value we are thinking of is x = i - CENTERJSAMPLE */
            /* Cr=>R value is nearest int to 1.40200 * x */
            Cr_r_tab[i] = jutils8.RIGHT_SHIFT((int) (FIX(1.40200) * x + ONE_HALF), SCALEBITS);
            /* Cb=>B value is nearest int to 1.77200 * x */
            Cb_b_tab[i] = jutils8.RIGHT_SHIFT((int) (FIX(1.77200) * x + ONE_HALF), SCALEBITS);
            /* Cr=>G value is scaled-up -0.71414 * x */
            Cr_g_tab[i] = (-FIX(0.71414)) * x;
            /* Cb=>G value is scaled-up -0.34414 * x */
            /* We also add in ONE_HALF so that need not do it in inner loop */
            Cb_g_tab[i] = (-FIX(0.34414)) * x + ONE_HALF;
        }
    }
}
