<!DOCTYPE html>
<html lang="en">
<head>
    <title>Home page</title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <meta name="theme-color" content="#563d7c">
</head>
<body>

<h1>Data</h1>
<p>${mapDataView}</p>
<#if mapData??>
<table class="table table-borderless" id="table_times">
    <thead>
    <tr>
        <th scope="col">Id</th>
        <th scope="col">Name</th>
        <th scope="col"> </th>
    </tr>
    </thead>
    <tbody>
    <#list mapData as obj>
    <tr id="tr_time">
        <td>${obj.id}</td>
        <td><#if obj.name??>${obj.name}<#else>---</#if></td>
        <#if isAdmin><td><a href="/app/${obj.roomNumber}">edit</a> &emsp;&emsp; <a href="/user/${obj.username}/${obj.roomNumber}">delete</a></td></#if>
    </tr>
    </#list>
    </tbody>
</table>
</#if>
</body>
</html>