package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.snippet

import scala.xml.NodeSeq

/**
  * Created by mattia on 01.09.15.
  */
object SnippetHTMLTemplate {

	def generateHTMLPage(imgAssetUrl: String, jsAssetUrl: String, question: String, answers: List[String], isTemplate: Boolean = false): NodeSeq = {
		<div ng-controller="QuestionCtrl">
			{if (isTemplate) {
			<p>
				<i>
					You have not accepted this HIT yet. Please note that, once you have accepted this HIT, another Snippet will be displayed (i.e. this is just a sample). The task will remain the same though.
				</i>
			</p>
					<p>
						You are only allowed to work on one HIT per batch. We generally start multiple batches at once, so in case you get an error message, please just look for another of our hits.
						The PREVIEW mode (where this message is displayed) should tell you if you're eligible to work on a HIT. If you get an error message only after accepting the HIT, it means that your browser doesn't relay your Turker ID to us. In that case, you can force it to store this information by opening a separate Tab and logging in here:
						<a href="https://taliesin.ifi.uzh.ch/crowdsa/logout">https://taliesin.ifi.uzh.ch/crowdsa/logout</a>
					</p>
		}}<p>
			Thank you for participating in our survey.
			<br/>
			Our goal is to see whether you are able to grasp some of the main concepts in the field of statistics without needing to be an expert in that field - just by basic text understanding. For that matter, we have prepared multiple such surveys; in all of which you can participate
			<b>at most once</b>
			.
		</p>
			<hr style="width:100%"/>
			<p>
				In the field of statistics, one generally uses
				<b>statistical methods</b>
				(such as ANOVA) to compare groups of data and derive findings. These
				<b>Statistical methods</b>
				in general require some
				<b>prerequisites</b>
				to be satisfied before being applied to data. Please have a look at the text-snipplet below. You'll find a
				<span style="background-color:#FFFF00;">statistical method marked in yellow</span>
				and a
				<span style="background-color:#00FF00;">prerequisite marked in green.</span>
			</p>

			<div class="row" d="snippetButtons">
				<div class="col-md-6">
					<button type="button" id="top" class="btn btn-info" style="width:200px;float:right;" aria-hidden="true">
						Scroll to Method
					</button>
				</div>
				<div class="col-md-6">
					<button type="button" id="bottom" class="btn btn-info" style="width:200px;" aria-hidden="true">
						Scroll to Prerequisite
					</button>
				</div>
			</div>

			<br/>

			<div class="row" style="display: table;">
				<div class="col-md-12" style="float: none;display: table-cell;vertical-align: top;">
					<div id="imgContainer" style="width:100%; min-height:350px; max-height:900px;border:1px solid black;overflow:auto;">
						<img id="snippet" src={imgAssetUrl} width="100%"></img>
					</div>
				</div>
			</div>

			<br/>

			<br/>
			<hr style="width:100%"/>
			<div>
				<h2>In the text above, is there any kind of relationship between the
					<span style="background-color:#00FF00;">prerequisite</span>
					and the
					<span style="background-color:#FFFF00;">method</span>
					?</h2>
				Note that the relationship can be direct or indirect.
				<br/>
				<ul>
					<li>example for a direct relationship: "We have tested [PREREQUISITE] before we used [METHOD] and found that ..."</li>
					<li>example for an indirect relationship: "Our data was tested for [PREREQUISITE]. Using [METHOD] on our data, we have found that ..."</li>
				</ul>
			</div>

			<form onsubmit="return checkFeedbackForm()">
				<h3>
					<label class="radio-inline">
						<input type="radio" name="isRelated" ng-model="isRelated" id="yes" value="Yes" required="required"/>
						Yes
					</label>
					<label class="radio-inline">
						<input type="radio" name="isRelated" ng-model="isRelated" id="no" value="No"/>
						No
					</label>
				</h3>

				<span ng-if="isRelated=='Yes'">
					<hr style="width:100%"/>
					<div>
						<h2>
							Did the authors of the text confirm that they have checked the
							<span style="background-color:#00FF00;">prerequisite</span>
							before applying the
							<span style="background-color:#FFFF00;">method</span>
							?
						</h2>
					</div>
					<h3>
						<label class="radio-inline">
							<input type="radio" name="isCheckedBefore" ng-model="isCheckedBefore" id="yes2" value="Yes" required="required"/>
							Yes
						</label>
						<label class="radio-inline">
							<input type="radio" name="isCheckedBefore" ng-model="isCheckedBefore" id="no2" value="No"/>
							No
						</label>
					</h3>
					{if (question!="") {
					<span ng-if="isCheckedBefore=='Yes'">
						<hr style="width:100%"/>
						<div>
							<h2>
								{question}
							</h2>
						</div>
						<h3>
								<select name="answer">
									{
										answers.map { answer =>
											<option value={answer}>{answer}</option>
										}
									}
								</select>
						</h3>
					</span>
				}}
				</span>
				<hr style="width:100%"/>

				<div class="form-group">
					<label for="descriptionIsRelated">
						Please briefly describe why you selected Yes/No in the previous questions. Please also let us know if you felt uncertain with the answer you've provided. This is also your opportunity to tell us what you thought about this HIT.
					</label>
					<textarea class="form-control" name="descriptionIsRelated" id="descriptionIsRelated" rows="5" required="required">Your text here</textarea>
				</div>

				<hr style="width:100%"/>
				<p>
					Please select the number below that best represents how certain you feel about the answer you have provided before.
				</p>

				<div class="form-group" style="width:100%;">
					<label class="col-sm-6 control-label">Not certain at all</label>
					<label class="col-sm-6 control-label" style="text-align: right">Absolutely certain</label>
				</div>

				<div class="form-group" style="width:100%;">
					<div class="col-sm-12">
						<div class="well">
							<input id="ex1" data-slider-id="ex1Slider" type="text" name="confidence" data-slider-min="1" data-slider-max="7" data-slider-step="1" data-slider-value="1" data="confidence: '1'" value="1" style="display: none;width:100%;">
							</input>
						</div>
					</div>
				</div>

				<hr style="width:100%"/>{if (!isTemplate) {
					<input type="submit" class="btn btn-large btn-primary" style="width:150px;float:right;" value="Submit Answer"/>
			}}
			</form>
			<br/>
			<br/>

			<script id="jsPlaceholder"></script>
			<script type="text/javascript" src={jsAssetUrl}></script>

		</div>
	}

	def generateJavascript: String = {

		"""$('#ex1').slider({
                tooltip: 'always',
                  formatter: function(value) {
                  return value;
                }
              });

          $('#top').click(function() {
            $('#imgContainer').animate({
              scrollTop: $('#imgContainer')[0].scrollHeight*(relativeHeightMethod-2.0)/100
              }, 1000);
          });

          $('#bottom').click(function() {
            $('#imgContainer').animate({
              scrollTop: $('#imgContainer')[0].scrollHeight*(relativeHeightPrerequisite-2.0)/100
              }, 1000);
          });

          $(document).ready( function() {
            $('#imgContainer').css({"width":"100%","min-height":"350px","max-height":"900px","border":"1px solid black","overflow":"auto", "height":snippetHeight})
          })


          $(document).ready( function() {
		  	var snippetHeight = $('#snippet').height();
	 		var containerHeight = $('#imgContainer').height();
            //if($('#snippet').height() < $('#imgContainer').height()){
			if(Math.min(snippetHeight, containerHeight) == snippetHeight) {
              $('#snippetButtons').hide();
            }else{
              $('#top').click();
            }
          });

          function checkFeedbackForm()  {
            var value = document.getElementById('descriptionIsRelated').value;
						if(value.length == 0 || value == 'Your text here') {
						  alert('Please provide feedback!');
							return false;
            } else {
						  return true;
						}
          };

        $('#ex1').slider({
          tooltip: 'always',
          formatter: function(value) {
            return value;
          }
        });
		"""
	}

}
