
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
    renderStats(stats);
  });
}

/* render */

function renderStats(stats, dynamicSelects) {
  var $optFields = $('#optFields').empty();
  $.each(stats.slices, function () {
    var $select = $('#' + this.dimension);
    if (!$select[0]) {
      if (!dynamicSelects) return;
      var $selectBox = $('#selectTemplate').tmpl({
        id: this.dimension,
        label: this.dimension,
        name: this.dimension + ((this.observations && this.observations[0].ref)? '.iri' : '')
      });
      $optFields.append($selectBox);
      $select = $('select', $selectBox);
    } else {
      if (dynamicSelects) {
        $select.empty().addClass('narrowed');
      } else if ($select.is('.narrowed')) {
        $select.empty().removeClass('narrowed');
      }
    }
    $.each(this.observations, function () {
      var value, label;
      if (this.ref) {
        value = '*/' + this.ref.substring(this.ref.lastIndexOf('/') + 1);
        label = this.ref.substring(this.ref.lastIndexOf('/') + 1);
      } else if (this.term) {
        value = this.term;
        label = this.term;
      } else if (this.year) {
        // TODO: add proper 'min-' and 'maxEx-' + this.dimension for date range
        value = '['+ this.year +'-01-01 TO '+ this.year +'-12-31]';
        label = this.year;
      }
      if (!value)
        return;
      $select.append('<option value="'+ value +'">'+ label +' ('+ this.count +')'+'</option>');
    });
  });
}

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
  if (results.statistics) {
    renderStats(results.statistics, true);
  } else {
    loadStats();
  }
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
  return iri.replace(/^https?:\/\/[^\/]+([^#]+?)(\/data\.json)?(#.+)?$/, "$1/data.json$3");
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

