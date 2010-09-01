$sparqltrees/base_html(
    title={
        Lista ($length(doc)$ poster)
    },
    main={
        <ul class="doclist">
            $doc:{
            <li>
                <h2><a href="$it.rdata_url$">$it.title$</a></h2>
                <h3>$it.serie.label$ $it.serieNummer$</h3>
                <div><code>$it.resource_uri$</code></div>
            </li>
            }$
        </ul>
    }
)$
