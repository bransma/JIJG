package edu.mayo.jpeg.bit12_16.structs12_16;

/*
 * Implementation is a port of the IJG code as modified by DCMTK
 *
 * jdmarker.c
 *
 * Copyright (C) 1991-1998, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains routines to decode JPEG datastream markers.
 * Most of the complexity arises from our desire to support input
 * suspension: if not all of the data for a marker is available,
 * we must exit back to the application.  On resumption, we reprocess
 * the marker.
 */

public abstract class jpeg_marker_reader12_16
{
    public int next_restart_num; /* next restart number expected (0-7) */
    public int discarded_bytes; /* # of bytes skipped looking for a marker */

    /* Limit on marker data length to save for each marker type */
    public int length_limit_COM;
    public int[] length_limit_APPn = new int[16];

    /* Status of COM/APPn marker saving */
    //jpeg_saved_marker_ptr cur_marker; /* NULL if not processing a marker */
    public int bytes_read; /* data bytes read so far in marker */
    /* Note: cur_marker is not linked into marker_list until it's all read. */

    public static final int APP0_DATA_LEN = 14; /* Length of interesting data in APP0 */
    public static final int APP14_DATA_LEN = 12; /* Length of interesting data in APP14 */
    public static final int APPN_DATA_LEN = 14; /* Must be the largest of the above!! */

    // JPEG marker codes
    public static final int M_SOF0 = 0xc0;
    public static final int M_SOF1 = 0xc1;
    public static final int M_SOF2 = 0xc2;
    public static final int M_SOF3 = 0xc3;

    public static final int M_SOF5 = 0xc5;
    public static final int M_SOF6 = 0xc6;
    public static final int M_SOF7 = 0xc7;

    public static final int M_JPG = 0xc8;
    public static final int M_SOF9 = 0xc9;
    public static final int M_SOF10 = 0xca;
    public static final int M_SOF11 = 0xcb;

    public static final int M_SOF13 = 0xcd;
    public static final int M_SOF14 = 0xce;
    public static final int M_SOF15 = 0xcf;

    public static final int M_DHT = 0xc4;

    public static final int M_DAC = 0xcc;

    public static final int M_RST0 = 0xd0;
    public static final int M_RST1 = 0xd1;
    public static final int M_RST2 = 0xd2;
    public static final int M_RST3 = 0xd3;
    public static final int M_RST4 = 0xd4;
    public static final int M_RST5 = 0xd5;
    public static final int M_RST6 = 0xd6;
    public static final int M_RST7 = 0xd7;

    public static final int M_SOI = 0xd8;
    public static final int M_EOI = 0xd9;
    public static final int M_SOS = 0xda;
    public static final int M_DQT = 0xdb;
    public static final int M_DNL = 0xdc;
    public static final int M_DRI = 0xdd;
    public static final int M_DHP = 0xde;
    public static final int M_EXP = 0xdf;

    public static final int M_APP0 = 0xe0;
    public static final int M_APP1 = 0xe1;
    public static final int M_APP2 = 0xe2;
    public static final int M_APP3 = 0xe3;
    public static final int M_APP4 = 0xe4;
    public static final int M_APP5 = 0xe5;
    public static final int M_APP6 = 0xe6;
    public static final int M_APP7 = 0xe7;
    public static final int M_APP8 = 0xe8;
    public static final int M_APP9 = 0xe9;
    public static final int M_APP10 = 0xea;
    public static final int M_APP11 = 0xeb;
    public static final int M_APP12 = 0xec;
    public static final int M_APP13 = 0xed;
    public static final int M_APP14 = 0xee;
    public static final int M_APP15 = 0xef;

    public static final int M_JPG0 = 0xf0;
    public static final int M_JPG13 = 0xfd;
    public static final int M_COM = 0xfe;

    public static final int M_TEM = 0x01;

    public static final int M_ERROR = 0x100;

    public jpeg_marker12_16 cur_marker; /* NULL if not processing a marker */
    public boolean saw_SOI; /* found SOI? */
    public boolean saw_SOF; /* found SOF? */

    public jpeg_source_mgr12_16 src;

    public abstract void jinit_marker_reader(jpeg_decompress_struct12_16 cinfo);

    public abstract void reset_marker_reader(jpeg_decompress_struct12_16 cinfo);

    /*
     * Read markers until SOS or EOI.
     *
     * Returns same codes as are defined for jpeg_consume_input:
     * JPEG_SUSPENDED, JPEG_REACHED_SOS, or JPEG_REACHED_EOI.
     */

    public abstract int read_markers(jpeg_decompress_struct12_16 cinfo);

    public abstract boolean read_restart_marker(jpeg_decompress_struct12_16 cinfo);
}