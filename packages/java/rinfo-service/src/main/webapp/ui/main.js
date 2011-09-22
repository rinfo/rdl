
var base = "";

$(function () {

  $.getJSON(base + '/-/stats', function (stats) {
    $.each(stats.slices, function () {
      var $select = $('#' + this.dimension);
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

  $('#queryForm').submit(function () {
    var $self = $(this);
    var serviceRef = $self.attr('action') +'?'+ $self.serialize();
    window.location.hash = serviceRef;
    queryService(serviceRef);
    return false;
  });

  $('#resultInfo .pagination a:link').live('click', function () {
    var $self = $(this);
    var ref = $self.attr('href');
    var ref = ref.substring(1);
    if (ref) {
      window.location.hash = ref;
      queryService(ref);
    }
    return false;
  });

  var serviceRef = window.location.hash.substring(1);
  queryService(serviceRef);

});

function queryService(serviceRef) {
  $.getJSON(base + serviceRef, function (results) {

    // TODO: compute pagination
    //results.startIndex
    //results.totalResults
    //results.itemsPerPage

    var endIndex = results.startIndex + results.itemsPerPage;
    if (endIndex > results.totalResults) {
      endIndex = results.totalResults;
    }

    $('#resultInfoTemplate').tmpl({
      queryStr: serviceRef,
      start: results.startIndex + 1,
      end: endIndex,
      totalResults: results.totalResults,
      results: results
    }).appendTo($('#resultInfo').empty());

    var $tbody = $('#resultRows')
    $tbody.empty();
    $('#resultRowTemplate').tmpl(results.items).appendTo($tbody);

  });
}

