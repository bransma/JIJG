package bransford.jpeg.bit12_16.dct12_16;

import bransford.jpeg.bit12_16.structs12_16.jpeg_component_info12_16;
import bransford.jpeg.bit12_16.structs12_16.jpeg_decompress_struct12_16;
import bransford.jpeg.bit12_16.jmorecfg12_16;

/*
 * jidctint.c
 *
 * Copyright (C) 1991-1998, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains a slow-but-accurate integer implementation of the
 * inverse DCT (Discrete Cosine Transform).  In the IJG code, this routine
 * must also perform dequantization of the input coefficients.
 *
 * A 2-D IDCT can be done by 1-D IDCT on each column followed by 1-D IDCT
 * on each row (or vice versa, but it's more convenient to emit a row at
 * a time).  Direct algorithms are also available, but they are much more
 * complex and seem not to be any faster when reduced to code.
 *
 * This implementation is based on an algorithm described in
 *   C. Loeffler, A. Ligtenberg and G. Moschytz, "Practical Fast 1-D DCT
 *   Algorithms with 11 Multiplications", Proc. Int'l. Conf. on Acoustics,
 *   Speech, and Signal Processing 1989 (ICASSP '89), pp. 988-991.
 * The primary algorithm described there uses 11 multiplies and 29 adds.
 * We use their alternate method with 12 multiplies and 32 adds.
 * The advantage of this method is that no data path contains more than one
 * multiplication; this allows a very simple and accurate implementation in
 * scaled fixed-point arithmetic, with a minimal number of shifts.
 */

public class jidctint12_16 extends jdct12_16
{
    public static final int CONST_BITS = 13;
    public static int PASS1_BITS;

    public static final int FIX_0_298631336 = 2446;    /* FIX(0.298631336) */
    public static final int FIX_0_390180644 = 3196;    /* FIX(0.390180644) */
    public static final int FIX_0_541196100 = 4433;    /* FIX(0.541196100) */
    public static final int FIX_0_765366865 = 6270;    /* FIX(0.765366865) */
    public static final int FIX_0_899976223 = 7373;    /* FIX(0.899976223) */
    public static final int FIX_1_175875602 = 9633;    /* FIX(1.175875602) */
    public static final int FIX_1_501321110 = 12299;    /* FIX(1.501321110) */
    public static final int FIX_1_847759065 = 15137;    /* FIX(1.847759065) */
    public static final int FIX_1_961570560 = 16069;    /* FIX(1.961570560) */
    public static final int FIX_2_053119869 = 16819;    /* FIX(2.053119869) */
    public static final int FIX_2_562915447 = 20995;    /* FIX(2.562915447) */
    public static final int FIX_3_072711026 = 25172;    /* FIX(3.072711026) */

    public jidctint12_16()
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

    public static int MULTIPLY(int var, int constant)
    {
        return var * constant;
    }

    public int DESCALE(int x, int n)
    {
        return (((x) + (1 << ((n) - 1)) >> n));
    }

    public int DEQUANTIZE(int coef, int quantval)
    {
        return MULTIPLY(coef, quantval);
    }

    // jpeg_decompress_struct cinfo, jpeg_component_info compptr, short[] coef_block, byte[][] output_buf,
    // int outp_buf_offset, int output_col,
    public void jpeg_idct_islow(jpeg_decompress_struct12_16 cinfo, jpeg_component_info12_16 compptr, short[] coef_block,
                                short[][] output_buf, int output_buff_offset,
                                int output_col)
    {
        int tmp0, tmp1, tmp2, tmp3;
        int tmp10, tmp11, tmp12, tmp13;
        int z1, z2, z3, z4, z5;
        short[] inptr;
        int[] quantptr;
        int[] wsptr;
        short[] outptr;
        short[] range_limit = cinfo.sample_range_limit;
        int range_limit_offset = cinfo.sample_range_limit_offset + jmorecfg12_16.CENTERJSAMPLE;
        int ctr;
        int[] workspace = new int[DCTSIZE2];    /* buffers data between passes */

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
                int dcval = DEQUANTIZE(inptr[inptr_offset], quantptr[quantptr_offset]) << PASS1_BITS;

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

            /* Even part: reverse the even part of the forward DCT. */
            /* The rotator is sqrt(2)*c(-6). */

            z2 = DEQUANTIZE(inptr[DCTSIZE * 2 + inptr_offset], quantptr[DCTSIZE * 2 + quantptr_offset]);
            z3 = DEQUANTIZE(inptr[DCTSIZE * 6 + inptr_offset], quantptr[DCTSIZE * 6 + quantptr_offset]);

            z1 = MULTIPLY(z2 + z3, FIX_0_541196100);
            tmp2 = z1 + MULTIPLY(z3, -FIX_1_847759065);
            tmp3 = z1 + MULTIPLY(z2, FIX_0_765366865);

            z2 = DEQUANTIZE(inptr[inptr_offset], quantptr[quantptr_offset]);
            z3 = DEQUANTIZE(inptr[DCTSIZE * 4 + inptr_offset], quantptr[DCTSIZE * 4 + quantptr_offset]);

            tmp0 = (z2 + z3) << CONST_BITS;
            tmp1 = (z2 - z3) << CONST_BITS;

            tmp10 = tmp0 + tmp3;
            tmp13 = tmp0 - tmp3;
            tmp11 = tmp1 + tmp2;
            tmp12 = tmp1 - tmp2;

            /* Odd part per figure 8; the matrix is unitary and hence its
             * transpose is its inverse.  i0..i3 are y7,y5,y3,y1 respectively.
             */

            tmp0 = DEQUANTIZE(inptr[DCTSIZE * 7 + inptr_offset], quantptr[DCTSIZE * 7 + quantptr_offset]);
            tmp1 = DEQUANTIZE(inptr[DCTSIZE * 5 + inptr_offset], quantptr[DCTSIZE * 5 + quantptr_offset]);
            tmp2 = DEQUANTIZE(inptr[DCTSIZE * 3 + inptr_offset], quantptr[DCTSIZE * 3 + quantptr_offset]);
            tmp3 = DEQUANTIZE(inptr[DCTSIZE + inptr_offset], quantptr[DCTSIZE + quantptr_offset]);

            z1 = tmp0 + tmp3;
            z2 = tmp1 + tmp2;
            z3 = tmp0 + tmp2;
            z4 = tmp1 + tmp3;
            z5 = MULTIPLY(z3 + z4, FIX_1_175875602); /* sqrt(2) * c3 */

            tmp0 = MULTIPLY(tmp0, FIX_0_298631336); /* sqrt(2) * (-c1+c3+c5-c7) */
            tmp1 = MULTIPLY(tmp1, FIX_2_053119869); /* sqrt(2) * ( c1+c3-c5+c7) */
            tmp2 = MULTIPLY(tmp2, FIX_3_072711026); /* sqrt(2) * ( c1+c3+c5-c7) */
            tmp3 = MULTIPLY(tmp3, FIX_1_501321110); /* sqrt(2) * ( c1+c3-c5-c7) */
            z1 = MULTIPLY(z1, -FIX_0_899976223); /* sqrt(2) * (c7-c3) */
            z2 = MULTIPLY(z2, -FIX_2_562915447); /* sqrt(2) * (-c1-c3) */
            z3 = MULTIPLY(z3, -FIX_1_961570560); /* sqrt(2) * (-c3-c5) */
            z4 = MULTIPLY(z4, -FIX_0_390180644); /* sqrt(2) * (c5-c3) */

            z3 += z5;
            z4 += z5;

            tmp0 += z1 + z3;
            tmp1 += z2 + z4;
            tmp2 += z2 + z3;
            tmp3 += z1 + z4;

            /* Final output stage: inputs are tmp10..tmp13, tmp0..tmp3 */

            wsptr[wsptr_offset] = (int) DESCALE(tmp10 + tmp3, CONST_BITS - PASS1_BITS);
            wsptr[DCTSIZE * 7 + wsptr_offset] = DESCALE(tmp10 - tmp3, CONST_BITS - PASS1_BITS);
            wsptr[DCTSIZE + wsptr_offset] = DESCALE(tmp11 + tmp2, CONST_BITS - PASS1_BITS);
            wsptr[DCTSIZE * 6 + wsptr_offset] = DESCALE(tmp11 - tmp2, CONST_BITS - PASS1_BITS);
            wsptr[DCTSIZE * 2 + wsptr_offset] = DESCALE(tmp12 + tmp1, CONST_BITS - PASS1_BITS);
            wsptr[DCTSIZE * 5 + wsptr_offset] = DESCALE(tmp12 - tmp1, CONST_BITS - PASS1_BITS);
            wsptr[DCTSIZE * 3 + wsptr_offset] = DESCALE(tmp13 + tmp0, CONST_BITS - PASS1_BITS);
            wsptr[DCTSIZE * 4 + wsptr_offset] = DESCALE(tmp13 - tmp0, CONST_BITS - PASS1_BITS);

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

            if (wsptr[1 + wsptr_offset] == 0 && wsptr[2 + wsptr_offset] == 0 && wsptr[3 + wsptr_offset] == 0
                    && wsptr[4 + wsptr_offset] == 0 && wsptr[5 + wsptr_offset] == 0 && wsptr[6 + wsptr_offset] == 0
                    && wsptr[7 + wsptr_offset] == 0)
            {
                /* AC terms all zero */
                short dcval = range_limit[range_limit_offset + DESCALE(wsptr[wsptr_offset], PASS1_BITS + 3)
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

            /* Even part: reverse the even part of the forward DCT. */
            /* The rotator is sqrt(2)*c(-6). */

            z2 = wsptr[2 + wsptr_offset];
            z3 = wsptr[6 + wsptr_offset];

            z1 = MULTIPLY(z2 + z3, FIX_0_541196100);
            tmp2 = z1 + MULTIPLY(z3, -FIX_1_847759065);
            tmp3 = z1 + MULTIPLY(z2, FIX_0_765366865);

            tmp0 = (wsptr[wsptr_offset] + wsptr[4 + wsptr_offset]) << CONST_BITS;
            tmp1 = (wsptr[wsptr_offset] - wsptr[4 + wsptr_offset]) << CONST_BITS;

            tmp10 = tmp0 + tmp3;
            tmp13 = tmp0 - tmp3;
            tmp11 = tmp1 + tmp2;
            tmp12 = tmp1 - tmp2;

            /* Odd part per figure 8; the matrix is unitary and hence its
             * transpose is its inverse.  i0..i3 are y7,y5,y3,y1 respectively.
             */

            tmp0 = wsptr[7 + wsptr_offset];
            tmp1 = wsptr[5 + wsptr_offset];
            tmp2 = wsptr[3 + wsptr_offset];
            tmp3 = wsptr[1 + wsptr_offset];

            z1 = tmp0 + tmp3;
            z2 = tmp1 + tmp2;
            z3 = tmp0 + tmp2;
            z4 = tmp1 + tmp3;
            z5 = MULTIPLY(z3 + z4, FIX_1_175875602); /* sqrt(2) * c3 */

            tmp0 = MULTIPLY(tmp0, FIX_0_298631336); /* sqrt(2) * (-c1+c3+c5-c7) */
            tmp1 = MULTIPLY(tmp1, FIX_2_053119869); /* sqrt(2) * ( c1+c3-c5+c7) */
            tmp2 = MULTIPLY(tmp2, FIX_3_072711026); /* sqrt(2) * ( c1+c3+c5-c7) */
            tmp3 = MULTIPLY(tmp3, FIX_1_501321110); /* sqrt(2) * ( c1+c3-c5-c7) */
            z1 = MULTIPLY(z1, -FIX_0_899976223); /* sqrt(2) * (c7-c3) */
            z2 = MULTIPLY(z2, -FIX_2_562915447); /* sqrt(2) * (-c1-c3) */
            z3 = MULTIPLY(z3, -FIX_1_961570560); /* sqrt(2) * (-c3-c5) */
            z4 = MULTIPLY(z4, -FIX_0_390180644); /* sqrt(2) * (c5-c3) */

            z3 += z5;
            z4 += z5;

            tmp0 += z1 + z3;
            tmp1 += z2 + z4;
            tmp2 += z2 + z3;
            tmp3 += z1 + z4;

            /* Final output stage: inputs are tmp10..tmp13, tmp0..tmp3 */
            outptr[outptr_offset] = range_limit[range_limit_offset + DESCALE(tmp10 + tmp3,
                    CONST_BITS + PASS1_BITS + 3)
                    & RANGE_MASK];
            outptr[7 + outptr_offset] = range_limit[range_limit_offset + DESCALE(tmp10 - tmp3,
                    CONST_BITS + PASS1_BITS + 3)
                    & RANGE_MASK];
            outptr[1 + outptr_offset] = range_limit[range_limit_offset + DESCALE(tmp11 + tmp2,
                    CONST_BITS + PASS1_BITS + 3)
                    & RANGE_MASK];
            outptr[6 + outptr_offset] = range_limit[range_limit_offset + DESCALE(tmp11 - tmp2,
                    CONST_BITS + PASS1_BITS + 3)
                    & RANGE_MASK];
            outptr[2 + outptr_offset] = range_limit[range_limit_offset + DESCALE(tmp12 + tmp1,
                    CONST_BITS + PASS1_BITS + 3)
                    & RANGE_MASK];
            outptr[5 + outptr_offset] = range_limit[range_limit_offset + DESCALE(tmp12 - tmp1,
                    CONST_BITS + PASS1_BITS + 3)
                    & RANGE_MASK];
            outptr[3 + outptr_offset] = range_limit[range_limit_offset + DESCALE(tmp13 + tmp0,
                    CONST_BITS + PASS1_BITS + 3)
                    & RANGE_MASK];
            outptr[4 + outptr_offset] = range_limit[range_limit_offset + DESCALE(tmp13 - tmp0,
                    CONST_BITS + PASS1_BITS + 3)
                    & RANGE_MASK];

            wsptr_offset += DCTSIZE;        /* advance pointer to next row */
        }
    }
}