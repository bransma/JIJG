package bransford.jpeg.bit8;

import bransford.jpeg.bit8.structs8.jpeg_decompress_struct8;
import bransford.jpeg.bit8.structs8.jpeg_source_mgr8;

import java.util.ArrayList;
import java.util.HashMap;

/*
 * Here's the routine that will replace the standard error_exit method:
 */
public class Driver8
{
    private static final String SPP = "spp";
    private static final String SIZE = "size";
    private static final String HEIGHT = "height";
    private static final String WIDTH = "width";

    public byte[][] decompress8(byte[][] inmemory, jpeglib8.J_COLOR_SPACE photometric_interpretation,
                              ArrayList<HashMap<String, Integer>> imageCharacteristics)
    {
        HashMap<String, Integer> characteristics;
        byte[][] imageData = new byte[inmemory.length][];
        int i = 0;
        for (byte[] pixels: inmemory)
        {
            characteristics = new HashMap<>();
            imageData[i] = decompress8(pixels, photometric_interpretation, characteristics);
            imageCharacteristics.add(characteristics);
            i++;
        }

        return imageData;
    }

    public byte[] decompress8(byte[] inmemory, jpeglib8.J_COLOR_SPACE photometric_interpretation,
                              HashMap<String, Integer> imageCharacteristics)
    {
        /* This struct contains the JPEG decompression parameters and pointers to
         * working space (which is allocated as needed by the JPEG library).
         */
        jpeg_decompress_struct8 cinfo = new jpeg_decompress_struct8();
        /* We use our private extension JPEG error handler.
         * Note that this struct must live as long as the main JPEG parameter
         * struct, to avoid dangling-pointer problems.
         */
        jerror8 jerr = new jerror8();

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
        cinfo.src = new jpeg_source_mgr8(inmemory);

        jdapimin8 jdapi = new jdapimin8();
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
                if ((photometric_interpretation != jpeglib8.J_COLOR_SPACE.JCS_GRAYSCALE && /* monochrome */
                        photometric_interpretation != jpeglib8.J_COLOR_SPACE.JCS_RGB && /* red/green/blue */
                        photometric_interpretation != jpeglib8.J_COLOR_SPACE.JCS_YCbCr && /* Y/Cb/Cr (also known as YUV) */
                        photometric_interpretation != jpeglib8.J_COLOR_SPACE.JCS_CMYK && /* C/M/Y/K */
                        photometric_interpretation != jpeglib8.J_COLOR_SPACE.JCS_YCCK)) /* Y/Cb/Cr/K */
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
                cinfo.out_color_space = jpeglib8.J_COLOR_SPACE.JCS_RGB;
            }
        }

        /* Step 4: set parameters for decompression */

        /* In this example, we don't need to change any of the defaults set by
         * jpeg_read_header(), so we do nothing here.
         */

        jdapistd8 jdapiStd = new jdapistd8();

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
        /* JSAMPLEs per row in output buffer */
        row_stride = cinfo.output_width * cinfo.output_components;
        /* Make a one-row-high sample array that will go away when done with image */

        /* Step 6: while (scan lines remain to be read) */
        /*           jpeg_read_scanlines(...); */

        /* Here we use the library's state variable cinfo.output_scanline as the
         * loop counter, so that we don't have to keep track ourselves.
         */
        int totalSize = cinfo.output_height * cinfo.output_width * cinfo.output_components;

        imageCharacteristics.put(SPP, cinfo.output_components);
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

        int rowsize = row_stride; // number of bytes per row
        // one row of uncompressed data
        byte[][] buffer = cinfo.mem.alloc_sarray(cinfo, row_stride, 1);

        // the byte[] containing the uncompressed data
        byte[] imageData = new byte[totalSize];

        int return_read_scanline;
        int scanline_num = 1;
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
            //memcpy(uncompressedFrameBuffer + (cinfo.output_scanline - 1) * rowsize, *buffer, rowsize);
        }

        /* Step 7: Finish decompression */

        boolean is_ok = jdapi.jpeg_finish_decompress(cinfo);

        /* And we're done! */
        if (is_ok)
        {
            return imageData;
        }
        else
        {
            return new byte[0];
        }
    }
}
