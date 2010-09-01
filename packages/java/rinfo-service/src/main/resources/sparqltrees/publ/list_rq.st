$sparqltrees/publ/base_rq(
    query_body={

        ?doc ?doc__daterel ?doc__daterel__value .
        ?doc__daterel__eventItem
            event:product ?doc;
            scv:dimension ?doc__daterel;
            tl:atYear ?doc__daterel__eventItem__1_atYear .
        FILTER(
            $filter_parts:{
                $if (it.typeSelector)$
                    ?doc__1_type = $it.typeSelector$
                $elseif (it.dateSelector)$
                    (?doc__daterel = $it.dateSelector$ &&
                    STR(?doc__daterel__eventItem__1_atYear) = "$it.value$")
                $elseif (it.publisherSelector)$
                    REGEX(STR(?doc__1_publisher), "[#/]$it.value$\$")
                $endif$
            }; separator="&&"$
        )

    },
    order_by={ DESC(?doc) }
)$
