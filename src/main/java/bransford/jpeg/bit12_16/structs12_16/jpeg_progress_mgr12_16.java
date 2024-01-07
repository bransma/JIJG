package bransford.jpeg.bit12_16.structs12_16;

public class jpeg_progress_mgr12_16
{
    public long pass_counter;            /* work units completed in this pass */
    public long pass_limit;              /* total number of work units in this pass */
    public int completed_passes;         /* passes completed so far */
    public int total_passes;             /* total number of passes expected */

    public void progress_monitor(jpeg_decompress_struct12_16 cinfo)
    {
        // no-op
    }
}
