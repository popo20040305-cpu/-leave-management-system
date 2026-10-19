// ---------- 基地址 ----------
var BASE_URL = 'http://127.0.0.1:8089/qingjia';

// ---------- JWT token 三件套 ----------
function getToken() {
    return sessionStorage.getItem('token');
}

// 给请求头加上 Authorization
function authHeaders(extra) {
    var h = extra || {};
    var t = getToken();
    if (t) h['Authorization'] = 'Bearer ' + t;
    return h;
}

// 遇到 401 就清 token、踢回登录页
function check401(r) {
    if (r.status === 401) {
        sessionStorage.removeItem('token');
        location.href = BASE_URL + '/login.html';
    }
    return r;
}

// ---------- jQuery 的全局钩子（管 $.ajax 的请求，比如 changePageSize） ----------
if (window.jQuery) {
    $.ajaxSetup({
        beforeSend: function (xhr) {
            var t = getToken();
            if (t) xhr.setRequestHeader('Authorization', 'Bearer ' + t);
        },
        complete: function (xhr) {
            if (xhr.status === 401) {
                sessionStorage.removeItem('token');
                location.href = BASE_URL + '/login.html';
            }
        }
    });
}

//修改页面大小
function changePageSize() {
    var pageSize = $("#pageSize").val()
    $.ajax({
        url: "/qingjia/setPageSize",
        type: 'GET',
        data: {"pageSize": pageSize},
        success: function (response) {
            if (response.code == 200) {
                alert("页面大小修改成功")
            }
        }
    });
}

// ---------- get 请求（fetch，加上了 token 和 401 处理） ----------
function ajaxGet(url, cb) {
    fetch(url, { headers: authHeaders() })
        .then(check401)
        .then(function (r) { return r.json(); })
        .then(cb)
        .catch(function () { alert('请求失败'); });
}

// ---------- post 请求 ----------
function ajaxPost(url, data, cb) {
    fetch(url, {
        method: 'POST',
        headers: authHeaders({ 'Content-Type': 'application/json', 'accept': 'application/json' }),
        body: JSON.stringify(data)
    })
        .then(check401)
        .then(function (r) { return r.json(); })
        .then(cb)
        .catch(function () { alert('保存失败'); });
}

// ---------- delete 请求 ----------
function ajaxDelete(url, cb) {
    fetch(url, { method: 'DELETE', headers: authHeaders() })
        .then(check401)
        .then(function (r) { return r.json(); })
        .then(cb)
        .catch(function () { alert('删除失败'); });
}
