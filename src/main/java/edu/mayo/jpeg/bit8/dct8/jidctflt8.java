package edu.mayo.jpeg.bit8.dct8;

import edu.mayo.jpeg.bit8.jmorecfg8;
import edu.mayo.jpeg.bit8.structs8.jpeg_component_info8;
import edu.mayo.jpeg.bit8.structs8.jpeg_decompress_struct8;

/*
 * jidctflt.c
 *
 * Copyright (C) 1994-1998, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains a floating-point implementation of the
 * inverse DCT (Discrete Cosine Transform).  In the IJG code, this routine
 * must also perform dequantization of the input coefficients.
 *
 * This implementation should be more accurate than either of the integer
 * IDCT implementations.  However, it may not give the same results on all
 * machines because of differences in roundoff behavior.  Speed will depend
 * on the hardware's floating point capacity.
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
 * The primary disadvantage of this method is that with a fixed-point
 * implementation, accuracy is lost due to imprecise representation of the
 * scaled quantization values.  However, that problem does not arise if
 * we use floating point arithmetic.
 */
public class jidctflt8 extends jdct8
{
    public int DESCALE(int x, int n)
    {
        return (((x) + (1 << ((n) - 1)) >> n));
    }

    public float DEQUANTIZE(int coef, float quantval)
    {
        return (((float) (coef)) * (quantval));
    }

    public float[] cast_to_float(int[] array)
    {
        float[] cast = new float[array.length];

        for (int i = 0; i < cast.length; i++)
        {
            cast[i] = (float) array[i];
        }

        return cast;
    }

    // jpeg_decompress_struct cinfo, jpeg_component_info compptr, short[] coef_block, byte[][] output_buf, int outp_buf_offset, int output_col,
    public void jpeg_idct_float(jpeg_decompress_struct8 cinfo, jpeg_component_info8 compptr, short[] coef_block, byte[][] output_buf, int output_buff_offset,
                                int output_col)
    {
        float tmp0, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7;
        float tmp10, tmp11, tmp12, tmp13;
        float z5, z10, z11, z12, z13;
        short[] inptr;
        // JCOEFPTR inptr;
        // FLOAT_MULT_TYPE * quantptr;
        // FAST_FLOAT * wsptr;
        float[] quantptr;
        float[] wsptr;
        byte[] outptr;
        byte[] range_limit = cinfo.sample_range_limit;
        int range_limit_offset = cinfo.sample_range_limit_offset + jmorecfg8.CENTERJSAMPLE;
        // JSAMPLE *range_limit = IDCT_range_limit(cinfo);
        int ctr;
        float[] workspace = new float[DCTSIZE2]; /* buffers data between passes */


        /* Pass 1: process columns from input, store into work array. */
        inptr = coef_block;
        quantptr = cast_to_float(compptr.dct_table);
        wsptr = workspace;
        int inptr_offset = 0, quantptr_offset = 0, wsptr_offset = 0;

        for (ctr = DCTSIZE; ctr > 0; ctr--) {
            /* Due to quantization, we will usually find that many of the input
             * coefficients are zero, especially the AC terms.  We can exploit this
             * by short-circuiting the IDCT calculation for any column in which all
             * the AC terms are zero.  In that case each output is equal to the
             * DC coefficient (with scale factor as needed).
             * With typical images and quantization tables, half or more of the
             * column DCT calculations can be simplified this way.
             */

            if (inptr[DCTSIZE + inptr_offset] == 0 && inptr[DCTSIZE*2 + inptr_offset] == 0 &&
                    inptr[DCTSIZE*3 + inptr_offset] == 0 && inptr[DCTSIZE*4 + inptr_offset] == 0 &&
                    inptr[DCTSIZE*5 + inptr_offset] == 0 && inptr[DCTSIZE*6 + inptr_offset] == 0 &&
                    inptr[DCTSIZE*7 + inptr_offset] == 0) {
                /* AC terms all zero */
                float dcval = DEQUANTIZE(inptr[inptr_offset], quantptr[quantptr_offset]);

                wsptr[wsptr_offset] = dcval;
                wsptr[DCTSIZE + wsptr_offset] = dcval;
                wsptr[DCTSIZE*2 + wsptr_offset] = dcval;
                wsptr[DCTSIZE*3 + wsptr_offset] = dcval;
                wsptr[DCTSIZE*4 + wsptr_offset] = dcval;
                wsptr[DCTSIZE*5 + wsptr_offset] = dcval;
                wsptr[DCTSIZE*6 + wsptr_offset] = dcval;
                wsptr[DCTSIZE*7 + wsptr_offset] = dcval;

                inptr_offset++;            /* advance pointers to next column */
                quantptr_offset++;
                wsptr_offset++;
                continue;
            }

            /* Even part */

            tmp0 = DEQUANTIZE(inptr[inptr_offset], quantptr[quantptr_offset]);
            tmp1 = DEQUANTIZE(inptr[DCTSIZE*2 + inptr_offset], quantptr[DCTSIZE*2 + quantptr_offset]);
            tmp2 = DEQUANTIZE(inptr[DCTSIZE*4 + inptr_offset], quantptr[DCTSIZE*4 + quantptr_offset]);
            tmp3 = DEQUANTIZE(inptr[DCTSIZE*6 + inptr_offset], quantptr[DCTSIZE*6 + quantptr_offset]);

            tmp10 = tmp0 + tmp2;	/* phase 3 */
            tmp11 = tmp0 - tmp2;

            tmp13 = tmp1 + tmp3;	/* phases 5-3 */
            tmp12 = (tmp1 - tmp3) * ((float) 1.414213562) - tmp13; /* 2*c4 */

            tmp0 = tmp10 + tmp13;	/* phase 2 */
            tmp3 = tmp10 - tmp13;
            tmp1 = tmp11 + tmp12;
            tmp2 = tmp11 - tmp12;

            /* Odd part */

            tmp4 = DEQUANTIZE(inptr[DCTSIZE + inptr_offset], quantptr[DCTSIZE + quantptr_offset]);
            tmp5 = DEQUANTIZE(inptr[DCTSIZE*3 + inptr_offset], quantptr[DCTSIZE*3 + quantptr_offset]);
            tmp6 = DEQUANTIZE(inptr[DCTSIZE*5 + inptr_offset], quantptr[DCTSIZE*5 + quantptr_offset]);
            tmp7 = DEQUANTIZE(inptr[DCTSIZE*7 + inptr_offset], quantptr[DCTSIZE*7 + quantptr_offset]);

            z13 = tmp6 + tmp5;		/* phase 6 */
            z10 = tmp6 - tmp5;
            z11 = tmp4 + tmp7;
            z12 = tmp4 - tmp7;

            tmp7 = z11 + z13;		/* phase 5 */
            tmp11 = (z11 - z13) * ((float) 1.414213562); /* 2*c4 */

            z5 = (z10 + z12) * ((float) 1.847759065); /* 2*c2 */
            tmp10 = ((float) 1.082392200) * z12 - z5; /* 2*(c2-c6) */
            tmp12 = ((float) -2.613125930) * z10 + z5; /* -2*(c2+c6) */

            tmp6 = tmp12 - tmp7;	/* phase 2 */
            tmp5 = tmp11 - tmp6;
            tmp4 = tmp10 + tmp5;

            wsptr[wsptr_offset] = tmp0 + tmp7;
            wsptr[DCTSIZE*7 + wsptr_offset] = tmp0 - tmp7;
            wsptr[DCTSIZE + wsptr_offset] = tmp1 + tmp6;
            wsptr[DCTSIZE*6 + wsptr_offset] = tmp1 - tmp6;
            wsptr[DCTSIZE*2 + wsptr_offset] = tmp2 + tmp5;
            wsptr[DCTSIZE*5 + wsptr_offset] = tmp2 - tmp5;
            wsptr[DCTSIZE*4 + wsptr_offset] = tmp3 + tmp4;
            wsptr[DCTSIZE*3 + wsptr_offset] = tmp3 - tmp4;

            inptr_offset++;            /* advance pointers to next column */
            quantptr_offset++;
            wsptr_offset++;
        }

        /* Pass 2: process rows from work array, store into output array. */
        /* Note that we must descale the results by a factor of 8 == 2**3. */

        int outptr_offset = 0;
        wsptr_offset = 0;

        for (ctr = 0; ctr < DCTSIZE; ctr++) {
            outptr = output_buf[ctr + output_buff_offset];
            outptr_offset = output_col;

            /* Rows of zeroes can be exploited in the same way as we did with columns.
             * However, the column calculation has created many nonzero AC terms, so
             * the simplification applies less often (typically 5% to 10% of the time).
             * And testing floats for zero is relatively expensive, so we don't bother.
             */

            /* Even part */

            tmp10 = wsptr[wsptr_offset] + wsptr[4 + wsptr_offset];
            tmp11 = wsptr[wsptr_offset] - wsptr[4 + wsptr_offset];

            tmp13 = wsptr[2 + wsptr_offset] + wsptr[6 + wsptr_offset];
            tmp12 = (wsptr[2 + wsptr_offset] - wsptr[6 + wsptr_offset]) * ((float) 1.414213562) - tmp13;

            tmp0 = tmp10 + tmp13;
            tmp3 = tmp10 - tmp13;
            tmp1 = tmp11 + tmp12;
            tmp2 = tmp11 - tmp12;

            /* Odd part */

            z13 = wsptr[5 + wsptr_offset] + wsptr[3 + wsptr_offset];
            z10 = wsptr[5 + wsptr_offset] - wsptr[3 + wsptr_offset];
            z11 = wsptr[1 + wsptr_offset] + wsptr[7 + wsptr_offset];
            z12 = wsptr[1 + wsptr_offset] - wsptr[7 + wsptr_offset];

            tmp7 = z11 + z13;
            tmp11 = (z11 - z13) * ((float) 1.414213562);

            z5 = (z10 + z12) * ((float) 1.847759065); /* 2*c2 */
            tmp10 = ((float) 1.082392200) * z12 - z5; /* 2*(c2-c6) */
            tmp12 = ((float) -2.613125930) * z10 + z5; /* -2*(c2+c6) */

            tmp6 = tmp12 - tmp7;
            tmp5 = tmp11 - tmp6;
            tmp4 = tmp10 + tmp5;

            /* Final output stage: scale down by a factor of 8 and range-limit */

            outptr[outptr_offset] = range_limit[range_limit_offset + DESCALE((int) (tmp0 + tmp7), 3)
                    & RANGE_MASK];
            outptr[7 + outptr_offset] = range_limit[range_limit_offset + DESCALE((int) (tmp0 - tmp7), 3)
                    & RANGE_MASK];
            outptr[1 + outptr_offset] = range_limit[range_limit_offset + DESCALE((int) (tmp1 + tmp6), 3)
                    & RANGE_MASK];
            outptr[6 + outptr_offset] = range_limit[range_limit_offset + DESCALE((int) (tmp1 - tmp6), 3)
                    & RANGE_MASK];
            outptr[2 + outptr_offset] = range_limit[range_limit_offset + DESCALE((int) (tmp2 + tmp5), 3)
                    & RANGE_MASK];
            outptr[5 + outptr_offset] = range_limit[range_limit_offset + DESCALE((int) (tmp2 - tmp5), 3)
                    & RANGE_MASK];
            outptr[4 + outptr_offset] = range_limit[range_limit_offset + DESCALE((int) (tmp3 + tmp4), 3)
                    & RANGE_MASK];
            outptr[3 + outptr_offset] = range_limit[range_limit_offset + DESCALE((int) (tmp3 - tmp4), 3)
                    & RANGE_MASK];

            wsptr_offset += DCTSIZE;		/* advance pointer to next row */
        }
    }
}
