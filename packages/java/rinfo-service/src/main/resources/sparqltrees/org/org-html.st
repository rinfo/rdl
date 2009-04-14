<html xml:lang="sv">
  <head profile="http://www.w3.org/ns/rdfa/">
    <title>Organisationer</title>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
    <link rel="stylesheet" href="/css/ontology.css" />
  </head>
  <body>
    <div id="main">
      <h1>Organisationer</h1>
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
                <em>$it.name</em>
                $it.someLabel$ ($it.type.label$)
              </p>
              $if (it.comment){
                <p>(<em>$it.comment$</em>)</p>
              }$
            </li>
          }$
          </ul>
        $endif$
        $if (it.feed)$
        <h3>Feeds</h3>
          $it.feed:{
            <dl>
              <dt>id</dt> <dd>$it.id.("\$value")$</dd>
              <dt>current</dt> <dd>$it.current.("\$uri")$</dd>
            </dl>
          }$
        $endif$
      }$
    </div>
  </body>
</html>
