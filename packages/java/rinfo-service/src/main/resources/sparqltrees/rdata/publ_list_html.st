$sparqltrees/base_html(
    title={ List },
    main={
        <ul>
            $doc:{
            <li>
                <h2><a href="$it.rdata_url$">$it.title$</a></h2>
                <p>
                    <code>$it.resource_uri$</code>
                </p>
            </li>
            }$
        </ul>
    }
)$
