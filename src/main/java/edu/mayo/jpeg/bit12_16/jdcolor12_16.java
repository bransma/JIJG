package edu.mayo.jpeg.bit12_16;

import edu.mayo.jpeg.bit12_16.error12_16.ErrorStrings12_16;
import edu.mayo.jpeg.bit12_16.structs12_16.jpeg_color_deconverter12_16;
import edu.mayo.jpeg.bit12_16.structs12_16.jpeg_decompress_struct12_16;

public class jdcolor12_16 extends jpeg_color_deconverter12_16
{
    /**************** YCbCr -> RGB conversion: most common case **************/

    /*
     * YCbCr is defined per CCIR 601-1, except that Cb and Cr are
     * normalized to the range 0..MAXJSAMPLE rather than -0.5 .. 0.5.
     * The conversion equations to be implemented are therefore
     *	R = Y                + 1.40200 * Cr
     *	G = Y - 0.34414 * Cb - 0.71414 * Cr
     *	B = Y + 1.77200 * Cb
     * where Cb and Cr represent the incoming values less CENTERJSAMPLE.
     * (These numbers are derived from TIFF 6.0 section 21, dated 3-June-92.)
     *
     * To avoid floating-point arithmetic, we represent the fractional constants
     * as integers scaled up by 2^16 (about 4 digits precision); we have to divide
     * the products by 2^16, with appropriate rounding, to get the correct answer.
     * Notice that Y, being an integral input, does not contribute any fraction
     * so it need not participate in the rounding.
     *
     * For even more speed, we avoid doing any multiplications in the inner loop
     * by precalculating the constants times Cb and Cr for all possible values.
     * For 8-bit JSAMPLEs this is very reasonable (only 256 entries per table);
     * for 12-bit samples it is still acceptable.  It's not very reasonable for
     * 16-bit samples, but if you want lossless storage you shouldn't be changing
     * colorspace anyway.
     * The Cr=>R and Cb=>B values can be rounded to integers in advance; the
     * values for the G calculation are left scaled up, since we must add them
     * together before rounding.
     */
    public static final int SCALEBITS = 16; // /* speediest right-shift on some machines */
    public static final int ONE_HALF = 1 << (SCALEBITS - 1);

    /*
     * Initialize tables for YCC.RGB colorspace conversion.
     */
    public void build_ycc_rgb_table(jpeg_decompress_struct12_16 cinfo)
    {
        int i;
        long x;
        //	#if BITS_IN_JSAMPLE == 16
        //	  /* no need for temporaries */
        //	#else
        //	  SHIFT_TEMPS
        //	#endif

        Cr_r_tab = new int[jmorecfg12_16.MAXJSAMPLE + 1];
        Cb_b_tab = new int[jmorecfg12_16.MAXJSAMPLE + 1];
        Cr_g_tab = new long[jmorecfg12_16.MAXJSAMPLE + 1];
        Cb_g_tab = new long[jmorecfg12_16.MAXJSAMPLE + 1];

        for (i = 0, x = -jmorecfg12_16.CENTERJSAMPLE; i <= jmorecfg12_16.MAXJSAMPLE; i++, x++)
        {
            /* i is the actual input pixel value, in the range 0..MAXJSAMPLE */
            /* The Cb or Cr value we are thinking of is x = i - CENTERJSAMPLE */

            if (jmorecfg12_16.BITS_IN_JSAMPLE == 16)
            {
                /* Bug fix 2001-11-06 by Eichelberg: The integer routines below
                   produce an overflow when used with MAXJSAMPLE == 65535.
                   Use floating point calculation instead. */

                /* Cr=>R value is nearest int to 1.40200 * x */
                Cr_r_tab[i] = (int) (1.40200 * (double) x + 0.5);
                /* Cb=>B value is nearest int to 1.77200 * x */
                Cb_b_tab[i] = (int) (1.77200 * (double) x + 0.5);
            }
            else
            {
                /* Cr=>R value is nearest int to 1.40200 * x */

                Cr_r_tab[i] = jutils12_16.RIGHT_SHIFT((int) (jdcolor12_16.FIX(1.40200) * x + ONE_HALF), SCALEBITS);
                /* Cb=>B value is nearest int to 1.77200 * x */
                Cb_b_tab[i] = jutils12_16.RIGHT_SHIFT((int) (jdcolor12_16.FIX(1.77200) * x + ONE_HALF), SCALEBITS);
            }

            /* Cr=>G value is scaled-up -0.71414 * x */
            Cr_g_tab[i] = (-jdcolor12_16.FIX(0.71414)) * x;
            /* Cb=>G value is scaled-up -0.34414 * x */
            /* We also add in ONE_HALF so that need not do it in inner loop */
            Cb_g_tab[i] = (-jdcolor12_16.FIX(0.34414)) * x + ONE_HALF;
        }
    }

    /*
     * Convert some rows of samples to the output colorspace.
     *
     * Note that we change from noninterleaved, one-plane-per-component format
     * to interleaved-pixel format.  The output buffer is therefore three times
     * as wide as the input buffer.
     * A starting row offset is provided only for the input buffer.  The caller
     * can easily adjust the passed output_buf value to accommodate any row
     * offset required on that side.
     */
    //    public void ycc_rgb_convert(jpeg_decompress_struct cinfo, JSAMPIMAGE input_buf, JDIMENSION input_row, JSAMPARRAY output_buf,
    //	    int num_rows)
//    public void ycc_rgb_convert(jpeg_decompress_struct cinfo,
//                                byte[][][] input_buf, int[] input_buf_offset, int input_row,
//                                byte[][] output_buf, int output_buf_offset, int num_rows)
    public void ycc_rgb_convert(jpeg_decompress_struct12_16 cinfo,
                                short[][][] input_buf, int[] input_buf_offset, int input_row,
                                short[][] output_buf, int output_buf_offset, int num_rows)
    {
        int y, cb, cr;
        short[] outptr;
        short[] inptr0, inptr1, inptr2;
        int col;
        int num_cols = cinfo.output_width;
        /* copy these pointers into registers if possible */
        short[] range_limit = cinfo.sample_range_limit;
        int range_limit_offset = cinfo.sample_range_limit_offset;
        int[] Crrtab = cinfo.cconvert.Cr_r_tab;
        int[] Cbbtab = cinfo.cconvert.Cb_b_tab;
        long[] Crgtab = cinfo.cconvert.Cr_g_tab;
        long[] Cbgtab = cinfo.cconvert.Cb_g_tab;

        while (--num_rows >= 0)
        {
            inptr0 = input_buf[0][input_row + input_buf_offset[0]];
            inptr1 = input_buf[1][input_row + input_buf_offset[1]];
            inptr2 = input_buf[2][input_row + input_buf_offset[2]];
            input_row++;
            int outptr_offset = 0;
            outptr = output_buf[output_buf_offset++];
            for (col = 0; col < num_cols; col++)
            {
                y = inptr0[col] & 0xFFFF;
                cb = inptr1[col] & 0xFFFF;
                cr = inptr2[col] & 0xFFFF;
                /* Range-limiting is essential due to noise introduced by DCT losses. */
                outptr[jmorecfg12_16.RGB_RED + outptr_offset] = range_limit[y + Crrtab[cr] + range_limit_offset];
                outptr[jmorecfg12_16.RGB_GREEN + outptr_offset] = range_limit[y +
                        (jutils12_16.RIGHT_SHIFT((int) (Cbgtab[cb] + Crgtab[cr]), SCALEBITS)) + range_limit_offset];
                outptr[jmorecfg12_16.RGB_BLUE + outptr_offset] = range_limit[y + Cbbtab[cb] + range_limit_offset];
                outptr_offset += jmorecfg12_16.RGB_PIXELSIZE;
            }
        }
    }

    /*
     * Adobe-style YCCK.CMYK conversion.
     * We convert YCbCr to R=1-C, G=1-M, and B=1-Y using the same
     * conversion as above, while passing K (black) unchanged.
     * We assume build_ycc_rgb_table has been called.
     */
//    public void ycck_cmyk_convert(jpeg_decompress_struct cinfo,
//                                  byte[][][] input_buf, int[] input_buf_offset, int input_row,
//                                  byte[][] output_buf, int output_buf_offset, int num_rows)
    public void ycck_cmyk_convert(jpeg_decompress_struct12_16 cinfo,
                                  short[][][] input_buf, int[] input_buf_offset, int input_row,
                                  short[][] output_buf, int output_buf_offset, int num_rows)
    {
        int y, cb, cr;
        short[] outptr;
        short[] inptr0, inptr1, inptr2, inptr3;
        int col;
        int num_cols = cinfo.output_width;
        /* copy these pointers into registers if possible */
        short[] range_limit = cinfo.sample_range_limit;
        int range_limit_offset = cinfo.sample_range_limit_offset;
        int[] Crrtab = cinfo.cconvert.Cr_r_tab;
        int[] Cbbtab = cinfo.cconvert.Cb_b_tab;
        long[] Crgtab = cinfo.cconvert.Cr_g_tab;
        long[] Cbgtab = cinfo.cconvert.Cb_g_tab;

        while (--num_rows >= 0)
        {
            inptr0 = input_buf[0][input_row + input_buf_offset[0]];
            inptr1 = input_buf[1][input_row + input_buf_offset[1]];
            inptr2 = input_buf[2][input_row + input_buf_offset[2]];
            inptr3 = input_buf[3][input_row + input_buf_offset[3]];
            input_row++;
            outptr = output_buf[output_buf_offset++];
            int outptr_offset = 0;
            for (col = 0; col < num_cols; col++)
            {
                y = inptr0[col] & 0xFFFF;
                cb = inptr1[col] & 0xFFFF;
                cr = inptr2[col] & 0xFFFF;
                /* Range-limiting is essential due to noise introduced by DCT losses. */
                outptr[outptr_offset] = range_limit[jmorecfg12_16.MAXJSAMPLE - (y + Crrtab[cr]) +
                        range_limit_offset];    /* red */
                outptr[1 + outptr_offset] = range_limit[jmorecfg12_16.MAXJSAMPLE -
                        (y +            /* green */
                        (jutils12_16.RIGHT_SHIFT((int) (Cbgtab[cb] + Crgtab[cr]),
                                SCALEBITS))) + range_limit_offset];
                outptr[2 + outptr_offset] = range_limit[jmorecfg12_16.MAXJSAMPLE -
                        (y + Cbbtab[cb]) + range_limit_offset];    /* blue */
                /* K passes through unchanged */
                outptr[3 + outptr_offset] = inptr3[col];    /* don't need GETJSAMPLE here */
                outptr_offset += 4;
            }
        }
    }

    /**************** Cases other than YCbCr . RGB **************/

    /*
     * Color conversion for no colorspace change: just copy the data,
     * converting from separate-planes to interleaved representation.
     */
//    public void null_convert(jpeg_decompress_struct cinfo,
//                             byte[][][] input_buf, int[] input_buf_offset, int input_row,
//                             byte[][] output_buf, int output_buf_offset, int num_rows)
    public void null_convert(jpeg_decompress_struct12_16 cinfo,
                             short[][][] input_buf, int[] input_buf_offset, int input_row,
                             short[][] output_buf, int output_buf_offset, int num_rows)
    {
        short[] inptr, outptr;
        int count;
        int num_components = cinfo.num_components;
        int num_cols = cinfo.output_width;
        int ci;

        while (--num_rows >= 0)
        {
            for (ci = 0; ci < num_components; ci++)
            {
                inptr = input_buf[ci][input_row + input_buf_offset[0]];
                int inptr_offset = 0;

                outptr = output_buf[output_buf_offset];
                int outptr_offset = ci;

                for (count = num_cols; count > 0; count--)
                {
                    outptr[outptr_offset] = inptr[inptr_offset++];    /* needn't bother with GETJSAMPLE() here */
                    outptr_offset += num_components;
                }
            }
            input_row++;
            output_buf_offset++;
        }
    }

    /*
     * Color conversion for grayscale: just copy the data.
     * This also works for YCbCr . grayscale conversion, in which
     * we just copy the Y (luminance) component and ignore chrominance.
     */
    public void grayscale_convert(jpeg_decompress_struct12_16 cinfo,
                                  short[][][] input_buf, int[] input_buf_offset, int input_row,
                                  short[][] output_buf, int output_buf_offset, int num_rows)
    {
        jutils12_16.jcopy_sample_rows(input_buf[0], (int) input_row + input_buf_offset[0], output_buf,
                output_buf_offset, num_rows, cinfo.output_width);
    }

    /*
     * Convert grayscale to RGB: just duplicate the graylevel three times.
     * This is provided to support applications that don't want to cope
     * with grayscale as a separate case.
     */
    public void gray_rgb_convert(jpeg_decompress_struct12_16 cinfo,
                                 short[][][] input_buf, int[] input_buf_offset, int input_row,
                                 short[][] output_buf, int output_buf_offset, int num_rows)
    {
        short[] inptr, outptr;
        int col;
        int num_cols = cinfo.output_width;

        while (--num_rows >= 0)
        {
            inptr = input_buf[0][input_row++ + input_buf_offset[0]];
            outptr = output_buf[output_buf_offset++];
            int outptr_offset = 0;
            for (col = 0; col < num_cols; col++)
            {
                /* We can dispense with GETJSAMPLE() here */
                outptr[jmorecfg12_16.RGB_RED + outptr_offset] = outptr[jmorecfg12_16.RGB_GREEN + outptr_offset] =
                        outptr[jmorecfg12_16.RGB_BLUE + outptr_offset] =
                                inptr[col];
                outptr_offset += jmorecfg12_16.RGB_PIXELSIZE;
            }
        }
    }


    /*
     * Module initialization routine for output colorspace conversion.
     */
    public void jinit_color_deconverter(jpeg_decompress_struct12_16 cinfo)
    {
        int ci;

        /* Make sure num_components agrees with jpeg_color_space */
        switch (cinfo.jpeg_color_space)
        {
            case JCS_GRAYSCALE ->
            {
                if (cinfo.num_components != 1)
                {
                    cinfo.err.ERREXIT(ErrorStrings12_16.JERR_BAD_J_COLORSPACE);
                }
            }
            case JCS_RGB, JCS_YCbCr ->
            {
                if (cinfo.num_components != 3)
                {
                    cinfo.err.ERREXIT(ErrorStrings12_16.JERR_BAD_J_COLORSPACE);
                }
            }
            case JCS_CMYK, JCS_YCCK ->
            {
                if (cinfo.num_components != 4)
                {
                    cinfo.err.ERREXIT(ErrorStrings12_16.JERR_BAD_J_COLORSPACE);
                }
            }
            default -> /* JCS_UNKNOWN can be anything */
            {
                if (cinfo.num_components < 1)
                {
                    cinfo.err.ERREXIT(ErrorStrings12_16.JERR_BAD_J_COLORSPACE);
                }
            }
        }

        /* Set out_color_components and conversion method based on requested space.
         * Also clear the component_needed flags for any unused components,
         * so that earlier pipeline stages can avoid useless computation.
         */

        switch (cinfo.out_color_space)
        {
            case JCS_GRAYSCALE ->
            {
                cinfo.out_color_components = 1;
                if (cinfo.jpeg_color_space == jpeglib12_16.J_COLOR_SPACE.JCS_GRAYSCALE
                        || cinfo.jpeg_color_space == jpeglib12_16.J_COLOR_SPACE.JCS_YCbCr)
                {
                    method = COLOR_CONVERT_METHOD.GRAYSCALE_CONVERT;
                    /* For color.grayscale conversion, only the Y (0) component is needed */
                    for (ci = 1; ci < cinfo.num_components; ci++)
                    {
                        cinfo.comp_info[ci].component_needed = false;
                    }
                }
                else
                {
                    cinfo.err.ERREXIT(ErrorStrings12_16.JERR_CONVERSION_NOTIMPL);
                }
            }
            case JCS_RGB ->
            {
                cinfo.out_color_components = jmorecfg12_16.RGB_PIXELSIZE;
                if (cinfo.jpeg_color_space == jpeglib12_16.J_COLOR_SPACE.JCS_YCbCr)
                {
                    method = COLOR_CONVERT_METHOD.YCC_RGB_CONVERT;
                    build_ycc_rgb_table(cinfo);
                }
                else if (cinfo.jpeg_color_space == jpeglib12_16.J_COLOR_SPACE.JCS_GRAYSCALE)
                {
                    method = COLOR_CONVERT_METHOD.GRAY_RGB_CONVERT;
                }
                else if (cinfo.jpeg_color_space == jpeglib12_16.J_COLOR_SPACE.JCS_RGB && jmorecfg12_16.RGB_PIXELSIZE == 3)
                {
                    method = COLOR_CONVERT_METHOD.NULL_CONVERT;
                }
                else
                {
                    cinfo.err.ERREXIT(ErrorStrings12_16.JERR_CONVERSION_NOTIMPL);
                }
            }
            case JCS_CMYK ->
            {
                cinfo.out_color_components = 4;
                if (cinfo.jpeg_color_space == jpeglib12_16.J_COLOR_SPACE.JCS_YCCK)
                {
                    method = COLOR_CONVERT_METHOD.YCCK_CMYK_CONVERT;
                    build_ycc_rgb_table(cinfo);
                }
                else if (cinfo.jpeg_color_space == jpeglib12_16.J_COLOR_SPACE.JCS_CMYK)
                {
                    method = COLOR_CONVERT_METHOD.NULL_CONVERT;
                }
                else
                {
                    cinfo.err.ERREXIT(ErrorStrings12_16.JERR_CONVERSION_NOTIMPL);
                }
            }
            default ->
            {
                /* Permit null conversion to same output space */
                if (cinfo.out_color_space == cinfo.jpeg_color_space)
                {
                    cinfo.out_color_components = cinfo.num_components;
                    method = COLOR_CONVERT_METHOD.NULL_CONVERT;
                }
                else
                {/* unsupported non-null conversion */
                    cinfo.err.ERREXIT(ErrorStrings12_16.JERR_CONVERSION_NOTIMPL);
                }
            }
        }

        if (cinfo.quantize_colors)
        {
            cinfo.output_components = 1; /* single colormapped output component */
        }
        else
        {
            cinfo.output_components = cinfo.out_color_components;
        }
    }

    /*
     * Empty method for start_pass.
     */
    public void start_pass(jpeg_decompress_struct12_16 cinfo)
    {
        /* no work needed */
    }

    public void color_convert(jpeg_decompress_struct12_16 cinfo,
                              short[][][] input_buf, int[] input_buf_offset, int input_row,
                              short[][] output_buf, int output_buf_offset, int num_rows)
    {
        switch (method)
        {
            case YCC_RGB_CONVERT ->
            {
                ycc_rgb_convert(cinfo, input_buf, input_buf_offset, input_row, output_buf, output_buf_offset, num_rows);
            }
            case GRAY_RGB_CONVERT ->
            {
                gray_rgb_convert(cinfo, input_buf, input_buf_offset, input_row, output_buf, output_buf_offset, num_rows);
            }
            case GRAYSCALE_CONVERT ->
            {
                grayscale_convert(cinfo, input_buf, input_buf_offset, input_row, output_buf, output_buf_offset, num_rows);
            }
            case NULL_CONVERT ->
            {
                null_convert(cinfo, input_buf, input_buf_offset, input_row, output_buf, output_buf_offset, num_rows);
            }
            case YCCK_CMYK_CONVERT ->
            {
                ycck_cmyk_convert(cinfo, input_buf, input_buf_offset, input_row, output_buf, output_buf_offset, num_rows);
            }
        }
    }

    public static int FIX(double x)
    {
        return ((int) ((x) * (1L << SCALEBITS) + 0.5));
    }
}
