# PaperValidator

Welcome to the PaperValidator, an open-source statistics validation tool released under the [MIT License](https://github.com/manuelroesch/PaperValidator/blob/master/LICENSE.md)
. This README file is structured as follows:

* [Overview](https://github.com/manuelroesch/PaperValidator#overview)
* [Installation Guide](https://github.com/manuelroesch/PaperValidator#installation-guide)
* [Functionality](https://github.com/manuelroesch/PaperValidator#functionality)
  * [Technical Details](https://github.com/manuelroesch/PaperValidator#technical-details)
  * [Functionality Overview](https://github.com/manuelroesch/PaperValidator#functionality-overview)
  * [Functionality for Authors](https://github.com/manuelroesch/PaperValidator#functionality-for-authors)
  * [Functionality for a Conference Chair](https://github.com/manuelroesch/PaperValidator#functionality-for-a-conference-chair)
* [Frequently Asked Questions](https://github.com/manuelroesch/PaperValidator#frequently-asked-questions)

## Overview

The validity of statistics in scientific publications is crucial for accurate and reliable results. However, there are many publications, that are not acceptable in this regard. The PaperValidator tool confronts this problem by allowing for the automated validation of statistics in publications, focusing mainly on statistical methods and their assumptions. The validation process is rule-based using partially crowd-sourced workers hired from the Amazon Mechanical Turk (MTurk) platform. The tool and the validation process, were successfully tested on 100 papers from the ACM Conference on Human Factors in Computing Systems (CHI).

Click on the following movie clip to see PaperValidator in action:

[![Watch Video](https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/movie-mock.jpg)](http://www.youtube.com/watch?v=fCDdrjJxUV4)

## Installation Guide

1. Install the two tools [Git](https://git-scm.com/) and [sbt](http://www.scala-sbt.org/) by following the instructions on their websites.
2. Clone your repository to a folder which you can choose freely

  ```
  git clone https://github.com/manuelroesch/PaperValidator.git
  ```

3. In case you don't have a MySQL database already running, install it from the [MySQL website](https://www.mysql.com/) by by following the instructions on their websites.
4. Set up your configuration file by editing the /conf/application.conf.sample file according to your needs. See all the important fileds listed below.

  ```
  db.default.url="mysql://username:passwort@127.0.0.1:3306/tablename" #Insert the URL to your database here
  hcomp.ballot.decoratedPortalKey = "mechanicalTurk" #Defines the crowd service you want to use
  hcomp.ballot.baseURL = "http://your-url.com" #Insert the URL where you have installed PaperValidator
  likertCleanedAnswers = 5 #Defining the minimal confidence required to take an Mturk answer into account.

  hcomp.mechanicalTurk.accessKeyID = "Mturk-Access-Key-ID" # Insert your Mturk access key
  hcomp.mechanicalTurk.secretAccessKey = "Mturk-Secret-Access-Key" # Insert your Secret Access Key
  hcomp.mechanicalTurk.sandbox = "false" # Disable sandbox mode

  helper.mailing.active = "true" #POP access to this email has to be enabled
  helper.mailing.from = "your-email@gmail.com"
  helper.mailing.pw = "your-password"

  hcomp.k = 3 #Beat-by-k, Defines the number of answers required to win (relative to the second most frequent response)
  hcomp.maxIterations = 10 #Insert a maximum number of iterations for the beat-by-k mechanism
  hcomp.paymentCents = 50 #How many cents are you willing to pay per Mturk task
  ```

  After having edited the configuration file you have to rename the file to application.conf.

5. Start PaperValidator by running the following command in the folder where you downloaded PaperValidator:

  ```
  sbt "run -Dhttp.port=1234 -Dconfig.resource=application.conf"
  ```

## Functionality

The PaperValidator consists of different parts, that are based on different frameworks and libraries.
In the following; each part is described in more detail.

### Technical Details
The PaperValidator system builds on the [Play! Framework](https://playframework.com/), which is a web framework facilitating the creation of web applications. The system is mainly written in [Scala](http://www.scala-lang.org/) and partially in [Java](http://java.com). As storage, a [MySQL](https://www.mysql.com/) database is used. The PDF processing relies on [Apache PDFBox](https://pdfbox.apache.org/), an open-source Java tool, which allows the extraction of content from PDF documents or the conversion of a PDF document into an image. For the crowd-sourcing component, which is used during the statistics validation process, the system makes use of the [PPLib](https://github.com/uzh/PPLib), a library, that facilitates the creation of crowdsourcing tasks. This library was used to send validation tasks to [Amazon Mechanical Turk (Mturk)](https://www.mturk.com/mturk/welcome), a popular crowdsourcing platform.

### Functionality Overview
The target users of PaperValidator are authors, reviewers, as well as conference chairs. For each of these users, the tool provides a different functionality. In addition, there are also the Mturk crowd workers who access the tool. All these different users are presented in Figure 1, where authors/reviewers are on the left, Mturk workers are on the right, and conference chairs are at the top.

<a href="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/overview.png" target="_blank">
<img src="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/overview.png" width="500">
</a>
</a><br><sup>Figure 1: An overview of the most important parts and functionalities of the PaperValidator tool.</sup>

### Functionality for Authors
An author starts the process by uploading his publication to PaperValidator using the provided upload form (Figure 2). In doing so, he has to select the conference to which he wants to upload the paper. The conference was previously created by a conference chair, which will be explained later. It is worth mentioning that the system supports the upload of a single PDF file as well as the upload of multiple PDF documents compressed in a ZIP file.

<a href="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/upload-paper.png" target="_blank">
<img src="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/upload-paper.png" width="500">
</a><br><sup>Figure 2: PaperValidator's paper upload form.</sup>

After the upload, the system analyses the paper using validation algorithms partially based on crowd workers. The PaperValidator performs an analysis consisting of four different parts: (1) There is the method-assumption part, which validates methods and assumptions; (2) the Statchecker part, which implements the functionality as provided by the Statchecker tool as presented in the RelatedWork section; (3) a part that validates some basic statistical rules; and (4) a part that performs some basic layout inspection.

Part (1), as summarized in Figure 3, is the most central and relevant part in this work. For this method-assumption part, the text is first extracted from the uploaded PDF and further processed using regular expressions search for a predetermined set of methods, assumptions, and their synonyms. After having determined all the methods and assumptions in the text, a matching algorithm determines, which methods and assumptions fit together by using a predefined list containing the method-assumption allocation.

The next step is the creation of method-assumption snippets, which are later sent to Mturk for validation. The creation of such snippets is necessary because the copyrights of the papers often prohibit papers be distributed as a whole. The creation of a snippet works as follows: First, a method-assumption pair, which has been extracted previously, is annotated in a copy of the uploaded PDF file. The method is annotated in yellow, the assumption in green. In the next step, the PDF file is converted to a PNG image and cropped so that both the method and assumption are visible. In case they are on different pages, the pages are put together into one image, and the page break is indicated by a page break symbol.

The last step in part (1) of the analysis is the validation of the snippet using crowd-sourcing. For this, a question is generated on Mturk. The Mturk worker (Mturker) then decides whether the method-assumption pair is related, and if the author has checked the assumption before applying the method. Thereby, we do not only ask one Mturker, but several of them with the stopping rule that the final answer must win with at least three more votes than the second most voted answer. To increase the reliability of the answers, we also introduced two further measures. First, we let the Mturker report their thoughts during the decision-making process and write them down. This should encourage them to think more deeply and elaborately. Second, we let them report their confidence from one to seven on a slider and eliminate all answers with a confidence lower than five from further analysis. The threshold of five was determined empirically by a couple of initial test runs and is also confirmed by the work of Lessel et al., who also uses a seven-point confidence scale with a threshold of 5.

<a href="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/analysis-overview.png" target="_blank">
<img src="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/analysis-overview.png" width="500">
</a><br><sup>Figure 3: The different steps of the PaperValidator’s method-assumption analysis.</sup>

Part (2) of the analysis, the Statchecker part, first converts the PDF to text and performs a validation equivalent to the functionality of the Statchecker R package presented in the common statistical tests like f-tests, t-tests, Z-tests or chisquare tests reported in the APA format using text search with regular expression from the converted text. All the extracted tests are, in the next step, recalculated and compared with the reported p-values. If such p-values are not in compliance with the recalculated p-values, they are saved as an error in the database.

Part (3) deals with basic statistical rules as reported in. Here, the first step is once again the conversion of the PDF into text. Next, PaperValidator performs a text search using regular expressions to answer the following questions:

* Is the sample size stated in the text?
* Is there any incorrect statistical terminology in the text?
* Does the PDF contain any p-values? Are they in the correct range and precision?
* Is there a mean without variance reported in the text?
* Has the author performed a statistical test without stating effect size or power of the test?

In part (4), a simple layout analysis is performed. For that, the PDF is converted into a PNG image, which is analyzed by PaperValidator considering the following questions:

* Does the paper have a certain distance between content and border so that it can be printed properly?
* Are there any colors used in the paper, which are difficult to read when printed in gray scale?

Notice that the analysis in part (4) is not directly related to statistics but indirectly; e.g. diagrams presented in unreadable colors makes it challenging for a reader to follow the reported explanations. Besides, part (4) is also a proof of concept, that the PaperValidator can be easily extended so that not only the contents but also the layout can be checked.

Having finished the paper analysis parts (1)-(4), the author, who has uploaded the PDF, will be notified by an email containing a hyperlink to the paper analysis result overview page (Figure 4). For each of the four analysis parts, the results are listed and depending on the result, a warning or an error is generated.

<a href="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/paper-detail.png" target="_blank">
<img src="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/paper-detail.png" width="500">
</a><br><sup>Figure 4: The result overview page showing all the analysis results.</sup>

Furthermore, the analysis results overview page also includes, a spell checker, which can be used besides spell checking, to verify the conversion process from PDF to text. If there are exceptional spelling mistakes listed, which are not present in the initial PDF file, there was an error in the conversion process and the analysis results are therefore not reliable.

Another source of information when an error happens during the PDF processing is the processing log, which also can be found on the result overview page. This log shows all the important events and reports all errors thrown by the tool. There is also a summary of all method-assumption snippets and their corresponding Mturk answers.

The result overview page also allows the download of the analyzed PDF in two versions; one is the blank version, which is equal to the one which was uploaded to the system, and the other is an annotated version in which all the findings are highlighted. The most dominant highlighting, thereby, is applied to methods with missing assumption.

### Functionality for a Conference Chair
The main functionality for a conference chair is related to the creation and administration of a conference. PaperValidator provides for this purpose several interfaces such as a conference creation form, conference settings pages as well as a conference overview page. In the following paragraphs, each of these interfaces is explained in more detail.

First, the conference creation form (Figure 5), allows the chair to create a conference by choosing a name for the conference and selecting a method-assumption template, which later builds the base for the method-assumption validation process. It is worth mentioning that this template is only the base and it is freely adaptable later. Having created the conference, the creator gets an email with a hyperlink to the conference overview page.

<a href="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/create-conference.png" target="_blank">
<img src="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/create-conference.png" width="500">
</a><br><sup>Figure 5: The conference creation form.</sup>

This conference overview page (Figure 6), consists of three parts. On the top, there are three buttons relating to different conference settings concerning the method-assumption validation. The next part, in the middle, lists all the papers, which have been uploaded to the conference so far. Besides the processing status of the uploaded papers, the list also shows how many warnings and errors have been found for each paper. Moreover, by clicking on a paper, a conference chair can get to the paper results overview page and use all its functionality, as has already been presented in the Functionality for Authors Section. The bottom of the conference overview page provides some statistics about all the uploaded papers and the findings of its validation process. Moreover, you can see the Mtruk answer detail from the whole conference and you can download the found methods and method-assumption pairs in the CSV file by clicking the buttons at the bottom of the page.

<a href="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/conference-overview.png" target="_blank">
<img src="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/conference-overview.png" width="500">
</a><br><sup>Figure 6: The conference overview page.</sup>

The three method-assumption validation settings pages, which are at the top of the conference overview page, have the following meanings. The first relates to the interface for inserting and editing methods, assumptions and their synonyms (Figure 7); the second is for linking methods with their associated assumptions (Figure 8), and the last is for flagging the linked method and assumptions (Figure 9). With this flagging option, a conference chair can assign an importance to each of the method-assumption linking. So for example, if a chair flags ANOVA and its assumption of a normal distribution as required, every paper will show an error when ANOVA is used without checking for a normal distribution first. The flagging also directly influences the highlighting color on the paper overview page.

<a href="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/settings-edit.png" target="_blank">
<img src="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/settings-edit.png" width="500">
</a><br><sup>Figure 7: The method and assumption editing page.</sup>

<a href="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/settings-link.png" target="_blank">
<img src="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/settings-link.png" width="500">
</a><br><sup>Figure 8: The method and assumption linking page.</sup>

<a href="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/settings-flag.png" target="_blank">
<img src="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/settings-flag.png" width="500">
</a><br><sup>Figure 9: The method and assumption flagging.</sup>

### Functionality for Authors and Conference Chair

Whenever an author or conference chair loses the email with the link to a publication or conference, there is the option of resending the link respectively sending the link to the my account page (Figure 11) by submitting the email address via the my account form (Figure 10). It is to notice that the link is sent maximally once a day to prevent spam.

<a href="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/my-account.png" target="_blank">
<img src="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/my-account.png" width="500">
</a><br><sup>Figure 10: Get your account link via the my account form.</sup>

The my account page (Figure 11) contains the uploaded PDFs as well as the created conferences from a particular email address. You can either get to the result overview page (Figure 4) or to the converence overview page by clicking on the according link.

<a href="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/my-account.png" target="_blank">
<img src="https://raw.githubusercontent.com/manuelroesch/PaperValidator/master/public/images/help/my-account.png" width="500">
</a><br><sup>Figure 11: The my account overview page.</sup>

## Frequently Asked Questions

_**Is PaperValidator really free from commercial interests?**_

Yes, PaperValidator is a project of the University of Zurich with the aim to improve statistics in scientific publications. There are no hidden commercial interests and the tool is truly free and independent.

_**I need support, who should I ask?**_

If you have any further questions or troubles with the installation, please ask manuel.roesch@uzh.ch. Notice that the support is limited and you may have to wait a day or two to get an answer.

_**Will PaperValidator be extended any time soon?**_

PaperValidator was built as an all-purpose research paper validation tool, which is open for many extensions. Further statistical analysis tests could be implemented alongside other tests concerning layout, grammar or content. What exactly the next steps are, is still under discussion.

---

*Text and images are partially taken from Rösch, M. (2016, September). PaperValidator - Towards the Automated Validation of Statistics in Publications. Master Thesis, University of Zurich.*