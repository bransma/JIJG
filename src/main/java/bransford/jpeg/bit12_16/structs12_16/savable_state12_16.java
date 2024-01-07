package bransford.jpeg.bit12_16.structs12_16;

import bransford.jpeg.bit12_16.jpeglib12_16;

public class savable_state12_16
{
    public int EOBRUN; /* remaining EOBs in EOBRUN (only used by jdphuff)*/
    public int[] last_dc_val = new int[jpeglib12_16.MAX_COMPS_IN_SCAN];

    public savable_state12_16 ASSIGN_STATE(savable_state12_16 dest)
    {
        if (jpeglib12_16.MAX_COMPS_IN_SCAN == 4)
        {
            dest.EOBRUN = this.EOBRUN;
            dest.last_dc_val[0] = this.last_dc_val[0];
            dest.last_dc_val[1] = this.last_dc_val[1];
            dest.last_dc_val[2] = this.last_dc_val[2];
            dest.last_dc_val[3] = this.last_dc_val[3];
        }

        return dest;
    }
}
