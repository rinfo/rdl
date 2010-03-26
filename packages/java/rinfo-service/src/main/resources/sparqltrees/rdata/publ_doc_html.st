$sparqltrees/base_html(
    title={ $first(doc).title$ },
    main={
        $doc:{
        <div class="doc">
            <h2>$it.serie.label$ $it.serieNummer$</h2>
            <p><em>$it.type.label$</em></p>
            <p><code>$it.resource_uri$</code></p>
            <table class="docmeta">
                <tr>
                    <th>Utgivare</th>
                    <td>$it.publisher.name$</td>
                </tr>
                $it.daterel:{
                    <tr>
                        <th>
                            $if (it.label)$
                                $it.label$
                            $else$
                                <code>$it.uri_term$</code>
                            $endif$
                        </th>
                        <td>$it.value$</td>
                    </tr>
                }$
                <!-- TODO:
                <tr>
                    <th>Källa</th>
                    <td><a href="/">Regeringskansliets rättsdatabaser</a></td>
                </tr>
                -->
            </table>

            $if (it.entry.repr)$
                <ul class="representations">
                    $it.entry.repr:{
                        <li>
                            <a href="$it.resource_uri$">$it.mediaType$</a>
                        </li>
                    }$
                </ul>
            $endif$
            <!--
            $if (it.pdf_url)$
                <p class="pdflink">
                    <a href="$it.pdf_url$">Ladda ner PDF-version av SFS 1999:175</a>
                </p>
            $endif$
            -->


            <!--
            <h2>Ändringar och övergångsbestämmelser</h2>
            -->
            $if (it.rel)$
                <h2>Relationer</h2>
                <ul class="relations">
                    $it.rel:{
                        <li>
                            <span class="rel">$it.label$</span>:
                            $it.subject:{
                                <a href="$it.rdata_url$">$if (it.label)$
                                        $it.label$
                                    $else$
                                        <code>$it.resource_uri$</code>
                                    $endif$
                                    $if (it.type)$ ($it.type.label$) $endif$</a>
                            }$
                        </li>
                    }$
                </ul>
            $endif$
        </div>
        }$
    },
    related={
        $doc:{
        <div class="panel">
            <!--
            <h2>Förarbeten</h2>
            -->
            $if (it.rev)$
                <h2>Andra dokument som relaterar till detta</h2>
                <ul class="incoming">
                    $it.rev:{
                        <li>
                            <span class="rel">$if (it.inverse)$
                                $if (it.inverse.label)$
                                    $it.inverse.label$
                                $else$
                                    <code>$it.inverse.uri_term$</code>
                                $endif$
                            $else$
                                <em>Via <code>$it.uri_term$</code></em>
                            $endif$</span>:
                            $it.subject:{
                                <a href="$it.rdata_url$">$it.resource_uri$</a>
                            }$
                        </li>
                    }$
                    <!--
                    <li class="current">
                        $it.title$
                        ($it.serie.label$ $it.serieNummer$)
                    </li>
                    -->
                </ul>
            $endif$
        </div>
        }$
    }
)$
