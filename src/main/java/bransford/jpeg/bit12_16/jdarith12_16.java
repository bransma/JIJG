package bransford.jpeg.bit12_16;

import bransford.jpeg.bit12_16.error12_16.ErrorStrings12_16;
import bransford.jpeg.bit12_16.structs12_16.jpeg_decompress_struct12_16;

public class jdarith12_16
{
    public static void jinit_arith_decoder(jpeg_decompress_struct12_16 cinfo)
    {
        cinfo.err.ERREXIT(ErrorStrings12_16.JERR_ARITH_NOTIMPL);
    }
}
