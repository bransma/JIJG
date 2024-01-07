package bransford.jpeg.bit12_16;

import bransford.jpeg.bit12_16.error12_16.JIJGRuntimeException12_16;
import bransford.jpeg.bit12_16.structs12_16.jpeg_decompress_struct12_16;
import bransford.jpeg.bit12_16.structs12_16.jpeg_error_mgr12_16;

import java.util.ArrayList;

public class jerror12_16 extends jpeg_error_mgr12_16
{
    public void reset_error_mgr(jpeg_decompress_struct12_16 cinfo)
    {
        num_warnings = 0;
        msg_code = 0;
    }

    public void jpeg_std_error()
    {
        trace_level = 0; /* default = no tracing */
        num_warnings = 0; /* no warnings emitted yet */
        msg_code = 0; /* may be useful as a flag for "no error" */

        /* Initialize message table pointers */
        jpeg_message_table = new ArrayList<String>();
        last_jpeg_message = 0;

        addon_message_table = null;
        first_addon_message = 0; /* for safety */
        last_addon_message = 0;
    }

    public void ERREXIT(String code)
    {
        emit_error_and_exit(code, new Object[]{});
    }

    public void ERREXIT1(String code, Object p1)
    {
        emit_error_and_exit(code, new Object[]{p1});
    }

    public void ERREXIT2(String code, Object p1, Object p2)
    {
        emit_error_and_exit(code, new Object[]{p1, p2});
    }

    public void ERREXIT3(String code, Object p1, Object p2, Object p3)
    {
        emit_error_and_exit(code, new Object[]{p1, p2, p3});
    }

    public void ERREXIT4(String code, Object p1, Object p2, Object p3, Object p4)
    {
        emit_error_and_exit(code, new Object[]{p1, p2, p3, p4});
    }

    public void WARNMS(String code)
    {
        emit_warning(code, new String[]{});
    }

    public void WARNMS1(String code, Object p1)
    {
        emit_warning(code, new Object[]{p1});
    }

    public void WARNMS2(String code, Object p1, Object p2)
    {
        emit_warning(code, new Object[]{p1, p2});
    }

    public void WARNMS3(String code, Object p1, Object p2, Object p3, Object p4)
    {
        emit_warning(code, new Object[]{p1, p2, p3, p4});
    }

    public void emit_error_and_exit(String code, Object[] codes)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Fatal ERROR: ");
        sb.append(code);
        for (Object s : codes)
        {
            sb.append(s).append(" ");
        }

        JIJGRuntimeException12_16 re = new JIJGRuntimeException12_16(sb.toString());
        re.printStackTrace();
        throw re;
    }

    public void emit_warning(String code, Object[] codes)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Warning: ");
        sb.append(code);
        for (Object s : codes)
        {
            sb.append(s).append(" ");
        }

        System.out.println(sb);
    }
 }
