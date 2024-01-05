package edu.mayo.jpeg.bit8;

import edu.mayo.jpeg.bit8.structs8.jpeg_decompress_struct8;

import static edu.mayo.jpeg.bit8.error8.ErrorStrings8.JERR_ARITH_NOTIMPL;

public class jdarith8
{
    public static void jinit_arith_decoder(jpeg_decompress_struct8 cinfo)
    {
        cinfo.err.ERREXIT(JERR_ARITH_NOTIMPL);
    }
}
