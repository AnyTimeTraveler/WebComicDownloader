package org.simonscode;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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

public class ComicDownloader {

    private static final int MAX_PROGRESS = 30;

    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("Usage: cdl [link to first page] [link to second page] [link to most recent page] [link to first image location] [target location for cbz file]");
            return;
        }

        // process parameters
        String firstPage = args[0];
        String secondPage = args[1];
        String lastPage = args[2];
        String firstImageLocation = args[3];
        String storageLocation = args[4];


        URL firstUrl;
        URL secondURL;
        URL lastUrl;
        try {
            firstUrl = new URL(firstPage);
            secondURL = new URL(secondPage);
            lastUrl = new URL(lastPage);
        } catch (MalformedURLException e) {
            System.err.println("Could not understand URL's, aborting.");
            return;
        }
        File cbzFile = new File(storageLocation);

        // check if download folder exists
        ZipOutputStream out;
        try {
            if (cbzFile.exists() && !cbzFile.delete() || !cbzFile.createNewFile() || !cbzFile.canWrite()) {
                System.err.printf("Could not create %s, file already exists, aborting.%n", storageLocation);
                return;
            }
            out = new ZipOutputStream(new FileOutputStream(cbzFile));
        } catch (IOException e) {
            System.err.println("Error during file creation, aborting.");
            System.err.println();
            System.err.println();
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
                    System.err.println("Could not find link to second page on first page, aborting.");
                    return;
                }
            }

            cssSelectorNextPageButton = nextPageHref.first().cssSelector();
            System.out.println("Found next-page-button!");

            Elements images = doc.body().getElementsByTag("img");
            for (Element image : images) {

                URL imageURL;
                try {
                    imageURL = getUrlFromLink(firstUrl, image.attr("src"));
                } catch (MalformedURLException e) {
                    System.err.println("Error while trying to find main image on first page. URL malformed, aborting.");
                    return;
                }
                if (imageURL.equals(new URL(firstImageLocation))) {
                    cssSelectorMainImage = image.cssSelector();
                    System.out.println("Found main image, proceeding to download...");
                    break;
                }
            }

            if (cssSelectorMainImage == null) {
                System.err.println("Could not find main image on first page, aborting.");
                return;
            }
        } catch (IOException e) {
            System.err.println("Unknown error while finding link to second page, aborting.");
            e.printStackTrace();
            return;
        }

        // get the image and the next page button for each page and repeat
        URL nextPageURL = firstUrl;
        int counter = 1;
        while (true) {
            try {
                System.out.printf("Downloading from %s ", nextPageURL);
                Document doc = Jsoup.parse(nextPageURL, 5_000);

                // get main image
                Elements mainImage = doc.select(cssSelectorMainImage);

                if (mainImage.isEmpty()) {
                    System.err.println("No image found, aborting!");
                    return;
                }

                // check if target file exists and download if it doesn't
                String imageLink = mainImage.first().attr("src");
                ZipEntry dl = new ZipEntry(createPath(counter++, getFiletypeFromURL(imageLink)));
                URL imageURL;
                try {
                    imageURL = getUrlFromLink(firstUrl, imageLink);
                } catch (MalformedURLException e) {
                    System.err.printf("Error while trying download image from page \"%s\". URL malformed, aborting.%n", imageLink);
                    return;
                }

                try {
                    byte[] imageData = downloadImage(imageURL);

                    out.putNextEntry(dl);
                    out.write(imageData, 0, imageData.length);
                    out.closeEntry();
                } catch (IOException e) {
                    System.err.printf("Error downloading image from \"%s\"%n", imageURL);
                    e.printStackTrace();
                }
                System.out.println("Done!");


                // get next link from button
                Elements nextButton = doc.select(cssSelectorNextPageButton);
                if (nextButton.isEmpty() || nextPageURL.equals(lastUrl)) {
                    System.out.printf("%nNo next-button found, that must mean I'm done!%n%n");
                    break;
                }
                try {
                    nextPageURL = getUrlFromLink(firstUrl, nextButton.first().attr("href"));
                } catch (MalformedURLException e) {
                    System.err.printf("Error while trying to get next page link from page \"%s\". URL malformed, aborting.%n", nextButton.first().attr("href"));
                    return;
                }

            } catch (IOException e) {
                System.err.println("Unknown error while downloading, aborting!");
                e.printStackTrace();
                return;
            }
        }

        try {
            out.close();
        } catch (IOException e) {
            System.err.println("Error finishing cbz file!");
            e.printStackTrace();
            return;
        }

        System.out.println("All finished up here :)");
        System.out.println();
        System.out.println("Thank you for using comic-downloader by Simon Struck!");
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
        while (strippedLink.charAt(0) == '/') {
            strippedLink = strippedLink.substring(1);
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
     * @return buffer that contains the downloaded data
     */
    private static byte[] downloadImage(URL url) throws IOException {
        byte[] buffer;

        URLConnection con = url.openConnection();
        con.connect();

        int length = con.getContentLength();
        buffer = new byte[length];

        // create progress meter
        System.out.print('[');
        int progress = 0;
        long progressAmount = length / 10;

        InputStream is = con.getInputStream();
        for (int i = 0; i < length; ) {
            int read = is.read(buffer, i, buffer.length - i);
            if (read == -1) {
                throw new IOException("Stream ended, even though not all bytes have been read.");
            }
            i += read;
            for (; progress < MAX_PROGRESS && progress * progressAmount < i; progress++) {
                System.out.print('#');
            }
        }
        for (; progress < MAX_PROGRESS; progress++) {
            System.out.print('#');
        }
        System.out.print("] ");
        is.close();
        return buffer;
    }

}
