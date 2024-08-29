package jijg;

import jijg.bit12_16.Driver12_16;
import jijg.bit12_16.jpeglib12_16;
import jijg.bit8.Driver8;
import jijg.bit8.error8.JIJGRuntimeException8;
import jijg.bit8.jpeglib8;

import java.util.HashMap;

public class JIJGDecoder
{
    public byte[] decompress(byte[] imageData, String photometricInterpretation, int bpp,
                             HashMap<String, Integer> imageCharacteristics)
    {
        if (bpp == 8)
        {
            return decompress8Bit(imageData, photometricInterpretation, imageCharacteristics);
        }
        else if (bpp == 10 || bpp == 12 || bpp == 16)
        {
            return decompress12_16Bit(imageData, photometricInterpretation, imageCharacteristics);
        }
        else
        {
            System.err.println("unknown bit depth");
            return null;
        }
    }

    public byte[] decompress8Bit(byte[] imageData, String photometricInterpretation, HashMap<String, Integer> imageCharacteristics)
    {
        byte[] decompressed = null;
        jpeglib8.J_COLOR_SPACE ijgPhotometricInterpretation = jpeglib8.J_COLOR_SPACE.JCS_UNKNOWN;
        switch(photometricInterpretation)
        {
            case "MONOCHROME1":
            case "MONOCHROME2":
            {
                ijgPhotometricInterpretation = jpeglib8.J_COLOR_SPACE.JCS_GRAYSCALE;
                break;
            }
            case "RGB":
            {
                ijgPhotometricInterpretation = jpeglib8.J_COLOR_SPACE.JCS_RGB;
                break;
            }
            case "CMYK":
            {
                ijgPhotometricInterpretation = jpeglib8.J_COLOR_SPACE.JCS_CMYK;
                break;
            }
            case "YCCK":
            {
                ijgPhotometricInterpretation = jpeglib8.J_COLOR_SPACE.JCS_YCCK;
                break;
            }
            default:
            {
                if (photometricInterpretation.contains("YBR"))
                {
                    ijgPhotometricInterpretation = jpeglib8.J_COLOR_SPACE.JCS_YCbCr;
                }
            }
        }

        Driver8 codec = new Driver8();
        try
        {
            decompressed = codec.decompress8(imageData, ijgPhotometricInterpretation,
                    imageCharacteristics);
            if (decompressed == null || decompressed.length == 0)
            {
                System.err.println("could not decompress data");
            }
            return decompressed;
        }
        catch (JIJGRuntimeException8 e)
        {
            e.printStackTrace();
            return decompressed;
        }
    }

    public byte[] decompress12_16Bit(byte[] imageData, String photometricInterpretation, HashMap<String, Integer> imageCharacteristics)
    {
        byte[] decompressed = null;
        jpeglib12_16.J_COLOR_SPACE ijgPhotometricInterpretation = jpeglib12_16.J_COLOR_SPACE.JCS_UNKNOWN;
        switch(photometricInterpretation)
        {
            case "MONOCHROME1":
            case "MONOCHROME2":
            {
                ijgPhotometricInterpretation = jpeglib12_16.J_COLOR_SPACE.JCS_GRAYSCALE;
                break;
            }
            case "RGB":
            {
                ijgPhotometricInterpretation = jpeglib12_16.J_COLOR_SPACE.JCS_RGB;
                break;
            }
            case "CMYK":
            {
                ijgPhotometricInterpretation = jpeglib12_16.J_COLOR_SPACE.JCS_CMYK;
                break;
            }
            case "YCCK":
            {
                ijgPhotometricInterpretation = jpeglib12_16.J_COLOR_SPACE.JCS_YCCK;
                break;
            }
            default:
            {
                if (photometricInterpretation.contains("YBR"))
                {
                    ijgPhotometricInterpretation = jpeglib12_16.J_COLOR_SPACE.JCS_YCbCr;
                }
            }
        }

        Driver12_16 codec = new Driver12_16();

        try
        {
            decompressed = codec.decompress12_16(imageData, ijgPhotometricInterpretation,
                    imageCharacteristics);
            if (decompressed == null || decompressed.length == 0)
            {
                System.err.println("could not decompress data");
            }
            return decompressed;
        }
        catch (JIJGRuntimeException8 e)
        {
            e.printStackTrace();
            return decompressed;
        }
    }
}
