package jijg.bit8.dct8;

import jijg.bit8.jmorecfg8;
import jijg.bit8.jpeglib8;

public abstract class jdct8
{
    public static final int DCTSIZE = jpeglib8.DCTSIZE;
    public static final int DCTSIZE2 = jpeglib8.DCTSIZE2;

    public static final int RANGE_MASK = (jmorecfg8.MAXJSAMPLE * 4 + 3);
}
