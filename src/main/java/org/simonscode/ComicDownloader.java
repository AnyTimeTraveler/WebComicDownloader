package org.simonscode;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static javax.swing.SpringLayout.*;

public class ComicDownloader {

    private static final int MAX_PROGRESS = 30;
    private static Config config;

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Webcomic Downloader");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setMinimumSize(new Dimension(462,222));


        //Create and populate the panel.
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
        JTextField firstPageField = makeRow(p, "Link to first page: ");
        JTextField secondPageField = makeRow(p, "Link to second page: ");
        JTextField firstImageField = makeRow(p, "Link to first image location: ");
        JTextField targetLocationField = new JTextField(10);

//        firstPageField.setText("https://nhentai.net/g/212120/1/");
//        secondPageField.setText("https://nhentai.net/g/212120/2/");
//        firstImageField.setText("https://i.nhentai.net/galleries/1132072/1.jpg");
//        targetLocationField.setText("C:\\Users\\null\\Desktop\\hentai.cbz");

        {
            JPanel targetFilePanel = new JPanel();
            JLabel targetLocationLabel = new JLabel("Target location for cbz file: ", JLabel.LEADING);
            targetLocationLabel.setLabelFor(targetLocationField);

            targetFilePanel.setLayout(new BoxLayout(targetFilePanel, BoxLayout.LINE_AXIS));
            JButton browseButton = new JButton("Browse");
            browseButton.addActionListener(event -> choseFile(frame, targetLocationField));

            p.add(targetLocationLabel);
            targetFilePanel.add(targetLocationField);
            targetFilePanel.add(browseButton);
            p.add(targetFilePanel);

            targetLocationField.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (targetLocationField.getText().isEmpty()) {
                        choseFile(frame, targetLocationField);
                    }
                }
            });
        }

        JProgressBar subTaskBar = new JProgressBar(0,10);
        p.add(subTaskBar);

        JPanel progressBarPanel = new JPanel();
        progressBarPanel.setLayout(new BoxLayout(progressBarPanel, BoxLayout.LINE_AXIS));
        JProgressBar progressBar = new JProgressBar();
        progressBarPanel.add(progressBar);

        JButton startButton = new JButton("Start");
        startButton.addActionListener(event ->
            new Thread(() -> scrape(firstPageField.getText(),
                secondPageField.getText(),
                firstImageField.getText(),
                targetLocationField.getText(),
                progressBar,
                subTaskBar)).start());

        progressBarPanel.add(progressBar);
        progressBarPanel.add(startButton);
        p.add(progressBarPanel);

        //Set up the content pane.
        p.setOpaque(true);  //content panes must be opaque
        frame.setContentPane(p);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    private static void choseFile(JFrame frame, JTextField targetLocation) {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setFileFilter(new FileNameExtensionFilter("Webcomic","cbz"));
        if (jFileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            targetLocation.setText(jFileChooser.getSelectedFile().getPath());
        }
    }

    private static JTextField makeRow(JPanel p, String label) {
        JLabel l = new JLabel(label, JLabel.LEADING);
        p.add(l);
        JTextField textField = new JTextField(20);
        l.setLabelFor(textField);
        p.add(textField);
        return textField;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ComicDownloader::createAndShowGUI);
    }

    private static void scrape(
        String firstPage,
        String secondPage,
        String firstImageLocation,
        String storageLocation,
        JProgressBar progressBar,
        JProgressBar subTaskBar) {

        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);

        URL firstUrl;
        URL secondURL;
        try {
            firstUrl = new URL(firstPage);
            secondURL = new URL(secondPage);
        } catch (MalformedURLException e) {
            setError(progressBar, "Could not understand URL's, aborting.");
            return;
        }


        // check if download folder exists
        ZipOutputStream out;
        try {
            if (cbzFile.exists() && !cbzFile.delete() || !cbzFile.createNewFile() || !cbzFile.canWrite()) {
                setError(progressBar, String.format("Could not create %s, file already exists, aborting.%n", storageLocation));
                return;
            }
            out = new ZipOutputStream(new FileOutputStream(cbzFile));
        } catch (IOException e) {
            setError(progressBar, "Error during file creation, aborting.");
            e.printStackTrace();
            return;
        }

        // get css selectors for page elements
        String cssSelectorNextPageButton;
        String cssSelectorMainImage = null;
        try {
            Document doc = Jsoup.parse(firstUrl, 5_000);

            Elements nextPageHref = doc.body().getElementsByAttributeValue("href", secondPage);

            if (nextPageHref.isEmpty()) {
                nextPageHref = doc.body().getElementsByAttributeValue("href", secondURL.getPath());
                if (nextPageHref.isEmpty()) {
                    setError(progressBar, "Could not find link to second page on first page, aborting.");
                    return;
                }
            }

            cssSelectorNextPageButton = nextPageHref.first().cssSelector();
            setMessage(progressBar, "Found next-page-button!");

            Elements images = doc.body().getElementsByTag("img");
            for (Element image : images) {

                URL imageURL;
                try {
                    imageURL = getUrlFromLink(firstUrl, image.attr("src"));
                } catch (MalformedURLException e) {
                    setError(progressBar, "Error while trying to find main image on first page. URL malformed, aborting.");
                    return;
                }
                if (imageURL.equals(new URL(firstImageLocation))) {
                    cssSelectorMainImage = image.cssSelector();
                    setMessage(progressBar, "Found main image, proceeding to download...");
                    break;
                }
            }

            if (cssSelectorMainImage == null) {
                setError(progressBar, "Could not find main image on first page, aborting.");
                return;
            }
        } catch (IOException e) {
            setError(progressBar, "Unknown error while finding link to second page, aborting.");
            e.printStackTrace();
            return;
        }

        // get the image and the next page button for each page and repeat
        URL nextPageURL = firstUrl;
        int counter = 1;
        while (true) {
            try {
                setMessage(progressBar, String.format("Downloading from %s ", nextPageURL));
                Document doc = Jsoup.parse(nextPageURL, 5_000);

                // get main image
                Elements mainImage = doc.select(cssSelectorMainImage);

                if (mainImage.isEmpty()) {
                    setError(progressBar, "No image found, aborting!");
                    return;
                }

                // check if target file exists and download if it doesn't
                String imageLink = mainImage.first().attr("src");
                ZipEntry dl = new ZipEntry(createPath(counter++, getFiletypeFromURL(imageLink)));
                URL imageURL;
                try {
                    imageURL = getUrlFromLink(firstUrl, imageLink);
                } catch (MalformedURLException e) {
                    setError(progressBar, String.format("Error while trying download image from page \"%s\". URL malformed, aborting.%n", imageLink));
                    return;
                }

                try {
                    byte[] imageData = downloadImage(imageURL, subTaskBar);

                    out.putNextEntry(dl);
                    out.write(imageData, 0, imageData.length);
                    out.closeEntry();
                } catch (IOException e) {
                    setError(progressBar, String.format("Error downloading image from \"%s\"%n", imageURL));
                    e.printStackTrace();
                }


                // get next link from button
                Elements nextButton = doc.select(cssSelectorNextPageButton);
                if (nextButton.isEmpty() || nextButton.attr("href").isEmpty()) {
                    setMessage(progressBar, "No next-button found, that must mean I'm done!");
                    break;
                }
                try {
                    nextPageURL = getUrlFromLink(firstUrl, nextButton.first().attr("href"));
                } catch (MalformedURLException e) {
                    String error = String.format("Error while trying to get next page link from page \"%s\". URL malformed, aborting.%n", nextButton.first().attr("href"));
                    setError(progressBar, error);
                    return;
                }

            } catch (IOException e) {
                setError(progressBar, "Unknown error while downloading, aborting!");
                e.printStackTrace();
                return;
            }
        }

        try {
            out.close();
        } catch (IOException e) {
            setError(progressBar, "Error finishing cbz file!");
            e.printStackTrace();
            return;
        }

        setMessage(progressBar, "Done :) Thank you for using comic-downloader by Simon Struck!");
        progressBar.setIndeterminate(false);
        progressBar.setValue(100);
    }


    private static void setMessage(JProgressBar progressBar, String message) {
        System.out.println(message);
        progressBar.setString(message);
    }

    private static void setError(JProgressBar progressBar, String errorMessage) {
        System.err.println(errorMessage);
        progressBar.setIndeterminate(false);
        progressBar.setString("ERROR: " + errorMessage);
    }

    /**
     * This method mainly exists to fix poorly formatted and relative links. (looking at you, xkcd)
     *
     * @param baseURL Just a URL from the same website
     * @param link    the raw link text that was extracted from html
     * @return a working URL or an exception
     * @throws MalformedURLException if no working URL can be built
     */
    private static URL getUrlFromLink(URL baseURL, String link) throws MalformedURLException {

        String strippedLink = link;
        if (!strippedLink.isEmpty()) {
            while (strippedLink.charAt(0) == '/') {
                strippedLink = strippedLink.substring(1);
            }
        }

        String[] parts = strippedLink.split("/+");

        int protocolIndex = -1;
        int baseIndex = -1;

        for (int i = 0; i < parts.length; i++) {
            if (protocolIndex == -1 && parts[i].startsWith("http")) {
                protocolIndex = i;
            } else if (i < 2 && parts[i].contains(".") && i != parts.length - 1) {
                baseIndex = i;
                break;
            }
        }
        if (baseIndex != -1) {
            if (protocolIndex != -1) {
                try {
                    return new URL(parts[protocolIndex] + "//" + parts[baseIndex] + '/' + String.join("/", Arrays.copyOfRange(parts, baseIndex + 1, parts.length)));
                } catch (MalformedURLException ignored) {
                }
            }
            if (protocolIndex == -1) {
                try {
                    return new URL(baseURL.getProtocol() + "://" + parts[baseIndex] + '/' + String.join("/", Arrays.copyOfRange(parts, baseIndex + 1, parts.length)));
                } catch (MalformedURLException ignored) {
                }
            }
        }
        return new URL(baseURL.getProtocol() + "://" + baseURL.getHost() + '/' + strippedLink);
    }

    private static String getFiletypeFromURL(String url) {
        return url.substring(url.lastIndexOf('.') + 1);
    }

    private static String createPath(int index, String fileType) {
        StringBuilder sb = new StringBuilder();

        String number = String.valueOf(index);
        for (int i = 0; i < (5 - number.length()); i++) {
            sb.append('0');
        }
        sb.append(number);
        sb.append('.');
        sb.append(fileType);

        return sb.toString();
    }

    /**
     * Downloads a file from a url into a buffer.
     *
     * @param url URL to download from
     * @param subTaskBar
     * @return buffer that contains the downloaded data
     */
    private static byte[] downloadImage(URL url, JProgressBar subTaskBar) throws IOException {
        byte[] buffer;

        URLConnection con = url.openConnection();
        con.connect();

        int length = con.getContentLength();
        buffer = new byte[length];

        // create progress meter
        System.out.print('[');
        int progress = 0;
        long progressAmount = length / 10;
        subTaskBar.setValue(0);
        subTaskBar.setMinimum(0);
        subTaskBar.setMaximum(length);

        InputStream is = con.getInputStream();
        for (int i = 0; i < length; ) {
            int read = is.read(buffer, i, buffer.length - i);
            if (read == -1) {
                throw new IOException("Stream ended, even though not all bytes have been read.");
            }
            i += read;
            for (; progress < MAX_PROGRESS && progress * progressAmount < i; progress++) {
                System.out.print('#');
                subTaskBar.setValue(i);
            }
        }
        for (; progress < MAX_PROGRESS; progress++) {
            System.out.print('#');
        }
        subTaskBar.setValue(length);
        System.out.println("] ");
        is.close();
        return buffer;
    }

}
