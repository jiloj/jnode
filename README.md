## jnode

A REST service to expose and easily get historical jeopardy data. This service uses the following technologies primarily: scala, play, slick, mysql.

### Background

I wanted to create jeopardy related computer software and wanted to learn something new while I did it. So I decided to create a base microservice for serving up Jeopardy! questions extracted from the j-archive. This is the central *node* of the entire computer framework I want to create to train for jeopardy, hence *jnode*. This is the central component and data access layer for all other components.

### Getting Started

### Requirements

To properly build and run this system the following software packages are needed.

- IntelliJ
- Scala 2.11 or 2.12
- JDK 8
- Mysql 5.7+

The project should then be imported into IntelliJ where it can be built and the setup, using REST routes can be done.

### Usage

The general flow to use this tool is to initialize the persistence layer, download the j-archive data, parse, insert, and profit! In this order more or less. The routes used for this an example setup is used below.

#### Initialize

`localhost/initialize`

This should be run on first creating the app / service and should not be run again. This route will setup the persistence layer on the local mysql installation and `jnode` table.

#### Fetch

`localhost/fetch?start={start}&end={end}&per={per}&interval={interval}&buffer={buffer}`

This will retrieve pages from j-archive and store them locally. The parameters define what range of pages to download, so as to control the range of data, and how fast to download the pages.

- `start`: The j-archive game id of the first page to download.
- `end`: The j-archive game id of the last page to download.
- `per`: The number of pages to download at a time. This is optional and has a default of 1.
- `interval`: The interval between page (group) requests, in ms. This is optional and has a default of 500.
- `buffer`: The number of page requests that can accumulate in the buffer before we start to throttle in some way. This is optional and has a default of 3.

#### Index

`localhost/index`

This will index all the pages in local cache. Note that this is an all or nothing event. A prior index cannot be saved, and once this route is run the prior index is completely destroyed and recreated.

#### Run through

Therefore a typical instantiation of a jnode server might look like the following rest calls.

```
localhost/initialize
localhost/fetch?start=1&end=6130
localhost/index
```

At this point, everything is setup and ready for information extraction.

### Other setup

J-archive pages have some UTF-8 in them on occasion. For this reason, the 

### Copyright

This project is read only. That is why there is no license provided with it. I would like people to see this work and learn from it, however. Since the data this scrapes from comes from j-archive, which hosts data that is not legally theirs, I do not want to cause any trouble for the j-archive team that is really doing god's work. So please, be mindful of how you use this repository.


Enjoy!


-- Matias Grioni
