<?php defined('POPLR') or die() ?>
<!doctype html>
<!--[if lt IE 7]> <html class="no-js lt-ie9 lt-ie8 lt-ie7" lang="en"> <![endif]-->
<!--[if IE 7]> <html class="no-js lt-ie9 lt-ie8" lang="en"> <![endif]-->
<!--[if IE 8]> <html class="no-js lt-ie9" lang="en"> <![endif]-->
<!--[if gt IE 8]><!--> <html class="no-js" lang="en"> <!--<![endif]-->
<head>
	<?php $poplr->template('layout.htmlhead') ?>
</head>
<body>
	<?php $poplr->contentTemplate() ?>
	<?php $poplr->template('layout.htmlfoot') ?>
</body>
</html>