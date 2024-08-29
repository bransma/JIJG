package jijg.bit8.structs8;

import jijg.bit8.jpeglib8;

public class savable_state8
{
    public int EOBRUN; /* remaining EOBs in EOBRUN (only used by jdphuff)*/
    public int[] last_dc_val = new int[jpeglib8.MAX_COMPS_IN_SCAN];

    public savable_state8 ASSIGN_STATE(savable_state8 dest)
    {
        if (jpeglib8.MAX_COMPS_IN_SCAN == 4)
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
