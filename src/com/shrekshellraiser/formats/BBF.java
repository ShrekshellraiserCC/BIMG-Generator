package com.shrekshellraiser.formats;

import com.shrekshellraiser.modes.IMode;
import com.shrekshellraiser.palettes.Palette;
import com.shrekshellraiser.utils.Utils;

import java.io.File;
import java.io.IOException;

import static com.shrekshellraiser.palettes.DefaultPalette.defaultPalette;

public class BBF implements IFormat {
    private final IMode[] frames;

    public BBF(IMode[] frames) {
        this.frames = frames;
    }

    public void save(String filename) throws IOException {
        Utils.writeToFile(filename, Utils.stringToInt(get()));
    }

    public void save(File filename) throws IOException {
        Utils.writeToFile(filename, Utils.stringToInt(get()));
    }

    @Override
    public String get() {
        StringBuilder fileDataStringBuilder = new StringBuilder();
        fileDataStringBuilder.append("BLBFOR1\n").append(frames[0].getWidth()).append("\n")
                .append(frames[0].getHeight()).append("\n")
                .append(frames.length).append("\n")
                .append(System.currentTimeMillis()).append("\n"); // This is supposed to be os.epoch("utc") in lua
        if (frames[0].getPalette().equals(defaultPalette))
            fileDataStringBuilder.append("{}").append("\n"); // this is supposed to be a "meta" parameter
        else {
            fileDataStringBuilder.append("{\"palette\":["); // Open meta
            for (IMode frame : frames) {
                fileDataStringBuilder.append("{");
                Palette palette = frame.getPalette();
                for (int i = 0; i < 16; i++) {
                    fileDataStringBuilder.append("\"").append(i).append("\":")
                            .append(palette.getColor(i));
                    if (i != 15)
                        fileDataStringBuilder.append(",");
                }
                fileDataStringBuilder.append("}");
                if (frame.getPalette().equals(frames[frames.length - 1].getPalette()))
                    break; // palette is the same as the last palette, should test for a single palette used for each frame
                if (frame != frames[frames.length - 1])
                    fileDataStringBuilder.append(",");
            }
            fileDataStringBuilder.append("]}\n");
        }
        for (IMode frame : frames) {
            String[][] frameString = frame.get();
            for (String[] line : frameString) {
                for (int charIndex = 0; charIndex < line[0].length(); charIndex++) {
                    fileDataStringBuilder.append(line[0].charAt(charIndex));
                    int FG = Character.digit(line[1].charAt(charIndex), 16);
                    int BG = Character.digit(line[2].charAt(charIndex), 16);
                    fileDataStringBuilder.append((char) ((FG << 4) + BG));
                }
            }
        }
        return fileDataStringBuilder.toString();
    }
}
