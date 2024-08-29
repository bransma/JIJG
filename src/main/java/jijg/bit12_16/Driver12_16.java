package jijg.bit12_16;

//

//  driver.c
//  jpegDecoder
//
//  Created by m056084 on 10/18/11.
//

import jijg.bit12_16.structs12_16.jpeg_decompress_struct12_16;
import jijg.bit12_16.structs12_16.jpeg_source_mgr12_16;

import java.util.HashMap;

/*
 * Here's the routine that will replace the standard error_exit method:
 */
public class Driver12_16
{
    private static final String SPP = "spp";
    private static final String SIZE = "size";
    private static final String HEIGHT = "height";
    private static final String WIDTH = "width";

    public byte[] decompress12_16(byte[] inmemory, jpeglib12_16.J_COLOR_SPACE photometric_interpretation,
                                  HashMap<String, Integer> imageCharacteristics)
    {
        /* This struct contains the JPEG decompression parameters and pointers to
         * working space (which is allocated as needed by the JPEG library).
         */
        jpeg_decompress_struct12_16 cinfo = new jpeg_decompress_struct12_16();
        /* We use our private extension JPEG error handler.
         * Note that this struct must live as long as the main JPEG parameter
         * struct, to avoid dangling-pointer problems.
         */
        jerror12_16 jerr = new jerror12_16();

        /* We set up the normal JPEG error routines, then override error_exit. */
        jerr.jpeg_std_error();
        cinfo.err = jerr;

        int row_stride; /* physical row width in output buffer */

        /* In this example we want to open the input file before doing anything else,
         * so that the setjmp() error recovery below can assume the file is open.
         * VERY IMPORTANT: use "b" option to fopen() if you are on a machine that
         * requires it in order to read binary files.
         */

        /* Step 1: allocate and initialize JPEG decompression object */
        /* Step 2: specify data source (eg, a file or an in memory buffer) */
        cinfo.src = new jpeg_source_mgr12_16(inmemory);

        jdapimin12_16 jdapi = new jdapimin12_16();
        jdapi.jpeg_CreateDecompress(cinfo);

        /* Step 3: read file parameters with jpeg_read_header() */
        int ret = jdapi.jpeg_read_header(cinfo, true);
        if (ret != 1)
        {
            System.out.println("something is wrong: " + ret);
        }

        // Need to take into account if it's lossless or not
        if (cinfo.num_components == 3)
        {
            if (cinfo.is_lossless)
            {
                /* error/unspecified */
                if ((photometric_interpretation != jpeglib12_16.J_COLOR_SPACE.JCS_GRAYSCALE && /* monochrome */
                        photometric_interpretation != jpeglib12_16.J_COLOR_SPACE.JCS_RGB && /* red/green/blue */
                        photometric_interpretation != jpeglib12_16.J_COLOR_SPACE.JCS_YCbCr && /* Y/Cb/Cr (also known as YUV) */
                        photometric_interpretation != jpeglib12_16.J_COLOR_SPACE.JCS_CMYK && /* C/M/Y/K */
                        photometric_interpretation != jpeglib12_16.J_COLOR_SPACE.JCS_YCCK)) /* Y/Cb/Cr/K */
                {
                    // photometric_interpretation is unknown or not supported
                    //fprintf(stderr, "Unsupported photometric interpretation: %i\n", photometric_interpretation);
                    return null;
                }
                // not a standard jpeg: use the passed photometric interpretation
                cinfo.jpeg_color_space = photometric_interpretation;
                cinfo.out_color_space = photometric_interpretation;
            }
            // else ijg has got it right, note: this is precisely the logic in SWT and DCMTK
            else
            {
                // always transform to RGB if lossy
                cinfo.out_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_RGB;
            }
        }

        /* Step 4: set parameters for decompression */

        /* In this example, we don't need to change any of the defaults set by
         * jpeg_read_header(), so we do nothing here.
         */

        jdapistd12_16 jdapiStd = new jdapistd12_16();

        /* Step 5: Start decompressor */

        boolean success = jdapiStd.jpeg_start_decompress(cinfo);

        if (!success)
        {
            System.out.println("jdapiStd.jpeg_start_decompress unsuccessful");
        }

        /* We can ignore the return value since suspension is not possible
         * with the stdio data source.
         */

        /* We may need to do some setup of our own at this point before reading
         * the data.  After jpeg_start_decompress() we have the correct scaled
         * output image dimensions available, as well as the output colormap
         * if we asked for color quantization.
         * In this example, we need to make an output work buffer of the right size.
         */
        /* pixels per row in output buffer */
        row_stride = cinfo.output_width * cinfo.output_components;

        int totalSize = cinfo.output_height * cinfo.output_width * cinfo.output_components;

        imageCharacteristics.put(SPP, 2);
        imageCharacteristics.put(SIZE, totalSize);
        imageCharacteristics.put(HEIGHT, cinfo.output_height);
        imageCharacteristics.put(WIDTH, cinfo.output_width);

        // Ensure the image ends on a quad-word boundary. This is important b/c the scanlines at
        // the end of a copy to the output buffer can spill over by 1-3 bytes, which will cause a seg fault
        // if it runs past the end
        while (totalSize % 4 != 0)
        {
            totalSize++;
        }

        int rowsize = row_stride; // number of pixels per row
        // one row of uncompressed data
        short[][] buffer = cinfo.mem.alloc_sarray(cinfo, row_stride, 1);

        // the short[] containing the uncompressed data of size xdim * ydim pixels
        // Note that a pixel may be a byte or a short, either way conversion to a byte[]
        // when the decompressed short[] is created, crates the correct length byte[],
        // eg. xdim*ydim*spp
        short[] imageData = new short[totalSize];

        int return_read_scanline;
        while (cinfo.output_scanline[0] < cinfo.output_height)
        {
            /* jpeg_read_scanlines expects an array of pointers to scanlines.
             * Here the array is only one element long, but you could ask for
             * more than one scanline at a time if that's more convenient.
             */
            return_read_scanline = jdapiStd.jpeg_read_scanlines(cinfo, buffer, 1);

            if (return_read_scanline <= 0 )
            {
                System.out.println("issue reading scanline: " + return_read_scanline);
            }

            /* Assume put_scanline_someplace wants a pointer and sample count. */
            System.arraycopy(buffer[0], 0, imageData, (cinfo.output_scanline[0] - 1) * rowsize, rowsize);
        }

        /* Step 7: Finish decompression */

        boolean is_ok = jdapi.jpeg_finish_decompress(cinfo);

        byte[] decompressed = null;
        if (is_ok)
        {
            decompressed = Driver12_16.ShortToByte_Twiddle_Method(imageData);
        }
        else
        {
            return new byte[0];
        }

        /* And we're done! */
        return decompressed;
    }

    public static byte[] ShortToByte_Twiddle_Method(short [] input)
    {
        int short_index, byte_index;
        int iterations = input.length;

        byte [] buffer = new byte[input.length * 2];

        short_index = byte_index = 0;

        while (short_index != iterations)
        {
            buffer[byte_index]     = (byte) (input[short_index] & 0x00FF);
            buffer[byte_index + 1] = (byte) ((input[short_index] & 0xFF00) >> 8);

            ++short_index; byte_index += 2;
        }

        return buffer;
    }
}
