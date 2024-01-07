package bransford.jpeg.bit8;

import bransford.jpeg.bit8.error8.ErrorStrings8;
import bransford.jpeg.bit8.structs8.d_derived_tbl8;
import bransford.jpeg.bit8.structs8.jpeg_component_info8;
import bransford.jpeg.bit8.structs8.jpeg_decompress_struct8;
import bransford.jpeg.bit8.structs8.lhd_output_ptr_info8;

public class jdlhuff8 extends jdhuff8
{
    public d_derived_tbl8[] derived_tbls = new d_derived_tbl8[jpeglib8.NUM_HUFF_TBLS];
    public d_derived_tbl8[] cur_tbls = new d_derived_tbl8[jpeglib8.NUM_HUFF_TBLS];

    public int[][] output_ptr = new int[jpeglib8.D_MAX_DATA_UNITS_IN_MCU][];
    public int num_output_ptrs = 0;

    public lhd_output_ptr_info8[] output_ptr_info = new lhd_output_ptr_info8[jpeglib8.D_MAX_DATA_UNITS_IN_MCU];
    public int[] output_ptr_index = new int[jpeglib8.D_MAX_DATA_UNITS_IN_MCU];

    @Override
    public boolean process_restart(jpeg_decompress_struct8 cinfo)
    {
        return false;
    }

    @Override
    public void entropy_start_pass(jpeg_decompress_struct8 cinfo)
    {
        int ci, dctbl, sampn, ptrn, yoffset, xoffset;
        jpeg_component_info8 compptr;

        for (ci = 0; ci < cinfo.comps_in_scan; ci++)
        {
            compptr = cinfo.cur_comp_info[ci];
            dctbl = compptr.dc_tbl_no;
            /* Make sure requested tables are present */
            if (dctbl < 0 || dctbl >= jpeglib8.NUM_HUFF_TBLS ||
                    cinfo.dc_huff_tbl_ptrs[dctbl] == null)
                cinfo.err.ERREXIT1(ErrorStrings8.JERR_NO_HUFF_TABLE, dctbl);
            /* Compute derived values for Huffman tables */
            /* We may do this more than once for a table, but it's not expensive */
            jpeg_make_d_derived_tbl(cinfo, true, dctbl, derived_tbls);
        }

        /* Precalculate decoding info for each sample in an MCU of this scan */
        for (sampn = 0, ptrn = 0; sampn < cinfo.data_units_in_MCU; )
        {
            compptr = cinfo.cur_comp_info[cinfo.MCU_membership[sampn]];
            ci = compptr.component_index;
            for (yoffset = 0; yoffset < compptr.MCU_height; yoffset++, ptrn++)
            {
                /* Precalculate the setup info for each output pointer */
                if (output_ptr_info[ptrn] == null)
                {
                    output_ptr_info[ptrn] = new lhd_output_ptr_info8();
                }
                output_ptr_info[ptrn].ci = ci;
                output_ptr_info[ptrn].yoffset = yoffset;
                output_ptr_info[ptrn].MCU_width = compptr.MCU_width;
                for (xoffset = 0; xoffset < compptr.MCU_width; xoffset++, sampn++)
                {
                    /* Precalculate the output pointer index for each sample */
                    output_ptr_index[sampn] = ptrn;
                    /* Precalculate which table to use for each sample */
                    cur_tbls[sampn] = derived_tbls[compptr.dc_tbl_no];
                }
            }
        }
        num_output_ptrs = ptrn;

        /* Initialize bitread state variables */
        bitstate.bits_left = 0;
        bitstate.get_buffer = 0; /* unnecessary, but keeps Purify quiet */
        insufficient_data = false;
    }

    @Override
    public boolean entropy_decode_mcu(jpeg_decompress_struct8 cinfo, short[][] MCU_data)
    {
        // lossless - refactor needed to simply not use inheritance for lossy/lossless
        return false;
    }


    public int decode_mcus(jpeg_decompress_struct8 cinfo,
                           int[][][] diff_buf,
                           int MCU_row_num,
                           int MCU_col_num,
                           int nMCU)
    {
        int mcu_num;
        int sampn, ci, yoffset, MCU_width, ptrn;

        /* Set output pointer locations based on MCU_col_num */
        int[] output_ptr_offset = new int[jpeglib8.D_MAX_DATA_UNITS_IN_MCU];
        for (ptrn = 0; ptrn < num_output_ptrs; ptrn++)
        {
            ci = output_ptr_info[ptrn].ci;
            yoffset = output_ptr_info[ptrn].yoffset;
            MCU_width = output_ptr_info[ptrn].MCU_width;
            output_ptr[ptrn] = new int[nMCU];// in pointer arith, points to location ;
            // diff_buf[ci][MCU_row_num + yoffset] = new int[nMCU];
            output_ptr_offset[ptrn] = MCU_col_num * MCU_width;
        }

        /*
         * If we've run out of data, zero out the buffers and return.
         * By resetting the undifferencer, the output samples will be CENTERJSAMPLE.
         *
         * NB: We should find a way to do this without interacting with the
         * undifferencer module directly.
         */
        if (!insufficient_data)
        {
            /* Load up working state */
            BITREAD_LOAD_STATE(cinfo, bitstate);

            /* Outer loop handles the number of MCU requested */

            for (mcu_num = 0; mcu_num < nMCU; mcu_num++)
            {
                /* Inner loop handles the samples in the MCU */
                for (sampn = 0; sampn < cinfo.data_units_in_MCU; sampn++)
                {
                    d_derived_tbl8 dctbl = cur_tbls[sampn];
                    int r;

                    /* Section H.2.2: decode the sample difference */
                    HUFF_DECODE(dctbl);
                    if (s != 0)
                    {
                        if (s == 16)  /* special case: always output 32768 */
                            s = 32768;
                        else
                        {    /* normal case: fetch subsequent bits */
                            CHECK_BIT_BUFFER(s);
                            r = GET_BITS(s);
                            s = HUFF_EXTEND(r);
                        }
                    }

                    /* Output the sample difference */
                     output_ptr[output_ptr_index[sampn]][mcu_num + output_ptr_offset[output_ptr_index[sampn]]] = s;
                }

                /* Completed MCU, so update state */
                BITREAD_SAVE_STATE(cinfo, bitstate);
            }
        }

        for (ptrn = 0; ptrn < num_output_ptrs; ptrn++)
        {
            ci = output_ptr_info[ptrn].ci;
            yoffset = output_ptr_info[ptrn].yoffset;
            System.arraycopy(output_ptr[ptrn], 0, diff_buf[ci][MCU_row_num + yoffset],
                    0, output_ptr[ptrn].length);
        }
        return nMCU;
    }
}