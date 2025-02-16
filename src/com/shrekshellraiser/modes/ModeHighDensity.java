package com.shrekshellraiser.modes;

import com.shrekshellraiser.ImageMaker;
import com.shrekshellraiser.dithers.IDither;
import com.shrekshellraiser.palettes.Color;
import com.shrekshellraiser.palettes.Palette;
import com.shrekshellraiser.palettes.PaletteImage;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class ModeHighDensity implements IMode {
    private final Palette palette;
    private final int width;
    private final int height;
    private final PaletteImage image;

    public ModeHighDensity(BufferedImage image, Palette palette, IDither dither) {
        this.palette = palette;
        this.image = new PaletteImage(image, this.palette, dither);
        this.width = 2 * (this.image.getWidth() / 2);
        this.height = 3 * (this.image.getHeight() / 3);
    }

    public ModeHighDensity(BufferedImage image, IDither dither, boolean lowResKMeans, Color[] start) {
        this(image, new Palette(KMeans.applyKMeans(lowResKMeans ?
                ImageMaker.resize(image, ImageMaker.kMeansWidth, ImageMaker.kMeansHeight) : image, 16, start)), dither);
    }

    private char[] getChar(int x, int y) {
        // will return a single character blit array ex {'\146', 'a', '0'}
        //                                                ^ except this is a character
        int[] indexFrequency = new int[16];
        Arrays.fill(indexFrequency, 0);
        int BGIndex = 0;
        int FGIndex = 0;
        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 3; dy++) {
                int index = image.getPixelIndex(x + dx, y + dy);
                indexFrequency[index]++;
                if (indexFrequency[index] > indexFrequency[BGIndex]) {
                    FGIndex = BGIndex;
                    BGIndex = index;
                } else if (index != BGIndex && indexFrequency[index] > indexFrequency[FGIndex]) {
                    FGIndex = index;
                }
            }
        }
        int[] crossPaletteLookup = {FGIndex, BGIndex};
        Palette reducedPalette = new Palette(new Color[]{palette.getColor(FGIndex), palette.getColor(BGIndex)});

        int charCode = 0;
        for (int dy = 2; dy >= 0; dy--) {
            for (int dx = 1; dx >= 0; dx--) {
                int index = image.getPixelIndex(x + dx, y + dy);
                int reducedIndex = reducedPalette.getClosestPaletteIndex(new Color(image.getPixelColor(x + dx, y + dy)));
                image.setPixelIndex(x + dx, y + dy, crossPaletteLookup[reducedIndex]);
                charCode = charCode << 1;
                charCode += (reducedIndex == 0) ? 0 : 1;
            }
        }
        // For future reference to convert a 2x3 section of pixels to a 2x3 "braille" character
        // 0x80 + binary count of top left -> bottom right with MS -> LS
        // If LS bit is set invert upper bits
        if ((charCode & 0b100000) > 0) {
            // invert colors
            int tmp = BGIndex;
            BGIndex = FGIndex;
            FGIndex = tmp;
            charCode ^= 0b111111;
        }

//        if t[6] == 1 then for i = 1, 5 do t[i] = 1-t[i] end end
        // t ^ 0b011111
//        local n = 128
//        for i = 0, 4 do n = n + t[i+1]*2^i end
        // n += t & 0b011111
//        return n, t[6] == 1
        charCode += 0x80;
        return new char[]{(char) charCode,
                Integer.toHexString(BGIndex).charAt(0),
                Integer.toHexString(FGIndex).charAt(0)};
    }

    private String[] getRow(int rowNum) {
        String[] im = new String[]{"","",""};
        for (int section = 0; section < this.width-1; section+=2) {
            char[] blit = getChar(section, rowNum);
            im[0] += blit[0];
            im[1] += blit[1];
            im[2] += blit[2];
        }
        return im;
    }

    public String[][] get() {
        String[][] im = new String[height / 3][3];
        for (int row = 0; row < height-2; row+=3) {
            im[row/3] = getRow(row);
        }
        return im;
    }

    public BufferedImage getImage() {
        get();
        return Mode.scaleImage(image.toImage(), 1, 1);
    }

    @Override
    public Palette getPalette() {
        return palette;
    }

    @Override
    public int getWidth() {
        return width / 2;
    }

    @Override
    public int getHeight() {
        return height / 3;
    }

}
