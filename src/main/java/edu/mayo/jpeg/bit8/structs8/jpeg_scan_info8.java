package edu.mayo.jpeg.bit8.structs8;

import edu.mayo.jpeg.bit8.jpeglib8;

public class jpeg_scan_info8
{
    public int comps_in_scan; /* number of components encoded in this scan */
    public int[] component_index = new int[jpeglib8.MAX_COMPS_IN_SCAN]; /* their SOF/comp_info[] indexes */
    public int Ss, Se; /* lossless JPEG predictor select parm (Ss) */
    public int Ah, Al; /* progressive JPEG successive approx. parms lossless JPEG point transform parm (Al) */
}
