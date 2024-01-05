package edu.mayo.jpeg.bit8.structs8;

import java.util.ArrayList;

public abstract class jpeg_error_mgr8
{
    public static final int JMSG_LENGTH_MAX = 200; /* recommended size of format_message buffer */
    public int num_warnings = 0;
    public int msg_code = 0;
    public int trace_level = 0; /* max msg_level that will be displayed */
    public int last_jpeg_message = 0;
    public int first_addon_message = 0; /* code for first string in addon table */
    public int last_addon_message = 0; /* code for last string in addon table */
    public ArrayList<String> jpeg_message_table;
    public ArrayList<String> addon_message_table;

    /* Error exit handler: does not return to caller */
    public abstract void ERREXIT(String code);

    public abstract void ERREXIT1(String code, Object p1);

    public abstract void ERREXIT2(String code, Object p1, Object p2);

    public abstract void ERREXIT3(String code, Object p1, Object p2, Object p3);

    public abstract void ERREXIT4(String code, Object p1, Object p2, Object p3, Object p4);

    public abstract void WARNMS(String code);

    public abstract void WARNMS1(String code, Object p1);

    public abstract void WARNMS2(String code, Object p1, Object p2);

    public abstract void WARNMS3(String code, Object p1, Object p2, Object p3, Object p4);

    /* Conditionally emit a trace or warning message */
    public abstract void emit_warning(String code, Object[] codes);

    public abstract void reset_error_mgr(jpeg_decompress_struct8 cinfo);

    public abstract void jpeg_std_error();
}
