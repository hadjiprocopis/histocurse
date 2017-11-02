#!/bin/bash

ant compiletest && java -classpath /home/andreas/PROJECTS/JAVA/HISTOGRAMS_ND/test:/usr/local/apache-commons/lib/commons-math-3.6.jar:/home/andreas/PROJECTS/JAVA/dist/lib/ahp.org.Cartesians.jar:/home/andreas/PROJECTS/JAVA/dist/lib/ahp.org.Histograms.jar TestHistogramNDFlatArray_randombins

