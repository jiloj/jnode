## jnode

A REST service to expose and easily get historical jeopardy data. This service uses the following technologies primarily: scala, play, slick.

### Background

I wanted to create jeopardy related computer software and wanted to learn something new while I did it. So I decided to create a base microservice for serving up Jeopardy! questions extracted from the j-archive. This is the central *node* of the entire computer framework I want to create to train for jeopardy, hence *jnode*. This is the central component and data access layer for all other components.

### Usage

The general flow to use this tool is to initialize the persistence layer, download the j-archive data, parse, insert, and profit! In this order more or less. The routes used for this an example setup is used below.

### Other setup

UTF8MB4

### Copyright

This project is read only. That is why there is no license provided with it. I would like people to see this work and learn from it, however. Since the data this scrapes from comes from j-archive, which hosts data that is not legally theirs, I do not want to cause any trouble for the j-archive team that is really doing god's work. So please, be mindful of how you use this repository.


Enjoy!


-- Matias Grioni
