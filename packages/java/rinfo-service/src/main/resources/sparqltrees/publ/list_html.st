$sparqltrees/base_html(
    title={
        Lista ($length(doc)$ poster)
    },
    main={
        <ul class="doclist">
            $doc:{
            <li>
                <h2><a href="$it.view_url$">$it.title$</a></h2>
                <h3>$it.identifier$</h3>
                <div><code>&lt;$it.resource_uri$&gt;</code></div>
            </li>
            }$
        </ul>
    }
)$
