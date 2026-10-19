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
public class FudaoyuanShenPiServiceImpl extends ServiceImpl<LeaveApplicationMapper, LeaveApplicationPojo>
implements FudaoyuanShenPiService {
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
    private static final int STATUS_FUDAOYUAN_PENDING = 3; // 待辅导员审批
    private static final int STATUS_YUANLINGDAO_PENDING = 4; // 待院领导审批（假设状态ID=4，请根据实际调整）
    private static final int STATUS_XIAOJIA = 5;           // 待销假
    private static final int STATUS_REJECTED = 6;          // 审批未通过

    @Override
    public Page<LeaveApplicationVo> pageByCollege(int curPage, Integer collegeId, Integer grade, Integer classId, Integer status) {
        return pageByCollege(curPage, PageConstant.pageSize, collegeId, grade, classId, status);
    }

    /**
     * 可指定每页条数的重载，给 /api/leave/fudaoyuan/index 用。
     */
    @Override
    public Page<LeaveApplicationVo> pageByCollege(long current, long size, Integer collegeId, Integer grade,
                                                  Integer classId, Integer status) {
        long safeCurrent = current < 1 ? 1 : current;
        long safeSize = size < 1 ? PageConstant.pageSize : size;
        // 1. 根据学院、年级、班级获取学生ID列表
        LambdaQueryWrapper<StudentPojo> stuWrapper = new LambdaQueryWrapper<>();
        // 先查班级，再查学生
        // 如果有班级ID，直接使用
        List<Integer> classIds = new ArrayList<>();
        if (classId != null && classId > 0) {
            classIds.add(classId);
        } else {
            // 否则根据学院和年级查班级
            LambdaQueryWrapper<ClassPojo> classWrapper = new LambdaQueryWrapper<>();
            classWrapper.eq(ClassPojo::getBelongToCollegeId, collegeId);
            if (grade != null) {
                classWrapper.eq(ClassPojo::getGrade, grade);
            }
            List<ClassPojo> classes = classService.list(classWrapper);
            classIds = classes.stream().map(ClassPojo::getId)
                    .collect(Collectors.toList());
        }

        if (classIds.isEmpty()) {
            return new Page<>(safeCurrent, safeSize);
        }

        stuWrapper.in(StudentPojo::getClassId, classIds);
        List<StudentPojo> students = studentService.list(stuWrapper);
        List<Integer> studentIds = students.stream().map(StudentPojo::getId)
                .collect(Collectors.toList());
        if (studentIds.isEmpty()) {
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

        // 3. 转换为Vo并填充关联信息
        Page<LeaveApplicationVo> voPage = new Page<>(pojoPage.getCurrent(), pojoPage.getSize(), pojoPage.getTotal());
        List<LeaveApplicationVo> voList = new ArrayList<>();

        // 批量查询学生和班级信息
        List<Integer> allStudentIds = pojoPage.getRecords().stream()
                .map(LeaveApplicationPojo::getStudentId)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, StudentPojo> studentMap = new HashMap<>();
        if (!allStudentIds.isEmpty()) {
            List<StudentPojo> studentsBatch = studentService.listByIds(allStudentIds);
            studentMap = studentsBatch.stream()
                    .collect(Collectors.toMap(StudentPojo::getId, s -> s));
        }
        Set<Integer> classIdSet = studentMap.values().stream()
                .map(StudentPojo::getClassId)
                .collect(Collectors.toSet());
        Map<Integer, ClassPojo> classMap = new HashMap<>();
        if (!classIdSet.isEmpty()) {
            List<ClassPojo> classes = classService.listByIds(new ArrayList<>(classIdSet));
            classMap = classes.stream().collect(Collectors.toMap(ClassPojo::getId, c -> c));
        }

        for (LeaveApplicationPojo pojo : pojoPage.getRecords()) {
            LeaveApplicationVo vo = new LeaveApplicationVo();
            BeanUtils.copyProperties(pojo, vo);
            vo.setTypeName(getTypeName(pojo.getType()));
            vo.setStatusName(getStatusName(pojo.getStatus()));

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

    // 辅助方法：从缓存获取名称
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
    @Override
    @Transactional
    public boolean approve(Integer applicationId, Integer teacherId, Integer result, String opinion) {
        LeaveApplicationPojo app = leaveApplicationService.getById(applicationId);
        if (app == null || app.getStatus() != STATUS_FUDAOYUAN_PENDING) {
            throw new ResultException(ResultEnum.LEAVE_APPLIICATION_STATUS_ERROR);
        }

        // 保存审批记录
        LeaveApplicationShenPiPojo shenPi = new LeaveApplicationShenPiPojo();
        shenPi.setApplicationId(applicationId);
        shenPi.setTeacherId(teacherId);
        shenPi.setResult(result);
        shenPi.setOpinion(opinion);
        shenPi.setCreateTime(new Date());
        shenPi.setRoleType(2); // 辅导员
        boolean saved = shenPiService.save(shenPi);
        if (!saved) {
            throw new ResultException(ResultEnum.EAVE_APPLIICATION_FUDAOYUAN_SHENPI_SAVE_ERROR);
        }

        // 更新申请状态
        if (result == 1) { // 通过
            int leaveType = app.getType();
            // 根据请假类型决定下一状态：假设类型1=一天以内，类型2=一天以外三天以内，类型3=三天以外，类型4=出恩施州
            // 按照业务规则，类型1在班主任审批后直接到待销假，但现在已经到了辅导员审批，说明类型1不会到辅导员，所以这里只有类型2、3、4
            // 类型2（一天以外三天以内）-> 待销假
            // 类型3（三天以外）、类型4（出恩施州）-> 待院领导审批
            if (leaveType == 2) {
                app.setStatus(STATUS_XIAOJIA);
            } else {
                app.setStatus(STATUS_YUANLINGDAO_PENDING);
            }
        } else {
            app.setStatus(STATUS_REJECTED);
        }
        return leaveApplicationService.updateById(app);
    }

}
