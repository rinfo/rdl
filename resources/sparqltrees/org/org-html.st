<title>Organizations</title>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<h1>Organizations</h1>
<div>
  $org:{
    <h4>$it.name$
    $if (it.alt)$<span>($it.alt$)</span>$endif$
    </h4>
    $if (it.comment)$ <p><em>$it.comment$</em></p> $endif$
    $if (it.serie)$
      <h5>Series</h5>
      <ul>
      $it.serie:{
        <li><strong>$it.shortname$</strong>
          $it.someLabel$ ($it.type.label$)</li>
      }$
      </ul>
    $endif$
    $if (it.feed)$
    <h5>Feeds</h5>
      $it.feed:{
        <dl>
          <dt>id</dt> <dd>$it.id.("\$value")$</dd>
          <dt>current</dt> <dd>$it.current.("\$uri")$</dd>
        </dl>
      }$
    $endif$
  }$
</div>
