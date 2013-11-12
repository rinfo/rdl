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

    addFilterForError(new Filter({errorType:1, pattern:"missing_expected.rq", subPattern:"publ#", title:"Inget v&auml;rde angivet f&ouml;r egenskap"}));
    addFilterForError(new Filter({errorType:2, pattern:"datatype_error.rq", subPattern:"publ#", title:"Egenskap matchar inte angiven datatyp"}));
    addFilterForError(new Filter({errorType:3, pattern:"Postens\\sangivna\\sURI\\smatchar\\sinte\\sdata", subPattern:"", title:"Postens angivna URI matchar inte data"}));

    if (allFilterMatches.length == 0) {
        $('.filterBox').hide();
    }
}

function wrapForSliding() {
    $('table.report').find('tr').find('td').wrapInner('<div style="display: block;" />');
}

function addFilterForError(filterType) {
    var matches = getMatchesForError(filterType);
    var unique_matches = matches.unique();

    var i;
    for (i = 0; i < unique_matches.length; ++i) {
        var match = unique_matches[i];
        var match_count = getCount(matches, match);

        var filterMatch = new FilterMatch({
            pattern:filterType.get('pattern'),
            subPattern:filterType.get('subPattern'),
            errorType:filterType.get('errorType'),
            title:filterType.get('title'),
            match:match,
            isDisplayed:false,
            id: filterType.get('errorType') + '_' + i});

        var div_object = $("<div id='filtrera' style='margin-bottom:10px;'><div>" + filterType.get('title') + " (" + match + ") - " + match_count + "st</div></div>");
        var div_button = $("<button id='filter_" + filterMatch.get('id') + "'>Visa</button>");

        div_button.click(createCallbackForError(filterMatch));
        div_object.append(div_button);
        $('div.filter').append(div_object);

        allFilterMatches.add(filterMatch);
    }
}

function getMatchesForError(filterType) {
    var matches = [];
    var pattern = filterType.get('pattern');

    if(isBlank(filterType.get('subPattern'))) {
        $('table.report').find('tr').each(function () {
            if ($(this).text().match(pattern)) {
                var status = $(this).find('td.status').find('div').text();
                matches.push(status);
            }
        });
    } else {
        $('table.report').find('tr').find('dl').each(function () {
            if ($(this).text().match(pattern)) {
                var subPattern = filterType.get('subPattern') + "(\\S+)";
                if ($(this).text().match(subPattern)) {
                    matches.push(RegExp.$1);
                }
            }
        });
    }

    return matches;
}

function createCallbackForError(filterMatch) {
    return function () {
        filterMatch.toggleVisibility();
    }
}

function hideAll() {
    console.log("hideAll");

    allFilterMatches.each(function (filterMatch) {
        console.log("filterMatch: " + filterMatch.get("title"));
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

function isBlank(str) {
    return (!str || /^\s*$/.test(str));
}

var Filter = Backbone.Model.extend({
        defaults: {
            pattern: "",
            subPattern: "",
            errorType: 0,
            title: ""
        },
        initialize: function(){
            this.logToConsole();
        },
        logToConsole:function () {
            console.log("pattern: " + this.get("pattern") + "," +
                " subPattern: " + this.get("subPattern") +
                " errorType: " + this.get("errorType") +
                ", title: " + this.get("title"));
        }
    });

var FilterMatch = Filter.extend({
        defaults: {
            match: "",
            isDisplayed: false,
            id: ""
        },
        initialize: function(){
            this.logToConsole();
        },
        logToConsole:function () {
            console.log("pattern: " + this.get("pattern") +
                " subPattern: " + this.get("subPattern") +
                ", errorType: " + this.get("errorType") +
                ", title: " + this.get("title") +
                ", match: " + this.get("match") +
                ", id: " + this.get("id") +
                ", isDisplayed: " + this.get("isDisplayed"));
        },
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
            $('table.report').find('tr').each(function () {
                if (hasFilterMatch(this,that)) {
                    $(this).find('div').slideUp(150);
                }
            });
            this.updateButton();
        },
        slideDown:function () {
            var that = this;
            that.set("isDisplayed", true);
            $('table.report').find('tr').each(function () {
                if (hasFilterMatch(this,that)) {
                    $(this).find('div').slideDown(150);
                }
            });
            this.updateButton();
        },
        updateButton:function () {
            var that = this;
            var text = that.get("isDisplayed") ? "D&ouml;lj" : "Visa";
            $('#filter_'+that.get('id')).html(text);
        }
    });

function hasFilterMatch(row, filterMatch) {
    return ($(row).text().match(filterMatch.get("pattern"))
        && $(row).text().match(filterMatch.get("match")));
}

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
