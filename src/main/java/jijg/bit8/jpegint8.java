package jijg.bit8;

public class jpegint8
{
    public static final int CSTATE_START = 100; /* after create_compress */
    public static final int CSTATE_SCANNING = 101; /* start_compress done, write_scanlines OK */
    public static final int CSTATE_RAW_OK = 102; /* start_compress done, write_raw_data OK */
    public static final int CSTATE_WRCOEFS = 103; /* jpeg_write_coefficients done */
    public static final int DSTATE_START = 200; /* after create_decompress */
    public static final int DSTATE_INHEADER = 201; /* reading header markers, no SOS yet */
    public static final int DSTATE_READY = 202; /* found SOS, ready for start_decompress */
    public static final int DSTATE_PRELOAD = 203; /* reading multiscan file in start_decompress*/
    public static final int DSTATE_PRESCAN = 204; /* performing dummy pass for 2-pass quant */
    public static final int DSTATE_SCANNING = 205; /* start_decompress done, read_scanlines OK */
    public static final int DSTATE_RAW_OK = 206; /* start_decompress done, read_raw_data OK */
    public static final int DSTATE_BUFIMAGE = 207; /* expecting jpeg_start_output */
    public static final int DSTATE_BUFPOST = 208; /* looking for SOS/EOI in jpeg_finish_output */
    public static final int DSTATE_RDCOEFS = 209; /* reading file in jpeg_read_coefficients */
    public static final int DSTATE_STOPPING = 210; /* looking for EOI in jpeg_finish_decompress */

    /* Declarations for both compression & decompression */
    public enum J_BUF_MODE
    { /* Operating modes for buffer controllers */
        JBUF_PASS_THRU, /* Plain stripwise operation */
        /* Remaining modes require a full-image buffer to have been created */
        JBUF_SAVE_SOURCE, /* Run source subobject only, save output */
        JBUF_CRANK_DEST, /* Run dest subobject only, using saved data */
        JBUF_SAVE_AND_PASS /* Run both subobjects, save output */
    }

}
