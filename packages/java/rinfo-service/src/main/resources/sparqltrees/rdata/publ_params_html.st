$sparqltrees/base_html(
    title={ Bläddra },
    main={
        <ul>
            $docType:{docType|
            <li>
                <h2>$docType.label$</h2>
                <ul>
                    $docType.publisher:{publ|
                    <li>
                        <ul>
                            <h3>$publ.name$</h3>
                            $publ.eventItem:{
                            <li>
                                <a href="$basePath$/list/publ?a=rpubl:$docType.uri_term$&amp;dct:publisher=$publ.uri_term$&amp;rpubl:$it.property.uri_term$=$it.year$">$it.year$</a>
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
