package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.common.DataInitializer;
import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.exception.ResultExceptionError;
import edu.hbmd.taotao.qingjia.mapper.LeaveApplicationMapper;
import edu.hbmd.taotao.qingjia.pojo.*;
import edu.hbmd.taotao.qingjia.properties.UploadFileProperties;
import edu.hbmd.taotao.qingjia.service.*;
import edu.hbmd.taotao.qingjia.vo.LeaveApplicationShenPiVo;
import edu.hbmd.taotao.qingjia.vo.LeaveApplicationVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class LeaveApplicationServiceImpl extends ServiceImpl<LeaveApplicationMapper, LeaveApplicationPojo>
implements LeaveApplicationService {
    @Autowired
    private TeacherService teacherService;  // 需要注入教师Service
    @Autowired
    LeaveApplicationShenPiService shenPiService;

    @Autowired
    private ServletContext servletContext;
    @Autowired
    StudentService studentService;
    @Autowired
    ClassService classService;
    @Autowired
    UploadFileProperties uploadFileProperties;

    private static final int STATUS_DRAFT = 1;          // 待提交
    private static final int STATUS_BANZHU_PENDING = 2; // 待班主任审批
    private static final int STATUS_XIAOJIA = 5;        // 待销假
    private static final int STATUS_ARCHIVED = 7;       // 归档

    @Override
    public Page<LeaveApplicationVo> pageMyApplications(int curPage, Integer studentId, Integer status) {
        return pageMyApplications(curPage, PageConstant.pageSize, studentId, status);
    }

    /**
     * 可指定每页条数的重载，给 /api/leave/student/index 用。
     * 两参版本保留不动（老 Thymeleaf 的 LeaveApplicationController.query 还在调它）。
     */
    @Override
    public Page<LeaveApplicationVo> pageMyApplications(long current, long size, Integer studentId, Integer status) {
        long safeCurrent = current < 1 ? 1 : current;
        long safeSize = size < 1 ? PageConstant.pageSize : size;
        Page<LeaveApplicationPojo> page = new Page<>(safeCurrent, safeSize);
        LambdaQueryWrapper<LeaveApplicationPojo> wrapper = new LambdaQueryWrapper<>();
        if (studentId!=null){
            wrapper.eq(LeaveApplicationPojo::getStudentId,studentId);
        }
        if (status!=null){
            wrapper.eq(LeaveApplicationPojo::getStatus,status);
        }
        Page<LeaveApplicationPojo> pojoPage = this.page(page,wrapper);
       // 2. 转换为Vo并填充关联数据（不联表，循环查询）
        Page<LeaveApplicationVo> voPage = new Page<>(pojoPage.getCurrent(), pojoPage.getSize(), pojoPage.getTotal());
        List<LeaveApplicationVo> voList = new ArrayList<>();
        for (LeaveApplicationPojo laPojo:pojoPage.getRecords()){
            LeaveApplicationVo vo = new LeaveApplicationVo();
            BeanUtils.copyProperties(laPojo, vo);
            vo.setStatusName(getStatusName(laPojo.getStatus()));
            vo.setTypeName(getTypeName(laPojo.getType()));
            // 填充审批记录（只查询，不联表）
            LambdaQueryWrapper<LeaveApplicationShenPiPojo> shenPiWapper=new LambdaQueryWrapper<>();
            shenPiWapper.eq(LeaveApplicationShenPiPojo::getApplicationId,laPojo.getId());
            List<LeaveApplicationShenPiPojo> shenPiList = shenPiService.list(shenPiWapper);
            List<LeaveApplicationShenPiVo> shenPiVoList = new ArrayList<>();
            for (LeaveApplicationShenPiPojo sp : shenPiList) {
                LeaveApplicationShenPiVo spVo = new LeaveApplicationShenPiVo();
                BeanUtils.copyProperties(sp, spVo);
                // 查询教师姓名
                TeacherPojo teacher = teacherService.getById(sp.getTeacherId());
                if (teacher != null) {
                    spVo.setTeacherName(teacher.getName());
                }
                shenPiVoList.add(spVo);
            }
            vo.setShenPiList(shenPiVoList);
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public boolean saveDraft(LeaveApplicationPojo application) {
        application.setStatus(STATUS_DRAFT);
        return this.saveOrUpdate(application);
    }

    @Override
    public boolean submitApplication(LeaveApplicationPojo application) {
        if (application.getId() == null) {
            // 新增提交：状态设为2（待班主任审批）
            application.setStatus(STATUS_BANZHU_PENDING);
            return save(application);
        } else {
            // 更新并提交：校验状态和权限
            LeaveApplicationPojo exist = getById(application.getId());
            if (exist == null || !exist.getStudentId().equals(application.getStudentId())) {
                throw new ResultException(ResultEnum.LEAVE_APPLIICATION_NO_PERMISSION);
            }
            if (exist.getStatus() != STATUS_DRAFT) {
                throw new ResultException(ResultEnum.LEAVE_APPLIICATION_NOT_SUBMIT);
            }
            // 更新基本信息，并将状态改为2
            exist.setType(application.getType());
            exist.setPath(application.getPath());
            exist.setReason(application.getReason());
            exist.setBegintime(application.getBegintime());
            exist.setEndtime(application.getEndtime());
            exist.setStatus(STATUS_BANZHU_PENDING);
            return updateById(exist);
        }
    }

    @Override
    public boolean submitDraft(Integer applicationId) {
        LeaveApplicationPojo app = getById(applicationId);
        if (app==null){
            throw new ResultException(ResultEnum.LEAVE_APPLIICATION_NOT_EXIST);
        }if (app.getStatus()!=STATUS_DRAFT){
            throw new ResultException(ResultEnum.LEAVE_APPLIICATION_STATUS_ERROR);
        }
        app.setStatus(STATUS_BANZHU_PENDING);
        return updateById(app);
    }

    @Override
    public boolean deleteDraft(Integer id) {
        LeaveApplicationPojo app=getById(id);
         if (app!=null&&app.getStatus()==STATUS_DRAFT) {
             return removeById(id);
         }
        return false;
    }

    @Override
    public LeaveApplicationVo getDetailById(Integer id) {
        LeaveApplicationPojo pojo = getById(id);
        if (pojo==null){
            throw new ResultException(ResultEnum.LEAVE_APPLIICATION_NOT_EXIST);
        }else{
            LeaveApplicationVo vo = new LeaveApplicationVo();
            BeanUtils.copyProperties(pojo,vo);
            vo.setTypeName(getTypeName(pojo.getType()));
            vo.setStatusName(getStatusName(pojo.getStatus()));

            StudentPojo student=studentService.getById(pojo.getStudentId());
            if (student!=null){
                vo.setStudentName(student.getName());
                ClassPojo clazz  = classService.getById(student.getClassId());
                if (clazz!=null){
                    vo.setClassName(clazz.getNum());
                }
            }
            LambdaQueryWrapper<LeaveApplicationShenPiPojo> shenPiWapper = new LambdaQueryWrapper<>();
            shenPiWapper.eq(LeaveApplicationShenPiPojo::getApplicationId,pojo.getId());
            List<LeaveApplicationShenPiPojo> shenPiList = shenPiService.list(shenPiWapper);
            List<LeaveApplicationShenPiVo> shenPiVoList = new ArrayList<>();
            for (LeaveApplicationShenPiPojo sp : shenPiList) {
                LeaveApplicationShenPiVo spVo = new LeaveApplicationShenPiVo();
                BeanUtils.copyProperties(sp, spVo);
                TeacherPojo teacher = teacherService.getById(sp.getTeacherId());
                if (teacher != null) {
                    spVo.setTeacherName(teacher.getName());
                }
                shenPiVoList.add(spVo);
            }
            vo.setShenPiList(shenPiVoList);
            return vo;
        }
    }

    @Override
    public boolean xiaoJia(Integer applicationId, MultipartFile photo,String stuNum) {
        LeaveApplicationPojo app = getById(applicationId);
        if (app == null || app.getStatus() != STATUS_XIAOJIA) {
            throw new ResultException(ResultEnum.LEAVE_APPLIICATION_STATUS_ERROR);
        }
        // 上传照片
        if (photo != null && !photo.isEmpty()) {
            try {
                String originalFilename = photo.getOriginalFilename();
                String ext = originalFilename.substring(originalFilename.lastIndexOf("."));

                String newFileName = "xiaojia" + stuNum+applicationId + ext;
                String uploadPath = uploadFileProperties.getUploadFileXiaoJiaPath();
                File dir = new File(uploadPath);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File dest = new File(dir, newFileName);
                photo.transferTo(dest);
                app.setXiaoJiaPhotoUrl("/"+uploadPath.substring(uploadPath.lastIndexOf("\\")+1) +"/"+ newFileName);
            } catch (IOException e) {
                throw new ResultExceptionError("销假照片保存异常:"+e.getMessage());
            }
        } else {
            throw new ResultException(ResultEnum.EAVE_APPLIICATION_XIAO_JIA_PHOTO_NOT_EXIST);
        }
        app.setXiaoJiaTime(new Date());
        app.setStatus(STATUS_ARCHIVED);
        return updateById(app);

    }

    // 辅助方法：获取类型名称（从缓存）
    private String getTypeName(Integer typeId) {
        List<LeaveApplicationTypePojo>  leaveTypes = (List<LeaveApplicationTypePojo>) servletContext.getAttribute(DataInitializer.LEAVE_APPLICATION_TYPES);
        for (LeaveApplicationTypePojo type:leaveTypes) {
            if (type.getId() == typeId){
                return type.getName();
            }
        }
        return "";
    }

    private String getStatusName(Integer statusId) {
        // 从缓存获取状态名称
        List<ApplicationStatusPojo> stutuses = (List<ApplicationStatusPojo>) servletContext.getAttribute(DataInitializer.APPLICATION_STATUS);
        for (ApplicationStatusPojo status:stutuses){
            if (status.getId() == statusId){
                return status.getApplicationStutas();
            }
        }
        return "状态" + statusId;
    }

}
