$(document).ready(function(){
  overrideFormSubmit();
});

function overrideFormSubmit() {
  $("#html-form").submit(function() {
    $.ajax({
      type: "POST",
      url: $("#html-form").attr("action"),
      data: $("#html-form").serialize(),
      beforeSend: function() {
        var target = document.getElementById('target');
        var spinner = new Spinner(spinnerOptions()).spin(target);
      },
      success: function(data) {
        $('#target').html(data);
      },
      error: function(xhr, ajaxOptions, thrownError) {
        $('#target').html(xhr.responseText);
      }
    });
    return false;
  });
}

function spinnerOptions() {
  var opts = {
    lines: 13, // The number of lines to draw
    length: 20, // The length of each line
    width: 10, // The line thickness
    radius: 30, // The radius of the inner circle
    corners: 1, // Corner roundness (0..1)
    rotate: 0, // The rotation offset
    direction: 1, // 1: clockwise, -1: counterclockwise
    color: '#000', // #rgb or #rrggbb or array of colors
    speed: 1, // Rounds per second
    trail: 60, // Afterglow percentage
    shadow: false, // Whether to render a shadow
    hwaccel: false, // Whether to use hardware acceleration
    className: 'spinner', // The CSS class to assign to the spinner
    zIndex: 2e9, // The z-index (defaults to 2000000000)
    top: 'auto', // Top position relative to parent in px
    left: 'auto' // Left position relative to parent in px
  };
  return opts;
}