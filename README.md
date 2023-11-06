# Spring Family Album

![Main View](img/ScreenshotMain.png)

This webapp is intended to show photos from local storage and is based on tags system. It reads files right from filesystem and displays them in webbrowser, it doesn't have it's own persistent state.

If you have tagged people in **Digikam** app, you can provide **Spring Family Album** a path to digikam database and you will be able to browse your photos grouped by people and year. You can fine-tune subset of images to be shown by whitelist and blacklist of directories in properties file.

## Quick Start

In order to run this app you need a **JDK 17** and **Maven** in your Path.

1. Download ZIP under `Code -> Download ZIP`
2. Extract it to directory of your choice;
3. Make sure `src\main\resources\application.yml` file has only these properties set and nothing else:
	```
    filepaths:
		imageObjectsJson: classpath:sampleData\imageObjs.json
		graphvizTree: classpath:sampleData\familyTree.dot
	```

4. Open Terminal in directory with `pom.xml` and execute `mvn clean spring-boot:run`
5. Visit http:\\localhost:8080

Webapp will launch with sample data - tagged photos and family tree are included in project. 



## Digikam Database

This app can extract Image objects from Digikam database. It is convenient because Digikam itself has capabilities of applying face tags to photos. In it's database Digikam contains filepaths relative to root photos directory. 
* Root photos directory is chosen during first run of **digikam**.
* `digikam4.db` file by default is in `C:\users\%USERNAME%` directory.

Add these properties to `src/main/resources/application.yml` file:

```
filepaths:
	dgkmRoot: E:\PHOTOS
jdbc:
	driverClassName: org.sqlite.JDBC
	url: jdbc:sqlite:C:/Users/user/digikam4.db
```

Notice how path to `digikam4.db` file is specified in `jdbc.url`.
Don't forget to comment out (or remove) `filepaths.imageObjectsJson` property since imageObjects will be constructed from sqlite database instead of json file.

## Basic usage

* Click checkboxes of people you want to see and click **Show** button, you will see all photos where those people are together in chronological order. 
* Choose a year and click **Show** button, you will see all photos of this year.
* You can combine two above filters.
* In single image view you can go to next/previous photo from search results.
* In single image view you can examine a list of people depicted in current photo and jump to anyone of them with a single click.
* If too many images have been found, pagination block will appear in search results.

## Additional capabilities

* You can use `tagIdToNameFile` property to specify a path to tagName->tagId TSV file which allows webapp to display more user-friendly names of tags in comparison to those which were extracted from your Digikam db.
* You can use `thumbsDirectory` property to specify a path to directory which contains `\d{32}.[(png)(jpg)]` files where 32 chars is a digikam-calculated hashcode of an image this thumbnail refers to. Those thumbnails will be used in gallery view and are supposed to lower disk and network usage. There is no implicit thumbnail creation capabilities in this webapp.
* Use `whiteListDirectories` and `blackListDirectories` properties to filter subset of images to be shown.
* Use `imageMagickHashFiles` property to specify a path(s) to `FullName->ImageHash->Length` TSV files, where `ImageHash` is a 64-char hashcode calculated by ImageMagick. It lets you to overwrite file paths for images found in `imageobjects.json` file. Fore every image deserialized from json file, if it's `imHash` found in one of those `imageMagickHashFiles`, filepath from this file will be used instead of one which is written in json file.

## Explore data

This app works around Neo4j database. It is emmbedded, so it isn't possible to run Neo4j Browser for embedded database which is currently in use. But once application is stopped, it leaves database populated in `./target/` directory, and you can run separate neo4j server against this database to visually explore data created by application. If you're in current directory with Powershell, execute following commands and visit http://localhost:7474 .

	$neo4jDataDir = './target/mydb/data/'
	docker run --name neo4j-1 --rm -v $neo4jDataDir`:/data -p 7474:7474 -p 7687:7687 -e NEO4J_AUTH=neo4j/qwertyuiop -d neo4j

![Neo4j Schema Visualization](img/neo4j%20schema.jpg)