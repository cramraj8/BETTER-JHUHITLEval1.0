# BETTER-JHUHITLEval1.0

How to run EnglishHITL ?
- use the following command

`
java -jar JHUHITLSystem-English.jar config.properties RM3 <path-to-indexDir> <ir-tasks.json> <output-file> <outputToIE-file> <logging-file> <watermark> <ci> <tk> <af> <li> <lf>

`

* ci - boolean to create annotation file (Default : true)
* tk - HITLTopKDocs to limit the top-k annotation documents per query (Default : 10)
* af - annotation filename
* li - boolean to load labelled annotation file (Default : false)
* lf - labelled filename

The last 5 arguments are used for creating annotation file with top-k retrieved documents per query & use the labelled documents to rerank using RM3
