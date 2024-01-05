package edu.mayo.jpeg.bit12_16.dct12_16;

import edu.mayo.jpeg.bit12_16.jmorecfg12_16;
import edu.mayo.jpeg.bit12_16.jpeglib12_16;

public abstract class jdct12_16
{
    public static final int DCTSIZE = jpeglib12_16.DCTSIZE;
    public static final int DCTSIZE2 = jpeglib12_16.DCTSIZE2;

    public static final int RANGE_MASK = (jmorecfg12_16.MAXJSAMPLE * 4 + 3);
}
