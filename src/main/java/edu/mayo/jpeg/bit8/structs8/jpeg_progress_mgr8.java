package edu.mayo.jpeg.bit8.structs8;

public class jpeg_progress_mgr8
{
    public long pass_counter;            /* work units completed in this pass */
    public long pass_limit;              /* total number of work units in this pass */
    public int completed_passes;         /* passes completed so far */
    public int total_passes;             /* total number of passes expected */

    public void progress_monitor(jpeg_decompress_struct8 cinfo)
    {
        // no-op
    }
}
