# Webcomic Downloader

A small java program that converts a webcomic into a .cbz file that you can read on your eReader for example.

![Downloader in action](https://raw.githubusercontent.com/AnyTimeTraveler/WebComicDownloader/master/imgs/in_action.png)


# Usage

Download the latest jar from [here](https://github.com/AnyTimeTraveler/WebComicDownloader/releases/)

Then run it with the following parameters:
1. link to first page
2. link to second page (so it can learn where the next button is)
3. link to most recent page (so it knows when it's done)
4. link to first image location (so it knows what image to grab)
5. path to save the resulting cbz file

## Here is how you get those links:

#### 1. Copy the link from the first button:

![Copy link from first button](https://raw.githubusercontent.com/AnyTimeTraveler/WebComicDownloader/master/imgs/save_first_link.png)

#### 2. Click on the 'first' button and then copy the link from the 'next' button on that page: 


![Copy link from next button](https://raw.githubusercontent.com/AnyTimeTraveler/WebComicDownloader/master/imgs/save_second_link.png)

#### 3. Copy the link from the 'last' button:

![Copy link from last button](https://raw.githubusercontent.com/AnyTimeTraveler/WebComicDownloader/master/imgs/save_last_link.png)

#### 4. Copy the 'link location' from the first image:

![Copy link location of the image](https://raw.githubusercontent.com/AnyTimeTraveler/WebComicDownloader/master/imgs/save_main_image.png)

#### 5. Put all those links after another, seperated by spaces:

```
    java -jar comic-downloader-1.0.jar [link to first page] [link to second page] [link to most recent page] [link to first image location] [target location for cbz file]
```

### Example for http://offsavingtheworld.com/

```
java -jar comic-downloader-1.0.jar http://offsavingtheworld.com/post/167791440063 http://offsavingtheworld.com/post/167791468476 http://offsavingtheworld.com/ https://66.media.tumblr.com/842aa917893b2ffd47d4ed14edc07803/tumblr_ox3qmmOPfF1w278xqo1_1280.png offsavingtheworld.cbz
```

### Example for https://xkcd.com/

```
java -jar comic-downloader-1.0.jar https://xkcd.com/1/ https://xkcd.com/2/ https://xkcd.com/2103/ https://imgs.xkcd.com/comics/barrel_cropped_(1).jpg xkcd.cbz
```

(xkcd has an API and should be downloaded via that to save bandwidth)

### This downloader is meant for entire backups and downloads the entire comic, every time it is used. Please don't overuse it and save bandwidth.



## This project is under MIT License

Have fun!