//消除iOS点击延迟
$(function () {
    FastClick.attach(document.body);
});

//提交一键评教请求
function postEvaluateForm() {
    $("#loadingToast, .weui_mask").show();
    let checked = $("#directlySubmit").prop("checked");
    $.ajax({
        url: '/evaluate',
        type: 'post',
        data: {
            directlySubmit: checked
        },
        success: function (result) {
            $("#loadingToast, .weui_mask").hide();
            if (result.success === true) {
                if (checked) {
                    weui.alert('已提交教学质量评价信息', {
                        title: '教学质量评价成功',
                        buttons: [{
                            label: '好',
                            type: 'primary'
                        }]
                    });
                } else {
                    weui.alert('请登录教务系统进行最终确认', {
                        title: '教学质量评价成功',
                        buttons: [{
                            label: '好',
                            type: 'primary'
                        }]
                    });
                }
            } else {
                $(".weui_warn").text(result.message).show().delay(2000).hide(0);
            }
        },
        error: function (result) {
            $("#loadingToast, .weui_mask").hide();
            if (result.status == 503) {
                //网络连接超时
                $(".weui_warn").text(result.responseJSON.message).show().delay(2000).hide(0);
            } else {
                $(".weui_warn").text("网络连接异常，请检查网络连接").show().delay(2000).hide(0);
            }
        }
    });
}