$sparqltrees/base_html(
    title={ $first(doc).identifier$ },
    heading={ $first(doc).title$ },
    main={
        $doc:{doc|
        <div class="doc">
            <h2>$doc.identifier$</h2>
            <p>
                <em><a href="$basePath$/browse/publ/$doc.type.qname$"
                        >$doc.type.label$</a></em>
            </p>
            <p><code>$doc.resource_uri$</code></p>
            <table class="docmeta">
                <tr>
                    <th>Utgivare</th>
                    <td>
                        <a href="$basePath$/browse/publ/$doc.type.qname$/$doc.publisher.uri_term$"
                            >$doc.publisher.name$</a>
                    </td>
                </tr>
                $if (doc.serie)$
                    <tr>
                        <th>Utgiven i</th>
                        <td>
                            $doc.serie.label$
                            <!--
                            <a href="$doc.serie.view_url$">$doc.serie.label$</a>
                            -->
                        </td>
                    </tr>
                $endif$
                $doc.daterel:{
                    <tr>
                        <th>
                            $if (it.label)$ $it.label$ $else$<code>$it.uri_term$</code>$endif$
                        </th>
                        <td>
                            <a href="$basePath$/list/publ/$doc.type.qname$/$doc.publisher.uri_term$/$it.qname$@$it.value.year$">$it.value.year$</a>-$it.value.monthDay$
                        </td>
                    </tr>
                }$
                $!<!-- TODO:
                <tr>
                    <th>Källa</th>
                    <td><a href="/">Regeringskansliets rättsdatabaser</a></td>
                </tr>
                -->!$
            </table>

            $if (doc.entry.repr)$
                <h3>Finns på formaten</h3>
                <ul class="representations">
                    $doc.entry.repr:{
                        <li>
                            <a href="$it.resource_uri$">
                                $if (mediaTypes.(it.mediaType))$
                                    $mediaTypes.(it.mediaType)$
                                $else$
                                    $it.mediaType$
                                $endif$
                            </a>
                        </li>
                    }$
                </ul>
            $endif$

            $if (doc.rel)$
                <h2>Relationer</h2>
                <ul class="relations">
                    $doc.rel:{
                        <li>
                            <span class="rel">$if (it.label)$
                                    $it.label$ $else$<code>$it.uri_term$</code>
                                $endif$ </span>:
                            <ul>
                            $it.subject:{
                                <li>
                                    <a href="$it.view_url$">$if (it.repr)$
                                            $it.repr$
                                        $else$
                                            <code>$it.resource_uri$</code>
                                        $endif$
                                        $if (it.type)$ ($it.type.label$) $endif$</a>
                                </li>
                            }$
                            </ul>
                        </li>
                    }$
                </ul>
            $endif$
        </div>
        }$
    },
    related={
        $doc:{doc|
        <div class="panel">
            $if (doc.rev)$
                <h2>Andra dokument som relaterar till detta</h2>
                <ul class="incoming">
                    $doc.rev:{
                        <li>
                            <span class="rel">$if (it.inverse)$
                                $if (it.inverse.label)$ $it.inverse.label$
                                $else$ <code>$it.inverse.uri_term$</code>
                                $endif$
                            $else$
                            <em>Via $if (it.label)$ $it.label$
                                $else$ <code>$it.uri_term$</code>
                                $endif$</em>
                            $endif$</span>:
                            <ul>
                            $it.subject:{
                                <li>
                                    <a href="$it.view_url$">$if (it.repr)$
                                            $it.repr$
                                        $else$
                                            <code>$it.resource_uri$</code>
                                        $endif$</a>
                                </li>
                            }$
                            </ul>
                        </li>
                    }$
                </ul>
            $endif$
        </div>
        }$
    }
)$
