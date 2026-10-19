package edu.hbmd.taotao.qingjia.api;

import edu.hbmd.taotao.qingjia.controller.BaseController;
import edu.hbmd.taotao.qingjia.util.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiBaseInfoController extends BaseController {
    @GetMapping("/baseInfo")
    public R baseInfo(){
        Map<String,Object> data = new HashMap<>();
        data.put(COLLEGES,getColleges());
        data.put(GRADES,getGrades());
        data.put(APROVE_TYPES,getAproveTypes());
        data.put(LEAVE_APPLICATION_TYPES,getleaveApplicationTypes());
        data.put(MENY_TYPES,getMenuTypes());
        data.put(ROLE_TYPES,getRoleTypes());
        data.put(SEX_TYPES,getSexTypes());
        data.put(APPLICATION_STATUSES,getApplicationStatuses());
        return R.ok().put("data",data);
    }

}
