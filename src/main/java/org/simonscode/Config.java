package org.simonscode;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class Config {

    // process parameters
    public final String firstPage;
    public final String secondPage;
    public final String firstImageLocation;
    public final String storageLocation;


    public final URL firstUrl;
    public final URL secondURL;
    public final File cbzFile;

    private Config(String firstPage, String secondPage, String firstImageLocation, String storageLocation, URL firstUrl, URL secondURL, File cbzFile) {
        this.firstPage = firstPage;
        this.secondPage = secondPage;
        this.firstImageLocation = firstImageLocation;
        this.storageLocation = storageLocation;
        this.firstUrl = firstUrl;
        this.secondURL = secondURL;
        this.cbzFile = cbzFile;
    }

    public static Config parse(String[] args) throws MalformedURLException {
        if (args.length != 5) {
            System.out.println("Usage: cdl [link to first page] [link to second page] [link to first image location] [target location for cbz file]");
            return null;
        }

        conf.firstUrl = new URL(conf.firstPage);
        conf.secondURL = new URL(conf.secondPage);

        Config conf = new Config(firstPage, secondPage, firstImageLocation, storageLocation, firstUrl, secondURL, cbzFile);

    }
}
