package edu.mayo.jpeg.bit12_16;

import edu.mayo.jpeg.bit12_16.error12_16.ErrorStrings12_16;
import edu.mayo.jpeg.bit12_16.structs12_16.d_derived_tbl12_16;
import edu.mayo.jpeg.bit12_16.structs12_16.jpeg_component_info12_16;
import edu.mayo.jpeg.bit12_16.structs12_16.jpeg_decompress_struct12_16;
import edu.mayo.jpeg.bit12_16.structs12_16.savable_state12_16;

/*
 * jdphuff.c
 *
 * Copyright (C) 1995-1998, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains Huffman entropy decoding routines for progressive JPEG.
 *
 * Much of the complexity here has to do with supporting input suspension.
 * If the data source module demands suspension, we want to be able to back
 * up to the start of the current MCU.  To do this, we copy state variables
 * into local working storage, and update them back to the permanent
 * storage only upon successful completion of an MCU.
 */

/*
 * Private entropy decoder object for progressive Huffman decoding.
 *
 * The savable_state subrecord contains fields that change within an MCU,
 * but must not be updated permanently until we complete the MCU.
 */

public class jdphuff12_16 extends jdhuff12_16
{
    /* Pointers to derived tables (these workspaces have image lifespan) */
    public d_derived_tbl12_16[] derived_tbls = new d_derived_tbl12_16[jpeglib12_16.NUM_HUFF_TBLS];

    public d_derived_tbl12_16 ac_derived_tbl; /* active table during an AC scan */

    /*
     * Initialize for a Huffman-compressed scan.
     */

    public void start_pass_phuff_decoder(jpeg_decompress_struct12_16 cinfo)
    {
        boolean is_DC_band, bad;
        int ci, coefi, tbl;
        int[] coef_bit_ptr;
        jpeg_component_info12_16 compptr;

        is_DC_band = (cinfo.Ss == 0);

        /* Validate scan parameters */
        bad = false;
        if (is_DC_band)
        {
            if (cinfo.Se != 0)
                bad = true;
        }
        else
        {
            /* need not check Ss/Se < 0 since they came from unsigned bytes */
            if (cinfo.Ss > cinfo.Se || cinfo.Se >= jpeglib12_16.DCTSIZE2)
                bad = true;
            /* AC scans may have only one component */
            if (cinfo.comps_in_scan != 1)
                bad = true;
        }
        if (cinfo.Ah != 0)
        {
            /* Successive approximation refinement scan: must have Al = Ah-1. */
            if (cinfo.Al != cinfo.Ah - 1)
                bad = true;
        }
        if (cinfo.Al > 13) /* need not check for < 0 */
            bad = true;
        /* Arguably the maximum Al value should be less than 13 for 8-bit precision,
         * but the spec doesn't say so, and we try to be liberal about what we
         * accept.  Note: large Al values could result in out-of-range DC
         * coefficients during early scans, leading to bizarre displays due to
         * overflows in the IDCT math.  But we won't crash.
         */
        if (bad)
        {
            cinfo.err.ERREXIT4(ErrorStrings12_16.JERR_BAD_PROGRESSION, cinfo.Ss, cinfo.Se, cinfo.Ah, cinfo.Al);
        }
        /* Update progression status, and verify that scan order is legal.
         * Note that inter-scan inconsistencies are treated as warnings
         * not fatal errors ... not clear if this is right way to behave.
         */
        for (ci = 0; ci < cinfo.comps_in_scan; ci++)
        {
            int cindex = cinfo.cur_comp_info[ci].component_index;
            coef_bit_ptr = cinfo.coef_bits[cindex];//[0];

            if (!is_DC_band && coef_bit_ptr[0] < 0)
            {/* AC without prior DC scan */
                cinfo.err.WARNMS2(ErrorStrings12_16.JWRN_BOGUS_PROGRESSION, cindex, 0);
            }

            for (coefi = cinfo.Ss; coefi <= cinfo.Se; coefi++)
            {
                int expected = Math.max(coef_bit_ptr[coefi], 0);
                if (cinfo.Ah != expected)
                    cinfo.err.WARNMS2(ErrorStrings12_16.JWRN_BOGUS_PROGRESSION, cindex, coefi);
                coef_bit_ptr[coefi] = cinfo.Al;
            }
        }

        /* Select MCU decoding routine */
        if (cinfo.Ah == 0)
        {
            if (is_DC_band)
                entropy_decode_mcu = ENTROPY_DECODE_MCU.decode_mcu_DC_first;
            else
                entropy_decode_mcu = ENTROPY_DECODE_MCU.decode_mcu_AC_first;
        }
        else
        {
            if (is_DC_band)
                entropy_decode_mcu = ENTROPY_DECODE_MCU.decode_mcu_DC_refine;
            else
                entropy_decode_mcu = ENTROPY_DECODE_MCU.decode_mcu_AC_refine;
        }

        for (ci = 0; ci < cinfo.comps_in_scan; ci++)
        {
            compptr = cinfo.cur_comp_info[ci];
            /* Make sure requested tables are present, and compute derived tables.
             * We may build same derived table more than once, but it's not expensive.
             */
            if (is_DC_band)
            {
                if (cinfo.Ah == 0)
                { /* DC refinement needs no table */
                    tbl = compptr.dc_tbl_no;
                    jpeg_make_d_derived_tbl(cinfo, true, tbl, derived_tbls);
                }
            }
            else
            {
                tbl = compptr.ac_tbl_no;
                jpeg_make_d_derived_tbl(cinfo, false, tbl, derived_tbls);
                /* remember the single active table */
                ac_derived_tbl = derived_tbls[tbl];
            }
            /* Initialize DC predictions to 0 */
            saved.last_dc_val[ci] = 0;
        }

        /* Initialize bitread state variables */
        bitstate.bits_left = 0;
        bitstate.get_buffer = 0; /* unnecessary, but keeps Purify quiet */
        insufficient_data = false;

        /* Initialize private state variables */
        saved.EOBRUN = 0;

        /* Initialize restart counter */
        restarts_to_go = cinfo.restart_interval;
    }

    /*
     * Check for a restart marker & resynchronize decoder.
     * Returns false if must suspend.
     */
    @Override
    public boolean process_restart(jpeg_decompress_struct12_16 cinfo)
    {
        int ci;

        /* Throw away any unused bits remaining in bit buffer; */
        /* include any full bytes in next_marker's count of discarded bytes */
        cinfo.marker.discarded_bytes += (bitstate.bits_left / 8);
        bitstate.bits_left = 0;

        /* Advance past the RSTn marker */
        if (!(cinfo.marker.read_restart_marker(cinfo)))
            return false;

        /* Re-initialize DC predictions to 0 */
        for (ci = 0; ci < cinfo.comps_in_scan; ci++)
            saved.last_dc_val[ci] = 0;
        /* Re-init EOB run count, too */
        saved.EOBRUN = 0;

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

    /*
     * Huffman MCU decoding.
     * Each of these routines decodes and returns one MCU's worth of
     * Huffman-compressed coefficients.
     * The coefficients are reordered from zigzag order into natural array order,
     * but are not dequantized.
     *
     * The i'th block of the MCU is stored into the block pointed to by
     * MCU_data[i].  WE ASSUME THIS AREA IS INITIALLY ZEROED BY THE CALLER.
     *
     * We return false if data source requested suspension.  In that case no
     * changes have been made to permanent state.  (Exception: some output
     * coefficients may already have been assigned.  This is harmless for
     * spectral selection, since we'll just re-assign them on the next call.
     * Successive approximation AC refinement has to be more careful, however.)
     */

    /*
     * MCU decoding for DC initial scan (either spectral selection,
     * or first pass of successive approximation).
     */
    public boolean decode_mcu_DC_first(jpeg_decompress_struct12_16 cinfo, short[][] MCU_data)
    {
        int Al = cinfo.Al;
        int blkn, ci;
        short[] block;
        savable_state12_16 state = new savable_state12_16();
        d_derived_tbl12_16 tbl;
        jpeg_component_info12_16 compptr;

        /* Process restart marker if needed; may have to suspend */
        if (cinfo.restart_interval == 0)
        {
            if (restarts_to_go == 0)
                if (!process_restart(cinfo))
                    return false;
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
                block = MCU_data[blkn];
                ci = cinfo.MCU_membership[blkn];
                compptr = cinfo.cur_comp_info[ci];
                tbl = derived_tbls[compptr.dc_tbl_no];

                /* Decode a single block's worth of coefficients */

                /* Section F.2.2.1: decode the DC coefficient difference */
                HUFF_DECODE(tbl);
                if (s != 0)
                {
                    CHECK_BIT_BUFFER(s);
                    r = GET_BITS(s);
                    s = HUFF_EXTEND(r);
                }

                /* Convert DC difference to actual value, update last_dc_val */
                s += state.last_dc_val[ci];
                state.last_dc_val[ci] = s;
                /* Scale and output the coefficient (assumes jpeg_natural_order[0]=0) */
                block[0] = (short) (s << Al);
            }

            /* Completed MCU, so update state */
            BITREAD_SAVE_STATE(cinfo, bitstate);
            ASSIGN_STATE(saved, state);
        }

        /* Account for restart interval (no-op if not using restarts) */
        restarts_to_go--;

        return true;
    }

    /*
     * MCU decoding for AC initial scan (either spectral selection,
     * or first pass of successive approximation).
     */
    public boolean decode_mcu_AC_first(jpeg_decompress_struct12_16 cinfo, short[][] MCU_data)
    {
        int Se = cinfo.Se;
        int Al = cinfo.Al;
        int k;
        int EOBRUN;
        short[] block;
        d_derived_tbl12_16 tbl;

        /* Process restart marker if needed; may have to suspend */
        if (cinfo.restart_interval != 0)
        {
            if (restarts_to_go == 0)
                if (!process_restart(cinfo))
                    return false;
        }

        /* If we've run out of data, just leave the MCU set to zeroes.
         * This way, we return uniform gray for the remainder of the segment.
         */
        if (!insufficient_data)
        {
            /* Load up working state.
             * We can avoid loading/saving bitread state if in an EOB run.
             */
            EOBRUN = saved.EOBRUN; /* only part of saved state we need */

            /* There is always only one block per MCU */

            if (EOBRUN > 0)     /* if it's a band of zeroes... */
                EOBRUN--;         /* ...process it now (we do nothing) */
            else
            {
                BITREAD_LOAD_STATE(cinfo, bitstate);
                block = MCU_data[0];
                tbl = ac_derived_tbl;

                for (k = cinfo.Ss; k <= Se; k++)
                {
                    HUFF_DECODE(tbl);
                    r = s >> 4;
                    s &= 15;
                    if (s != 0)
                    {
                        k += r;
                        CHECK_BIT_BUFFER(s);

                        r = GET_BITS(s);
                        s = HUFF_EXTEND(r);
                        /* Scale and output coefficient in natural (dezigzagged) order */
                        block[jutils12_16.jpeg_natural_order[k]] = (short) (s << Al);
                    }
                    else
                    {
                        if (r == 15)
                        {    /* ZRL */
                            k += 15;        /* skip 15 zeroes in band */
                        }
                        else
                        {      /* EOBr, run length is 2^r + appended bits */
                            EOBRUN = (1 << r);
                            if (r != 0)
                            {
                                /* EOBr, r > 0 */
                                CHECK_BIT_BUFFER(r);
                                r = GET_BITS(r);
                                EOBRUN += r;
                            }
                            EOBRUN--;       /* this band is processed at this moment */
                            break;      /* force end-of-band */
                        }
                    }
                }

                BITREAD_SAVE_STATE(cinfo, bitstate);
            }

            /* Completed MCU, so update state */
            saved.EOBRUN = EOBRUN; /* only part of saved state we need */
        }

        /* Account for restart interval (no-op if not using restarts) */
        restarts_to_go--;

        return true;
    }

    /*
     * MCU decoding for DC successive approximation refinement scan.
     * Note: we assume such scans can be multi-component, although the spec
     * is not very clear on the point.
     */
    public boolean decode_mcu_DC_refine(jpeg_decompress_struct12_16 cinfo, short[][] MCU_data)
    {
        int p1 = 1 << cinfo.Al;  /* 1 in the bit position being coded */
        int blkn;
        short[] block;

        /* Process restart marker if needed; may have to suspend */
        if (cinfo.restart_interval != 0)
        {
            if (restarts_to_go == 0)
                if (!process_restart(cinfo))
                    return false;
        }

        /* Not worth the cycles to check insufficient_data here,
         * since we will not change the data anyway if we read zeroes.
         */

        /* Load up working state */
        BITREAD_LOAD_STATE(cinfo, bitstate);

        /* Outer loop handles each block in the MCU */

        for (blkn = 0; blkn < cinfo.data_units_in_MCU; blkn++)
        {
            block = MCU_data[blkn];

            /* Encoded data is simply the next bit of the two's-complement DC value */
            CHECK_BIT_BUFFER(1);
            if (GET_BITS(1) != 0)
                block[0] |= (short) p1;
            /* Note: since we use |=, repeating the assignment later is safe */
        }

        /* Completed MCU, so update state */
        BITREAD_SAVE_STATE(cinfo, bitstate);

        /* Account for restart interval (no-op if not using restarts) */
        restarts_to_go--;

        return true;
    }

    /*
     * MCU decoding for AC successive approximation refinement scan.
     */
    public boolean decode_mcu_AC_refine(jpeg_decompress_struct12_16 cinfo, short[][] MCU_data)
    {
        int Se = cinfo.Se;
        int p1 = 1 << cinfo.Al;  /* 1 in the bit position being coded */
        int m1 = (-1) << cinfo.Al;   /* -1 in the bit position being coded */
        int k;
        int EOBRUN;
        short[] block;
        short[] thiscoef;
        d_derived_tbl12_16 tbl;
        int num_newnz;
        int[] newnz_pos = new int[jpeglib12_16.DCTSIZE2];

        /* Process restart marker if needed; may have to suspend */
        if (cinfo.restart_interval != 0)
        {
            if (restarts_to_go == 0)
                if (!process_restart(cinfo))
                    return false;
        }

        /* If we've run out of data, don't modify the MCU.
         */
        if (!insufficient_data)
        {

            /* Load up working state */
            BITREAD_LOAD_STATE(cinfo, bitstate);
            EOBRUN = saved.EOBRUN; /* only part of saved state we need */

            /* There is always only one block per MCU */
            block = MCU_data[0];
            tbl = ac_derived_tbl;

            /* If we are forced to suspend, we must undo the assignments to any newly
             * nonzero coefficients in the block, because otherwise we'd get confused
             * next time about which coefficients were already nonzero.
             * But we need not undo addition of bits to already-nonzero coefficients;
             * instead, we can test the current bit to see if we already did it.
             */
            num_newnz = 0;

            /* initialize coefficient loop counter to start of band */
            k = cinfo.Ss;

            if (EOBRUN == 0)
            {
                for (; k <= Se; k++)
                {
                    if (!HUFF_DECODE(tbl))
                    {
                        while (num_newnz > 0)
                            block[newnz_pos[--num_newnz]] = 0;

                        return false;
                    }
                    r = s >> 4;
                    s &= 15;
                    if (s != 0)
                    {
                        if (s != 1)       /* size of new coef should always be 1 */
                            cinfo.err.WARNMS(ErrorStrings12_16.JWRN_HUFF_BAD_CODE);
                        if (!CHECK_BIT_BUFFER(1))
                        {
                            while (num_newnz > 0)
                                block[newnz_pos[--num_newnz]] = 0;

                            return false;
                        }
                        if (GET_BITS(1) != 0)
                            s = p1;     /* newly nonzero coef is positive */
                        else
                            s = m1;     /* newly nonzero coef is negative */
                    }
                    else
                    {
                        if (r != 15)
                        {
                            EOBRUN = (1 << r);    /* EOBr, run length is 2^r + appended bits */
                            if (r != 0)
                            {
                                CHECK_BIT_BUFFER(r);
                                r = GET_BITS(r);
                                EOBRUN += r;
                            }
                            break;      /* rest of block is handled by EOB logic */
                        }
                        /* note s = 0 for processing ZRL */
                    }
                    /* Advance over already-nonzero coefs and r still-zero coefs,
                     * appending correction bits to the nonzeroes.  A correction bit is 1
                     * if the absolute value of the coefficient must be increased.
                     */
                    do
                    {
                        thiscoef = block;
                        int thiscoef_offset = jutils12_16.jpeg_natural_order[k];
                        if (thiscoef[thiscoef_offset] != 0)
                        {
                            if (!CHECK_BIT_BUFFER(1))
                            {
                                while (num_newnz > 0)
                                    block[newnz_pos[--num_newnz]] = 0;

                                return false;
                            }
                            if (GET_BITS(1) != 0)
                            {
                                if ((thiscoef[thiscoef_offset] & p1) == 0)
                                { /* do nothing if already set it */
                                    if (thiscoef[thiscoef_offset] >= 0)
                                        thiscoef[thiscoef_offset] += p1;
                                    else
                                        thiscoef[thiscoef_offset] += m1;
                                }
                            }
                        }
                        else
                        {
                            if (--r < 0)
                                break;        /* reached target zero coefficient */
                        }
                        k++;
                    }
                    while (k <= Se);
                    if (s != 0)
                    {
                        int pos = jutils12_16.jpeg_natural_order[k];
                        /* Output newly nonzero coefficient */
                        block[pos] = (short) s;
                        /* Remember its position in case we have to suspend */
                        newnz_pos[num_newnz++] = pos;
                    }
                }
            }

            if (EOBRUN > 0)
            {
                /* Scan any remaining coefficient positions after the end-of-band
                 * (the last newly nonzero coefficient, if any).  Append a correction
                 * bit to each already-nonzero coefficient.  A correction bit is 1
                 * if the absolute value of the coefficient must be increased.
                 */
                for (; k <= Se; k++)
                {
                    thiscoef = block;
                    int thiscoef_offset = jutils12_16.jpeg_natural_order[k];
                    if (thiscoef[thiscoef_offset] != 0)
                    {
                        CHECK_BIT_BUFFER(1);
                        if (GET_BITS(1) != 0)
                        {
                            if ((thiscoef[thiscoef_offset] & p1) == 0)
                            { /* do nothing if already changed it */
                                if (thiscoef[thiscoef_offset] >= 0)
                                    thiscoef[thiscoef_offset] += p1;
                                else
                                    thiscoef[thiscoef_offset] += m1;
                            }
                        }
                    }
                }
                /* Count one block completed in EOB run */
                EOBRUN--;
            }

            /* Completed MCU, so update state */
            BITREAD_SAVE_STATE(cinfo, bitstate);
            saved.EOBRUN = EOBRUN; /* only part of saved state we need */
        }

        /* Account for restart interval (no-op if not using restarts) */
        restarts_to_go--;

        return true;
    }

    /*
     * Module initialization routine for progressive Huffman entropy decoding.
     */
    public void jinit_phuff_decoder(jpeg_decompress_struct12_16 cinfo)
    {
        int[] coef_bit_ptr;
        int ci, i;

        /* Mark derived tables unallocated */
        for (i = 0; i < jpeglib12_16.NUM_HUFF_TBLS; i++)
        {
            derived_tbls[i] = null;
        }

        cinfo.coef_bits = new int[cinfo.num_components][jpeglib12_16.DCTSIZE2];
        /* Create progression status table */
        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            coef_bit_ptr = cinfo.coef_bits[ci];
            for (i = 0; i < jpeglib12_16.DCTSIZE2; i++)
            {
                coef_bit_ptr[i] = -1;
            }
        }
    }

    @Override
    public void entropy_start_pass(jpeg_decompress_struct12_16 cinfo)
    {
        start_pass_phuff_decoder(cinfo);
    }

    @Override
    public boolean entropy_decode_mcu(jpeg_decompress_struct12_16 cinfo, short[][] MCU_data)
    {
        switch (entropy_decode_mcu)
        {
            case decode_mcu_AC_first ->
            {
                return decode_mcu_AC_first(cinfo, MCU_data);
            }
            case decode_mcu_AC_refine ->
            {
                return decode_mcu_AC_refine(cinfo, MCU_data);
            }
            case decode_mcu_DC_first ->
            {
                return decode_mcu_DC_first(cinfo, MCU_data);
            }
            case decode_mcu_DC_refine ->
            {
                return decode_mcu_DC_refine(cinfo, MCU_data);
            }
            default ->
            {
                return false;
            }
        }
    }
}
