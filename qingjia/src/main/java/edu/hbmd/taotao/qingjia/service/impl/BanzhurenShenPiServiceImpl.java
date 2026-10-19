package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.common.DataInitializer;
import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.mapper.LeaveApplicationMapper;
import edu.hbmd.taotao.qingjia.pojo.*;
import edu.hbmd.taotao.qingjia.service.*;
import edu.hbmd.taotao.qingjia.vo.LeaveApplicationShenPiVo;
import edu.hbmd.taotao.qingjia.vo.LeaveApplicationVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletContext;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BanzhurenShenPiServiceImpl extends ServiceImpl<LeaveApplicationMapper, LeaveApplicationPojo>
implements BanzhurenShenPiService {
    @Autowired
    private LeaveApplicationService leaveApplicationService;
    @Autowired
    private LeaveApplicationShenPiService shenPiService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private ClassService classService;
    @Autowired
    private TeacherService teacherService;
    @Autowired
    private ServletContext servletContext;

    // 状态常量
    private static final int STATUS_BANZHU_PENDING = 2;   // 待班主任审批
    private static final int STATUS_FUDAOYUAN_PENDING = 3;
    private static final int STATUS_XIAOJIA = 5;
    private static final int STATUS_REJECTED = 6;

    @Override
    public Page<LeaveApplicationVo> pageByClassIds(int curPage, List<Integer> classIds, Integer status) {
        return pageByClassIds(curPage, PageConstant.pageSize, classIds, status);
    }

    /**
     * 可指定每页条数的重载，给 /api/leave/banzhuren/index 用。
     */
    @Override
    public Page<LeaveApplicationVo> pageByClassIds(long current, long size, List<Integer> classIds, Integer status) {
        long safeCurrent = current < 1 ? 1 : current;
        long safeSize = size < 1 ? PageConstant.pageSize : size;
// 1. 根据班级ID列表获取所有学生ID
        List<Integer> studentIds = new ArrayList<>();
        if (classIds != null && !classIds.isEmpty()) {
            LambdaQueryWrapper<StudentPojo> stuWrapper = new LambdaQueryWrapper<>();
            stuWrapper.in(StudentPojo::getClassId, classIds);
            List<StudentPojo> students = studentService.list(stuWrapper);
            studentIds = students.stream().map(StudentPojo::getId)
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (studentIds.isEmpty()) {
            // 如果没有学生，返回空分页
            return new Page<>(safeCurrent, safeSize);
        }

        // 2. 分页查询请假记录
        Page<LeaveApplicationPojo> page = new Page<>(safeCurrent, safeSize);
        LambdaQueryWrapper<LeaveApplicationPojo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(LeaveApplicationPojo::getStudentId, studentIds);
        if (status != null) {
            wrapper.eq(LeaveApplicationPojo::getStatus, status);
        }
        wrapper.orderByDesc(LeaveApplicationPojo::getId);
        Page<LeaveApplicationPojo> pojoPage = leaveApplicationService.page(page, wrapper);

        // 3. 转换为 Vo 并填充关联信息
        Page<LeaveApplicationVo> voPage = new Page<>(pojoPage.getCurrent(), pojoPage.getSize(), pojoPage.getTotal());
        List<LeaveApplicationVo> voList = new ArrayList<>();

        // 收集所有学生ID，批量查询学生信息
        List<Integer> allStudentIds = pojoPage.getRecords().stream()
                .map(LeaveApplicationPojo::getStudentId) // 收集所有学生ID
                .distinct()// 去重
                .collect(Collectors.toList());// 收集为list
        Map<Integer, StudentPojo> studentMap = new HashMap<>();
        if (!allStudentIds.isEmpty()) {
            List<StudentPojo> students = studentService.listByIds(allStudentIds);
            studentMap = students.stream().collect(Collectors.toMap(StudentPojo::getId, s -> s));
        }
        // 收集所有班级ID，批量查询班级信息
        Set<Integer> classIdSet = new HashSet<>();
        for (StudentPojo s : studentMap.values()) {
            classIdSet.add(s.getClassId());
        }
        Map<Integer, ClassPojo> classMap = new HashMap<>();
        if (!classIdSet.isEmpty()) {
            List<ClassPojo> classes = classService.listByIds(new ArrayList<>(classIdSet));
            classMap = classes.stream().collect(Collectors.toMap(ClassPojo::getId, c -> c));
        }

        for (LeaveApplicationPojo pojo : pojoPage.getRecords()) {
            LeaveApplicationVo vo = new LeaveApplicationVo();
            BeanUtils.copyProperties(pojo, vo);
            // 填充类型和状态名称（从缓存）
            vo.setTypeName(getTypeName(pojo.getType()));
            vo.setStatusName(getStatusName(pojo.getStatus()));

            // 填充学生姓名和班级名称
            StudentPojo stu = studentMap.get(pojo.getStudentId());
            if (stu != null) {
                vo.setStudentName(stu.getName());
                ClassPojo cls = classMap.get(stu.getClassId());
                if (cls != null) {
                    vo.setClassName(cls.getNum());
                }
            }

            // 填充审批记录
            LambdaQueryWrapper<LeaveApplicationShenPiPojo> sw = new LambdaQueryWrapper<>();
            sw.eq(LeaveApplicationShenPiPojo::getApplicationId, pojo.getId());
            sw.orderByAsc(LeaveApplicationShenPiPojo::getCreateTime);
            List<LeaveApplicationShenPiPojo> shenPiList = shenPiService.list(sw);
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
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;

    }

    @Override
    public LeaveApplicationVo getDetail(Integer applicationId) {
        return leaveApplicationService.getDetailById(applicationId);
    }

    @Override
    @Transactional
    public boolean approve(Integer applicationId, Integer teacherId, Integer result, String opinion) {
       LeaveApplicationPojo pojo = getById(applicationId);
       if (pojo==null){
           throw  new ResultException(ResultEnum.LEAVE_APPLIICATION_NOT_EXIST);
       }else if (pojo.getStatus() != STATUS_BANZHU_PENDING){
           throw  new ResultException(ResultEnum.LEAVE_APPLIICATION_STATUS_ERROR);
       }
       LeaveApplicationShenPiPojo shenPiPojo = new LeaveApplicationShenPiPojo();
       shenPiPojo.setApplicationId(applicationId);
       shenPiPojo.setTeacherId(teacherId);
       shenPiPojo.setResult(result);
       shenPiPojo.setOpinion(opinion);
       shenPiPojo.setCreateTime(new Date());
       shenPiPojo.setRoleType(1); // 辅导员
        boolean save = shenPiService.save(shenPiPojo);
        if (!save){
            throw  new ResultException(ResultEnum.EAVE_APPLIICATION_BANZHUREN_SHENPI_SAVE_ERROR);
        }
        // 3. 更新申请状态
        if (result == 1) { // 通过
            // 根据请假类型决定下一状态（假设类型1=一天以内，其他需要辅导员审批）
            int leaveType = pojo.getType();
            if (leaveType == 1) {
                pojo.setStatus(STATUS_XIAOJIA); // 待销假
            } else {
                pojo.setStatus(STATUS_FUDAOYUAN_PENDING); // 待辅导员审批
            }
        } else { // 不通过
            pojo.setStatus(STATUS_REJECTED);
        }

        return updateById(pojo);
    }

    // ---------- 从缓存获取请求类型名称 ----------
    private String getTypeName(Integer typeId) {
        List<LeaveApplicationTypePojo> types = (List<LeaveApplicationTypePojo>) servletContext
                .getAttribute(DataInitializer.LEAVE_APPLICATION_TYPES);
        if (types != null) {
            for (LeaveApplicationTypePojo t : types) {
                if (t.getId().equals(typeId)) return t.getName();
            }
        }
        return "未知类型";
    }
    //根据请求状态id获取请求状态名称
    private String getStatusName(Integer statusId) {
        List<ApplicationStatusPojo> statuses = (List<ApplicationStatusPojo>) servletContext
                .getAttribute(DataInitializer.APPLICATION_STATUS);
        if (statuses != null) {
            for (ApplicationStatusPojo s : statuses) {
                if (s.getId().equals(statusId)) return s.getApplicationStutas();
            }
        }
        return "未知状态";
    }

}
