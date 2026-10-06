<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>${paramName}</title>
</head>
<style>
    #paramContent {
        font-size: 20px;
        line-height: 30px;
        width: 100%;
    }

    .copyButton {
        background-color: #367b55;
        border-radius: 2px;
        font-size: 20px;
        padding: 5px;
        margin: 10px 0;
    }

    .content {
        text-align: center;
        margin: 0 300px;
        border-radius: 8px;
    }
</style>
<body>
<div class="content">
    <h3>${paramName}</h3>
    <textarea readonly rows="10" id="paramContent" onclick=copyinput()>
        ${paramValue}
    </textarea>
    <output type="button" class="copyButton" value="复制全部" onclick="copy()"/>
</div>

</body>
<script language="JavaScript">
    function copy() {
        var output = document.getElementById("paramContent");
        output.select();
        document.execCommand("Copy");
    }
</script>
</html>
