package bransford.jpeg.bit12_16.dct12_16;
/*
 * jidctfst.c
 *
 * Copyright (C) 1994-1998, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains a fast, not so accurate integer implementation of the
 * inverse DCT (Discrete Cosine Transform).  In the IJG code, this routine
 * must also perform dequantization of the input coefficients.
 *
 * A 2-D IDCT can be done by 1-D IDCT on each column followed by 1-D IDCT
 * on each row (or vice versa, but it's more convenient to emit a row at
 * a time).  Direct algorithms are also available, but they are much more
 * complex and seem not to be any faster when reduced to code.
 *
 * This implementation is based on Arai, Agui, and Nakajima's algorithm for
 * scaled DCT.  Their original paper (Trans. IEICE E-71(11):1095) is in
 * Japanese, but the algorithm is described in the Pennebaker & Mitchell
 * JPEG textbook (see REFERENCES section in file README).  The following code
 * is based directly on figure 4-8 in P&M.
 * While an 8-point DCT cannot be done in less than 11 multiplies, it is
 * possible to arrange the computation so that many of the multiplies are
 * simple scalings of the final outputs.  These multiplies can then be
 * folded into the multiplications or divisions by the JPEG quantization
 * table entries.  The AA&N method leaves only 5 multiplies and 29 adds
 * to be done in the DCT itself.
 * The primary disadvantage of this method is that with fixed-point math,
 * accuracy is lost due to imprecise representation of the scaled
 * quantization values.  The smaller the quantization table entry, the less
 * precise the scaled value, so this implementation does worse with high-
 * quality-setting files than with low-quality ones.
 */

import bransford.jpeg.bit12_16.structs12_16.jpeg_component_info12_16;
import bransford.jpeg.bit12_16.structs12_16.jpeg_decompress_struct12_16;
import bransford.jpeg.bit12_16.jmorecfg12_16;
import bransford.jpeg.bit12_16.jutils12_16;

/* Scaling decisions are generally the same as in the LL&M algorithm;
 * see jidctint.c for more details.  However, we choose to descale
 * (right shift) multiplication products as soon as they are formed,
 * rather than carrying additional fractional bits into subsequent additions.
 * This compromises accuracy slightly, but it lets us save a few shifts.
 * More importantly, 16-bit arithmetic is then adequate (for 8-bit samples)
 * everywhere except in the multiplications proper; this saves a good deal
 * of work on 16-bit-int machines.
 *
 * The dequantized coefficients are not integers because the AA&N scaling
 * factors have been incorporated.  We represent them scaled up by PASS1_BITS,
 * so that the first and second IDCT rounds have the same input scaling.
 * For 8-bit JSAMPLEs, we choose IFAST_SCALE_BITS = PASS1_BITS so as to
 * avoid a descaling shift; this compromises accuracy rather drastically
 * for small quantization table entries, but it saves a lot of shifts.
 * For 12-bit JSAMPLEs, there's no hope of using 16x16 multiplies anyway,
 * so we use a much larger scaling factor to preserve accuracy.
 *
 * A final compromise is to represent the multiplicative constants to only
 * 8 fractional bits, rather than 13.  This saves some shifting work on some
 * machines, and may also reduce the cost of multiplication (since there
 * are fewer one-bits in the constants).
 */

public class jidctfst12_16 extends jdct12_16
{
    public static final int CONST_BITS = 8;
    public static int PASS1_BITS;

    public static int DCTELEMBITS = 16;

    public static int FIX_1_082392200 = 277;        /* FIX(1.082392200) */
    public static int FIX_1_414213562 = 362;    /* FIX(1.414213562) */
    public static int FIX_1_847759065 = 473;        /* FIX(1.847759065) */
    public static int FIX_2_613125930 = 669;        /* FIX(2.613125930) */

    public jidctfst12_16()
    {
        if (jmorecfg12_16.BITS_IN_JSAMPLE == 8)
        {
            PASS1_BITS = 2;
        }
        else
        {
            PASS1_BITS = 1;
        }
    }
    public static int IDESCALE(int x, int n)
    {
        return (IRIGHT_SHIFT((x) + (1 << ((n) - 1)), n));
    }

    public int MULTIPLY(int var, int const_)
    {
        return ((int) DESCALE(var * const_, CONST_BITS));
    }

    public int DESCALE(int x, int n)
    {
        return jutils12_16.RIGHT_SHIFT(x, n);
    }

    public static int IRIGHT_SHIFT(int x, int shft)
    {
        return jutils12_16.RIGHT_SHIFT(x, shft);
    }

    public static int DEQUANTIZE(int coef, int quantval)
    {
        return (coef * quantval);
    }


    // jpeg_decompress_struct cinfo, jpeg_component_info compptr, short[] coef_block, byte[][] output_buf, int outp_buf_offset, int output_col,
    public void jpeg_idct_ifast(jpeg_decompress_struct12_16 cinfo, jpeg_component_info12_16 compptr, short[] coef_block,
                                short[][] output_buf, int output_buff_offset, int output_col)
    {
        int tmp0, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7;
        int tmp10, tmp11, tmp12, tmp13;
        int z5, z10, z11, z12, z13;

        short[] inptr;
        int[] quantptr;
        int[] wsptr;
        short[] outptr;
        short[] range_limit = cinfo.sample_range_limit;
        int range_limit_offset = cinfo.sample_range_limit_offset + jmorecfg12_16.CENTERJSAMPLE;
        int ctr;
        int[] workspace = new int[DCTSIZE2];    /* buffers data between passes */

        /* Pass 1: process columns from input, store into work array. */
        inptr = coef_block;
        quantptr = compptr.dct_table;
        wsptr = workspace;

        int inptr_offset = 0, quantptr_offset = 0, wsptr_offset = 0;
        for (ctr = DCTSIZE; ctr > 0; ctr--)
        {
            /* Due to quantization, we will usually find that many of the input
             * coefficients are zero, especially the AC terms.  We can exploit this
             * by short-circuiting the IDCT calculation for any column in which all
             * the AC terms are zero.  In that case each output is equal to the
             * DC coefficient (with scale factor as needed).
             * With typical images and quantization tables, half or more of the
             * column DCT calculations can be simplified this way.
             */

            if (inptr[DCTSIZE + inptr_offset] == 0 && inptr[DCTSIZE * 2 + inptr_offset] == 0 &&
                    inptr[DCTSIZE * 3 + inptr_offset] == 0 && inptr[DCTSIZE * 4 + inptr_offset] == 0 &&
                    inptr[DCTSIZE * 5 + inptr_offset] == 0 && inptr[DCTSIZE * 6 + inptr_offset] == 0 &&
                    inptr[DCTSIZE * 7 + inptr_offset] == 0)
            {
                /* AC terms all zero */
                int dcval = DEQUANTIZE(inptr[inptr_offset], quantptr[quantptr_offset]);

                wsptr[wsptr_offset] = dcval;
                wsptr[DCTSIZE + wsptr_offset] = dcval;
                wsptr[DCTSIZE * 2 + wsptr_offset] = dcval;
                wsptr[DCTSIZE * 3 + wsptr_offset] = dcval;
                wsptr[DCTSIZE * 4 + wsptr_offset] = dcval;
                wsptr[DCTSIZE * 5 + wsptr_offset] = dcval;
                wsptr[DCTSIZE * 6 + wsptr_offset] = dcval;
                wsptr[DCTSIZE * 7 + wsptr_offset] = dcval;

                inptr_offset++;            /* advance pointers to next column */
                quantptr_offset++;
                wsptr_offset++;
                continue;
            }

            /* Even part */

            tmp0 = DEQUANTIZE(inptr[inptr_offset], quantptr[quantptr_offset]);
            tmp1 = DEQUANTIZE(inptr[DCTSIZE * 2 + inptr_offset], quantptr[DCTSIZE * 2 + quantptr_offset]);
            tmp2 = DEQUANTIZE(inptr[DCTSIZE * 4 + inptr_offset], quantptr[DCTSIZE * 4 + quantptr_offset]);
            tmp3 = DEQUANTIZE(inptr[DCTSIZE * 6 + inptr_offset], quantptr[DCTSIZE * 6 + quantptr_offset]);

            tmp10 = tmp0 + tmp2;    /* phase 3 */
            tmp11 = tmp0 - tmp2;

            tmp13 = tmp1 + tmp3;    /* phases 5-3 */
            tmp12 = MULTIPLY(tmp1 - tmp3, FIX_1_414213562) - tmp13; /* 2*c4 */

            tmp0 = tmp10 + tmp13;    /* phase 2 */
            tmp3 = tmp10 - tmp13;
            tmp1 = tmp11 + tmp12;
            tmp2 = tmp11 - tmp12;

            /* Odd part */

            tmp4 = DEQUANTIZE(inptr[DCTSIZE + inptr_offset], quantptr[DCTSIZE + quantptr_offset]);
            tmp5 = DEQUANTIZE(inptr[DCTSIZE * 3 + inptr_offset], quantptr[DCTSIZE * 3 + quantptr_offset]);
            tmp6 = DEQUANTIZE(inptr[DCTSIZE * 5 + inptr_offset], quantptr[DCTSIZE * 5 + quantptr_offset]);
            tmp7 = DEQUANTIZE(inptr[DCTSIZE * 7 + inptr_offset], quantptr[DCTSIZE * 7 + quantptr_offset]);

            z13 = tmp6 + tmp5;        /* phase 6 */
            z10 = tmp6 - tmp5;
            z11 = tmp4 + tmp7;
            z12 = tmp4 - tmp7;

            tmp7 = z11 + z13;        /* phase 5 */
            tmp11 = MULTIPLY(z11 - z13, FIX_1_414213562); /* 2*c4 */

            z5 = MULTIPLY(z10 + z12, FIX_1_847759065); /* 2*c2 */
            tmp10 = MULTIPLY(z12, FIX_1_082392200) - z5; /* 2*(c2-c6) */
            tmp12 = MULTIPLY(z10, -FIX_2_613125930) + z5; /* -2*(c2+c6) */

            tmp6 = tmp12 - tmp7;    /* phase 2 */
            tmp5 = tmp11 - tmp6;
            tmp4 = tmp10 + tmp5;

            wsptr[wsptr_offset] = (tmp0 + tmp7);
            wsptr[DCTSIZE * 7 + wsptr_offset] = (tmp0 - tmp7);
            wsptr[DCTSIZE + wsptr_offset] = (tmp1 + tmp6);
            wsptr[DCTSIZE * 6 + wsptr_offset] = (tmp1 - tmp6);
            wsptr[DCTSIZE * 2 + wsptr_offset] = (tmp2 + tmp5);
            wsptr[DCTSIZE * 5 + wsptr_offset] = (tmp2 - tmp5);
            wsptr[DCTSIZE * 4 + wsptr_offset] = (tmp3 + tmp4);
            wsptr[DCTSIZE * 3 + wsptr_offset] = (tmp3 - tmp4);

            inptr_offset++;            /* advance pointers to next column */
            quantptr_offset++;
            wsptr_offset++;
        }

        /* Pass 2: process rows from work array, store into output array. */
        /* Note that we must descale the results by a factor of 8 == 2**3, */
        /* and also undo the PASS1_BITS scaling. */

        int outptr_offset = 0;
        wsptr_offset = 0;

        for (ctr = 0; ctr < DCTSIZE; ctr++)
        {
            outptr = output_buf[ctr + output_buff_offset];
            outptr_offset = output_col;
            /* Rows of zeroes can be exploited in the same way as we did with columns.
             * However, the column calculation has created many nonzero AC terms, so
             * the simplification applies less often (typically 5% to 10% of the time).
             * On machines with very fast multiplication, it's possible that the
             * test takes more time than it's worth.  In that case this section
             * may be commented out.
             */

            if (wsptr[1 + wsptr_offset] == 0 && wsptr[2 + wsptr_offset] == 0 && wsptr[3 + wsptr_offset] == 0 && wsptr[4 + wsptr_offset] == 0 &&
                    wsptr[5 + wsptr_offset] == 0 && wsptr[6 + wsptr_offset] == 0 && wsptr[7 + wsptr_offset] == 0)
            {
                /* AC terms all zero */
                short dcval = range_limit[IDESCALE(wsptr[wsptr_offset], PASS1_BITS + 3)
                        & RANGE_MASK];

                outptr[outptr_offset] = dcval;
                outptr[1 + outptr_offset] = dcval;
                outptr[2 + outptr_offset] = dcval;
                outptr[3 + outptr_offset] = dcval;
                outptr[4 + outptr_offset] = dcval;
                outptr[5 + outptr_offset] = dcval;
                outptr[6 + outptr_offset] = dcval;
                outptr[7 + outptr_offset] = dcval;

                wsptr_offset += DCTSIZE;        /* advance pointer to next row */
                continue;
            }

            /* Even part */

            tmp10 = (wsptr[wsptr_offset] + wsptr[4 + wsptr_offset]);
            tmp11 = (wsptr[wsptr_offset] - wsptr[4 + wsptr_offset]);

            tmp13 = (wsptr[2 + wsptr_offset] + wsptr[6 + wsptr_offset]);
            tmp12 = MULTIPLY(wsptr[2 + wsptr_offset] - wsptr[6 + wsptr_offset], FIX_1_414213562)
                    - tmp13;

            tmp0 = tmp10 + tmp13;
            tmp3 = tmp10 - tmp13;
            tmp1 = tmp11 + tmp12;
            tmp2 = tmp11 - tmp12;

            /* Odd part */

            z13 = wsptr[5 + wsptr_offset] + wsptr[3 + wsptr_offset];
            z10 = wsptr[5 + wsptr_offset] - wsptr[3 + wsptr_offset];
            z11 = wsptr[1 + wsptr_offset] + wsptr[7 + wsptr_offset];
            z12 = wsptr[1 + wsptr_offset] - wsptr[7 + wsptr_offset];

            tmp7 = z11 + z13;        /* phase 5 */
            tmp11 = MULTIPLY(z11 - z13, FIX_1_414213562); /* 2*c4 */

            z5 = MULTIPLY(z10 + z12, FIX_1_847759065); /* 2*c2 */
            tmp10 = MULTIPLY(z12, FIX_1_082392200) - z5; /* 2*(c2-c6) */
            tmp12 = MULTIPLY(z10, -FIX_2_613125930) + z5; /* -2*(c2+c6) */

            tmp6 = tmp12 - tmp7;    /* phase 2 */
            tmp5 = tmp11 - tmp6;
            tmp4 = tmp10 + tmp5;

            /* Final output stage: scale down by a factor of 8 and range-limit */

            outptr[outptr_offset] = range_limit[IDESCALE(tmp0 + tmp7, PASS1_BITS + 3)
                    & RANGE_MASK];
            outptr[7 + outptr_offset] = range_limit[IDESCALE(tmp0 - tmp7, PASS1_BITS + 3)
                    & RANGE_MASK];
            outptr[1 + outptr_offset] = range_limit[IDESCALE(tmp1 + tmp6, PASS1_BITS + 3)
                    & RANGE_MASK];
            outptr[6 + outptr_offset] = range_limit[IDESCALE(tmp1 - tmp6, PASS1_BITS + 3)
                    & RANGE_MASK];
            outptr[2 + outptr_offset] = range_limit[IDESCALE(tmp2 + tmp5, PASS1_BITS + 3)
                    & RANGE_MASK];
            outptr[5 + outptr_offset] = range_limit[IDESCALE(tmp2 - tmp5, PASS1_BITS + 3)
                    & RANGE_MASK];
            outptr[4 + outptr_offset] = range_limit[IDESCALE(tmp3 + tmp4, PASS1_BITS + 3)
                    & RANGE_MASK];
            outptr[3 + outptr_offset] = range_limit[IDESCALE(tmp3 - tmp4, PASS1_BITS + 3)
                    & RANGE_MASK];

            wsptr_offset += DCTSIZE;        /* advance pointer to next row */
        }
    }
}
