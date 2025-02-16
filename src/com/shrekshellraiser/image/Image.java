package com.shrekshellraiser.image;

import com.shrekshellraiser.dithers.IDither;
import com.shrekshellraiser.formats.BBF;
import com.shrekshellraiser.formats.BIMG;
import com.shrekshellraiser.formats.Format;
import com.shrekshellraiser.formats.NFP;
import com.shrekshellraiser.modes.*;
import com.shrekshellraiser.palettes.Palette;
import com.shrekshellraiser.utils.Utils;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class Image {
    public BufferedImage[] imageArr;

    public Image(File file, boolean wipeFrames) throws IOException {
        if (Objects.equals(FilenameUtils.getExtension(file.getName()), "gif")) {
            // terrible check for a gif file
            imageArr = GifReader.openGif(file, wipeFrames);
        } else {
            imageArr = new BufferedImage[]{ImageIO.read(file)};
        }
    }

    public Image(String filename, boolean wipeFrames) throws IOException {
        if (Objects.equals(FilenameUtils.getExtension(filename), "gif")) {
            // terrible check for a gif file
            imageArr = GifReader.openGif(new File(filename), wipeFrames);
        } else {
            imageArr = new BufferedImage[]{ImageIO.read(new File(filename))};
        }
    }

    public Image(BufferedImage[] imageArr) {
        this.imageArr = imageArr;
    }

    public BufferedImage[] scale(int sizeX, int sizeY) {
        BufferedImage[] outputImages = new BufferedImage[imageArr.length];
        for (int i = 0; i < outputImages.length; i++) {
            BufferedImage inputImage = Utils.copyImage(imageArr[i]);
            if (inputImage.getWidth() > sizeX) {
                double scale = sizeX / (double) inputImage.getWidth();
                inputImage = Mode.scaleImage(inputImage, scale, scale);
            }
            if (inputImage.getHeight() > sizeY) {
                double scale = sizeY / (double) inputImage.getHeight();
                inputImage = Mode.scaleImage(inputImage, scale, scale);
            }
            System.out.println("Final resolution is " + inputImage.getWidth() + " by "
                    + inputImage.getHeight());
            outputImages[i] = inputImage;
        }
        return outputImages;
    }

    public IMode[] convert(Mode.IM_MODE mode, Palette palette, IDither dither, boolean autoSingle, int blitChar, boolean lowResKMeans) {
        Palette singlePalette = null;
        IMode[] im = new IMode[imageArr.length];
        for (int i = 0; i < imageArr.length; i++) {
            BufferedImage inputImage = imageArr[i];
            long startTime = System.nanoTime();
            if (!autoSingle || i == 0) {
                im[i] = switch (mode) {
                    case HD -> new ModeHighDensity(inputImage, palette, dither);
                    case LD -> new ModeLowDensity(inputImage, palette, dither, blitChar);
                    case HD_AUTO -> new ModeHighDensity(inputImage, dither, lowResKMeans);
                    case LD_AUTO -> new ModeLowDensity(inputImage, dither, blitChar, lowResKMeans);
                };
            } else {
                im[i] = switch (mode) {
                    case HD, HD_AUTO -> new ModeHighDensity(inputImage, singlePalette, dither);
                    case LD, LD_AUTO -> new ModeLowDensity(inputImage, singlePalette, dither, blitChar);
                };
            }
            if (i == 0 && autoSingle) {
                singlePalette = im[0].getPalette();
            }
            long endTime = System.nanoTime();
            System.out.println("Quantized com.shrekshellraiser.image in " + (endTime - startTime) / 1000000.0f + "ms.");
        }
        return im;
    }

    public void save(IMode[] im, Format filetype, String filename) throws IOException {
        long startTime = System.nanoTime();
        long endTime;
        switch (filetype) {
            case BIMG -> {
                BIMG bimg = new BIMG(im);
                double secondsPerFrame = 0.1;
                if (imageArr.length > 1)
                    bimg.writeKeyValuePair("secondsPerFrame", secondsPerFrame);
                bimg.save(filename);
                endTime = System.nanoTime();
                System.out.println("Wrote bimg in " + (endTime - startTime) / 1000000.0f + "ms.");
            }
            case BBF -> {
                BBF bbf = new BBF(im);
                bbf.save(filename);
                endTime = System.nanoTime();
                System.out.println("Wrote bbf in " + (endTime - startTime) / 1000000.0f + "ms.");
            }
            case NFP -> {
                NFP nfp = new NFP(im);
                nfp.save(filename);
                endTime = System.nanoTime();
                System.out.println("Wrote nfp in " + (endTime - startTime) / 1000000.0f + "ms.");
            }
        }
    }
}
