package jijg.bit8;

import jijg.bit8.error8.ErrorStrings8;
import jijg.bit8.structs8.*;

import java.util.Arrays;

public abstract class jdhuff8
{
    // typdef . public long bit_buf_type;	/* type of bit-extraction buffer */
    public static final int BIT_BUF_SIZE = 32; /* size of buffer in bits */
    public static final int HUFF_LOOKAHEAD = 8;

    public boolean insufficient_data = false; /* set true after emmitting warning */
    /* These fields are loaded into local variables at start of each MCU.
     * In case of suspension, we exit WITHOUT updating them.
     */
    public bitread_perm_state8 bitstate = new bitread_perm_state8(); /* Bit buffer at start of MCU */

    public savable_state8 saved = new savable_state8(); /* Other state at start of MCU */

    // Equivalent to BITREAD_STATE_VARS
    public bitread_working_state8 br_state = new bitread_working_state8();
    public int s, r, bits_left = 0;
    public int get_buffer = 0;
    //   

    /* These fields are NOT loaded into local working state. */
    public int restarts_to_go; /* MCUs left in this restart interval */

    public static boolean AVOID_TABLES = false;

    /* entry n is 2**(n-1) */
    public static final int[] extend_test = new int[]{0, 0x0001, 0x0002, 0x0004, 0x0008, 0x0010, 0x0020, 0x0040, 0x0080, 0x0100,
            0x0200, 0x0400, 0x0800, 0x1000, 0x2000, 0x4000};

    /* entry n is (-1 << n) + 1 */
    public static final int[] extend_offset = new int[]{0, ((-1) << 1) + 1, ((-1) << 2) + 1, ((-1) << 3) + 1, ((-1) << 4) + 1,
            ((-1) << 5) + 1, ((-1) << 6) + 1, ((-1) << 7) + 1, ((-1) << 8) + 1, ((-1) << 9) + 1, ((-1) << 10) + 1, ((-1) << 11) + 1,
            ((-1) << 12) + 1, ((-1) << 13) + 1, ((-1) << 14) + 1, ((-1) << 15) + 1};

    /* function pointers to the relevant entropy decoding method */
    public ENTROPY_DECODE_MCU entropy_decode_mcu;

    public enum ENTROPY_DECODE_MCU
    {
        decode_mcu_DC_first, decode_mcu_AC_first, decode_mcu_DC_refine, decode_mcu_AC_refine, decode_mcu, decode_mcus
    }

    public abstract boolean process_restart(jpeg_decompress_struct8 cinfo);

    public abstract void entropy_start_pass(jpeg_decompress_struct8 cinfo);

    /* the various decode methods for jd*huff */
    public abstract boolean entropy_decode_mcu(jpeg_decompress_struct8 cinfo, short[][] MCU_data);

    public void jpeg_make_d_derived_tbl(jpeg_decompress_struct8 cinfo, boolean isDC, int tblno, d_derived_tbl8[] dtbl)
    {
        JHUFF_TBL8 htbl;
        int p, i, l, si, numsymbols;
        int lookbits, ctr;
        byte[] huffsize = new byte[257]; // in C char = byte
        int[] huffcode = new int[257];
        int code;

        /* Note that huffsize[] and huffcode[] are filled in code-length order,
         * paralleling the order of the symbols themselves in htbl.huffval[].
         */

        /* Find the input Huffman table */
        if (tblno < 0 || tblno >= jpeglib8.NUM_HUFF_TBLS)
        {
            cinfo.err.ERREXIT1(ErrorStrings8.JERR_NO_HUFF_TABLE, tblno);
        }

        htbl = isDC ? cinfo.dc_huff_tbl_ptrs[tblno] : cinfo.ac_huff_tbl_ptrs[tblno];
        if (htbl == null)
        {
            cinfo.err.ERREXIT1(ErrorStrings8.JERR_NO_HUFF_TABLE, tblno);
            return;
        }

        if (dtbl[tblno] == null)
        {
            dtbl[tblno] = new d_derived_tbl8();
        }
        dtbl[tblno].pub = htbl; /* fill in back link */

        /* Figure C.1: make table of Huffman code length for each symbol */

        p = 0;
        for (l = 1; l <= 16; l++)
        {
            i = (int) htbl.bits[l] & 0xFF;

            if (p + i > 256)
            {
                /* protect against table overrun */
                cinfo.err.ERREXIT(ErrorStrings8.JERR_BAD_HUFF_TABLE);
            }

            while (i-- != 0)
            {
                huffsize[p++] = (byte) l;
            }
        }
        huffsize[p] = 0;
        numsymbols = p;

        /* Figure C.2: generate the codes themselves */
        /* We also validate that the counts represent a legal Huffman code tree. */

        code = 0;
        si = huffsize[0];
        p = 0;
        while (huffsize[p] != 0)
        {
            while (huffsize[p] == si)
            {
                huffcode[p++] = code;
                code++;
            }
            /* code is now 1 more than the last code used for codelength si; but
             * it must still fit in si bits, since no code is allowed to be all ones.
             * BUG FIX: Comparison must be >, not >=
             */
            if (code > (1 << si))
            {
                cinfo.err.ERREXIT(ErrorStrings8.JERR_BAD_HUFF_TABLE);
            }
            code <<= 1;
            si++;
        }

        /* Figure F.15: generate decoding tables for bit-sequential decoding */

        p = 0;
        for (l = 1; l <= 16; l++)
        {
            if (htbl.bits[l] != 0)
            {
                /* valoffset[l] = huffval[] index of 1st symbol of code length l,
                 * minus the minimum code of length l
                 */
                dtbl[tblno].valoffset[l] = p - huffcode[p];
                p += htbl.bits[l];
                dtbl[tblno].maxcode[l] = huffcode[p - 1]; /* maximum code of length l */
            }
            else
            {
                dtbl[tblno].maxcode[l] = -1; /* -1 if no codes of this length */
            }
        }
        dtbl[tblno].maxcode[17] = 0xFFFFFL; /* ensures jpeg_huff_decode terminates */

        /* Compute lookahead tables to speed up decoding.
         * First we set all the table entries to 0, indicating "too long";
         * then we iterate through the Huffman codes that are short enough and
         * fill in all the entries that correspond to bit sequences starting
         * with that code.
         */

        Arrays.fill(dtbl[tblno].look_nbits, 0);

        p = 0;
        for (l = 1; l <= HUFF_LOOKAHEAD; l++)
        {
            for (i = 1; i <= (int) htbl.bits[l]; i++, p++)
            {
                /* l = current code's length, p = its index in huffcode[] & huffval[]. */
                /* Generate left-justified code followed by all possible bit sequences */
                lookbits = huffcode[p] << (HUFF_LOOKAHEAD - l);
                for (ctr = 1 << (HUFF_LOOKAHEAD - l); ctr > 0; ctr--)
                {
                    dtbl[tblno].look_nbits[lookbits] = l;
                    dtbl[tblno].look_sym[lookbits] = htbl.huffval[p];
                    lookbits++;
                }
            }
        }

        /* Validate symbols as being reasonable.
         * For AC tables, we make no check, but accept all byte values 0..255.
         * For DC tables, we require the symbols to be in range 0..16.
         * (Tighter bounds could be applied depending on the data depth and mode,
         * but this is sufficient to ensure safe decoding.)
         */
        if (isDC)
        {
            for (i = 0; i < numsymbols; i++)
            {
                int sym = htbl.huffval[i];
                if (sym < 0 || sym > 16)
                {
                    cinfo.err.ERREXIT(ErrorStrings8.JERR_BAD_HUFF_TABLE);
                }
            }
        }
    }

    public boolean jpeg_fill_bit_buffer(int nbits)
    {
        /* Copy heavily used state fields into locals (hopefully registers) */
        int bytes_in_buffer = br_state.bytes_in_buffer;
        jpeg_decompress_struct8 cinfo = br_state.cinfo;

        /* Attempt to load at least MIN_GET_BITS bits into get_buffer. */
        /* (It is assumed that no request will be for more than that many bits.) */
        /* We fail to do so only if we hit a marker or are forced to suspend. */

        if (cinfo.unread_marker == 0)
        { /* cannot advance past a marker */
            while (bits_left < MIN_GET_BITS())
            {
                int c;

                /* Attempt to read a byte */
                if (bytes_in_buffer == 0)
                {
                    if (!cinfo.src.fill_input_buffer(cinfo))
                    {
                        return false;
                    }
                }
                // I do this in the src mgr: bytes_in_buffer--;
                c = cinfo.src.GETJOCTET(1);
                bytes_in_buffer = cinfo.src.bytes_in_buffer;

                /* If it's 0xFF, check and discard stuffed zero byte */
                if (c == 0xFF)
                {
                    /* Loop here to discard any padding FF's on terminating marker,
                     * so that we can save a valid unread_marker value.  NOTE: we will
                     * accept multiple FF's followed by a 0 as meaning a single FF data
                     * byte.  This data pattern is not valid according to the standard.
                     */
                    do
                    {
                        if (bytes_in_buffer == 0)
                        {
                            if (!cinfo.src.fill_input_buffer(cinfo))
                            {
                                return false;
                            }
                        }
                        c = cinfo.src.GETJOCTET(1);
                        bytes_in_buffer = cinfo.src.bytes_in_buffer;
                    }
                    while (c == 0xFF);

                    if (c == 0)
                    {
                        /* Found FF/00, which represents an FF data byte */
                        c = 0xFF;
                    }
                    else
                    {
                        /* Oops, it's actually a marker indicating end of compressed data.
                         * Save the marker code for later use.
                         * Fine point: it might appear that we should save the marker into
                         * bitread working state, not straight into permanent state.  But
                         * once we have hit a marker, we cannot need to suspend within the
                         * current MCU, because we will read no more bytes from the data
                         * source.  So it is OK to update permanent state right away.
                         */
                        cinfo.unread_marker = c;
                        /* See if we need to insert some fake zero bits. */
                        if (no_more_bytes(cinfo, nbits))
                        {
                            break;
                        }
                    }
                }

                /* OK, load c into get_buffer */
                get_buffer = (get_buffer << 8) | c;
                bits_left += 8;
            } /* end while */
        }
        else
        {
            no_more_bytes(cinfo, nbits);
            /* We get here if we've read the marker that terminates the compressed
             * data segment.  There should be enough bits in the buffer register
             * to satisfy the request; if so, no problem.
             */
        }

        /* Unload the local registers */
        br_state.next_input_byte = cinfo.src.next_input_byte;
        br_state.bytes_in_buffer = cinfo.src.bytes_in_buffer;
        br_state.get_buffer = get_buffer;
        br_state.bits_left = bits_left;
        br_state.next_byte = cinfo.src.nextByte(cinfo.src.next_input_byte);

        return true;

    }

    public int jpeg_huff_decode(d_derived_tbl8 htbl, int min_bits)
    {
        int l = min_bits;
        long code;

        /* HUFF_DECODE has determined that the code is at least min_bits */
        /* bits long, so fetch that many bits in one swoop. */

        CHECK_BIT_BUFFER(l);
        code = GET_BITS(l);

        /* Collect the rest of the Huffman code one bit at a time. */
        /* This is per Figure F.16 in the JPEG spec. */

        while (code > htbl.maxcode[l])
        {
            code <<= 1;
            CHECK_BIT_BUFFER(1);
            code |= GET_BITS(1);
            l++;
        }

        /* Unload the local registers */
        br_state.get_buffer = get_buffer;
        br_state.bits_left = bits_left;

        /* With garbage input we may reach the sentinel value l = 17. */

        if (l > 16)
        {
            if (br_state.cinfo != null)
                br_state.cinfo.err.WARNMS(ErrorStrings8.JWRN_HUFF_BAD_CODE);

            return 0; /* fake a zero as the safest result */
        }

        return (htbl.pub.huffval[(int) (code + htbl.valoffset[l])]) & 0xFF;
    }

    public final void BITREAD_LOAD_STATE(jpeg_decompress_struct8 cinfo, bitread_perm_state8 bitstate)
    {
        br_state.cinfo = cinfo;
        br_state.next_input_byte = cinfo.src.next_input_byte;
        br_state.next_byte = cinfo.src.nextByte(cinfo.src.next_input_byte);
        br_state.bytes_in_buffer = cinfo.src.bytes_in_buffer;
        get_buffer = bitstate.get_buffer;
        bits_left = bitstate.bits_left;
    }

    public final void BITREAD_SAVE_STATE(jpeg_decompress_struct8 cinfo, bitread_perm_state8 bitstate)
    {
        cinfo.src.next_input_byte = br_state.next_input_byte;
        cinfo.src.bytes_in_buffer = br_state.bytes_in_buffer;
        bitstate.get_buffer = get_buffer;
        bitstate.bits_left = bits_left;
    }

    public final void ASSIGN_STATE(savable_state8 dest, savable_state8 src)
    {
        dest.last_dc_val[0] = src.last_dc_val[0];
        dest.last_dc_val[1] = src.last_dc_val[1];
        dest.last_dc_val[2] = src.last_dc_val[2];
        dest.last_dc_val[3] = src.last_dc_val[3];
    }

    public final boolean HUFF_DECODE(d_derived_tbl8 htbl)
    {
        int nb = 0, look;
        if (bits_left < HUFF_LOOKAHEAD)
        {
            if (!jpeg_fill_bit_buffer(0))
            {
                return false;
            }

            get_buffer = br_state.get_buffer;
            bits_left = br_state.bits_left;

            if (bits_left < HUFF_LOOKAHEAD)
            {
                nb = 1;
                slowlable(htbl, nb);
            }
        }

        if (nb != 1)
        {
            look = PEEK_BITS();
            if ((nb = htbl.look_nbits[look]) != 0)
            {
                DROP_BITS(nb);
                s = htbl.look_sym[look] & 0xFF;
            }
            else
            {
                nb = HUFF_LOOKAHEAD + 1;
                slowlable(htbl, nb);
            }
        }
        return true;
    }

    private int PEEK_BITS()
    {
        return (((get_buffer >> (bits_left - (HUFF_LOOKAHEAD)))) & ((1 << (HUFF_LOOKAHEAD)) - 1));
    }

    public final void DROP_BITS(int nbits)
    {
        bits_left -= nbits;
    }

    public final int GET_BITS(int nbits)
    {
        return (((get_buffer >> (bits_left -= (nbits)))) & ((1 << (nbits)) - 1));
    }

    public final int MIN_GET_BITS()
    {
        return (BIT_BUF_SIZE - 7);
    }

    public final boolean CHECK_BIT_BUFFER(int nbits)
    {
        if (bits_left < (nbits))
        {
            if (!jpeg_fill_bit_buffer(nbits))
            {
                return false;
            }
            get_buffer = br_state.get_buffer;
            bits_left = br_state.bits_left;
        }
        return true;
    }

    public final int HUFF_EXTEND(int x)
    {
        if (AVOID_TABLES)
        {
            return ((x) < (1 << ((s) - 1)) ? (x) + (((-1) << (s)) + 1) : (x));
        }
        else
        {
            return s = ((x) < extend_test[s] ? (x) + extend_offset[s] : (x));
        }

    }

    private void slowlable(d_derived_tbl8 htbl, int nb)
    {
        //System.out.println("in slowable, bits_left = " + bits_left + " | nb = " + nb);
        if ((s = jpeg_huff_decode(htbl, nb)) < 0)
        {
            return;
        }
        get_buffer = br_state.get_buffer;
        bits_left = br_state.bits_left;
    }

    private boolean no_more_bytes(jpeg_decompress_struct8 cinfo, int nbits)
    {
        if (nbits > bits_left)
        {
            /* Uh-oh.  Report corrupted data to user and stuff zeroes into
             * the data stream, so that we can produce some kind of image.
             * We use a nonvolatile flag to ensure that only one warning message
             * appears per data segment.
             */
            jdhuff8 huffd = cinfo.codec.entropy_private;

            if (!huffd.insufficient_data)
            {
                cinfo.err.WARNMS(ErrorStrings8.JWRN_HIT_MARKER);
                huffd.insufficient_data = true;
            }
            /* Fill the buffer with zero bits */
            get_buffer <<= MIN_GET_BITS() - bits_left;
            bits_left = MIN_GET_BITS();
            return false;
        }
        return true;
    }
}
