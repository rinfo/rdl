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
    removeHeader();
    wrapForSliding();

    var filter_div = $("<div class='filterBox'><h3>Filtrera</h3><div class='filter'></div></div>");
    $("h4:contains('Poster')").before(filter_div);

    allFilterMatches.reset();

    addFilterForError(new Filter({errorType:1, pattern:"Saknar obligatoriskt värde för egenskap", subPattern:"publ#"}));
    addFilterForError(new Filter({errorType:2, pattern:"Värdet matchar inte datatyp för egenskap", subPattern:"publ#"}));
    addFilterForError(new Filter({errorType:3, pattern:"Angiven URI matchar inte den URI som beräknats utifrån egenskaper i dokumentet", subPattern:""}));
    addFilterForError(new Filter({errorType:4, pattern:"Saknar svenskt språkattribut (xml:lang) för egenskap", subPattern:"terms/"}));
    addFilterForError(new Filter({errorType:5, pattern:"Kan inte tolka URI:n", subPattern:""}));

    removeURIFromCodeElements();

    createFilterForPostsWithoutErrors();
    createFilterForUnmatchedRows();

    if (allFilterMatches.length == 0) {
        $('.filterBox').hide();
    } else if (allFilterMatches.length > 1) {
        var reset_button = $("<div id='reset'><button onclick='resetAll()'>&Aring;terst&auml;ll</button></div>");
        $('div.filter').append(reset_button);
    }

    resetAll();
}

function removeHeader() {
    $('#target').find('h1').first().remove();
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
            match:match,
            isDisplayed:false,
            rows:[],
            id:filterType.get('errorType') + '_' + i});

        var div_object = $("<div id='filtrera'><div>" + htmlEscape(filterType.get('pattern')) + ": " + match + " - " + match_count + "st</div></div>");
        var div_button = $("<button id='filter_" + filterMatch.get('id') + "'>Visa</button>");

        div_button.click(createCallbackForError(filterMatch));
        div_object.append(div_button);
        $('div.filter').append(div_object);

        filterMatch.setupRows();

        allFilterMatches.add(filterMatch);
    }
}

function getMatchesForError(filterType) {
    var matches = [];
    var pattern = encodeForRegexPattern(filterType.get('pattern'));

    if (isBlank(filterType.get('subPattern'))) {
        $('table.report').find('tr').each(function () {
            if ($(this).text().match(pattern)) {
                matches.push("");
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
    logToConsole("hideAll");

    allFilterMatches.each(function (filterMatch) {
        logToConsole("filterMatch: " + filterMatch.get('rows'));
        filterMatch.slideUp();
    });
}

function createFilterForPostsWithoutErrors() {
    var rowsWithoutErrors = getRowsWithoutErrors();

    if(rowsWithoutErrors.length > 0) {

        var filterMatch = new FilterMatch({
            pattern:"Korrekta poster",
            subPattern:"",
            errorType:0,
            match:"",
            isDisplayed:false,
            rows:rowsWithoutErrors,
            id:'rowsWithoutErrors'});

        var div_object = $("<div id='filtrera'><div>" + htmlEscape(filterMatch.get('pattern')) + " - " + rowsWithoutErrors.length + "st</div></div>");
        var div_button = $("<button id='filter_" + filterMatch.get('id') + "'>Visa</button>");

        div_button.click(createCallbackForError(filterMatch));
        div_object.append(div_button);
        $('div.filter').append(div_object);

        allFilterMatches.add(filterMatch);
    }
}

function createFilterForUnmatchedRows() {
    var unmatchedRows = getUnmatchedRows();

    if(unmatchedRows.length > 0) {

        var filterMatch = new FilterMatch({
            pattern:"Övriga",
            subPattern:"",
            errorType:0,
            match:"",
            isDisplayed:false,
            rows:unmatchedRows,
            id:'unmatchedRows'});

        var div_object = $("<div id='filtrera'><div>" + htmlEscape(filterMatch.get('pattern')) + " - " + unmatchedRows.length + "st</div></div>");
        var div_button = $("<button id='filter_" + filterMatch.get('id') + "'>Visa</button>");

        div_button.click(createCallbackForError(filterMatch));
        div_object.append(div_button);
        $('div.filter').append(div_object);

        allFilterMatches.add(filterMatch);
    }
}

function getRowsWithoutErrors() {

    var rowsWithoutErrors = [];

    $('table.report').find('tr').find('.status').find('div').each(function () {
        var status = $(this).text();
        if (status == "OK") {
            var row = $(this).closest('tr').find('.position').find('div').text();
            rowsWithoutErrors.push(row);
        }
    });

    logToConsole("rowsWithoutErrors: " + rowsWithoutErrors);

    return rowsWithoutErrors;
}

function getUnmatchedRows() {

    var unmatchedRows = [];

    $('table.report').find('tr').find('.position').find('div').each(function () {
        var row = $(this).text();
        var isMatched = false;

        allFilterMatches.each(function (filterMatch) {
            var matchedRows = filterMatch.get('rows');

            if($.inArray(row, matchedRows) > -1) {
                logToConsole("row: " + row + ", matchedRows: " + matchedRows);
                isMatched = true;
            }
        });

        if(!isMatched) {
            unmatchedRows.push(row);
        }
    });

    logToConsole("unmatchedRows: " + unmatchedRows);

    return unmatchedRows;
}

function resetAll() {
    allFilterMatches.each(function (filterMatch) {
        filterMatch.slideUp();
    });
}

function removeURIFromCodeElements() {
    logToConsole("removeURIFromMessages");

    $('table.report').find('tr').find('code').text(function (i, t) {
        return t.replace("http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#","")
                .replace("http://purl.org/dc/terms/","");
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
    defaults:{
        pattern:"",
        subPattern:"",
        errorType:0
    },
    initialize:function () {
        this.logToConsole();
    },
    logToConsole:function () {
        logToConsole("pattern: " + this.get("pattern") + "," +
            " subPattern: " + this.get("subPattern") +
            " errorType: " + this.get("errorType"));
    }
});

var FilterMatch = Filter.extend({
    defaults:{
        match:"",
        isDisplayed:false,
        id:"",
        rows:[]
    },
    initialize:function () {
        this.logToConsole();
    },
    logToConsole:function () {
        logToConsole("pattern: " + this.get("pattern") +
            " subPattern: " + this.get("subPattern") +
            ", errorType: " + this.get("errorType") +
            ", match: " + this.get("match") +
            ", id: " + this.get("id") +
            ", rows: " + this.get("rows") +
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
        this.set("isDisplayed", false);
        this._slide("up");
        this._updateButton();
    },
    slideDown:function () {
        this.set("isDisplayed", true);
        this._slide("down");
        this._updateButton();
    },
    _updateButton:function () {
        var text = this.get("isDisplayed") ? "D&ouml;lj" : "Visa";
        $('#filter_' + this.get('id')).html(text);
    },
    _slide:function (direction) {
        var that = this;
        $('table.report').find('tr').find('.position').find('div').each(function () {
            var row = $(this).text();
            if($.inArray(row, that.get('rows')) > -1) {
                if (direction == "up") {
                    $(this).closest('tr').find('div').slideUp(150);
                } else {
                    $(this).closest('tr').find('div').slideDown(150);
                }
            }
        });
    },
    setupRows:function () {
        logToConsole("setupRow");
        var that = this;
        $('table.report').find('tr').each(function () {
            if (that._hasFilterMatch(this)) {
                var row = $(this).find('.position').find('div').text();
                logToConsole("add row: " + row);
                var newRows = _.clone(that.get('rows'));
                newRows.push(row);
                that.set('rows', newRows);
            }
        });
    },
    _hasFilterMatch:function (row) {
        var that = this;
        return $(row).text().match(encodeForRegexPattern(that.get("pattern")))
            && $(row).text().match(encodeForRegexPattern(that.get("match")));
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

//console.log might not be defined for i.e. IE8
var alertFallback = false;
if (typeof console === "undefined" || typeof console.log === "undefined") {
    console = {};
    if (alertFallback) {
        console.log = function (msg) {
            alert(msg);
        };
    } else {
        console.log = function () {
        };
    }
}

var logEnabled = false;

function logToConsole(message) {
    if (logEnabled) {
        console.log(message);
    }
}

function htmlEscape(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/å/g, '&aring;')
        .replace(/ä/g, '&auml;')
        .replace(/ö/g, '&ouml;')
        .replace(/Å/g, '&Aring;')
        .replace(/Ä/g, '&Auml;')
        .replace(/Ö/g, '&Ouml;');
}

function encodeForRegexPattern(str) {
    return String(str)
        .replace(/\s/g, '\\s')
        .replace(/\(/g, '\\(')
        .replace(/\)/g, '\\)')
        .replace(/å/g, '.')
        .replace(/ä/g, '.')
        .replace(/ö/g, '.')
        .replace(/Å/g, '.')
        .replace(/Ä/g, '.')
        .replace(/Ö/g, '.');
}

function isBlank(str) {
    return (!str || /^\s*$/.test(str));
}
