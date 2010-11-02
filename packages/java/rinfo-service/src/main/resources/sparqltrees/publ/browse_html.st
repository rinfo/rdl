$sparqltrees/base_html(
    title={ Bläddra },
    main={
        <ul>
            $docType:{docType|
            <li>
                <h2>
                    <a href="$basePath$/browse/publ/$docType.qname$">$docType.label$</a>
                </h2>
                <ul>
                    $docType.publisher:{publ|
                    <li>
                        <ul>
                            <h3>
                                <a href="$basePath$/browse/publ/$docType.qname$/$publ.uri_term$"
                                   >$publ.name$</a>
                            </h3>
                            $publ.time:{
                            <li>
                            <a href="$basePath$/list/publ/$docType.qname$/$publ.uri_term$/$it.prop.qname$@$it.year$">$it.year$</a>
                            ($it.prop.label$)
                            </li>
                            }$
                        </ul>
                    </li>
                    }$
                </ul>
            </li>
            }$
        </ul>
    }
)$
