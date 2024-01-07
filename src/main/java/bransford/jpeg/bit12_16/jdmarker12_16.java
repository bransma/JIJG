package bransford.jpeg.bit12_16;

import bransford.jpeg.bit12_16.error12_16.ErrorStrings12_16;
import bransford.jpeg.bit12_16.error12_16.TraceStrings12_16;
import bransford.jpeg.bit12_16.structs12_16.*;
import bransford.jpeg.bit12_16.structs12_16.*;

public class jdmarker12_16 extends jpeg_marker_reader12_16
{
    public void jinit_marker_reader(jpeg_decompress_struct12_16 cinfo)
    {
        reset_marker_reader(cinfo);
        src = cinfo.src;
    }

    public void reset_marker_reader(jpeg_decompress_struct12_16 cinfo)
    {
        cinfo.input_scan_number = 0; /* no SOS seen yet */
        cinfo.unread_marker = 0; /* no pending marker */
        saw_SOI = false; /* set internal state too */
        saw_SOF = false;
        discarded_bytes = 0;
        cur_marker = null;
    }

    /*
     * Read markers until SOS or EOI.
     *
     * Returns same codes as are defined for jpeg_consume_input:
     * JPEG_SUSPENDED, JPEG_REACHED_SOS, or JPEG_REACHED_EOI.
     */
    public int read_markers(jpeg_decompress_struct12_16 cinfo)
    {
        /* Outer loop repeats once for each marker. */
        for (; ; )
        {
            // Collect the marker proper, unless we already did. */
            // firstMarker enforces the requirement that SOI appear first. */
            if (cinfo.unread_marker == 0)
            {
                if (!saw_SOI)
                {
                    if (!first_marker(cinfo))
                    {
                        return jpeglib12_16.JPEG_SUSPENDED;
                    }
                }
                else
                {
                    if (!next_marker(cinfo))
                    {
                        return jpeglib12_16.JPEG_SUSPENDED;
                    }
                }
            }
            // At this point cinfo's unread_marker contains the marker code and the
            // input point is just past the marker proper, but before any parameters.
            // A suspension will cause us to return with this state still true
            switch (cinfo.unread_marker)
            {
                case (M_SOI):
                {
                    if (!get_soi(cinfo))
                    {
                        return jpeglib12_16.JPEG_SUSPENDED;
                    }
                    break;
                }
                case M_SOF0: /* Baseline */
                case M_SOF1: /* Extended sequential, Huffman */
                {
                    if (!get_sof(cinfo, jpeglib12_16.J_CODEC_PROCESS.JPROC_SEQUENTIAL, false, jpeglib12_16.DCTSIZE))
                    {
                        return jpeglib12_16.JPEG_SUSPENDED;
                    }
                    break;
                }
                case M_SOF2: /* Progressive, Huffman */
                {
                    if (!get_sof(cinfo, jpeglib12_16.J_CODEC_PROCESS.JPROC_PROGRESSIVE, false, jpeglib12_16.DCTSIZE))
                    {
                        return jpeglib12_16.JPEG_SUSPENDED;
                    }
                    break;
                }
                case M_SOF3: /* Lossless, Huffman */
                {
                    cinfo.is_lossless = true;
                    if (!get_sof(cinfo, jpeglib12_16.J_CODEC_PROCESS.JPROC_LOSSLESS, false, 1))
                    {
                        return jpeglib12_16.JPEG_SUSPENDED;
                    }
                    break;
                }
                case M_SOF9: /* Extended sequential, arithmetic */
                {
                    if (!get_sof(cinfo, jpeglib12_16.J_CODEC_PROCESS.JPROC_SEQUENTIAL, true, jpeglib12_16.DCTSIZE))
                    {
                        return jpeglib12_16.JPEG_SUSPENDED;
                    }
                    break;
                }
                case M_SOF10: /* Progressive, arithmetic */
                {
                    if (!get_sof(cinfo, jpeglib12_16.J_CODEC_PROCESS.JPROC_PROGRESSIVE, true, jpeglib12_16.DCTSIZE))
                    {
                        return jpeglib12_16.JPEG_SUSPENDED;
                    }
                    break;
                }
                case M_SOF11: /* Lossless, arithmetic */
                {
                    if (!get_sof(cinfo, jpeglib12_16.J_CODEC_PROCESS.JPROC_LOSSLESS, true, 1))
                    {
                        return jpeglib12_16.JPEG_SUSPENDED;
                    }
                    break;
                }
                // Currently unsupported SOFn types
                case M_SOF5: /* Differential sequential, Huffman */
                case M_SOF6: /* Differential progressive, Huffman */
                case M_SOF7: /* Differential lossless, Huffman */
                case M_JPG: /* Reserved for JPEG extensions */
                case M_SOF13: /* Differential sequential, arithmetic */
                case M_SOF14: /* Differential progressive, arithmetic */
                case M_SOF15: /* Differential lossless, arithmetic */
                {
                    cinfo.err.ERREXIT1(ErrorStrings12_16.JERR_SOF_UNSUPPORTED, cinfo.unread_marker);
                    return jpeglib12_16.JPEG_SUSPENDED;
                }
                case M_SOS:
                {
                    if (get_sos(cinfo))
                    {
                        cinfo.unread_marker = 0; /* processed the marker */
                        return jpeglib12_16.JPEG_REACHED_SOS;
                    }
                    else
                    {
                        return jpeglib12_16.JPEG_SUSPENDED;
                    }
                }
                case M_EOI:
                {
                    cinfo.unread_marker = 0; /* processed the marker */
                    return jpeglib12_16.JPEG_REACHED_EOI;
                }
                case M_DAC:
                case M_COM:

                case M_DNL: /* Ignore DNL ... perhaps the wrong thing */
                {
                    if (!skip_variable())
                    {
                        return jpeglib12_16.JPEG_SUSPENDED;
                    }
                    break;
                }
                case M_DHT:
                {
                    if (!get_dht(cinfo))
                    {
                        return jpeglib12_16.JPEG_SUSPENDED;
                    }
                    break;
                }
                case M_DQT:
                {
                    if (!get_dqt(cinfo))
                    {
                        return jpeglib12_16.JPEG_SUSPENDED;
                    }
                    break;
                }
                case M_DRI:
                {
                    if (!get_dri(cinfo))
                    {
                        return jpeglib12_16.JPEG_SUSPENDED;
                    }
                    break;
                }
                case M_APP0:
                case M_APP1:
                case M_APP2:
                case M_APP3:
                case M_APP4:
                case M_APP5:
                case M_APP6:
                case M_APP7:
                case M_APP8:
                case M_APP9:
                case M_APP10:
                case M_APP11:
                case M_APP12:
                case M_APP13:
                case M_APP14:
                case M_APP15:
                {
                    // this method reads both APP1&15
                    if (!get_interesting_appn(cinfo))
                    {
                        return jpeglib12_16.JPEG_SUSPENDED;
                    }
                    break;
                }
                case M_RST0: /* these are all parameterless */
                case M_RST1:
                case M_RST2:
                case M_RST3:
                case M_RST4:
                case M_RST5:
                case M_RST6:
                case M_RST7:
                case M_TEM:
                    break;
                default:
                    // must be DHP, EXP, JPGn, or RESn
                    // For now, we treat the reserved markers as fatal errors since they are
                    // likely to be used to signal incompatible JPEG Part 3 extensions.
                    // Once the JPEG 3 version-number marker is well defined, this code
                    // ought to change!
                    cinfo.err.ERREXIT1(ErrorStrings12_16.JERR_UNKNOWN_MARKER, cinfo.unread_marker);
                    break;
            }
            /* Successfully processed marker, so reset state variable */
            cinfo.unread_marker = 0;
        } /* end loop */
    }

    /*
     * Routines to process JPEG markers.
     *
     * Entry condition: JPEG marker itself has been read and its code saved
     *   in cinfo.unread_marker; input restart point is just after the marker.
     *
     * Exit: if return true, have read and processed any parameters, and have
     *   updated the restart point to point after the parameters.
     *   If return false, was forced to suspend before reaching end of
     *   marker parameters; restart point has not been moved.  Same routine
     *   will be called again after application supplies more input data.
     *
     * This approach to suspension assumes that all of a marker's parameters
     * can fit into a single input bufferload.  This should hold for "normal"
     * markers.  Some COM/APPn markers might have large parameter segments
     * that might not fit.  If we are simply dropping such a marker, we use
     * skip_input_data to get past it, and thereby put the problem on the
     * source manager's shoulders.  If we are saving the marker's contents
     * into memory, we use a slightly different convention: when forced to
     * suspend, the marker processor updates the restart point to the end of
     * what it's consumed (ie, the end of the buffer) before returning false.
     * On resumption, cinfo.unread_marker still contains the marker code,
     * but the data source will point to the next chunk of marker data.
     * The marker processor must retain internal state to deal with this.
     *
     * Note that we don't bother to avoid duplicate trace messages if a
     * suspension occurs within marker parameters.  Other side effects
     * require more care.
     */
    private boolean get_soi(jpeg_decompress_struct12_16 cinfo)
        /* Process an SOI marker */
    {
        int i;

        if (saw_SOI)
        {
            cinfo.err.ERREXIT(ErrorStrings12_16.JERR_SOI_DUPLICATE);
            return false;
        }

        /* Reset all parameters that are defined to be reset by SOI */

        for (i = 0; i < jpeglib12_16.NUM_ARITH_TBLS; i++)
        {
            cinfo.arith_dc_L[i] = 0;
            cinfo.arith_dc_U[i] = 1;
            cinfo.arith_ac_K[i] = 5;
        }
        cinfo.restart_interval = 0;

        /* Set initial assumptions for colorspace etc */

        cinfo.jpeg_color_space = jpeglib12_16.J_COLOR_SPACE.JCS_UNKNOWN;
        cinfo.CCIR601_sampling = false; /* Assume non-CCIR sampling??? */

        cinfo.saw_JFIF_marker = false;
        cinfo.JFIF_major_version = 1; /* set default JFIF APP0 values */
        cinfo.JFIF_minor_version = 1;
        cinfo.density_unit = 0;
        cinfo.X_density = 1;
        cinfo.Y_density = 1;
        cinfo.saw_Adobe_marker = false;
        cinfo.Adobe_transform = 0;

        saw_SOI = true;
        return true;
    }

    private boolean get_sof(jpeg_decompress_struct12_16 cinfo, jpeglib12_16.J_CODEC_PROCESS process, boolean is_arith, int data_unit)

        /* Process a SOFn marker */
    {
        long length;
        int c, ci;
        //cinfo.jpegComponentInfo compptr;

        cinfo.data_unit = data_unit;
        cinfo.process = process;
        cinfo.arith_code = is_arith;

        length = src.INPUT_2BYTES();

        cinfo.data_precision = src.INPUT_BYTE();
        jmorecfg12_16.BITS_IN_JSAMPLE = cinfo.data_precision;
        switch (cinfo.data_precision)
        {
            case 12 ->
            {
                jmorecfg12_16.MAXJSAMPLE = 4095;
                jmorecfg12_16.CENTERJSAMPLE = 2048;
                idct_controller12_16.IFAST_SCALE_BITS = 13;
            }
            case 16 ->
            {
                jmorecfg12_16.MAXJSAMPLE = 65535;
                jmorecfg12_16.CENTERJSAMPLE = 32768;
                idct_controller12_16.IFAST_SCALE_BITS = 13;
            }
            default ->
            {
                jmorecfg12_16.MAXJSAMPLE = 255;
                jmorecfg12_16.CENTERJSAMPLE = 128;
                idct_controller12_16.IFAST_SCALE_BITS = 2;
            }
        }
        cinfo.image_height = src.INPUT_2BYTES();
        cinfo.image_width = src.INPUT_2BYTES();
        cinfo.num_components = src.INPUT_BYTE();

        length -= 8;

        if (saw_SOF)
        {
            cinfo.err.ERREXIT(ErrorStrings12_16.JERR_SOF_DUPLICATE);
            return false;
        }

        /* We don't support files in which the image height is initially specified */
        /* as 0 and is later redefined by DNL.  As long as we have to check that,  */
        /* might as well have a general sanity check. */
        if (cinfo.image_height <= 0 || cinfo.image_width <= 0 || cinfo.num_components <= 0)
        {
            cinfo.err.ERREXIT(ErrorStrings12_16.JERR_EMPTY_IMAGE);
            return false;
        }

        if (length != ((long) cinfo.num_components * 3))
        {
            cinfo.err.ERREXIT(ErrorStrings12_16.JERR_BAD_LENGTH);
            return false;
        }

        cinfo.comp_info = new jpeg_component_info12_16[cinfo.num_components];

        for (ci = 0; ci < cinfo.num_components; ci++)
        {
            cinfo.comp_info[ci] = new jpeg_component_info12_16();
            cinfo.comp_info[ci].component_index = ci;
            cinfo.comp_info[ci].component_id = src.INPUT_BYTE();
            c = src.INPUT_BYTE();
            cinfo.comp_info[ci].h_samp_factor = (c >> 4) & 15;
            cinfo.comp_info[ci].v_samp_factor = (c) & 15;
            cinfo.comp_info[ci].quant_tbl_no = src.INPUT_BYTE();
        }

        saw_SOF = true;
        return true;
    }

    private boolean get_sos(jpeg_decompress_struct12_16 cinfo)

        /* Process a SOS marker */
    {
        long length;
        int i, ci, n, c, cc;
        jpeg_component_info12_16 compptr = null;

        if (!saw_SOF)
        {
            cinfo.err.ERREXIT(TraceStrings12_16.SOS_NO_SOF);
            return false;
        }

        length = src.INPUT_2BYTES();

        n = src.INPUT_BYTE();

        if (length != (n * 2L + 6) || n < 1 || n > jpeglib12_16.MAX_COMPS_IN_SCAN)
        {
            cinfo.err.ERREXIT(ErrorStrings12_16.JERR_BAD_LENGTH);
            return false;
        }

        cinfo.comps_in_scan = n;

        /* Collect the component-spec parameters */

        for (i = 0; i < n; i++)
        {
            cc = src.INPUT_BYTE();
            c = src.INPUT_BYTE();

            boolean id_found = false;
            for (ci = 0; ci < cinfo.num_components; ci++)
            {
                if (cc == cinfo.comp_info[ci].component_id)
                {
                    compptr = cinfo.comp_info[ci];
                    id_found = true;
                    break;
                }
            }

            if (!id_found)
            {
                cinfo.err.ERREXIT1(ErrorStrings12_16.JERR_BAD_COMPONENT_ID, cc);
            }
            cinfo.cur_comp_info[i] = compptr;
            compptr.dc_tbl_no = (c >> 4) & 15;
            compptr.ac_tbl_no = (c) & 15;
        }

        /* Collect the additional scan parameters Ss, Se, Ah/Al. */
        c = src.INPUT_BYTE();
        cinfo.Ss = c;
        c = src.INPUT_BYTE();
        cinfo.Se = c;
        c = src.INPUT_BYTE();
        cinfo.Ah = (c >> 4) & 15;
        cinfo.Al = (c) & 15;

        /* Count another SOS marker */
        cinfo.input_scan_number++;

        return true;
    }

    private boolean get_dht(jpeg_decompress_struct12_16 cinfo)

        /* Process a DHT marker */
    {
        long length;
        int i, idx, count;
        JHUFF_TBL12_16 htblptr;

        length = src.INPUT_2BYTES();
        length -= 2;

        while (length > 16)
        {
            byte[] bits = new byte[17];
            byte[] huffval = new byte[256];
            idx = src.INPUT_BYTE();

            bits[0] = 0;
            count = 0;
            for (i = 1; i <= 16; i++)
            {
                bits[i] = (byte) src.INPUT_BYTE();
                count += bits[i];
            }

            length -= 1 + 16;

            /* Here we just do minimal validation of the counts to avoid walking
             * off the end of our table space.  jdhuff.c will check more carefully.
             */
            if (count > 256 || ((long) count) > length)
            {
                cinfo.err.ERREXIT(ErrorStrings12_16.JERR_BAD_HUFF_TABLE);
                return false;
            }

            for (i = 0; i < count; i++)
            {
                huffval[i] = (byte) src.INPUT_BYTE();
            }

            length -= count;

            if ((idx & 0x10) != 0)
            {
                /* AC table definition */
                idx -= 0x10;
                htblptr = cinfo.ac_huff_tbl_ptrs[idx] = new JHUFF_TBL12_16();
            }
            else
            { /* DC table definition */
                htblptr = cinfo.dc_huff_tbl_ptrs[idx] = new JHUFF_TBL12_16();
            }

            if (idx >= jpeglib12_16.NUM_HUFF_TBLS)
            {
                cinfo.err.ERREXIT(ErrorStrings12_16.JERR_DHT_INDEX);
            }

            System.arraycopy(bits, 0, htblptr.bits, 0, bits.length);
            System.arraycopy(huffval, 0, htblptr.huffval, 0, huffval.length);
        }

        if (length != 0)
        {
            cinfo.err.ERREXIT(ErrorStrings12_16.JERR_BAD_LENGTH);
            return false;
        }

        return true;
    }

    private boolean get_dqt(jpeg_decompress_struct12_16 cinfo)

        /* Process a DQT marker */
    {
        long length;
        int n, i, prec;
        int tmp;

        length = src.INPUT_2BYTES();
        length -= 2;

        while (length > 0)
        {
            n = src.INPUT_BYTE();
            prec = n >> 4;
            n &= 0x0F;

            if (n >= jpeglib12_16.NUM_QUANT_TBLS)
            {
                cinfo.err.ERREXIT(ErrorStrings12_16.JERR_DQT_INDEX);
                return false;
            }

            if (cinfo.quant_tbl_ptrs[n] == null)
            {
                cinfo.quant_tbl_ptrs[n] = new JQUANT_TBL12_16();
            }

            for (i = 0; i < jpeglib12_16.DCTSIZE2; i++)
            {
                if (prec > 0)
                {
                    tmp = src.INPUT_2BYTES();
                }
                else
                {
                    tmp = src.INPUT_BYTE();
                }
                /* We convert the zigzag-order table to natural array order. */
                cinfo.quant_tbl_ptrs[n].quantval[jutils12_16.jpeg_natural_order[i]] = (short) tmp;
            }

            length -= jpeglib12_16.DCTSIZE2 + 1;
            if (prec > 0)
            {
                length -= jpeglib12_16.DCTSIZE2;
            }
        }

        if (length != 0)
        {
            cinfo.err.ERREXIT(ErrorStrings12_16.JERR_BAD_LENGTH);
            return false;
        }

        return true;
    }

    private boolean get_dri(jpeg_decompress_struct12_16 cinfo)

        /* Process a DRI marker */
    {
        int length;
        int tmp;

        length = src.INPUT_2BYTES();

        if (length != 4)
        {
            cinfo.err.ERREXIT(ErrorStrings12_16.JERR_BAD_LENGTH);
            return false;
        }

        tmp = src.INPUT_2BYTES();

        cinfo.restart_interval = tmp;

        return true;
    }

    /*
     * Routines for processing APPn and COM markers.
     * These are either saved in memory or discarded, per application request.
     * APP0 and APP14 are specially checked to see if they are
     * JFIF and Adobe markers, respectively.
     */

    private boolean examine_app0(jpeg_decompress_struct12_16 cinfo, byte[] data, long datalen, long remaining)

        /* Examine first few bytes from an APP0.
         * Take appropriate action if it is a JFIF marker.
         * datalen is # of bytes at data[], remaining is length of rest of marker data.
         */
    {
        long totallen = datalen + remaining;

        if (datalen >= APP0_DATA_LEN && (data[0] & 0xFF) == 0x4A && (data[1] & 0xFF) == 0x46 && (data[2] & 0xFF) == 0x49
                && (data[3] & 0xFF) == 0x46 && (data[4] & 0xFF) == 0)
        {
            /* Found JFIF APP0 marker: save info */
            cinfo.saw_JFIF_marker = true;
            cinfo.JFIF_major_version = (byte) (data[5] & 0xFF);
            cinfo.JFIF_minor_version = (byte) (data[6] & 0xFF);
            cinfo.density_unit = (byte) (data[7] & 0xFF);
            cinfo.X_density = (short) (((data[8] & 0xFF) << 8) + (data[9] & 0xFF));
            cinfo.Y_density = (short) (((data[10] & 0xFF) << 8) + (data[11] & 0xFF));
            /* Check version.
             * Major version must be 1, anything else signals an incompatible change.
             * (We used to treat this as an error, but now it's a nonfatal warning,
             * because some bozo at Hijaak couldn't read the spec.)
             * Minor version should be 0..2, but process anyway if newer.
             */
            if (cinfo.JFIF_major_version != 1)
            {
                cinfo.err.WARNMS2(ErrorStrings12_16.JWRN_JFIF_MAJOR, cinfo.JFIF_major_version, cinfo.JFIF_minor_version);
            }

            /* Validate thumbnail dimensions and issue appropriate messages */
            if (((data[12] & 0xFF) | (data[13] & 0xFF)) > 0)
            {
                cinfo.err.WARNMS(TraceStrings12_16.JFIF_THUMBNAIL);
            }

            totallen -= APP0_DATA_LEN;

            if (totallen != ((long) (data[12] & 0xFF) * (long) (data[13] & 0xFF) * (long) 3))
            {
                cinfo.err.WARNMS(TraceStrings12_16.JFIF_BADTHUMBNAILSIZE);
            }
        }
        else if (datalen >= 6 && (data[0] & 0xFF) == 0x4A && (data[1] & 0xFF) == 0x46 && (data[2] & 0xFF) == 0x58
                && (data[3] & 0xFF) == 0x58 && (data[4] & 0xFF) == 0)
        {
            /* Found JFIF "JFXX" extension APP0 marker */
            /* The library doesn't actually do anything with these,
             * but we try to produce a helpful trace message.
             */
            switch ((data[5]) & 0xFF)
            {
                case 0x10 ->
                    System.out.println("found 0x10");
                case 0x11 ->
                    System.out.println("found 0x11");
                case 0x13 ->
                    System.out.println("found 0x13");
            }
        }
        else
        {
            cinfo.err.WARNMS(TraceStrings12_16.APP0 + " total length = " + totallen);
        }

        return true;
    }

    private void examine_app14(jpeg_decompress_struct12_16 cinfo, byte[] data, long datalen)

        /* Examine first few bytes from an APP14.
         * Take appropriate action if it is an Adobe marker.
         * datalen is # of bytes at data[], remaining is length of rest of marker data.
         */
    {
        int transform;

        if (datalen >= APP14_DATA_LEN && (data[0] & 0xFF) == 0x41 && (data[1] & 0xFF) == 0x64 && (data[2] & 0xFF) == 0x6F
                && (data[3] & 0xFF) == 0x62 && (data[4] & 0xFF) == 0x65)
        {
            /* Found Adobe APP14 marker */
            transform = data[11] & 0xFF;
            cinfo.saw_Adobe_marker = true;
            cinfo.Adobe_transform = (byte) transform;
        }
    }

    private boolean get_interesting_appn(jpeg_decompress_struct12_16 cinfo)

        /* Process an APP0 or APP14 marker without saving it */
    {
        int length;
        byte[] b = new byte[APPN_DATA_LEN];
        int i, numtoread;

        length = src.INPUT_2BYTES();
        length -= 2;

        /* get the interesting part of the marker data */
        if (length >= APPN_DATA_LEN)
        {
            numtoread = APPN_DATA_LEN;
        }
        else numtoread = Math.max(length, 0);

        for (i = 0; i < numtoread; i++)
            b[i] = (byte) src.INPUT_BYTE();

        length -= numtoread;

        /* process it */
        switch (cinfo.unread_marker)
        {
            case M_APP0:
                if (examine_app0(cinfo, b, numtoread, length))
                {
                    break;
                }
                else
                {
                    return false;
                }

            case M_APP14:
                examine_app14(cinfo, b, numtoread);
                break;
            default:
                /* can't get here unless jpeg_save_markers chooses wrong processor */
                cinfo.err.ERREXIT(ErrorStrings12_16.JERR_UNKNOWN_MARKER);
                return false;
        }

        /* skip any remaining data -- could be lots */

        if (length > 0)
        {
            src.skip_input_data(length);
        }

        return true;
    }

    /**
     * Like next_marker, but used to obtain the initial SOI marker.
     * For this marker, we do not allow preceding garbage or fill; otherwise,
     * we might well scan an entire input file before realizing it ain't JPEG.
     * If an application wants to process non-JFIF files, it must seek to the
     * SOI before calling the JPEG library.
     */
    private boolean first_marker(jpeg_decompress_struct12_16 cinfo)
    {
        int c = src.INPUT_BYTE();
        int c2 = src.INPUT_BYTE();
        if (c != 0xFF || c2 != M_SOI)
        {
            cinfo.err.ERREXIT2(ErrorStrings12_16.JERR_NO_SOI, c, c2);
        }

        cinfo.unread_marker = c2;
        return true;
    }

    /*
     * Find the next JPEG marker, save it in cinfo.unread_marker.
     * Returns false if had to suspend before reaching a marker;
     * in that case cinfo.unread_marker is unchanged.
     *
     * Note that the result might not be a valid marker code,
     * but it will never be 0 or FF.
     */
    private boolean next_marker(jpeg_decompress_struct12_16 cinfo)
    {
        int c;

        for (; ; )
        {
            c = src.INPUT_BYTE();
            // Skip any non-FF bytes.
            while (c != 0xFF)
            {
                discarded_bytes++;
                c = src.INPUT_BYTE();
            }

            // This loop swallows any duplicate FF bytes.  Extra FFs are legal as
            // pad bytes, so don't count them in discarded_bytes.  We assume there
            // will not be so many consecutive FF bytes as to overflow a suspending
            // data source's input buffer.
            do
            {
                c = src.INPUT_BYTE();
            }
            while (c == 0xFF);

            if (c != 0)
            {
                break; /* found a valid marker, exit loop */
            }

            discarded_bytes += 2;
        }

        if (discarded_bytes != 0)
        {
            cinfo.err.WARNMS2(ErrorStrings12_16.JWRN_EXTRANEOUS_DATA, cinfo.marker.discarded_bytes, c);
            discarded_bytes = 0;
        }

        cinfo.unread_marker = c;

        return true;
    }

    /**
     * Skip over an uninteresting or unknown variable length marker
     */
    private boolean skip_variable()
    {
        int length = src.INPUT_2BYTES() - 2;

        if (length > 0)
        {
            src.skip_input_data(length);
        }

        return true;
    }

    /*
     * Read a restart marker, which is expected to appear next in the datastream;
     * if the marker is not there, take appropriate recovery action.
     * Returns false if suspension is required.
     *
     * This is called by the entropy decoder after it has read an appropriate
     * number of MCUs.  cinfo.unread_marker may be nonzero if the entropy decoder
     * has already read a marker from the data source.  Under normal conditions
     * cinfo.unread_marker will be reset to 0 before returning; if not reset,
     * it holds a marker which the decoder will be unable to read past.
     */

    public boolean read_restart_marker(jpeg_decompress_struct12_16 cinfo)
    {
        /* Obtain a marker unless we already did. */
        /* Note that next_marker will complain if it skips any data. */
        if (cinfo.unread_marker == 0)
        {
            if (!next_marker(cinfo))
                return false;
        }

        if (cinfo.unread_marker == (M_RST0 + cinfo.marker.next_restart_num))
        {
            /* Normal case --- swallow the marker and let entropy decoder continue */
            //TRACEMS1(cinfo, 3, JTRC_RST, cinfo.marker.next_restart_num);
            cinfo.unread_marker = 0;
        }
        else
        {
            /* Uh-oh, the restart markers have been messed up. */
            /* Let the data source manager determine how to resync. */
            if (!jpeg_resync_to_restart(cinfo, cinfo.marker.next_restart_num))
                return false;
        }

        /* Update next-restart state */
        cinfo.marker.next_restart_num = (cinfo.marker.next_restart_num + 1) & 7;

        return true;
    }

    /*
     * This is the default resync_to_restart method for data source managers
     * to use if they don't have any better approach.  Some data source managers
     * may be able to back up, or may have additional knowledge about the data
     * which permits a more intelligent recovery strategy; such managers would
     * presumably supply their own resync method.
     *
     * read_restart_marker calls resync_to_restart if it finds a marker other than
     * the restart marker it was expecting.  (This code is *not* used unless
     * a nonzero restart interval has been declared.)  cinfo.unread_marker is
     * the marker code actually found (might be anything, except 0 or FF).
     * The desired restart marker number (0..7) is passed as a parameter.
     * This routine is supposed to apply whatever error recovery strategy seems
     * appropriate in order to position the input stream to the next data segment.
     * Note that cinfo.unread_marker is treated as a marker appearing before
     * the current data-source input point; usually it should be reset to zero
     * before returning.
     * Returns false if suspension is required.
     *
     * This implementation is substantially constrained by wanting to treat the
     * input as a data stream; this means we can't back up.  Therefore, we have
     * only the following actions to work with:
     *   1. Simply discard the marker and let the entropy decoder resume at next
     *      byte of file.
     *   2. Read forward until we find another marker, discarding intervening
     *      data.  (In theory we could look ahead within the current bufferload,
     *      without having to discard data if we don't find the desired marker.
     *      This idea is not implemented here, in part because it makes behavior
     *      dependent on buffer size and chance buffer-boundary positions.)
     *   3. Leave the marker unread (by failing to zero cinfo.unread_marker).
     *      This will cause the entropy decoder to process an empty data segment,
     *      inserting dummy zeroes, and then we will reprocess the marker.
     *
     * #2 is appropriate if we think the desired marker lies ahead, while #3 is
     * appropriate if the found marker is a future restart marker (indicating
     * that we have missed the desired restart marker, probably because it got
     * corrupted).
     * We apply #2 or #3 if the found marker is a restart marker no more than
     * two counts behind or ahead of the expected one.  We also apply #2 if the
     * found marker is not a legal JPEG marker code (it's certainly bogus data).
     * If the found marker is a restart marker more than 2 counts away, we do #1
     * (too much risk that the marker is erroneous; with luck we will be able to
     * resync at some future point).
     * For any valid non-restart JPEG marker, we apply #3.  This keeps us from
     * overrunning the end of a scan.  An implementation limited to single-scan
     * files might find it better to apply #2 for markers other than EOI, since
     * any other marker would have to be bogus data in that case.
     */

    public boolean jpeg_resync_to_restart(jpeg_decompress_struct12_16 cinfo, int desired)
    {
        int marker = cinfo.unread_marker;
        int action;

        /* Always put up a warning. */
        cinfo.err.WARNMS2(ErrorStrings12_16.JWRN_MUST_RESYNC, marker, desired);

        /* Outer loop handles repeated decision after scanning forward. */
        for (; ; )
        {
            if (marker < M_SOF0)
                action = 2; /* invalid marker */
            else if (marker < M_RST0 || marker > M_RST7)
                action = 3; /* valid non-restart marker */
            else
            {
                if (marker == (M_RST0 + ((desired + 1) & 7)) || marker == (M_RST0 + ((desired + 2) & 7)))
                    action = 3; /* one of the next two expected restarts */
                else if (marker == (M_RST0 + ((desired - 1) & 7)) || marker == (M_RST0 + ((desired - 2) & 7)))
                    action = 2; /* a prior restart, so advance */
                else
                    action = 1; /* desired restart or too far away */
            }

            switch (action)
            {
                case 1:
                    /* Discard marker and let entropy decoder resume processing. */
                    cinfo.unread_marker = 0;
                    return true;
                case 2:
                    /* Scan to the next marker, and repeat the decision loop. */
                    if (!next_marker(cinfo))
                        return false;
                    marker = cinfo.unread_marker;
                    break;
                case 3:
                    /* Return without advancing past this marker. */
                    /* Entropy decoder will be forced to process an empty segment. */
                    return true;
            }
        } /* end loop */
    }
}