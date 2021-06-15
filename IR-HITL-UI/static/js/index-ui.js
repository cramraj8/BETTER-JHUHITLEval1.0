
$(document).ready(function () {
  console.log("JS Started working ... ");
  // slider defaults
  setDefaultFirst();
  setDefaultsecond();
  // clear the button
});


function example_1_clicked() {
  $('#task_title').val('Flint Water Crisis');
  $("#task_stmt").val('Examine the causes and responses to the Flint water crisis that exposed residents to high levels of lead poisoning');
  $("#task_narr").val('The Flint water crisis is a public health crisis that started in 2014, after the drinking water source for the city of Flint, Michigan was changed from treated Detroit Water and Sewerage Department water (sourced from Lake Huron and the Detroit River) to the Flint River.  As a result, the Flint residents were exposed to elevated levels of lead.');
  $('#req_text').val('Identify any government officials criminally charged as a result of the water crisis.');


}

function example_2_clicked() {
  $('#task_title').val('Solar power in the US');
  $('#req_text').val('What federal government programs have supported the growth of solar energy?');
  $("#task_stmt").val('To understand the development of solar energy in the US.  How did it start, how did it grow to industrial scale and significant residential penetration. Solar energy required public and private investment in the production of the solar panels.');
  $("#task_narr").val('The US has made significant investments in solar energy over the past decade.  Currently, residential photovoltaics supply the majority of solar power in the US, but recently large scale solar power generation facilities have been deployed in southwestern states.  The boom in solar energy is creating new jobs in manufacturing, installation, and power generation.  Recently, tariffs have brought some uncertainty to the marketplace.');



}


function CleanButtonClicked() {
      console.log("clicked clear ...");

      $('#task_title').val('');
      $('#task_stmt').val('');
      $('#task_narr').val('');
      $('#req_text').val('');
      $('#no_fd').val('');
      $('#no_ft').val('');
      $('#remove_stopwords').prop('checked', true);
      setDefaultFirst();
      setDefaultsecond();
}

function ResetButtonClicked() {
  $.ajax({
    url: "/reset",
    type: "POST",
    contentType: "application/json"
  })
  .fail(function (jqXHR, textStatus, errorThrown) {
    console.log("fail: ", textStatus, errorThrown);
  });

  $('#bottom_div').hide();
}

// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================

function setDefaultFirst() {
  const el = document.getElementById("firstRangeInput");
  el.value = 0.3; // 0.6;
  let value = el.value * 100;
  var children = el.parentNode.childNodes[1].childNodes;
  children[1].style.width = value + "%";
  children[5].style.left = value + "%";
  children[7].style.left = value + "%";
  children[11].style.left = value + "%";
  children[11].childNodes[1].innerHTML = el.value;
}

function setDefaultsecond() {
  const el = document.getElementById("secondRangeInput");
  el.value = 0.8; // 0.7;
  let value = (1 - el.value) * 100;
  var children = el.parentNode.childNodes[1].childNodes;
  children[3].style.width = value + "%";
  children[5].style.right = value + "%";
  children[9].style.left = el.value * 100 + "%";
  children[13].style.left = el.value * 100 + "%";
  children[13].childNodes[1].innerHTML = el.value;
}

// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================

// ==========================Message space =====================================
function togggleAlert(msg) {
  // MZ: disable this function for now
  // let message = msg;
  // if (message) {
  //   $("#message-space").html(`
  //     <div class="col-md-6 alert alert-success alert-dismissible" role="alert">
  //       <a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a>
  //       <p id="message-space-msg">${message}</p>
  //     </div>
  //   `);
  // }
}

// ============================= keyterms filled by ajax ======================
function fillTerms(terms, prevTerms) {
  let terms_html = "<h4>Top-20 Keywords</h4>";
  terms.forEach(displayTerm);
  $("#terms_QE_div").html(terms_html);

  function displayTerm(term) {
    let span_class = "";
    if ($.inArray(term, prevTerms) == -1) {
      span_class = "progress-bar-success";
    }
    terms_html = terms_html +
      `<span class="badge ${span_class}" style="margin: 0.1rem;" ><span style="padding: 0rem;font-size: small;">${term}</span> </span>`;
  }
}

// ====================== fill side buttons =============
// ====================== fill side buttons =============
function fillSideButtons(query_results, query_id, prev_docids) {
  let sidebtns = "";
  query_results.forEach(addDocButton);

  let div_html = `
    <h3>Query #${query_id} Results</h3>
    <div id="search_ids" class="btn-group-vertical">
    ${sidebtns}
    </div>
    `;
  $("#left_side_div").html(div_html);

  function addDocButton (pair,index) {
    let btn_class = 'btn-secondary';
    let prevIndex=$.inArray(pair["docID"], prev_docids)
    let color="green"
    let direction ="arrow-up"
    let count=Math.abs(index-prevIndex)
    if (prevIndex == -1) {
    btn_class = 'btn-side'
    color="white"
    count=0;
    }else{
    if(index>prevIndex){
    color="red";
    direction ="arrow-down"
    }
  }
  let arrow=""
  if(count){
    arrow=`<span style="font-size:2rem;font-weight: 600;"><i style="vertical-align: middle;color:${color};" data-feather="${direction}"></i>${count}<span>`
    }
    // sidebtns = sidebtns +
    // `<div class="form-group form-inline">
    // <span style="height:100%;color:black;font-size: 2rem;font-weight: 600;";>${index+1}</span>
    sidebtns = sidebtns +
    `<div class="form-group form-inline">
    <button data-toggle="tooltip" data-placement="right" title="${pair["docTextSentences"][0]}"
    onclick='scrollToDiv("${pair["docID"]}_tab1","${pair["docID"]}_tab2")' role="button" type="button"
    class="btn ${btn_class}" style="padding-left:1px; padding-right:1px; width: 200px; height: 10rem; vertical-align: middle;font-size: 1rem;font-weight: 600;
    ">
    <span style="white-space: normal; font-size: 150%; vertical-align: middle; display: inline-block;font">
    ${pair["rank"]}. ${pair["docTextSentences"][0]}
    </span>
    </button>
    ${arrow}
    <div>
    <br>`;
  }
  feather.replace({width: '1em', height: '1.5em'})
}


// =============================== tab1
function filltextRanks(query_results) {
  let textranksdoc = "";
  let trows = [];
  query_results.forEach((pair) => getsents(pair["docTextSentences"]));
  function getsents(sentences) {
    let rows = "";
    for (let sent of sentences) {
      rows =
        rows +
        `<tr>
          <th class="sent-row-col-1" width="10%">
            <div class="rating">
                <div class="like grow" onclick="thumbsUpClicked()" style="width:10px; display:inline;">
                  <i class="fa fa-thumbs-up fa-2x thumbs_up" aria-hidden="true" rate_val="0"></i>
                </div>
                <div class="dislike grow" onclick="thumbsDownClicked()" style="width:10px; display:inline;">
                  <i class="fa fa-thumbs-down fa-2x thumbs_down" aria-hidden="true" rate_val="0"></i>
                </div>
            </div>
          </th>
          <th class="sent-row-col-2" width="90%" style="font-weight: normal;overflow-wrap:break-word;">${sent}</th>
        </tr>`;
    }
    trows.push(rows);
  }

  function getpairs(pair, index, query_results) {
    textranksdoc =
      textranksdoc +
      `<div id='${pair["docID"]}_tab1'>
        <h4>Document ${index+1} : ${pair["docTextSentences"][0]}</h4>
        <table class="table table-condensed table-striped" id="sent_hitl_table">
          ${trows[index]}
        </table>
      </div>`;
  }

  query_results.forEach(getpairs);
  $("#textrank").html(textranksdoc);
}

// =============================== tab2
function fillHighlighted(query_results, highlighted_text_list) {
  let textranksdoc = "";
  let trows = [];
  query_results.forEach((pair) => getsents(pair["filteredTextWithEntitiesHighlighted"]));
  function getsents(sentences) {
    let rows = "";
    for (let sent of sentences) {
      rows =
        rows +
        `<tr>
          <th class="sent-row-col-1" width="10%">
            <div class="rating">

            </div>
          </th>
          <th class="sent-row-col-2" width="90%" style="font-weight: normal;overflow-wrap:break-word;">${sent}</th>
        </tr>`;
    }
    trows.push(rows);
  }

  function getpairs(pair, index, query_results) {
    textranksdoc =
      textranksdoc +
      `<div id='${pair["docID"]}_tab1'>
        <h4>Document ${index+1} : ${pair["docTextSentences"][0]}</h4>
        <table class="table table-condensed table-striped" id="sent_hitl_table">
          ${trows[index]}
        </table>
      </div>`;
  }

  query_results.forEach(getpairs);

  $("#entityrankth").html(textranksdoc);
}


// =============================== tab3
function fillHighlightedIEEvents(query_results, highlighted_text_list) {
  let textranksdoc = "";
  let trows = [];
  query_results.forEach((pair) => getsents(pair["ie_events_per_sentences"]));
  function getsents(sentences) {
    let rows = "";
    for (let sent of sentences) {
      rows =
        rows +
        `<tr>
          <th class="sent-row-col-1" width="10%">
            <div class="rating">

            </div>
          </th>
          <th class="sent-row-col-2" width="90%" style="font-weight: normal;overflow-wrap:break-word;">${sent}</th>
        </tr>`;
    }
    trows.push(rows);
  }

  function getpairs(pair, index, query_results) {
    textranksdoc =
      textranksdoc +
      `<div id='${pair["docID"]}_tab1'>
        <h4>Document ${index+1} : ${pair["docTextSentences"][0]}</h4>
        <table class="table table-condensed table-striped" id="ie_hitl_table">
          ${trows[index]}
        </table>
      </div>`;
  }

  query_results.forEach(getpairs);

  $("#ierankth").html(textranksdoc);

}

//   ============================
function do_dom_fill(response) {
  console.log("let's fill dom");
  $('#bottom_div').show();
  togggleAlert(response.message_board);
  fillTerms(response.terms_QE_list, response.prev_terms_QE_list);
  fillSideButtons(response.query_results, response.current_query_id, response.prev_docids);
  filltextRanks(response.query_results);
  // fillHighlighted(response.query_results, response.highlighted_text_list);
  fillHighlighted(response.query_results, response.highlighted_text_list);
  fillHighlightedIEEvents(response.query_results, response.highlighted_text_list);

  feather.replace();
}

// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================

function InputQueryButtonClicked() {
    console.log("clicked search ...");
    $("#search_button").attr("disabled", true);

    var task_title = $('#task_title').val();
    var task_stmt = $('#task_stmt').val();
    var task_narr = $('#task_narr').val();
    var req_text = $('#req_text').val();
    var no_fd = $('#no_fd').val();
    var no_ft = $('#no_ft').val();
    var firstRangeInput = $('#firstRangeInput').val();
    var secondRangeInput = $('#secondRangeInput').val();
    if($("#remove_stopwords").is(':checked')) {
       var remove_stopwords = 'true';
    } else {
       var remove_stopwords = 'false';
    }

    var input_query_array = [];
    input_query_array.push({"task_title" : task_title});
    input_query_array.push({"task_stmt" : task_stmt});
    input_query_array.push({"task_narr" : task_narr});
    input_query_array.push({"req_text" : req_text});
    input_query_array.push({"no_fd" : no_fd});
    input_query_array.push({"no_ft" : no_ft});
    input_query_array.push({"queryWeight" : firstRangeInput});
    input_query_array.push({"HITLWeight" : secondRangeInput});
    input_query_array.push({"remove_stopwords" : remove_stopwords});

    $.ajax({
      url: "/layer1_search",
      type: "POST",
      contentType: "application/json",
      data: JSON.stringify(input_query_array),
    })
      .done(function (response) {

        do_dom_fill(response);

        $("#search_button").attr("disabled", false);
        console.log("Layer-1 results retrieved !");
      })
      .fail(function (jqXHR, textStatus, errorThrown) {
        console.log("fail: ", textStatus, errorThrown);
      });
}

function PreviousQueryButtonClicked() {
  $.ajax({
    url: "/prev",
    type: "POST",
    contentType: "application/json"
  })
  .done(function (response) {
    do_dom_fill(response)
  })
  .fail(function (jqXHR, textStatus, errorThrown) {
    console.log("fail: ", textStatus, errorThrown);
  });
}

function NextQueryButtonClicked() {
  $.ajax({
    url: "/next",
    type: "POST",
    contentType: "application/json"
  })
  .done(function (response) {
    do_dom_fill(response)
  })
  .fail(function (jqXHR, textStatus, errorThrown) {
    console.log("fail: ", textStatus, errorThrown);
  });
}

// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================

function EntityTAB_EntitiesClicked() {
  var thisElement = $(event.target);
  var thisElementRating = $(thisElement).attr("rate_val");
  if (thisElementRating == "0") {
    thisElement.removeClass("ENT_neutral_like");
    thisElement.addClass("ENT_pos_like");
    $(thisElement).attr("rate_val", "1");
  } else if (thisElementRating == "1") {
    thisElement.removeClass("ENT_pos_like");
    thisElement.addClass("ENT_neg_like");
    $(thisElement).attr("rate_val", "-1");
  } else {
    thisElement.removeClass("ENT_neg_like");
    thisElement.addClass("ENT_neutral_like");
    $(thisElement).attr("rate_val", "0");
  }
}

function handleEntityTAB_Reranking() {
  console.log("start Entity feedback re-rankings");
  $("#ent_rank_btn").attr("disabled", true);

  var task_title = $('#task_title').val();
  var task_stmt = $('#task_stmt').val();
  var task_narr = $('#task_narr').val();
  var req_text = $('#req_text').val();
  var no_fd = $('#no_fd').val();
  var no_ft = $('#no_ft').val();
  var firstRangeInput = $('#firstRangeInput').val();
  var secondRangeInput = $('#secondRangeInput').val();
  if($("#remove_stopwords").is(':checked')) {
     var remove_stopwords = 'true';
  } else {
     var remove_stopwords = 'false';
  }

  var input_query_array = [];
  input_query_array.push({"task_title" : task_title});
  input_query_array.push({"task_stmt" : task_stmt});
  input_query_array.push({"task_narr" : task_narr});
  input_query_array.push({"req_text" : req_text});
  input_query_array.push({"no_fd" : no_fd});
  input_query_array.push({"no_ft" : no_ft});
  input_query_array.push({"queryWeight" : firstRangeInput});
  input_query_array.push({"HITLWeight" : secondRangeInput});
  input_query_array.push({"remove_stopwords" : remove_stopwords});

  var annotations_array = [];
  var i = 0;
  $("#ent_hitl tr th .ent-hl").each(function () {
    var entity_text = $(this).text();
    var entity_rating = $(this).attr("rate_val");

    if (entity_rating == "1") {
      annotations_array.push({
            positive: entity_text,
      });
    } else if (entity_rating == "-1") {
      annotations_array.push({
            negative: entity_text,
      });
    }
    i++;
  });

  var parsing_array = {"input_query_array": input_query_array,
                        "annotations_array": annotations_array};

  // compose AJAX transfer
  $.ajax({
    url: "/layer2_search",
    type: "POST",
    contentType: "application/json",
    data: JSON.stringify(parsing_array),
  })
    .done(function (response) {
      $(".ent-hl").removeClass("ENT_pos_like");
      $(".ent-hl").removeClass("ENT_neg_like");
      $(".ent-hl").addClass("ENT_neutral_like");

      do_dom_fill(response);

      $("#ent_rank_btn").attr("disabled", false);
      console.log("HITL sentence annotations parsed to FLASK -> success");
    })
    .fail(function (jqXHR, textStatus, errorThrown) {
      console.log("fail: ", textStatus, errorThrown);
    });
}

// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================

function thumbsUpClicked() {
  var thisElement = $(event.target);
  if (thisElement.hasClass("like_rate_action")) {
    thisElement.removeClass("like_rate_action");
    $(thisElement).attr("rate_val", "0");
  } else {
    thisElement.addClass("like_rate_action");
    $(thisElement).attr("rate_val", "1");

    // var thumbsDown = thisElement.parent().parent().children('.thumbs_down');
    // console.log(thumbsDown.attr('class'));
    // thumbsDown.removeClass("dislike_rate_action");
    // $(thumbsDown).attr("rate_val", "0");
  }


}

function thumbsDownClicked() {
  var thisElement = $(event.target);
  if (thisElement.hasClass("dislike_rate_action")) {
    thisElement.removeClass("dislike_rate_action");
    $(thisElement).attr("rate_val", "0");
  } else {
    thisElement.addClass("dislike_rate_action");
    $(thisElement).attr("rate_val", "1");

    // var thumbsUp = thisElement.parent().parent().children('.thumbs_up');
    // console.log(thumbsUp.attr('class'));
    // thumbsUp.removeClass("like_rate_action");
    // $(thumbsUp).attr("rate_val", "0");
  }
}

// ===========================
function handleTextTAB_Reranking() {
  console.log("start Text feedback re-rankings");
  $("#text_rank_btn").attr("disabled", false);

  var task_title = $('#task_title').val();
  var task_stmt = $('#task_stmt').val();
  var task_narr = $('#task_narr').val();
  var req_text = $('#req_text').val();
  var no_fd = $('#no_fd').val();
  var no_ft = $('#no_ft').val();
  var firstRangeInput = $('#firstRangeInput').val();
  var secondRangeInput = $('#secondRangeInput').val();
  if($("#remove_stopwords").is(':checked')) {
     var remove_stopwords = 'true';
  } else {
     var remove_stopwords = 'false';
  }

  var input_query_array = [];
  input_query_array.push({"task_title" : task_title});
  input_query_array.push({"task_stmt" : task_stmt});
  input_query_array.push({"task_narr" : task_narr});
  input_query_array.push({"req_text" : req_text});
  input_query_array.push({"no_fd" : no_fd});
  input_query_array.push({"no_ft" : no_ft});
  input_query_array.push({"queryWeight" : firstRangeInput});
  input_query_array.push({"HITLWeight" : secondRangeInput});
  input_query_array.push({"remove_stopwords" : remove_stopwords});


  var annotations_array = [];
  var i = 0;
  $(".rating").each(function () {

      var thumbsUp_rate = $(this).children(".like")
                                  .children(".thumbs_up")
                                  .attr("rate_val");
      var thumbsDown_rate = $(this).children(".dislike")
                                  .children(".thumbs_down")
                                  .attr("rate_val");

    if (thumbsUp_rate == "1" && thumbsDown_rate == "0") {
        var pos_text = $(this)
                            .parent()
                            .parent()
                            .children(".sent-row-col-2")
                            .text(); // .html();

        annotations_array.push({
            positive: pos_text,
        });
    } else if (thumbsUp_rate == "0" && thumbsDown_rate == "1") {
        var neg_text = $(this)
                          .parent()
                          .parent()
                          .children(".sent-row-col-2")
                          .text(); // .html();

        annotations_array.push({
            negative: neg_text,
        });
    }
    i++;
  });

  var parsing_array = {"input_query_array": input_query_array,
                       "annotations_array": annotations_array};

  // compose AJAX transfer
  $.ajax({
    url: "/layer2_search",
    type: "POST",
    contentType: "application/json",
    data: JSON.stringify(parsing_array),
  })
    .done(function (response) {

      // remove thumbs-up and down marks
      $(".thumbs_up").removeClass("like_rate_action");
      $(".thumbs_down").removeClass("dislike_rate_action");

      do_dom_fill(response);

      $("#text_rank_btn").attr("disabled", false);
      console.log("HITL sentence annotations parsed to FLASK -> success");
    })
    .fail(function (jqXHR, textStatus, errorThrown) {
      console.log("fail: ", textStatus, errorThrown);
    });
}

// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================

function IE_TAB_EntitiesClicked() {
  var thisElement = $(event.target);
  var thisElementRating = $(thisElement).attr("rate_val");
  if (thisElementRating == "0") {
    thisElement.removeClass("ENT_neutral_like");
    thisElement.addClass("ENT_pos_like");
    $(thisElement).attr("rate_val", "1");
  } else if (thisElementRating == "1") {
    thisElement.removeClass("ENT_pos_like");
    thisElement.addClass("ENT_neg_like");
    $(thisElement).attr("rate_val", "-1");
  } else {
    thisElement.removeClass("ENT_neg_like");
    thisElement.addClass("ENT_neutral_like");
    $(thisElement).attr("rate_val", "0");
  }
}






// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================
