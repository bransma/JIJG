package bransford.jpeg.bit12_16;

import bransford.jpeg.bit12_16.error12_16.ErrorStrings12_16;
import bransford.jpeg.bit12_16.structs12_16.d_derived_tbl12_16;
import bransford.jpeg.bit12_16.structs12_16.jpeg_component_info12_16;
import bransford.jpeg.bit12_16.structs12_16.jpeg_decompress_struct12_16;
import bransford.jpeg.bit12_16.structs12_16.savable_state12_16;

/*
 * jdshuff.c
 *
 * Copyright (C) 1991-1998, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains Huffman entropy decoding routines for sequential JPEG.
 *
 * Much of the complexity here has to do with supporting input suspension.
 * If the data source module demands suspension, we want to be able to back
 * up to the start of the current MCU.  To do this, we copy state variables
 * into local working storage, and update them back to the permanent
 * storage only upon successful completion of an MCU.
 */

/*
 * Private entropy decoder object for Huffman decoding.
 *
 * The savable_state subrecord contains fields that change within an MCU,
 * but must not be updated permanently until we complete the MCU.
 */

public class jdshuff12_16 extends jdhuff12_16
{
    /* Pointers to derived tables (these workspaces have image lifespan) */
    public d_derived_tbl12_16[] dc_derived_tbls = new d_derived_tbl12_16[jpeglib12_16.NUM_HUFF_TBLS];
    public d_derived_tbl12_16[] ac_derived_tbls = new d_derived_tbl12_16[jpeglib12_16.NUM_HUFF_TBLS];

    /* Precalculated info set up by start_pass for use in decode_mcu: */

    /* Pointers to derived tables to be used for each block within an MCU */
    public d_derived_tbl12_16[] dc_cur_tbls = new d_derived_tbl12_16[jpeglib12_16.D_MAX_DATA_UNITS_IN_MCU];
    public d_derived_tbl12_16[] ac_cur_tbls = new d_derived_tbl12_16[jpeglib12_16.D_MAX_DATA_UNITS_IN_MCU];
    /* Whether we care about the DC and AC coefficient values for each block */
    public boolean[] dc_needed = new boolean[jpeglib12_16.D_MAX_DATA_UNITS_IN_MCU];
    public boolean[] ac_needed = new boolean[jpeglib12_16.D_MAX_DATA_UNITS_IN_MCU];

    public void start_pass_huff_decoder(jpeg_decompress_struct12_16 cinfo)
    {
        int ci, blkn, dctbl, actbl;
        jpeg_component_info12_16 compptr;

        /* Check that the scan parameters Ss, Se, Ah/Al are OK for sequential JPEG.
         * This ought to be an error condition, but we make it a warning because
         * there are some baseline files out there with all zeroes in these bytes.
         */
        if (cinfo.Ss != 0 || cinfo.Se != jpeglib12_16.DCTSIZE2 - 1 ||
                cinfo.Ah != 0 || cinfo.Al != 0)
            cinfo.err.WARNMS(ErrorStrings12_16.JWRN_NOT_SEQUENTIAL);

        for (ci = 0; ci < cinfo.comps_in_scan; ci++)
        {
            compptr = cinfo.cur_comp_info[ci];
            dctbl = compptr.dc_tbl_no;
            actbl = compptr.ac_tbl_no;
            /* Compute derived values for Huffman tables */
            /* We may do this more than once for a table, but it's not expensive */
            jpeg_make_d_derived_tbl(cinfo, true, dctbl, dc_derived_tbls);//[dctbl]);
            jpeg_make_d_derived_tbl(cinfo, false, actbl, ac_derived_tbls);//[actbl]);
            /* Initialize DC predictions to 0 */
            saved.last_dc_val[ci] = 0;
        }

        /* Precalculate decoding info for each block in an MCU of this scan */
        for (blkn = 0; blkn < cinfo.data_units_in_MCU; blkn++)
        {
            ci = cinfo.MCU_membership[blkn];
            compptr = cinfo.cur_comp_info[ci];
            /* Precalculate which table to use for each block */
            dc_cur_tbls[blkn] = dc_derived_tbls[compptr.dc_tbl_no];
            ac_cur_tbls[blkn] = ac_derived_tbls[compptr.ac_tbl_no];
            /* Decide whether we really care about the coefficient values */
            if (compptr.component_needed)
            {
                dc_needed[blkn] = true;
                /* we don't need the ACs if producing a 1/8th-size image */
                ac_needed[blkn] = (compptr.codec_data_unit > 1);
            }
            else
            {
                dc_needed[blkn] = ac_needed[blkn] = false;
            }
        }

        /* Initialize bitread state variables */
        bitstate.bits_left = 0;
        bitstate.get_buffer = 0; /* unnecessary, but keeps Purify quiet */
        insufficient_data = false;

        /* Initialize restart counter */
        restarts_to_go = cinfo.restart_interval;
    }

    public void jinit_shuff_decoder(jpeg_decompress_struct12_16 cinfo)
    {
        entropy_decode_mcu = ENTROPY_DECODE_MCU.decode_mcu;

        /* Mark tables unallocated */
        for (int i = 0; i < jpeglib12_16.NUM_HUFF_TBLS; i++)
        {
            dc_derived_tbls[i] = ac_derived_tbls[i] = null;
        }
    }

    @Override
    public boolean process_restart(jpeg_decompress_struct12_16 cinfo)
    {
        int ci;

        /* Throw away any unused bits remaining in bit buffer; */
        /* include any full bytes in next_marker's count of discarded bytes */
        cinfo.marker.discarded_bytes += (bitstate.bits_left / 8);
        bitstate.bits_left = 0;

        /* Advance past the RSTn marker */
        if (!cinfo.marker.read_restart_marker(cinfo))
            return false;

        /* Re-initialize DC predictions to 0 */
        for (ci = 0; ci < cinfo.comps_in_scan; ci++)
            saved.last_dc_val[ci] = 0;

        /* Reset restart counter */
        restarts_to_go = cinfo.restart_interval;

        /* Reset out-of-data flag, unless read_restart_marker left us smack up
         * against a marker.  In that case we will end up treating the next data
         * segment as empty, and we can avoid producing bogus output pixels by
         * leaving the flag set.
         */
        if (cinfo.unread_marker == 0)
            insufficient_data = false;

        return true;
    }

    @Override
    public void entropy_start_pass(jpeg_decompress_struct12_16 cinfo)
    {
        start_pass_huff_decoder(cinfo);
    }

    /*
     * Decode and return one MCU's worth of Huffman-compressed coefficients.
     * The coefficients are reordered from zigzag order into natural array order,
     * but are not dequantized.
     *
     * The i'th block of the MCU is stored into the block pointed to by
     * MCU_data[i].  WE ASSUME THIS AREA HAS BEEN ZEROED BY THE CALLER.
     * (Wholesale zeroing is usually a little faster than retail...)
     *
     * Returns false if data source requested suspension.  In that case no
     * changes have been made to permanent state.  (Exception: some output
     * coefficients may already have been assigned.  This is harmless for
     * this module, since we'll just re-assign them on the next call.)
     */

    public boolean decode_mcu(jpeg_decompress_struct12_16 cinfo, short[][] MCU_data)
    {
        int blkn;
        savable_state12_16 state = new savable_state12_16();

        /* Process restart marker if needed; may have to suspend */
        if (cinfo.restart_interval != 0)
        {
            if (restarts_to_go == 0)
            {
                if (!process_restart(cinfo))
                    return false;
            }
        }

        /* If we've run out of data, just leave the MCU set to zeroes.
         * This way, we return uniform gray for the remainder of the segment.
         */
        if (!insufficient_data)
        {
            /* Load up working state */
            BITREAD_LOAD_STATE(cinfo, bitstate);
            ASSIGN_STATE(state, saved);

            /* Outer loop handles each block in the MCU */

            for (blkn = 0; blkn < cinfo.data_units_in_MCU; blkn++)
            {
                short[] block = MCU_data[blkn];
                d_derived_tbl12_16 dctbl = dc_cur_tbls[blkn];
                d_derived_tbl12_16 actbl = ac_cur_tbls[blkn];
                int k, r;

                /* Decode a single block's worth of coefficients */

                /* Section F.2.2.1: decode the DC coefficient difference */
                HUFF_DECODE(dctbl);
                if (s != 0)
                {
                    if (!CHECK_BIT_BUFFER(s))
                    {
                        return false;
                    }
                    r = GET_BITS(s);
                    HUFF_EXTEND(r);
                }

                if (dc_needed[blkn])
                {
                    /* Convert DC difference to actual value, update last_dc_val */
                    int ci = cinfo.MCU_membership[blkn];
                    s += state.last_dc_val[ci];
                    state.last_dc_val[ci] = s;
                    /* Output the DC coefficient (assumes jpeg_natural_order[0] = 0) */
                    block[0] = (short) s;
                }

                if (ac_needed[blkn])
                {

                    /* Section F.2.2.2: decode the AC coefficients */
                    /* Since zeroes are skipped, output area must be cleared beforehand */
                    for (k = 1; k < jpeglib12_16.DCTSIZE2; k++)
                    {
                        HUFF_DECODE(actbl);
                        r = s >> 4;
                        s &= 15;

                        if (s != 0)
                        {
                            k += r;

                            if (!CHECK_BIT_BUFFER(s))
                            {
                                return false;
                            }
                            r = GET_BITS(s);
                            s = HUFF_EXTEND(r);
                            /* Output coefficient in natural (dezigzagged) order.
                             * Note: the extra entries in jpeg_natural_order[] will save us
                             * if k >= DCTSIZE2, which could happen if the data is corrupted.
                             */
                            block[jutils12_16.jpeg_natural_order[k]] = (short) s;
                        }
                        else
                        {
                            if (r != 15)
                                break;
                            k += 15;
                        }
                    }

                }
                else
                {
                    /* Section F.2.2.2: decode the AC coefficients */
                    /* In this path we just discard the values */
                    for (k = 1; k < jpeglib12_16.DCTSIZE2; k++)
                    {
                        HUFF_DECODE(actbl);

                        r = s >> 4;
                        s &= 15;

                        if (s != 0)
                        {
                            k += r;
                            if (!CHECK_BIT_BUFFER(s))
                            {
                                return false;
                            }
                            DROP_BITS(s);
                        }
                        else
                        {
                            if (r != 15)
                                break;
                            k += 15;
                        }
                    }
                }
            }

            /* Completed MCU, so update state */
            BITREAD_SAVE_STATE(cinfo, bitstate);
            ASSIGN_STATE(saved, state);
        }

        restarts_to_go--;

        return true;
    }

    @Override
    public boolean entropy_decode_mcu(jpeg_decompress_struct12_16 cinfo, short[][] MCU_data)
    {
        return decode_mcu(cinfo, MCU_data);
    }
}
