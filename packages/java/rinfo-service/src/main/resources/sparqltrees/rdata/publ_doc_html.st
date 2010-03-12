$sparqltrees/base_html(
    title={ Document },
    main={
        $doc:{
            <h2>$it.title$</h2>
            <h3>$it.resource_uri$</h3>
            <p>$it.type.label$</p>
        }$
    }
)$
