package jijg.bit8;

import jijg.bit8.error8.ErrorStrings8;
import jijg.bit8.structs8.JQUANT_TBL8;
import jijg.bit8.structs8.idct_controller8;
import jijg.bit8.structs8.jpeg_component_info8;
import jijg.bit8.structs8.jpeg_decompress_struct8;

import static jijg.bit8.jpeglib8.J_DCT_METHOD.JDCT_ISLOW;

/*
 * The decompressor input side (jdinput.c) saves away the appropriate
 * quantization table for each component at the start of the first scan
 * involving that component.  (This is necessary in order to correctly
 * decode files that reuse Q-table slots.)
 * When we are ready to make an output pass, the saved Q-table is converted
 * to a multiplier table that will actually be used by the IDCT routine.
 * The multiplier table contents are IDCT-method-dependent.  To support
 * application changes in IDCT method between scans, we can remake the
 * multiplier tables if necessary.
 * In buffered-image mode, the first output pass may occur before any data
 * has been seen for some components, and thus before their Q-tables have
 * been saved away.  To handle this case, multiplier tables are preset
 * to zeroes; the result of the IDCT will be a neutral gray level.
 */

public class jddctmgr8 extends idct_controller8
{

    public jddctmgr8()
    {
        if (jmorecfg8.DCT_ISLOW_SUPPORTED)
        {
            PROVIDE_ISLOW_TABLES = true;
        }
        else PROVIDE_ISLOW_TABLES = jmorecfg8.IDCT_SCALING_SUPPORTED;
    }

    @Override
    public void start_pass(jpeg_decompress_struct8 cinfo)
    {
        jdlossy8 lossyd = (jdlossy8) cinfo.codec;
        int ci, i;
        jpeg_component_info8 compptr;
        jpeglib8.J_DCT_METHOD method = null;
        inverse_DCT_method_ptr method_ptr = null;
        JQUANT_TBL8 qtbl;

        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            compptr = cinfo.comp_info[ci];
            /* Select the proper IDCT routine for this component's scaling */
            switch (compptr.codec_data_unit)
            {
                case 1:
                {
                    if (jmorecfg8.IDCT_SCALING_SUPPORTED)
                    {
                        method_ptr = inverse_DCT_method_ptr.jpeg_idct_1x1;
                        method = JDCT_ISLOW; /* jidctred uses islow-style table */
                    }
                    break;
                }
                case 2:
                {
                    if (jmorecfg8.IDCT_SCALING_SUPPORTED)
                    {

                        method_ptr = inverse_DCT_method_ptr.jpeg_idct_2x2;
                        method = JDCT_ISLOW; /* jidctred uses islow-style table */
                    }
                    break;
                }
                case 4:
                {
                    if (jmorecfg8.IDCT_SCALING_SUPPORTED)
                    {

                        method_ptr = inverse_DCT_method_ptr.jpeg_idct_4x4;
                        method = JDCT_ISLOW; /* jidctred uses islow-style table */
                    }
                    break;
                }
                case jpeglib8.DCTSIZE:
                {
                    switch (cinfo.dct_method)
                    {
                        case JDCT_ISLOW:
                        {
                            if (jmorecfg8.DCT_ISLOW_SUPPORTED)
                            {
                                method_ptr = inverse_DCT_method_ptr.jpeg_idct_islow;
                                method = JDCT_ISLOW;
                            }
                            break;
                        }
                        case JDCT_IFAST:
                        {
                            if (jmorecfg8.DCT_IFAST_SUPPORTED)
                            {
                                method_ptr = inverse_DCT_method_ptr.jpeg_idct_ifast;
                                method = jpeglib8.J_DCT_METHOD.JDCT_IFAST;
                            }
                            break;
                        }
                        case JDCT_FLOAT:
                        {
                            if (jmorecfg8.DCT_FLOAT_SUPPORTED)
                            {
                                method_ptr = inverse_DCT_method_ptr.jpeg_idct_float;
                                method = jpeglib8.J_DCT_METHOD.JDCT_FLOAT;
                            }
                            break;
                        }
                        default:
                        {
                            cinfo.err.ERREXIT(ErrorStrings8.JERR_NOT_COMPILED);
                        }
                    }
                    break;
                }
                default:
                {
                    cinfo.err.ERREXIT1(ErrorStrings8.JERR_BAD_DCTSIZE, compptr.codec_data_unit);
                }
            }

            lossyd.inverse_DCT[ci] = method_ptr;
            /* Create multiplier table from quant table.
             * However, we can skip this if the component is uninteresting
             * or if we already built the table.  Also, if no quant table
             * has yet been saved for the component, we leave the
             * multiplier table all-zero; we'll be reading zeroes from the
             * coefficient controller's buffer anyway.
             */
            if (!compptr.component_needed || cur_method[ci] == method)
            {
                continue;
            }

            qtbl = compptr.quant_table;
            if (qtbl == null)
            {
                /* happens if no data yet for component */
                continue;
            }

            cur_method[ci] = method;

            switch (method)
            {
                case JDCT_ISLOW:
                {
                    if (PROVIDE_ISLOW_TABLES)
                    {
                        /* For LL&M IDCT method, multipliers are equal to raw quantization
                         * coefficients, but are stored as ints to ensure access efficiency.
                         */
                        for (i = 0; i < jpeglib8.DCTSIZE2; i++)
                        {
                            compptr.dct_table[i] = qtbl.quantval[i];
                        }
                    }
                    break;
                }
                case JDCT_IFAST:
                {
                    if (jmorecfg8.DCT_IFAST_SUPPORTED)
                    {
                        /* For AA&N IDCT method, multipliers are equal to quantization
                         * coefficients scaled by scalefactor[row]*scalefactor[col], where
                         *   scalefactor[0] = 1
                         *   scalefactor[k] = cos(k*PI/16) * sqrt(2)    for k=1..7
                         * For integer operation, the multiplier table is to be scaled by
                         * IFAST_SCALE_BITS.
                         */
                        for (i = 0; i < jpeglib8.DCTSIZE2; i++)
                        {
                            compptr.dct_table[i] = DESCALE(qtbl.quantval[i] * aanscales[i], CONST_BITS - IFAST_SCALE_BITS);
                        }
                    }
                    break;
                }
                case JDCT_FLOAT:
                {
                    if (jmorecfg8.DCT_FLOAT_SUPPORTED)
                    {
                        /* For float AA&N IDCT method, multipliers are equal to quantization
                         * coefficients scaled by scalefactor[row]*scalefactor[col], where
                         *   scalefactor[0] = 1
                         *   scalefactor[k] = cos(k*PI/16) * sqrt(2)    for k=1..7
                         */
                        int row, col;

                        i = 0;
                        for (row = 0; row < jpeglib8.DCTSIZE; row++)
                        {
                            for (col = 0; col < jpeglib8.DCTSIZE; col++)
                            {
                                compptr.dct_table_f[i] = (float) ((double) qtbl.quantval[i] * aanscalefactor[row]
                                        * aanscalefactor[col]);
                                i++;
                            }
                        }
                    }
                    break;
                }
                default:
                {
                    cinfo.err.ERREXIT(ErrorStrings8.JERR_NOT_COMPILED);
                }
            }
        }
    }

    @Override
    public void jinit_inverse_dct(jpeg_decompress_struct8 cinfo)
    {
        jpeg_component_info8 comptr;

        for (int ci = 0; ci < cinfo.num_components; ci++)
        {
            comptr = cinfo.comp_info[ci];
            comptr.dct_table = new int[jpeglib8.DCTSIZE2];
            comptr.dct_table_f = new float[jpeglib8.DCTSIZE2];
            cur_method[ci] = jpeglib8.J_DCT_METHOD.UNINITIALIZED;
        }
    }

    @Override
    public void inverse_DCT(jpeg_decompress_struct8 cinfo, jpeg_component_info8 compptr, short[] coef_block, byte[][] output_buf,
                            int output_buff_offset, int output_col, inverse_DCT_method_ptr method_ptr)
    {
        switch (method_ptr)
        {
            case jpeg_idct_1x1:
                idctred.jpeg_idct_1x1(cinfo, compptr, coef_block, output_buf, output_buff_offset, output_col);
                break;
            case jpeg_idct_2x2:
                idctred.jpeg_idct_2x2(cinfo, compptr, coef_block, output_buf, output_buff_offset, output_col);
                break;
            case jpeg_idct_4x4:
                idctred.jpeg_idct_4x4(cinfo, compptr, coef_block, output_buf, output_buff_offset, output_col);
                break;
            case jpeg_idct_float:
                idctflt.jpeg_idct_float(cinfo, compptr, coef_block, output_buf, output_buff_offset, output_col);
                break;
            case jpeg_idct_ifast:
                idctfst.jpeg_idct_ifast(cinfo, compptr, coef_block, output_buf, output_buff_offset, output_col);
                break;
            case jpeg_idct_islow:
                idctint.jpeg_idct_islow(cinfo, compptr, coef_block, output_buf, output_buff_offset, output_col);
                break;
            default: cinfo.err.ERREXIT("NO MEHTOD");
        }
    }
}
