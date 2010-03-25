$sparqltrees/base_html(
    title={ Organisationer },
    main={

        $org:{
            <h2>
                <a href="$it.resource_uri$">$it.name$
                $if (it.alt)$<span>($it.alt$)</span>$endif$</a>
            </h2>
            $if (it.comment)$ <p><em>$it.comment$</em></p> $endif$
            $if (it.serie)$
                <h3>Serier</h3>
                <ul>
                    $it.serie:{
                    <li>
                        <p>
                            <a href="$it.resource_uri$">$it.shortname$</a>
                            $it.someLabel$ ($it.type.label$)
                        </p>
                        $if (it.comment)$
                        <p>(<em>$it.comment$</em>)</p>
                        $endif$
                    </li>
                    }$
                </ul>
            $endif$
            $if (it.datasource)$
            <h3>Datakällor</h3>
                <ul>
                $it.datasource:{
                    <li>
                        <a href="$it.current.resource_uri$"
                            ><code>$it.resource_uri$</code></a>
                    </li>
                }$
                </ul>
            $endif$
        }$

    }
)$
