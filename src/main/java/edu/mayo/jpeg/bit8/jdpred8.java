package edu.mayo.jpeg.bit8;

import edu.mayo.jpeg.bit8.structs8.jpeg_decompress_struct8;

import static edu.mayo.jpeg.bit8.error8.ErrorStrings8.JERR_BAD_LOSSLESS;

public class jdpred8
{
    /* It is useful to allow each component to have a separate undiff method. */
    enum predict_undifference
    {
        jpeg_undifference1, jpeg_undifference2, jpeg_undifference3, jpeg_undifference4, jpeg_undifference5,
        jpeg_undifference6, jpeg_undifference7, jpeg_undifference_first_row
    }

    public int Ra, Rb, Rc = 0;


    public int PREDICTOR4()
    {
        return (Ra + Rb - Rc);
    }

    public int PREDICTOR5()
    {
        return (Ra + jutils8.RIGHT_SHIFT(Rb - Rc, 1));
    }

    public int PREDICTOR6()
    {
        return (Rb + jutils8.RIGHT_SHIFT(Ra - Rc, 1));
    }

    public int PREDICTOR7()
    {
        return jutils8.RIGHT_SHIFT(Ra + Rb, 1);
    }

    /*
     * 1-Dimensional undifferencer routine.
     *
     * This macro implements the 1-D horizontal predictor (1).  INITIAL_PREDICTOR
     * is used as the special case predictor for the first column, which must be
     * either INITIAL_PREDICTOR2 or INITIAL_PREDICTORx.  The remaining samples
     * use PREDICTOR1.
     *
     * The reconstructed sample is supposed to be calculated modulo 2^16, so we
     * logically AND the result with 0xFFFF.
     */

    public void UNDIFFERENCE_1D(int INITIAL_PREDICTOR, int[] diff_buf, int[] undiff_buf, int width)
    {
        int xindex;

        Ra = (diff_buf[0] + INITIAL_PREDICTOR) & 0xFFFF;
        undiff_buf[0] = Ra;

        for (xindex = 1; xindex < width; xindex++)
        {
            Ra = (diff_buf[xindex] + Ra) & 0xFFFF;
            undiff_buf[xindex] = Ra;
        }
    }

    /*
     * 2-Dimensional undifferencer routine.
     *
     * This macro implements the 2-D horizontal predictors (#2-7).  PREDICTOR2 is
     * used as the special case predictor for the first column.  The remaining
     * samples use PREDICTOR, which is a function of Ra, Rb, Rc.
     *
     * Because prev_row and output_buf may point to the same storage area (in an
     * interleaved image with Vi=1, for example), we must take care to buffer Rb/Rc
     * before writing the current reconstructed sample value into output_buf.
     *
     * The reconstructed sample is supposed to be calculated modulo 2^16, so we
     * logically AND the result with 0xFFFF.
     */

    public void UNDIFFERENCE_2D(int PREDICTOR, int[] diff_buf, int[] undiff_buf, int[] prev_row, int width)
    {
        int xindex;

        Rb = prev_row[0] & 0xFF;
        Ra = (diff_buf[0] + Rb) & 0xFFFF;
        undiff_buf[0] = Ra;

        for (xindex = 1; xindex < width; xindex++)
        {
            Rc = Rb;
            Rb = prev_row[xindex] & 0xFF;
            Ra = (diff_buf[xindex] + PREDICTOR) & 0xFFFF;
            undiff_buf[xindex] = Ra;
        }
    }

    /*
     * Undifferencers for the all rows but the first in a scan or restart interval.
     * The first sample in the row is undifferenced using the vertical
     * predictor (2).  The rest of the samples are undifferenced using the
     * predictor specified in the scan header.
     */

    public void jpeg_undifference1(jpeg_decompress_struct8 cinfo, int comp_index,
                                   int[] diff_buf, int[] prev_row,
                                   int[] undiff_buf, int width)
    {
        UNDIFFERENCE_1D(prev_row[0] & 0xFF, diff_buf, undiff_buf, width);
    }

    public void jpeg_undifference2(jpeg_decompress_struct8 cinfo, int comp_index,
                                   int[] diff_buf, int[] prev_row,
                                   int[] undiff_buf, int width)
    {
        UNDIFFERENCE_2D(Rb, diff_buf, undiff_buf, prev_row, width);
    }

    public void jpeg_undifference3(jpeg_decompress_struct8 cinfo, int comp_index,
                                   int[] diff_buf, int[] prev_row,
                                   int[] undiff_buf, int width)
    {
        UNDIFFERENCE_2D(Rc, diff_buf, undiff_buf, prev_row, width);
    }

    public void jpeg_undifference4(jpeg_decompress_struct8 cinfo, int comp_index,
                                   int[] diff_buf, int[] prev_row,
                                   int[] undiff_buf, int width)
    {
        UNDIFFERENCE_2D(PREDICTOR4(), diff_buf, undiff_buf, prev_row, width);
    }

    public void jpeg_undifference5(jpeg_decompress_struct8 cinfo, int comp_index,
                                   int[] diff_buf, int[] prev_row,
                                   int[] undiff_buf, int width)
    {
        UNDIFFERENCE_2D(PREDICTOR5(), diff_buf, undiff_buf, prev_row, width);
    }

    public void jpeg_undifference6(jpeg_decompress_struct8 cinfo, int comp_index,
                                   int[] diff_buf, int[] prev_row,
                                   int[] undiff_buf, int width)
    {
        UNDIFFERENCE_2D(PREDICTOR6(), diff_buf, undiff_buf, prev_row, width);
    }

    public void jpeg_undifference7(jpeg_decompress_struct8 cinfo, int comp_index,
                                   int[] diff_buf, int[] prev_row,
                                   int[] undiff_buf, int width)
    {
        UNDIFFERENCE_2D(PREDICTOR7(), diff_buf, undiff_buf, prev_row, width);
    }


    public void jpeg_undifference_first_row(jpeg_decompress_struct8 cinfo, int comp_index,
                                            int[] diff_buf, int[] prev_row,
                                            int[] undiff_buf, int width)
    {
        jdlossls8 losslsd = (jdlossls8) cinfo.codec;

        UNDIFFERENCE_1D(1 << (cinfo.data_precision - cinfo.Al - 1), diff_buf, undiff_buf, width);

        /*
         * Now that we have undifferenced the first row, we want to use the
         * undifferencer which corresponds to the predictor specified in the
         * scan header.
         */
        switch (cinfo.Ss)
        {
            case 1 -> losslsd.predict_undifference[comp_index] = predict_undifference.jpeg_undifference1;
            case 2 -> losslsd.predict_undifference[comp_index] = predict_undifference.jpeg_undifference2;
            case 3 -> losslsd.predict_undifference[comp_index] = predict_undifference.jpeg_undifference3;
            case 4 -> losslsd.predict_undifference[comp_index] = predict_undifference.jpeg_undifference4;
            case 5 -> losslsd.predict_undifference[comp_index] = predict_undifference.jpeg_undifference5;
            case 6 -> losslsd.predict_undifference[comp_index] = predict_undifference.jpeg_undifference6;
            case 7 -> losslsd.predict_undifference[comp_index] = predict_undifference.jpeg_undifference7;
        }
    }

    public void predict_start_pass(jpeg_decompress_struct8 cinfo)
    {
        jdlossls8 losslsd = (jdlossls8) cinfo.codec;
        int ci;

        /* Check that the scan parameters Ss, Se, Ah, Al are OK for lossless JPEG.
         *
         * Ss is the predictor selection value (psv).  Legal values for sequential
         * lossless JPEG are: 1 <= psv <= 7.
         *
         * Se and Ah are not used and should be zero.
         *
         * Al specifies the point transform (Pt).  Legal values are: 0 <= Pt <= 15.
         */

        if (cinfo.Ss < 1 || cinfo.Ss > 7 ||
                cinfo.Se != 0 || cinfo.Ah != 0 ||
                cinfo.Al > 15)               /* need not check for < 0 */
            cinfo.err.WARNMS3(JERR_BAD_LOSSLESS, cinfo.Ss, cinfo.Se, cinfo.Ah, cinfo.Al);

        /* Set undifference functions to first row function */
        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            losslsd.predict_undifference[ci] = predict_undifference.jpeg_undifference_first_row;
        }
    }
}
