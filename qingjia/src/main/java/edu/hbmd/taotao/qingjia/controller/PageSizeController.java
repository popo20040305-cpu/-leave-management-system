package edu.hbmd.taotao.qingjia.controller;

import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.util.R;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
@Controller
public class PageSizeController {
    @GetMapping("/setPageSize")
    @ResponseBody
    public R setPageSize(Integer pageSize){
        if (pageSize<=0){
            pageSize = 10;
        }
        PageConstant.pageSize = pageSize;
        return  R.ok("页面大小设置成功");
    }

}
