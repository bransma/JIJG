package bransford.jpeg;

import bransford.jpeg.bit8.Driver8;
import bransford.jpeg.bit8.error8.JIJGRuntimeException8;
import bransford.jpeg.bit8.jpeglib8;
import bransford.jpeg.bit12_16.Driver12_16;
import bransford.jpeg.bit12_16.error12_16.JIJGRuntimeException12_16;
import bransford.jpeg.bit12_16.jpeglib12_16;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

/*
 * Here's the routine that will replace the standard error_exit method:
 */
public class Main
{
    public static void main(String[] args)
    {
        Main main = new Main();

        String bitDepth = args[0];
        if (bitDepth.equals("8"))
        {
            main.run8Bit(args[1], args[2], args[3]);
        }
        else if (bitDepth.equals("16"))
        {
            main.run12_16Bit(args[1], args[2], args[3]);
        }
    }

    public void run8Bit(String dcmFile, String photometricInterpretation, String outputFile)
    {
        jpeglib8.J_COLOR_SPACE ijgPhotometricInterpretation = jpeglib8.J_COLOR_SPACE.JCS_UNKNOWN;
        switch(photometricInterpretation)
        {
            case "MONOCHROME1", "MONOCHROME2" ->
            {
                ijgPhotometricInterpretation = jpeglib8.J_COLOR_SPACE.JCS_GRAYSCALE;
            }
            case "RGB" ->
            {
                ijgPhotometricInterpretation = jpeglib8.J_COLOR_SPACE.JCS_RGB;
            }
            case "CMYK" ->
            {
                ijgPhotometricInterpretation = jpeglib8.J_COLOR_SPACE.JCS_CMYK;
            }
            case "YCCK" ->
            {
                ijgPhotometricInterpretation = jpeglib8.J_COLOR_SPACE.JCS_YCCK;
            }
            default ->
            {
                if (photometricInterpretation.contains("YBR"))
                {
                    ijgPhotometricInterpretation = jpeglib8.J_COLOR_SPACE.JCS_YCbCr;
                }
            }
        }

        Driver8 codec = new Driver8();

        // String gs_8bit = "/Users/m056084/dicom/lossless8bit.od";
        // String color = "/Users/m056084/JPegData/color/tiamat.jpeg";
        // String ycbcr = "/Users/m056084/dicom/black_ybr_s2.od";
        // String gs_8bit_lossy = "/Users/m056084/dicom/lossy_pixels.od";
        try
        {
            HashMap<String, Integer> imageCharacteristics = new HashMap<String, Integer>();
            // byte[] decompressed = codec.decompress(readRawData(color), jpeglib.J_COLOR_SPACE.JCS_RGB, imageCharacteristics);
            // byte[] decompressed = codec.decompress(readRawData(gs_8bit_lossy), jpeglib.J_COLOR_SPACE.JCS_GRAYSCALE, imageCharacteristics);
//            byte[] decompressed = codec.decompress8(readRawData(gs_8bit), jpeglib8.J_COLOR_SPACE.JCS_GRAYSCALE,
//                    imageCharacteristics);
//
//            byte[] decompressed = codec.decompress8(readRawData(dcmFile), jpeglib8.J_COLOR_SPACE.JCS_GRAYSCALE,
//                    imageCharacteristics);

            byte[] decompressed = codec.decompress8(readRawData(dcmFile), ijgPhotometricInterpretation,
                    imageCharacteristics);

            if (decompressed.length > 0)
            {
                /* Step 8: Release JPEG decompression object */

                /* This is an important step since it will release a good deal of memory. */
                // outputFile
                //try (FileOutputStream fos = new FileOutputStream("/Users/m056084/dicom/12BitCompressed_dcmpr.od"))
                try (FileOutputStream fos = new FileOutputStream(outputFile))
                {
                    fos.write(decompressed);
                }
                catch (IOException ioe)
                {
                    ioe.printStackTrace();
                }
            }

            // byte[] decompressed = codec.decompress(readRawData(ycbcr), jpeglib.J_COLOR_SPACE.JCS_YCbCr, imageCharacteristics);
            System.out.println(imageCharacteristics + "\nbyte array length = " + decompressed.length);
        }
        catch (IOException | JIJGRuntimeException8 e)
        {
            // System.err.println("Unable to decompress " + ycbcr + " returning null");
            // System.err.println("Unable to decompress " + gs_16bit + " returning null");
            System.err.println("Unable to decompress " + dcmFile + " returning null");
        }
    }

    public void run12_16Bit(String dcmFile, String photometricInterpretation, String outputFile)
    {
        jpeglib12_16.J_COLOR_SPACE ijgPhotometricInterpretation = jpeglib12_16.J_COLOR_SPACE.JCS_UNKNOWN;
        switch(photometricInterpretation)
        {
            case "MONOCHROME1", "MONOCHROME2" ->
            {
                ijgPhotometricInterpretation = jpeglib12_16.J_COLOR_SPACE.JCS_GRAYSCALE;
            }
            case "RGB" ->
            {
                ijgPhotometricInterpretation = jpeglib12_16.J_COLOR_SPACE.JCS_RGB;
            }
            case "CMYK" ->
            {
                ijgPhotometricInterpretation = jpeglib12_16.J_COLOR_SPACE.JCS_CMYK;
            }
            case "YCCK" ->
            {
                ijgPhotometricInterpretation = jpeglib12_16.J_COLOR_SPACE.JCS_YCCK;
            }
            default ->
            {
                if (photometricInterpretation.contains("YBR"))
                {
                    ijgPhotometricInterpretation = jpeglib12_16.J_COLOR_SPACE.JCS_YCbCr;
                }
            }
        }

        Driver12_16 codec = new Driver12_16();

        // String gs_12bit = "/Users/m056084/dicom/JPegData/lossy/12BitCompressed.od";
        // String gs_16bit = "/Users/m056084/dicom/16BitTest.od";

        try
        {
            HashMap<String, Integer> imageCharacteristics = new HashMap<String, Integer>();
//            byte[] decompressed = codec.decompress(readRawData(gs_16bit), jpeglib.J_COLOR_SPACE.JCS_GRAYSCALE,
//                    imageCharacteristics);
            byte[] decompressed = codec.decompress12_16(readRawData(dcmFile), ijgPhotometricInterpretation,
                    imageCharacteristics);

            /* This is an important step since it will release a good deal of memory. */
            //try (FileOutputStream fos = new FileOutputStream("/Users/m056084/dicom/12BitCompressed_dcmpr.od"))
            try (FileOutputStream fos = new FileOutputStream(outputFile))
            {
                fos.write(decompressed);
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
            }

            // byte[] decompressed = codec.decompress(readRawData(ycbcr), jpeglib.J_COLOR_SPACE.JCS_YCbCr, imageCharacteristics);
            System.out.println(imageCharacteristics + "\nbyte array length = " +
                    (decompressed != null ? decompressed.length : 0));
        }
        catch (IOException | JIJGRuntimeException12_16 e)
        {
            // System.err.println("Unable to decompress " + ycbcr + " returning null");
            // System.err.println("Unable to decompress " + gs_16bit + " returning null");
            System.err.println("Unable to decompress " + dcmFile + " returning null");
        }
    }

    public static byte[] readRawData(String fileName) throws IOException
    {
        RandomAccessFile in = new RandomAccessFile(fileName, "r");
        byte[] rawData = new byte[(int) in.length()];
        in.readFully(rawData);
        in.close();
        return rawData;
    }
}
