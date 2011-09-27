
var base = "";

/* main */

$(function () {

  loadStats();

  $('#queryForm').submit(function () {
    var $self = $(this);
    var serviceRef = $self.attr('action') +'?'+ $self.serialize();
    window.location.hash = serviceRef;
    queryService(serviceRef);
    return false;
  });

  $('#resultsView a:link, a.svc:link, a.sort:link').live('click', function () {
    var serviceRef = $(this).attr('href').substring(1);
    if (serviceRef) {
      window.location.hash = serviceRef;
      queryService(serviceRef);
    }
    return false;
  });

  var serviceRef = window.location.hash.substring(1);
  queryService(serviceRef);

});

/* lookup */

function queryService(serviceRef) {
  if (!serviceRef) {
    return;
  }
  loader.start($('#content'));
  $.getJSON(base + serviceRef, function (data) {
    loader.stop($('#content'));
    (data.itemsPerPage? renderResults : renderDocument)(serviceRef, data);
  }).error(function (response) {
    loader.stop($('#content'));
    renderError(serviceRef, response);
  });
}

function loadStats() {
  loader.start($('#queryBox'));
  $.getJSON(base + '/-/stats', function (stats) {
    loader.stop($('#queryBox'));
    $.each(stats.slices, function () {
      var $select = $('#' + this.dimension);
      $select.empty();
      $.each(this.observations, function () {
        if (!this.term) return; // TODO: date stats
        var key = this.term;
        if (this.term.indexOf('/') > -1) {
          key = '*' + this.term.substring(this.term.lastIndexOf('/') + 1);
        }
        var label = this.term.substring(this.term.lastIndexOf('/') + 1);
        $select.append('<option value="'+ key +'">'+ label +' ('+ this.count +')'+'</option>');
      });
    });
  });
}

/* render */

function renderResults(serviceRef, results) {
  var endIndex = results.startIndex + results.itemsPerPage;
  if (endIndex > results.totalResults) {
    endIndex = results.totalResults;
  }
  $('#errorInfo').empty();
  $('#documentView').empty();
  $('#resultsTemplate').tmpl({
    queryStr: serviceRef,
    start: results.startIndex + 1,
    end: endIndex,
    totalResults: results.totalResults,
    results: results
  }).appendTo($('#resultsView').removeClass('folded').empty());
}

function renderDocument(serviceRef, doc) {
  $('#errorInfo').empty();
  $('#resultsView:has(*)').addClass('folded');
  $('#documentTemplate').tmpl({
    heading: doc.identifier || doc.name || doc.altLabel || doc.label,
    obj: doc
  }).appendTo($('#documentView').empty());
}

function renderError(serviceRef, response) {
  $('#documentView').empty();
  $('#errorTemplate').tmpl({
    serviceRef: serviceRef,
    response: response
  }).appendTo($('#errorInfo').empty());
}

/* utils */

function toServiceRef(iri) {
  return iri.replace(/^https?:\/\/[^\/]+(.+?)(\/data\.json)?$/, "$1") + "/data.json";
}

function sortLink(serviceRef, sortTerm) {
  var match = serviceRef.match(/\&_sort=([^&]+)/);
  var currSort = match? match[1] : "";
  var doSort = sortTerm;
  if (currSort === '-'+sortTerm) {
    doSort = "";
  } else if (currSort === sortTerm) {
    doSort = '-' + sortTerm;
  }
  return serviceRef.replace(/\&_sort=[^&]+/, "") +
      (doSort? '&_sort=' + doSort : "");
}

/**
 * Throbbing loader indicator.
 */
var loader = new function () {

  var dur = this.dur = 1000;

  this.start = function (o) {
    return fadeOut.apply(o.addClass('loading')[0]);
  }
  this.stop = function (o) {
    return o.stop(true).removeClass('loading').css('opacity', 1);
  }

  function fadeOut() {
    $(this).animate({'opacity': 0.2}, {queue: true, duration: dur, complete: fadeIn});
  }
  function fadeIn() {
    $(this).animate({'opacity': 1}, {queue: true, duration: dur, complete: fadeOut});
  }

};

