$sparqltrees/base_html(
    title={ List },
    main={
        <ul>
            $doc:{
            <li>
                <h2>$it.title$</h2>
                <p>
                    <code>$it.resource_uri$</code>
                </p>
            </li>
            }$
        </ul>
    }
)$
