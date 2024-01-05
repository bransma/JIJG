package edu.mayo.jpeg.bit8.structs8;

/*
 * jdcolor.c
 *
 * Copyright (C) 1991-1997, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains output colorspace conversion routines.
 */
public abstract class jpeg_color_deconverter8
{
    /* Private state for YCC.RGB conversion */
    public int[] Cr_r_tab; /* => table for Cr to R conversion */
    public int[] Cb_b_tab; /* => table for Cb to B conversion */
    public long[] Cr_g_tab; /* => table for Cr to G conversion */
    public long[] Cb_g_tab; /* => table for Cb to G conversion */

    public enum COLOR_CONVERT_METHOD
    {
        YCC_RGB_CONVERT, NULL_CONVERT, GRAYSCALE_CONVERT, GRAY_RGB_CONVERT, YCCK_CMYK_CONVERT
    }

    public static COLOR_CONVERT_METHOD method;

    /*
     * Initialize tables for YCC.RGB colorspace conversion.
     */
    public abstract void build_ycc_rgb_table(jpeg_decompress_struct8 cinfo);

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
    public abstract void ycc_rgb_convert(jpeg_decompress_struct8 cinfo,
                                         byte[][][] input_buf, int[] input_buf_offset, int input_row,
                                         byte[][] output_buf, int output_buf_offset, int num_rows);

    /**************** Cases other than YCbCr . RGB **************/

    /*
     * Color conversion for no colorspace change: just copy the data,
     * converting from separate-planes to interleaved representation.
     */
    public abstract void null_convert(jpeg_decompress_struct8 cinfo,
                                      byte[][][] input_buf, int[] input_buf_offset, int input_row,
                                      byte[][] output_buf, int output_buf_offset, int num_rows);

    /*
     * Color conversion for grayscale: just copy the data.
     * This also works for YCbCr . grayscale conversion, in which
     * we just copy the Y (luminance) component and ignore chrominance.
     */
    public abstract void grayscale_convert(jpeg_decompress_struct8 cinfo,
                                           byte[][][] input_buf, int[] input_buf_offset, int input_row,
                                           byte[][] output_buf, int output_buf_offset, int num_rows);

    /*
     * Convert grayscale to RGB: just duplicate the graylevel three times.
     * This is provided to support applications that don't want to cope
     * with grayscale as a separate case.
     */
    public abstract void gray_rgb_convert(jpeg_decompress_struct8 cinfo,
                                          byte[][][] input_buf, int[] input_buf_offset, int input_row,
                                          byte[][] output_buf, int output_buf_offset, int num_rows);

    /*
     * Adobe-style YCCK.CMYK conversion.
     * We convert YCbCr to R=1-C, G=1-M, and B=1-Y using the same
     * conversion as above, while passing K (black) unchanged.
     * We assume build_ycc_rgb_table has been called.
     */
    public abstract void ycck_cmyk_convert(jpeg_decompress_struct8 cinfo,
                                           byte[][][] input_buf, int[] input_buf_offset, int input_row,
                                           byte[][] output_buf, int output_buf_offset, int num_rows);

    /*
     * Module initialization routine for output colorspace conversion.
     */
    public abstract void jinit_color_deconverter(jpeg_decompress_struct8 cinfo);

    /*
     * Empty method for start_pass.
     */
    public abstract void start_pass(jpeg_decompress_struct8 cinfo);

    public abstract void color_convert(jpeg_decompress_struct8 cinfo,
                                       byte[][][] input_buf, int[] input_buf_offset, int input_row,
                                       byte[][] output_buf, int output_buf_offset, int num_rows);
}
