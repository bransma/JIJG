package bransford.jpeg.bit12_16.dct12_16;
/*
 * jidctred.c
 *
 * Copyright (C) 1994-1998, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains inverse-DCT routines that produce reduced-size output:
 * either 4x4, 2x2, or 1x1 pixels from an 8x8 DCT block.
 *
 * The implementation is based on the Loeffler, Ligtenberg and Moschytz (LL&M)
 * algorithm used in jidctint.c.  We simply replace each 8-to-8 1-D IDCT step
 * with an 8-to-4 step that produces the four averages of two adjacent outputs
 * (or an 8-to-2 step producing two averages of four outputs, for 2x2 output).
 * These steps were derived by computing the corresponding values at the end
 * of the normal LL&M code, then simplifying as much as possible.
 *
 * 1x1 is trivial: just take the DC coefficient divided by 8.
 *
 * See jidctint.c for additional comments.
 */

import bransford.jpeg.bit12_16.structs12_16.jpeg_component_info12_16;
import bransford.jpeg.bit12_16.structs12_16.jpeg_decompress_struct12_16;
import bransford.jpeg.bit12_16.jmorecfg12_16;

public class jidctred12_16 extends jdct12_16
{
    public final int CONST_BITS = 13;
    public int PASS1_BITS;
    public final int FIX_0_211164243 = 1730;    /* FIX(0.211164243) */
    public final int FIX_0_509795579 = 4176;    /* FIX(0.509795579) */
    public final int FIX_0_601344887 = 4926;    /* FIX(0.601344887) */
    public final int FIX_0_720959822 = 5906;    /* FIX(0.720959822) */
    public final int FIX_0_765366865 = 6270;    /* FIX(0.765366865) */
    public final int FIX_0_850430095 = 6967;    /* FIX(0.850430095) */
    public final int FIX_0_899976223 = 7373;    /* FIX(0.899976223) */
    public final int FIX_1_061594337 = 8697;    /* FIX(1.061594337) */
    public final int FIX_1_272758580 = 10426;    /* FIX(1.272758580) */
    public final int FIX_1_451774981 = 11893;    /* FIX(1.451774981) */
    public final int FIX_1_847759065 = 15137;    /* FIX(1.847759065) */
    public final int FIX_2_172734803 = 17799;    /* FIX(2.172734803) */
    public final int FIX_2_562915447 = 20995;    /* FIX(2.562915447) */
    public final int FIX_3_624509785 = 29692;    /* FIX(3.624509785) */

    public jidctred12_16()
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

    public int MULTIPLY(int var, int a_const)
    {
        return var * a_const;
    }

    public int DEQUANTIZE(int coef, int quantval)
    {
        return (((coef)) * (quantval));
    }

    public int DESCALE(int x, int n)
    {
        return (((x) + (1 << ((n) - 1)) >> n));
    }

    /*
     * Perform dequantization and inverse DCT on one block of coefficients,
     * producing a reduced-size 4x4 output block.
     */
    public void jpeg_idct_4x4(jpeg_decompress_struct12_16 cinfo, jpeg_component_info12_16 compptr, short[] coef_block,
                              short[][] output_buf, int output_buff_offset, int output_col)
    {
        int tmp0, tmp2, tmp10, tmp12;
        int z1, z2, z3, z4;

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

        for (ctr = DCTSIZE; ctr > 0; inptr_offset++, quantptr_offset++, wsptr_offset++, ctr--)
        {
            /* Don't bother to process column 4, because second pass won't use it */
            if (ctr == DCTSIZE - 4)
            {
                 continue;
            }

            if (inptr[DCTSIZE + inptr_offset] == 0 && inptr[DCTSIZE * 2 + inptr_offset] == 0 &&
                    inptr[DCTSIZE * 3 + inptr_offset] == 0 && inptr[DCTSIZE * 5 + inptr_offset] == 0 &&
                    inptr[DCTSIZE * 6 + inptr_offset] == 0 && inptr[DCTSIZE * 7 + inptr_offset] == 0)
            {
                /* AC terms all zero; we need not examine term 4 for 4x4 output */
                int dcval = DEQUANTIZE(inptr[inptr_offset], quantptr[quantptr_offset]) << PASS1_BITS;

                wsptr[wsptr_offset] = dcval;
                wsptr[DCTSIZE + wsptr_offset] = dcval;
                wsptr[DCTSIZE * 2 + wsptr_offset] = dcval;
                wsptr[DCTSIZE * 3 + wsptr_offset] = dcval;

                continue;
            }

            /* Even part */

            tmp0 = DEQUANTIZE(inptr[inptr_offset], quantptr[quantptr_offset]);
            tmp0 <<= (CONST_BITS + 1);

            z2 = DEQUANTIZE(inptr[DCTSIZE * 2 + inptr_offset], quantptr[DCTSIZE * 2 + quantptr_offset]);
            z3 = DEQUANTIZE(inptr[DCTSIZE * 6 + inptr_offset], quantptr[DCTSIZE * 6 + quantptr_offset]);

            tmp2 = MULTIPLY(z2, FIX_1_847759065) + MULTIPLY(z3, -FIX_0_765366865);

            tmp10 = tmp0 + tmp2;
            tmp12 = tmp0 - tmp2;

            /* Odd part */

            z1 = DEQUANTIZE(inptr[DCTSIZE * 7 + inptr_offset], quantptr[DCTSIZE * 7 + quantptr_offset]);
            z2 = DEQUANTIZE(inptr[DCTSIZE * 5 + inptr_offset], quantptr[DCTSIZE * 5 + quantptr_offset]);
            z3 = DEQUANTIZE(inptr[DCTSIZE * 3 + inptr_offset], quantptr[DCTSIZE * 3 + quantptr_offset]);
            z4 = DEQUANTIZE(inptr[DCTSIZE + inptr_offset], quantptr[DCTSIZE + quantptr_offset]);

            tmp0 = MULTIPLY(z1, -FIX_0_211164243) /* sqrt(2) * (c3-c1) */
                    + MULTIPLY(z2, FIX_1_451774981) /* sqrt(2) * (c3+c7) */
                    + MULTIPLY(z3, -FIX_2_172734803) /* sqrt(2) * (-c1-c5) */
                    + MULTIPLY(z4, FIX_1_061594337); /* sqrt(2) * (c5+c7) */

            tmp2 = MULTIPLY(z1, -FIX_0_509795579) /* sqrt(2) * (c7-c5) */
                    + MULTIPLY(z2, -FIX_0_601344887) /* sqrt(2) * (c5-c1) */
                    + MULTIPLY(z3, FIX_0_899976223) /* sqrt(2) * (c3-c7) */
                    + MULTIPLY(z4, FIX_2_562915447); /* sqrt(2) * (c1+c3) */

            /* Final output stage */

            wsptr[wsptr_offset] = DESCALE(tmp10 + tmp2, CONST_BITS - PASS1_BITS + 1);
            wsptr[DCTSIZE * 3 + wsptr_offset] = DESCALE(tmp10 - tmp2, CONST_BITS - PASS1_BITS + 1);
            wsptr[DCTSIZE + wsptr_offset] = DESCALE(tmp12 + tmp0, CONST_BITS - PASS1_BITS + 1);
            wsptr[DCTSIZE * 2 + wsptr_offset] = DESCALE(tmp12 - tmp0, CONST_BITS - PASS1_BITS + 1);
        }

        /* Pass 2: process 4 rows from work array, store into output array. */

        int outptr_offset;
        wsptr_offset = 0;

        for (ctr = 0; ctr < 4; ctr++)
        {
            outptr = output_buf[ctr + output_buff_offset];
            outptr_offset = output_col;

            /* It's not clear whether a zero row test is worthwhile here ... */

            if (wsptr[1 + wsptr_offset] == 0 && wsptr[2 + wsptr_offset] == 0 && wsptr[3 + wsptr_offset] == 0 &&
                    wsptr[5 + wsptr_offset] == 0 && wsptr[6 + wsptr_offset] == 0 && wsptr[7 + wsptr_offset] == 0)
            {
                /* AC terms all zero */
                short dcval = range_limit[range_limit_offset + DESCALE(wsptr[0], PASS1_BITS + 3)
                        & RANGE_MASK];

                outptr[outptr_offset] = dcval;
                outptr[1 + outptr_offset] = dcval;
                outptr[2 + outptr_offset] = dcval;
                outptr[3 + outptr_offset] = dcval;

                wsptr_offset += DCTSIZE;        /* advance pointer to next row */
                continue;
            }

            /* Even part */

            tmp0 = (wsptr[wsptr_offset]) << (CONST_BITS + 1);

            tmp2 = MULTIPLY(wsptr[2 + wsptr_offset], FIX_1_847759065)
                    + MULTIPLY(wsptr[6 + wsptr_offset], -FIX_0_765366865);

            tmp10 = tmp0 + tmp2;
            tmp12 = tmp0 - tmp2;

            /* Odd part */

            z1 = wsptr[7 + wsptr_offset];
            z2 = wsptr[5 + wsptr_offset];
            z3 = wsptr[3 + wsptr_offset];
            z4 = wsptr[1 + wsptr_offset];

            tmp0 = MULTIPLY(z1, -FIX_0_211164243) /* sqrt(2) * (c3-c1) */
                    + MULTIPLY(z2, FIX_1_451774981) /* sqrt(2) * (c3+c7) */
                    + MULTIPLY(z3, -FIX_2_172734803) /* sqrt(2) * (-c1-c5) */
                    + MULTIPLY(z4, FIX_1_061594337); /* sqrt(2) * (c5+c7) */

            tmp2 = MULTIPLY(z1, -FIX_0_509795579) /* sqrt(2) * (c7-c5) */
                    + MULTIPLY(z2, -FIX_0_601344887) /* sqrt(2) * (c5-c1) */
                    + MULTIPLY(z3, FIX_0_899976223) /* sqrt(2) * (c3-c7) */
                    + MULTIPLY(z4, FIX_2_562915447); /* sqrt(2) * (c1+c3) */

            /* Final output stage */

            outptr[0] = range_limit[range_limit_offset + DESCALE(tmp10 + tmp2,
                    CONST_BITS + PASS1_BITS + 3 + 1) & RANGE_MASK];
            outptr[3] = range_limit[range_limit_offset + DESCALE(tmp10 - tmp2,
                    CONST_BITS + PASS1_BITS + 3 + 1) & RANGE_MASK];
            outptr[1] = range_limit[range_limit_offset + DESCALE(tmp12 + tmp0,
                    CONST_BITS + PASS1_BITS + 3 + 1) & RANGE_MASK];
            outptr[2] = range_limit[range_limit_offset + DESCALE(tmp12 - tmp0,
                    CONST_BITS + PASS1_BITS + 3 + 1) & RANGE_MASK];

            wsptr_offset += DCTSIZE;        /* advance pointer to next row */
        }
    }

    /*
     * Perform dequantization and inverse DCT on one block of coefficients,
     * producing a reduced-size 2x2 output block.
     */
    public void jpeg_idct_2x2(jpeg_decompress_struct12_16 cinfo, jpeg_component_info12_16 compptr, short[] coef_block,
                              short[][] output_buf, int output_buff_offset, int output_col)
    {
        int tmp0, tmp10, z1;
        short[] inptr;
        int[] quantptr;
        int[] wsptr;
        short[] outptr;
        int ctr;
        int[] workspace = new int[DCTSIZE2];    /* buffers data between passes */

        short[] range_limit = cinfo.sample_range_limit;
        int range_limit_offset = cinfo.sample_range_limit_offset + jmorecfg12_16.CENTERJSAMPLE;

        inptr = coef_block;
        quantptr = compptr.dct_table;
        wsptr = workspace;

        int inptr_offset = 0, quantptr_offset = 0, wsptr_offset = 0;

        /* Pass 1: process columns from input, store into work array. */
        for (ctr = DCTSIZE; ctr > 0; inptr_offset++, quantptr_offset++, wsptr_offset++, ctr--)
        {
            /* Don't bother to process columns 2,4,6 */
            if (ctr == DCTSIZE - 2 || ctr == DCTSIZE - 4 || ctr == DCTSIZE - 6)
            {
                continue;
            }

            if (inptr[DCTSIZE + inptr_offset] == 0 && inptr[DCTSIZE * 3 + inptr_offset] == 0 &&
                    inptr[DCTSIZE * 5 + inptr_offset] == 0 && inptr[DCTSIZE * 7 + inptr_offset] == 0)
            {
                /* AC terms all zero; we need not examine terms 2,4,6 for 2x2 output */
                int dcval = DEQUANTIZE(inptr[inptr_offset], quantptr[quantptr_offset]) << PASS1_BITS;

                wsptr[wsptr_offset] = dcval;
                wsptr[DCTSIZE + wsptr_offset] = dcval;
                continue;
            }

            /* Even part */

            z1 = DEQUANTIZE(inptr[inptr_offset], quantptr[quantptr_offset]);
            tmp10 = z1 << (CONST_BITS + 2);

            /* Odd part */

            z1 = DEQUANTIZE(inptr[DCTSIZE * 7 + inptr_offset], quantptr[DCTSIZE * 7 + quantptr_offset]);
            tmp0 = MULTIPLY(z1, -FIX_0_720959822); /* sqrt(2) * (c7-c5+c3-c1) */
            z1 = DEQUANTIZE(inptr[DCTSIZE * 5 + inptr_offset], quantptr[DCTSIZE * 5 + quantptr_offset]);
            tmp0 += MULTIPLY(z1, FIX_0_850430095); /* sqrt(2) * (-c1+c3+c5+c7) */
            z1 = DEQUANTIZE(inptr[DCTSIZE * 3 + inptr_offset], quantptr[DCTSIZE * 3 + quantptr_offset]);
            tmp0 += MULTIPLY(z1, -FIX_1_272758580); /* sqrt(2) * (-c1+c3-c5-c7) */
            z1 = DEQUANTIZE(inptr[DCTSIZE + inptr_offset], quantptr[DCTSIZE + quantptr_offset]);
            tmp0 += MULTIPLY(z1, FIX_3_624509785); /* sqrt(2) * (c1+c3+c5+c7) */

            /* Final output stage */

            wsptr[wsptr_offset] = DESCALE(tmp10 + tmp0, CONST_BITS - PASS1_BITS + 2);
            wsptr[DCTSIZE + wsptr_offset] = DESCALE(tmp10 - tmp0, CONST_BITS - PASS1_BITS + 2);
        }

        /* Pass 2: process 2 rows from work array, store into output array. */

        int outptr_offset;
        wsptr_offset = 0;

        for (ctr = 0; ctr < 2; ctr++)
        {
            outptr = output_buf[ctr + output_buff_offset];
            outptr_offset = output_col;
            /* It's not clear whether a zero row test is worthwhile here ... */

            if (wsptr[1 + wsptr_offset] == 0 && wsptr[3 + wsptr_offset] == 0 && wsptr[5 +
                    wsptr_offset] == 0 && wsptr[7 + wsptr_offset] == 0)
            {
                /* AC terms all zero */
                short dcval = range_limit[DESCALE(wsptr[wsptr_offset], PASS1_BITS + 3) & RANGE_MASK];

                outptr[outptr_offset] = dcval;
                outptr[1 + outptr_offset] = dcval;

                wsptr_offset += DCTSIZE;        /* advance pointer to next row */
                continue;
            }

            /* Even part */

            tmp10 = (wsptr[wsptr_offset]) << (CONST_BITS + 2);

            /* Odd part */

            tmp0 = MULTIPLY(wsptr[7 + wsptr_offset], -FIX_0_720959822) /* sqrt(2) * (c7-c5+c3-c1) */
                    + MULTIPLY(wsptr[5 + wsptr_offset], FIX_0_850430095) /* sqrt(2) * (-c1+c3+c5+c7) */
                    + MULTIPLY(wsptr[3 + wsptr_offset], -FIX_1_272758580) /* sqrt(2) * (-c1+c3-c5-c7) */
                    + MULTIPLY(wsptr[1 + wsptr_offset], FIX_3_624509785); /* sqrt(2) * (c1+c3+c5+c7) */

            /* Final output stage */

            outptr[0] = range_limit[range_limit_offset + DESCALE(tmp10 + tmp0,
                    CONST_BITS + PASS1_BITS + 3 + 2) & RANGE_MASK];
            outptr[1] = range_limit[range_limit_offset + DESCALE(tmp10 - tmp0,
                    CONST_BITS + PASS1_BITS + 3 + 2) & RANGE_MASK];

            wsptr_offset += DCTSIZE;        /* advance pointer to next row */
        }
    }

    /*
     * Perform dequantization and inverse DCT on one block of coefficients,
     * producing a reduced-size 1x1 output block.
     */
    public void jpeg_idct_1x1(jpeg_decompress_struct12_16 cinfo, jpeg_component_info12_16 compptr, short[] coef_block,
                              short[][] output_buf, int output_buff_offset, int output_col)
    {
        int dcval;
        int[] quantptr;
        short[] range_limit = cinfo.sample_range_limit;

        /* We hardly need an inverse DCT routine for this: just take the
         * average pixel value, which is one-eighth of the DC coefficient.
         */
        quantptr = compptr.dct_table;
        int range_limit_offset = cinfo.sample_range_limit_offset + jmorecfg12_16.CENTERJSAMPLE;
        dcval = DEQUANTIZE(coef_block[0], quantptr[0]);
        dcval = DESCALE(dcval, 3);
        output_buf[output_buff_offset][output_col] = range_limit[range_limit_offset + dcval & RANGE_MASK];
    }
}
