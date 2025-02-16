package com.shrekshellraiser;

import com.shrekshellraiser.dithers.DitherFloydSteinberg;
import com.shrekshellraiser.dithers.DitherNone;
import com.shrekshellraiser.dithers.DitherOrdered;
import com.shrekshellraiser.dithers.IDither;
import com.shrekshellraiser.formats.BBF;
import com.shrekshellraiser.formats.BIMG;
import com.shrekshellraiser.formats.IFormat;
import com.shrekshellraiser.formats.NFP;
import com.shrekshellraiser.modes.*;
import com.shrekshellraiser.palettes.Color;
import com.shrekshellraiser.palettes.DefaultPalette;
import com.shrekshellraiser.palettes.Palette;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ImageMaker {
    public static final Palette defaultPalette = DefaultPalette.defaultPalette;
    static Palette palette = defaultPalette;
    static IM_MODE mode = IM_MODE.LD;

    private final Option option_postImageFile = Option.builder("post")
            .required(false)
            .desc("Output processed image to path")
            .hasArg(true)
            .build();
    private final Option option_doDither = Option.builder("d")
            .required(false)
            .desc("Do Floyd-Steinberg dithering")
            .longOpt("dither")
            .hasArg(false)
            .build();
    private final Option option_doOrderedDither = Option.builder("ordered")
            .required(false)
            .desc("Do ordered dithering")
            .valueSeparator(',')
            .hasArgs()
            .build();
    private final Option option_highDensity = Option.builder("hd")
            .required(false)
            .desc("High density")
            .hasArg(false)
            .build();
    private final Option option_customPalette = Option.builder("p")
            .required(false)
            .desc("Comma separated list of palette colors")
            .longOpt("palette")
            .hasArgs()
            .valueSeparator(',')
            .build();
    private final Option option_autoPalette = Option.builder("auto")
            .required(false)
            .desc("Automatically generate palette")
            .hasArg(false)
            .build();
    private final Option option_secondsPerFrame = Option.builder("spf")
            .required(false)
            .desc("Seconds per frame")
            .hasArg(true)
            .build();
    private final Option option_uncapResolution = Option.builder()
            .required(false)
            .desc("Uncap the resolution")
            .longOpt("uncapresolution")
            .hasArg(false)
            .build();
    private final Option option_bbf = Option.builder("bbf")
            .required(false)
            .desc("Save output in bbf format")
            .hasArg(false)
            .build();
    private final Option option_nfp = Option.builder("nfp")
            .required(false)
            .desc("Save output in nfp format")
            .hasArg(false)
            .build();
    private final Option option_wipeFrames = Option.builder()
            .required(false)
            .desc("Empty each frame of gifs")
            .longOpt("wipeframes")
            .hasArg(false)
            .build();
    private final Option option_autoSingle = Option.builder()
            .required(false)
            .desc("Automatically generate a palette for the first frame, and use the same for all others")
            .longOpt("autosingle")
            .hasArg(false)
            .build();
    private final Option option_silent = Option.builder()
            .required(false)
            .desc("Silently process")
            .longOpt("silent")
            .hasArg(false)
            .build();
    private final Option option_stream = Option.builder()
            .required(false)
            .desc("Open a websocket stream")
            .longOpt("stream")
            .hasArg(false)
            .build();
    private final Option option_streamPort = Option.builder()
            .required(false)
            .desc("Choose the port for the websocket stream")
            .longOpt("port")
            .hasArg(true)
            .build();
    private final Option option_resize = Option.builder()
            .required(false)
            .desc("Set the resolution to resize input image")
            .longOpt("resize")
            .numberOfArgs(2)
            .valueSeparator(',')
            .build();
    private final Option option_region = Option.builder()
            .required(false)
            .desc("Set the region to capture stream input from")
            .longOpt("region")
            .numberOfArgs(4)
            .valueSeparator(',')
            .build();
    private final Option option_autocount = Option.builder()
            .required(false)
            .desc("Calculate a palette automatically for 1 in every N frames")
            .longOpt("autocount")
            .hasArg()
            .build();
    private final Option option_lowResKMeans = Option.builder()
            .required(false)
            .desc("Shrink the image down for KMeans")
            .longOpt("lowreskmeans")
            .build();
    private final CommandLineParser parser = new DefaultParser();
    private final Options options = new Options();
    private CommandLine commandLine = null;
    private boolean savePostImage = false;
    private double secondsPerFrame = 0.2;
    private boolean uncapResolution = false;
    private boolean wipeFrames = false;
    private boolean autoSingle = false;
    private boolean silent = false;
    private boolean stream = false;
    private int frameCount = 0;
    private int port = 8080;
    private boolean resize = false;
    private int width = 100;
    private int height = 100;
    public static final int kMeansWidth = 100;
    public static final int kMeansHeight = 60;
    private boolean lowResKMeans = false;
    private int autoCount = Integer.MAX_VALUE;
    private Rectangle region = null;
    private Palette singlePalette = null;
    private IDither dither = new DitherNone();
    private IM_FILETYPE filetype = IM_FILETYPE.BIMG;
    private String postImagePath = "";

    public ImageMaker(String[] args) {
        options.addOption(option_postImageFile);
        options.addOption(option_doDither);
        options.addOption(option_doOrderedDither);
        options.addOption(option_highDensity);
        options.addOption(option_customPalette);
        options.addOption(option_autoPalette);
        options.addOption(option_secondsPerFrame);
        options.addOption(option_uncapResolution);
        options.addOption(option_bbf);
        options.addOption(option_nfp);
        options.addOption(option_wipeFrames);
        options.addOption(option_autoSingle);
        options.addOption(option_silent);
        options.addOption(option_stream);
        options.addOption(option_streamPort);
        options.addOption(option_resize);
        options.addOption(option_region);
        options.addOption(option_autocount);
        options.addOption(option_lowResKMeans);

        try {
            parse(args);
        } catch (ParseException e) {
            showHelp();
            System.exit(0);
        }
    }

    private void showHelp() {
        String header = "Convert an Image into an bimg file.\n\n";
        String footer = """
                """;

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("BIMG <input> <output>",
                header, options, footer, true);
    }

    // default CC palette
    public static void main(String[] args) {
        ImageMaker maker = new ImageMaker(args);
        maker.fullProcess();
    }

    private void fullProcess() {
        String[] args = commandLine.getArgs();
        if (!stream) {
            try {
                String inputFn = args[0];
                BufferedImage[] imageArr;
                if (Objects.equals(FilenameUtils.getExtension(inputFn), "gif")) {
                    // terrible check for a gif file
                    imageArr = GifReader.openGif(new File(inputFn), wipeFrames);
                } else {
                    imageArr = new BufferedImage[]{ImageIO.read(new File(inputFn))};
                }
                IFormat format = processFrame(imageArr);
                format.save(args[1]);
            } catch (IOException e) {
                showHelp();
            }
        } else {
            WebsocketStream stream = new WebsocketStream(port, this);
            stream.run();
        }
    }

    private void parse(String[] args) throws ParseException {
        commandLine = parser.parse(options, args);
        silent = commandLine.hasOption(option_silent);

        if (!silent) System.out.println("BIMG Image Generator version " + ImageMakerGUI.VERSION);
        if (!silent) System.out.println("Pass any arguments in to trigger the CLI version");
        if (!silent) System.out.println("Run without any arguments to run the GUI");

        stream = commandLine.hasOption(option_stream);
        lowResKMeans = commandLine.hasOption(option_lowResKMeans);
        if (commandLine.hasOption(option_streamPort)) {
            try {
                port = Integer.decode(commandLine.getOptionValue(option_streamPort));
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number!");
            }
        }
        if (commandLine.hasOption(option_autocount)) {
            autoSingle = true;
            try {
                autoCount = Integer.decode(commandLine.getOptionValue(option_autocount));
            } catch (NumberFormatException e) {
                System.out.println("Invalid autocount frame count!");
            }
        }
        if (commandLine.hasOption(option_region)) {
            try {
                String[] options = commandLine.getOptionValues(option_region);
                int x0 = Integer.decode(options[0]);
                int y0 = Integer.decode(options[1]);
                int w = Integer.decode(options[2]);
                int h = Integer.decode(options[3]);
                region = new Rectangle(x0, y0, w, h);
            } catch (NumberFormatException e) {
                System.out.println("Invalid region! Example Usage:");
                System.out.println("--region 0 0 1920 1080");
            }
        }
        uncapResolution = (commandLine.hasOption(option_uncapResolution));
        if (commandLine.hasOption(option_resize)) {
            resize = true;
            uncapResolution = true;
            try {
                String[] options = commandLine.getOptionValues(option_resize);
                width = Integer.decode(options[0]);
                height = Integer.decode(options[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid Width/Height! Example usage:");
                System.out.println("--resize 256 128");
            }
        }
        if (commandLine.hasOption(option_postImageFile)) {
            savePostImage = true;
            postImagePath = commandLine.getOptionValue(option_postImageFile);
        }
        if (commandLine.hasOption(option_doDither))
            dither = new DitherFloydSteinberg();
        else if (commandLine.hasOption(option_doOrderedDither)) {
            try {
                int thresholdMapSize = Integer.decode(commandLine.getOptionValues(option_doOrderedDither)[0]);
                double colorSpread = Double.parseDouble(commandLine.getOptionValues(option_doOrderedDither)[1]);
                dither = new DitherOrdered(thresholdMapSize, colorSpread);
            } catch (NumberFormatException e) {
                System.out.println("Please provide the threshold map size and color spread. Example usage: ");
                System.out.println("-ordered 4 50");
                return;
            }
        }

        if (commandLine.hasOption(option_highDensity)) {
            mode = IM_MODE.HD;
        }


        if (commandLine.hasOption(option_autoSingle))
            autoSingle = true;
        if (commandLine.hasOption(option_autoPalette) || autoSingle) {
            mode = switch (mode) {
                case HD -> IM_MODE.HD_AUTO;
                case LD -> IM_MODE.LD_AUTO;
                case HD_AUTO, LD_AUTO -> null; // should be impossible to reach
            };
        } else if (commandLine.hasOption(option_customPalette)) {
            try {
                String[] paletteColors = commandLine.getOptionValues(option_customPalette);
                Color[] colors = new Color[paletteColors.length];
                for (int index = 0; index < paletteColors.length; index++) {
                    colors[index] = new Color(Integer.decode(paletteColors[index]));
                }
                palette = new Palette(colors);
            } catch (NumberFormatException e) {
                System.out.println("Incorrectly formatted palette. Example usage: ");
                System.out.println("-p=1,2,0xFF0000");
                return;
            }
        }
        if (commandLine.hasOption(option_secondsPerFrame))
            secondsPerFrame = Double.parseDouble(commandLine.getOptionValue(option_secondsPerFrame));

        if (commandLine.hasOption(option_bbf))
            filetype = IM_FILETYPE.BBF;
        else if (commandLine.hasOption(option_nfp))
            filetype = IM_FILETYPE.NFP;

        if (commandLine.hasOption(option_wipeFrames))
            wipeFrames = true;

    }

    public void setSize(int width, int height) {
        resize = true;
        this.width = width;
        this.height = height;
        uncapResolution = true;
    }

    public static BufferedImage resize(BufferedImage im, int width, int height) {
        double wScale = width / (double) im.getWidth();
        double hScale = height / (double) im.getHeight();
        return Mode.scaleImage(im, wScale, hScale);
    }

    public IFormat processFrame(BufferedImage[] imageArr) throws IOException {
        IMode[] im = new IMode[imageArr.length];
        for (int i = 0; i < imageArr.length; i++) {
            BufferedImage inputImage = imageArr[i];
            if (!uncapResolution) {
                final int MAX_WIDTH_HIGH = 102;
                final int MAX_HEIGHT_HIGH = 57;
                final int MAX_WIDTH_LOW = 51;
                final int MAX_HEIGHT_LOW = 19;
                int maxWidth = ((mode == IM_MODE.HD || mode == IM_MODE.HD_AUTO) ? MAX_WIDTH_HIGH : MAX_WIDTH_LOW);
                if (inputImage.getWidth() > maxWidth) {
                    double scale = ((mode == IM_MODE.HD || mode == IM_MODE.HD_AUTO) ? MAX_WIDTH_HIGH
                            : MAX_WIDTH_LOW) / (double) inputImage.getWidth();
                    if (!silent) System.out.println("Image is too wide, resizing! Was " + inputImage.getWidth() + " by " + inputImage.getHeight());
                    inputImage = Mode.scaleImage(inputImage, scale, scale);
                }
                if (inputImage.getHeight() > ((mode == IM_MODE.HD || mode == IM_MODE.HD_AUTO) ? MAX_HEIGHT_HIGH
                        : MAX_HEIGHT_LOW)) {
                    double scale = ((mode == IM_MODE.HD || mode == IM_MODE.HD_AUTO) ? MAX_HEIGHT_HIGH
                            : MAX_HEIGHT_LOW) / (double) inputImage.getHeight();
                    if (!silent) System.out.println("Image is too tall, resizing! Was " + inputImage.getWidth() + " by " + inputImage.getHeight());
                    inputImage = Mode.scaleImage(inputImage, scale, scale);
                }
                if (!silent) System.out.println("Final resolution is " + inputImage.getWidth() + " by "
                        + inputImage.getHeight());
            } else if (resize) {
                inputImage = resize(inputImage, width, height);
            }
            long startTime = System.nanoTime();
            if (!autoSingle || (frameCount == 0 || frameCount % autoCount == 0)) {
                im[i] = switch (mode) {
                    case HD -> new ModeHighDensity(inputImage, palette, dither);
                    case LD -> new ModeLowDensity(inputImage, palette, dither);
                    case HD_AUTO -> new ModeHighDensity(inputImage, dither, lowResKMeans);
                    case LD_AUTO -> new ModeLowDensity(inputImage, dither, lowResKMeans);
                };
            } else {
                im[i] = switch (mode) {
                    case HD, HD_AUTO -> new ModeHighDensity(inputImage, singlePalette, dither);
                    case LD, LD_AUTO -> new ModeLowDensity(inputImage, singlePalette, dither);
                };
            }
            if (autoSingle && (frameCount == 0 || frameCount % autoCount == 0)) {
                singlePalette = im[0].getPalette();
            }
            long endTime = System.nanoTime();
            if (!silent) System.out.println("Quantized image in " + (endTime - startTime) / 1000000.0f + "ms.");
            if (savePostImage) {
                startTime = System.nanoTime();
                if (imageArr.length > 1) {
                    ImageIO.write(im[i].getImage(), FilenameUtils.getExtension(postImagePath),
                            new File(insertBeforeFileEx(postImagePath, String.valueOf(i))));
                } else {
                    ImageIO.write(im[i].getImage(), FilenameUtils.getExtension(postImagePath),
                            new File(postImagePath));
                }
                endTime = System.nanoTime();
                if (!silent) System.out.println("Wrote post image in " + (endTime - startTime) / 1000000.0f + "ms.");
            }
            frameCount++;
        }
        long startTime = System.nanoTime();
        long endTime;
        IFormat format = null;
        switch (filetype) {
            case BIMG -> {
                BIMG bimg = new BIMG(im);
                if (imageArr.length > 1)
                    bimg.writeKeyValuePair("secondsPerFrame", secondsPerFrame);
                format = bimg;
            }
            case BBF -> {
                format = new BBF(im);
            }
            case NFP -> {
                format = new NFP(im);
            }
        }
        endTime = System.nanoTime();
        if (!silent) System.out.println("Wrote output in " + (endTime - startTime) / 1000000.0f + "ms.");
        return format;
    }

    static String insertBeforeFileEx(String filename, String insert) {
        String modFilename = FilenameUtils.removeExtension(filename) + insert;
        if (!Objects.equals(FilenameUtils.getExtension(filename), ""))
            modFilename += "." + FilenameUtils.getExtension(filename);
        return modFilename;
    }

    public Rectangle getRegion() {
        return region;
    }

    enum IM_MODE {
        HD,
        LD,
        HD_AUTO,
        LD_AUTO
    }

    enum IM_FILETYPE {
        BIMG,
        BBF,
        NFP
    }


}
