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
                            $publ.eventItem:{
                            <li>
                                <a href="$basePath$/list/publ/$docType.qname$/$publ.uri_term$/$it.property.qname$@$it.year$">$it.year$</a>
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
