
$(document).ready(function () {
  console.log("JS Started working ... ");
  // slider defaults
  setDefaultFirst();
  setDefaultsecond();
  // clear the button
});


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
// function togggleAlert(msg) {
//   let message = msg;
//   if (message) {
//     $("#message-space").html(`<div class="col-md-4 col-md-offset-4" >
//           <div class="alert alert-success alert-dismissible " role="alert" >
//               <p id="message-space-msg">${message}</p>
//               <button type="button" class="close" data-dismiss="alert" aria-label="Close">
//                   <span aria-hidden="true">&times;</span>
//               </button>
//           </div>
//       </div>`);
//   }
// }

function togggleAlert(msg) {
  let message = msg;
  if (message) {
    $("#message-space").html(`<div class="col-md-4 col-md-offset-4" >
          <div class="alert alert-success alert-dismissible " role="alert" >
          <a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a>
              <p id="message-space-msg">${message}</p>

          </div>
      </div>`);
  }
}

// ============================= keyterms filled by ajax ======================
function fillTerms(terms) {
  let terms_html = "";
  terms.forEach(
    (term) =>
      (terms_html =
        terms_html +
        `<span class="badge badge-secondary" style="margin: 0.1rem;" > <span style="padding: 1rem;font-size: small;">${term}</span> </span>`)
  );
  $("#terms_QE_div").html(terms_html);
}



// ====================== fill side buttons =============
function fillsideButtons(query_results) {
  let sidebtns = "";
  query_results.forEach((pair) => {
    sidebtns =
    sidebtns +
    `<button data-toggle="tooltip" data-placement="right" title="${pair["docTextSentences"][0]}"
      onclick='scrollToDiv("${pair["docID"]}_tab1","${pair["docID"]}_tab2")' role="button" type="button"
      class="btn btn-primary" style="padding-left:1px; padding-right:1px; width: 200px; height: 60px; vertical-align: middle;">

      <span style="white-space: normal; font-size: 75%; vertical-align: middle; display: inline-block;">
          ${pair["docTextSentences"][0]}
      </span>

  </button>
      <br>`;

      // style="padding-left:1px; font-size: 75%; overflow: hidden; text-overflow: ellipsis; vertical-align: top; word-wrap: break-word;">
      // white-space: nowrap;

    //   sidebtns +
    //   `<button data-toggle="tooltip" data-placement="right" title="${pair["docTextSentences"][0]}"
    //     onclick='scrollToDiv("${pair["docID"]}_tab1","${pair["docID"]}_tab2")' role="button" type="button"
    //     class="btn btn-primary" style="max-width:200px;>
    //
    //     <!-- <i style="vertical-align: middle; text-align: left;" data-feather="file-text"></i>    -->
    //     <!-- <span class="glyphicon glyphicon-list-alt" style="font-size:x-small;"></span>    -->
    //     <!-- [ENG] ${pair["docTextSentences"][0]}    -->
    //
    //     <span style="text-align: left;overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
    //         ${pair["rank"]} - ${pair["docTextSentences"][0]}
    //     </span>
    //
    // </button>
    //     <br>`;
  });


  // query_results.forEach((pair) => {
  //   sidebtns =
  //     sidebtns +
  //     `<button data-toggle="tooltip" data-placement="right" title="${pair["docTextSentences"][0]}"
  //       onclick='scrollToDiv("${pair["docID"]}_tab1","${pair["docID"]}_tab2")' role="button" type="button"
  //       class="btn btn-primary" >
  //       <i style="vertical-align: middle;" data-feather="file-text"></i>
  //       <!-- <span class="glyphicon glyphicon-list-alt" style="font-size:x-small;"></span>    -->
  //       [ENG] Document - ${pair["rank"]}
  //   </button>
  //       <br>`;
  // });

  $("#search_ids").html(sidebtns);
}


// =============================== tab1
function filltextRanks(query_results) {
  let textranksdoc = "";
  let trows = [];
  query_results.forEach((pair) => getsents(pair["docTextSentences"]));
  function getsents(sentences) {
    let rows = "";
    // for (let sent of sentences) {
    //   rows =
    //     rows +
    //     `<tr>
    //   <th class="sent-row-col-1" width="10%">
    //
    //     <div class="rating">
    //
    //         <div class="like grow" onclick="thumbsUpClicked()" style="width:10px; display:inline;">
    //           <!-- <i class="fa fa-thumbs-up fa-1x" aria-hidden="true" rate_val="0"></i> -->
    //           <i class="thumbs_up" style="vertical-align: middle;" data-feather="thumbs-up" aria-hidden="true" rate_val="0"></i>
    //         </div>
    //         <div class="dislike grow" onclick="thumbsDownClicked()" style="width:10px; display:inline;">
    //           <!-- <i class="fa fa-thumbs-down fa-1x" aria-hidden="true" rate_val="0"></i> -->
    //           <i class="thumbs_down" style="vertical-align: middle;" data-feather="thumbs-down" aria-hidden="true" rate_val="0"></i>
    //         </div>
    //
    //     </div>
    //
    //   </th>
    //   <th class="sent-row-col-2" width="90%" style="font-weight: normal;overflow-wrap:break-word;">${sent}</th>
    // </tr>`;
    // }
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
      ` <div id='${pair["docID"]}_tab1'>
      <p  ><strong>Document ID : ${pair["docID"]}</strong></p>
      <table class="table table-condensed table-striped" id="sent_hitl_table">
      <tr>
          <th width="5%">Labels</th>
          <th width="95%">Document Sentences</th>
      </tr>
      ${trows[index]}
      </table>
  </div>`;
  }

  query_results.forEach(getpairs);
  $("#textrank").html(textranksdoc);
}



// =============================== tab2
function fillHighlighted(query_results, highlighted_text_list) {
  let head = `<tr >
    <th width="5%">Rank</th>
    <th width="95%">Document Text</th>
</tr>`;
  let rows = "";
  // query_results.forEach((pair) => {
  //   rows =
  //     rows +
  //     `
  //       <tr id="${pair["docID"]}_tab2">
  //           <th width="5%"> ${pair["rank"]} </th>
  //           <th width="95%" style="font-weight: normal;overflow-wrap:break-word;">
  //             <p><strong>Document ID :${pair["docID"]}</strong></p>
  //             ${highlighted_text_list[pair["docID"]]} </th>
  //       </tr>`;
  // });
  query_results.forEach((pair) => {
    rows =
      rows +
      `
        <tr id="${pair["docID"]}_tab2">
            <th width="5%"> ${pair["rank"]} </th>
            <th width="95%" style="font-weight: normal;">
              <p><strong>Document ID :${pair["docID"]}</strong></p>
              ${highlighted_text_list[pair["docID"]]} </th>
        </tr>`;
  });

  // $("#entityrankth").html(rows);
  $("#entityrankth").html(head + rows);
}

//   ============================
function do_dom_fill(response) {
  console.log("let's fill dom");
  togggleAlert(response.message_board);
  fillTerms(response.terms_QE_list);
  fillsideButtons(response.query_results);
  filltextRanks(response.query_results);
  fillHighlighted(response.query_results, response.highlighted_text_list);

  feather.replace();
}




// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================




function InputQueryButtonClicked() {
    console.log("clicked search ...");
    $("#search_button").attr("disabled", true);

    // $('#search_button').disabled = true;

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

    console.log(firstRangeInput, secondRangeInput);
    // console.log(input_query_array);

    $.ajax({
      url: "/layer1_search",
      type: "POST",
      contentType: "application/json",
      data: JSON.stringify(input_query_array),
    })
      .done(function (layer1_results) {

        do_dom_fill_layer1_results(layer1_results);

        // $('#search_button').disabled = false;
        $("#search_button").attr("disabled", false);
        console.log("Layer-1 results retrieved !");
      })
      .fail(function (jqXHR, textStatus, errorThrown) {
        console.log("fail: ", textStatus, errorThrown);
      });
}

//   ============================
function do_dom_fill_layer1_results(response) {
  console.log("let's fill dom - for layer-1 search results");
  togggleAlert(response.message_board);
  fillTerms(response.terms_QE_list);
  fillsideButtons(response.query_results);
  filltextRanks(response.query_results);
  fillHighlighted(response.query_results, response.highlighted_text_list);

  feather.replace();
}



// =============================================================================================================================================
// =============================================================================================================================================
// =============================================================================================================================================



function EntityTAB_EntitiesClicked() { // ENTClicked
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

function handleEntityTAB_Reranking() { // handleHITLAnnotationsENT
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

  // console.log(input_query_array);

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



function thumbsUpClicked() { // likeClicked
  var thisElement = $(event.target);
  if (thisElement.hasClass("like_rate_action")) {
    thisElement.removeClass("like_rate_action");
    $(thisElement).attr("rate_val", "0");
  } else {
    thisElement.addClass("like_rate_action");
    $(thisElement).attr("rate_val", "1");
  }
}

function thumbsDownClicked() { // dislikeClicked
  var thisElement = $(event.target);
  if (thisElement.hasClass("dislike_rate_action")) {
    thisElement.removeClass("dislike_rate_action");
    $(thisElement).attr("rate_val", "0");
  } else {
    thisElement.addClass("dislike_rate_action");
    $(thisElement).attr("rate_val", "1");
  }
}

// ===========================
function handleTextTAB_Reranking() { // handleHITLAnnotations
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
