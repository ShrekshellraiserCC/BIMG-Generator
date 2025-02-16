package com.shrekshellraiser;

import com.shrekshellraiser.dithers.*;
import com.shrekshellraiser.formats.BBF;
import com.shrekshellraiser.formats.BIMG;
import com.shrekshellraiser.formats.NFP;
import com.shrekshellraiser.image.Image;
import com.shrekshellraiser.modes.IMode;
import com.shrekshellraiser.modes.Mode;
import com.shrekshellraiser.utils.Utils;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static com.shrekshellraiser.palettes.DefaultPalette.defaultPalette;

public class ImageMakerGUI implements ActionListener, ItemListener {
    static final String VERSION = "9.5";
    private final JFileChooser fc;
    JFrame frame;
    private Image image = null;
    private JComboBox<String> resolutionMode;
    private JSpinner blitChar;
    private JComboBox<String> paletteMode;
    private JComboBox<String> ditherMode;
    private JCheckBox wipeFrames;
    private JLabel outputImage;
    private JTextField monitorHorizontal;
    private JTextField monitorVertical;
    private JComboBox<String> fitUnit;
    private Checkbox fitToMonitor;
    private JSpinner visualScale;
    private JLabel thresholdMapLabel;
    private JComboBox<Integer> thresholdMap;
    private JLabel colorSpreadLabel;
    private JSpinner colorSpread;
    private JComboBox<String> fileType;
    private IMode[] im = null;
    private JLabel monitorHorizontalLabel;
    private JLabel monitorVerticalLabel;

    public ImageMakerGUI() {
        fc = new JFileChooser();
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            ImageMaker.main(args);
            return;
        }
        System.out.println("BIMG Image Generator version " + ImageMakerGUI.VERSION);
        System.out.println("Pass any arguments in to trigger the CLI version");
        System.out.println("Run without any arguments to run the GUI");
        ImageMakerGUI main = new ImageMakerGUI();
        main.startGUI();
    }

    public void startGUI() {
        frame = new JFrame("BIMG Converter Version " + VERSION);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        SwingUtilities.updateComponentTreeUI(frame);

        JPanel topPanel = new JPanel();

        Button openButton = new Button("Open"); // top bar
        openButton.addActionListener(this);
        topPanel.add(openButton);

        resolutionMode = new JComboBox<>(Utils.getNames(resolutionModes.class));
        topPanel.add(resolutionMode);
        resolutionMode.addActionListener(this);

        blitChar = new JSpinner(new SpinnerNumberModel(159, 1, 255, 1));
        topPanel.add(blitChar);
        blitChar.setVisible(false);

        paletteMode = new JComboBox<>(Utils.getNames(paletteModes.class));
        topPanel.add(paletteMode);

        ditherMode = new JComboBox<>(Utils.getNames(ditherModes.class));
        ditherMode.addActionListener(this);
        topPanel.add(ditherMode);
        thresholdMapLabel = new JLabel("Threshold Map");
        topPanel.add(thresholdMapLabel);
        thresholdMap = new JComboBox<>(new Integer[]{2, 4, 8});
        topPanel.add(thresholdMap);
        colorSpreadLabel = new JLabel("Color Spread");
        topPanel.add(colorSpreadLabel);
        colorSpread = new JSpinner(new SpinnerNumberModel(50, 5, 200.0, 5));
        topPanel.add(colorSpread);

        thresholdMapLabel.setVisible(false);
        thresholdMap.setVisible(false);
        colorSpreadLabel.setVisible(false);
        colorSpread.setVisible(false);

        wipeFrames = new JCheckBox("Wipe Frames");
        topPanel.add(wipeFrames);

        Button refreshButton = new Button("Refresh");
        refreshButton.addActionListener(this);
        topPanel.add(refreshButton);

        frame.getContentPane().add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(); // center preview
        outputImage = new JLabel();
        centerPanel.add(outputImage);

        frame.getContentPane().add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(); // bottom bar
        monitorHorizontalLabel = new JLabel("X");
        bottomPanel.add(monitorHorizontalLabel);
        monitorHorizontal = new JTextField("51", 3);
        bottomPanel.add(monitorHorizontal);
        monitorVerticalLabel = new JLabel("Y");
        bottomPanel.add(monitorVerticalLabel);
        monitorVertical = new JTextField("19", 3);
        bottomPanel.add(monitorVertical);

        fitUnit = new JComboBox<>(new String[]{"Chars", "Monitors"});
        bottomPanel.add(fitUnit);

        fitToMonitor = new Checkbox("Fit to Size", true);
        fitToMonitor.addItemListener(this);
        bottomPanel.add(fitToMonitor);

        bottomPanel.add(new JLabel("Preview scale"));
        visualScale = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        bottomPanel.add(visualScale);

        fileType = new JComboBox<>(new String[]{"BIMG", "BBF", "NFP"});
        bottomPanel.add(fileType);
        fileType.addActionListener(this);
        Button saveButton = new Button("Save");
        saveButton.addActionListener(this);
        bottomPanel.add(saveButton);

        Button savePreviewButton = new Button("Save Preview");
        savePreviewButton.addActionListener(this);
        bottomPanel.add(savePreviewButton);

        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        frame.setMinimumSize(new Dimension(800, 500));
        frame.setVisible(true);


    }

    public void actionPerformed(ActionEvent e) {
        String event = e.getActionCommand();
        System.out.println("Action performed " + event);
        if (Objects.equals(event, "Open")) {
            int returnVal = fc.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                // user selected a file
                File file = fc.getSelectedFile();
                try {
                    image = new Image(file, wipeFrames.isSelected());
                    refresh();
                } catch (IOException ex) {
                    System.out.println("Can't open file " + file.getName() + ": " + ex);
                }
            }
        } else if (Objects.equals(event, "Refresh")) {
            refresh();
        } else if (Objects.equals(event, "Save")) {
            int returnVal = fc.showSaveDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                // user selected a file
                File file = fc.getSelectedFile();
                String filetype = (String) fileType.getSelectedItem();
                float startTime = System.nanoTime();
                float endTime;
                try {
                    if (im == null)
                        im = getImages();
                    switch (filetype) {
                        case "BIMG" -> {
                            BIMG bimg = new BIMG(im);
                            if (im.length > 1)
                                bimg.writeKeyValuePair("secondsPerFrame", 0.1f);
                            bimg.save(file);
                            endTime = System.nanoTime();
                            System.out.println("Wrote bimg in " + (endTime - startTime) / 1000000.0f + "ms.");
                        }
                        case "BBF" -> {
                            BBF bbf = new BBF(im);
                            bbf.save(file);
                            endTime = System.nanoTime();
                            System.out.println("Wrote bbf in " + (endTime - startTime) / 1000000.0f + "ms.");
                        }
                        case "NFP" -> {
                            NFP nfp = new NFP(im);
                            nfp.save(file);
                            endTime = System.nanoTime();
                            System.out.println("Wrote nfp in " + (endTime - startTime) / 1000000.0f + "ms.");
                        }
                        default -> throw new IllegalStateException("No mode selected");
                    }
                } catch (IOException er) {
                    System.out.println("Unable to save file " + er);
                }
            }
        } else if (event.equals("Save Preview")) {
            // user requested to save the preview com.shrekshellraiser.image
            int returnVal = fc.showSaveDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    if (im == null)
                        return;
                    File outputFile = fc.getSelectedFile();
                    ImageIO.write(im[0].getImage(), FilenameUtils.getExtension(outputFile.getName()), outputFile);
                } catch (IOException ex) {
                    System.out.println("Unable to save file " + ex);
                }
            }
        } else if (event.equals("comboBoxChanged")) {
            // The selected dithering mode was changed
            boolean ditherModeIsOrdered = ditherMode.getSelectedItem().equals("Ordered");
            thresholdMap.setVisible(ditherModeIsOrdered);
            thresholdMapLabel.setVisible(ditherModeIsOrdered);
            boolean ditherModeIsNoise = ditherMode.getSelectedItem().equals("BlueNoise");
            colorSpread.setVisible(ditherModeIsOrdered || ditherModeIsNoise);
            colorSpreadLabel.setVisible(ditherModeIsOrdered || ditherModeIsNoise);

            blitChar.setVisible(resolutionMode.getSelectedItem().equals("LD"));

            boolean NFPSelected = fileType.getSelectedItem().equals("NFP");
            resolutionMode.setEnabled(!NFPSelected);
            paletteMode.setEnabled(!NFPSelected);

            if (NFPSelected) {
                resolutionMode.setSelectedIndex(0); // set resolution mode to LD
                resolutionMode.getModel().setSelectedItem("LD"); // set resolution mode to LD
                paletteMode.setSelectedIndex(2); // set palette to default
                paletteMode.getModel().setSelectedItem("Default"); // set palette to default
            }
        }
    }

    private void refresh() {
        im = getImages();
        int scale = (int) visualScale.getValue();
        outputImage.setIcon(new ImageIcon(Mode.scaleImage(im[0].getImage(), scale, scale)));
        outputImage.repaint();
        frame.pack();
    }

    private IMode[] getImages() {
        return getImages(image);
    }

    private IMode[] getImages(Image image1) {
        if (fitToMonitor.getState()) {
            int xRes = Integer.parseInt(monitorHorizontal.getText());
            int yRes = Integer.parseInt(monitorVertical.getText());
            if (fitUnit.getSelectedItem() == "Monitors") {
                xRes = 15 + (21 * (xRes - 1));
                yRes = 10 + (14 * (yRes - 1));
            }
            if (resolutionMode.getSelectedItem() == "HD") {
                xRes *= 2;
                yRes *= 3;
            }
            image1 = new Image(image.scale(xRes, yRes));
        }
        // perform checks for high resolution images
        String palette = (String) paletteMode.getSelectedItem();
        Mode.IM_MODE mode;
        boolean autoMode = Objects.equals(palette, "Auto") || Objects.equals(palette, "AutoSingle");
        int width = image1.imageArr[0].getWidth();
        int height = image1.imageArr[0].getHeight();
        int pixelCount = height * width;
        if ((pixelCount > 400000 && autoMode) || pixelCount > 1500000) {
            // give warning about com.shrekshellraiser.image size
            int result = JOptionPane.showConfirmDialog(null,
                    "The com.shrekshellraiser.image you are trying to process is " + pixelCount
                            + " pixels (" + width + "x" + height
                            + "). This may take a long time to process, proceed?", "Image is large",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.YES_OPTION)
                return null;
        }
        if (resolutionMode.getSelectedItem() == "HD") {
            mode = autoMode ? Mode.IM_MODE.HD_AUTO : Mode.IM_MODE.HD;
        } else {
            mode = autoMode ? Mode.IM_MODE.LD_AUTO : Mode.IM_MODE.LD;
        }
        IDither dither = switch ((String) Objects.requireNonNull(ditherMode.getSelectedItem())) {
            case "FloydSteinberg" -> new DitherFloydSteinberg();
            case "Ordered" -> new DitherOrdered((int) thresholdMap.getSelectedItem(), (double) colorSpread.getValue());
            case "None" -> new DitherNone();
            case "BlueNoise" -> new DitherBlueNoise((double) colorSpread.getValue());
            default -> throw new IllegalStateException("Unexpected value: " + ditherMode.getSelectedItem());
        };
        return image1.convert(mode, defaultPalette, dither,
                Objects.equals(paletteMode.getSelectedItem(), "AutoSingle"), (Integer) blitChar.getValue(), false);
    }

    @Override
    public void itemStateChanged(ItemEvent itemEvent) {
        System.out.println("Checkbox changed" + itemEvent);
        // the fitToMonitor checkbox was changed
        monitorHorizontal.setVisible(fitToMonitor.getState());
        monitorHorizontalLabel.setVisible(fitToMonitor.getState());
        monitorVertical.setVisible(fitToMonitor.getState());
        monitorVerticalLabel.setVisible(fitToMonitor.getState());
        fitUnit.setVisible(fitToMonitor.getState());
    }

    enum resolutionModes {
        HD,
        LD
    }

    enum paletteModes {
        Auto,
        AutoSingle,
        Default
    }

    enum ditherModes {
        FloydSteinberg,
        Ordered,
        None,
        BlueNoise,
    }

    enum outputModes {
        BIMG,
        BBF,
        NFP
    }
}

