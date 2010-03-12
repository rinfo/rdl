$sparqltrees/base_html(
    title={ Organisationer },
    main={

        $org:{
            <h2>
                <a href="$it.resource_uri$">$it.name$
                $if (it.alt)$<span>($it.alt$)</span>$endif$</a>
            </h2>
            <div>$it.resource_uri$</div>
            $if (it.comment)$ <p><em>$it.comment$</em></p> $endif$
            $if (it.serie)$
                <h3>Serier</h3>
                <ul>
                    $it.serie:{
                    <li>
                        <p>
                            <a href="$it.resource_uri$">$it.shortname$</a>
                            <em>$it.name$</em>
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
                $it.datasource:{
                <dl>
                    <dt>URI</dt> <dd>$it.resource_uri$</dd>
                    <dt>current</dt> <dd>$it.current.resource_uri$</dd>
                </dl>
                }$
            $endif$
        }$

    }
)$
