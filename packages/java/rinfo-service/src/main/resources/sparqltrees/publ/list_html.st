$sparqltrees/base_html(
    title={
        Lista ($length(doc)$ poster)
    },
    main={
        <!-- TODO: get directly (from query), not via doc (list could be empty) -->
        <p>
            <a href="$basePath$/browse/publ/$first(doc).type.qname$"
                >$first(doc).type.label$</a> /
            <a href="$basePath$/browse/publ/$first(doc).type.qname$/$first(doc).publisher.uri_term$"
                >$first(doc).publisher.uri_term$</a> /
            <a href="$basePath$/list/publ/$first(doc).type.qname$/$first(doc).publisher.uri_term$/$first(first(doc).daterel).qname$@$first(first(doc).daterel).value.year$"
                >$first(first(doc).daterel).value.year$</a> /
        </p>

        <ul class="doclist">
            $doc:{
            <li>
                $if (it.title)$
                    <h2><a href="$it.view_url$">$it.title$</a></h2>
                    <h3>$it.identifier$</h3>
                $else$
                    <h2><a href="$it.view_url$">$it.identifier$</a></h2>
                $endif$
                <div><code>&lt;$it.resource_uri$&gt;</code></div>
            </li>
            }$
        </ul>
    }
)$
