package jijg.bit8;

import jijg.bit8.error8.ErrorStrings8;
import jijg.bit8.structs8.jpeg_decompress_struct8;

public class jdarith8
{
    public static void jinit_arith_decoder(jpeg_decompress_struct8 cinfo)
    {
        cinfo.err.ERREXIT(ErrorStrings8.JERR_ARITH_NOTIMPL);
    }
}
