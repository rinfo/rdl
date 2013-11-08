$(document).ready(function () {
    overrideFormSubmit();
});

function overrideFormSubmit() {
    $("#html-form").submit(function () {
        $.ajax({
            type:"POST",
            url:$("#html-form").attr("action"),
            data:$("#html-form").serialize(),
            beforeSend:function () {
                var target = document.getElementById('target');
                var spinner = new Spinner(spinnerOptions).spin(target);
            },
            success:function (data) {
                $('#target').html(data);
                addErrorFilters();
            },
            error:function (xhr, ajaxOptions, thrownError) {
                $('#target').html(xhr.responseText);
            }
        });
        return false;
    });
}

function addErrorFilters() {
    wrapForSliding();

    var filter_div = $("<div class='filterBox'><h3>Filtrera</h3><div class='filter'></div></div>");
    $("h4:contains('Poster')").before(filter_div);

    allFilterMatches.reset();

    var filter1 = new Filter({errorType:"missing_expected.rq", title:"Inget v&auml;rde angivet f&ouml;r egenskap"});
    var filter2 = new Filter({errorType:"datatype_error.rq", title:"Egenskap matchar inte angiven datatyp"});

    addFilterForError(filter1);
    addFilterForError(filter2);

    if (allFilterMatches.length == 0) {
        $('.filterBox').hide();
    }
}

function wrapForSliding() {
    $('table.report').find('tr').find('td').wrapInner('<div style="display: block;" />');
}

function addFilterForError(filterType) {
    var matches = getMatchesForError(filterType.get('errorType'));
    var unique_matches = matches.unique();

    var i;
    for (i = 0; i < unique_matches.length; ++i) {
        var match = unique_matches[i];
        var match_count = getCount(matches, match);

        var filterMatch = new FilterMatch({errorType:filterType.get('errorType'),
            title:filterType.get('title'),
            match:match});

        var div_object = $("<div id='filtrera' style='margin-bottom:10px;'><div>" + match_count + "st: " + filterType.get('title') + " (" + match + ")</div></div>");
        var div_button = $("<button>Visa/D&ouml;lj</button>");

        div_button.click(createCallbackForError(filterMatch));
        div_object.append(div_button);
        $('div.filter').append(div_object);

        allFilterMatches.add(filterMatch);
    }
}

function getMatchesForError(errorType) {
    var matches = [];

    $("dl:contains(" + errorType + ")").each(function () {
        if ($(this).text().match(/publ#(\S+)/)) {
            matches.push(RegExp.$1);
        }
    })

    return matches;
}

function createCallbackForError(filterMatch) {
    return function () {
        filterMatch.toggleVisibility();
        filterMatch.logToConsole();
    }
}

function hideAll() {
    console.log("hideAll");
    console.log(allFilterMatches.models);

    allFilterMatches.each(function (filterMatch) {
        filterMatch.slideUp();
    });
}

Array.prototype.unique = function () {
    var arrVal = this;
    var uniqueArr = [];
    for (var i = arrVal.length; i--;) {
        var val = arrVal[i];
        if ($.inArray(val, uniqueArr) === -1) {
            uniqueArr.unshift(val);
        }
    }
    return uniqueArr;
}

function getCount(arr, val) {
    var ob = {};
    var len = arr.length;
    for (var k = 0; k < len; k++) {
        if (ob.hasOwnProperty(arr[k])) {
            ob[arr[k]]++;
            continue;
        }
        ob[arr[k]] = 1;
    }
    return ob[val];
}

var Filter = Backbone.Model.extend({
    errorType:"",
    title:"",
    logToConsole:function () {
        console.log("errorType: " + this.get("errorType") + ", title: " + this.get("title"));
    }
});

var FilterMatch = Filter.extend({
    match:"",
    isDisplayed:true,

    toggleVisibility:function () {
        if (this.get("isDisplayed")) {
            this.slideUp();
        } else {
            hideAll();
            this.slideDown();
        }
    },
    slideUp:function () {
        var that = this;
        that.set("isDisplayed", false);
        $("tr:has(td:contains(" + that.get("errorType") + "))").each(function () {
            if ($(this).text().match(that.get("match"))) {
                $(this).find('div').slideUp(150);
            }
        });
    },
    slideDown:function () {
        var that = this;
        that.set("isDisplayed", true);
        $("tr:has(td:contains(" + that.get("errorType") + "))").each(function () {
            if ($(this).text().match(that.get("match"))) {
                $(this).find('div').slideDown(150);
            }
        });
    },
    logToConsole:function () {
        console.log("errorType: " + this.get("errorType") + ", title: " + this.get("title") + ", match: " + this.get("match") + ", isDisplayed: " + this.get("isDisplayed"));
    }
});

var FilterMatchCollection = Backbone.Collection.extend({
    model:FilterMatch
});

var allFilterMatches = new FilterMatchCollection();

var spinnerOptions = {
    lines:13, // The number of lines to draw
    length:20, // The length of each line
    width:10, // The line thickness
    radius:30, // The radius of the inner circle
    corners:1, // Corner roundness (0..1)
    rotate:0, // The rotation offset
    direction:1, // 1: clockwise, -1: counterclockwise
    color:'#000', // #rgb or #rrggbb or array of colors
    speed:1, // Rounds per second
    trail:60, // Afterglow percentage
    shadow:false, // Whether to render a shadow
    hwaccel:false, // Whether to use hardware acceleration
    className:'spinner', // The CSS class to assign to the spinner
    zIndex:2e9, // The z-index (defaults to 2000000000)
    top:'auto', // Top position relative to parent in px
    left:'auto' // Left position relative to parent in px
};
