package edu.mayo.jpeg.bit8;

import edu.mayo.jpeg.bit8.structs8.jpeg_decompress_struct8;

public class jdscaler8
{
    public int scale_factor;

    public enum SCALE_METHODS
    {
        simple_upscale, simple_downscale, noscale,
    }

    public SCALE_METHODS scale_method;

    public void scaler_start_pass(jpeg_decompress_struct8 cinfo)
    {
        int downscale = jmorecfg8.BITS_IN_JSAMPLE < cinfo.data_precision ?
                cinfo.data_precision - jmorecfg8.BITS_IN_JSAMPLE : 0;
        scale_factor = cinfo.Al - downscale;

        if (scale_factor > 0)
        {
            scale_method = SCALE_METHODS.simple_upscale;
        }
        else if (scale_factor < 0)
        {
            scale_factor *= -1;
            scale_method = SCALE_METHODS.simple_downscale;
        }
        else
        {
            scale_method = SCALE_METHODS.noscale;
        }
    }

    public void jinit_d_scaler(jpeg_decompress_struct8 cinfo)
    {

    }

    public void noscale(jpeg_decompress_struct8 cinfo, int[]  diff_buf, byte[] output_buf, int width)
    {
        int xindex;

        for (xindex = 0; xindex < width; xindex++)
        {
            output_buf[xindex] = (byte) diff_buf[xindex];
        }
    }

    public void simple_downscale(jpeg_decompress_struct8 cinfo, int[]  diff_buf, byte[] output_buf, int width)
    {
        int xindex;

        for (xindex = 0; xindex < width; xindex++)
            output_buf[xindex] = (byte) jutils8.RIGHT_SHIFT(diff_buf[xindex], scale_factor);
    }

    public void simple_upscale(jpeg_decompress_struct8 cinfo, int[]  diff_buf, byte[] output_buf, int width)
    {
        int xindex;

        for (xindex = 0; xindex < width; xindex++)
            output_buf[xindex] = (byte) (diff_buf[xindex] << scale_factor);
    }
}
