# PaperValidator

Welcome to the PaperValidator page. The following you can find an overview, a setup guide and a functionality summary.

## Overview

The validity of statistics in scientific publications is crucial for accurate and reliable results.
However, there are many publications, that are not acceptable in this regard. PaperValidator confronts this problem
by proposing a tool that allows for the automated validation of statistics in publications, focusing mainly on
statistical methods and their assumptions. The validation process is rule-based using partially crowd-sourced workers
hired from the Amazon Mechanical Turk (MTurk) platform. The tool and the validation process, were successfully
tested on 100 papers from the ACM Conference on Human Factors in Computing Systems (CHI).

Watch the following movie to see PaperValidator in action.

Here is a short clip with an overview of PaperValidator's functionallity.
[![Watch Video](https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/movie-mock.jpg)](http://www.youtube.com/watch?v=fCDdrjJxUV4)

## Setup

1. Install the two popular tools [Git](https://git-scm.com/) and [sbt](http://www.scala-sbt.org/) by following the
instructions on their websites.
2. Clone your repository to a folder which you can choose freely
```
git clone https://github.com/manuelroesch/PaperValidator.git
```
3. Set up your configuration file by editing the /conf/application.conf.sample file according to your need. After
having done that you have to rename the file to application.conf.
(TODO: Extend this step with further explanations)

4. Start PaperValidator by running the following command in the folder where you downloaded PaperValidator:
```
sbt run -Dhttp.port=1234 -Dconfig.resource=application.conf
```

5. Use PaperValidator according to the "Functionality" section

## Functionality

The PaperValidator consists of different parts, that are based on different frameworks and libraries. Figure 1 presents an
overview of this system and in the following; each part is described in more detail.

### Technical Details
The PaperValidator system builds on the Play! Framework, which is a web framework facilitating the creation of web
applications. The system is mainly written in Scala and partially in Java8. As storage, we use a MySQL9 database, which
runs with our web application on a server at the University of Zurich with an Intel(R) Xeon(R) CPU X5570 @ 2.93GHz
and 80 GB RAM. The PDF processing relies on Apache PDFBox10, an open-source Java tool, which allows the extraction
of content from PDF documents or the conversion of a PDF document into an image. For the crowd-sourcing component,
which is used during the statistics validation process, the system makes use of the PPLib [2], a library, that
facilitates the creation of crowdsourcing tasks. This library was used to send validation tasks to Amazon Mechanical
Turk11 (Mturk), a popular crowdsourcing platform.

### Functionality Overview
The target users of PaperValidator are authors, reviewers, as well as conference chairs. For each of these users, the tool
provides a different functionality. In addition, there are also the Mturk crowd workers who access the tool. All these different
users are presented in Figure 1, where authors/reviewers are on the left, Mturk workers are on the right, and conference
chairs are at the top.

### Functionality for Authors
As can be seen in Figure 1 on the left, an author starts the process by uploading his publication to PaperValidator using
the provided upload form. In doing so, he has to select the conference to which he wants to upload the paper. The conference
was previously created by a conference chair (Figure 1 at the top), which will be explained later. It is worth mentioning that
the system supports the upload of a single PDF file as well as the upload of multiple PDF documents compressed in a ZIP
file.

After the upload, the system analyses the paper using validation algorithms partially based on crowd workers. The
PaperValidator performs an analysis consisting of four different parts: (1) There is the method-assumption part, which
validates methods and assumptions; (2) the Statchecker part, which implements the functionality as provided by the
Statchecker tool as presented in the RelatedWork section; (3) a part that validates some basic statistical rules; and (4) a
part that performs some basic layout inspection. The parts are marked with brackets and numbers in Figure 1.

Part (1), as summarized in Figure 2, is the most central and relevant part in this work. For this method-assumption part, the
text is first extracted from the uploaded PDF and further processed using regular expressions search for a predetermined
set of methods, assumptions, and their synonyms. After having determined all the methods and assumptions in the text, a
matching algorithm determines, which methods and assumptions fit together by using a predefined list containing the
method-assumption allocation.

The next step is the creation of method-assumption snippets, which are later sent to Mturk for validation. The creation of
such snippets is necessary because the copyrights of the papers often prohibit papers be distributed as a whole. The creation
of a snippet works as follows: First, a method-assumption pair, which has been extracted previously, is annotated in a copy of
the uploaded PDF file. The method is annotated in yellow, the assumption in green. In the next step, the PDF file is converted
to a PNG image and cropped so that both the method and assumption are visible. In case they are on different pages,
the pages are put together into one image, and the page break is indicated by a page break symbol. An example of such a
snippet is shown in Figure 3.

The last step in part (1) of the analysis is the validation of the snippet using crowd-sourcing. For this, a question is generated
on Mturk, as shown in Figure 4. The Mturk worker (Mturker) then decides whether the method-assumption pair is related,
and if the author has checked the assumption before applying the method. Thereby, we do not only ask one Mturker, but several
of them with the stopping rule that the final answer must win with at least three more votes than the second most voted
answer. To increase the reliability of the answers, we also introduced two further measures. First, we let the Mturker report
their thoughts during the decision-making process and write them down. This should encourage them to think more deeply
and elaborately. Second, we let them report their confidence from one to seven on a slider (see Figure 4 at the bottom) and
eliminate all answers with a confidence lower than five from further analysis. The threshold of five was determined empirically
by a couple of initial test runs and is also confirmed by the work of Lessel et al. [12], who also uses a seven-point
confidence scale with a threshold of 5.

Part (2) of the analysis, the Statchecker part, first converts the PDF to text and performs a validation equivalent to the
functionality of the Statchecker R package presented in the common statistical tests like f-tests, t-tests, Z-tests or
chisquare tests reported in the APA format using text search with regular expression from the converted text. All the extracted
tests are, in the next step, recalculated and compared with the reported p-values. If such p-values are not in compliance with
the recalculated p-values, they are saved as an error in the database.

Part (3) deals with basic statistical rules as reported in [16]. Here, the first step is once again the conversion of
the PDF into text. Next, PaperValidator performs a text search using regular expressions to answer the following questions:
- Is the sample size stated in the text?
- Is there any incorrect statistical terminology in the text?
- Does the PDF contain any p-values? Are they in the correct range and precision?
- Is there a mean without variance reported in the text?
- Has the author performed a statistical test without stating effect size or power of the test?

In part (4), a simple layout analysis is performed. For that, the PDF is converted into a PNG image, which is analyzed by
PaperValidator considering the following questions:

- Does the paper have a certain distance between content and border so that it can be printed properly?
- Are there any colors used in the paper, which are difficult to read when printed in gray scale?

Notice that the analysis in part (4) is not directly related to statistics but indirectly; e.g. diagrams presented in unreadable
colors makes it challenging for a reader to follow the reported explanations. Besides, part (4) is also a proof of concept, that
the PaperValidator can be easily extended so that not only the contents but also the layout can be checked.
Having finished the paper analysis parts (1)-(4), the author, who has uploaded the PDF, will be notified by an email containing
a hyperlink to the paper analysis result overview page as shown in Figure 5. For each of the four analysis parts, the
results are listed and depending on the result, a warning or an error is generated.

Furthermore, the analysis results overview page also includes, a spell checker, which can be used besides spell checking, to
verify the conversion process from PDF to text. If there are exceptional spelling mistakes listed, which are not present
in the initial PDF file, there was an error in the conversion process and the analysis results are therefore not reliable.
Another source of information when an error happens during the PDF processing is the processing log, which also can be
found on the result overview page. This log shows all the important events and reports all errors thrown by the tool.
There is also a summary of all method-assumption snippets and their corresponding Mturk answers.
The result overview page also allows the download of the analyzed PDF in two versions; one is the blank version, which
is equal to the one which was uploaded to the system, and the other is an annotated version in which all the findings
are highlighted. The most dominant highlighting, thereby, is applied to methods with missing assumption.

## Questions
If you have any further questions, please ask manuel.roesch@gmail.com.