package jijg.bit12_16.structs12_16;

import jijg.bit12_16.jpeglib12_16;

public class jpeg_scan_info12_16
{
    public int comps_in_scan; /* number of components encoded in this scan */
    public int[] component_index = new int[jpeglib12_16.MAX_COMPS_IN_SCAN]; /* their SOF/comp_info[] indexes */
    public int Ss, Se; /* lossless JPEG predictor select parm (Ss) */
    public int Ah, Al; /* progressive JPEG successive approx. parms lossless JPEG point transform parm (Al) */
}
