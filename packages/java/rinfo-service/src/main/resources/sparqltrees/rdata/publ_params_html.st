$sparqltrees/base_html(
    title={ Bläddra },
    main={
        <ul>
            $docType:{docType|
            <li>
                <h2>
                    <a href="$basePath$/browse/publ/rpubl:$docType.uri_term$">$docType.label$</a>
                </h2>
                <ul>
                    $docType.publisher:{publ|
                    <li>
                        <ul>
                            <h3>
                                <a href="$basePath$/browse/publ/rpubl:$docType.uri_term$/$publ.uri_term$"
                                   >$publ.name$</a>
                            </h3>
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
