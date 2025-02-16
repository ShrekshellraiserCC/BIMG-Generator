package com.shrekshellraiser.palettes;

public class Palette {
    private Color[] colors;

    public Palette(Color[] colors) {
        this.colors = colors;
    }

    public Color[] getColors() {
        return colors;
    }
    public Color getColor(int index) {
        return colors[index];
    }

    public int getLength() {
        return colors.length;
    }

    public int getClosestPaletteIndex(Color color) {
        int closestIndex = 0;
        double closestDiff = 0xFFFFFF;
        for (int index = 0; index < colors.length; index++) {
            double colorDiff = color.diff(colors[index]);
            if (colorDiff < closestDiff) {
                closestDiff = colorDiff;
                closestIndex = index;
            }
        }
        return closestIndex;
    }

    public boolean equals(Palette palette) {
        if (this.getLength() == palette.getLength()) {
            for (int colorIndex = 0; colorIndex < this.getLength(); colorIndex++) {
                if (getColor(colorIndex) != palette.getColor(colorIndex))
                    return false;
            }
            return true;
        }
        return false;
    }
}
